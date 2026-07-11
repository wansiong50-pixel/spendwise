@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.spendwise.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendwise.app.analytics.MonthlySpendingSummary
import com.spendwise.app.backup.AutoBackupWorker
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryTotal
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.ui.theme.AppIsDark
import com.spendwise.app.ui.theme.perfMode
import com.spendwise.app.ui.theme.AppOnSurface
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.AppSurfaceContainer
import com.spendwise.app.ui.theme.AppSurfaceContainerHigh
import com.spendwise.app.ui.theme.AppSurfaceLow
import com.spendwise.app.ui.theme.FinanceNumFeatures
import com.spendwise.app.ui.theme.NoFontPadding
import com.spendwise.app.ui.theme.SpendWiseMotion
import com.spendwise.app.ui.theme.SwButter
import com.spendwise.app.ui.theme.SwCoral
import com.spendwise.app.ui.theme.SwHeat0
import com.spendwise.app.ui.theme.SwHeat1
import com.spendwise.app.ui.theme.SwHeat2
import com.spendwise.app.ui.theme.SwHeat3
import com.spendwise.app.ui.theme.SwHeat4
import com.spendwise.app.ui.theme.SwInk
import com.spendwise.app.ui.theme.SwMint
import com.spendwise.app.ui.theme.SwNeg
import com.spendwise.app.ui.theme.SwPeach
import com.spendwise.app.ui.theme.SwPeachSoft
import com.spendwise.app.ui.theme.SwPink
import com.spendwise.app.ui.theme.SwPos
import com.spendwise.app.ui.theme.SwSky
import com.spendwise.app.ui.theme.SwViolet
import com.spendwise.app.ui.theme.SwVioletSoft
import com.spendwise.app.ui.theme.TrimBoth
import com.spendwise.app.ui.theme.pressable
import com.spendwise.app.ui.theme.pressableNoIndication
import com.spendwise.app.export.BackupResult
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
//  SpendWise v2 — single-file 1:1 port of the May 2026 redesign handoff.
//
//  Every screen below mirrors a 380×820 artboard from the Claude Design bundle
//  at spendwise/project/{screens-1.jsx, screens-2.jsx}. Spacing, radii, type
//  weights, colors, and component compositions all trace back to that source —
//  not to the old "warm cream ledger" direction. When in doubt, the JSX is
//  authoritative; this file should be read alongside it.
// ═════════════════════════════════════════════════════════════════════════════

private val ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

// ── Navigation routes ────────────────────────────────────────────────────────

private enum class V2Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", SpendWiseIcons.Home),
    Activity("activity", "Activity", SpendWiseIcons.List),
    Insights("insights", "Insights", SpendWiseIcons.Chart)
}

// Material "shared axis X" pattern for peer-tab transitions. Outgoing slides
// out + fades in one direction while the incoming slides in + fades from the
// opposite side, with a small delay on the incoming so the two views are
// never both at full opacity in the same place. This eliminates the doubled-
// text overlap you get from a pure crossfade between Home / Activity /
// Insights — the eye tracks position, not just alpha.
private val V2_PEER_ROUTES: List<String> = V2Tab.entries.map { it.route }

/**
 * Returns +1 if the user is moving forward through the peer tabs
 * (Home → Activity → Insights), −1 if backward, 0 if either endpoint is
 * not a peer tab (drill-down to Accounts, modal sheets, etc.) so the
 * caller can fall back to a plain crossfade.
 */
private fun v2PeerSlideDirection(initial: NavBackStackEntry?, target: NavBackStackEntry?): Int {
    val from = V2_PEER_ROUTES.indexOf(initial?.destination?.route)
    val to = V2_PEER_ROUTES.indexOf(target?.destination?.route)
    if (from < 0 || to < 0) return 0
    return when {
        to > from -> 1
        to < from -> -1
        else -> 0
    }
}

// Fallback (non-peer) fade timing. Outgoing fades out first, incoming fades
// in *after* the outgoing finishes — staggering eliminates the brief moment
// where both destinations are partially visible in the same position, which
// otherwise reads as the labels on each screen ghosting through each other
// during a drill-down (Dashboard → Accounts, Insights → Categories, etc.).
// Timing lives in the 150–250ms band the fast-feeling apps use (X, Linear):
// anything longer reads as the app wading, and short transitions also hide
// dropped frames better.
//
// INVARIANT: peer pages have transparent backgrounds over the shared canvas,
// so their alphas must be strictly sequential or the two pages ghost through
// each other — V2_PEER_FADE_IN_DELAY_MS must be >= V2_PEER_FADE_OUT_MS, and
// V2_FADE_IN_MS's delay (= V2_FADE_OUT_MS, wired in v2PeerEnterTransition)
// covers the non-peer fallback the same way. Opaque modal routes are exempt:
// their overlap reads as layering, not ghosting.
private const val V2_FADE_OUT_MS = 100
private const val V2_FADE_IN_MS = 150
private const val V2_PEER_ENTER_MS = 250
private const val V2_PEER_EXIT_MS = 200
private const val V2_PEER_FADE_IN_MS = 170
private const val V2_PEER_FADE_IN_DELAY_MS = 90
private const val V2_PEER_FADE_OUT_MS = 90

private fun v2PeerEnterTransition(
    scope: AnimatedContentTransitionScope<NavBackStackEntry>
): EnterTransition {
    val dir = v2PeerSlideDirection(scope.initialState, scope.targetState)
    return if (dir != 0) {
        // Incoming enters from the opposite side of the outgoing's exit
        // direction, fading in slightly *after* the outgoing has started
        // leaving — Material shared-axis timing.
        slideInHorizontally(
            animationSpec = tween(V2_PEER_ENTER_MS, easing = SpendWiseMotion.EaseDrawer),
            initialOffsetX = { full -> dir * full / 8 }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = V2_PEER_FADE_IN_MS,
                delayMillis = V2_PEER_FADE_IN_DELAY_MS,
                easing = SpendWiseMotion.EaseOut
            )
        )
    } else {
        fadeIn(
            tween(
                durationMillis = V2_FADE_IN_MS,
                delayMillis = V2_FADE_OUT_MS,
                easing = SpendWiseMotion.EaseOut
            )
        )
    }
}

private fun v2PeerExitTransition(
    scope: AnimatedContentTransitionScope<NavBackStackEntry>
): ExitTransition {
    val dir = v2PeerSlideDirection(scope.initialState, scope.targetState)
    return if (dir != 0) {
        slideOutHorizontally(
            animationSpec = tween(V2_PEER_EXIT_MS, easing = SpendWiseMotion.EaseDrawer),
            targetOffsetX = { full -> -dir * full / 8 }
        ) + fadeOut(tween(V2_PEER_FADE_OUT_MS, easing = SpendWiseMotion.EaseInOut))
    } else {
        fadeOut(tween(V2_FADE_OUT_MS, easing = SpendWiseMotion.EaseInOut))
    }
}

