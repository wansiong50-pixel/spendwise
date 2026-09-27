package com.spendwise.app.ui.botanical

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendwise.app.backup.AutoBackupWorker
import com.spendwise.app.data.CategoryDeletion
import com.spendwise.app.domain.Category
import com.spendwise.app.export.BackupPreview
import com.spendwise.app.export.BackupResult
import com.spendwise.app.ui.DeleteCategoryResult
import com.spendwise.app.ui.ExpenseTrackerViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private object Routes {
    const val Home = "home"
    const val Activity = "activity"
    const val Insights = "insights"
    const val Accounts = "accounts"
    const val Categories = "categories"
    const val Recurring = "recurring"
    const val Settings = "settings"
    const val Entry = "entry?kind={kind}&date={date}&expenseId={expenseId}&transferId={transferId}"
}

private enum class Tab(val route: String, val label: String, val icon: () -> ImageVector) {
    Home(Routes.Home, "Home", { BotIcons.Home }),
    Activity(Routes.Activity, "Activity", { BotIcons.Activity }),
    Insights(Routes.Insights, "Insights", { BotIcons.Insights })
}

private data class CategoryFormRequest(val categoryId: Long?, val income: Boolean)
private data class AccountFormRequest(val accountId: Long?)
private data class RuleFormRequest(val ruleId: Long?)
private data class BreakdownRequest(val categoryId: Long, val period: InsightsPeriod)

/**
 * The app shell: navigation between the three tabs and the secondary pages,
 * the floating tab pill, and every sheet. Sheets live here so they cover the
 * tab pill and survive tab switches.
 */
