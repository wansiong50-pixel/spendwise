@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.spendwise.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.analytics.MonthlySpendingSummary
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MonthlyAggregate
import android.widget.Toast
import com.spendwise.app.export.ExpenseExporter
import com.spendwise.app.domain.Budget
import com.spendwise.app.ui.theme.AppIsDark
import com.spendwise.app.ui.theme.AppOnSurface
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.AppSurfaceContainer
import com.spendwise.app.ui.theme.AppSurfaceContainerHigh
import com.spendwise.app.ui.theme.AppSurfaceLow
import com.spendwise.app.ui.theme.FinanceNumFeatures
import com.spendwise.app.ui.theme.NoFontPadding
import com.spendwise.app.ui.theme.SpendWiseMotion
import com.spendwise.app.ui.theme.swScale
import com.spendwise.app.ui.theme.perfMode
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
import com.spendwise.app.ui.theme.rememberSheetDragState
import com.spendwise.app.ui.theme.sheetDragToDismiss
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val ZONE_KL: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

// ═════════════════════════════════════════════════════════════════════════════
//  01 · DASHBOARD
//
//  1:1 port of `screens-1.jsx :: ScreenDashboard`. Composition order:
//    Header pill + circle icons → Greeting → Hero gradient pebble (with chips
//    inside) → Net cashflow row → Heatmap card → Top categories card → Recent
//    activity card. Bottom nav floats over the page from the shell.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun V2DashboardScreen(
    state: DashboardUiState,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenMonthPicker: () -> Unit,
    onOpenAccounts: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onSeeAllActivity: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val summary = state.summary
    val monthLabel = summary.month.format(DateTimeFormatter.ofPattern("MMM"))
    val yearLabel = summary.month.year.toString()
    val isDark = com.spendwise.app.ui.theme.AppIsDark

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 14.dp,
            bottom = v2BottomNavOverlayPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            // ── Header — month pill + circle icons ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 0.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .pressableNoIndication(onClick = onOpenMonthPicker),
                    color = if (isDark) AppSurface else AppSurfaceContainer,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(monthLabel, color = SwInk, style = v2T(13f, FontWeight.Bold))
                        Text(yearLabel, color = AppOnSurfaceVariant, style = v2T(13f, FontWeight.Medium))
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            null,
                            tint = SwInk,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V2CircleButton(
                        icon = Icons.Filled.Search,
                        onClick = onSeeAllActivity,
                        contentDescription = "Search transactions"
                    )
                    V2CircleButton(
                        icon = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        onClick = { onDarkModeChange(!isDarkMode) },
                        contentDescription = if (isDarkMode) "Light mode" else "Dark mode"
                    )
                    V2CircleButton(
                        icon = Icons.Filled.Settings,
                        onClick = onOpenSettings,
                        contentDescription = "Settings"
                    )
                }
            }
        }
        item {
            // ── Greeting ──────────────────────────────────────────────
            val sz = swScale
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp * sz, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = greetingFor(),
                    color = AppOnSurfaceVariant,
                    style = v2T(14f * sz, FontWeight.Medium)
                )
                Text(
                    text = "Here's where your money is.",
                    color = SwInk,
                    style = v2T(26f * sz, FontWeight.Bold, letter = -0.52f),
                    // Cap line length so wider phones (Pixel-class, 448dp+) get
                    // the same two-line headline hierarchy as compact phones,
                    // instead of collapsing to a single-line band of text.
                    modifier = Modifier.widthIn(max = 280.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item {
            // ── Hero gradient pebble ──────────────────────────────────
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp * swScale)) {
                V2HeroBalance(
                    cents = state.totalBalanceCents,
                    accountCount = state.accounts.size,
                    earnedCents = summary.totalIncomeCents,
                    spentCents = summary.totalExpenseCents,
                    onTap = onOpenAccounts
                )
            }
        }
        item {
            // ── Net cashflow row ──────────────────────────────────────
            if (summary.totalIncomeCents > 0L || summary.totalExpenseCents > 0L) {
                val sz = swScale
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 18.dp * sz, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Net cashflow · ${summary.month.format(DateTimeFormatter.ofPattern("MMM"))}",
                            color = AppOnSurfaceVariant,
                            style = v2T(13f * sz, FontWeight.Medium)
                        )
                        val net = summary.netCents
                        V2Amount(
                            cents = net,
                            size = 26f * sz,
                            sign = if (net >= 0L) '+' else null,
                            color = if (net >= 0L) SwPos else SwNeg
                        )
                    }
                    val keepPct = if (summary.totalIncomeCents > 0L) {
                        ((summary.totalIncomeCents - summary.totalExpenseCents).toFloat() /
                            summary.totalIncomeCents.toFloat() * 100f)
                            .coerceAtLeast(0f)
                            .toInt()
                    } else null
                    if (keepPct != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "You're keeping ",
                                color = AppOnSurfaceVariant,
                                style = v2T(12f, FontWeight.Medium)
                            )
                            Text(
                                "$keepPct%",
                                color = SwInk,
                                style = v2T(12f, FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
        item {
            // ── Heatmap card ──────────────────────────────────────────
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                V2Card {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        V2SectionHead(
                            title = "Spending heatmap",
                            rightLabel = summary.month.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        )
                        V2Heatmap(summary = summary)
                        V2HeatmapLegend()
                    }
                }
            }
        }
        item {
            // ── Top categories card ───────────────────────────────────
            val topCats = summary.categoryTotals.take(5)
            if (topCats.isNotEmpty()) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    V2Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            V2SectionHead(
                                title = "Where you spent",
                                rightLabel = "See all",
                                onRightClick = onSeeAllActivity
                            )
                            val total = topCats.sumOf { it.totalCents }.coerceAtLeast(1L)
                            topCats.forEach { cat ->
                                val vis = visualForCategory(cat.categoryName, cat.categoryIconName, cat.categoryColor)
                                val budget = state.budgets.find { it.categoryId == cat.categoryId }
                                V2CategoryProgressRow(
                                    name = cat.categoryName,
                                    cents = cat.totalCents,
                                    fraction = (cat.totalCents.toFloat() / total.toFloat())
                                        .coerceIn(0f, 1f),
                                    color = vis.color,
                                    icon = vis.icon,
                                    budgetLimitCents = budget?.monthlyLimitCents
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            // ── Recent activity card ──────────────────────────────────
            val recent = remember(summary) {
                (summary.recentTransactions + summary.incomeRecentTransactions)
                    .sortedByDescending { it.occurredAtMillis }
                    .take(3)
            }
            if (recent.isNotEmpty()) {
                val incomeCategoryIds = remember(state.categories) {
                    state.categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()
                }
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    V2Card {
                        Column {
                            V2SectionHead(
                                title = "Recent activity",
                                rightLabel = "See all",
                                onRightClick = onSeeAllActivity
                            )
                            Spacer(Modifier.height(8.dp))
                            recent.forEachIndexed { i, exp ->
                                if (i > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(v2Hairline())
                                    )
                                }
                                V2RecentRow(
                                    expense = exp,
                                    isIncome = exp.categoryId in incomeCategoryIds,
                                    onClick = { onEditExpense(exp.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Dashboard sub-composables ───────────────────────────────────────────────

@Composable
private fun V2HeroBalance(
    cents: Long,
    accountCount: Int,
    earnedCents: Long,
    spentCents: Long,
    onTap: () -> Unit
) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    // Hero text reads on whichever gradient is below. Light gradient is
    // pastel → use dark ink. Dark gradient is indigo → use white. Computing
    // these once here keeps the call sites below readable.
    val headlineColor = if (isDark) Color.White else Color(0xFF15121F)
    val mutedColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF15121F).copy(alpha = 0.7f)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF15121F).copy(alpha = 0.65f)
    val arrowBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.6f)
    val arrowTint = if (isDark) Color.White else SwInk

    val sz = swScale
    V2GradientHero(onClick = onTap) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 22.dp * sz,
                    end = 22.dp * sz,
                    top = 22.dp * sz,
                    bottom = 20.dp * sz
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    V2Eyebrow(text = "Total balance", color = mutedColor)
                    Spacer(Modifier.height(10.dp * sz))
                    V2Amount(
                        cents = cents,
                        size = 42f * sz,
                        color = headlineColor,
                        showDecimals = true,
                        animateValue = true
                    )
                    Spacer(Modifier.height(8.dp * sz))
                    val subtitle = when {
                        accountCount == 0 -> "Tap to add an account"
                        accountCount == 1 -> "Across 1 account · tap to manage"
                        else -> "Across $accountCount accounts · tap to manage"
                    }
                    Text(
                        text = subtitle,
                        color = subtitleColor,
                        style = v2T(13f * sz, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = arrowBg,
                    modifier = Modifier.size(40.dp),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            null,
                            tint = arrowTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (earnedCents > 0L || spentCents > 0L) {
                Spacer(Modifier.height(18.dp * sz))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (earnedCents > 0L) {
                        V2HeroChip(
                            label = "Earned",
                            cents = earnedCents,
                            positive = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (spentCents > 0L) {
                        V2HeroChip(
                            label = "Spent",
                            cents = spentCents,
                            positive = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V2HeroChip(label: String, cents: Long, positive: Boolean, modifier: Modifier = Modifier) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    // Chip glass: 55% white over pastel reads as frosted; 10% white over
    // indigo reads as glassy. The hairline border kicks in only on dark so
    // the chip doesn't visually drop into the gradient.
    val chipBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.55f)
    val labelColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF15121F).copy(alpha = 0.65f)
    // Spent on dark uses pure white (the gradient is the warm context); on
    // light it uses ink because white-on-pastel is unreadable.
    val amountColor = when {
        positive -> SwPos
        isDark -> Color.White
        else -> SwInk
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(chipBg)
            .then(
                if (isDark) Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            V2Eyebrow(text = label, color = labelColor)
            V2Amount(
                cents = cents,
                size = 16f,
                sign = if (positive) '+' else '−',
                color = amountColor,
                showDecimals = false
            )
        }
    }
}

@Composable
private fun V2CategoryProgressRow(
    name: String,
    cents: Long,
    fraction: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    budgetLimitCents: Long? = null
) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    val limit = budgetLimitCents ?: 0L
    val hasBudget = limit > 0L
    val ratio = if (hasBudget) cents.toFloat() / limit.toFloat() else 0f
    
    // Choose progress bar color based on budget breach
    val progressBarColor = when {
        hasBudget && ratio >= 1.0f -> SwNeg // Red for 100%+
        hasBudget && ratio >= 0.8f -> SwPeach // Amber/Orange for 80%-100%
        else -> if (isDark) SwViolet else SwInk // Claude dark uses violet progress on aubergine.
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        V2Tile(color = color, icon = icon, size = 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = name,
                    color = SwInk,
                    style = v2T(14.5f, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RM ${formatRinggit(cents / 100L)}",
                        color = SwInk,
                        style = v2N(13.5f, FontWeight.Bold)
                    )
                    if (hasBudget) {
                        Text(
                            text = "of RM ${formatRinggit(limit / 100L)}",
                            color = AppOnSurfaceVariant,
                            style = v2T(11f, FontWeight.Medium)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(AppSurfaceLow)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(progressBarColor)
                )
            }
            
            // If budget breached, show a tiny helper warning subtext below
            if (hasBudget && ratio >= 0.8f) {
                val warningMsg = if (ratio >= 1.0f) "Over budget limit!" else "Approaching budget limit (80%+)"
                val warningColor = if (ratio >= 1.0f) SwNeg else SwPeach
                Text(
                    text = warningMsg,
                    color = warningColor,
                    style = v2T(10f, FontWeight.Bold, letter = 0.02f)
                )
            }
        }
    }
}

@Composable
private fun V2RecentRow(expense: Expense, isIncome: Boolean, onClick: (() -> Unit)? = null) {
    val vis = visualForCategory(expense.categoryName, expense.categoryIconName, expense.categoryColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.pressableNoIndication(onClick = onClick)
                else Modifier
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        V2Tile(color = vis.color, icon = vis.icon, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.merchant.ifBlank { expense.categoryName },
                color = SwInk,
                style = v2T(14.5f, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${expense.categoryName} · ${relativeDayLabel(expense.occurredAtMillis)}",
                color = AppOnSurfaceVariant,
                style = v2T(12f, FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        V2Amount(
            cents = expense.amountCents,
            size = 14.5f,
            sign = if (isIncome) '+' else '−',
            color = if (isIncome) SwPos else SwInk,
            showDecimals = true
        )
    }
}

// ── Heatmap ─────────────────────────────────────────────────────────────────

@Composable
private fun V2Heatmap(summary: MonthlySpendingSummary) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    val ramp = listOf(SwHeat0, SwHeat1, SwHeat2, SwHeat3, SwHeat4)
    val yearMonth = summary.month
    val today = remember { LocalDate.now(ZONE_KL) }
    val isCurrent = remember(yearMonth, today) { YearMonth.from(today) == yearMonth }
    val todayDay = if (isCurrent) today.dayOfMonth else -1
    val daysInMonth = yearMonth.lengthOfMonth()
    // DayOfWeek.value: Mon=1..Sun=7; we render Sun-first columns, so offset 0
    // when the month starts on Sunday and 6 when it starts on Saturday.
    val leadingOffset = yearMonth.atDay(1).dayOfWeek.value % 7
    val maxDaily = summary.dailyTotals.values.maxOrNull() ?: 0L
    val gap = 6.dp

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { letter ->
                Text(
                    text = letter,
                    color = AppOnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = v2T(10f, FontWeight.SemiBold, letter = 0.5f)
                )
            }
        }
        val rowCount = ((leadingOffset + daysInMonth) + 6) / 7
        for (rowIdx in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                for (col in 0 until 7) {
                    val cellIdx = rowIdx * 7 + col
                    val day = cellIdx - leadingOffset + 1
                    val inMonth = day in 1..daysInMonth
                    val spend = if (inMonth) summary.dailyTotals[day] ?: 0L else 0L
                    val level = when {
                        !inMonth -> -1
                        spend <= 0L || maxDaily <= 0L -> 0
                        else -> {
                            val ratio = spend.toFloat() / maxDaily.toFloat()
                            when {
                                ratio > 0.75f -> 4
                                ratio > 0.5f -> 3
                                ratio > 0.25f -> 2
                                else -> 1
                            }
                        }
                    }
                    val color = if (level < 0) Color.Transparent else ramp[level]
                    val isToday = inMonth && day == todayDay
                    val cellShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(cellShape)
                            .background(color)
                            .then(
                                if (isToday) Modifier.border(2.dp, SwInk, cellShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        if (inMonth) {
                            // Heatmap semantic flips between modes:
                            //   light — heat4 is dark violet, so high-intensity
                            //   cells get a light label. Low cells stay dark.
                            //   dark — heat4 is bright lavender, so high cells
                            //   get a *dark* label. Low cells (which sit on
                            //   the surface tone) get a faint light label.
                            val cellTextColor = if (isDark) {
                                if (level >= 3) Color.Black.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.45f)
                            } else {
                                if (level >= 3) Color.White.copy(alpha = 0.85f)
                                else SwInk.copy(alpha = 0.5f)
                            }
                            Text(
                                text = day.toString(),
                                modifier = Modifier.padding(4.dp),
                                color = cellTextColor,
                                style = v2T(9f, FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Less", color = AppOnSurfaceVariant, style = v2T(11f, FontWeight.Medium))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(SwHeat0, SwHeat1, SwHeat2, SwHeat3, SwHeat4).forEach {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(it)
                )
            }
        }
        Text("More", color = AppOnSurfaceVariant, style = v2T(11f, FontWeight.Medium))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  02 · ACTIVITY (Transactions)
//
//  Port of screens-1.jsx :: ScreenTransactions. Back / Activity / search
//  circle header; month eyebrow + signed total; filter chip strip; day-grouped
//  pebble cards with TODAY · SUN 18 MAY eyebrows over each.
// ═════════════════════════════════════════════════════════════════════════════

private enum class V2Filter { All, Expense, Income }

@Composable
internal fun V2ActivityScreen(
    state: DashboardUiState,
    // SQL-side per-month totals for the whole ledger — the 12-month trend
    // sparklines read these, since state.expenses only holds the selected month.
    monthlyAggregates: List<MonthlyAggregate>,
    selectedMonth: YearMonth,
    onOpenMonthPicker: () -> Unit,
    selectedAccountId: Long?,
    selectedCategoryId: Long?,
    onOpenAccountPicker: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onSelectAccount: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onBack: () -> Unit,
    onSeeBreakdown: () -> Unit,
    onOpenTx: (Long) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(V2Filter.All) }
    // Search mode — magnifying-glass icon flips the header into a text input
    // that live-filters by merchant / notes / category name. Search stacks on
    // top of the other chips so users can narrow further inside an active
    // Expense / Income / Account / Category filter rather than replacing it.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(enabled = searchOpen) {
        searchOpen = false
        searchQuery = ""
    }
    val incomeIds = remember(state.categories) {
        state.categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()
    }
    // If the currently-filtered account was archived or deleted while the
    // sheet was closed, drop the filter rather than show an empty list.
    LaunchedEffect(state.accounts, selectedAccountId) {
        if (selectedAccountId != null && state.accounts.none { it.id == selectedAccountId }) {
            onSelectAccount(null)
        }
    }
    LaunchedEffect(state.categories, selectedCategoryId) {
        if (selectedCategoryId != null && state.categories.none { it.id == selectedCategoryId }) {
            onSelectCategory(null)
        }
    }
    val selectedAccountName = remember(selectedAccountId, state.accounts) {
        selectedAccountId?.let { id -> state.accounts.find { it.id == id }?.name }
    }
    val selectedCategoryName = remember(selectedCategoryId, state.categories) {
        selectedCategoryId?.let { id -> state.categories.find { it.id == id }?.name }
    }
    val searchKey = searchQuery.trim().lowercase()
    val filteredExpenses = remember(state.expenses, selectedMonth, filter, selectedAccountId, selectedCategoryId, incomeIds, searchKey) {
        state.expenses.asSequence()
            .filter {
                val ym = YearMonth.from(Instant.ofEpochMilli(it.occurredAtMillis).atZone(ZONE_KL).toLocalDate())
                ym == selectedMonth
            }
            .filter {
                when (filter) {
                    V2Filter.All -> true
                    V2Filter.Expense -> it.categoryId !in incomeIds
                    V2Filter.Income -> it.categoryId in incomeIds
                }
            }
            .filter { exp ->
                val aid = selectedAccountId
                aid == null || exp.accountId == aid
            }
            .filter { exp ->
                val cid = selectedCategoryId
                cid == null || exp.categoryId == cid
            }
            .filter { exp ->
                if (searchKey.isBlank()) true
                else exp.merchant.lowercase().contains(searchKey) ||
                    exp.notes.lowercase().contains(searchKey) ||
                    exp.categoryName.lowercase().contains(searchKey)
            }
            .sortedByDescending { it.occurredAtMillis }
            .toList()
    }
    val monthTotalCents = remember(filteredExpenses, incomeIds, filter) {
        filteredExpenses.asSequence()
            .filter { filter == V2Filter.Income || it.categoryId !in incomeIds }
            .sumOf { it.amountCents }
    }
    val monthEntries = filteredExpenses.size
    val accountNamesById = remember(state.accounts) {
        state.accounts.associate { it.id to it.name }
    }
    val activityGroups = remember(filteredExpenses, incomeIds) {
        filteredExpenses
            .groupBy {
                Instant.ofEpochMilli(it.occurredAtMillis).atZone(ZONE_KL).toLocalDate()
            }
            .toSortedMap(compareByDescending { it })
            .map { (date, rows) ->
                V2ActivityDayGroup(
                    date = date,
                    rows = rows,
                    spendCents = rows.asSequence()
                        .filter { it.categoryId !in incomeIds }
                        .sumOf { it.amountCents }
                )
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 14.dp,
            bottom = v2BottomNavOverlayPadding()
        )
    ) {
        item {
            // Header bar — flips between "Back / Activity / Search" and an
            // inline search field. Auto-focuses on open so the keyboard rises
            // without an extra tap.
            if (searchOpen) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V2CircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = {
                            searchOpen = false
                            searchQuery = ""
                            keyboard?.hide()
                        },
                        contentDescription = "Close search"
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppSurfaceContainer)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            null,
                            tint = AppOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search merchants, notes, categories",
                                    color = AppOnSurfaceVariant.copy(alpha = 0.85f),
                                    style = v2T(13f, FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = v2T(13f, FontWeight.SemiBold).copy(color = SwInk),
                                cursorBrush = SolidColor(SwInk),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SwInk.copy(alpha = 0.12f))
                                    .pressableNoIndication { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    null,
                                    tint = SwInk,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V2CircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        contentDescription = "Back"
                    )
                    Text("Activity", color = SwInk, style = v2T(15f, FontWeight.Bold))
                    V2CircleButton(
                        icon = Icons.Filled.Search,
                        onClick = { searchOpen = true },
                        contentDescription = "Search transactions"
                    )
                }
            }
        }
        item {
            // Month summary — the eyebrow is now the month-picker entry
            // point. A small chevron next to the label signals the affordance
            // without adding another chrome icon to the header.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.pressableNoIndication(onClick = onOpenMonthPicker),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    V2Eyebrow(text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        null,
                        tint = AppOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    V2Amount(
                        cents = monthTotalCents,
                        size = 36f,
                        color = SwInk,
                        animateValue = true
                    )
                    Text(
                        text = "${if (filter == V2Filter.Income) "income" else "spent"} · ${monthEntries} ${if (monthEntries == 1) "entry" else "entries"}",
                        color = AppOnSurfaceVariant,
                        style = v2T(13f, FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
        item {
            // Filter chip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                V2Pill(
                    text = "All",
                    variant = if (filter == V2Filter.All) V2PillVariant.Dark else V2PillVariant.Soft,
                    onClick = { filter = V2Filter.All }
                )
                V2Pill(
                    text = "Expense",
                    variant = if (filter == V2Filter.Expense) V2PillVariant.Dark else V2PillVariant.Soft,
                    leadingDot = SwNeg,
                    onClick = { filter = V2Filter.Expense }
                )
                V2Pill(
                    text = "Income",
                    variant = if (filter == V2Filter.Income) V2PillVariant.Dark else V2PillVariant.Soft,
                    leadingDot = SwPos,
                    onClick = { filter = V2Filter.Income }
                )
                // Account scope chip — opens a bottom-sheet picker. Dark when
                // any account is active so its "filtered" state matches the
                // visual language of the other chips; the chosen account
                // name folds into the label so the chip is self-describing
                // without having to open the sheet to remember the scope.
                V2Pill(
                    text = if (selectedAccountName != null) "Account · $selectedAccountName" else "Account",
                    leading = Icons.Filled.AccountBalance,
                    trailing = Icons.Filled.KeyboardArrowDown,
                    variant = if (selectedAccountId != null) V2PillVariant.Dark else V2PillVariant.Soft,
                    onClick = onOpenAccountPicker
                )
                V2Pill(
                    text = if (selectedCategoryName != null) "Category - $selectedCategoryName" else "Category",
                    leading = Icons.Filled.Receipt,
                    trailing = Icons.Filled.KeyboardArrowDown,
                    variant = if (selectedCategoryId != null) V2PillVariant.Dark else V2PillVariant.Soft,
                    onClick = onOpenCategoryPicker
                )
            }
        }
        // ── Filter detail card — only when Expense or Income chip is active.
        // 12-month trend + MoM delta + top categories (expense) or sources +
        // pace-projected net (income). Keys on selectedMonth/filter/expenses so
        // the heavy computation doesn't re-run while the user just scrolls.
        if (filter == V2Filter.Expense) {
            item(key = "filter-card-expense") {
                val expTrend = remember(monthlyAggregates, selectedMonth) {
                    buildMonthlyTrend(monthlyAggregates, selectedMonth, income = false)
                }
                val topExpenseCats = remember(state.expenses, state.categories, selectedMonth, incomeIds) {
                    state.expenses
                        .asSequence()
                        .filter {
                            val ym = YearMonth.from(Instant.ofEpochMilli(it.occurredAtMillis).atZone(ZONE_KL).toLocalDate())
                            ym == selectedMonth && it.categoryId !in incomeIds
                        }
                        .groupBy { it.categoryId }
                        .map { (categoryId, group) ->
                            val first = group.first()
                            com.spendwise.app.domain.CategoryTotal(
                                categoryId = categoryId,
                                categoryName = first.categoryName,
                                categoryIconName = first.categoryIconName,
                                categoryColor = first.categoryColor,
                                totalCents = group.sumOf { it.amountCents },
                                transactionCount = group.size
                            )
                        }
                        .sortedByDescending { it.totalCents }
                }
                V2ExpenseFilterCard(
                    selectedMonth = selectedMonth,
                    trend = expTrend,
                    topCategories = topExpenseCats,
                    onSeeBreakdown = onSeeBreakdown
                )
            }
        } else if (filter == V2Filter.Income) {
            item(key = "filter-card-income") {
                val incTrend = remember(monthlyAggregates, selectedMonth) {
                    buildMonthlyTrend(monthlyAggregates, selectedMonth, income = true)
                }
                val incomeSources = remember(state.expenses, state.categories, selectedMonth, incomeIds) {
                    state.expenses
                        .asSequence()
                        .filter {
                            val ym = YearMonth.from(Instant.ofEpochMilli(it.occurredAtMillis).atZone(ZONE_KL).toLocalDate())
                            ym == selectedMonth && it.categoryId in incomeIds
                        }
                        .groupBy { it.categoryId }
                        .map { (categoryId, group) ->
                            val first = group.first()
                            com.spendwise.app.domain.CategoryTotal(
                                categoryId = categoryId,
                                categoryName = first.categoryName,
                                categoryIconName = first.categoryIconName,
                                categoryColor = first.categoryColor,
                                totalCents = group.sumOf { it.amountCents },
                                transactionCount = group.size
                            )
                        }
                        .sortedByDescending { it.totalCents }
                }
                // Pace projection — extrapolate this month's net (income −
                // expense) to a full month. Null if we have no data so the
                // insight strip stays hidden rather than reading "you'll keep
                // RM 0".
                val paceNet = remember(state.expenses, selectedMonth, incomeIds) {
                    val today = LocalDate.now(ZONE_KL)
                    val isCurrentMonth = YearMonth.from(today) == selectedMonth
                    val daysInMonth = selectedMonth.lengthOfMonth()
                    val daysElapsed = if (isCurrentMonth) today.dayOfMonth.coerceAtLeast(1) else daysInMonth
                    val monthExpenses = state.expenses.filter {
                        YearMonth.from(Instant.ofEpochMilli(it.occurredAtMillis).atZone(ZONE_KL).toLocalDate()) == selectedMonth
                    }
                    val incomeSoFar = monthExpenses.filter { it.categoryId in incomeIds }.sumOf { it.amountCents }
                    val expenseSoFar = monthExpenses.filter { it.categoryId !in incomeIds }.sumOf { it.amountCents }
                    if (incomeSoFar == 0L && expenseSoFar == 0L) null
                    else {
                        val netSoFar = incomeSoFar - expenseSoFar
                        // Linear extrapolation — simple and roughly right; a
                        // smarter projection would account for recurring bills
                        // due later in the month but that's a separate ticket.
                        (netSoFar.toDouble() / daysElapsed.toDouble() * daysInMonth.toDouble()).toLong()
                    }
                }
                V2IncomeFilterCard(
                    selectedMonth = selectedMonth,
                    trend = incTrend,
                    sources = incomeSources,
                    paceProjectedNetCents = paceNet
                )
            }
        }
        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val monthLabel = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM"))
                    val message = if (searchKey.isNotBlank())
                        "Nothing matches \"${searchQuery.trim()}\" in $monthLabel."
                    else
                        "Nothing in $monthLabel yet."
                    Text(
                        text = message,
                        color = AppOnSurfaceVariant,
                        style = v2T(14f, FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            activityGroups.forEach { group ->
                val date = group.date
                val rows = group.rows
                item(key = "head-$date") {
                    val today = LocalDate.now(ZONE_KL)
                    val label = when (date) {
                        today -> "Today · ${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))}"
                        today.minusDays(1) -> "Yesterday · ${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))}"
                        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label.uppercase(),
                            color = AppOnSurfaceVariant,
                            style = v2T(12f, FontWeight.Bold, letter = 0.4f)
                        )
                        Text(
                            text = "RM ${formatRinggit(group.spendCents / 100L)}.${"%02d".format(group.spendCents % 100L)}",
                            color = AppOnSurfaceVariant,
                            style = v2N(12f, FontWeight.Bold)
                        )
                    }
                }
                item(key = "card-$date") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)) {
                        V2Card(padding = 6.dp) {
                            Column {
                                rows.forEachIndexed { i, exp ->
                                    if (i > 0) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(v2Hairline())
                                        )
                                    }
                                    V2ActivityRow(
                                        expense = exp,
                                        isIncome = exp.categoryId in incomeIds,
                                        accountName = accountNamesById[exp.accountId] ?: "Account",
                                        onClick = { onOpenTx(exp.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    }
}

private data class V2ActivityDayGroup(
    val date: LocalDate,
    val rows: List<Expense>,
    val spendCents: Long
)

@Composable
private fun V2ActivityRow(
    expense: Expense,
    isIncome: Boolean,
    accountName: String,
    onClick: () -> Unit
) {
    val vis = visualForCategory(expense.categoryName, expense.categoryIconName, expense.categoryColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        V2Tile(color = vis.color, icon = vis.icon, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.merchant.ifBlank { expense.categoryName },
                color = SwInk,
                style = v2T(14.5f, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${expense.categoryName} · $accountName",
                color = AppOnSurfaceVariant,
                style = v2T(12f, FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        V2Amount(
            cents = expense.amountCents,
            size = 14.5f,
            sign = if (isIncome) '+' else '−',
            color = if (isIncome) SwPos else SwInk,
            showDecimals = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sparkline — a 12-month trend line with a soft gradient fill underneath. The
// stroke uses the ink color so it reads "neutral data"; the fill gradient
// takes the accent color (coral for expense, pos for income) at 35% top → 0%
// bottom so the area cue is felt without overpowering the line.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun V2Sparkline(
    points: List<Long>,
    strokeColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return
    val maxV = points.max().coerceAtLeast(1L)
    val minV = points.min()
    val range = (maxV - minV).coerceAtLeast(1L).toFloat()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 4f
        val padBot = 4f
        val usableH = (h - padTop - padBot).coerceAtLeast(1f)
        val step = w / (points.size - 1).coerceAtLeast(1).toFloat()
        val ys = points.map { v ->
            val norm = (v - minV).toFloat() / range
            padTop + (1f - norm) * usableH
        }
        val line = Path().apply {
            moveTo(0f, ys.first())
            for (i in 1 until ys.size) lineTo(i * step, ys[i])
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor.copy(alpha = 0.45f), fillColor.copy(alpha = 0f)),
                startY = 0f,
                endY = h
            )
        )
        drawPath(
            path = line,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        // Endpoint dot — anchors the eye on the latest month.
        drawCircle(
            color = strokeColor,
            radius = 3.dp.toPx(),
            center = Offset(w, ys.last())
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter detail cards — expand above the list when the Expense or Income chip
// is active. Shared structure: gradient hero (total + MoM delta + sparkline),
// inline body (top cats / sources), footer actions or insight strip.
// ─────────────────────────────────────────────────────────────────────────────

private data class V2TrendData(
    val currentCents: Long,
    val previousCents: Long,
    val trend: List<Long>
) {
    val deltaPct: Int?
        get() {
            if (previousCents <= 0L) return null
            val delta = (currentCents - previousCents).toDouble() / previousCents.toDouble() * 100.0
            return delta.toInt()
        }
}

private fun buildMonthlyTrend(
    aggregates: List<MonthlyAggregate>,
    endMonth: YearMonth,
    income: Boolean,
    months: Int = 12
): V2TrendData {
    val totalsByMonth = aggregates.associate {
        it.month to (if (income) it.incomeCents else it.expenseCents)
    }
    val series = (0 until months).map { offset ->
        val ym = endMonth.minusMonths((months - 1 - offset).toLong())
        totalsByMonth[ym] ?: 0L
    }
    val current = totalsByMonth[endMonth] ?: 0L
    val previous = totalsByMonth[endMonth.minusMonths(1)] ?: 0L
    return V2TrendData(current, previous, series)
}

@Composable
private fun V2ExpenseFilterCard(
    selectedMonth: YearMonth,
    trend: V2TrendData,
    topCategories: List<com.spendwise.app.domain.CategoryTotal>,
    onSeeBreakdown: () -> Unit
) {
    val monthLabel = selectedMonth.format(DateTimeFormatter.ofPattern("MMM"))
    val total = topCategories.sumOf { it.totalCents }.coerceAtLeast(1L)
    val isDark = AppIsDark
    // Hero painted on dark canvas in dark mode (wine) vs warm pastel in light (peach→coral),
    // matching the v2 redesign reference. Text/sparkline switch to white in dark for legibility.
    val heroGradient = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF3A1F2D), Color(0xFF6A2A55)))
    } else {
        Brush.linearGradient(colors = listOf(SwPeachSoft, SwCoral.copy(alpha = 0.9f)))
    }
    val heroDecorationColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f)
    val onHeroPrimary = if (isDark) Color.White else SwInk
    val onHeroMuted = if (isDark) Color.White.copy(alpha = 0.7f) else SwInk.copy(alpha = 0.7f)
    val sparkColor = if (isDark) Color.White else SwInk
    val progressColor = if (isDark) SwViolet else SwInk
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
        V2Card(padding = 0.dp) {
            Column {
                // Hero strip — theme-aware gradient + corner circle decoration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(heroGradient)
                ) {
                    // Decorative top-right circle (130dp, offset off-card for a soft arc edge)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(heroDecorationColor)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SwNeg)
                                )
                                Text(
                                    text = "EXPENSE · $monthLabel".uppercase(),
                                    color = onHeroMuted,
                                    style = v2T(11f, FontWeight.Bold, letter = 0.6f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            V2Amount(cents = trend.currentCents, size = 30f, sign = '−', color = onHeroPrimary)
                            Spacer(Modifier.height(6.dp))
                            V2DeltaRow(
                                deltaPct = trend.deltaPct,
                                previousCents = trend.previousCents,
                                lowerIsBetter = true,
                                previousLabel = "vs ${selectedMonth.minusMonths(1).format(DateTimeFormatter.ofPattern("MMM"))}",
                                onHeroMutedColor = onHeroMuted
                            )
                        }
                        V2Sparkline(
                            points = trend.trend,
                            strokeColor = sparkColor,
                            fillColor = sparkColor,
                            modifier = Modifier
                                .width(110.dp)
                                .height(50.dp)
                        )
                    }
                }
                // Top expense categories
                if (topCategories.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)) {
                        Text(
                            "TOP EXPENSE CATEGORIES",
                            color = AppOnSurfaceVariant,
                            style = v2T(11f, FontWeight.Bold, letter = 0.6f)
                        )
                        Spacer(Modifier.height(12.dp))
                        topCategories.take(4).forEach { cat ->
                            val vis = visualForCategory(cat.categoryName, cat.categoryIconName, cat.categoryColor)
                            V2FilterMiniRow(
                                name = cat.categoryName,
                                cents = cat.totalCents,
                                color = vis.color,
                                icon = vis.icon,
                                fraction = (cat.totalCents.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                                progressColor = progressColor
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                // Footer actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(v2Hairline().copy(alpha = 0.0f))
                        .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V2FilterFooterButton(
                        text = "See breakdown",
                        icon = Icons.Filled.BarChart,
                        background = AppSurfaceContainer,
                        foreground = SwInk,
                        modifier = Modifier.weight(1f),
                        onClick = onSeeBreakdown
                    )
                }
            }
        }
    }
}

@Composable
private fun V2IncomeFilterCard(
    selectedMonth: YearMonth,
    trend: V2TrendData,
    sources: List<com.spendwise.app.domain.CategoryTotal>,
    paceProjectedNetCents: Long?
) {
    val monthLabel = selectedMonth.format(DateTimeFormatter.ofPattern("MMM"))
    val isDark = AppIsDark
    val heroGradient = if (isDark) {
        Brush.linearGradient(colors = listOf(Color(0xFF1F3A2D), Color(0xFF2A6A55)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFD9F5E6), SwMint))
    }
    val heroDecorationColor = if (isDark) SwMint.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.35f)
    val onHeroPrimary = if (isDark) Color.White else SwInk
    val onHeroMuted = if (isDark) Color.White.copy(alpha = 0.7f) else SwInk.copy(alpha = 0.7f)
    val sparkStroke = if (isDark) SwMint else SwInk
    val sparkFill = if (isDark) SwMint else SwPos
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
        V2Card(padding = 0.dp) {
            Column {
                // Hero strip — theme-aware (dark green in dark, cream→mint in light)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(heroGradient)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(heroDecorationColor)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SwPos)
                                )
                                Text(
                                    text = "INCOME · $monthLabel".uppercase(),
                                    color = onHeroMuted,
                                    style = v2T(11f, FontWeight.Bold, letter = 0.6f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            V2Amount(cents = trend.currentCents, size = 30f, sign = '+', color = onHeroPrimary)
                            Spacer(Modifier.height(6.dp))
                            V2DeltaRow(
                                deltaPct = trend.deltaPct,
                                previousCents = trend.previousCents,
                                lowerIsBetter = false,
                                previousLabel = "vs ${selectedMonth.minusMonths(1).format(DateTimeFormatter.ofPattern("MMM"))}",
                                onHeroMutedColor = onHeroMuted
                            )
                        }
                        V2Sparkline(
                            points = trend.trend,
                            strokeColor = sparkStroke,
                            fillColor = sparkFill,
                            modifier = Modifier
                                .width(110.dp)
                                .height(50.dp)
                        )
                    }
                }
                // Income sources
                if (sources.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "INCOME SOURCES",
                                color = AppOnSurfaceVariant,
                                style = v2T(11f, FontWeight.Bold, letter = 0.6f)
                            )
                            Text(
                                "${sources.size} streams",
                                color = SwPos,
                                style = v2T(11f, FontWeight.Bold)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        sources.take(4).forEach { src ->
                            val vis = visualForCategory(src.categoryName, src.categoryIconName, src.categoryColor)
                            V2IncomeSourceRow(name = src.categoryName, cents = src.totalCents, color = vis.color, icon = vis.icon)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                // Insight footer — pace-projected net cashflow this month
                if (paceProjectedNetCents != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppSurfaceLow)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            null,
                            tint = SwViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        val verb = if (paceProjectedNetCents >= 0L) "keep" else "be in the red"
                        val amountStr = "RM ${formatRinggit(kotlin.math.abs(paceProjectedNetCents) / 100L)}"
                        Text(
                            buildString {
                                append("At this pace you'll ")
                                append(verb)
                                append(" ")
                                append(amountStr)
                                append(" this month.")
                            },
                            color = AppOnSurface,
                            style = v2T(12f, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V2DeltaRow(
    deltaPct: Int?,
    previousCents: Long,
    lowerIsBetter: Boolean,
    previousLabel: String,
    onHeroMutedColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (deltaPct != null) {
            val isFavorable = if (lowerIsBetter) deltaPct < 0 else deltaPct > 0
            val chipColor = if (isFavorable) SwPos else SwNeg
            val chipBg = chipColor.copy(alpha = 0.18f)
            val arrow = if (deltaPct < 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(arrow, null, tint = chipColor, modifier = Modifier.size(11.dp))
                Text(
                    "${kotlin.math.abs(deltaPct)}%",
                    color = chipColor,
                    style = v2N(11f, FontWeight.ExtraBold)
                )
            }
            Text(
                "$previousLabel · RM ${formatRinggit(previousCents / 100L)}",
                color = onHeroMutedColor.copy(alpha = onHeroMutedColor.alpha * 0.93f),
                style = v2T(12f, FontWeight.Medium)
            )
        } else {
            Text(
                "no prior month to compare",
                color = onHeroMutedColor.copy(alpha = onHeroMutedColor.alpha * 0.78f),
                style = v2T(12f, FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun V2FilterMiniRow(
    name: String,
    cents: Long,
    color: Color,
    icon: ImageVector,
    fraction: Float,
    progressColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        V2Tile(color = color, icon = icon, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    name,
                    color = SwInk,
                    style = v2T(13f, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    "RM ${formatRinggit(cents / 100L)}",
                    color = SwInk,
                    style = v2N(12.5f, FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(AppSurfaceLow)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(progressColor)
                )
            }
        }
    }
}

@Composable
private fun V2IncomeSourceRow(name: String, cents: Long, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        V2Tile(color = color, icon = icon, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = SwInk, style = v2T(13f, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            "+ RM ${formatRinggit(cents / 100L)}",
            color = SwPos,
            style = v2N(12.5f, FontWeight.Bold)
        )
    }
}

@Composable
private fun V2FilterFooterButton(
    text: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .pressableNoIndication(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = foreground, modifier = Modifier.size(14.dp))
        Text(text, color = foreground, style = v2T(12.5f, FontWeight.Bold))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transaction detail sheet — tapping a row in Activity opens this before edit.
// Edit is intentionally the secondary action; the primary moment is "did I
// really spend that?" — backed by a hero block, the structured detail rows,
// the note, and (for expenses) a mini-history at the same merchant so the
// user can sanity-check whether this amount is normal.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun V2TxDetailSheet(
    visible: Boolean,
    expense: Expense?,
    state: DashboardUiState,
    // Async lookup for the history strip (same merchant / same income
    // category) — a bounded SQL query, since state.expenses only covers the
    // selected month and history spans the whole ledger.
    loadHistory: suspend (Expense, Boolean) -> List<Expense>,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Keep the last-known expense while the sheet animates out so the body
    // doesn't blank to "loading" mid-exit. Cleared via DisposableEffect when
    // visibility flips back on with a different id (handled below by remember
    // keying on expense.id).
    if (expense == null && !visible) return
    val current = expense ?: return
    val incomeIds = remember(state.categories) {
        state.categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()
    }
    val isIncome = current.categoryId in incomeIds
    val accountName = state.accounts.find { it.id == current.accountId }?.name ?: "Account"
    // Seeded with just the open row so the sheet renders instantly; the full
    // strip arrives one frame later from the DB.
    val merchantHistory by produceState(
        initialValue = listOf(current),
        current.id, isIncome
    ) {
        value = loadHistory(current, isIncome)
    }
    val vis = visualForCategory(current.categoryName, current.categoryIconName, current.categoryColor)
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    val heroBrush = when {
        isIncome && isDark -> Brush.linearGradient(
            listOf(Color(0xFF1F3A2D), Color(0xFF2A6A55))
        )
        isIncome -> Brush.linearGradient(listOf(Color(0xFFD9F5E6), SwMint))
        isDark -> Brush.linearGradient(
            listOf(Color(0xFF3A1F2D), Color(0xFF5238A6))
        )
        else -> Brush.linearGradient(listOf(SwPeachSoft, SwCoral.copy(alpha = 0.9f)))
    }
    val sign = if (isIncome) '+' else '−'
    val amountColor = if (isIncome) SwPos else if (isDark) Color.White else SwInk
    val dotColor = if (isIncome) SwPos else SwNeg
    val labelKind = if (isIncome) "INCOME" else "EXPENSE"
    val merchantTitle = current.merchant.ifBlank { current.categoryName }
    val whenText = remember(current.occurredAtMillis) {
        val zdt = Instant.ofEpochMilli(current.occurredAtMillis).atZone(ZONE_KL)
        zdt.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
    val context = LocalContext.current
    val shareText = remember(current.id, current.amountCents, current.notes, merchantTitle, isIncome, whenText, accountName) {
        val amount = "RM ${formatRinggit(current.amountCents / 100L)}.${"%02d".format(current.amountCents % 100L)}"
        listOf(
            merchantTitle,
            "${if (isIncome) "+" else "-"} $amount",
            "$whenText · $accountName",
            current.notes.takeIf { it.isNotBlank() }
        ).filterNotNull().joinToString("\n")
    }

    // Gesture-driven dismissal: the sheet tracks the finger, release velocity
    // carries through, and the scrim dims in lockstep with the drag progress
    // (read in the draw phase, so dragging never recomposes the tree).
    val drag = rememberSheetDragState(visible)
    val scrimBaseAlpha = if (isDark) 0.50f else 0.45f
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseStandard)),
        exit = fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseStandard))
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = scrimBaseAlpha * (1f - drag.progress)))
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseDrawer),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseDrawer),
                    targetOffsetY = { it }
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sheetDragToDismiss(drag, onDismiss)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = AppSurface,
                    shadowElevation = if (isDark) 24.dp else 0.dp,
                    tonalElevation = 0.dp
                ) {
            // Cap the body's scroll height so Delete / Edit transaction stay
            // pinned at the bottom of the sheet no matter how busy the body
            // gets (long note + 4-row history).
            val bodyMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.55f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Grabber (fixed)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppSurfaceContainer)
                    )
                }
                // Top action bar mirrors the Claude sheet: close on the left,
                // share and overflow actions on the right.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V2CircleButton(
                        icon = Icons.Filled.Close,
                        onClick = onDismiss,
                        contentDescription = "Close",
                        size = 38.dp,
                        bg = if (isDark) AppSurfaceLow else null
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V2CircleButton(
                            icon = Icons.AutoMirrored.Filled.Send,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share transaction"))
                            },
                            contentDescription = "Share transaction",
                            size = 38.dp,
                            bg = if (isDark) AppSurfaceLow else null
                        )
                        V2CircleButton(
                            icon = Icons.Filled.MoreHoriz,
                            onClick = {
                                Toast.makeText(context, "More transaction actions coming soon", Toast.LENGTH_SHORT).show()
                            },
                            contentDescription = "More actions",
                            size = 38.dp,
                            bg = if (isDark) AppSurfaceLow else null
                        )
                    }
                }
                // ── Scrollable body ─────────────────────────────────────────
                // heightIn(max=...) caps the body so the action row stays at
                // the bottom of the sheet; when the body content is shorter
                // than that cap the Column hugs its content. When longer,
                // verticalScroll lets the user reach everything.
                Column(
                    modifier = Modifier
                        .heightIn(max = bodyMaxHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                // Hero block
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(heroBrush)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.08f else 0.35f))
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(vis.icon, null, tint = if (isDark) Color.White else SwInk, modifier = Modifier.size(28.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Text(
                                        "$labelKind · ${expense.categoryName}".uppercase(),
                                        color = if (isDark) Color.White.copy(alpha = 0.70f) else SwInk.copy(alpha = 0.65f),
                                        style = v2T(11f, FontWeight.Bold, letter = 0.6f)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    merchantTitle,
                                    color = if (isDark) Color.White else SwInk,
                                    style = v2T(18f, FontWeight.ExtraBold, letter = -0.18f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        V2Amount(cents = expense.amountCents, size = 36f, sign = sign, color = amountColor)
                    }
                }
                // Detail rows
                Box(
                    modifier = Modifier
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppSurfaceLow)
                ) {
                    Column {
                        V2DetailRow(icon = Icons.Filled.CalendarToday, label = "When", value = whenText, showDivider = false)
                        V2DetailRow(icon = Icons.Filled.AccountBalance, label = "Account", value = accountName, showDivider = true)
                        if (isIncome && merchantHistory.size > 1) {
                            // Recurring hint — if user logs the same income category multiple times, show cadence
                            val sortedTimes = merchantHistory.map { it.occurredAtMillis }.sorted()
                            val avgGapDays = if (sortedTimes.size >= 2) {
                                val gaps = sortedTimes.zipWithNext { a, b -> (b - a) / 86_400_000L }
                                if (gaps.isEmpty()) null else gaps.average().toLong()
                            } else null
                            if (avgGapDays != null && avgGapDays in 20..40) {
                                val nextDate = Instant.ofEpochMilli(sortedTimes.last())
                                    .atZone(ZONE_KL)
                                    .toLocalDate()
                                    .plusDays(avgGapDays)
                                V2DetailRow(
                                    icon = Icons.Filled.Repeat,
                                    label = "Recurring",
                                    value = "Monthly · next ${nextDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}",
                                    showDivider = true
                                )
                            }
                        }
                    }
                }
                // Note
                if (expense.notes.isNotBlank()) {
                    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
                        Text("NOTE", color = AppOnSurfaceVariant, style = v2T(11f, FontWeight.Bold, letter = 0.6f))
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppSurfaceLow)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                expense.notes,
                                color = if (isDark) Color(0xFFC9C4DC) else AppOnSurface,
                                style = v2T(13.5f, FontWeight.Medium)
                            )
                        }
                    }
                }
                // Merchant history (expense only) — last few entries at the same merchant
                if (!isIncome && merchantHistory.size > 1) {
                    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
                        val firstWord = merchantTitle.split(' ').firstOrNull() ?: merchantTitle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "HISTORY AT $firstWord".uppercase(),
                                color = AppOnSurfaceVariant,
                                style = v2T(11f, FontWeight.Bold, letter = 0.6f)
                            )
                            Text(
                                "${merchantHistory.size} total",
                                color = AppOnSurfaceVariant,
                                style = v2T(11f, FontWeight.SemiBold)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppSurfaceLow)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Column {
                                merchantHistory.take(4).forEachIndexed { i, h ->
                                    if (i > 0) {
                                        Box(
                                            Modifier.fillMaxWidth().height(1.dp).background(v2Hairline())
                                        )
                                    }
                                    val isCurrent = h.id == expense.id
                                    val date = Instant.ofEpochMilli(h.occurredAtMillis).atZone(ZONE_KL).toLocalDate()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            date.format(DateTimeFormatter.ofPattern("d MMM")),
                                            color = if (isCurrent) SwInk else AppOnSurface,
                                            style = v2T(12.5f, if (isCurrent) FontWeight.Bold else FontWeight.SemiBold)
                                        )
                                        Text(
                                            "RM ${formatRinggit(h.amountCents / 100L)}.${"%02d".format(h.amountCents % 100L)}",
                                            color = if (isCurrent) SwInk else AppOnSurfaceVariant,
                                            style = v2N(12.5f, if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                } // end scrollable body
                // Actions (pinned at the bottom of the sheet)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V2SheetActionButton(
                        text = "Delete",
                        icon = Icons.Filled.Delete,
                        background = AppSurfaceLow,
                        foreground = SwNeg,
                        weight = 1f,
                        onClick = { onDelete(current.id) }
                    )
                        V2SheetActionButton(
                            text = "Edit transaction",
                            icon = Icons.Filled.Edit,
                            background = if (isDark) SwViolet else SwInk,
                            foreground = Color.White,
                            weight = 2f,
                            onClick = { onEdit(current.id) }
                    )
                }
                    } // end inner Column
                } // end Surface
            } // end inner AnimatedVisibility (slide)
        } // end scrim Box
    } // end outer AnimatedVisibility (fade)
}

