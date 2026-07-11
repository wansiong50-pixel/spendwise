package com.spendwise.app.data

import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryPeriodStats
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.MonthlyAggregate
import com.spendwise.app.domain.RangeStats
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurringRule
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Strategy for deleting a custom category that still has expenses pointing to
 * it. The category row can't be removed while expense rows reference it
 * (FK is RESTRICT), so the caller must declare what to do with those expenses
 * first.
 */
sealed interface CategoryDeletion {
    /** Move every expense from the source category to [destinationId] first. */
    data class Migrate(val destinationId: Long) : CategoryDeletion
    /** Delete every expense in this category along with the category itself. */
    data object DeleteExpenses : CategoryDeletion
}

/**
 * Result of attempting to archive an account.
 *  - [Archived] — gone from active surfaces, no further action needed.
 *  - [Blocked] — the account still has transactions; UI must reassign them to
 *    another account before archive is allowed. (Phase 2: surfaces as a
 *    snackbar; Phase 3+ will offer an in-flow reassign dialog mirroring the
 *    category deletion strategy.)
 */
sealed interface ArchiveAccountResult {
    data object Archived : ArchiveAccountResult
    data class Blocked(
        val transactionCount: Int,
        // Recurring rules also anchor to an account; archiving out from
        // under one would keep logging money into a surface hidden from the
        // dashboard total.
        val recurringRuleCount: Int = 0
    ) : ArchiveAccountResult
}

interface ExpenseRepository {
    /**
     * Expenses whose occurrence falls in `[startMillis, endMillis)`. Every
     * screen is month- or year-scoped, so callers observe only the window they
     * render — the full ledger is never materialized in memory.
     */
    fun expensesInRange(startMillis: Long, endMillis: Long): Flow<List<Expense>>

    /**
     * The most recently *entered* expenses (creation order), bounded to a few
     * hundred rows. Backs merchant suggestions and canonicalization, which
     * rank by entry recency and never benefit from older rows.
     */
    val recentExpenses: Flow<List<Expense>>

    /**
     * Per-month expense/income totals for the entire ledger, one row per month
     * with data — SQL-aggregated, so it stays a few dozen tiny objects no
     * matter how large the table grows. Feeds pickers, trends, and
     * has-data checks.
     */
    val monthlyAggregates: Flow<List<MonthlyAggregate>>

    /** Entry count per calendar day (KL time) — date-picker dots/counters. */
    val dayEntryCounts: Flow<Map<LocalDate, Int>>

    /** All-time entry count per category — guards category deletion. */
    val categoryEntryCounts: Flow<Map<Long, Int>>

    /** Per-category entry count + total inside `[startMillis, endMillis)`. */
    fun categoryStatsInRange(startMillis: Long, endMillis: Long): Flow<List<CategoryPeriodStats>>

    /** Non-income count + spend total inside `[startMillis, endMillis)`. */
    suspend fun rangeStats(startMillis: Long, endMillis: Long): RangeStats

    /**
     * History strip for the transaction-detail sheet: rows sharing [merchant]
     * (case-insensitive), most recent first, bounded by [limit].
     */
    suspend fun expensesByMerchant(merchant: String, limit: Int): List<Expense>

    /** History strip variant for income rows: same category, most recent first. */
    suspend fun expensesByCategory(categoryId: Long, limit: Int): List<Expense>

    val categories: Flow<List<Category>>
    val budgets: Flow<List<Budget>>

    /**
     * Active (non-archived) accounts with their derived current balance,
     * recomputed whenever an account row or any expense changes.
     */
    val accounts: Flow<List<Account>>

    /**
     * Archived accounts. Sibling to [accounts] — same shape, derived current
     * balance still computed so the UI can show "what this account held when
     * it was last active." Used by the Accounts screen's collapsible
     * Archived section so users can restore an accidentally archived
     * account without recreating it from scratch.
     */
    val archivedAccounts: Flow<List<Account>>

    suspend fun seedDefaultCategories()

    /**
     * Ensure at least one account exists. Mirrors the fresh-install path of
     * the v5→v6 migration: that migration seeds a default Wallet, but Room
     * runs CREATE TABLE (not migrations) on first install, so fresh DBs need
     * this hook. No-op if any account row already exists.
     */
    suspend fun seedDefaultAccount()

    suspend fun saveExpense(
        id: Long?,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        merchant: String,
        notes: String,
        occurredAtMillis: Long
    )

    suspend fun createCategory(name: String, color: Long, iconName: String, isIncome: Boolean): Long

    suspend fun updateCustomCategory(
        id: Long,
        name: String,
        color: Long,
        iconName: String,
        isIncome: Boolean
    ): Boolean

    /**
     * Delete a custom category. Succeeds if zero expenses reference it.
     * Returns false if it's a built-in, doesn't exist, or still has expenses
     * — for the last case, call [deleteCustomCategory] with a strategy.
     */
    suspend fun deleteCustomCategory(id: Long): Boolean

    /**
     * Delete a custom category that still has expenses, applying [strategy]
     * (reassign to another category, or wipe the expenses along with it) in a
     * single atomic transaction.
     */
    suspend fun deleteCustomCategory(id: Long, strategy: CategoryDeletion): Boolean

    suspend fun deleteExpense(id: Long)

    /**
     * Create a new account. Returns the new account id on success, or a
     * non-null error message if validation failed (duplicate name, blank, etc.).
     */
    suspend fun createAccount(
        name: String,
        type: AccountType,
        startingBalanceCents: Long,
        iconName: String,
        color: Long
    ): Long

    /**
     * Update mutable fields on an existing account. Starting balance is
     * editable — changing it shifts every historical current-balance reading
     * because balance is computed, never stored.
     */
    suspend fun updateAccount(
        id: Long,
        name: String,
        type: AccountType,
        startingBalanceCents: Long,
        iconName: String,
        color: Long
    )

    /**
     * Archive an account if no transactions point at it. The FK on expenses
     * is RESTRICT, so we check first and report the count back to the UI
     * rather than letting Room throw an FK violation at write time.
     */
    suspend fun archiveAccount(id: Long): ArchiveAccountResult

    /**
     * Flip an account's archived flag back off. Always succeeds — there's no
     * data integrity risk because unarchiving doesn't introduce orphan rows.
     */
    suspend fun unarchiveAccount(id: Long)

    suspend fun saveCategoryBudget(categoryId: Long, limitCents: Long)
    suspend fun deleteCategoryBudget(categoryId: Long)

    // ── Recurring transactions ───────────────────────────────────────────

    /** All rules, soonest-due first, with category display data joined in. */
    val recurringRules: Flow<List<RecurringRule>>

    /**
     * Create ([id] == null) or update a recurring rule. On update, the next
     * due date is preserved when the schedule (cadence + first occurrence)
     * is unchanged — editing the amount of a rule must not re-trigger a
     * backfill — and reset to [firstOccurrenceEpochDay] when it changed.
     */
    suspend fun saveRecurringRule(
        id: Long?,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        merchant: String,
        notes: String,
        cadence: RecurrenceCadence,
        firstOccurrenceEpochDay: Long
    )

    suspend fun deleteRecurringRule(id: Long)

    suspend fun setRecurringRulePaused(id: Long, paused: Boolean)

    /**
     * Materialize expense rows for every active rule whose next occurrence is
     * on or before [todayEpochDay] (KL calendar day), advancing each rule's
     * schedule — all in one transaction. Returns how many expenses were
     * logged so the UI can announce the catch-up. Safe to call on every
     * launch; a no-op when nothing is due.
     */
    suspend fun processDueRecurringRules(todayEpochDay: Long): Int
}
