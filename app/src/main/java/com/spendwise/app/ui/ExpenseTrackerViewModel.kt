package com.spendwise.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.app.AppContainer
import com.spendwise.app.analytics.MonthlySpendingSummary
import com.spendwise.app.analytics.SpendingAnalyzer
import com.spendwise.app.backup.AutoBackupWorker
import com.spendwise.app.data.AppearancePreferenceStore
import com.spendwise.app.data.ArchiveAccountResult
import com.spendwise.app.data.BackupPreferenceStore
import com.spendwise.app.data.BackupSettings
import com.spendwise.app.data.CategoryDeletion
import com.spendwise.app.data.ExpenseRepository
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryPeriodStats
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.ExpenseValidationError
import com.spendwise.app.domain.ExpenseValidator
import com.spendwise.app.domain.MerchantNames
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.MonthlyAggregate
import com.spendwise.app.domain.RangeStats
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurringRule
import com.spendwise.app.export.BackupManager
import com.spendwise.app.export.BackupResult
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val summary: MonthlySpendingSummary,
    val categories: List<Category>,
    /**
     * The SELECTED MONTH's expenses only — not the full ledger. Every screen
     * that reads this is month-scoped; year-scoped surfaces (Insights) and
     * cross-month lookups (merchant history, suggestions) have their own
     * dedicated, bounded flows on the ViewModel. Keeping the window small is
     * what caps the app's resident memory as the ledger grows.
     */
    val expenses: List<Expense>,
    val accounts: List<Account>,
    // Soft-deleted accounts. Surfaced in the Accounts screen's collapsible
    // Archived section so a mistakenly archived account can be restored
    // without recreating it. NOT included in [totalBalanceCents] because the
    // user's intent when archiving was "stop counting this toward what I
    // have".
    val archivedAccounts: List<Account>,
    // Sum of every active account's current balance — surfaced as the
    // "Total balance" hero on the dashboard. Computed here rather than in the
    // composable so the value is stable across recomposition and easy to
    // unit-test.
    val totalBalanceCents: Long,
    val budgets: List<Budget>
)

/**
 * Result of attempting to delete a category. Tells the UI what to do next:
 *  - [Deleted] — gone, no further action needed.
 *  - [Blocked] — show the reason as an inline error (e.g. built-in category).
 *  - [NeedsStrategy] — category still has expenses; UI must prompt the user
 *    to either migrate them elsewhere or delete them along with the category.
 */
sealed interface DeleteCategoryResult {
    data object Deleted : DeleteCategoryResult
    data class Blocked(val reason: String) : DeleteCategoryResult
    data class NeedsStrategy(val expenseCount: Int) : DeleteCategoryResult
}