private const val ROUTE_ADD_EXPENSE = "add_expense?expenseId={expenseId}&income={income}"
private const val ARG_EXPENSE_ID = "expenseId"
private const val ARG_INCOME = "income"
private const val ROUTE_ACCOUNTS = "accounts"
private const val ROUTE_ACCOUNT_FORM = "account_form?accountId={accountId}"
private const val ARG_ACCOUNT_ID = "accountId"
private const val ROUTE_CATEGORIES = "categories"
private const val ROUTE_RECURRING = "recurring"

// ═════════════════════════════════════════════════════════════════════════════
//  Entry shell
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Top-level shell. Mounts the v2 NavHost, paints the lavender canvas, and
 * floats the bottom-nav pill cluster over the page so scrolling content
 * passes underneath it (the cluster is not a Material `bottomBar`).
 *
 * Keeps the legacy entry-point name [ExpenseTrackerApp] so MainActivity wires
 * to the new system without a corresponding change.
 */
@Composable
fun ExpenseTrackerApp(
    viewModel: ExpenseTrackerViewModel,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthsWithData by viewModel.monthsWithData.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    // Bounded companion flows to the month-scoped dashboard state: SQL-side
    // monthly totals (pickers/trends), the recent-entries window (merchant
    // suggestions), per-day entry counts (date-picker dots), and the Insights
    // tab's year window. Together these replace the old everything-in-memory
    // expense list.
    val monthlyAggregates by viewModel.monthlyAggregates.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val dayEntryCounts by viewModel.dayEntryCounts.collectAsStateWithLifecycle()
    val currentMonthCategoryStats by viewModel.currentMonthCategoryStats.collectAsStateWithLifecycle()
    val insightsYearExpenses by viewModel.insightsYearExpenses.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPeerTab = currentRoute in setOf(V2Tab.Home.route, V2Tab.Activity.route, V2Tab.Insights.route)

    var monthPickerOpen by remember { mutableStateOf(false) }
    var customRangeOpen by remember { mutableStateOf(false) }

    // Hoisted from V2ActivityScreen so the picker sheets render above the
    // bottom nav pill. State lives here; the Activity screen reads it and
    // fires callbacks to open/select.
    var accountPickerOpen by remember { mutableStateOf(false) }
    var categoryPickerOpen by remember { mutableStateOf(false) }
    var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Tx detail sheet — hoisted for the same reason: needs to paint over the
    // bottom nav, and hide it. Wrapping in Dialog caused layout-vs-window
    // sizing issues that clipped the action row.
    var openedTxId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Insights 3-dot menu — hoisted so the shell can blur + dim the bottom
    // nav (which lives here, not in V2InsightsScreen) while the popover is
    // open. Page-level blur stays inside V2InsightsScreen.
    var insightsMenuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var yearPickerOpen by remember { mutableStateOf(false) }
    // Lives in the ViewModel (not rememberSaveable) because the Insights
    // year drives a SQL-scoped expense window there.
    val selectedYear by viewModel.selectedInsightsYear.collectAsStateWithLifecycle()
    // Confirmation gate for "Restore from backup". The destructive nature
    // (wipes everything) means we never launch the file picker directly off
    // the settings row — the user must affirm in the confirmation sheet
    // first, and only then does the system file picker open.
    var restoreConfirmOpen by remember { mutableStateOf(false) }

    // SAF launchers — no permissions needed, the system file picker handles
    // both the location choice and the access grant. The returned URI is a
    // content:// URI scoped to this app's process, so we just hand it
    // straight to the ViewModel.
    val context = LocalContext.current
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    // Automatic-backup folder picker. Unlike the one-shot CreateDocument URI
    // above, the tree URI must outlive this process (the daily worker writes
    // into it), so we take the persistable read+write grant before handing
    // it to the ViewModel. Picking a folder also turns the toggle on — that's
    // the only reason the picker ever opens.
    val autoBackupSettings by viewModel.autoBackupSettings.collectAsStateWithLifecycle()
    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                AutoBackupWorker.URI_PERMISSION_FLAGS
            )
            viewModel.setAutoBackupFolder(uri)
            viewModel.setAutoBackupEnabled(true)
        }
    }

    // Surface the outcome of any backup/restore op as a long toast. Using
    // toast (rather than a Snackbar) keeps the change self-contained — the
    // top-level Scaffold has no SnackbarHost wired and adding one would
    // ripple through every screen. Cleared via the ViewModel so a config
    // change doesn't re-show the same message.
    LaunchedEffect(backupResult) {
        val result = backupResult ?: return@LaunchedEffect
        val message = when (result) {
            is BackupResult.ExportSuccess -> {
                val n = result.expenseCount
                "Backed up $n expense${if (n == 1) "" else "s"}"
            }
            is BackupResult.ImportSuccess -> {
                val n = result.expenseCount
                "Restored $n expense${if (n == 1) "" else "s"}"
            }
            is BackupResult.Failure -> result.reason
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.clearBackupResult()
    }

    // Re-run the recurring catch-up whenever the app returns to the
    // foreground. The init-time run only covers process starts; Android keeps
    // processes resident for days, so without this a rule due "today" would
    // never log for users who switch back to the app instead of relaunching
    // it. Concurrent-safe: the repository processes due rules inside one
    // transaction.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                viewModel.runRecurringCatchUp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Announce the recurring catch-up ("Logged 2 recurring transactions") the
    // same way — a toast keeps it out of every screen's layout, and clearing
    // via the ViewModel stops a config change from re-announcing.
    val recurringCatchUp by viewModel.recurringCatchUpCount.collectAsStateWithLifecycle()
    LaunchedEffect(recurringCatchUp) {
        val n = recurringCatchUp ?: return@LaunchedEffect
        Toast.makeText(
            context,
            "Logged $n recurring transaction${if (n == 1) "" else "s"}",
            Toast.LENGTH_LONG
        ).show()
        viewModel.clearRecurringCatchUp()
    }

    // Single source of truth for switching between Home / Activity / Insights.
    // Used by both the bottom nav and any in-screen affordance (Home's search
    // icon, "See all" links, on-screen back buttons) so the back stack stays
    // single-level — clicking Home in the nav pill always lands on Home,
    // regardless of how the user got to a peer tab.
    val navigateToPeerTab: (V2Tab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = V2Tab.Home.route,
                enterTransition = { v2PeerEnterTransition(this) },
                exitTransition = { v2PeerExitTransition(this) },
                popEnterTransition = { v2PeerEnterTransition(this) },
                popExitTransition = { v2PeerExitTransition(this) }
            ) {
                composable(V2Tab.Home.route) {
                    V2DashboardScreen(
                        state = state,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        onOpenMonthPicker = { monthPickerOpen = true },
                        onOpenAccounts = { navController.navigate(ROUTE_ACCOUNTS) },
                        onEditExpense = { id ->
                            navController.navigate("add_expense?expenseId=$id&income=false")
                        },
                        onSeeAllActivity = { navigateToPeerTab(V2Tab.Activity) },
                        onOpenSettings = { settingsOpen = true }
                    )
                }
                composable(V2Tab.Activity.route) {
                    V2ActivityScreen(
                        state = state,
                        monthlyAggregates = monthlyAggregates,
                        selectedMonth = selectedMonth,
                        onOpenMonthPicker = { monthPickerOpen = true },
                        selectedAccountId = selectedAccountId,
                        selectedCategoryId = selectedCategoryId,
                        onOpenAccountPicker = { accountPickerOpen = true },
                        onOpenCategoryPicker = { categoryPickerOpen = true },
                        onSelectAccount = { selectedAccountId = it },
                        onSelectCategory = { selectedCategoryId = it },
                        onBack = { navigateToPeerTab(V2Tab.Home) },
                        onSeeBreakdown = { navigateToPeerTab(V2Tab.Insights) },
                        onOpenTx = { id -> openedTxId = id }
                    )
                }
                composable(V2Tab.Insights.route) {
                    val prevYearHasData = remember(monthlyAggregates, selectedYear) {
                        monthlyAggregates.any { it.month.year == selectedYear - 1 }
                    }
                    V2InsightsScreen(
                        state = state,
                        yearExpenses = insightsYearExpenses,
                        prevYearHasData = prevYearHasData,
                        selectedYear = selectedYear,
                        onOpenYearPicker = { yearPickerOpen = true },
                        onBack = { navigateToPeerTab(V2Tab.Home) },
                        onManageCategories = { navController.navigate(ROUTE_CATEGORIES) },
                        menuOpen = insightsMenuOpen,
                        onMenuOpenChange = { insightsMenuOpen = it }
                    )
                }
                composable(
                    route = ROUTE_ADD_EXPENSE,
                    arguments = listOf(
                        navArgument(ARG_EXPENSE_ID) { type = NavType.LongType; defaultValue = -1L },
                        navArgument(ARG_INCOME) { type = NavType.BoolType; defaultValue = false }
                    ),
                    enterTransition = {
                        scaleIn(tween(SpendWiseMotion.Modal, easing = SpendWiseMotion.EaseOut), 0.96f) +
                            fadeIn(tween(SpendWiseMotion.Modal, easing = SpendWiseMotion.EaseOut))
                    },
                    exitTransition = {
                        scaleOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseInOut), 0.97f) +
                            fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseInOut))
                    }
                ) { entry ->
                    val expenseId = entry.arguments?.getLong(ARG_EXPENSE_ID) ?: -1L
                    val initialIncome = entry.arguments?.getBoolean(ARG_INCOME) ?: false
                    // Resolved from the month window (every edit entry point is a
                    // month-scoped list); the recent-entries window is a fallback
                    // for the rare edit of a row whose date was just moved out of
                    // the selected month.
                    val editing = if (expenseId > 0) {
                        state.expenses.find { it.id == expenseId }
                            ?: recentExpenses.find { it.id == expenseId }
                    } else null
                    LaunchedEffect(expenseId) { viewModel.clearFormError() }
                    V2AddExpenseSheet(
                        existing = editing,
                        categories = state.categories,
                        accounts = state.accounts,
                        expenses = recentExpenses,
                        dayEntryCounts = dayEntryCounts,
                        formError = formError,
                        initialIncome = initialIncome,
                        onSave = viewModel::saveExpense,
                        onCreateCategory = viewModel::createCategory,
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_ACCOUNTS) {
                    V2AccountsScreen(
                        accounts = state.accounts,
                        archivedAccounts = state.archivedAccounts,
                        totalBalanceCents = state.totalBalanceCents,
                        onBack = { navController.popBackStack() },
                        onAdd = { navController.navigate("account_form?accountId=-1") },
                        onEdit = { id -> navController.navigate("account_form?accountId=$id") },
                        onRestore = viewModel::unarchiveAccount
                    )
                }
                composable(
                    route = ROUTE_ACCOUNT_FORM,
                    arguments = listOf(
                        navArgument(ARG_ACCOUNT_ID) { type = NavType.LongType; defaultValue = -1L }
                    ),
                    enterTransition = {
                        scaleIn(tween(SpendWiseMotion.Modal, easing = SpendWiseMotion.EaseOut), 0.96f) +
                            fadeIn(tween(SpendWiseMotion.Modal, easing = SpendWiseMotion.EaseOut))
                    },
                    exitTransition = {
                        scaleOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseInOut), 0.97f) +
                            fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseInOut))
                    }
                ) { entry ->
                    val accountId = entry.arguments?.getLong(ARG_ACCOUNT_ID) ?: -1L
                    val editing = if (accountId > 0) state.accounts.find { it.id == accountId } else null
                    V2AccountFormSheet(
                        existing = editing,
                        onSaveCreate = viewModel::createAccount,
                        onSaveUpdate = viewModel::updateAccount,
                        onArchive = viewModel::archiveAccount,
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_CATEGORIES) {
                    V2CategoriesScreen(
                        state = state,
                        currentMonthStats = currentMonthCategoryStats,
                        onBack = { navController.popBackStack() },
                        onCreateCategory = viewModel::createCategory,
                        onUpdateCategory = viewModel::updateCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        onDeleteCategoryWithStrategy = viewModel::deleteCategoryWithStrategy
                    )
                }
                composable(ROUTE_RECURRING) {
                    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
                    V2RecurringScreen(
                        rules = recurringRules,
                        categories = state.categories,
                        accounts = state.accounts,
                        onBack = { navController.popBackStack() },
                        onSave = viewModel::saveRecurringRule,
                        onDelete = viewModel::deleteRecurringRule,
                        onSetPaused = viewModel::setRecurringRulePaused
                    )
                }
            }

            val hideBottomNav = accountPickerOpen || categoryPickerOpen || monthPickerOpen || customRangeOpen || settingsOpen || yearPickerOpen || restoreConfirmOpen || (openedTxId != null)
            // Blur + dim the bottom nav when the Insights 3-dot popover is
            // open, so it joins the page in receding behind the menu rather
            // than competing with it.
            // Modifier.blur is the single most expensive GPU op in the app —
            // skip the radius entirely on low-RAM devices (Android Go tier),
            // which fall back to the menu scrim alone. The visible effect is
            // a slightly less dramatic focus shift, but the device gets to
            // keep 60 fps when the menu opens instead of choking at <30.
            val skipBlur = perfMode.lowSpec
            val navBlur by animateDpAsState(
                targetValue = if (insightsMenuOpen && !skipBlur) {
                    if (isDarkMode) 2.dp else 12.dp
                } else 0.dp,
                animationSpec = tween(180),
                label = "navBlur"
            )
            if (isPeerTab && !hideBottomNav) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .blur(navBlur)
                ) {
                    V2BottomNav(
                        activeRoute = currentRoute,
                        onSelect = navigateToPeerTab,
                        onFab = { navController.navigate("add_expense?expenseId=-1&income=false") }
                    )
                }
            }

            // Tx detail sheet — re-resolves the expense from current state
            // each composition so concurrent edits/deletes update the sheet
            // (or auto-dismiss if the record is gone).
            val openedTx = openedTxId?.let { id -> state.expenses.find { it.id == id } }
            LaunchedEffect(openedTxId, openedTx) {
                if (openedTxId != null && openedTx == null) openedTxId = null
            }
            V2TxDetailSheet(
                visible = openedTx != null,
                expense = openedTx,
                state = state,
                loadHistory = viewModel::merchantHistoryFor,
                onEdit = { id ->
                    openedTxId = null
                    navController.navigate("add_expense?expenseId=$id&income=false")
                },
                onDelete = { id ->
                    openedTxId = null
                    viewModel.deleteExpense(id)
                },
                onDismiss = { openedTxId = null }
            )

            // Account & category picker sheets — rendered after the bottom
            // nav so they paint on top and aren't blocked by the floating pill.
            V2AccountPickerSheet(
                visible = accountPickerOpen,
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelect = {
                    selectedAccountId = it
                    accountPickerOpen = false
                },
                onDismiss = { accountPickerOpen = false }
            )
            V2CategoryPickerSheet(
                visible = categoryPickerOpen,
                categories = state.categories,
                selectedCategoryId = selectedCategoryId,
                onSelect = {
                    selectedCategoryId = it
                    categoryPickerOpen = false
                },
                onDismiss = { categoryPickerOpen = false }
            )
            V2SettingsSheet(
                visible = settingsOpen,
                onManageAccounts = { navController.navigate(ROUTE_ACCOUNTS) },
                onManageCategories = { navController.navigate(ROUTE_CATEGORIES) },
                onManageRecurring = { navController.navigate(ROUTE_RECURRING) },
                onBackup = {
                    // Suggested filename includes a timestamp so the user
                    // ends up with a sortable history (spendwise-backup-20260526-2230.json,
                    // …-20260527-0915.json) instead of overwriting older
                    // backups by accident. The system file picker still
                    // lets them edit it before saving.
                    val stamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                    exportLauncher.launch("spendwise-backup-$stamp.json")
                },
                onRestore = { restoreConfirmOpen = true },
                autoBackup = autoBackupSettings,
                onAutoBackupToggle = { enabled ->
                    if (enabled && autoBackupSettings.treeUri == null) {
                        // No folder yet — the pick itself enables the toggle.
                        backupFolderLauncher.launch(null)
                    } else {
                        viewModel.setAutoBackupEnabled(enabled)
                    }
                },
                onChooseBackupFolder = { backupFolderLauncher.launch(null) },
                onBackupNow = viewModel::backupNow,
                onDismiss = { settingsOpen = false }
            )
            V2RestoreConfirmSheet(
                visible = restoreConfirmOpen,
                onConfirm = {
                    restoreConfirmOpen = false
                    importLauncher.launch(arrayOf("application/json"))
                },
                onDismiss = { restoreConfirmOpen = false }
            )
            V2YearPickerSheet(
                visible = yearPickerOpen,
                selected = selectedYear,
                years = remember(monthlyAggregates) {
                    val years = monthlyAggregates.map { it.month.year }.toMutableSet()
                    years.add(LocalDate.now(ZONE).year)
                    years.sortedDescending()
                },
                onSelect = {
                    viewModel.setInsightsYear(it)
                    yearPickerOpen = false
                },
                onDismiss = { yearPickerOpen = false }
            )
        }

        // Per-month spend totals (expenses only — income excluded) feed the
        // month-picker's compact bar+amount cards. Comes straight off the
        // SQL-side monthly aggregates — no expense rows involved.
        val monthSpendCents = remember(monthlyAggregates) {
            monthlyAggregates.associate { it.month to it.expenseCents }
        }
        // Custom range sheet visibility — driven by the month-picker's
        // "Custom range" chip. We keep the month picker hidden while the
        // range picker is on screen so only one sheet's modal scrim is
        // visible at a time.
        // LocalDate isn't Parcelable, and rotating mid-edit would lose the
        // inner sheet state anyway, so plain remember is fine here.
        var customRangeFrom by remember {
            mutableStateOf(LocalDate.now(ZONE).withDayOfMonth(1))
        }
        var customRangeTo by remember { mutableStateOf(LocalDate.now(ZONE)) }

        V2MonthPickerSheet(
            visible = monthPickerOpen,
            selected = selectedMonth,
            monthsWithData = monthsWithData,
            monthSpendCents = monthSpendCents,
            onSelect = {
                viewModel.setSelectedMonth(it)
                monthPickerOpen = false
            },
            onDismiss = { monthPickerOpen = false },
            onCustomRange = {
                // Default range = selected month so the calendar opens in
                // a familiar context. User can drag either endpoint.
                val firstOfMonth = selectedMonth.atDay(1)
                val today = LocalDate.now(ZONE)
                customRangeFrom = firstOfMonth
                customRangeTo = if (selectedMonth == YearMonth.from(today)) today
                    else selectedMonth.atEndOfMonth()
                monthPickerOpen = false
                customRangeOpen = true
            }
        )

        V2CustomRangeSheet(
            visible = customRangeOpen,
            initialFrom = customRangeFrom,
            initialTo = customRangeTo,
            queryRangeStats = viewModel::rangeStats,
            onApply = { from, to ->
                customRangeFrom = from
                customRangeTo = to
                // The dashboard is per-month today; approximate "apply
                // range" by jumping to the FROM month so at least one end
                // of the range is reflected in the visible data. A
                // proper range-aware scope is a follow-up that touches
                // the spending analyzer + dashboard UI.
                viewModel.setSelectedMonth(YearMonth.from(from))
                customRangeOpen = false
            },
            onBack = {
                customRangeOpen = false
                monthPickerOpen = true
            },
            onDismiss = { customRangeOpen = false }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Shared primitives — bottom nav, circles, pills, cards, amount, tiles
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Floating black pill nav + violet FAB. Matches the v2 reference's "one loud
 * thing per screen" rule — the cluster is the silhouette that identifies the
 * app from across the room.
 *
 * The active-tab indicator is a *single* white capsule that springs between
 * items rather than three independent backgrounds that fade in/out — the trick
 * big-app tab strips use (Linear, Robinhood, iOS TabView): the eye tracks one
 * object across the strip instead of seeing one capsule disappear and another
 * appear. Item bounds are captured via `onGloballyPositioned` and fed into a
 * low-bouncy spring driving the capsule's offset + width.
 *
 * Items + FAB are kept as siblings of the same Row so `Arrangement.SpaceBetween`
 * distributes slack evenly between all four (matching `tokens.jsx` BottomNav).
 * The capsule is a Box-overlay layered behind the Row in the same coordinate
 * space, so its `offset` lines up exactly with each item's reported bounds.
 */
@Composable
private fun V2BottomNav(
    activeRoute: String?,
    onSelect: (V2Tab) -> Unit,
    onFab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppIsDark
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        // Cap the pill width and center it so wide phones (Pixel 9 Pro XL,
        // Pro Max-class, foldable inner) don't get the "icons drifting apart"
        // look — items stay packed at vivo-style density instead of stretching
        // across all available horizontal space.
        contentAlignment = Alignment.Center
    ) {
        val pillHeight = 68.dp
        val tabs = V2Tab.entries
        val selectedIndex = tabs.indexOfFirst { it.route == activeRoute }.coerceAtLeast(0)
        // Claude reference: light = solid ink pill; dark = translucent
        // aubergine pill at 85% with a faint white hairline and black lift.
        val pillBg = if (isDark) Color(0xFF1A1727).copy(alpha = 0.85f) else Color(0xFF15121F)
        val capsuleColor = if (isDark) Color(0xFFF4F1FA) else Color.White

        val itemBounds = remember {
            mutableStateListOf<Rect>().apply { repeat(tabs.size) { add(Rect.Zero) } }
        }
        val target = itemBounds.getOrNull(selectedIndex) ?: Rect.Zero
        // iOS-flavoured spring: soft, highly damped movement so the selected
        // capsule glides into place instead of snapping a hard edge.
        // Reduced-motion users get an instant snap — the capsule is a
        // positional transform (not a fade), exactly the category Apple/Google
        // accessibility guidance asks us to drop when the user opts out.
        val reduced = perfMode.reducedMotion
        // Softer springs than the original 210/180 — the lower stiffness
        // stretches the glide out slightly so the capsule eases into place
        // instead of arriving with a hint of snap, and now that the item
        // padding + label width animate continuously the capsule is tracking
        // a moving target rather than lurching between two width jumps.
        val animX by animateFloatAsState(
            targetValue = target.left,
            animationSpec = if (reduced) snap() else spring(
                dampingRatio = 0.92f,
                stiffness = 160f,
                visibilityThreshold = 0.5f
            ),
            label = "navCapsuleX"
        )
        val animW by animateFloatAsState(
            targetValue = target.width,
            animationSpec = if (reduced) snap() else spring(
                dampingRatio = 0.94f,
                stiffness = 140f,
                visibilityThreshold = 0.5f
            ),
            label = "navCapsuleW"
        )
        val density = LocalDensity.current

        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(pillHeight),
            color = pillBg,
            shape = RoundedCornerShape(percent = 50),
            shadowElevation = if (isDark) 20.dp else 12.dp,
            tonalElevation = 0.dp,
            border = if (isDark)
                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            else null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                // Capsule overlay: aligned to center-start of the padded
                // content area, then shifted by the spring-tracked offset.
                // Sits behind the Row so the icons + labels paint on top.
                if (target.width > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset { IntOffset(animX.roundToInt(), 0) }
                            .height(48.dp)
                            .width(with(density) { animW.toDp() })
                            .clip(RoundedCornerShape(percent = 50))
                            .background(capsuleColor)
                    )
                }
                // The FAB lives in its own slot OUTSIDE the SpaceBetween row.
                // Reason: when a tab is tapped, the new selected item picks up
                // its 18.dp padding + label immediately while the old item's
                // label is still in the layout tree fading out (its
                // AnimatedVisibility exit keeps the Text at full width for the
                // 80ms fade). For that brief overlap window the tabs' total
                // width exceeds the row, and SpaceBetween would push the FAB
                // past the pill's right edge — where the Surface's rounded
                // clip trims it, reading as a momentary "squeeze". Allocating
                // the FAB's 52.dp first via weight means any tab overflow
                // happens inside the inner row instead.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            V2BottomNavItem(
                                tab = tab,
                                selected = index == selectedIndex,
                                onClick = { onSelect(tab) },
                                onBounds = { rect -> itemBounds[index] = rect }
                            )
                        }
                    }
                    // FAB sits at the right end of the pill, ringed by ink
                    // so the violet circle reads as "inside" the pill.
                    V2Fab(onClick = onFab)
                }
            }
        }
    }
}