@Composable
fun ExpenseTrackerApp(viewModel: ExpenseTrackerViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = remember { BotToastState() }

    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val aggregates by viewModel.monthlyAggregates.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val monthTransfers by viewModel.selectedMonthTransfers.collectAsStateWithLifecycle()
    val rules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val entryCounts by viewModel.categoryEntryCounts.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val backupSettings by viewModel.autoBackupSettings.collectAsStateWithLifecycle()
    val activityRange by viewModel.activityRange.collectAsStateWithLifecycle()

    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = route in Tab.entries.map { it.route }

    val incomeIds = remember(state.categories) { incomeCategoryIds(state.categories) }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val names = remember(state.accounts, state.archivedAccounts) { accountNames(state.accounts + state.archivedAccounts) }
    val monthItems = remember(state.expenses, monthTransfers, incomeIds) {
        ledgerItems(state.expenses, monthTransfers, incomeIds)
    }

    // ── Overlay state ───────────────────────────────────────────────────
    var periodPickerOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<LedgerItem?>(null) }
    var dayOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var breakdown by remember { mutableStateOf<BreakdownRequest?>(null) }
    var categoryForm by remember { mutableStateOf<CategoryFormRequest?>(null) }
    var migrationFor by remember { mutableStateOf<Long?>(null) }
    var accountForm by remember { mutableStateOf<AccountFormRequest?>(null) }
    var ruleForm by remember { mutableStateOf<RuleFormRequest?>(null) }
    var confirm by remember { mutableStateOf<ConfirmRequest?>(null) }
    var restoreReview by remember { mutableStateOf<RestoreReview?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }
    var homeHeroCovered by remember { mutableStateOf(false) }
    val editCache = remember { mutableMapOf<String, LedgerItem>() }

    // Activity filters live here so the filter sheet can sit above the tab pill.
    var activityKind by rememberSaveable { mutableStateOf(ActivityKind.All) }
    var activitySearch by rememberSaveable { mutableStateOf("") }
    var activityAccount by rememberSaveable { mutableStateOf<Long?>(null) }
    var activityCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    val activityQuery = ActivityQuery(activityKind, activitySearch, activityAccount, activityCategory)
    val clearActivityFilters = {
        activityKind = ActivityKind.All
        activitySearch = ""
        activityAccount = null
        activityCategory = null
        viewModel.setActivityRange(null)
    }

    val changeMonth: (YearMonth) -> Unit = {
        viewModel.setSelectedMonth(it)
        viewModel.setActivityRange(null)
    }
    val goTab: (String) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openEntry: (EntryKind, LocalDate) -> Unit = { kind, date ->
        navController.navigate("entry?kind=${kind.name}&date=$date")
    }
    val editItem: (LedgerItem) -> Unit = { item ->
        editCache[item.key] = item
        detail = null
        when (item) {
            is LedgerItem.Entry -> navController.navigate("entry?expenseId=${item.expense.id}")
            is LedgerItem.Move -> navController.navigate("entry?transferId=${item.transfer.id}")
        }
    }
    val openCategoryForm: (Category?, Boolean) -> Unit = { category, income ->
        categoryForm = CategoryFormRequest(category?.id, category?.isIncomeAdjustment ?: income)
    }

    // ── Launchers ───────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                when (val preview = viewModel.previewBackup(uri)) {
                    is BackupPreview.Ready -> restoreReview = RestoreReview(uri, displayName(context, uri), preview)
                    is BackupPreview.Invalid -> toast.show(preview.reason)
                }
            }
        }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, AutoBackupWorker.URI_PERMISSION_FLAGS)
            viewModel.setAutoBackupFolder(uri)
            viewModel.setAutoBackupEnabled(true)
            toast.show("Backup folder selected · daily backups on")
        }
    }
    val saveCopy = {
        val stamp = LocalDateTime.now(KL_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        exportLauncher.launch("spendwise-backup-$stamp.json")
    }

    // ── Messages ────────────────────────────────────────────────────────
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    LaunchedEffect(backupResult) {
        val result = backupResult ?: return@LaunchedEffect
        toast.show(
            when (result) {
                is BackupResult.ExportSuccess -> "Backup saved · ${result.expenseCount} ${if (result.expenseCount == 1) "entry" else "entries"}"
                is BackupResult.ImportSuccess -> "Backup restored · ${result.expenseCount} ${if (result.expenseCount == 1) "entry" else "entries"}"
                is BackupResult.Failure -> result.reason
            }
        )
        viewModel.clearBackupResult()
    }
    val caughtUp by viewModel.recurringCatchUpCount.collectAsStateWithLifecycle()
    LaunchedEffect(caughtUp) {
        val n = caughtUp ?: return@LaunchedEffect
        toast.show("$n recurring ${if (n == 1) "entry" else "entries"} added")
        viewModel.clearRecurringCatchUp()
    }
    val message by viewModel.userMessage.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        toast.show(text)
        viewModel.clearUserMessage()
    }

    // Due recurring entries catch up whenever the app returns to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) viewModel.runRecurringCatchUp()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Light status icons over the dark tops of every page; dark ones once
    // Home's paper has scrolled under the status bar. The navigation handle
    // is dark over Home's paper and light over the navy pages.
    val activity = LocalActivity.current
    val view = LocalView.current
    val paperUnderStatusBar = route == Routes.Home && homeHeroCovered
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = paperUnderStatusBar
            isAppearanceLightNavigationBars = route == Routes.Home
        }
    }
    val statusScrim by animateColorAsState(
        targetValue = when {
            route == Routes.Home -> if (homeHeroCovered) Bot.Paper.copy(alpha = 0.94f) else Bot.Paper.copy(alpha = 0f)
            route?.startsWith("entry") == true -> Bot.PageDeep.copy(alpha = 0f)
            else -> Bot.PageDeep.copy(alpha = 0.95f)
        },
        animationSpec = tween(150),
        label = "statusScrim"
    )
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBottom = maxOf(20.dp, navInset)
    val tabClearance = 70.dp + navBottom + 16.dp
    val pageClearance = navInset + 32.dp

    CompositionLocalProvider(LocalToast provides toast) {
        Box(Modifier.fillMaxSize().background(Bot.PageDeep)) {
            NavHost(
                navController = navController,
                startDestination = Routes.Home,
                enterTransition = { fadeIn(tween(160)) },
                exitTransition = { fadeOut(tween(100)) },
                popEnterTransition = { fadeIn(tween(160)) },
                popExitTransition = { fadeOut(tween(100)) }
            ) {
                composable(Routes.Home) {
                    HomeScreen(
                        state = state,
                        month = month,
                        monthTransfers = monthTransfers,
                        bottomPadding = tabClearance,
                        onMonthChange = changeMonth,
                        onOpenPeriodPicker = { periodPickerOpen = true },
                        onOpenSettings = { navController.navigate(Routes.Settings) },
                        onOpenAccounts = { navController.navigate(Routes.Accounts) },
                        onAdd = { openEntry(it, todayKl()) },
                        onOpenBudgets = { navController.navigate(Routes.Categories) },
                        onOpenInsights = { goTab(Routes.Insights) },
                        onOpenActivity = { goTab(Routes.Activity) },
                        onCategory = { breakdown = BreakdownRequest(it.id, InsightsPeriod.OfMonth(month)) },
                        onDay = { dayOpen = it.toString() },
                        onOpenItem = { detail = it },
                        onHeroCoveredChange = { homeHeroCovered = it }
                    )
                }
                composable(Routes.Activity) {
                    val rangeWindow by viewModel.activityRangeEntries.collectAsStateWithLifecycle()
                    val showTrend = activityRange == null &&
                        (activityKind == ActivityKind.Expense || activityKind == ActivityKind.Income)
                    val trend = if (showTrend) viewModel.trendExpenses.collectAsStateWithLifecycle().value else emptyList()
                    val items = if (activityRange != null) {
                        remember(rangeWindow, incomeIds) { ledgerItems(rangeWindow.expenses, rangeWindow.transfers, incomeIds) }
                    } else monthItems
                    ActivityScreen(
                        month = month,
                        items = items,
                        categories = state.categories,
                        accounts = state.accounts + state.archivedAccounts,
                        query = activityQuery,
                        range = activityRange,
                        trendExpenses = trend,
                        bottomPadding = tabClearance,
                        onQueryChange = {
                            activityKind = it.kind
                            activitySearch = it.search
                            activityAccount = it.accountId
                            activityCategory = it.categoryId
                        },
                        onClearRange = { viewModel.setActivityRange(null) },
                        onClearAll = clearActivityFilters,
                        onMonthChange = changeMonth,
                        onOpenPeriodPicker = { periodPickerOpen = true },
                        onAdd = { openEntry(EntryKind.Expense, todayKl()) },
                        onOpenFilters = { filtersOpen = true },
                        onOpenItem = { detail = it }
                    )
                }
                composable(Routes.Insights) {
                    val yearExpenses by viewModel.insightsYearExpenses.collectAsStateWithLifecycle()
                    InsightsScreen(
                        state = state,
                        month = month,
                        yearExpenses = yearExpenses,
                        aggregates = aggregates,
                        bottomPadding = tabClearance,
                        onMonthChange = changeMonth,
                        onOpenPeriodPicker = { periodPickerOpen = true },
                        onCategory = { category, period -> breakdown = BreakdownRequest(category.id, period) },
                        onExportCsv = { period ->
                            when (period) {
                                is InsightsPeriod.OfMonth -> viewModel.exportCsv(
                                    monthLabel(period.month),
                                    period.month.toString(),
                                    period.month.atDay(1),
                                    period.month.plusMonths(1).atDay(1)
                                )
                                is InsightsPeriod.OfYear -> viewModel.exportCsv(
                                    period.year.toString(),
                                    period.year.toString(),
                                    LocalDate.of(period.year, 1, 1),
                                    LocalDate.of(period.year + 1, 1, 1)
                                )
                            }
                        }
                    )
                }
                composable(
                    route = Routes.Entry,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType; defaultValue = EntryKind.Expense.name },
                        navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("transferId") { type = NavType.LongType; defaultValue = -1L }
                    ),
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(150)) },
                    popExitTransition = { fadeOut(tween(150)) }
                ) { entry ->
                    val args = entry.arguments
                    val expenseId = args?.getLong("expenseId") ?: -1L
                    val transferId = args?.getLong("transferId") ?: -1L
                    val target = remember(expenseId, transferId) {
                        when {
                            expenseId > 0 -> {
                                val known = editCache["e$expenseId"] as? LedgerItem.Entry
                                    ?: (monthItems.firstOrNull { it.key == "e$expenseId" } as? LedgerItem.Entry)
                                    ?: recentExpenses.firstOrNull { it.id == expenseId }
                                        ?.let { LedgerItem.Entry(it, it.categoryId in incomeIds) }
                                known?.let { EntryTarget.EditExpense(it.expense, it.isIncome) }
                            }
                            transferId > 0 -> {
                                val known = editCache["t$transferId"] as? LedgerItem.Move
                                    ?: (monthItems.firstOrNull { it.key == "t$transferId" } as? LedgerItem.Move)
                                known?.let { EntryTarget.EditTransfer(it.transfer) }
                            }
                            else -> EntryTarget.New(
                                kind = runCatching { EntryKind.valueOf(args?.getString("kind").orEmpty()) }.getOrDefault(EntryKind.Expense),
                                date = args?.getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: todayKl()
                            )
                        }
                    }
                    if (target == null) {
                        // The entry vanished (deleted elsewhere, or the process was restored).
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        EntryScreen(
                            target = target,
                            categories = state.categories,
                            accounts = state.accounts,
                            recentExpenses = recentExpenses,
                            formError = formError,
                            onClearError = viewModel::clearFormError,
                            onSaveExpense = viewModel::saveExpense,
                            onSaveTransfer = viewModel::saveTransfer,
                            onCreateCategory = viewModel::createCategory,
                            onClose = { navController.popBackStack() },
                            onSaved = { date, updated ->
                                navController.popBackStack()
                                changeMonth(YearMonth.from(date))
                                clearActivityFilters()
                                goTab(Routes.Activity)
                                toast.show(if (updated) "Entry updated" else "Entry saved")
                            }
                        )
                    }
                }
                composable(Routes.Accounts) {
                    AccountsScreen(
                        accounts = state.accounts,
                        archivedAccounts = state.archivedAccounts,
                        totalBalance = state.totalBalanceCents,
                        bottomPadding = pageClearance,
                        onBack = { navController.popBackStack() },
                        onAdd = { accountForm = AccountFormRequest(null) },
                        onEdit = { accountForm = AccountFormRequest(it.id) },
                        onRestore = {
                            viewModel.unarchiveAccount(it.id)
                            toast.show("Account restored")
                        }
                    )
                }
                composable(Routes.Categories) {
                    CategoriesScreen(
                        categories = state.categories,
                        budgets = state.budgets,
                        month = month,
                        spentByCategory = remember(state.summary) {
                            (state.summary.categoryTotals + state.summary.incomeCategoryTotals)
                                .associate { it.categoryId to it.totalCents }
                        },
                        bottomPadding = pageClearance,
                        onBack = { navController.popBackStack() },
                        onAdd = { income -> openCategoryForm(null, income) },
                        onEdit = { openCategoryForm(it, it.isIncomeAdjustment) }
                    )
                }
                composable(Routes.Recurring) {
                    RecurringScreen(
                        rules = rules,
                        categories = state.categories,
                        bottomPadding = pageClearance,
                        onBack = { navController.popBackStack() },
                        onAdd = { ruleForm = RuleFormRequest(null) },
                        onEdit = { ruleForm = RuleFormRequest(it.id) },
                        onToggle = { rule ->
                            viewModel.setRecurringRulePaused(rule.id, paused = !rule.isPaused)
                            toast.show(if (rule.isPaused) "Recurring entry resumed · paused dates skipped" else "Recurring entry paused")
                        },
                        onCheck = viewModel::checkDueRecurring
                    )
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        backup = backupSettings,
                        bottomPadding = pageClearance,
                        onBack = { navController.popBackStack() },
                        onAccounts = { navController.navigate(Routes.Accounts) },
                        onCategories = { navController.navigate(Routes.Categories) },
                        onRecurring = { navController.navigate(Routes.Recurring) },
                        onBackupNow = {
                            val folder = backupSettings.treeUri
                            if (folder != null) {
                                viewModel.backupNow()
                                toast.show("Saving a backup to ${backupFolderName(folder)}")
                            } else {
                                saveCopy()
                            }
                        },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                        onToggleDaily = { enable ->
                            when {
                                enable && backupSettings.treeUri == null -> folderLauncher.launch(null)
                                else -> {
                                    viewModel.setAutoBackupEnabled(enable)
                                    toast.show(if (enable) "Daily backups on" else "Daily backups paused")
                                }
                            }
                        },
                        onChooseFolder = { folderLauncher.launch(null) },
                        onDownloadCopy = saveCopy
                    )
                }
            }

            // Keeps status-bar icons legible over content scrolling beneath them.
            Box(Modifier.fillMaxWidth().height(statusTop).background(statusScrim))

            if (onTab) {
                BottomNav(
                    active = route,
                    onSelect = goTab,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = navBottom)
                )
            }

            // ── Sheets ─────────────────────────────────────────────────────
            val dayDate = dayOpen?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            DaySheet(
                date = dayDate,
                items = remember(monthItems, dayDate) { monthItems.filter { it.date == dayDate } },
                categoriesById = categoriesById,
                names = names,
                onOpen = { detail = it },
                onAdd = { date ->
                    dayOpen = null
                    openEntry(EntryKind.Expense, date)
                },
                onDismiss = { dayOpen = null }
            )

            val breakdownCategory = breakdown?.let { request -> categoriesById[request.categoryId] }
            val yearForBreakdown = (breakdown?.period as? InsightsPeriod.OfYear)
            val breakdownYear = if (yearForBreakdown != null) {
                viewModel.insightsYearExpenses.collectAsStateWithLifecycle().value
            } else emptyList()
            BreakdownSheet(
                category = breakdownCategory,
                periodLabel = breakdown?.period?.label().orEmpty(),
                items = remember(breakdown, state.expenses, breakdownYear, incomeIds) {
                    val request = breakdown
                    if (request == null) emptyList() else {
                        val source = if (request.period is InsightsPeriod.OfYear) breakdownYear else state.expenses
                        ledgerItems(source.filter { it.categoryId == request.categoryId }, emptyList(), incomeIds)
                    }
                },
                categoriesById = categoriesById,
                names = names,
                onOpen = { detail = it },
                onEditCategory = {
                    breakdown = null
                    openCategoryForm(it, it.isIncomeAdjustment)
                },
                onDismiss = { breakdown = null }
            )

            EntryDetailSheet(
                item = detail?.let { current -> monthItems.firstOrNull { it.key == current.key } ?: current },
                categoriesById = categoriesById,
                names = names,
                loadHistory = viewModel::merchantHistoryFor,
                onEdit = { item ->
                    dayOpen = null
                    breakdown = null
                    editItem(item)
                },
                onDelete = { item ->
                    confirm = ConfirmRequest(
                        title = "Delete this entry?",
                        body = "Its effect on your account balances will be reversed.",
                        action = "Delete entry"
                    ) {
                        when (item) {
                            is LedgerItem.Entry -> viewModel.deleteExpense(item.expense.id)
                            is LedgerItem.Move -> viewModel.deleteTransfer(item.transfer.id)
                        }
                        detail = null
                        toast.show("Entry deleted")
                    }
                },
                onOpenHistory = { expense -> detail = LedgerItem.Entry(expense, expense.categoryId in incomeIds) },
                onShare = { text ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "SpendWise transaction")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "Share transaction")) }
                },
                onDismiss = { detail = null }
            )

            val editingCategory = categoryForm?.categoryId?.let { categoriesById[it] }
            val categoryUsed = editingCategory != null &&
                ((entryCounts[editingCategory.id] ?: 0) > 0 || rules.any { it.categoryId == editingCategory.id })
            CategoryFormSheet(
                visible = categoryForm != null,
                existing = editingCategory,
                initialIncome = categoryForm?.income ?: false,
                budgetCents = editingCategory?.let { c -> state.budgets.firstOrNull { it.categoryId == c.id }?.monthlyLimitCents } ?: 0L,
                used = categoryUsed,
                onSave = { name, iconName, color, isIncome, budget ->
                    val existing = editingCategory
                    val error = if (existing == null) {
                        viewModel.createCategory(name, iconName, color, isIncome, budget)
                    } else {
                        viewModel.updateCategory(existing.id, name, iconName, color, isIncome, budget)
                    }
                    if (error == null) {
                        categoryForm = null
                        toast.show("Category saved")
                    }
                    error
                },
                onDelete = editingCategory?.takeIf { it.isCustom }?.let { category ->
                    {
                        if (categoryUsed) {
                            categoryForm = null
                            migrationFor = category.id
                        } else {
                            confirm = ConfirmRequest(
                                title = "Delete ${category.name}?",
                                body = "This unused category will be removed.",
                                action = "Delete category"
                            ) {
                                when (val result = viewModel.deleteCategory(category.id)) {
                                    DeleteCategoryResult.Deleted -> {
                                        categoryForm = null
                                        toast.show("Deleted ${category.name}")
                                    }
                                    is DeleteCategoryResult.Blocked -> toast.show(result.reason)
                                    is DeleteCategoryResult.NeedsStrategy -> {
                                        categoryForm = null
                                        migrationFor = category.id
                                    }
                                }
                            }
                        }
                    }
                },
                onDismiss = { categoryForm = null }
            )

            val migrating = migrationFor?.let { categoriesById[it] }
            CategoryMigrationSheet(
                category = migrating,
                entryCount = migrating?.let { entryCounts[it.id] } ?: 0,
                ruleCount = migrating?.let { c -> rules.count { it.categoryId == c.id } } ?: 0,
                destinations = migrating?.let { c ->
                    state.categories.filter { it.id != c.id && it.isIncomeAdjustment == c.isIncomeAdjustment }
                }.orEmpty(),
                hasBudget = migrating?.let { c -> state.budgets.any { it.categoryId == c.id } } ?: false,
                onCancel = {
                    val id = migrationFor
                    migrationFor = null
                    if (id != null) categoryForm = CategoryFormRequest(id, migrating?.isIncomeAdjustment ?: false)
                },
                onMove = { destination ->
                    val category = migrating ?: return@CategoryMigrationSheet
                    viewModel.deleteCategoryWithStrategy(category.id, CategoryDeletion.Migrate(destination))
                    migrationFor = null
                    toast.show("Moved entries and deleted ${category.name}")
                },
                onDeleteAll = {
                    val category = migrating ?: return@CategoryMigrationSheet
                    viewModel.deleteCategoryWithStrategy(category.id, CategoryDeletion.DeleteExpenses)
                    migrationFor = null
                    toast.show("Deleted ${category.name} and its entries")
                }
            )

            val editingAccount = accountForm?.accountId?.let { id ->
                (state.accounts + state.archivedAccounts).firstOrNull { it.id == id }
            }
            AccountFormSheet(
                visible = accountForm != null,
                existing = editingAccount,
                onSave = { name, type, starting, iconName, color ->
                    val existing = editingAccount
                    val error = if (existing == null) {
                        viewModel.createAccount(name, type, starting, iconName, color)
                    } else {
                        viewModel.updateAccount(existing.id, name, type, starting, iconName, color)
                    }
                    if (error == null) {
                        accountForm = null
                        toast.show("Account saved")
                    }
                    error
                },
                onArchive = {
                    val account = editingAccount
                    if (account == null) null else {
                        val error = viewModel.archiveAccount(account.id)
                        if (error == null) {
                            accountForm = null
                            toast.show("Account archived")
                        }
                        error
                    }
                },
                onRestore = {
                    editingAccount?.let { viewModel.unarchiveAccount(it.id) }
                    accountForm = null
                    toast.show("Account restored")
                },
                onDismiss = { accountForm = null }
            )

            val editingRule = ruleForm?.ruleId?.let { id -> rules.firstOrNull { it.id == id } }
            RuleFormSheet(
                visible = ruleForm != null,
                existing = editingRule,
                categories = state.categories,
                accounts = state.accounts,
                onSave = { id, amount, categoryId, accountId, name, notes, cadence, firstDate ->
                    val error = viewModel.saveRecurringRule(id, amount, categoryId, accountId, name, notes, cadence, firstDate)
                    if (error == null) {
                        ruleForm = null
                        toast.show("Recurring entry saved")
                    }
                    error
                },
                onDelete = editingRule?.let { rule ->
                    {
                        confirm = ConfirmRequest(
                            title = "Delete recurring entry?",
                            body = "Existing transactions stay in your Activity. This rule will no longer create entries.",
                            action = "Delete rule"
                        ) {
                            viewModel.deleteRecurringRule(rule.id)
                            ruleForm = null
                            toast.show("Recurring rule deleted")
                        }
                    }
                },
                onDismiss = { ruleForm = null }
            )

            FilterSheet(
                visible = filtersOpen,
                month = month,
                query = activityQuery,
                range = activityRange,
                accounts = state.accounts,
                archivedAccounts = state.archivedAccounts,
                categories = state.categories,
                monthItems = monthItems,
                loadEntries = viewModel::loadEntries,
                onApply = { accountId, categoryId, range ->
                    activityAccount = accountId
                    activityCategory = categoryId
                    viewModel.setActivityRange(range)
                    filtersOpen = false
                },
                onDismiss = { filtersOpen = false }
            )

            RestoreReviewSheet(
                review = restoreReview,
                currentEntries = entryCounts.values.sum(),
                onConfirm = { review ->
                    restoreReview = null
                    viewModel.importBackup(review.uri)
                },
                onDismiss = { restoreReview = null }
            )

            ConfirmSheet(request = confirm, onDismiss = { confirm = null })

            CalendarPicker(
                visible = periodPickerOpen,
                mode = CalendarMode.Month,
                initial = month.atDay(1),
                onCancel = { periodPickerOpen = false },
                onConfirm = {
                    changeMonth(YearMonth.from(it))
                    periodPickerOpen = false
                }
            )

            BotToastHost(
                state = toast,
                bottomOffset = if (onTab) navBottom + 80.dp else navBottom + 12.dp
            )
        }
    }
}

/** The floating tab pill: Home, Activity, Insights, each with a label. */
@Composable
private fun BottomNav(active: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Bot.NavBg)
            .border(1.dp, Bot.NavBorder, RoundedCornerShape(999.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab.route == active
            val bg by animateColorAsState(if (selected) Bot.NavActive else Color.Transparent, tween(120), label = "tabBg")
            val ink by animateColorAsState(if (selected) Color.White else Bot.NavInk, tween(120), label = "tabInk")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(bg)
                    .botPress(scale = 1f, role = Role.Tab) {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(tab.route)
                        }
                    }
                    .semantics {
                        this.selected = selected
                        contentDescription = tab.label
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BotIcon(tab.icon(), size = 22.dp, tint = ink)
                Text(tab.label, style = body(12f, FontWeight.Medium, lineHeight = 1.2f), color = ink, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: "Backup file"

