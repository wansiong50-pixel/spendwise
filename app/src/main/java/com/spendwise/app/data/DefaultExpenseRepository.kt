package com.spendwise.app.data

import androidx.room.withTransaction
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryPeriodStats
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.MonthlyAggregate
import com.spendwise.app.domain.RangeStats
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurrenceSchedule
import com.spendwise.app.domain.RecurringRule
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DefaultExpenseRepository(
    private val database: ExpenseDatabase,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val recurringRuleDao: RecurringRuleDao
) : ExpenseRepository {
    override val categories: Flow<List<Category>> = categoryDao
        .observeCategories()
        .mapList { it.toDomain() }

    override val budgets: Flow<List<Budget>> = budgetDao
        .observeBudgets()
        .mapList { it.toDomain() }

    override fun expensesInRange(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        combine(
            expenseDao.observeExpensesInRange(startMillis, endMillis),
            categoryDao.observeCategories()
        ) { expenseEntities, categoryEntities ->
            val categoryById = categoryEntities.associateBy { it.id }
            expenseEntities.map { it.toDomain(categoryById[it.categoryId]) }
        }

    override val recentExpenses: Flow<List<Expense>> = combine(
        expenseDao.observeRecentExpenses(RECENT_EXPENSES_LIMIT),
        categoryDao.observeCategories()
    ) { expenseEntities, categoryEntities ->
        val categoryById = categoryEntities.associateBy { it.id }
        expenseEntities.map { it.toDomain(categoryById[it.categoryId]) }
    }

    override val monthlyAggregates: Flow<List<MonthlyAggregate>> =
        expenseDao.observeMonthlyAggregates().map { rows ->
            rows.mapNotNull { row ->
                // monthKey is "yyyy-MM" from strftime; a malformed key can only
                // come from a corrupted row, which we skip rather than crash on.
                runCatching { YearMonth.parse(row.monthKey) }.getOrNull()?.let {
                    MonthlyAggregate(it, row.expenseCents, row.incomeCents)
                }
            }
        }

    override val dayEntryCounts: Flow<Map<LocalDate, Int>> =
        expenseDao.observeDayEntryCounts().map { rows ->
            rows.associate { LocalDate.ofEpochDay(it.epochDay) to it.entryCount }
        }

    override val categoryEntryCounts: Flow<Map<Long, Int>> =
        expenseDao.observeCategoryCounts().map { rows ->
            rows.associate { it.categoryId to it.entryCount }
        }

    override fun categoryStatsInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<CategoryPeriodStats>> =
        expenseDao.observeCategoryStatsInRange(startMillis, endMillis).map { rows ->
            rows.map { CategoryPeriodStats(it.categoryId, it.entryCount, it.totalCents) }
        }

    override suspend fun rangeStats(startMillis: Long, endMillis: Long): RangeStats {
        val row = expenseDao.rangeStats(startMillis, endMillis)
        return RangeStats(row.entryCount, row.spendCents)
    }

    override suspend fun expensesByMerchant(merchant: String, limit: Int): List<Expense> =
        expenseDao.byMerchant(merchant, limit).toDomainList()

    override suspend fun expensesByCategory(categoryId: Long, limit: Int): List<Expense> =
        expenseDao.byCategory(categoryId, limit).toDomainList()

    private suspend fun List<ExpenseEntity>.toDomainList(): List<Expense> {
        val categoryById = categoryDao.observeCategories().first().associateBy { it.id }
        return map { it.toDomain(categoryById[it.categoryId]) }
    }

    override val accounts: Flow<List<Account>> = combine(
        accountDao.observeAccounts(),
        expenseDao.observeAccountDeltas()
    ) { accountEntities, deltas ->
        accountsWithBalances(accountEntities, deltas)
    }

    override val archivedAccounts: Flow<List<Account>> = combine(
        accountDao.observeArchivedAccounts(),
        expenseDao.observeAccountDeltas()
    ) { accountEntities, deltas ->
        accountsWithBalances(accountEntities, deltas)
    }

    /**
     * Per-account current balance =
     *   startingBalance + Σ income on this account − Σ expense on this account
     *
     * The signed delta is aggregated in SQL (one row per account) so balance
     * recomputation never materializes expense rows, no matter how large the
     * ledger grows.
     */
    private fun accountsWithBalances(
        accountEntities: List<AccountEntity>,
        deltas: List<AccountDeltaRow>
    ): List<Account> {
        val signedDeltaByAccount = deltas.associate { it.accountId to it.deltaCents }
        return accountEntities.map { entity ->
            val delta = signedDeltaByAccount[entity.id] ?: 0L
            entity.toDomain(currentBalanceCents = entity.startingBalanceCents + delta)
        }
    }

    override suspend fun seedDefaultAccount() {
        if (accountDao.count() > 0) return

        // Mirrors the fresh-install side of MIGRATION_5_6 exactly: same name,
        // type, color, icon. Starting balance is 0 — the user sets a real
        // balance from the Accounts screen in Phase 2. Note we don't pin
        // id = 1 here (autoGenerate handles it) since on a fresh DB there's
        // nothing competing for that id and the repository's `saveExpense`
        // fallback uses `defaultAccountId()` rather than a hardcoded 1L.
        accountDao.insert(
            AccountEntity(
                name = "Wallet",
                type = "CASH",
                startingBalanceCents = 0L,
                color = 0xff64748bL,
                iconName = "account_balance_wallet",
                sortOrder = 0,
                isArchived = false
            )
        )
    }

    override suspend fun seedDefaultCategories() {
        if (categoryDao.count() > 0) return

        categoryDao.insertAll(
            listOf(
                CategoryEntity(1L, "Food", 0xff0f766e, "restaurant", false, false),
                CategoryEntity(2L, "Transport", 0xffd97706, "directions_car", false, false),
                CategoryEntity(3L, "Bills", 0xffdc5f4d, "receipt", false, false),
                CategoryEntity(4L, "Shopping", 0xff2563eb, "shopping_bag", false, false),
                CategoryEntity(5L, "Health", 0xff16a34a, "local_hospital", false, false),
                // "Salary" replaces the older "Income/Adjustment" label —
                // shorter, more concrete, matches how most users think about
                // their primary income source. The isIncomeAdjustment = true
                // flag still routes entries here through the income track
                // (excluded from expense totals, surfaces under the Income
                // mode of the Breakdown screen, drives the Net cashflow
                // hero on the Dashboard). Users who need adjustment/refund
                // tracking can create a custom income category for it.
                CategoryEntity(6L, "Salary", 0xff64748b, "account_balance_wallet", true, false)
            )
        )
    }

    override suspend fun saveExpense(
        id: Long?,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        merchant: String,
        notes: String,
        occurredAtMillis: Long
    ) {
        // On edit, carry the original creation timestamp forward — @Update
        // rewrites the whole row, and stamping "now" here would make every
        // edit look like a fresh entry (skewing the merchant suggestions,
        // which rank by createdAtMillis). Falls back to now if the row
        // vanished between the UI read and this write.
        val createdAtMillis = if (id != null) {
            expenseDao.byId(id)?.createdAtMillis ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
        val entity = ExpenseEntity(
            id = id ?: 0L,
            amountCents = amountCents,
            categoryId = categoryId,
            accountId = accountId,
            merchant = merchant.trim(),
            notes = notes.trim(),
            occurredAtMillis = occurredAtMillis,
            createdAtMillis = createdAtMillis
        )

        if (id == null) {
            expenseDao.insert(entity)
        } else {
            expenseDao.update(entity)
        }
    }

    override suspend fun createCategory(
        name: String,
        color: Long,
        iconName: String,
        isIncome: Boolean
    ): Long {
        val nextId = categoryDao.maxId() + 1L
        categoryDao.insert(
            CategoryEntity(
                id = nextId,
                name = name.trim(),
                color = color,
                iconName = iconName,
                isIncomeAdjustment = isIncome,
                isCustom = true
            )
        )
        return nextId
    }

    override suspend fun updateCustomCategory(
        id: Long,
        name: String,
        color: Long,
        iconName: String,
        isIncome: Boolean
    ): Boolean {
        return categoryDao.updateCustomCategory(
            id = id,
            name = name.trim(),
            color = color,
            iconName = iconName,
            isIncome = isIncome
        ) > 0
    }

    override suspend fun deleteCustomCategory(id: Long): Boolean {
        if (categoryDao.expenseCountForCategory(id) > 0) return false
        // A rule without its category is meaningless, so rules die with the
        // category (same transaction — the rules' RESTRICT FK would otherwise
        // block the category row's removal).
        return database.withTransaction {
            recurringRuleDao.deleteByCategory(id)
            categoryDao.deleteCustomCategory(id) > 0
        }
    }

    override suspend fun deleteCustomCategory(id: Long, strategy: CategoryDeletion): Boolean {
        // Single transaction: reassign-or-delete the expenses FIRST so the FK
        // (RESTRICT) is satisfied, then drop the category row. If any step
        // fails, the whole thing rolls back and the DB stays consistent.
        // Recurring rules follow the same strategy as the expenses: migrated
        // along on Migrate, dropped on DeleteExpenses.
        return database.withTransaction {
            when (strategy) {
                is CategoryDeletion.Migrate -> {
                    if (strategy.destinationId == id) return@withTransaction false
                    expenseDao.reassignExpenses(fromId = id, toId = strategy.destinationId)
                    recurringRuleDao.reassignCategory(fromId = id, toId = strategy.destinationId)
                }
                CategoryDeletion.DeleteExpenses -> {
                    expenseDao.deleteExpensesByCategory(id)
                    recurringRuleDao.deleteByCategory(id)
                }
            }
            categoryDao.deleteCustomCategory(id) > 0
        }
    }

    override suspend fun deleteExpense(id: Long) {
        expenseDao.deleteById(id)
    }

    override suspend fun createAccount(
        name: String,
        type: AccountType,
        startingBalanceCents: Long,
        iconName: String,
        color: Long
    ): Long {
        return accountDao.insert(
            AccountEntity(
                name = name.trim(),
                type = type.storageKey,
                startingBalanceCents = startingBalanceCents,
                color = color,
                iconName = iconName,
                // Append-at-end ordering by default. Manual reordering UX is
                // out of scope for Phase 2 — users live with creation order
                // until we decide if drag-to-reorder is worth it.
                sortOrder = accountDao.count(),
                isArchived = false
            )
        )
    }

    override suspend fun updateAccount(
        id: Long,
        name: String,
        type: AccountType,
        startingBalanceCents: Long,
        iconName: String,
        color: Long
    ) {
        // Preserve sortOrder + isArchived by reading the existing row first.
        // We don't expose them as form fields in Phase 2, and Room's @Update
        // would clobber them to 0/false if we built a fresh entity here.
        val existing = accountDao.listAllIncludingArchived().firstOrNull { it.id == id } ?: return
        accountDao.update(
            existing.copy(
                name = name.trim(),
                type = type.storageKey,
                startingBalanceCents = startingBalanceCents,
                color = color,
                iconName = iconName
            )
        )
    }

    override suspend fun archiveAccount(id: Long): ArchiveAccountResult {
        val count = accountDao.transactionCountForAccount(id)
        if (count > 0) return ArchiveAccountResult.Blocked(transactionCount = count)
        // A rule pointing at an archived account would keep materializing
        // expenses into a surface excluded from the dashboard total — block
        // until the user retargets or deletes the rule.
        val ruleCount = recurringRuleDao.countForAccount(id)
        if (ruleCount > 0) {
            return ArchiveAccountResult.Blocked(transactionCount = 0, recurringRuleCount = ruleCount)
        }
        accountDao.archive(id)
        return ArchiveAccountResult.Archived
    }

    override suspend fun unarchiveAccount(id: Long) {
        accountDao.unarchive(id)
    }

    override suspend fun saveCategoryBudget(categoryId: Long, limitCents: Long) {
        budgetDao.insert(
            BudgetEntity(
                categoryId = categoryId,
                monthlyLimitCents = limitCents
            )
        )
    }

    override suspend fun deleteCategoryBudget(categoryId: Long) {
        budgetDao.deleteForCategory(categoryId)
    }

    // ── Recurring transactions ───────────────────────────────────────────

    override val recurringRules: Flow<List<RecurringRule>> = combine(
        recurringRuleDao.observeRules(),
        categoryDao.observeCategories()
    ) { ruleEntities, categoryEntities ->
        val categoryById = categoryEntities.associateBy { it.id }
        ruleEntities.map { it.toDomain(categoryById[it.categoryId]) }
    }

    override suspend fun saveRecurringRule(
        id: Long?,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        merchant: String,
        notes: String,
        cadence: RecurrenceCadence,
        firstOccurrenceEpochDay: Long
    ) {
        if (id == null) {
            recurringRuleDao.insert(
                RecurringRuleEntity(
                    amountCents = amountCents,
                    categoryId = categoryId,
                    accountId = accountId,
                    merchant = merchant.trim(),
                    notes = notes.trim(),
                    cadence = cadence.storageKey,
                    anchorEpochDay = firstOccurrenceEpochDay,
                    nextDueEpochDay = firstOccurrenceEpochDay,
                    isPaused = false,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            return
        }

        val existing = recurringRuleDao.byId(id) ?: return
        val scheduleChanged = existing.anchorEpochDay != firstOccurrenceEpochDay ||
            existing.cadence != cadence.storageKey
        // Same schedule → keep the advanced due date so editing the amount
        // doesn't replay occurrences that already logged.
        // Changed schedule → apply it to UPCOMING occurrences only (first
        // occurrence of the new schedule strictly after today). Restarting
        // from the anchor would replay history: a cadence-only edit on a
        // months-old rule would instantly duplicate every logged occurrence.
        // Backdated backfill is a create-time behavior, never an edit one.
        val nextDueEpochDay = if (scheduleChanged) {
            RecurrenceSchedule.firstOccurrenceOnOrAfter(
                anchor = LocalDate.ofEpochDay(firstOccurrenceEpochDay),
                cadence = cadence,
                lowerBound = LocalDate.now(ZONE_KL).plusDays(1)
            ).toEpochDay()
        } else {
            existing.nextDueEpochDay
        }
        recurringRuleDao.update(
            existing.copy(
                amountCents = amountCents,
                categoryId = categoryId,
                accountId = accountId,
                merchant = merchant.trim(),
                notes = notes.trim(),
                cadence = cadence.storageKey,
                anchorEpochDay = firstOccurrenceEpochDay,
                nextDueEpochDay = nextDueEpochDay
            )
        )
    }

    override suspend fun deleteRecurringRule(id: Long) {
        recurringRuleDao.deleteById(id)
    }

    override suspend fun setRecurringRulePaused(id: Long, paused: Boolean) {
        if (paused) {
            recurringRuleDao.setPaused(id, true)
            return
        }
        // Resuming skips the paused gap: a pause means "these occurrences
        // didn't happen" (cancelled subscription, moved out), so the schedule
        // fast-forwards to its next occurrence from today instead of
        // backfilling months of entries the user explicitly stopped. The
        // maxOf keeps an already-future due date (paused and resumed the
        // same day) from being pulled backward into a duplicate.
        database.withTransaction {
            val existing = recurringRuleDao.byId(id)
            if (existing != null) {
                val lowerBound = maxOf(
                    LocalDate.now(ZONE_KL).toEpochDay(),
                    existing.nextDueEpochDay
                )
                val fastForwarded = RecurrenceSchedule.firstOccurrenceOnOrAfter(
                    anchor = LocalDate.ofEpochDay(existing.anchorEpochDay),
                    cadence = RecurrenceCadence.fromStorageKey(existing.cadence),
                    lowerBound = LocalDate.ofEpochDay(lowerBound)
                )
                recurringRuleDao.advanceNextDue(id, fastForwarded.toEpochDay())
            }
            recurringRuleDao.setPaused(id, false)
        }
    }

    override suspend fun processDueRecurringRules(todayEpochDay: Long): Int {
        val today = LocalDate.ofEpochDay(todayEpochDay)
        var logged = 0
        // One transaction for the whole catch-up — INCLUDING the due-rules
        // read. Catch-up runs from several triggers (process start, app
        // foreground, rule save, restore); reading inside the transaction
        // means a concurrent run sees the advanced due dates instead of the
        // same stale set, which would double-log every occurrence. It also
        // keeps a crash mid-catch-up safe: everything rolls back and the
        // next run simply redoes it.
        database.withTransaction {
            val due = recurringRuleDao.dueRules(todayEpochDay)
            for (rule in due) {
                val result = RecurrenceSchedule.occurrencesDue(
                    nextDue = LocalDate.ofEpochDay(rule.nextDueEpochDay),
                    today = today,
                    cadence = RecurrenceCadence.fromStorageKey(rule.cadence),
                    anchor = LocalDate.ofEpochDay(rule.anchorEpochDay)
                )
                for (date in result.dates) {
                    expenseDao.insert(
                        ExpenseEntity(
                            amountCents = rule.amountCents,
                            categoryId = rule.categoryId,
                            accountId = rule.accountId,
                            merchant = rule.merchant,
                            notes = rule.notes,
                            occurredAtMillis = date.atStartOfDay(ZONE_KL).toInstant().toEpochMilli(),
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                    logged++
                }
                recurringRuleDao.advanceNextDue(rule.id, result.newNextDue.toEpochDay())
            }
        }
        return logged
    }
}

private fun <T, R> Flow<List<T>>.mapList(transform: (T) -> R): Flow<List<R>> {
    return map { values -> values.map(transform) }
}

/**
 * Cap on the reactive recent-expenses window. Merchant suggestion quality
 * plateaus well below this; the cap is what keeps the app's resident expense
 * memory bounded as the ledger grows over years.
 */
private const val RECENT_EXPENSES_LIMIT = 500

// Recurring occurrences are calendar dates; converting them to the stored
// occurredAtMillis uses the same KL zone as everything else in the app.
private val ZONE_KL = ZoneId.of("Asia/Kuala_Lumpur")
