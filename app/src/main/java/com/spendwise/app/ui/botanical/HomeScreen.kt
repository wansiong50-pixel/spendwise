package com.spendwise.app.ui.botanical

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.R
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Transfer
import com.spendwise.app.ui.DashboardUiState
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: DashboardUiState,
    month: YearMonth,
    monthTransfers: List<Transfer>,
    bottomPadding: Dp,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAdd: (EntryKind) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenActivity: () -> Unit,
    onCategory: (Category) -> Unit,
    onDay: (LocalDate) -> Unit,
    onOpenItem: (LedgerItem) -> Unit,
    onHeroCoveredChange: (Boolean) -> Unit
) {
    val incomeIds = remember(state.categories) { incomeCategoryIds(state.categories) }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val names = remember(state.accounts, state.archivedAccounts) { accountNames(state.accounts + state.archivedAccounts) }
    val recent = remember(state.expenses, monthTransfers, incomeIds) {
        ledgerItems(state.expenses, monthTransfers, incomeIds).take(3)
    }
    val spentByCategory = remember(state.summary) {
        state.summary.categoryTotals.associate { it.categoryId to it.totalCents }
    }

    // Once the painted hero has scrolled out from under the status bar, the
    // shell swaps in a paper scrim with dark icons.
    val scroll = rememberScrollState()
    val bounce = rememberBounceOverscroll()
    var heroHeight by remember { mutableIntStateOf(0) }
    val statusPx = with(LocalDensity.current) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val covered by remember(statusPx) {
        derivedStateOf { heroHeight > 0 && scroll.value > heroHeight - statusPx }
    }
    LaunchedEffect(covered) { onHeroCoveredChange(covered) }

    Box(Modifier.fillMaxSize().background(Bot.Paper)) {
        HeroPainting(
            heroHeight = heroHeight,
            top = { bounce.offsetY - scroll.value }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll, overscrollEffect = bounce)
                .padding(bottom = bottomPadding)
        ) {
            HomeContent(
                heroModifier = Modifier.onSizeChanged { heroHeight = it.height },
                state = state,
                month = month,
                recent = recent,
                categoriesById = categoriesById,
                names = names,
                spentByCategory = spentByCategory,
                onMonthChange = onMonthChange,
                onOpenPeriodPicker = onOpenPeriodPicker,
                onOpenSettings = onOpenSettings,
                onOpenAccounts = onOpenAccounts,
                onAdd = onAdd,
                onOpenBudgets = onOpenBudgets,
                onOpenInsights = onOpenInsights,
                onOpenActivity = onOpenActivity,
                onCategory = onCategory,
                onDay = onDay,
                onOpenItem = onOpenItem
            )
        }
    }
}

private val HeroShape = RoundedCornerShape(bottomStart = 46.dp, bottomEnd = 46.dp)

/**
 * The painted flower behind the hero. It lives outside the scrolling column
 * so it can do what an iOS stretchy header does: scroll with the hero, and
 * when Home is pulled down past its top, stay pinned to the top of the
 * screen and grow to fill the gap instead of leaving blank paper. [top] is
 * where the hero currently starts on screen.
 */
@Composable
private fun HeroPainting(heroHeight: Int, top: () -> Float) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fallback = with(LocalDensity.current) { (320.dp + statusTop).roundToPx() }
    // Only a pull past the top changes the frame's size; ordinary scrolling just moves it.
    val stretch = remember { derivedStateOf { top().coerceAtLeast(0f) } }
    Box(
        Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                // Pulled down: the frame grows to meet the hero again, its top pinned to
                // the screen, and the painting (aspect fill) zooms to fill it.
                val height = (if (heroHeight > 0) heroHeight else fallback) + stretch.value.roundToInt()
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .graphicsLayer {
                translationY = top().coerceAtMost(0f)
                shape = HeroShape
                clip = true
            }
    ) {
        Image(
            painter = painterResource(R.drawable.botanical_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Legibility wash from the bottom: #030916 at 85% fading out by 65%.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color.Transparent,
                        1f to Color(0xD9030916)
                    )
                )
        )
    }
}