@Composable
private fun V2DetailRow(icon: ImageVector, label: String, value: String, showDivider: Boolean) {
    Column {
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(v2Hairline()))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
                label,
                color = AppOnSurfaceVariant,
                style = v2T(12f, FontWeight.SemiBold),
                modifier = Modifier.width(78.dp)
            )
            Text(
                value,
                color = SwInk,
                style = v2T(13.5f, FontWeight.Bold),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.V2SheetActionButton(
    text: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    weight: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(weight)
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .pressableNoIndication(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = foreground, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = foreground, style = v2T(13.5f, FontWeight.Bold))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  03 · INSIGHTS (Breakdown)
//
//  Port of screens-1.jsx :: ScreenBreakdown. Year display + Change pill, dark
//  cashflow card with 12 monthly bar pairs and a highlighted month, Spend /
//  Income segmented toggle, donut card with 4-item legend, and a category
//  list pebble card.
// ═════════════════════════════════════════════════════════════════════════════

internal enum class InsightsCategoryMode { Spend, Income }

private data class V2InsightsYearSummary(
    val expenseMonthly: LongArray,
    val incomeMonthly: LongArray,
    val totalExpense: Long,
    val totalIncome: Long
)

@Composable
internal fun V2InsightsScreen(
    state: DashboardUiState,
    // The selected year's expense window (SQL-scoped by the ViewModel) —
    // state.expenses only carries the dashboard's selected month.
    yearExpenses: List<Expense>,
    prevYearHasData: Boolean,
    selectedYear: Int,
    onOpenYearPicker: () -> Unit,
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit
) {
    val today = remember { LocalDate.now(ZONE_KL) }
    var selectedMonth by rememberSaveable { mutableStateOf(today.monthValue) }
    var mode by rememberSaveable { mutableStateOf(InsightsCategoryMode.Spend) }
    val incomeIds = remember(state.categories) {
        state.categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()
    }

    val yearSummary = remember(yearExpenses, incomeIds) {
        val expenseMonthly = LongArray(12)
        val incomeMonthly = LongArray(12)
        yearExpenses.forEach { exp ->
            val monthIndex = Instant.ofEpochMilli(exp.occurredAtMillis)
                .atZone(ZONE_KL)
                .toLocalDate()
                .monthValue - 1
            if (exp.categoryId in incomeIds) incomeMonthly[monthIndex] += exp.amountCents
            else expenseMonthly[monthIndex] += exp.amountCents
        }
        V2InsightsYearSummary(
            expenseMonthly = expenseMonthly,
            incomeMonthly = incomeMonthly,
            totalExpense = expenseMonthly.sum(),
            totalIncome = incomeMonthly.sum()
        )
    }
    val expenseMonthly = yearSummary.expenseMonthly
    val incomeMonthly = yearSummary.incomeMonthly
    val totalExpense = yearSummary.totalExpense
    val totalIncome = yearSummary.totalIncome
    val net = totalIncome - totalExpense

    LaunchedEffect(selectedYear, selectedMonth, today) {
        val coerced = coerceInsightsMonthForYear(selectedYear, selectedMonth, today)
        if (coerced != selectedMonth) selectedMonth = coerced
    }
    val selectedYearMonth = remember(selectedYear, selectedMonth) {
        YearMonth.of(selectedYear, selectedMonth)
    }
    val activeCategoryTotals = remember(yearExpenses, selectedYear, selectedMonth, incomeIds, mode) {
        insightsCategoryTotalsForMonth(
            expenses = yearExpenses,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            incomeCategoryIds = incomeIds,
            mode = mode
        )
    }
    val activeCategoryTotalCents = remember(activeCategoryTotals) {
        activeCategoryTotals.sumOf { it.totalCents }.coerceAtLeast(1L)
    }
    val ctx = LocalContext.current
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    val activeCategoryCount = remember(state.categories) {
        state.categories.count { !it.isIncomeAdjustment }
    }

    // Low-spec devices skip the page blur entirely — the scrim alpha below
    // already focuses attention on the menu, and blur is the most expensive
    // GPU op in this composable.
    val skipBlur = perfMode.lowSpec
    val pageBlur by animateDpAsState(
        targetValue = if (menuOpen && !skipBlur) {
            if (isDark) 2.dp else 12.dp
        } else 0.dp,
        animationSpec = tween(SpendWiseMotion.Tooltip, easing = SpendWiseMotion.EaseStandard),
        label = "insightsBlur"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (menuOpen) {
            if (isDark) 0.5f else 0.28f
        } else 0f,
        animationSpec = tween(SpendWiseMotion.Tooltip, easing = SpendWiseMotion.EaseStandard),
        label = "insightsScrim"
    )
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // Modifier.blur is a no-op below API 31 — devices without
            // RenderEffect support fall back to the dim scrim alone, which
            // still focuses attention on the menu.
            .blur(pageBlur),
        contentPadding = PaddingValues(
            top = 14.dp,
            bottom = v2BottomNavOverlayPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V2CircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back"
                )
                Text("Insights", color = SwInk, style = v2T(15f, FontWeight.Bold))
                Box {
                    V2CircleButton(
                        icon = Icons.Filled.MoreHoriz,
                        onClick = { onMenuOpenChange(true) },
                        contentDescription = "More",
                        bg = if (isDark && menuOpen) SwViolet else null,
                        tint = if (isDark && menuOpen) Color.White else null
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { onMenuOpenChange(false) },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AppSurface,
                        shadowElevation = if (isDark) 18.dp else 8.dp,
                        border = if (isDark)
                            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        else null,
                        modifier = Modifier.widthIn(min = 240.dp)
                    ) {
                        V2InsightsMenuItem(
                            icon = Icons.Filled.Download,
                            title = "Export as CSV",
                            subtitle = "$selectedYear · all categories",
                            onClick = {
                                onMenuOpenChange(false)
                                val result = ExpenseExporter.exportYear(
                                    context = ctx,
                                    year = selectedYear,
                                    zone = ZONE_KL,
                                    expenses = yearExpenses,
                                    categories = state.categories,
                                    accounts = state.accounts
                                )
                                val message = when (result) {
                                    is ExpenseExporter.ExportResult.Shared ->
                                        "${result.rowCount} entries ready to share"
                                    ExpenseExporter.ExportResult.Empty ->
                                        "Nothing to export in $selectedYear yet"
                                    is ExpenseExporter.ExportResult.Failed ->
                                        "Couldn't export: ${result.message}"
                                }
                                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                        V2InsightsMenuItem(
                            icon = Icons.Filled.Description,
                            title = "Export as PDF",
                            subtitle = "Printable summary",
                            comingSoon = true,
                            onClick = {
                                onMenuOpenChange(false)
                                Toast.makeText(ctx, "PDF export coming soon", Toast.LENGTH_SHORT).show()
                            }
                        )
                        V2InsightsMenuItem(
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            title = "Compare years",
                            subtitle = if (prevYearHasData)
                                "${selectedYear - 1} vs $selectedYear"
                            else
                                "No data for ${selectedYear - 1}",
                            comingSoon = true,
                            enabled = prevYearHasData,
                            onClick = {
                                onMenuOpenChange(false)
                                Toast.makeText(ctx, "Year-vs-year comparison coming soon", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(1.dp)
                                .background(v2Hairline())
                        )
                        V2InsightsMenuItem(
                            icon = Icons.Filled.Savings,
                            title = "Set yearly budget",
                            subtitle = "Track this year's target",
                            comingSoon = true,
                            onClick = {
                                onMenuOpenChange(false)
                                Toast.makeText(ctx, "Yearly budgets coming soon", Toast.LENGTH_SHORT).show()
                            }
                        )
                        V2InsightsMenuItem(
                            icon = Icons.Filled.Category,
                            title = "Edit categories",
                            subtitle = "$activeCategoryCount active",
                            onClick = {
                                onMenuOpenChange(false)
                                onManageCategories()
                            }
                        )
                    }
                }
            }
        }
        item {
            // Year selector + NET YTD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Year", color = AppOnSurfaceVariant, style = v2T(13f, FontWeight.SemiBold))
                    // The whole "2026 ▾" cluster is one tap target. The pill
                    // is gated by yearsWithData.size > 1 — on a fresh install
                    // there's only ever one year, and a chevron pointing to
                    // a picker that would show one option is a lie.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.pressableNoIndication(scale = 0.97f) { onOpenYearPicker() }
                    ) {
                        Text(
                            text = selectedYear.toString(),
                            color = SwInk,
                            style = v2N(32f, FontWeight.ExtraBold)
                        )
                        V2Pill(
                            text = "Change",
                            variant = V2PillVariant.Soft,
                            trailing = Icons.Filled.KeyboardArrowDown,
                            onClick = { onOpenYearPicker() }
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    V2Eyebrow("Net YTD")
                    V2Amount(
                        cents = net,
                        size = 20f,
                        sign = if (net >= 0) '+' else null,
                        color = if (net >= 0) SwPos else SwNeg
                    )
                }
            }
        }
        item {
            // Dark cashflow card
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                V2DarkCashflowCard(
                    expenseMonthly = expenseMonthly,
                    incomeMonthly = incomeMonthly,
                    totalEarned = totalIncome,
                    totalSpent = totalExpense,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onPreviousMonth = {
                        selectedMonth = coerceInsightsMonthForYear(
                            selectedYear = selectedYear,
                            selectedMonth = selectedMonth - 1,
                            today = today
                        )
                    },
                    onNextMonth = {
                        selectedMonth = coerceInsightsMonthForYear(
                            selectedYear = selectedYear,
                            selectedMonth = selectedMonth + 1,
                            today = today
                        )
                    },
                    onSelectMonth = {
                        selectedMonth = coerceInsightsMonthForYear(selectedYear, it, today)
                    }
                )
            }
        }
        item {
            // By category + segmented Spend/Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By category - ${selectedYearMonth.format(DateTimeFormatter.ofPattern("MMM"))}",
                    color = SwInk,
                    style = v2T(15f, FontWeight.Bold, letter = -0.15f),
                    // Weighted so the segmented pill keeps its intrinsic size on
                    // narrow/large-font devices — the title ellipsizes instead of
                    // squeezing the pill until its labels wrap.
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 10.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppSurfaceContainer)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    V2SegBtn("Spend", mode == InsightsCategoryMode.Spend) { mode = InsightsCategoryMode.Spend }
                    V2SegBtn("Income", mode == InsightsCategoryMode.Income) { mode = InsightsCategoryMode.Income }
                }
            }
        }
        item {
            // Donut card
            if (activeCategoryTotals.isNotEmpty()) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    V2Card {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            V2Donut(
                                values = activeCategoryTotals.map { it.totalCents },
                                colors = activeCategoryTotals.mapIndexed { i, _ -> donutColor(i) },
                                size = 130.dp,
                                label = if (mode == InsightsCategoryMode.Spend) "SPEND" else "INCOME"
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                activeCategoryTotals.take(4).forEachIndexed { i, c ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(donutColor(i))
                                        )
                                        Text(
                                            text = c.categoryName,
                                            color = SwInk,
                                            style = v2T(12.5f, FontWeight.SemiBold),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${((c.totalCents.toFloat() / activeCategoryTotalCents) * 100).toInt()}%",
                                            color = AppOnSurfaceVariant,
                                            style = v2N(12f, FontWeight.Bold)
                                        )
                                    }
                                }
                                if (activeCategoryTotals.size > 4) {
                                    Text(
                                        "+${activeCategoryTotals.size - 4} more",
                                        color = AppOnSurfaceVariant,
                                        style = v2T(11f, FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (activeCategoryTotals.isNotEmpty()) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    V2Card {
                        Column {
                            activeCategoryTotals.forEachIndexed { i, c ->
                                if (i > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(v2Hairline())
                                    )
                                }
                                val vis = visualForCategory(c.categoryName, c.categoryIconName, c.categoryColor)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    V2Tile(color = vis.color, icon = vis.icon, size = 40.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            c.categoryName,
                                            color = SwInk,
                                            style = v2T(14.5f, FontWeight.SemiBold),
                                            maxLines = 1
                                        )
                                        Text(
                                            "${((c.totalCents.toFloat() / activeCategoryTotalCents) * 100).toInt()}% of ${if (mode == InsightsCategoryMode.Spend) "spend" else "income"}",
                                            color = AppOnSurfaceVariant,
                                            style = v2T(12f, FontWeight.Medium)
                                        )
                                    }
                                    Text(
                                        text = "RM ${formatRinggit(c.totalCents / 100L)}",
                                        color = SwInk,
                                        style = v2N(14.5f, FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        // Scrim sits above the page content while DropdownMenu remains a popup.
        // Click-to-dismiss mirrors onDismissRequest.
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = menuOpen,
                        onClick = { onMenuOpenChange(false) }
                    )
            )
        }
    }
}

@Composable
internal fun V2YearPickerSheet(
    visible: Boolean,
    selected: Int,
    years: List<Int>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseStandard)),
        exit = fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseStandard))
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseDrawer),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseDrawer),
                    targetOffsetY = { it }
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = AppSurface,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(AppSurfaceContainer)
                            )
                        }
                        Text("Select year", color = SwInk, style = v2T(15f, FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            years.forEach { y ->
                                val isSelected = y == selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) (if (isDark) SwViolet else SwInk) else AppSurfaceLow)
                                        .pressableNoIndication(scale = 0.985f) { onSelect(y) }
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = y.toString(),
                                        color = if (isSelected) Color.White else SwInk,
                                        style = v2N(16f, FontWeight.ExtraBold)
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(percent = 50))
                                                .background(Color.White.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3-dot menu row used by the Insights screen. Title + muted subtitle +
// leading icon, with a "Soon" pill on stubbed actions so the user knows the
// row is intentionally non-functional rather than broken.
@Composable
private fun V2InsightsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    comingSoon: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    DropdownMenuItem(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        enabled = enabled,
        text = {
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        title,
                        color = if (enabled) SwInk else SwInk.copy(alpha = 0.4f),
                        style = v2T(14f, FontWeight.SemiBold)
                    )
                    if (comingSoon) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(SwVioletSoft)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Soon",
                                color = SwViolet,
                                style = v2T(9.5f, FontWeight.ExtraBold, letter = 0.4f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = AppOnSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                    style = v2T(11.5f, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) AppSurfaceLow else AppSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (enabled) SwInk else SwInk.copy(alpha = 0.4f),
                    modifier = Modifier.size(17.dp)
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun V2SegBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) (if (isDark) SwViolet else SwInk) else Color.Transparent)
            .pressableNoIndication(scale = 0.96f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else AppOnSurfaceVariant,
            style = v2T(12f, FontWeight.Bold),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun V2CashflowMonthButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.035f))
            .pressableNoIndication(enabled = enabled, scale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 0.9f else 0.25f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun V2DarkCashflowCard(
    expenseMonthly: LongArray,
    incomeMonthly: LongArray,
    totalEarned: Long,
    totalSpent: Long,
    selectedYear: Int,
    selectedMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (Int) -> Unit
) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    val today = LocalDate.now(ZONE_KL)
    val isCurrentYear = selectedYear == today.year
    val canPreviousMonth = selectedMonth > 1
    val canNextMonth = selectedMonth < if (isCurrentYear) today.monthValue else 12
    val maxBar = maxOf(
        expenseMonthly.maxOrNull() ?: 0L,
        incomeMonthly.maxOrNull() ?: 0L,
        1L
    )
    // Light mode: flat ink card with a deep ink-tinted shadow — the card *is*
    // the loud thing on the screen. Dark mode: subtle aubergine→surface
    // gradient + 6%-white hairline; without that the card sinks into the
    // canvas because the surface tone is only one notch above the screen.
    val cardModifier = Modifier
        .fillMaxWidth()
        .let {
            if (isDark) it
            else it.shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color(0xFF15121F).copy(alpha = 0.45f),
                spotColor = Color(0xFF15121F).copy(alpha = 0.45f)
            )
        }
        .clip(RoundedCornerShape(28.dp))
        .let {
            if (isDark) it.background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF2A1F4D), Color(0xFF1A1727)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ) else it.background(Color(0xFF15121F))
        }
        .let {
            if (isDark) it.border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(28.dp)
            ) else it
        }
        .padding(22.dp)
    Box(modifier = cardModifier) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    V2Eyebrow(
                        text = "Cashflow · 12 months",
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        V2YTDStat("Earned", totalEarned, SwMint)
                        V2YTDStat("Spent", totalSpent, Color.White)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "VIEWING",
                        color = Color.White.copy(alpha = 0.55f),
                        style = v2T(10.5f, FontWeight.Bold, letter = 0.5f)
                    )
                    Text(
                        text = YearMonth.of(selectedYear, selectedMonth)
                            .format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        color = Color.White,
                        style = v2N(13.5f, FontWeight.ExtraBold)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    V2CashflowMonthButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        enabled = canPreviousMonth,
                        onClick = onPreviousMonth
                    )
                    V2CashflowMonthButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        enabled = canNextMonth,
                        onClick = onNextMonth
                    )
                }
            }
            // 12 monthly bar pairs.
            //
            // Layout choice: tooltip slot is ALWAYS reserved (transparent when
            // there's nothing to show) so the bar baselines line up across all
            // 12 columns.
            //
            // Bar widths are weighted (not fixed dp) so the chart breathes
            // edge-to-edge on any phone width.
            val tooltipSlotHeight = 36.dp
            val chartHeight = 130.dp
            val extraBottomPadding = 26.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + tooltipSlotHeight + extraBottomPadding),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (m in 0 until 12) {
                    val month = m + 1
                    val isFuture = isCurrentYear && month > today.monthValue
                    val isHl = selectedMonth == month
                    val sH = (expenseMonthly[m].toFloat() / maxBar.toFloat()).coerceIn(0f, 1f)
                    val iH = (incomeMonthly[m].toFloat() / maxBar.toFloat()).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressableNoIndication(
                                enabled = !isFuture,
                                scale = 0.96f,
                                onClick = { onSelectMonth(month) }
                            ),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Reserved tooltip slot — fixed-height, transparent on
                        // non-highlighted months. Keeps the bars below it on a
                        // shared baseline regardless of which month is current.
                        Box(
                            modifier = Modifier.height(tooltipSlotHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (isHl) {
                                Text(
                                    text = "S ${compactRinggitBare(expenseMonthly[m])} · E ${compactRinggitBare(incomeMonthly[m])}",
                                    color = Color.White,
                                    modifier = Modifier
                                        .requiredWidth(104.dp)
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = RoundedCornerShape(percent = 50),
                                            clip = false,
                                            ambientColor = SwViolet.copy(alpha = 0.4f),
                                            spotColor = SwViolet.copy(alpha = 0.4f)
                                        )
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(SwViolet)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    style = v2N(9.5f, FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // Bar pair — fills the remaining chart height. Bottom
                        // alignment is the natural Row default for `Bottom`
                        // verticalAlignment.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight - 20.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center,
                            // 3dp internal gap — tight enough to read as one
                            // "month brick" but wide enough to keep each bar
                            // distinct against the dark backdrop.
                        ) {
                            // Spend bar (left). Slightly wider than v1 (8dp
                            // vs 6dp) so the chart reads from across the room.
                            Box(
                                Modifier
                                    .weight(1f, fill = true)
                                    .padding(end = 2.dp)
                                    .fillMaxHeight(sH.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        when {
                                            isHl -> SwViolet
                                            isFuture -> Color.White.copy(alpha = 0.18f)
                                            else -> Color.White.copy(alpha = 0.8f)
                                        }
                                    )
                            )
                            // Income bar (right). Mint on selected month, dim
                            // mint on future months (data hasn't arrived yet).
                            Box(
                                Modifier
                                    .weight(1f, fill = true)
                                    .padding(start = 2.dp)
                                    .fillMaxHeight(iH.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        when {
                                            isHl -> SwMint
                                            isFuture -> SwMint.copy(alpha = 0.2f)
                                            else -> SwMint.copy(alpha = 0.55f)
                                        }
                                    )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "JFMAMJJASOND"[m].toString(),
                            color = when {
                                isHl -> Color.White
                                isFuture -> Color.White.copy(alpha = 0.25f)
                                else -> Color.White.copy(alpha = 0.5f)
                            },
                            style = v2T(10f, FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isFuture) "-" else compactRinggitBare(expenseMonthly[m]),
                            color = when {
                                isHl -> Color.White.copy(alpha = 0.85f)
                                isFuture -> Color.White.copy(alpha = 0.22f)
                                else -> Color.White.copy(alpha = 0.42f)
                            },
                            style = v2N(8.5f, FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = "▲",
                            color = if (isHl) SwViolet else Color.Transparent,
                            style = v2T(6f, FontWeight.Bold),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = "Tap a month to switch, or use the arrows above",
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = v2T(11f, FontWeight.Medium)
            )
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.85f)))
                    Text("Spent", color = Color.White.copy(alpha = 0.6f), style = v2T(11f, FontWeight.Medium))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SwMint))
                    Text("Earned", color = Color.White.copy(alpha = 0.6f), style = v2T(11f, FontWeight.Medium))
                }
            }
        }
    }
}

