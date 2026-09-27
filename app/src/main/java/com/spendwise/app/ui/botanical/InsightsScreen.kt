package com.spendwise.app.ui.botanical

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.spendwise.app.R
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MonthlyAggregate
import com.spendwise.app.ui.DashboardUiState
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class InsightsScope(val label: String) { Month("Month"), Year("Year") }
private enum class BreakdownMode(val label: String) { Spending("Spending"), Income("Income") }

/** A category's total for the period being viewed. */
data class CategoryShare(val category: Category, val cents: Long)

/** One view of the category breakdown, so a change of view can crossfade. */
private data class Breakdown(
    val period: InsightsPeriod,
    val mode: BreakdownMode,
    val total: Long,
    val shares: List<CategoryShare>
)

/** The period an Insights view (or a category breakdown opened from it) covers. */
sealed interface InsightsPeriod {
    data class OfMonth(val month: YearMonth) : InsightsPeriod
    data class OfYear(val year: Int) : InsightsPeriod
}

fun InsightsPeriod.label(): String = when (this) {
    is InsightsPeriod.OfMonth -> monthLabel(month)
    is InsightsPeriod.OfYear -> year.toString()
}

@Composable
fun InsightsScreen(
    state: DashboardUiState,
    month: YearMonth,
    yearExpenses: List<Expense>,
    aggregates: List<MonthlyAggregate>,
    bottomPadding: Dp,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onCategory: (Category, InsightsPeriod) -> Unit,
    onExportCsv: (InsightsPeriod) -> Unit
) {
    var scope by rememberSaveable { mutableStateOf(InsightsScope.Month) }
    var mode by rememberSaveable { mutableStateOf(BreakdownMode.Spending) }
    val byMonth = remember(aggregates) { aggregateTotals(aggregates) }
    val period: InsightsPeriod = if (scope == InsightsScope.Year) InsightsPeriod.OfYear(month.year) else InsightsPeriod.OfMonth(month)
    val now: Totals
    val before: Totals
    val beforeLabel: String
    if (scope == InsightsScope.Year) {
        now = yearTotals(aggregates, month.year)
        before = yearTotals(aggregates, month.year - 1)
        beforeLabel = (month.year - 1).toString()
    } else {
        now = Totals(state.summary.totalIncomeCents, state.summary.totalExpenseCents)
        before = byMonth[month.minusMonths(1)] ?: Totals(0, 0)
        beforeLabel = monthShortGb(month.minusMonths(1).monthValue)
    }
    val shares = remember(scope, mode, state.summary, state.categories, yearExpenses) {
        val income = mode == BreakdownMode.Income
        val totals: Map<Long, Long> = if (scope == InsightsScope.Month) {
            (if (income) state.summary.incomeCategoryTotals else state.summary.categoryTotals)
                .associate { it.categoryId to it.totalCents }
        } else {
            val incomeIds = incomeCategoryIds(state.categories)
            yearExpenses
                .filter { (it.categoryId in incomeIds) == income }
                .groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
        }
        state.categories
            .filter { it.isIncomeAdjustment == income }
            .map { CategoryShare(it, totals[it.id] ?: 0L) }
            .filter { it.cents > 0 }
            .sortedByDescending { it.cents }
    }
    val breakdownTotal = if (mode == BreakdownMode.Income) now.income else now.expense

    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item(key = "header") {
                Box(itemMotion()) {
                    ScreenHeader("Insights", month, onMonthChange, onOpenPeriodPicker) {
                        BotIcon(BotIcons.Insights, size = 22.dp, tint = Color.White)
                    }
                }
            }
            item(key = "scope") {
                TabSegment(
                    options = InsightsScope.entries,
                    selected = scope,
                    onSelect = { scope = it },
                    label = { it.label },
                    modifier = itemMotion().padding(top = 16.dp, bottom = 16.dp)
                )
            }
            item(key = "net") {
                Column(itemMotion()) {
                    AnimatedContent(
                        targetState = if (scope == InsightsScope.Year) "${month.year} · Net cash flow" else "Net cash flow",
                        transitionSpec = {
                            fadeIn(BotMotion.smooth(0.28f))
                                .togetherWith(fadeOut(BotMotion.smooth(0.18f)))
                                .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                        },
                        label = "insightsKicker"
                    ) { kicker ->
                        Kicker(kicker, Modifier.padding(bottom = 8.dp))
                    }
                    Money(
                        cents = now.net,
                        style = moneyStyle(58f, letterSpacingPx = -2f),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        animate = true
                    )
                }
            }
            item(key = "totals") {
                InsightsTotals(now, before, beforeLabel, itemMotion().padding(top = 20.dp, bottom = 24.dp))
            }
            item(key = "cashflow") {
                Box(itemMotion()) { CashFlowPanel(aggregates = byMonth, month = month) }
            }
            item(key = "csv") {
                BotButton(
                    "Export ${period.label()} as CSV",
                    onClick = { onExportCsv(period) },
                    type = ButtonType.Secondary,
                    modifier = itemMotion().fillMaxWidth().padding(top = 16.dp)
                )
            }
            item(key = "categories") {
                Panel(itemMotion().padding(top = 14.dp)) {
                    SectionHead("By category") {
                        Money(
                            cents = breakdownTotal,
                            style = moneyStyle(25f, letterSpacingPx = -0.6f),
                            color = Bot.SurfaceInk,
                            centsScale = 0.8f,
                            abbreviate = true,
                            modifier = Modifier.widthIn(max = 180.dp),
                            align = Alignment.End,
                            animate = true
                        )
                    }
                    TabSegment(
                        options = BreakdownMode.entries,
                        selected = mode,
                        onSelect = { mode = it },
                        label = { it.label },
                        inPanel = true,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                    )
                    // Another view of the breakdown crossfades in while the card eases to its new height.
                    AnimatedContent(
                        targetState = Breakdown(period, mode, breakdownTotal, shares),
                        transitionSpec = {
                            fadeIn(BotMotion.smooth(0.32f))
                                .togetherWith(fadeOut(BotMotion.smooth(0.2f)))
                                .using(SizeTransform(clip = true) { _, _ -> BotMotion.Resize })
                        },
                        contentKey = { it.period to it.mode },
                        label = "breakdown"
                    ) { shown ->
                        Column {
                            if (shown.total == 0L || shown.shares.isEmpty()) {
                                EmptyState(
                                    title = "No category activity yet",
                                    body = "Add entries in this period to see your breakdown.",
                                    onDark = false
                                )
                            } else {
                                shown.shares.forEachIndexed { index, share ->
                                    BreakdownRow(
                                        share = share,
                                        total = shown.total,
                                        modeLabel = shown.mode.label.lowercase(),
                                        showDivider = index < shown.shares.lastIndex,
                                        onClick = { onCategory(share.category, shown.period) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "note") {
                Note(
                    "Transfers and opening balance adjustments are excluded from income and spending.",
                    modifier = itemMotion().padding(top = 25.dp)
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(share: CategoryShare, total: Long, modeLabel: String, showDivider: Boolean, onClick: () -> Unit) {
    val art = share.category.art
    Column(Modifier.fillMaxWidth().botRowPress(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArtImage(art.drawable, size = 43.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        share.category.name,
                        style = display(20f),
                        color = Bot.SurfaceInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Money(
                        cents = share.cents,
                        style = moneyStyle(25f, letterSpacingPx = -0.6f),
                        color = Bot.SurfaceInk,
                        centsScale = 0.8f
                    )
                }
                Track(share.cents.toFloat() / total, art.color, Modifier.padding(top = 8.dp, bottom = 6.dp))
                Text(
                    "${(share.cents * 100.0 / total).roundToInt()}% of $modeLabel",
                    style = body(10f),
                    color = Color(0xFF818897)
                )
            }
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDF0F3)))
    }
}

/**
 * Income and expenses as a diptych cut from one pixel-art scene: dawn for
 * money in, dusk for money out. The horizon runs continuously across the gap.
 */
@Composable
private fun InsightsTotals(now: Totals, before: Totals, beforeLabel: String, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.insights_totals_art)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TotalCard("Income", now.income, before.income, beforeLabel, income = true, art = art, modifier = Modifier.weight(1f))
        TotalCard("Expenses", now.expense, before.expense, beforeLabel, income = false, art = art, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TotalCard(
    title: String,
    cents: Long,
    before: Long,
    beforeLabel: String,
    income: Boolean,
    art: ImageBitmap,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    val gap = with(LocalDensity.current) { 10.dp.toPx() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .heightIn(min = 150.dp)
            .clip(shape)
            .background(if (income) Color(0xFF0C1A2C) else Color(0xFF0A0B1E))
            .semantics(mergeDescendants = true) {}
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Scene spans both cards plus the gap; each card shows its half.
            val sceneWidth = size.width * 2 + gap
            val sceneHeight = sceneWidth * art.height / art.width
            val left = if (income) 0f else size.width - sceneWidth
            drawImage(
                image = art,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(art.width, art.height),
                dstOffset = IntOffset(left.roundToInt(), (size.height - sceneHeight).roundToInt()),
                dstSize = IntSize(sceneWidth.roundToInt(), sceneHeight.roundToInt()),
                filterQuality = FilterQuality.None
            )
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0x59050A17),
                    0.3f to Color(0x00050A17),
                    0.55f to Color(0x00050A17),
                    1f to Color(0xB3050A17)
                )
            )
        }
        Box(Modifier.matchParentSize().border(1.dp, Color(0x1FFFFFFF), shape))
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (income) Bot.PositiveOnDark else Bot.NegativeOnDark)
                )
                Text(title, style = body(14f, FontWeight.Medium, lineHeight = 1.3f), color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Money(
                cents = cents,
                style = moneyStyle(34f, letterSpacingPx = -1f),
                color = Color.White,
                abbreviate = true,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                animate = true
            )
            ChangeChip(now = cents, before = before, label = beforeLabel, income = income)
        }
    }
}

/** Change against the previous period. Rising income reads as good; rising spending as caution. */
@Composable
private fun ChangeChip(now: Long, before: Long, label: String, income: Boolean) {
    val pct = if (before == 0L) null else ((now - before) * 100.0 / before).roundToInt()
    val text = when {
        pct == null -> "Nothing recorded in $label"
        pct == 0 -> "Same as $label"
        else -> "${abs(pct)}% vs $label"
    }
    val color = when {
        pct == null || pct == 0 -> Color(0xFFE6EBF5)
        (if (income) pct > 0 else pct < 0) -> Color(0xFFBFF0D6)
        else -> Color(0xFFFFC9D4)
    }
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0x99081126))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (pct != null && pct != 0) BotIcon(if (pct > 0) BotIcons.Up else BotIcons.Down, size = 13.dp, tint = color)
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                fadeIn(BotMotion.smooth(0.28f))
                    .togetherWith(fadeOut(BotMotion.smooth(0.18f)))
                    .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
            },
            label = "changeChip"
        ) { line ->
            Text(line, style = body(12f, FontWeight.Medium, lineHeight = 1.3f), color = animatedColor(color, "changeInk"), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Twelve months of income and expenses. The window stays anchored to the
 * current month so the chart doesn't shift as the period changes — the
 * picked month is highlighted instead; only a month older than the window
 * moves it back. A tapped bar holds until the period changes.
 */
@Composable
private fun CashFlowPanel(aggregates: Map<YearMonth, Totals>, month: YearMonth) {
    val current = YearMonth.from(todayKl())
    val end = if (month > current.minusMonths(12)) current else month
    val months = remember(end) { (11 downTo 0).map { end.minusMonths(it.toLong()) } }
    var selected by remember(month) { mutableStateOf(month) }
    val index = months.indexOf(selected).takeIf { it >= 0 } ?: 11
    val bars = months.map { aggregates[it] ?: Totals(0, 0) }
    val maxValue = maxOf(bars.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L, 1L)
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    Panel {
        SectionHead("Cash flow", modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                if (end == current) "Last 12 months" else "12 months",
                style = body(13f, FontWeight.Medium, lineHeight = 1.4f),
                color = Bot.ChartLabel
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${monthLabel(months.first())} – ${monthLabel(end)}",
                style = body(13f, FontWeight.Medium, lineHeight = 1.4f),
                color = Bot.ChartLabel,
                modifier = Modifier.weight(1f)
            )
            ChartPageButton(BotIcons.Back, "Earlier cash flow months", enabled = scroll.value > 2) {
                scope.launch { scroll.animateScrollTo((scroll.value - scroll.viewportSize).coerceAtLeast(0)) }
            }
            Spacer(Modifier.width(4.dp))
            ChartPageButton(BotIcons.Next, "Later cash flow months", enabled = scroll.value < scroll.maxValue - 2) {
                scope.launch { scroll.animateScrollTo((scroll.value + scroll.viewportSize).coerceAtMost(scroll.maxValue)) }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 12.dp
            val groupWidth = maxOf(42.dp, (maxWidth - 60.dp) / 6)
            val groupPx = with(density) { (groupWidth + gap).toPx() }
            LaunchedEffect(month, scroll.maxValue) {
                // Start at the most recent months, then make sure the picked one is visible.
                val target = months.indexOf(month)
                val leftEdge = if (target >= 0) (target * groupPx).roundToInt() else scroll.maxValue
                scroll.scrollTo(minOf(scroll.maxValue, maxOf(leftEdge - (scroll.viewportSize - groupPx.roundToInt()), 0)))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp)
                    .horizontalScroll(scroll)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                months.forEachIndexed { i, m ->
                    val isSelected = i == index
                    val fade by animateFloatAsState(if (isSelected) 1f else 0.28f, BotMotion.smooth(0.3f), label = "bar")
                    val labelInk = animatedColor(if (isSelected) Bot.Navy else Bot.ChartLabel.copy(alpha = 0.6f), "barLabel")
                    Column(
                        modifier = Modifier
                            .width(groupWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .botPress(scale = 1f) { selected = m }
                            .semantics {
                                this.selected = isSelected
                                contentDescription = "${monthLabel(m)}: income ${formatRm(bars[i].income)}, expenses ${formatRm(bars[i].expense)}"
                            }
                            .padding(horizontal = 3.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            listOf(bars[i].income to Bot.IncomeBar, bars[i].expense to Bot.ExpenseBar).forEach { (value, color) ->
                                Box(
                                    Modifier
                                        .width(17.dp)
                                        .growingBar((value.toFloat() / maxValue).coerceIn(0f, 1f), order = i, minHeight = 2.dp)
                                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        .graphicsLayer { alpha = fade }
                                        .background(color)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            monthShort(m.monthValue),
                            style = body(13f, if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = labelInk
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Income" to Bot.IncomeBar, "Expenses" to Bot.ExpenseBar).forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                    Text(label, style = body(13f), color = Color(0xFF788095))
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(Bot.RuleSoft))
        // The readout slides the way the selection moved.
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(BotMotion.smooth(0.36f, IntOffset.VisibilityThreshold)) { w -> direction * w / 6 } + fadeIn(BotMotion.smooth(0.28f)))
                    .togetherWith(slideOutHorizontally(BotMotion.smooth(0.36f, IntOffset.VisibilityThreshold)) { w -> -direction * w / 6 } + fadeOut(BotMotion.smooth(0.18f)))
            },
            modifier = Modifier
                .padding(top = 14.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            label = "cashFlowDetail"
        ) { shownIndex ->
            Column(Modifier.fillMaxWidth()) {
                Text(monthLabel(months[shownIndex]), style = body(14f, FontWeight.SemiBold), color = Bot.SurfaceInk)
                Spacer(Modifier.height(8.dp))
                DetailLine("Income", formatRm(bars[shownIndex].income))
                DetailLine("Expenses", formatRm(bars[shownIndex].expense))
                DetailLine("Net cash flow", formatRm(bars[shownIndex].net))
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = body(14f, lineHeight = 1.5f), color = Bot.SurfaceInk, modifier = Modifier.weight(1f))
        Text(
            value,
            style = body(14f, FontWeight.SemiBold, lineHeight = 1.5f).copy(fontFeatureSettings = TabularNums),
            color = Bot.SurfaceInk
        )
    }
}

@Composable
private fun ChartPageButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Bot.ChipBg)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = 16.dp, tint = Color(0xFF263E61))
    }
}

