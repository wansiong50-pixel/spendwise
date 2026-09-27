package com.spendwise.app.ui.botanical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.ui.DateRange
import com.spendwise.app.ui.EntryWindow
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(
    month: YearMonth,
    items: List<LedgerItem>,
    categories: List<Category>,
    accounts: List<Account>,
    query: ActivityQuery,
    range: DateRange?,
    trendExpenses: List<Expense>,
    bottomPadding: Dp,
    onQueryChange: (ActivityQuery) -> Unit,
    onClearRange: () -> Unit,
    onClearAll: () -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onAdd: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenItem: (LedgerItem) -> Unit
) {
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val names = remember(accounts) { accountNames(accounts) }
    val shown = remember(items, query, categoriesById) { items.filter { matches(it, query, categoriesById) } }
    val summary = remember(shown) { totalsOf(shown) }
    val figure = when (query.kind) {
        ActivityKind.Expense -> summary.expense
        ActivityKind.Income -> summary.income
        ActivityKind.Transfer -> shown.sumOf { it.cents }
        ActivityKind.All -> summary.net
    }
    val days = remember(shown) { shown.groupBy { it.date }.toList() }
    val filterCount = listOf(query.accountId != null, query.categoryId != null, range != null).count { it }
    val narrowed = query.search.isNotBlank() || query.kind != ActivityKind.All || filterCount > 0
    var aboutOpen by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val figureSize = ((widthDp - 32) * 0.146f).coerceIn(44f, 58f)

    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = "Activity",
                    month = month,
                    onMonthChange = onMonthChange,
                    onOpenPicker = onOpenPeriodPicker
                ) {
                    CircleIconButton(BotIcons.Plus, "Add entry", onClick = onAdd)
                }
            }
            item(key = "figure") {
                Column {
                    Row(
                        modifier = Modifier.padding(top = 15.dp, bottom = 8.dp).heightIn(min = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            when (query.kind) {
                                ActivityKind.Expense -> "Total spending"
                                ActivityKind.Income -> "Total income"
                                ActivityKind.Transfer -> "Money moved"
                                ActivityKind.All -> "Net cash flow"
                            },
                            style = body(14f, FontWeight.Medium, lineHeight = 1.4f),
                            color = Bot.PageMuted
                        )
                        if (query.kind == ActivityKind.All) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .botPress { aboutOpen = !aboutOpen }
                                    .semantics { contentDescription = "About net cash flow" },
                                contentAlignment = Alignment.Center
                            ) {
                                BotIcon(BotIcons.Info, size = 16.dp, tint = Bot.PageMuted)
                            }
                        }
                    }
                    AnimatedVisibility(visible = aboutOpen && query.kind == ActivityKind.All) {
                        Text(
                            "Net cash flow is income minus expenses. Transfers between your accounts are excluded.",
                            style = body(13f, lineHeight = 1.6f),
                            color = Bot.SecondaryInk,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFDCE1EA), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                    Money(
                        cents = figure,
                        style = moneyStyle(figureSize, letterSpacingPx = -2f),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item(key = "tools") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchField(
                        value = query.search,
                        onValueChange = { onQueryChange(query.copy(search = it)) },
                        onDone = { focus.clearFocus() },
                        modifier = Modifier.weight(1f)
                    )
                    FilterButton(count = filterCount, onClick = onOpenFilters)
                }
            }
            item(key = "tabs") {
                TabSegment(
                    options = ActivityKind.entries,
                    selected = query.kind,
                    onSelect = { onQueryChange(query.copy(kind = it)) },
                    label = { it.label },
                    fontSize = 13f,
                    modifier = Modifier.padding(top = 15.dp)
                )
            }
            if (filterCount > 0) {
                item(key = "chips") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        query.accountId?.let { id ->
                            FilterChip(names[id] ?: "Account") { onQueryChange(query.copy(accountId = null)) }
                        }
                        query.categoryId?.let { id ->
                            FilterChip(categoriesById[id]?.name ?: "Category") { onQueryChange(query.copy(categoryId = null)) }
                        }
                        range?.let { r ->
                            FilterChip("${shortDateLabel(r.from)} – ${shortDateLabel(r.to)}", onRemove = onClearRange)
                        }
                        if (filterCount > 1) {
                            Box(
                                Modifier
                                    .heightIn(min = 44.dp)
                                    .botPress {
                                        onQueryChange(query.copy(accountId = null, categoryId = null))
                                        onClearRange()
                                    }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Clear all",
                                    style = body(13f, FontWeight.Medium).copy(textDecoration = TextDecoration.Underline),
                                    color = Bot.PageMuted
                                )
                            }
                        }
                    }
                }
            }
            if (range == null && (query.kind == ActivityKind.Expense || query.kind == ActivityKind.Income)) {
                item(key = "trend-${query.kind}") {
                    val incomeIds = remember(categories) { incomeCategoryIds(categories) }
                    val comparison = remember(trendExpenses, query, categoriesById, month) {
                        val matching = trendExpenses
                            .map { LedgerItem.Entry(it, it.categoryId in incomeIds) }
                            .filter { matches(it, query, categoriesById) }
                            .groupBy { YearMonth.from(it.date) }
                            .mapValues { (_, rows) -> rows.sumOf { it.cents } }
                        monthComparison({ matching[it] ?: 0L }, month)
                    }
                    MonthlyComparisonPanel(
                        comparison = comparison,
                        month = month,
                        income = query.kind == ActivityKind.Income,
                        filtered = filterCount > 0 || query.search.isNotBlank(),
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = if (query.search.isNotBlank()) "No entries matching “${query.search.trim()}”" else "No entries in this period",
                        body = if (narrowed) "Clear filters to show all entries for the selected month." else "Add an entry or choose another month.",
                        action = if (narrowed) "Clear filters" else "Add entry",
                        onAction = if (narrowed) onClearAll else onAdd
                    )
                }
            } else {
                days.forEach { (date, dayItems) ->
                    item(key = "day-$date") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                dayHeading(date),
                                style = body(13f),
                                color = Color(0xFFBBC2D1),
                                modifier = Modifier.weight(1f).semantics { heading() }
                            )
                            Money(
                                cents = totalsOf(dayItems).net,
                                style = body(12f, FontWeight.Medium),
                                color = Color(0xFFBBC2D1),
                                centsScale = 1f,
                                currencyWeight = FontWeight.Medium
                            )
                        }
                    }
                    item(key = "list-$date") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.White)
                        ) {
                            dayItems.forEachIndexed { index, item ->
                                val text = rowText(item, categoriesById, names)
                                TransactionRow(
                                    title = text.title,
                                    meta = text.meta,
                                    cents = item.cents,
                                    kind = item.kind,
                                    art = text.art,
                                    onClick = { onOpenItem(item) },
                                    showDivider = index < dayItems.lastIndex
                                )
                            }
                        }
                    }
                }
                item(key = "count") {
                    Note(
                        "${shown.size} ${if (shown.size == 1) "entry" else "entries"}",
                        modifier = Modifier.padding(top = 25.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(999.dp))
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BotIcon(BotIcons.Search, size = 18.dp, tint = Color.White)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("Search entries", style = body(16f), color = Color(0xFFA7B1C8), maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = body(16f, color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search entries" }
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(36.dp)
                    .botPress { onValueChange("") }
                    .semantics { contentDescription = "Clear search" },
                contentAlignment = Alignment.Center
            ) {
                BotIcon(BotIcons.Close, size = 16.dp, tint = Color.White)
            }
        }
    }
}