@Composable
private fun V2YTDStat(label: String, cents: Long, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            color = Color.White.copy(alpha = 0.55f),
            style = v2T(10.5f, FontWeight.SemiBold, letter = 0.5f)
        )
        Text(
            text = compactRinggit(cents),
            color = color,
            style = v2N(22f, FontWeight.ExtraBold)
        )
    }
}

@Composable
private fun V2Donut(values: List<Long>, colors: List<Color>, size: Dp, label: String) {
    val isDark = com.spendwise.app.ui.theme.AppIsDark
    val trackColor = if (isDark) AppSurfaceLow else Color(0xFFF1EEF7)
    val total = values.sum().coerceAtLeast(1L)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 18.dp.toPx()
            val padding = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            // Track ring
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            var startAngle = -90f
            values.forEachIndexed { i, v ->
                val sweep = (v.toFloat() / total.toFloat()) * 360f
                drawArc(
                    color = colors.getOrElse(i) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = AppOnSurfaceVariant, style = v2T(10f, FontWeight.SemiBold))
            Text(
                text = compactRinggit(total),
                color = SwInk,
                style = v2N(16f, FontWeight.ExtraBold)
            )
        }
    }
}

@Composable
private fun donutColor(index: Int): Color = listOf(
    SwViolet, SwMint, SwPeach, SwSky, SwButter, SwPink, SwCoral
)[index % 7]

