package com.spendwise.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Fixed UTC offset for Asia/Kuala_Lumpur (+08:00, no DST since 1982) in
 * milliseconds. SQLite's date functions can't resolve IANA zone ids, so the
 * month/day bucketing queries below shift epoch millis by this constant before
 * formatting — the result matches `Instant.atZone(ZONE_KL).toLocalDate()`
 * exactly for every timestamp this app produces. Must stay in sync with the
 * `Asia/Kuala_Lumpur` zone the ViewModel and screens use.
 */
private const val KL_OFFSET_MILLIS = 28_800_000L

/** Raw month bucket from SQL — `monthKey` is "yyyy-MM" in KL time. */
data class MonthlyAggregateRow(
    val monthKey: String,
    val expenseCents: Long,
    val incomeCents: Long
)

/** Entry count for one KL-time calendar day (`epochDay` = LocalDate.toEpochDay). */
data class DayCountRow(val epochDay: Long, val entryCount: Int)

data class CategoryStatsRow(val categoryId: Long, val entryCount: Int, val totalCents: Long)

data class CategoryCountRow(val categoryId: Long, val entryCount: Int)

/** Signed income−expense sum per account, for derived balances. */
data class AccountDeltaRow(val accountId: Long, val deltaCents: Long)

data class RangeStatsRow(val entryCount: Int, val spendCents: Long)

@Dao
interface ExpenseDao {
    // The UI is month/year-scoped everywhere, so screens observe only the
    // window they render instead of the whole ledger. Keeps per-write
    // re-emissions proportional to the window, not the table. (There is
    // deliberately no observe-everything query — backup uses the one-shot
    // `allOnce` below.)
    @Query(
        """
        SELECT * FROM expenses
        WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis
        ORDER BY occurredAtMillis DESC, id DESC
        """
    )
    fun observeExpensesInRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>