@Composable
private fun HomeContent(
    heroModifier: Modifier,
    state: DashboardUiState,
    month: YearMonth,
    recent: List<LedgerItem>,
    categoriesById: Map<Long, Category>,
    names: Map<Long, String>,
    spentByCategory: Map<Long, Long>,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAdd: (EntryKind) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenActivity: () -> Unit,
    onCategory: (Category) -> Unit,
    onDay: (LocalDate) -> Unit,
    onOpenItem: (LedgerItem) -> Unit
) {
    Column {
        HomeHero(
            modifier = heroModifier,
            month = month,
            totalBalance = state.totalBalanceCents,
            onMonthChange = onMonthChange,
            onOpenPeriodPicker = onOpenPeriodPicker,
            onOpenSettings = onOpenSettings,
            onOpenAccounts = onOpenAccounts,
            onAdd = onAdd
        )
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp)) {
            BudgetSummary(
                month = month,
                income = state.summary.totalIncomeCents,
                spending = state.summary.totalExpenseCents,
                rows = remember(state.categories, state.budgets, spentByCategory) {
                    budgetRows(state.categories, state.budgets, spentByCategory)
                },
                onCategory = onCategory,
                onManage = onOpenBudgets
            )
            Spacer(Modifier.height(24.dp))
            TopSpending(
                categories = state.categories.filter { !it.isIncomeAdjustment },
                spentByCategory = spentByCategory,
                onCategory = onCategory,
                onOpenInsights = onOpenInsights
            )
            Spacer(Modifier.height(24.dp))
            SpendingHeatmap(
                month = month,
                dailyTotals = state.summary.dailyTotals,
                monthSpend = state.summary.totalExpenseCents,
                onDay = onDay
            )
            Spacer(Modifier.height(24.dp))
            Panel(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                SectionHead(
                    "Recent activity",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp)
                ) { ChipButton("View activity", onClick = onOpenActivity) }
                // A new set of entries (another month, a new entry) crossfades in as the card resizes.
                AnimatedContent(
                    targetState = recent,
                    transitionSpec = {
                        fadeIn(BotMotion.smooth(0.32f))
                            .togetherWith(fadeOut(BotMotion.smooth(0.2f)))
                            .using(SizeTransform(clip = true) { _, _ -> BotMotion.Resize })
                    },
                    contentKey = { rows -> rows.map { it.key } },
                    label = "recentActivity"
                ) { rows ->
                    Column {
                        if (rows.isEmpty()) {
                            EmptyState(
                                title = "No entries this month",
                                body = "Add an expense, income, or transfer to see it here.",
                                action = "Add an entry",
                                onAction = { onAdd(EntryKind.Expense) },
                                onDark = false
                            )
                        } else {
                            rows.forEachIndexed { index, item ->
                                val text = rowText(item, categoriesById, names)
                                TransactionRow(
                                    title = text.title,
                                    meta = text.meta,
                                    cents = item.cents,
                                    kind = item.kind,
                                    art = text.art,
                                    onClick = { onOpenItem(item) },
                                    horizontalPadding = 20.dp,
                                    showDivider = index < rows.lastIndex
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    modifier: Modifier,
    month: YearMonth,
    totalBalance: Long,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAdd: (EntryKind) -> Unit
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val widthDp = LocalConfiguration.current.screenWidthDp
    // clamp(45px, 14vw, 62px)
    val balanceSize = (widthDp * 0.14f).coerceIn(45f, 62f)
    // The painting behind is drawn by HeroPainting, which can stretch.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp + statusTop)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp + statusTop)
                .padding(start = 16.dp, end = 16.dp, top = statusTop + 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodSelector(month = month, onChange = onMonthChange, onOpenPicker = onOpenPeriodPicker)
                Spacer(Modifier.weight(1f))
                CircleIconButton(BotIcons.Menu, "Open settings", onClick = onOpenSettings)
            }
            Column(Modifier.padding(vertical = 16.dp)) {
                Text("Total balance", style = display(27f, FontWeight.Light), color = Color.White)
                Spacer(Modifier.height(5.dp))
                Money(
                    cents = totalBalance,
                    style = moneyStyle(balanceSize, letterSpacingPx = -2f),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    animate = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroAction("Add income", BotIcons.Down, Modifier.weight(1f)) { onAdd(EntryKind.Income) }
                HeroAction("Add expense", BotIcons.Up, Modifier.weight(1f)) { onAdd(EntryKind.Expense) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Bot.Paper)
                        .botPress(onClick = onOpenAccounts)
                        .semantics { contentDescription = "Accounts" },
                    contentAlignment = Alignment.Center
                ) {
                    BotIcon(BotIcons.Wallet, size = 22.dp, tint = Bot.Ink)
                }
            }
        }
    }
}

@Composable
private fun HeroAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    // Single-line label that never clips: when space runs short (narrow
    // phone, large text) the icon goes first, then the label steps down in
    // size until it fits.
    val style = body(15f, FontWeight.Medium, color = Bot.Ink)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val labelWidth = remember(label, style, density) { measurer.measure(label, style).size.width }
        val showIcon = with(density) { labelWidth.toDp() } + (18 + 8 + 32 + 8).dp <= maxWidth
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFFDEDFE2))
                .botPress(onClick = onClick)
                .semantics { contentDescription = label }
                .padding(horizontal = if (showIcon) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                BotIcon(icon, size = 18.dp, tint = Bot.Ink)
                Spacer(Modifier.width(8.dp))
            }
            BasicText(
                text = label,
                style = style,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 11.sp,
                    maxFontSize = style.fontSize,
                    stepSize = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun BudgetSummary(
    month: YearMonth,
    income: Long,
    spending: Long,
    rows: List<BudgetRow>,
    onCategory: (Category) -> Unit,
    onManage: () -> Unit
) {
    Panel(Modifier.animateContentSize(BotMotion.Resize)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "This month",
                    style = display(27f, FontWeight.Medium, letterSpacing = -0.5f, lineHeight = 1.15f),
                    color = Bot.SurfaceInk,
                    modifier = Modifier.semantics { heading() }
                )
                Support(monthLabel(month), Modifier.padding(top = 6.dp))
            }
            ChipButton("Budgets", onClick = onManage)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Income" to income, "Spending" to spending).forEach { (label, cents) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, style = body(14f, FontWeight.Medium), color = Bot.SurfaceInk)
                    Money(
                        cents = cents,
                        style = moneyStyle(25f, letterSpacingPx = -0.6f),
                        color = Bot.SurfaceInk,
                        centsScale = 0.8f,
                        abbreviate = true,
                        modifier = Modifier.fillMaxWidth(),
                        animate = true
                    )
                }
            }
        }
        if (rows.isEmpty()) {
            Support("Set a category budget to track what you have left.")
        } else {
            rows.take(3).forEach { row ->
                val tone = row.tone()
                val color = when (tone) {
                    BudgetTone.Over -> Bot.BudgetOver
                    BudgetTone.Near -> Bot.BudgetNear
                    BudgetTone.Ok -> Bot.BudgetOk
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .botRowPress { onCategory(row.category) }
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.RuleSoft))
                    Column(Modifier.padding(vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.category.name,
                                style = body(14f, FontWeight.SemiBold, lineHeight = 1.5f),
                                color = Bot.SurfaceInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "RM ${formatAmount(row.spent)} / ${formatAmount(row.limit)}",
                                style = body(13f, lineHeight = 1.5f),
                                color = Bot.SurfaceInk,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Track(row.ratio, color, Modifier.padding(vertical = 9.dp))
                        Text(row.statusText(), style = body(13f, FontWeight.Medium, lineHeight = 1.5f), color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSpending(
    categories: List<Category>,
    spentByCategory: Map<Long, Long>,
    onCategory: (Category) -> Unit,
    onOpenInsights: () -> Unit
) {
    val top = remember(categories, spentByCategory) {
        categories.sortedByDescending { spentByCategory[it.id] ?: 0L }.take(5)
    }
    Panel {
        SectionHead("Top spending", modifier = Modifier.padding(bottom = 14.dp)) {
            ChipButton("View insights", onClick = onOpenInsights)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Tiles share the width, never narrower than 76dp; past that the row scrolls.
            val gap = 12.dp
            val count = top.size.coerceAtLeast(1)
            val tileWidth = max(76f, ((maxWidth - gap * (count - 1)) / count).value).dp
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                top.forEach { category ->
                    // Keyed, so when another month reorders the ranking the tiles glide to their places.
                    key(category.id) {
                        Column(
                            modifier = Modifier
                                .animatePlacement(BotMotion.smooth(0.45f, IntOffset.VisibilityThreshold))
                                .width(tileWidth)
                                .heightIn(min = 90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .botPress { onCategory(category) }
                                .semantics { contentDescription = "${category.name}, ${formatRm(spentByCategory[category.id] ?: 0L)}" }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ArtImage(category.art.drawable, size = 54.dp)
                            Text(
                                category.name,
                                style = display(17f),
                                color = Bot.SurfaceInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Money(
                                cents = spentByCategory[category.id] ?: 0L,
                                style = body(13f, FontWeight.Medium, lineHeight = 1.4f),
                                color = Bot.SurfaceMuted,
                                centsScale = 1f,
                                currencyWeight = FontWeight.Medium,
                                abbreviate = true,
                                align = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                                animate = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingHeatmap(
    month: YearMonth,
    dailyTotals: Map<Int, Long>,
    monthSpend: Long,
    onDay: (LocalDate) -> Unit
) {
    val today = todayKl()
    val maxDay = max(dailyTotals.values.maxOrNull() ?: 0L, 1L)
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Spending heatmap",
                style = display(23f, FontWeight.Medium, letterSpacing = -0.5f),
                color = Bot.SurfaceInk,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.semantics { contentDescription = "Low to high spending" }
            ) {
                Bot.Heat.forEach { color ->
                    Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            "SMTWTFS".forEach { letter ->
                Text(
                    letter.toString(),
                    style = body(11f, FontWeight.Medium),
                    color = Color(0xFF737989),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(bottom = 6.dp)
                )
            }
        }
        // Another month's pattern washes in cell by cell; a sixth week unfolds or folds away.
        Column(
            modifier = Modifier.animateContentSize(BotMotion.Resize),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            calendarCells(month).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f)) {
                            if (date != null) {
                                val total = dailyTotals[date.dayOfMonth] ?: 0L
                                val index = if (total > 0) max(1, ceil(total.toDouble() / maxDay * 4).toInt()) else 0
                                val future = date > today
                                val heat = animatedColor(
                                    if (total > 0) Bot.Heat[index.coerceAtMost(4)] else Color(0xFFF1F0F3),
                                    "heat"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (future) 0.4f else 1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(heat)
                                        .botPress(enabled = !future) { onDay(date) }
                                        .semantics {
                                            contentDescription = "${date.dayOfMonth} ${monthLabel(month)}: ${formatRm(total)} spent"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        date.dayOfMonth.toString(),
                                        style = body(11f, FontWeight.Medium),
                                        color = if (index >= 3) Color.White else Color(0xFF555555)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "${formatRm(monthSpend)} spent this month. Select a day to view entries.",
            style = body(11f, lineHeight = 1.5f),
            color = Color(0xFF778090),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