private fun groupCategoryTotals(expenses: List<Expense>): List<com.spendwise.app.domain.CategoryTotal> {
    return expenses
        .groupBy { it.categoryId }
        .map { (_, group) ->
            com.spendwise.app.domain.CategoryTotal(
                categoryId = group.first().categoryId,
                categoryName = group.first().categoryName,
                totalCents = group.sumOf { it.amountCents },
                transactionCount = group.size,
                categoryIconName = group.first().categoryIconName,
                categoryColor = group.first().categoryColor
            )
        }
        .sortedByDescending { it.totalCents }
}

// ── Shared formatting helpers ────────────────────────────────────────────────

internal fun coerceInsightsMonthForYear(
    selectedYear: Int,
    selectedMonth: Int,
    today: LocalDate
): Int {
    val clamped = selectedMonth.coerceIn(1, 12)
    return if (selectedYear == today.year) clamped.coerceAtMost(today.monthValue) else clamped
}

internal fun insightsCategoryTotalsForMonth(
    expenses: List<Expense>,
    selectedYear: Int,
    selectedMonth: Int,
    incomeCategoryIds: Set<Long>,
    mode: InsightsCategoryMode
): List<com.spendwise.app.domain.CategoryTotal> {
    val month = YearMonth.of(selectedYear, selectedMonth.coerceIn(1, 12))
    return groupCategoryTotals(
        expenses.filter { expense ->
            val occurredMonth = YearMonth.from(
                Instant.ofEpochMilli(expense.occurredAtMillis)
                    .atZone(ZONE_KL)
                    .toLocalDate()
            )
            val isIncome = expense.categoryId in incomeCategoryIds
            occurredMonth == month && when (mode) {
                InsightsCategoryMode.Spend -> !isIncome
                InsightsCategoryMode.Income -> isIncome
            }
        }
    )
}

