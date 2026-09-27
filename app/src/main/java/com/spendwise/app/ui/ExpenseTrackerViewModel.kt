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
import com.spendwise.app.data.ArchiveAccountResult
import com.spendwise.app.data.BackupPreferenceStore
import com.spendwise.app.data.BackupSettings
import com.spendwise.app.data.CategoryDeletion
import com.spendwise.app.data.ExpenseRepository
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.ExpenseValidationError
import com.spendwise.app.domain.ExpenseValidator
import com.spendwise.app.domain.MerchantNames
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.MonthlyAggregate
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurringRule
import com.spendwise.app.domain.Transfer
import com.spendwise.app.export.BackupManager
import com.spendwise.app.export.BackupPreview
import com.spendwise.app.export.BackupResult
import com.spendwise.app.export.PeriodCsvExporter
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An inclusive span of days. */
data class DateRange(val from: LocalDate, val to: LocalDate)

/** Entries and transfers loaded for an arbitrary window. */
data class EntryWindow(
    val expenses: List<Expense> = emptyList(),
    val transfers: List<Transfer> = emptyList()
)

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

    /**
     * All-time entry count per category. Eager for the same reason as
     * [recentExpenses]: [deleteCategory] reads `.value` synchronously. The
     * category form reads it too — a category's kind can only change while
     * nothing is filed under it.
     */
    val categoryEntryCounts: StateFlow<Map<Long, Int>> =
        expenseRepository.categoryEntryCounts
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyMap()
            )

    // ── Insights year scope ──────────────────────────────────────────────
    // Insights' Year view covers the selected month's calendar year. The
    // year's rows are a SQL-scoped flow — at most one year resident, and only
    // while Year view is actually subscribed.

    @OptIn(ExperimentalCoroutinesApi::class)
    val insightsYearExpenses: StateFlow<List<Expense>> = _selectedMonth
        .map { it.year }
        .distinctUntilChanged()
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

    /**
     * Six months of rows ending with the selected month — the Activity
     * spending/income trend, which must honour the current search and
     * filters and so can't come from the pre-aggregated monthly totals.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val trendExpenses: StateFlow<List<Expense>> = _selectedMonth
        .flatMapLatest { month ->
            expenseRepository.expensesInRange(
                month.minusMonths(5).startMillis(),
                month.plusMonths(1).startMillis()
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Activity custom date range ───────────────────────────────────────
    // Null means "the selected month". A range replaces the month window for
    // Activity only; changing the month clears it.

    private val _activityRange = MutableStateFlow<DateRange?>(null)
    val activityRange: StateFlow<DateRange?> = _activityRange

    fun setActivityRange(range: DateRange?) {
        _activityRange.value = range
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activityRangeEntries: StateFlow<EntryWindow> = _activityRange
        .flatMapLatest { range ->
            if (range == null) {
                flowOf(EntryWindow())
            } else {
                val start = range.from.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = range.to.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                combine(
                    expenseRepository.expensesInRange(start, end),
                    expenseRepository.transfersInRange(start, end)
                ) { expenses, transfers -> EntryWindow(expenses, transfers) }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EntryWindow()
        )

    /** One-off read of every entry between two dates (inclusive) — the filter sheet's live count. */
    suspend fun loadEntries(from: LocalDate, to: LocalDate): EntryWindow {
        val start = from.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = to.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return EntryWindow(
            expenseRepository.expensesInRange(start, end).first(),
            expenseRepository.transfersInRange(start, end).first()
        )
    }

    // ── Status messages ──────────────────────────────────────────────────

    /** One-shot status line; the shell shows it as a toast and clears it. */
    val userMessage = MutableStateFlow<String?>(null)

    fun clearUserMessage() {
        userMessage.value = null
    }

    fun postMessage(message: String) {
        userMessage.value = message
    }

    // ── CSV export ───────────────────────────────────────────────────────

    /**
     * Share every entry from [from] (inclusive) to [untilExclusive] as CSV.
     * Reads the period straight from the database, so a year export doesn't
     * depend on which month the screens have loaded.
     */
    fun exportCsv(periodLabel: String, fileStem: String, from: LocalDate, untilExclusive: LocalDate) {
        viewModelScope.launch {
            val start = from.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = untilExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val expenses = expenseRepository.expensesInRange(start, end).first()
            val transfers = expenseRepository.transfersInRange(start, end).first()
            val count = expenses.size + transfers.size
            if (count == 0) {
                userMessage.value = "No entries in this period to export."
                return@launch
            }
            val state = dashboardState.value
            val csv = withContext(Dispatchers.Default) {
                PeriodCsvExporter.buildCsv(
                    expenses = expenses,
                    transfers = transfers,
                    categories = state.categories,
                    accounts = state.accounts + state.archivedAccounts,
                    zone = zoneId
                )
            }
            val shared = withContext(Dispatchers.IO) {
                PeriodCsvExporter.share(
                    getApplication(),
                    csv,
                    "SpendWise-$fileStem.csv",
                    "SpendWise — $periodLabel"
                )
            }
            userMessage.value = if (shared) {
                "$count ${if (count == 1) "entry" else "entries"} exported. Transfers are labelled separately."
            } else {
                "Couldn't export this period. Try again."
            }
        }
    }

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

    // ── Transfers ────────────────────────────────────────────────────────

    /** The selected month's transfers — interleaved into the Activity timeline. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedMonthTransfers: StateFlow<List<Transfer>> = _selectedMonth
        .flatMapLatest { month ->
            expenseRepository.transfersInRange(
                month.startMillis(),
                month.plusMonths(1).startMillis()
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Save a transfer. Same contract as [saveExpense]: false + [formError]
     * on validation failure, true when the write was dispatched.
     */
    fun saveTransfer(
        id: Long?,
        amountInput: String,
        fromAccountId: Long?,
        toAccountId: Long?,
        notes: String,
        dateInput: String
    ): Boolean {
        val amountCents = MoneyFormatter.parseToCents(amountInput)
        if (amountCents == null) {
            formError.value = "Enter an amount above RM 0.00."
            return false
        }
        if (fromAccountId == null || toAccountId == null) {
            formError.value = "Choose both accounts."
            return false
        }
        if (fromAccountId == toAccountId) {
            formError.value = "Choose two different accounts."
            return false
        }
        val occurredAtMillis = runCatching {
            LocalDate.parse(dateInput).atStartOfDay(zoneId).toInstant().toEpochMilli()
        }.getOrNull()
        if (occurredAtMillis == null) {
            formError.value = "Use date format YYYY-MM-DD."
            return false
        }
        formError.value = null

        viewModelScope.launch {
            expenseRepository.saveTransfer(
                id = id,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amountCents = amountCents,
                notes = notes,
                occurredAtMillis = occurredAtMillis
            )
        }
        return true
    }

    fun deleteTransfer(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteTransfer(id)
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
        if (name.isBlank()) return "Give this category a name."
        if (dashboardState.value.categories.any { it.name.equals(name, ignoreCase = true) }) {
            return "A category with this name already exists."
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
        if (name.isBlank()) return "Give this category a name."
        if (
            dashboardState.value.categories.any {
                it.id != categoryId && it.name.equals(name, ignoreCase = true)
            }
        ) {
            return "A category with this name already exists."
        }
        val budgetLimitCents = if (budgetLimitInput.isNotBlank()) {
            // Strict parse (zero rejected): a RM 0.00 monthly limit is never
            // meaningful — blank is the way to say "no budget".
            MoneyFormatter.parseToCents(budgetLimitInput) ?: return "Enter a valid budget limit."
        } else null

        viewModelScope.launch {
            // Built-ins keep their kind (and can't be deleted); name, art and
            // budget are the user's to change.
            if (category.isCustom) {
                expenseRepository.updateCustomCategory(
                    id = categoryId,
                    name = name,
                    color = color,
                    iconName = iconName,
                    isIncome = isIncome
                )
            } else {
                expenseRepository.updateBuiltInCategory(
                    id = categoryId,
                    name = name,
                    color = color,
                    iconName = iconName
                )
            }
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

    /** Reads a chosen backup file for the restore review; changes nothing. */
    suspend fun previewBackup(uri: Uri): BackupPreview = backupManager.previewBackup(uri)

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

    /** Recurring → "Check due entries": always answers, even when nothing was due. */
    fun checkDueRecurring() {
        viewModelScope.launch {
            val logged = expenseRepository.processDueRecurringRules(
                LocalDate.now(zoneId).toEpochDay()
            )
            userMessage.value = if (logged > 0) {
                "$logged recurring ${if (logged == 1) "entry" else "entries"} added"
            } else {
                "Recurring entries are up to date"
            }
        }
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
        if (name.isBlank()) return "Give this account a name."
        if (
            (dashboardState.value.accounts + dashboardState.value.archivedAccounts)
                .any { it.name.equals(name, ignoreCase = true) }
        ) {
            return "An account with this name already exists."
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
        if (name.isBlank()) return "Give this account a name."
        if (
            (dashboardState.value.accounts + dashboardState.value.archivedAccounts).any {
                it.id != accountId && it.name.equals(name, ignoreCase = true)
            }
        ) {
            return "An account with this name already exists."
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
        // A leading minus is an amount already owed (a credit card carrying a
        // balance, an overdraft) — the form's Positive / Negative choice.
        val negative = trimmed.startsWith("-") || trimmed.startsWith("−")
        // allowZero: a typed "0" must behave the same as leaving the field
        // blank — both mean "this account starts at zero".
        val magnitude = MoneyFormatter.parseToCents(
            trimmed.trimStart('-', '−'),
            allowZero = true
        ) ?: return null
        return if (negative) -magnitude else magnitude
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
            backupManager = appContainer.backupManager,
            backupPreferenceStore = appContainer.backupPreferenceStore
        ) as T
    }
}