@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    val active = count > 0
    Box(Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (active) Color.White else Color(0x12FFFFFF))
                .border(1.dp, if (active) Color.White else Color(0x25FFFFFF), CircleShape)
                .botPress(onClick = onClick)
                .semantics { contentDescription = if (active) "Filters, $count active" else "Filters" },
            contentAlignment = Alignment.Center
        ) {
            BotIcon(BotIcons.Filter, size = 20.dp, tint = if (active) Bot.Navy else Color.White)
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B1428))
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Bot.Orchid),
                contentAlignment = Alignment.Center
            ) {
                Text(count.toString(), style = body(11f, FontWeight.Bold), color = Bot.Navy)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0x1FFFFFFF))
            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(99.dp))
            .botPress(onClick = onRemove)
            .semantics { contentDescription = "Remove filter: $label" }
            .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            label,
            style = body(13f, FontWeight.Medium, lineHeight = 1.3f),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp)
        )
        BotIcon(BotIcons.Close, size = 14.dp, tint = Color.White.copy(alpha = 0.75f))
    }
}

/** Six months as quiet bars: this month carries the accent, last month (the comparison) sits half-lit. */
@Composable
fun MonthlyComparisonPanel(
    comparison: MonthComparison,
    month: YearMonth,
    income: Boolean,
    filtered: Boolean,
    modifier: Modifier = Modifier
) {
    val percent = comparison.percent
    val change = when {
        percent == null -> if (comparison.current > 0) "No entries last month to compare." else "No entries in either month."
        percent == 0 -> "Unchanged from last month."
        else -> "${abs(percent)}% ${if (percent > 0) "more" else "less"} than last month."
    }
    val toneColor = when {
        percent == null || percent == 0 -> Bot.SurfaceInk
        (if (income) percent > 0 else percent < 0) -> Bot.TrendGood
        else -> Bot.TrendCaution
    }
    val trend = if (income) Bot.IncomeBar else Bot.ExpenseBar
    val maximum = maxOf(comparison.series.maxOrNull() ?: 0L, 1L)
    Panel(modifier) {
        SectionHead(if (income) "Income trend" else "Spending trend", modifier = Modifier.padding(bottom = 10.dp)) {
            Text("Last 6 months", style = body(13f, FontWeight.Medium, lineHeight = 1.4f), color = Bot.ChartLabel)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (percent != null && percent != 0) {
                BotIcon(if (percent > 0) BotIcons.Up else BotIcons.Down, size = 14.dp, tint = toneColor)
            }
            Text(change, style = body(16f, FontWeight.SemiBold, lineHeight = 1.35f), color = toneColor)
        }
        Text(
            "${monthLabel(comparison.previousMonth)} · ${formatRm(comparison.previous)}",
            style = body(13f, lineHeight = 1.5f),
            color = Bot.SurfaceMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (comparison.series.any { it > 0 }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(112.dp)
                    .semantics {
                        contentDescription = "Six monthly totals ending ${monthLabel(month)}: " +
                            comparison.series.joinToString(", ") { formatRm(it) }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comparison.series.forEachIndexed { index, value ->
                    val m = month.minusMonths((5 - index).toLong())
                    val current = index == 5
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            val fraction = (value.toFloat() / maximum).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .widthIn(max = 28.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .heightIn(min = 3.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(
                                        if (value == 0L) Color(0xFFDFE3EB)
                                        else trend.copy(alpha = if (current) 1f else if (index == 4) 0.5f else 0.22f)
                                    )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            monthShort(m.monthValue),
                            style = body(12f, if (current) FontWeight.Bold else FontWeight.Medium),
                            color = if (current) Bot.Navy else Bot.ChartLabel
                        )
                    }
                }
            }
        }
        if (filtered) {
            Text(
                "Current filters apply",
                style = body(12f, lineHeight = 1.5f),
                color = Bot.SurfaceMuted,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

/**
 * Account, category and date filters (`FilterSheet`). Edits are drafts until
 * "Show N entries"; the count is live, including for a custom range that
 * isn't loaded yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    visible: Boolean,
    month: YearMonth,
    query: ActivityQuery,
    range: DateRange?,
    accounts: List<Account>,
    archivedAccounts: List<Account>,
    categories: List<Category>,
    monthItems: List<LedgerItem>,
    loadEntries: suspend (LocalDate, LocalDate) -> EntryWindow,
    onApply: (accountId: Long?, categoryId: Long?, range: DateRange?) -> Unit,
    onDismiss: () -> Unit
) {
    var accountId by remember(visible) { mutableStateOf(query.accountId) }
    var categoryId by remember(visible) { mutableStateOf(query.categoryId) }
    var useRange by remember(visible) { mutableStateOf(range != null) }
    var from by remember(visible) { mutableStateOf(range?.from ?: month.atDay(1)) }
    var to by remember(visible) { mutableStateOf(range?.to ?: minOf(month.atEndOfMonth(), todayKl())) }
    var picking by remember(visible) { mutableStateOf<String?>(null) }
    var rangeItems by remember(visible) { mutableStateOf<List<LedgerItem>?>(null) }
    val incomeIds = remember(categories) { incomeCategoryIds(categories) }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val invalid = useRange && from > to

    LaunchedEffect(visible, useRange, from, to) {
        if (!visible || !useRange || from > to) return@LaunchedEffect
        delay(150)
        val window = loadEntries(from, to)
        rangeItems = ledgerItems(window.expenses, window.transfers, incomeIds)
    }
    val draft = query.copy(accountId = accountId, categoryId = categoryId)
    val count = when {
        invalid -> 0
        useRange -> rangeItems?.count { matches(it, draft, categoriesById) }
        else -> monthItems.count { matches(it, draft, categoriesById) }
    }
    val active = listOf(accountId != null, categoryId != null, useRange).count { it }

    Box(Modifier.fillMaxSize()) {
        BotSheet(
            visible = visible,
            onDismiss = onDismiss,
            title = "Filters",
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BotButton(
                        "Reset",
                        onClick = {
                            accountId = null
                            categoryId = null
                            useRange = false
                        },
                        type = ButtonType.Secondary,
                        enabled = active > 0,
                        dmSans = true
                    )
                    BotButton(
                        when {
                            invalid -> "Check dates"
                            count == null -> "Counting…"
                            count == 1 -> "Show 1 entry"
                            else -> "Show $count entries"
                        },
                        onClick = { onApply(accountId, categoryId, if (useRange) DateRange(from, to) else null) },
                        enabled = !invalid,
                        dmSans = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        ) {
            FilterGroup("Account", first = true) {
                FilterOption("All accounts", accountId == null) { accountId = null }
                (accounts + archivedAccounts).forEach { account ->
                    FilterOption(
                        if (account.isArchived) "${account.name} (archived)" else account.name,
                        accountId == account.id
                    ) { accountId = account.id }
                }
            }
            FilterGroup("Category") {
                FilterOption("All categories", categoryId == null) { categoryId = null }
                categories.forEach { category ->
                    FilterOption(category.name, categoryId == category.id) { categoryId = category.id }
                }
            }
            FilterGroup("Date") {
                FilterOption(monthLabel(month), !useRange) { useRange = false }
                FilterOption("Custom range", useRange) { useRange = true }
            }
            if (useRange) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                    DateField("From", from, Modifier.weight(1f)) { picking = "from" }
                    DateField("To", to, Modifier.weight(1f)) { picking = "to" }
                }
            }
            if (invalid) {
                ErrorBox("Choose an end date on or after the start date.", Modifier.padding(top = 16.dp))
            }
        }
        CalendarPicker(
            visible = visible && picking != null,
            mode = CalendarMode.Date,
            initial = if (picking == "to") to else from,
            onCancel = { picking = null },
            onConfirm = { date ->
                if (picking == "to") to = date else from = date
                picking = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, first: Boolean = false, content: @Composable () -> Unit) {
    Text(
        title,
        style = body(14f, FontWeight.Medium),
        color = Bot.SurfaceMuted,
        modifier = Modifier
            .padding(top = if (first) 0.dp else 20.dp, bottom = 10.dp)
            .semantics { heading() }
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Bot.ActionSolid else Color(0xFFF0F2F6))
            .botPress(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = body(14f, FontWeight.Medium, lineHeight = 1.3f),
            color = if (selected) Color.White else Bot.SecondaryInk,
            textAlign = TextAlign.Center
        )
    }
}

/** A date shown as a field; tapping opens the illustrated calendar. */
@Composable
fun DateField(label: String, date: LocalDate, modifier: Modifier = Modifier, isError: Boolean = false, onClick: () -> Unit) {
    Column(modifier) {
        FieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 49.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Bot.FieldBg)
                .border(1.dp, if (isError) Color(0xFFD9788C) else Bot.FieldBorder, RoundedCornerShape(17.dp))
                .botPress(scale = 0.98f, onClick = onClick)
                .semantics { contentDescription = "$label: ${dateLabel(date)}, change date" }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dateLabel(date), style = body(14f), color = Bot.FieldInk, modifier = Modifier.weight(1f), maxLines = 1)
            BotIcon(BotIcons.Calendar, size = 18.dp, tint = Bot.FieldInk)
        }
    }
}

