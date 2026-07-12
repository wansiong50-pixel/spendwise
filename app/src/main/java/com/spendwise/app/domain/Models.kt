package com.spendwise.app.domain

data class Category(
    val id: Long,
    val name: String,
    val color: Long,
    val iconName: String = "",
    val isIncomeAdjustment: Boolean = false,
    val isCustom: Boolean = false
)

data class Expense(
    val id: Long,
    val amountCents: Long,
    val categoryId: Long,
    val categoryName: String,
    val accountId: Long,
    val merchant: String,
    val notes: String,
    val occurredAtMillis: Long,
    val createdAtMillis: Long,
    val categoryIconName: String = "",
    val categoryColor: Long? = null
)

/**
 * Discriminator for [Account] surfaces. Lives as a string in the DB so a new
 * type can be added without a schema migration; the enum is just the closed
 * set the UI knows how to render. Anything stored that doesn't match falls
 * back to [Cash] in the mapper.
 */
enum class AccountType(val storageKey: String, val displayLabel: String) {
    Cash("CASH", "Cash"),
    Bank("BANK", "Bank"),
    EWallet("EWALLET", "E-wallet"),
    Credit("CREDIT", "Credit");

    companion object {
        fun fromStorageKey(key: String): AccountType =
            entries.firstOrNull { it.storageKey == key } ?: Cash
    }
}

/**
 * A user-owned money source. [currentBalanceCents] is derived
 * (`startingBalance + Σ income − Σ expense bound to this account`); it is NOT
 * stored. Recomputed by the repository on every transaction/account change so
 * edits and deletes never desync the balance from the underlying ledger.
 */
data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val startingBalanceCents: Long,
    val currentBalanceCents: Long,
    val color: Long,
    val iconName: String,
    val sortOrder: Int,
    val isArchived: Boolean
)

data class CategoryTotal(
    val categoryId: Long,
    val categoryName: String,
    val totalCents: Long,
    val transactionCount: Int,
    val categoryIconName: String = "",
    val categoryColor: Long? = null
)

data class Budget(
    val id: Long,
    val categoryId: Long,
    val monthlyLimitCents: Long
)

/**
 * One calendar month's expense/income totals, aggregated in SQL. A row per
 * month that has data — the whole table collapses to a few dozen of these, so
 * pickers, trend sparklines, and "has data" checks never need the raw rows.
 */
data class MonthlyAggregate(
    val month: java.time.YearMonth,
    val expenseCents: Long,
    val incomeCents: Long
)

/**
 * Per-category entry count + amount total over some period (SQL GROUP BY).
 * Powers the Categories screen's "this month" chips and the category form's
 * usage line without materializing the period's expense rows.
 */
data class CategoryPeriodStats(
    val categoryId: Long,
    val entryCount: Int,
    val totalCents: Long
)

/** Non-income entry count + spend total for an arbitrary date range. */
data class RangeStats(
    val entryCount: Int,
    val spendCents: Long
)

/**
 * Money moved between two of the user's own accounts. Excluded from every
 * spending/income statistic by construction — only account balances and the
 * activity timeline see it. [fromAccountName]/[toAccountName] are joined in
 * by the repository for display, mirroring [Expense.categoryName].
 */
data class Transfer(
    val id: Long,
    val fromAccountId: Long,
    val fromAccountName: String,
    val toAccountId: Long,
    val toAccountName: String,
    val amountCents: Long,
    val notes: String,
    val occurredAtMillis: Long,
    val createdAtMillis: Long
)

/**
 * How often a recurring rule fires. Stored as a string key (same pattern as
 * [AccountType]) so adding a cadence never needs a schema migration; unknown
 * stored values fall back to [Monthly] in the mapper.
 */
enum class RecurrenceCadence(val storageKey: String, val displayLabel: String) {
    Weekly("WEEKLY", "Weekly"),
    Monthly("MONTHLY", "Monthly"),
    Yearly("YEARLY", "Yearly");

    companion object {
        fun fromStorageKey(key: String): RecurrenceCadence =
            entries.firstOrNull { it.storageKey == key } ?: Monthly
    }
}

/**
 * A recurring transaction template (rent, subscription, salary). Real
 * [Expense] rows are materialized from it on app launch whenever
 * [nextDueEpochDay] (KL-calendar epoch day) has passed.
 *
 * [anchorEpochDay] is the user-picked first occurrence and never moves — it
 * keeps a month-end rule on the intended day across short months.
 * [categoryName]/[categoryColor]/[categoryIconName] are joined in by the
 * repository for display, mirroring [Expense].
 */
data class RecurringRule(
    val id: Long,
    val amountCents: Long,
    val categoryId: Long,
    val categoryName: String,
    val accountId: Long,
    val merchant: String,
    val notes: String,
    val cadence: RecurrenceCadence,
    val anchorEpochDay: Long,
    val nextDueEpochDay: Long,
    val isPaused: Boolean,
    val createdAtMillis: Long,
    val categoryIconName: String = "",
    val categoryColor: Long? = null,
    val isIncome: Boolean = false
)