class ExpenseTrackerViewModel(
    application: Application,
    private val expenseRepository: ExpenseRepository,
    private val spendingAnalyzer: SpendingAnalyzer,
    private val appearancePreferenceStore: AppearancePreferenceStore,
    private val backupManager: BackupManager,
    private val backupPreferenceStore: BackupPreferenceStore
) : AndroidViewModel(application) {

    private val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
    private val _selectedMonth = MutableStateFlow(YearMonth.now(zoneId))
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    val formError = MutableStateFlow<String?>(null)

    fun setSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    /** Epoch millis of this month's first instant in KL time. */
    private fun YearMonth.startMillis(): Long =
        atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    /**
     * Whole-ledger per-month totals, aggregated in SQL — one small object per
     * month with data. Feeds the month picker, trend sparklines, year picker,
     * and months-with-data gating without ever loading expense rows.
     */
    val monthlyAggregates: StateFlow<List<MonthlyAggregate>> =
        expenseRepository.monthlyAggregates
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * Set of months for which at least one expense exists. The dashboard
     * month-picker uses this to disable empty months so the user can't navigate
     * to a period with nothing to see.
     */
    // Derived from the shared [monthlyAggregates] StateFlow (not the
    // repository flow directly) so both consumers ride one Room observer.
    val monthsWithData: StateFlow<Set<YearMonth>> = monthlyAggregates
        .map { aggregates -> aggregates.map { it.month }.toSet() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    /**
     * Recently-entered expenses (bounded window, creation order). Backs the
     * add/edit sheet's merchant suggestions and save-time canonicalization.
     * Shared Eagerly because [saveExpense] reads `.value` synchronously — a
     * WhileSubscribed flow with no UI collector would never populate.
     */
    val recentExpenses: StateFlow<List<Expense>> = expenseRepository.recentExpenses
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Entry count per calendar day — date-picker dots + per-day counters. */
    val dayEntryCounts: StateFlow<Map<LocalDate, Int>> = expenseRepository.dayEntryCounts
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    /**
     * All-time entry count per category. Eager for the same reason as
     * [recentExpenses]: [deleteCategory] reads `.value` synchronously.
     */
    private val categoryEntryCounts: StateFlow<Map<Long, Int>> =
        expenseRepository.categoryEntryCounts
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyMap()
            )

    /**
     * Per-category count + spend for the CURRENT calendar month (not the
     * browsed month) — the Categories screen's "used N× · RM X this month"
     * chips. "Now" is resolved when collection (re)starts — the flow { }
     * wrapper runs per subscription, so with WhileSubscribed sharing a
     * process that lives across a month boundary picks up the new month the
     * next time the screen is opened after idle.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthCategoryStats: StateFlow<Map<Long, CategoryPeriodStats>> =
        flow { emit(YearMonth.now(zoneId)) }
            .flatMapLatest { now ->
                expenseRepository.categoryStatsInRange(
                    now.startMillis(),
                    now.plusMonths(1).startMillis()
                )
            }
            .map { stats -> stats.associateBy { it.categoryId } }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    // ── Insights year scope ──────────────────────────────────────────────
    // The Insights tab browses by calendar year. Keeping the selected year
    // here (rather than in the shell) lets the year's expense window be a
    // SQL-scoped flow — at most one year of rows resident, only while the
    // Insights tab is actually subscribed.

    private val _selectedInsightsYear = MutableStateFlow(LocalDate.now(zoneId).year)
    val selectedInsightsYear: StateFlow<Int> = _selectedInsightsYear

    fun setInsightsYear(year: Int) {
        _selectedInsightsYear.value = year
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val insightsYearExpenses: StateFlow<List<Expense>> = _selectedInsightsYear
        .flatMapLatest { year ->
            expenseRepository.expensesInRange(
                YearMonth.of(year, 1).startMillis(),
                YearMonth.of(year + 1, 1).startMillis()
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Live summary for the custom-range picker: non-income count + spend. */
    suspend fun rangeStats(from: LocalDate, to: LocalDate): RangeStats =
        expenseRepository.rangeStats(
            from.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            to.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        )

    /**
     * History strip for the transaction-detail sheet. Income rows group by
     * category ("other Salary entries"); expense rows group by merchant.
     * Mirrors the previous in-memory grouping, now as a bounded SQL lookup.
     */
    suspend fun merchantHistoryFor(expense: Expense, isIncome: Boolean): List<Expense> {
        val history = if (isIncome) {
            expenseRepository.expensesByCategory(expense.categoryId, limit = 40)
        } else {
            val key = expense.merchant.trim()
            if (key.isBlank()) return listOf(expense)
            expenseRepository.expensesByMerchant(key, limit = 40)
        }
        return history.ifEmpty { listOf(expense) }
    }

    // The selected month's expense window, joined with the month it belongs to
    // so downstream state never sees a month/window mismatch mid-switch.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedMonthExpenses: Flow<Pair<YearMonth, List<Expense>>> =
        _selectedMonth.flatMapLatest { month ->
            expenseRepository
                .expensesInRange(month.startMillis(), month.plusMonths(1).startMillis())
                .map { month to it }
        }

    val dashboardState: StateFlow<DashboardUiState> = combine(
        selectedMonthExpenses,
        expenseRepository.categories,
        expenseRepository.accounts,
        expenseRepository.archivedAccounts,
        expenseRepository.budgets
    ) { (month, expenses), categories, accounts, archivedAccounts, budgets ->
        DashboardUiState(
            summary = spendingAnalyzer.summarize(expenses, categories, month),
            categories = categories,
            expenses = expenses,
            accounts = accounts,
            archivedAccounts = archivedAccounts,
            totalBalanceCents = accounts.sumOf { it.currentBalanceCents },
            budgets = budgets
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(
                summary = spendingAnalyzer.summarize(emptyList(), emptyList()),
                categories = emptyList(),
                expenses = emptyList(),
                accounts = emptyList(),
                archivedAccounts = emptyList(),
                totalBalanceCents = 0L,
                budgets = emptyList()
            )
        )

    val isDarkMode: StateFlow<Boolean> = appearancePreferenceStore.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val startupDarkModePreference: StateFlow<Boolean?> =
        appearancePreferenceStore.startupDarkModePreference.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            expenseRepository.seedDefaultCategories()
            expenseRepository.seedDefaultAccount()
            // Recurring catch-up after seeding so FK targets exist on a fresh
            // install. Further runs fire on every app foreground (see the
            // shell's ON_START observer) — a resident process would otherwise
            // never notice a due date passing.
            runRecurringCatchUpNow()
        }
    }

    /**
     * Wipe any leftover validation error. Called by the UI when the add/edit
     * form mounts so a previous failed-save banner doesn't follow the user
     * into a fresh session (or into editing a different expense).
     */
    fun clearFormError() {
        formError.value = null
    }

    fun saveExpense(
        id: Long?,
        amountInput: String,
        categoryId: Long?,
        accountId: Long?,
        merchant: String,
        notes: String,
        dateInput: String
    ): Boolean {
        // Look up whether the chosen category is an income/adjustment — the
        // validator uses this to relax the "merchant required" rule.
        val isIncome = dashboardState.value.categories
            .firstOrNull { it.id == categoryId }
            ?.isIncomeAdjustment == true
        // recentExpenses arrives from SQL already sorted by createdAtMillis
        // descending, which is exactly the recency ranking canonicalize wants.
        val canonicalMerchant = MerchantNames.canonicalize(
            input = merchant,
            existing = recentExpenses.value
                .asSequence()
                .filter { id == null || it.id != id }
                .map { it.merchant }
                .toList()
        )

        val validationErrors = ExpenseValidator.validate(amountInput, canonicalMerchant, categoryId, isIncome)
        if (validationErrors.isNotEmpty()) {
            formError.value = validationErrors.toMessage()
            return false
        }

        // Account is required from Phase 3 onward — the picker UI defaults
        // to the most-recent or default account, so a null arriving here
        // means either no accounts exist (first-launch race vs the seed) or
        // a code path forgot to thread the pick. Surface it as a clear
        // validation error rather than silently swallowing.
        if (accountId == null) {
            formError.value = "Choose an account."
            return false
        }

        val occurredAtMillis = runCatching {
            LocalDate.parse(dateInput)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()

        if (occurredAtMillis == null) {
            formError.value = "Use date format YYYY-MM-DD."
            return false
        }

        // The validator already ran parseToCents, so this only trips in edge
        // cases (e.g. an amount too large to fit in Long cents) — but it must
        // set an error rather than return silently, or the Save button would
        // appear to do nothing.
        val amountCents = MoneyFormatter.parseToCents(amountInput)
        if (amountCents == null) {
            formError.value = "Enter an amount above RM 0.00."
            return false
        }
        formError.value = null

        viewModelScope.launch {
            expenseRepository.saveExpense(
                id = id,
                amountCents = amountCents,
                categoryId = requireNotNull(categoryId),
                accountId = accountId,
                merchant = canonicalMerchant,
                notes = notes,
                occurredAtMillis = occurredAtMillis
            )
        }

        return true
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(id)
        }
    }

    fun createCategory(
        nameInput: String,
        iconName: String,
        color: Long,
        isIncome: Boolean,
        budgetLimitInput: String
    ): String? {
        val name = nameInput.trim()
        if (name.isBlank()) return "Add a category name."
        if (dashboardState.value.categories.any { it.name.equals(name, ignoreCase = true) }) {
            return "That category already exists."
        }
        val budgetLimitCents = if (budgetLimitInput.isNotBlank()) {
            // Strict parse (zero rejected): a RM 0.00 monthly limit is never
            // meaningful — blank is the way to say "no budget".
            MoneyFormatter.parseToCents(budgetLimitInput) ?: return "Enter a valid budget limit."
        } else null

        viewModelScope.launch {
            val nextId = expenseRepository.createCategory(
                name = name,
                color = color,
                iconName = iconName,
                isIncome = isIncome
            )
            if (budgetLimitCents != null) {
                expenseRepository.saveCategoryBudget(nextId, budgetLimitCents)
            }
        }
        return null
    }

    fun updateCategory(
        categoryId: Long,
        nameInput: String,
        iconName: String,
        color: Long,
        isIncome: Boolean,
        budgetLimitInput: String
    ): String? {
        val name = nameInput.trim()
        val category = dashboardState.value.categories.firstOrNull { it.id == categoryId }
            ?: return "That category is no longer available."
        if (!category.isCustom) return "Built-in categories can't be edited."
        if (name.isBlank()) return "Add a category name."
        if (
            dashboardState.value.categories.any {
                it.id != categoryId && it.name.equals(name, ignoreCase = true)
            }
        ) {
            return "That category already exists."
        }
        val budgetLimitCents = if (budgetLimitInput.isNotBlank()) {
            // Strict parse (zero rejected): a RM 0.00 monthly limit is never
            // meaningful — blank is the way to say "no budget".
            MoneyFormatter.parseToCents(budgetLimitInput) ?: return "Enter a valid budget limit."
        } else null

        viewModelScope.launch {
            expenseRepository.updateCustomCategory(
                id = categoryId,
                name = name,
                color = color,
                iconName = iconName,
                isIncome = isIncome
            )
            if (budgetLimitCents != null) {
                expenseRepository.saveCategoryBudget(categoryId, budgetLimitCents)
            } else {
                expenseRepository.deleteCategoryBudget(categoryId)
            }
        }
        return null
    }

    fun deleteCategory(categoryId: Long): DeleteCategoryResult {
        val category = dashboardState.value.categories.firstOrNull { it.id == categoryId }
            ?: return DeleteCategoryResult.Blocked("That category is no longer available.")
        if (!category.isCustom) {
            return DeleteCategoryResult.Blocked("Built-in categories can't be deleted.")
        }
        val expenseCount = categoryEntryCounts.value[categoryId] ?: 0
        if (expenseCount > 0) {
            return DeleteCategoryResult.NeedsStrategy(expenseCount)
        }

        viewModelScope.launch {
            expenseRepository.deleteCustomCategory(categoryId)
        }
        return DeleteCategoryResult.Deleted
    }

    fun deleteCategoryWithStrategy(categoryId: Long, strategy: CategoryDeletion) {
        viewModelScope.launch {
            expenseRepository.deleteCustomCategory(categoryId, strategy)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            appearancePreferenceStore.setDarkMode(enabled)
        }
    }

    // One-shot StateFlow the Settings UI consumes for snackbar messages.
    // Null = nothing to show. Callers should reset to null after surfacing
    // the result so a config change doesn't re-show the same snackbar.
    val backupResult = MutableStateFlow<BackupResult?>(null)

    fun clearBackupResult() {
        backupResult.value = null
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupResult.value = backupManager.exportBackup(uri)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importBackup(uri)
            backupResult.value = result
            // A restored ledger may carry rules whose occurrences came due
            // since the backup was made. Materialize them now instead of
            // leaving a stale ledger until the next process restart.
            if (result is BackupResult.ImportSuccess) {
                runRecurringCatchUpNow()
            }
        }
    }

    // ── Automatic backups ────────────────────────────────────────────────

    val autoBackupSettings: StateFlow<BackupSettings> = backupPreferenceStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupSettings()
    )

    /**
     * Flip the daily-backup toggle. Scheduling lives here (not in the UI) so
     * the persisted flag and the WorkManager job can never disagree.
     */
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferenceStore.setEnabled(enabled)
        }
        if (enabled) {
            AutoBackupWorker.schedule(getApplication())
        } else {
            AutoBackupWorker.cancel(getApplication())
        }
    }

    /**
     * Persist the backup folder. The UI must take the persistable URI
     * permission (with [AutoBackupWorker.URI_PERMISSION_FLAGS]) before
     * calling this — the worker re-checks the grant on every run.
     */
    fun setAutoBackupFolder(uri: Uri) {
        viewModelScope.launch {
            backupPreferenceStore.setTreeUri(uri.toString())
        }
    }

    /** One-off backup into the chosen folder — the "Back up now" button. */
    fun backupNow() {
        AutoBackupWorker.backupNow(getApplication())
    }

    // ── Recurring transactions ───────────────────────────────────────────

    val recurringRules: StateFlow<List<RecurringRule>> = expenseRepository.recurringRules
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // One-shot count of expenses materialized by the launch catch-up. The
    // shell surfaces it as a toast ("Logged 2 recurring transactions") and
    // clears it so a config change doesn't re-announce.
    val recurringCatchUpCount = MutableStateFlow<Int?>(null)

    fun clearRecurringCatchUp() {
        recurringCatchUpCount.value = null
    }

    /**
     * Run the recurring catch-up now. Fired from every trigger that can make
     * occurrences newly due: process start (init), the app returning to the
     * foreground (a resident process crossing midnight/a due date would
     * otherwise never log), rule saves, and backup restores. Safe to call
     * concurrently — the repository reads and advances due rules inside one
     * transaction, so overlapping runs can't double-log.
     */
    fun runRecurringCatchUp() {
        viewModelScope.launch { runRecurringCatchUpNow() }
    }

    private suspend fun runRecurringCatchUpNow() {
        val logged = expenseRepository.processDueRecurringRules(
            LocalDate.now(zoneId).toEpochDay()
        )
        if (logged > 0) recurringCatchUpCount.value = logged
    }

    /**
     * Create ([id] == null) or update a recurring rule. Returns null on
     * success or an inline-displayable error message — same contract as
     * [createAccount].
     */
    fun saveRecurringRule(
        id: Long?,
        amountInput: String,
        categoryId: Long?,
        accountId: Long?,
        merchant: String,
        notes: String,
        cadence: RecurrenceCadence,
        firstOccurrenceInput: String
    ): String? {
        val amountCents = MoneyFormatter.parseToCents(amountInput)
            ?: return "Enter an amount above RM 0.00."
        if (categoryId == null) return "Choose a category."
        if (accountId == null) return "Choose an account."
        val isIncome = dashboardState.value.categories
            .firstOrNull { it.id == categoryId }
            ?.isIncomeAdjustment == true
        val name = merchant.trim()
        if (name.isBlank() && !isIncome) return "Add a merchant or description."
        val firstOccurrence = runCatching { LocalDate.parse(firstOccurrenceInput) }.getOrNull()
            ?: return "Use date format YYYY-MM-DD."

        viewModelScope.launch {
            expenseRepository.saveRecurringRule(
                id = id,
                amountCents = amountCents,
                categoryId = categoryId,
                accountId = accountId,
                merchant = name,
                notes = notes,
                cadence = cadence,
                firstOccurrenceEpochDay = firstOccurrence.toEpochDay()
            )
            // A rule starting today (or backdated) should hit the ledger
            // immediately, not on the next launch.
            val logged = expenseRepository.processDueRecurringRules(
                LocalDate.now(zoneId).toEpochDay()
            )
            if (logged > 0) recurringCatchUpCount.value = logged
        }
        return null
    }

    fun deleteRecurringRule(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteRecurringRule(id)
        }
    }

    fun setRecurringRulePaused(id: Long, paused: Boolean) {
        viewModelScope.launch {
            expenseRepository.setRecurringRulePaused(id, paused)
        }
    }

    /**
     * Create a new account. Returns null on success or a non-null error
     * message suitable for inline display in the form. Validates name
     * uniqueness (case-insensitive) and amount parseability against the same
     * rules the UI uses for expense amounts.
     */
    fun createAccount(
        nameInput: String,
        type: AccountType,
        startingBalanceInput: String,
        iconName: String,
        color: Long
    ): String? {
        val name = nameInput.trim()
        if (name.isBlank()) return "Add an account name."
        if (dashboardState.value.accounts.any { it.name.equals(name, ignoreCase = true) }) {
            return "That account already exists."
        }
        val startingBalanceCents = parseBalanceInput(startingBalanceInput)
            ?: return "Enter a valid starting balance."

        viewModelScope.launch {
            expenseRepository.createAccount(
                name = name,
                type = type,
                startingBalanceCents = startingBalanceCents,
                iconName = iconName,
                color = color
            )
        }
        return null
    }

    fun updateAccount(
        accountId: Long,
        nameInput: String,
        type: AccountType,
        startingBalanceInput: String,
        iconName: String,
        color: Long
    ): String? {
        val name = nameInput.trim()
        if (name.isBlank()) return "Add an account name."
        if (
            dashboardState.value.accounts.any {
                it.id != accountId && it.name.equals(name, ignoreCase = true)
            }
        ) {
            return "That account already exists."
        }
        val startingBalanceCents = parseBalanceInput(startingBalanceInput)
            ?: return "Enter a valid starting balance."

        viewModelScope.launch {
            expenseRepository.updateAccount(
                id = accountId,
                name = name,
                type = type,
                startingBalanceCents = startingBalanceCents,
                iconName = iconName,
                color = color
            )
        }
        return null
    }

    /**
     * Archive an account. Returns null on success or an error message when
     * blocked (e.g. account still has transactions). Phase 2 doesn't surface
     * an in-flow reassign dialog — the form just shows the error and the user
     * is expected to move transactions manually first. That UX can grow later
     * if it becomes annoying.
     */
    suspend fun archiveAccount(accountId: Long): String? {
        return when (val result = expenseRepository.archiveAccount(accountId)) {
            ArchiveAccountResult.Archived -> null
            is ArchiveAccountResult.Blocked -> {
                if (result.recurringRuleCount > 0) {
                    val r = result.recurringRuleCount
                    val ruleWord = if (r == 1) "rule uses" else "rules use"
                    "Can't archive — $r recurring $ruleWord this account."
                } else {
                    val n = result.transactionCount
                    val transactionWord = if (n == 1) "transaction" else "transactions"
                    "Can't archive — $n $transactionWord still point here."
                }
            }
        }
    }

    fun unarchiveAccount(accountId: Long) {
        viewModelScope.launch {
            expenseRepository.unarchiveAccount(accountId)
        }
    }

    /**
     * Parse a balance string in the same shape as expense amounts (decimal
     * with up to 2 places, optional thousand separators stripped). Returns
     * null on parse failure. Blank input is treated as 0 — a common case for
     * a freshly created account before the user knows the exact figure.
     */
    private fun parseBalanceInput(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return 0L
        // allowZero: a typed "0" must behave the same as leaving the field
        // blank — both mean "this account starts at zero".
        return MoneyFormatter.parseToCents(trimmed, allowZero = true)
    }

    private fun List<ExpenseValidationError>.toMessage(): String {
        return joinToString(separator = " ") { error ->
            when (error) {
                ExpenseValidationError.InvalidAmount -> "Enter an amount above RM 0.00."
                ExpenseValidationError.MissingMerchant -> "Add a merchant or description."
                ExpenseValidationError.MissingCategory -> "Choose a category."
            }
        }
    }
}

class ExpenseTrackerViewModelFactory(
    private val application: Application,
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExpenseTrackerViewModel(
            application = application,
            expenseRepository = appContainer.expenseRepository,
            spendingAnalyzer = appContainer.spendingAnalyzer,
            appearancePreferenceStore = appContainer.appearancePreferenceStore,
            backupManager = appContainer.backupManager,
            backupPreferenceStore = appContainer.backupPreferenceStore
        ) as T
    }
}
