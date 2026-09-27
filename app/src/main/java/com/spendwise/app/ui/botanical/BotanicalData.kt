package com.spendwise.app.ui.botanical

import androidx.annotation.DrawableRes
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MonthlyAggregate
import com.spendwise.app.domain.Transfer
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * One line in a ledger list. Expenses, income entries and transfers live in
 * different tables; the screens list them together, newest first.
 */
sealed interface LedgerItem {
    val key: String
    val date: LocalDate
    val createdAtMillis: Long
    val cents: Long
    val kind: EntryKind

    data class Entry(val expense: Expense, val isIncome: Boolean) : LedgerItem {
        override val key get() = "e${expense.id}"
        override val date: LocalDate = expense.occurredAtMillis.toKlDate()
        override val createdAtMillis get() = expense.createdAtMillis
        override val cents get() = expense.amountCents
        override val kind get() = if (isIncome) EntryKind.Income else EntryKind.Expense
    }

    data class Move(val transfer: Transfer) : LedgerItem {
        override val key get() = "t${transfer.id}"
        override val date: LocalDate = transfer.occurredAtMillis.toKlDate()
        override val createdAtMillis get() = transfer.createdAtMillis
        override val cents get() = transfer.amountCents
        override val kind get() = EntryKind.Transfer
    }
}

/** Newest date first; entries made later on the same day first. */
val LedgerOrder: Comparator<LedgerItem> =
    compareByDescending<LedgerItem> { it.date }.thenByDescending { it.createdAtMillis }

fun ledgerItems(
    expenses: List<Expense>,
    transfers: List<Transfer>,
    incomeIds: Set<Long>
): List<LedgerItem> =
    (expenses.map { LedgerItem.Entry(it, it.categoryId in incomeIds) } + transfers.map { LedgerItem.Move(it) })
        .sortedWith(LedgerOrder)

fun incomeCategoryIds(categories: List<Category>): Set<Long> =
    categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()

/** Name shown for a transfer, which stores only notes. */
fun transferTitle(transfer: Transfer): String =
    transfer.notes.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "Account transfer" }

/** Title, context line and art for a ledger row. */
data class RowText(val title: String, val meta: String, @param:DrawableRes val art: Int?)

fun rowText(item: LedgerItem, categoriesById: Map<Long, Category>, accountNames: Map<Long, String>): RowText =
    when (item) {
        is LedgerItem.Entry -> {
            val e = item.expense
            val category = categoriesById[e.categoryId]
            val categoryName = category?.name ?: e.categoryName
            RowText(
                title = e.merchant.ifBlank { categoryName },
                meta = "$categoryName · ${accountNames[e.accountId] ?: "Account"}",
                art = (category?.art ?: categoryArtFor(e.categoryName, e.categoryIconName, item.isIncome)).drawable
            )
        }
        is LedgerItem.Move -> RowText(
            title = transferTitle(item.transfer),
            meta = "${item.transfer.fromAccountName} → ${item.transfer.toAccountName}",
            art = null
        )
    }