@Composable
private fun V2BottomNavItem(
    tab: V2Tab,
    selected: Boolean,
    onClick: () -> Unit,
    onBounds: (Rect) -> Unit,
) {
    val isDark = AppIsDark
    // Only the foreground (icon/label) tweens — the background is now the
    // single sliding capsule managed by V2NavItemStrip.
    val fg by animateColorAsState(
        targetValue = if (selected) {
            if (isDark) Color(0xFF0F0D1A) else Color(0xFF15121F)
        } else {
            Color.White.copy(alpha = if (isDark) 0.5f else 0.65f)
        },
        animationSpec = tween(SpendWiseMotion.Tooltip, easing = SpendWiseMotion.EaseStandard),
        label = "bnFg"
    )
    // The 14↔18dp padding tweens instead of jumping so the item's width (and
    // therefore the capsule's spring target and the row's SpaceBetween slack)
    // changes continuously — the instant jump was half of the "capsule lurch"
    // on tab switch.
    val reduced = perfMode.reducedMotion
    val itemPad by animateDpAsState(
        targetValue = if (selected) 18.dp else 14.dp,
        animationSpec = if (reduced) snap() else tween(
            SpendWiseMotion.Tooltip,
            easing = SpendWiseMotion.EaseStandard
        ),
        label = "bnPad"
    )
    // onGloballyPositioned is OUTERMOST so the reported bounds include the
    // 14/18dp padding around the icon+label — otherwise the capsule would
    // only cover the inner content area and leave the icon hanging outside.
    // Light tick on tab switch — the physical texture X/Instagram-tier apps
    // put under every navigation gesture. Selecting the already-active tab
    // stays silent.
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .onGloballyPositioned { coords -> onBounds(coords.boundsInParent()) }
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .pressableNoIndication(scale = 0.96f) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .padding(horizontal = itemPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = fg,
            modifier = Modifier.size(20.dp)
        )
        // Label grows/collapses horizontally alongside the fade. Without the
        // shrink, the outgoing label held its full width for the whole 80ms
        // fade — during that overlap the incoming tab has already claimed its
        // label + padding, the row briefly overflows, and SpaceBetween
        // squeezes the exiting Text until it wraps ("Insights" breaking onto
        // two lines). Shrinking releases the width progressively, and
        // softWrap=false guarantees the label clips instead of wrapping no
        // matter how tight the row gets mid-transition.
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(SpendWiseMotion.Tooltip)) +
                expandHorizontally(
                    animationSpec = tween(SpendWiseMotion.Tooltip, easing = SpendWiseMotion.EaseStandard),
                    expandFrom = Alignment.Start
                ),
            exit = fadeOut(tween(120)) +
                shrinkHorizontally(
                    animationSpec = tween(120, easing = SpendWiseMotion.EaseStandard),
                    shrinkTowards = Alignment.Start
                )
        ) {
            Text(
                text = tab.label,
                color = fg,
                style = v2TextStyle(13.5f, FontWeight.SemiBold),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun V2Fab(onClick: () -> Unit) {
    val isDark = AppIsDark
    val haptics = LocalHapticFeedback.current
    // Ring matches the nav pill it sits inside, so the FAB looks like it was
    // punched out of the pill rather than sitting on top of it. Light = ink
    // pill, dark = aubergine pill.
    val ringColor = if (isDark) Color(0xFF0F0D1A) else Color(0xFF15121F)
    // The glow stack — in dark mode the FAB is the brightest pixel on the
    // screen, so we render a soft violet bloom behind it. Compose's coloured
    // shadow API takes ambient + spot tints; together they read as a single
    // halo at 20dp elevation. Light mode keeps a quieter 8dp neutral shadow.
    val glowModifier = if (isDark) {
        Modifier.shadow(
            elevation = 24.dp,
            shape = CircleShape,
            clip = false,
            ambientColor = SwViolet.copy(alpha = 0.55f),
            spotColor = SwViolet.copy(alpha = 0.6f)
        )
    } else {
        Modifier.shadow(
            elevation = 8.dp,
            shape = CircleShape,
            clip = false,
            ambientColor = SwViolet.copy(alpha = 0.55f),
            spotColor = SwViolet.copy(alpha = 0.55f)
        )
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .then(glowModifier)
            .pressableNoIndication(scale = 0.92f) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = SwViolet,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(4.dp, ringColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = SpendWiseIcons.Plus,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** White-disc icon button used in headers next to the month pill. */
@Composable
internal fun V2CircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    bg: Color? = null,
    tint: Color? = null
) {
    val isDark = AppIsDark
    val resolvedBg = bg ?: if (isDark) AppSurface else Color.White
    val resolvedTint = tint ?: SwInk
    Surface(
        modifier = Modifier
            .size(size)
            .pressableNoIndication(scale = 0.92f, onClick = onClick),
        color = resolvedBg,
        shape = CircleShape,
        shadowElevation = if (isDark) 0.dp else 4.dp,
        tonalElevation = 0.dp,
        border = if (isDark && bg == null)
            androidx.compose.foundation.BorderStroke(1.dp, v2Hairline())
        else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/** Pill variants — Dark (selected), Soft (default), Ghost (transparent). */
internal enum class V2PillVariant { Dark, Soft, Ghost }

@Composable
internal fun V2Pill(
    text: String,
    onClick: () -> Unit = {},
    variant: V2PillVariant = V2PillVariant.Soft,
    leading: ImageVector? = null,
    leadingDot: Color? = null,
    trailing: ImageVector? = null,
    bg: Color? = null,
    fg: Color? = null,
    modifier: Modifier = Modifier
) {
    val isDark = AppIsDark
    val resolvedBg = bg ?: when (variant) {
        V2PillVariant.Dark -> if (isDark) SwViolet else SwInk
        // surface3 (E8E4F2) — bumped from surface2 so the soft pill reads as a
        // distinct chip against the lavender screen bg (#E8E3F4 is too close).
        V2PillVariant.Soft -> if (isDark) AppSurface else AppSurfaceContainer
        V2PillVariant.Ghost -> Color.Transparent
    }
    val resolvedFg = fg ?: if (variant == V2PillVariant.Dark) Color.White else SwInk
    // Active state inverts the dot to the pill's foreground — matches the v2
    // reference where the red/green tone fades to white once the chip is on.
    val resolvedDot = leadingDot?.let { if (variant == V2PillVariant.Dark) resolvedFg else it }
    Surface(
        modifier = modifier
            .height(36.dp)
            .pressableNoIndication(onClick = onClick),
        color = resolvedBg,
        shape = RoundedCornerShape(percent = 50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leading != null) {
                Icon(leading, null, tint = resolvedFg, modifier = Modifier.size(14.dp))
            }
            if (resolvedDot != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(resolvedDot)
                )
            }
            Text(text = text, color = resolvedFg, style = v2TextStyle(13f, FontWeight.SemiBold))
            if (trailing != null) {
                Icon(trailing, null, tint = resolvedFg, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * The canonical pebble card. In light mode it's a true-white surface lifted off
 * the lavender canvas by a soft violet-tinted drop shadow. In dark mode the
 * shadow is dropped entirely — the v2 spec calls for brightness-based
 * elevation only, so the surface tone (#1A1727) on the aubergine canvas
 * (#0F0D1A) carries depth on its own. Drop shadows in dark just smear the
 * canvas without adding clarity.
 *
 * Pass [color] = null to use the surface token, which is what every screen
 * does. Card-as-background-image variants override this.
 */
@Composable
internal fun V2Card(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    color: Color? = null,
    padding: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val isDark = AppIsDark
    val resolvedColor = color ?: AppSurface
    // Large soft shadows are pure overdraw on budget GPUs — every card on
    // screen pays for its blurred shadow bitmap each frame. Low-spec devices
    // get a tighter 6dp lift that reads nearly the same at a fraction of the
    // fill cost; dark mode already draws no shadow at all.
    val cardElevation = if (perfMode.lowSpec) 6.dp else 14.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (isDark) it
                else it.shadow(
                    elevation = cardElevation,
                    shape = RoundedCornerShape(radius),
                    clip = false,
                    ambientColor = Color(0xFF281E50).copy(alpha = 0.25f),
                    spotColor = Color(0xFF281E50).copy(alpha = 0.25f)
                )
            }
            .clip(RoundedCornerShape(radius))
            .background(resolvedColor)
            .padding(padding)
    ) { content() }
}

/**
 * Gradient hero pebble. Two distinct moods:
 *
 *  • Light — peach→lavender pastel (`SwPeachSoft → SwVioletSoft`). Reads warm,
 *    like dawn behind a frosted window. Caustic discs are white-translucent
 *    (top) and violet-translucent (bottom).
 *
 *  • Dark — moody aubergine→indigo→violet (`#2A1F4D → #5238A6 → #8C7BFF`).
 *    Reads like a deep dusk sky lit from above. The caustic discs go to
 *    white-low-alpha + peach-low-alpha so the warm temperature still threads
 *    through the dark composition.
 *
 * The hero is the screen's most prominent surface in both modes — the
 * gradient shift is what carries the day/night flip more than any other piece
 * of UI.
 */
@Composable
internal fun V2GradientHero(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val isDark = AppIsDark
    val peachSoft = SwPeachSoft
    val violetSoft = SwVioletSoft
    // Cache the brush so it isn't reallocated every recomposition. The hero
    // sits at the top of the dashboard and the parent recomposes whenever the
    // animated balance value ticks — without remember, each tick spawns a new
    // Brush and a new List<Color>, both of which were measurable allocations
    // on the Pixel-emulator Davey traces.
    val heroBrush = remember(isDark, peachSoft, violetSoft) {
        val colors = if (isDark) {
            // Three-stop indigo gradient: deep aubergine → mid indigo → bright
            // violet. Tilted 135° via the linearGradient start/end below.
            listOf(Color(0xFF2A1F4D), Color(0xFF5238A6), Color(0xFF8C7BFF))
        } else {
            listOf(peachSoft, violetSoft)
        }
        Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
    val topDiscColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f)
    val bottomDiscColor = if (isDark) Color(0xFFF4A874).copy(alpha = 0.18f) else SwViolet.copy(alpha = 0.18f)
    val pressMod = if (onClick != null) Modifier.pressable(scale = 0.985f, onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(heroBrush)
            .then(
                // In dark, the hero's edge fades into the canvas without a
                // hairline; the spec calls for `1px rgba(255,255,255,0.06)`
                // to keep the gradient from disappearing into the screen.
                if (isDark) Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(radius)
                ) else Modifier
            )
            .then(pressMod)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(topDiscColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = 90.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(bottomDiscColor)
        )
        BoxWithConstraints { content() }
    }
}

private typealias BoxWithConstraintsScope = androidx.compose.foundation.layout.BoxWithConstraintsScope

/** Rounded-square pastel tile that hosts a category line icon. */
@Composable
internal fun V2Tile(
    color: Color,
    icon: ImageVector,
    size: Dp = 44.dp,
    iconTint: Color = if (AppIsDark) Color(0xFF0F0D1A) else SwInk,
    modifier: Modifier = Modifier
) {
    val radius = size * 0.32f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Big tabular money number with the muted "RM" prefix at 55%/55%.
 *
 * Sign handling: positive amounts may get an explicit "+" via [sign]; negative
 * amounts get "−" automatically when [cents] is negative. Pass [sign] = null
 * to suppress signs entirely (the default for hero balances).
 *
 * [animateValue] = true makes the displayed value roll from the previous
 * value to [cents] on each change (Robinhood/Mint/Wallet "count up" trick).
 * Opt in only for *aggregate* amounts (totals, balances, hero numbers) — never
 * on per-row transaction amounts, since those don't change in place. Cents
 * are interpolated as Float, so the precise roll is accurate up to about
 * RM 167,772 (2^24 cents); larger amounts snap without animation.
 */
@Composable
internal fun V2Amount(
    cents: Long,
    size: Float = 28f,
    color: Color = SwInk,
    sign: Char? = null,
    showDecimals: Boolean = true,
    weight: FontWeight = FontWeight.ExtraBold,
    animateValue: Boolean = false,
    modifier: Modifier = Modifier
) {
    // When animating, drive a Float that tracks `cents` with a critically-
    // damped spring. Slightly under-damped would feel toy-ish on money;
    // dampingRatio = 1f gives a clean "settle, no overshoot" tick that reads
    // as accurate. Stiffness 220 gives the roll a perceptible ~500ms duration
    // — long enough to register, short enough not to feel slow.
    val centsAsFloat = cents.toFloat()
    val animationAllowed = animateValue && abs(cents) < 16_000_000L
    val animatedFloat by animateFloatAsState(
        targetValue = centsAsFloat,
        animationSpec = spring(
            dampingRatio = 1f,
            stiffness = 220f,
            visibilityThreshold = 1f
        ),
        label = "v2AmountCounter"
    )
    val displayedCents = if (animationAllowed) animatedFloat.toLong() else cents
    val absCents = abs(displayedCents)
    val ringgit = absCents / 100
    val sub = absCents % 100
    val intStr = formatThousands(ringgit)
    val displaySign = when {
        sign != null -> sign
        displayedCents < 0 -> '−'
        else -> null
    }
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        if (displaySign != null) {
            Text(
                text = "$displaySign ",
                color = color,
                style = v2NumStyle(size, weight)
            )
        }
        Text(
            text = "RM",
            color = color.copy(alpha = 0.5f),
            modifier = Modifier.padding(
                end = (size * 0.18f).dp,
                bottom = (size * 0.12f).dp
            ),
            style = v2NumStyle(size * 0.55f, FontWeight.Medium)
        )
        Text(text = intStr, color = color, style = v2NumStyle(size, weight))
        if (showDecimals) {
            Text(
                text = ".%02d".format(sub),
                color = color.copy(alpha = 0.5f),
                style = v2NumStyle(size, weight)
            )
        }
    }
}

private fun formatThousands(value: Long): String {
    if (value == 0L) return "0"
    val s = value.toString()
    val out = StringBuilder()
    var count = 0
    for (i in s.indices.reversed()) {
        out.append(s[i])
        count++
        if (count % 3 == 0 && i > 0) out.append(',')
    }
    return out.reverse().toString()
}

@Composable
internal fun V2Eyebrow(
    text: String,
    color: Color = AppOnSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = color,
        modifier = modifier,
        style = v2TextStyle(11f, FontWeight.Bold, letter = 0.8f)
    )
}

@Composable
internal fun V2SectionHead(
    title: String,
    rightLabel: String? = null,
    onRightClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SwInk,
            style = v2TextStyle(15f, FontWeight.Bold, letter = -0.15f)
        )
        if (rightLabel != null) {
            Row(
                modifier = if (onRightClick != null)
                    Modifier.pressableNoIndication(onClick = onRightClick) else Modifier,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rightLabel,
                    color = AppOnSurfaceVariant,
                    style = v2TextStyle(12f, FontWeight.SemiBold)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = AppOnSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// ── Type-style helpers ──────────────────────────────────────────────────────

private fun v2TextStyle(
    sizeSp: Float,
    weight: FontWeight = FontWeight.Normal,
    letter: Float = 0f
): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    fontWeight = weight,
    letterSpacing = letter.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = TrimBoth
)

private fun v2NumStyle(sizeSp: Float, weight: FontWeight): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    fontWeight = weight,
    letterSpacing = (-0.02f * sizeSp).sp,
    fontFeatureSettings = FinanceNumFeatures,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = TrimBoth
)

// ── Category & account visual mapping ───────────────────────────────────────
//
// Maps a category/account name onto a (color, icon) pair from the v2 palette.
// Falls back to a neutral violet tile when nothing matches — keeps the UI
// consistent for user-created categories before they pick custom colors.

internal data class V2CatVisual(val color: Color, val icon: ImageVector)

@Composable
internal fun visualForCategory(name: String, iconName: String?, storedColor: Long? = null): V2CatVisual {
    val n = name.lowercase()
    val neutralViolet = if (AppIsDark) SwViolet else SwVioletSoft
    val customColor = v2StoredPaletteColor(storedColor)
    val explicitIcon = v2CategoryIconFromName(iconName)
    if (customColor != null || explicitIcon != null) {
        return V2CatVisual(
            customColor ?: when {
                "salary" in n || "bonus" in n -> SwMint
                "groc" in n || "market" in n || "shop" in n -> SwMint
                "food" in n || "dining" in n || "eat" in n || "cafe" in n || "restaurant" in n -> SwPeach
                "rent" in n || "hous" in n || "mortgage" in n -> neutralViolet
                "transit" in n || "transport" in n || "grab" in n || "car" in n || "fuel" in n -> SwSky
                "bill" in n || "util" in n || "subscription" in n || "internet" in n -> SwButter
                "entertain" in n || "fun" in n || "movie" in n || "game" in n -> SwPink
                "gift" in n -> SwCoral
                "health" in n || "medical" in n -> SwMint
                else -> neutralViolet
            },
            explicitIcon ?: Icons.Filled.Receipt
        )
    }
    return when {
        iconName == "salary" || "salary" in n || "bonus" in n -> V2CatVisual(SwMint, Icons.Filled.AutoAwesome)
        "groc" in n || "market" in n || "shop" in n -> V2CatVisual(SwMint, Icons.Filled.ShoppingCart)
        "food" in n || "dining" in n || "eat" in n || "cafe" in n || "restaurant" in n -> V2CatVisual(SwPeach, Icons.Filled.Restaurant)
        "rent" in n || "hous" in n || "mortgage" in n -> V2CatVisual(neutralViolet, Icons.Filled.Home)
        "transit" in n || "transport" in n || "grab" in n || "car" in n || "fuel" in n -> V2CatVisual(SwSky, Icons.Filled.DirectionsCar)
        "bill" in n || "util" in n || "subscription" in n || "internet" in n -> V2CatVisual(SwButter, Icons.Filled.PhoneAndroid)
        "entertain" in n || "fun" in n || "movie" in n || "game" in n -> V2CatVisual(SwPink, Icons.Filled.Movie)
        "gift" in n -> V2CatVisual(SwCoral, Icons.Filled.CardGiftcard)
        "health" in n || "medical" in n -> V2CatVisual(SwMint, Icons.Filled.Restaurant)
        else -> V2CatVisual(neutralViolet, Icons.Filled.Receipt)
    }
}

private fun v2CategoryIconFromName(iconName: String?): ImageVector? = when (iconName) {
    "restaurant", "food" -> Icons.Filled.Restaurant
    "shopping_bag", "cart", "grocery" -> Icons.Filled.ShoppingCart
    "directions_car", "car", "transport" -> Icons.Filled.DirectionsCar
    "house", "home", "rent" -> Icons.Filled.Home
    "phone", "bills" -> Icons.Filled.PhoneAndroid
    "movie", "film" -> Icons.Filled.Movie
    "gift", "favorite", "heart" -> Icons.Filled.CardGiftcard
    "spark", "salary", "bonus" -> Icons.Filled.AutoAwesome
    "account_balance_wallet", "wallet" -> Icons.Filled.AccountBalanceWallet
    "cash" -> Icons.Filled.Payments
    "card" -> Icons.Filled.CreditCard
    "receipt", "box" -> Icons.Filled.Receipt
    else -> null
}

private fun v2StoredPaletteColor(storedColor: Long?): Color? {
    if (storedColor == null) return null
    return if (storedColor in setOf(
            0xFFC5BCF8L,
            0xFFF8C99BL,
            0xFF9FE3C5L,
            0xFFF8C0D6L,
            0xFFB8D5FFL,
            0xFFFFE08AL,
            0xFFF4A39BL,
            0xFFFCE0BCL
        )
    ) Color(storedColor) else null
}

@Composable
internal fun visualForAccount(account: Account): V2CatVisual {
    return when (account.type) {
        AccountType.Cash -> V2CatVisual(SwPeach, Icons.Filled.Payments)
        AccountType.Bank -> V2CatVisual(if (AppIsDark) SwViolet else SwVioletSoft, Icons.Filled.AccountBalance)
        AccountType.EWallet -> V2CatVisual(SwMint, Icons.Filled.PhoneAndroid)
        AccountType.Credit -> V2CatVisual(
            if (AppIsDark) SwViolet else SwInk,
            Icons.Filled.CreditCard
        ).copy()
    }
}

private fun V2CatVisual.copy(): V2CatVisual = V2CatVisual(this.color, this.icon)

/** Compact bottom padding so scroll content clears the floating nav cluster. */
@Composable
internal fun v2BottomNavOverlayPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 110.dp

/**
 * Hairline divider color — ~6% alpha against ink in light, ~6% alpha against
 * white in dark. Use anywhere we'd otherwise hardcode `Color(0x0F15121F)` for
 * a list-row separator or chip outline; that hex is invisible against the
 * dark surface tone.
 */
@Composable
internal fun v2Hairline(): Color =
    if (AppIsDark) Color.White.copy(alpha = 0.06f)
    else Color(0xFF15121F).copy(alpha = 0.06f)