internal fun formatRinggit(ringgit: Long): String {
    val s = ringgit.toString()
    val out = StringBuilder()
    var count = 0
    for (i in s.indices.reversed()) {
        out.append(s[i])
        count++
        if (count % 3 == 0 && i > 0) out.append(',')
    }
    return out.reverse().toString()
}

private fun compactRinggit(cents: Long): String {
    val r = cents / 100L
    return when {
        r >= 1_000_000L -> "RM ${"%.1f".format(r / 1_000_000.0)}m"
        r >= 1_000L -> "RM ${"%.1f".format(r / 1_000.0)}k"
        else -> "RM ${formatRinggit(r)}"
    }
}

private fun compactRinggitBare(cents: Long): String {
    val r = cents / 100L
    return when {
        r >= 1_000_000L -> "${"%.1f".format(r / 1_000_000.0)}m"
        r >= 1_000L -> "${"%.1f".format(r / 1_000.0)}k"
        else -> formatRinggit(r)
    }
}

private fun relativeDayLabel(epochMillis: Long): String {
    val d = Instant.ofEpochMilli(epochMillis).atZone(ZONE_KL).toLocalDate()
    val today = LocalDate.now(ZONE_KL)
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZONE_KL).format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (d) {
        today -> "Today · $time"
        today.minusDays(1) -> "Yesterday · $time"
        else -> "${d.format(DateTimeFormatter.ofPattern("d MMM"))} · $time"
    }
}

private fun greetingFor(): String {
    val hour = Instant.now().atZone(ZONE_KL).hour
    return when {
        hour < 5 -> "Up late"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

// Internal alias to avoid colliding with the SpendWiseV2.kt-local helpers
internal fun v2T(sizeSp: Float, weight: FontWeight = FontWeight.Normal, letter: Float = 0f): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    fontWeight = weight,
    letterSpacing = letter.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = TrimBoth
)

internal fun v2N(sizeSp: Float, weight: FontWeight): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    fontWeight = weight,
    letterSpacing = (-0.02f * sizeSp).sp,
    fontFeatureSettings = FinanceNumFeatures,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = TrimBoth
)