/** Income minus expenses; transfers move money between the user's own accounts and never count. */
data class Totals(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

fun totalsOf(items: List<LedgerItem>): Totals = Totals(
    income = items.filter { it.kind == EntryKind.Income }.sumOf { it.cents },
    expense = items.filter { it.kind == EntryKind.Expense }.sumOf { it.cents }
)

// ── Budgets ──────────────────────────────────────────────────────────────────

data class BudgetRow(val category: Category, val spent: Long, val limit: Long) {
    val ratio: Float get() = if (limit > 0) spent.toFloat() / limit else 0f
}

/** Categories with a monthly budget, most-used first (the Home "This month" card shows three). */
fun budgetRows(
    categories: List<Category>,
    budgets: List<Budget>,
    spentByCategory: Map<Long, Long>
): List<BudgetRow> {
    val limits = budgets.associate { it.categoryId to it.monthlyLimitCents }
    return categories
        .filter { !it.isIncomeAdjustment && (limits[it.id] ?: 0L) > 0L }
        .map { BudgetRow(it, spentByCategory[it.id] ?: 0L, limits.getValue(it.id)) }
        .sortedByDescending { it.ratio }
}

enum class BudgetTone { Ok, Near, Over }

fun BudgetRow.tone(): BudgetTone = when {
    ratio >= 1f -> BudgetTone.Over
    ratio >= 0.8f -> BudgetTone.Near
    else -> BudgetTone.Ok
}

fun BudgetRow.statusText(): String = when {
    spent > limit -> "RM ${formatAmount(spent - limit)} over budget"
    spent == limit -> "Budget reached · RM 0.00 left"
    ratio >= 0.8f -> "Near limit · RM ${formatAmount(limit - spent)} left"
    else -> "RM ${formatAmount(limit - spent)} left"
}

// ── Trends ───────────────────────────────────────────────────────────────────

/** Six monthly totals ending with [month], and the change against last month. */
data class MonthComparison(
    val series: List<Long>,
    val current: Long,
    val previous: Long,
    val previousMonth: YearMonth
) {
    /** Whole-percent change, or null when last month had nothing to compare. */
    val percent: Int? get() = if (previous == 0L) null else ((current - previous) * 100.0 / previous).roundToInt()
}

fun monthComparison(totalFor: (YearMonth) -> Long, month: YearMonth): MonthComparison {
    val series = (5 downTo 0).map { totalFor(month.minusMonths(it.toLong())) }
    return MonthComparison(series, series[5], series[4], month.minusMonths(1))
}

fun aggregateTotals(aggregates: List<MonthlyAggregate>): Map<YearMonth, Totals> =
    aggregates.associate { it.month to Totals(it.incomeCents, it.expenseCents) }

/** Year totals summed from monthly aggregates. */
fun yearTotals(aggregates: List<MonthlyAggregate>, year: Int): Totals =
    aggregates.filter { it.month.year == year }.let { rows ->
        Totals(rows.sumOf { it.incomeCents }, rows.sumOf { it.expenseCents })
    }

// ── Activity filters ─────────────────────────────────────────────────────────

enum class ActivityKind(val label: String) { All("All"), Expense("Expense"), Income("Income"), Transfer("Transfer") }

/** What the Activity list is narrowed by. Null account/category mean "all". */
data class ActivityQuery(
    val kind: ActivityKind = ActivityKind.All,
    val search: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null
)

/**
 * The prototype's `matchesQuery`, minus dates (the caller already loaded the
 * month or range). Search covers name, notes and category; a transfer
 * matches an account filter from either side and never matches a category.
 */
fun matches(item: LedgerItem, query: ActivityQuery, categoriesById: Map<Long, Category>): Boolean {
    val kindOk = when (query.kind) {
        ActivityKind.All -> true
        ActivityKind.Expense -> item.kind == EntryKind.Expense
        ActivityKind.Income -> item.kind == EntryKind.Income
        ActivityKind.Transfer -> item.kind == EntryKind.Transfer
    }
    if (!kindOk) return false
    val needle = query.search.trim().lowercase()
    return when (item) {
        is LedgerItem.Entry -> {
            val e = item.expense
            (query.accountId == null || e.accountId == query.accountId) &&
                (query.categoryId == null || e.categoryId == query.categoryId) &&
                (needle.isEmpty() ||
                    "${e.merchant} ${e.notes} ${categoriesById[e.categoryId]?.name ?: e.categoryName}"
                        .lowercase().contains(needle))
        }
        is LedgerItem.Move -> {
            val t = item.transfer
            query.categoryId == null &&
                (query.accountId == null || t.fromAccountId == query.accountId || t.toAccountId == query.accountId) &&
                (needle.isEmpty() ||
                    "${transferTitle(t)} ${t.notes} ${t.fromAccountName} ${t.toAccountName} transfer"
                        .lowercase().contains(needle))
        }
    }
}

fun accountNames(accounts: List<Account>): Map<Long, String> = accounts.associate { it.id to it.name }