    // Most recently *entered* rows (creation order, not occurrence order) —
    // feeds merchant suggestions/canonicalization, which rank by recency of
    // entry. The limit bounds memory; anything older than the last few
    // hundred entries has effectively zero suggestion weight anyway.
    @Query("SELECT * FROM expenses ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeRecentExpenses(limit: Int): Flow<List<ExpenseEntity>>

    // Per-month expense/income totals for the whole ledger — one tiny row per
    // month with data. Powers the month picker, trend sparklines, year picker,
    // and months-with-data gating without loading any expense rows.
    @Query(
        """
        SELECT strftime('%Y-%m', (e.occurredAtMillis + $KL_OFFSET_MILLIS) / 1000, 'unixepoch') AS monthKey,
               SUM(CASE WHEN c.isIncomeAdjustment = 1 THEN 0 ELSE e.amountCents END) AS expenseCents,
               SUM(CASE WHEN c.isIncomeAdjustment = 1 THEN e.amountCents ELSE 0 END) AS incomeCents
        FROM expenses e JOIN categories c ON c.id = e.categoryId
        GROUP BY monthKey
        """
    )
    fun observeMonthlyAggregates(): Flow<List<MonthlyAggregateRow>>

    // Entries per KL calendar day — date-picker dots + per-day entry counts.
    @Query(
        """
        SELECT (occurredAtMillis + $KL_OFFSET_MILLIS) / 86400000 AS epochDay,
               COUNT(*) AS entryCount
        FROM expenses
        GROUP BY epochDay
        """
    )
    fun observeDayEntryCounts(): Flow<List<DayCountRow>>

    // Per-category count + total inside a window (Categories screen chips).
    @Query(
        """
        SELECT categoryId, COUNT(*) AS entryCount, SUM(amountCents) AS totalCents
        FROM expenses
        WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis
        GROUP BY categoryId
        """
    )
    fun observeCategoryStatsInRange(startMillis: Long, endMillis: Long): Flow<List<CategoryStatsRow>>

    // All-time per-category entry counts — the delete-category flow needs the
    // true count (a month-scoped one would let deletes slip through and then
    // fail silently against the FK-RESTRICT check).
    @Query("SELECT categoryId, COUNT(*) AS entryCount FROM expenses GROUP BY categoryId")
    fun observeCategoryCounts(): Flow<List<CategoryCountRow>>

    // Signed per-account delta (income adds, expense subtracts) for derived
    // account balances.
    @Query(
        """
        SELECT e.accountId AS accountId,
               SUM(CASE WHEN c.isIncomeAdjustment = 1 THEN e.amountCents ELSE -e.amountCents END) AS deltaCents
        FROM expenses e JOIN categories c ON c.id = e.categoryId
        GROUP BY e.accountId
        """
    )
    fun observeAccountDeltas(): Flow<List<AccountDeltaRow>>

    // Non-income count + spend inside an arbitrary window — the custom-range
    // picker's live summary line.
    @Query(
        """
        SELECT COUNT(*) AS entryCount, COALESCE(SUM(e.amountCents), 0) AS spendCents
        FROM expenses e JOIN categories c ON c.id = e.categoryId
        WHERE c.isIncomeAdjustment = 0
          AND e.occurredAtMillis >= :startMillis AND e.occurredAtMillis < :endMillis
        """
    )
    suspend fun rangeStats(startMillis: Long, endMillis: Long): RangeStatsRow

    // Transaction-detail history strips. Merchant match is case-insensitive
    // on the trimmed name — SQLite LOWER/TRIM are ASCII-only, which matches
    // the ASCII merchant names this app collects; worst case a non-ASCII
    // merchant just shows a shorter history.
    @Query(
        """
        SELECT * FROM expenses
        WHERE TRIM(LOWER(merchant)) = TRIM(LOWER(:merchant))
        ORDER BY occurredAtMillis DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun byMerchant(merchant: String, limit: Int): List<ExpenseEntity>

    @Query(
        """
        SELECT * FROM expenses
        WHERE categoryId = :categoryId
        ORDER BY occurredAtMillis DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun byCategory(categoryId: Long, limit: Int): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Bulk operations used when a category is deleted with a migration target
    // or with cascading-expense delete. Callers must wrap both DAO ops in a
    // single `withTransaction` block — the `expenses.categoryId` FK is RESTRICT,
    // so the category row can only be removed AFTER every reference is cleared.
    @Query("UPDATE expenses SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun reassignExpenses(fromId: Long, toId: Long): Int

    @Query("DELETE FROM expenses WHERE categoryId = :categoryId")
    suspend fun deleteExpensesByCategory(categoryId: Long): Int

    // Backup/restore helpers. `allOnce` is a one-shot snapshot (vs the
    // reactive `observeExpenses`). REPLACE on insertAll is defensive — the
    // restore path wipes the table first, so conflicts shouldn't happen, but
    // REPLACE keeps the op idempotent if a future caller re-imports.
    @Query("SELECT * FROM expenses")
    suspend fun allOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(id), 0) FROM categories")
    suspend fun maxId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query(
        """
        UPDATE categories
        SET name = :name, color = :color, iconName = :iconName, isIncomeAdjustment = :isIncome
        WHERE id = :id AND isCustom = 1
        """
    )
    suspend fun updateCustomCategory(
        id: Long,
        name: String,
        color: Long,
        iconName: String,
        isIncome: Boolean
    ): Int

    @Query("DELETE FROM categories WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomCategory(id: Long): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun expenseCountForCategory(categoryId: Long): Int

    // Backup/restore helpers. `allOnce` snapshots every category including
    // built-ins so restoring a backup faithfully reproduces the user's
    // category list (any user-renamed built-ins included).
    @Query("SELECT * FROM categories")
    suspend fun allOnce(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    // Excludes archived accounts so the picker and dashboard don't surface
    // money sources the user has retired. `listAllIncludingArchived` exists for
    // settings/audit views that need the full set.
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    // Snapshot variant used by the repository's update path so we can preserve
    // sortOrder/isArchived without losing them in @Update's full-row rewrite.
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC")
    suspend fun listAllIncludingArchived(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    // The default account id is the first active account by sort order, used
    // as a fallback when a transaction is saved without an explicit account
    // pick (Phase 1: the picker UI doesn't exist yet, so every save lands
    // here). The migration seeds a "Wallet" row at id = 1 so this never
    // returns null in practice — the nullable return is just defensive.
    @Query("SELECT id FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun defaultAccountId(): Long?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE accounts SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    @Query("SELECT * FROM accounts WHERE isArchived = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeArchivedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM expenses WHERE accountId = :accountId")
    suspend fun transactionCountForAccount(accountId: Long): Int

    // Backup/restore helpers. `allOnce` covers both active and archived rows
    // so a restored backup keeps the user's archived accounts in the
    // Archived section instead of resurrecting them as active.
    @Query("SELECT * FROM accounts")
    suspend fun allOnce(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface RecurringRuleDao {
    // Soonest-due first — the natural reading order for the management screen
    // ("what's about to hit my ledger next").
    @Query("SELECT * FROM recurring_rules ORDER BY nextDueEpochDay ASC, id ASC")
    fun observeRules(): Flow<List<RecurringRuleEntity>>

    // Active rules whose next occurrence is today or earlier — the launch
    // catch-up processes exactly this set.
    @Query("SELECT * FROM recurring_rules WHERE isPaused = 0 AND nextDueEpochDay <= :todayEpochDay")
    suspend fun dueRules(todayEpochDay: Long): List<RecurringRuleEntity>

    // Suspend point-lookup (NOT the Flow) so callers inside withTransaction
    // stay on the transaction thread — collecting a Flow there can deadlock.
    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): RecurringRuleEntity?

    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recurring_rules SET nextDueEpochDay = :nextDueEpochDay WHERE id = :id")
    suspend fun advanceNextDue(id: Long, nextDueEpochDay: Long)

    @Query("UPDATE recurring_rules SET isPaused = :paused WHERE id = :id")
    suspend fun setPaused(id: Long, paused: Boolean)

    // Category-deletion support: rules follow the same strategy as the
    // category's expenses (reassign on Migrate, delete on DeleteExpenses) so
    // the RESTRICT FK never blocks the category row's removal.
    @Query("UPDATE recurring_rules SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun reassignCategory(fromId: Long, toId: Long): Int

    @Query("DELETE FROM recurring_rules WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring_rules WHERE accountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int

    // Backup/restore helpers.
    @Query("SELECT * FROM recurring_rules")
    suspend fun allOnce(): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)

    // Backup/restore helpers.
    @Query("SELECT * FROM budgets")
    suspend fun allOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
