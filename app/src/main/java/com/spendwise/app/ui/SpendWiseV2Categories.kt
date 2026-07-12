@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.spendwise.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.app.data.CategoryDeletion
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryPeriodStats
import com.spendwise.app.domain.Budget
import com.spendwise.app.ui.theme.AppIsDark
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.AppSurfaceContainer
import com.spendwise.app.ui.theme.AppSurfaceLow
import com.spendwise.app.ui.theme.SwInk
import com.spendwise.app.ui.theme.SwPos
import com.spendwise.app.ui.theme.SwNeg
import com.spendwise.app.ui.theme.SwViolet
import com.spendwise.app.ui.theme.SwVioletSoft
import com.spendwise.app.ui.theme.SwPeach
import com.spendwise.app.ui.theme.pressableNoIndication
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ZONE_KL = ZoneId.of("Asia/Kuala_Lumpur")

@Composable
internal fun V2CategoriesScreen(
    state: DashboardUiState,
    // Per-category count + spend for the current calendar month, SQL-side —
    // state.expenses only carries the dashboard's selected month, which may
    // be a different month than "now".
    currentMonthStats: Map<Long, CategoryPeriodStats>,
    onBack: () -> Unit,
    onCreateCategory: (String, String, Long, Boolean, String) -> String?,
    onUpdateCategory: (Long, String, String, Long, Boolean, String) -> String?,
    onDeleteCategory: (Long) -> DeleteCategoryResult,
    onDeleteCategoryWithStrategy: (Long, CategoryDeletion) -> Unit
) {
    val categories = state.categories
    val budgets = state.budgets
    val isDark = AppIsDark

    var isIncomeTab by rememberSaveable { mutableStateOf(false) }

    // Form sheet state
    var categoryFormOpen by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    val thisMonthStatsByCategory = currentMonthStats
    val budgetsByCategory = remember(budgets) { budgets.associateBy { it.categoryId } }
    
    val expenseCategories = remember(categories) { categories.filter { !it.isIncomeAdjustment } }
    val incomeCategories = remember(categories) { categories.filter { it.isIncomeAdjustment } }
    val activeCategories = if (isIncomeTab) incomeCategories else expenseCategories
    
    val totalCategoriesCount = categories.size
    val incomeCategoriesCount = incomeCategories.size
    val expenseCategoriesCount = expenseCategories.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp)
    ) {
        // Header
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
                    contentDescription = "Back",
                    bg = if (isDark) AppSurfaceLow else null
                )
                Text("Categories", color = SwInk, style = v2T(15f, FontWeight.Bold))
                V2CircleButton(
                    icon = Icons.Filled.Add,
                    onClick = {
                        editingCategory = null
                        categoryFormOpen = true
                    },
                    contentDescription = "Add category",
                    bg = if (isDark) AppSurfaceLow else null
                )
            }
        }
        
        // Hero summary
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                V2Eyebrow(text = "$totalCategoriesCount categories · $incomeCategoriesCount income, $expenseCategoriesCount expense")
                Text(
                    text = "Tap any to edit, hold to reorder.",
                    color = SwInk,
                    style = v2T(22f, FontWeight.Bold, letter = -0.22f)
                )
            }
        }
        
        // Toggle tabs
        item {
            Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppSurfaceLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    V2SheetSeg(
                        label = "Expense · $expenseCategoriesCount",
                        selected = !isIncomeTab,
                        modifier = Modifier.weight(1f)
                    ) { isIncomeTab = false }
                    V2SheetSeg(
                        label = "Income · $incomeCategoriesCount",
                        selected = isIncomeTab,
                        modifier = Modifier.weight(1f)
                    ) { isIncomeTab = true }
                }
            }
        }
        
        // Category List
        if (activeCategories.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    V2Card(padding = 6.dp) {
                        Column {
                            activeCategories.forEachIndexed { index, cat ->
                                if (index > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(v2Hairline())
                                    )
                                }
                                
                                val stats = thisMonthStatsByCategory[cat.id]
                                val count = stats?.entryCount ?: 0
                                val totalCents = stats?.totalCents ?: 0L
                                val vis = visualForCategory(cat.name, cat.iconName, cat.color)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            if (cat.isCustom) {
                                                editingCategory = cat
                                                categoryFormOpen = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom drag handle visual
                                    Column(
                                        modifier = Modifier.width(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .width(14.dp)
                                                    .height(1.5.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(AppOnSurfaceVariant.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                    
                                    V2Tile(color = vis.color, icon = vis.icon, size = 42.dp)
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = cat.name,
                                                color = SwInk,
                                                style = v2T(14.5f, FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!cat.isCustom) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(AppSurfaceLow)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "SYSTEM",
                                                        color = AppOnSurfaceVariant,
                                                        style = v2T(9.5f, FontWeight.Bold, letter = 0.06f)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "$count ${if (count == 1) "transaction" else "transactions"} this month",
                                            color = AppOnSurfaceVariant,
                                            style = v2T(12f, FontWeight.Medium),
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "RM ${formatRinggit(totalCents / 100L)}.${"%02d".format(totalCents % 100L)}",
                                                color = SwInk,
                                                maxLines = 1,
                                                softWrap = false,
                                                style = v2N(13.5f, FontWeight.Bold)
                                            )
                                            // budget limits limit warning check
                                            val budgetLimit = budgetsByCategory[cat.id]
                                            if (budgetLimit != null && !isIncomeTab) {
                                                val limit = budgetLimit.monthlyLimitCents
                                                if (limit > 0) {
                                                    val ratio = totalCents.toFloat() / limit.toFloat()
                                                    val warningText = when {
                                                        ratio >= 1.0f -> "LIMIT REACHED"
                                                        ratio >= 0.8f -> "80% BREACHED"
                                                        else -> null
                                                    }
                                                    val warningColor = when {
                                                        ratio >= 1.0f -> SwNeg
                                                        ratio >= 0.8f -> SwPeach
                                                        else -> AppOnSurfaceVariant
                                                    }
                                                    if (warningText != null) {
                                                        Text(
                                                            text = warningText,
                                                            color = warningColor,
                                                            style = v2T(9f, FontWeight.Bold, letter = 0.04f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (cat.isCustom) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Edit category",
                                                tint = AppOnSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = "Locked category",
                                                tint = AppOnSurfaceVariant.copy(alpha = 0.4f),
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
        
        // Add bottom CTA button
        item {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pressableNoIndication(scale = 0.97f) {
                            editingCategory = null
                            categoryFormOpen = true
                        },
                    color = if (isDark) SwViolet else SwInk,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = if (isDark) 12.dp else 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isIncomeTab) "New income category" else "New expense category",
                            color = Color.White,
                            style = v2T(14f, FontWeight.Bold)
                        )
                    }
                }
            }
        }
        
        // Footer note
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SwVioletSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "?",
                        color = SwInk,
                        style = v2T(12f, FontWeight.ExtraBold)
                    )
                }
                Text(
                    text = "System categories (Housing, Transport, Bills, Shopping, Health, Salary) cannot be edited or deleted. Custom categories can have monthly budget limits configured.",
                    color = AppOnSurfaceVariant,
                    style = v2T(12.5f, FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    
    // Category Form sheet
    V2CategoryFormSheet(
        visible = categoryFormOpen,
        existing = editingCategory,
        initialIncome = isIncomeTab,
        onSaveCreate = { name, iconName, color, isIncome, budgetLimit ->
            val err = onCreateCategory(name, iconName, color, isIncome, budgetLimit)
            if (err == null) {
                categoryFormOpen = false
            }
            err
        },
        onSaveUpdate = { id, name, iconName, color, isIncome, budgetLimit ->
            val err = onUpdateCategory(id, name, iconName, color, isIncome, budgetLimit)
            if (err == null) {
                categoryFormOpen = false
            }
            err
        },
        onDelete = onDeleteCategory,
        onDeleteWithStrategy = { id, strategy ->
            onDeleteCategoryWithStrategy(id, strategy)
            categoryFormOpen = false
        },
        categories = categories,
        budgets = budgets,
        usageCountThisMonth = editingCategory?.let {
            thisMonthStatsByCategory[it.id]?.entryCount
        } ?: 0,
        onDismiss = { categoryFormOpen = false }
    )
}

@Composable
private fun V2SheetSeg(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isDark = AppIsDark
    val selectedBg = if (isDark) SwInk else SwInk
    val selectedFg = if (isDark) Color(0xFF0F0D1A) else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) selectedBg else Color.Transparent)
            .pressableNoIndication(scale = 0.96f, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) selectedFg else AppOnSurfaceVariant,
            style = v2T(13f, FontWeight.Bold)
        )
    }
}
