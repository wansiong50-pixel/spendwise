@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.spendwise.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.AccountType
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Budget
import com.spendwise.app.data.CategoryDeletion
import com.spendwise.app.domain.MerchantNames
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.RangeStats
import com.spendwise.app.ui.theme.AppIsDark
import com.spendwise.app.ui.theme.AppCtaBg
import com.spendwise.app.ui.theme.AppCtaFg
import com.spendwise.app.ui.theme.AppOnSurface
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.AppSurfaceContainer
import com.spendwise.app.ui.theme.AppSurfaceContainerHigh
import com.spendwise.app.ui.theme.AppSurfaceLow
import com.spendwise.app.ui.theme.FinanceNumFeatures
import com.spendwise.app.ui.theme.SpendWiseMotion
import com.spendwise.app.ui.theme.SwButter
import com.spendwise.app.ui.theme.SwCoral
import com.spendwise.app.ui.theme.SwInk
import com.spendwise.app.ui.theme.SwMint
import com.spendwise.app.ui.theme.SwNeg
import com.spendwise.app.ui.theme.SwPeach
import com.spendwise.app.ui.theme.SwPink
import com.spendwise.app.ui.theme.SwPos
import com.spendwise.app.ui.theme.SwSky
import com.spendwise.app.ui.theme.SwViolet
import com.spendwise.app.ui.theme.SwVioletSoft
import com.spendwise.app.ui.theme.perfMode
import com.spendwise.app.ui.theme.pressable
import com.spendwise.app.ui.theme.pressableNoIndication
import com.spendwise.app.ui.theme.rememberSheetDragState
import com.spendwise.app.ui.theme.sheetDragToDismiss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ZONE_M: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

// ═════════════════════════════════════════════════════════════════════════════
//  04 · ADD EXPENSE / INCOME (modal sheet)
//
//  Port of screens-2.jsx :: ScreenAddExpense.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun V2AddExpenseSheet(
    existing: Expense?,
    categories: List<Category>,
    accounts: List<Account>,
    // The recent-entries window (bounded, creation order) — merchant
    // suggestions rank by entry recency so older rows add nothing.
    expenses: List<Expense>,
    // Entry count per calendar day, SQL-aggregated over the whole ledger —
    // date-picker dots + the "Entries" counter.
    dayEntryCounts: Map<LocalDate, Int>,
    formError: String?,
    initialIncome: Boolean,
    onSave: (
        id: Long?,
        amountInput: String,
        categoryId: Long?,
        accountId: Long?,
        merchant: String,
        notes: String,
        dateInput: String
    ) -> Boolean,
    onCreateCategory: (
        nameInput: String,
        iconName: String,
        color: Long,
        isIncome: Boolean,
        budgetLimitInput: String
    ) -> String?,
    onClose: () -> Unit
) {
    val isEdit = existing != null
    val expenseCategories = categories.filter { !it.isIncomeAdjustment }
    val incomeCategories = categories.filter { it.isIncomeAdjustment }
    val existingIsIncome = existing != null && incomeCategories.any { it.id == existing.categoryId }
    var income by rememberSaveable(existing?.id) { mutableStateOf(if (isEdit) existingIsIncome else initialIncome) }

    var amountInput by rememberSaveable(existing?.id) {
        mutableStateOf(
            if (existing != null) MoneyFormatter.centsToInput(existing.amountCents)
            else ""
        )
    }
    var selectedCategoryId by rememberSaveable(existing?.id, income) {
        mutableStateOf(
            existing?.categoryId
                ?: (if (income) incomeCategories else expenseCategories).firstOrNull()?.id
        )
    }
    var selectedAccountId by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id)
    }
    var merchant by rememberSaveable(existing?.id) { mutableStateOf(existing?.merchant ?: "") }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes ?: "") }
    val initialDate = if (existing != null) {
        Instant.ofEpochMilli(existing.occurredAtMillis).atZone(ZONE_M).toLocalDate()
    } else LocalDate.now(ZONE_M)
    var dateInput by rememberSaveable(existing?.id) { mutableStateOf(initialDate.toString()) }
    var datePickerOpen by remember { mutableStateOf(false) }
    var categoryFormOpen by remember { mutableStateOf(false) }
    var pendingCreatedCategoryName by remember { mutableStateOf<String?>(null) }

    val activeCategories = if (income) incomeCategories else expenseCategories
    val isDark = AppIsDark
    val incomeCategoryIds = remember(categories) {
        categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()
    }
    val historicalMerchants = remember(expenses, income, incomeCategoryIds) {
        expenses.asSequence()
            .filter { expense -> existing?.id == null || expense.id != existing.id }
            .filter { expense ->
                val isExpenseIncome = expense.categoryId in incomeCategoryIds
                if (income) isExpenseIncome else !isExpenseIncome
            }
            .sortedByDescending { it.createdAtMillis }
            .map { it.merchant }
            .filter { it.isNotBlank() }
            .toList()
    }
    var merchantFocused by remember { mutableStateOf(false) }
    var debouncedMerchant by remember { mutableStateOf(merchant) }
    LaunchedEffect(merchant) {
        delay(220)
        debouncedMerchant = merchant
    }
    val cleanedMerchantQuery = MerchantNames.clean(debouncedMerchant)
    val shouldShowMerchantSuggestions = merchantFocused || cleanedMerchantQuery.isNotBlank()
    val merchantSuggestionLimit = if (cleanedMerchantQuery.isBlank()) 3 else 5
    val merchantSuggestions = remember(
        debouncedMerchant,
        historicalMerchants,
        merchantSuggestionLimit,
        shouldShowMerchantSuggestions
    ) {
        if (!shouldShowMerchantSuggestions) {
            emptyList()
        } else {
            MerchantNames.suggest(
                query = debouncedMerchant,
                existing = historicalMerchants,
                limit = merchantSuggestionLimit
            )
        }
    }

    LaunchedEffect(income) {
        if (selectedCategoryId !in activeCategories.map { it.id }) {
            selectedCategoryId = activeCategories.firstOrNull()?.id
        }
    }
    LaunchedEffect(categories, pendingCreatedCategoryName, income) {
        val pending = pendingCreatedCategoryName ?: return@LaunchedEffect
        categories.firstOrNull {
            it.name.equals(pending, ignoreCase = true) && it.isIncomeAdjustment == income
        }?.let {
            selectedCategoryId = it.id
            pendingCreatedCategoryName = null
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top dim band that suggests the underlying content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background((if (isDark) Color.Black else Color(0xFF15121F)).copy(alpha = 0.35f))
        )
        // Sheet body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(AppSurface)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Grabber
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppSurfaceContainer)
                )
            }
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V2CircleButton(
                    icon = Icons.Filled.Close,
                    onClick = onClose,
                    contentDescription = "Close",
                    size = 38.dp,
                    bg = if (isDark) AppSurfaceLow else null
                )
                Text(
                    text = if (isEdit) "Edit ${if (income) "income" else "expense"}"
                    else "Add ${if (income) "income" else "expense"}",
                    color = SwInk,
                    style = v2T(15f, FontWeight.Bold)
                )
                Spacer(Modifier.size(38.dp))
            }
            // Kind toggle
            if (!isEdit) {
                Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppSurfaceLow)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        V2SheetSeg("Expense", !income, Modifier.weight(1f)) { income = false }
                        V2SheetSeg("Income", income, Modifier.weight(1f)) { income = true }
                    }
                }
            }
            // Big amount
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V2Eyebrow(text = "Amount")
                V2AmountInput(
                    value = amountInput,
                    onChange = { amountInput = it },
                    color = if (income) SwPos else SwInk
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(2.dp)
                        .background((if (income) SwPos else SwInk).copy(alpha = 0.18f))
                )
            }
            // Date row — the dots-chip ("more options") from the design is
            // deferred until receipt-photo / recurring / tag features exist.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 0.dp)
            ) {
                V2FieldChip(
                    icon = Icons.Filled.CalendarToday,
                    label = formatFriendlyDate(dateInput),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { datePickerOpen = true }
                )
            }
            // Category
            V2SheetSectionLabel("Category")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeCategories.forEach { cat ->
                    val vis = visualForCategory(cat.name, cat.iconName, cat.color)
                    V2CategoryTile(
                        name = cat.name,
                        icon = vis.icon,
                        color = vis.color,
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id }
                    )
                }
                V2NewCategoryTile(onClick = { categoryFormOpen = true })
            }
            // Account
            V2SheetSectionLabel("Account")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    val vis = visualForAccount(account)
                    V2AccountPill(
                        name = account.name,
                        icon = vis.icon,
                        color = vis.color,
                        selected = selectedAccountId == account.id,
                        needsLightIcon = account.type == AccountType.Credit,
                        onClick = { selectedAccountId = account.id }
                    )
                }
            }
            // Merchant
            V2SheetSectionLabel("Merchant")
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                V2SearchField(
                    value = merchant,
                    onChange = { merchant = it },
                    placeholder = if (income) "Source (optional)" else "Where did you spend?",
                    onFocusChanged = { merchantFocused = it }
                )
            }
            AnimatedVisibility(visible = merchantSuggestions.isNotEmpty()) {
                V2MerchantSuggestionStrip(
                    suggestions = merchantSuggestions,
                    selectedText = merchant,
                    onSelect = { merchant = it }
                )
            }
            // Notes
            V2SheetSectionLabel("Notes · optional")
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                V2NotesField(value = notes, onChange = { notes = it })
            }
            // Error
            if (!formError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                ) {
                    Text(formError, color = SwNeg, style = v2T(13f, FontWeight.SemiBold))
                }
            }
            // Save button. In dark mode the CTA flips from ink to violet with
            // a halo — same role as the FAB ("brightest pixel on screen"),
            // because ink-on-aubergine would just merge into the sheet.
            val ctaColor = if (isDark) SwViolet else SwInk
            val saveHaptics = LocalHapticFeedback.current
            Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 28.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .let {
                            if (isDark) it.shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(percent = 50),
                                clip = false,
                                ambientColor = SwViolet.copy(alpha = 0.4f),
                                spotColor = SwViolet.copy(alpha = 0.45f)
                            ) else it
                        }
                        .pressableNoIndication(scale = 0.97f) {
                            val ok = onSave(
                                existing?.id,
                                amountInput,
                                selectedCategoryId,
                                selectedAccountId,
                                merchant,
                                notes,
                                dateInput
                            )
                            if (ok) {
                                // Physical "it landed" tick — the save is the
                                // one moment worth a definite haptic.
                                saveHaptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                onClose()
                            }
                        },
                    color = ctaColor,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "Save changes" else "Save ${if (income) "income" else "expense"}",
                            color = Color.White,
                            style = v2T(15f, FontWeight.Bold)
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }

        V2DatePickerSheet(
            visible = datePickerOpen,
            selectedDate = runCatching { LocalDate.parse(dateInput) }.getOrElse { LocalDate.now(ZONE_M) },
            dayEntryCounts = dayEntryCounts,
            onSelect = {
                dateInput = it.toString()
                datePickerOpen = false
            },
            onDismiss = { datePickerOpen = false }
        )
        V2CategoryFormSheet(
            visible = categoryFormOpen,
            initialIncome = income,
            onSaveCreate = { name, iconName, color, isIncome, budgetLimit ->
                val result = onCreateCategory(name, iconName, color, isIncome, budgetLimit)
                if (result == null) {
                    pendingCreatedCategoryName = name.trim()
                    income = isIncome
                    categoryFormOpen = false
                }
                result
            },
            onDismiss = { categoryFormOpen = false }
        )
    }
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
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) selectedFg else AppOnSurfaceVariant,
            style = v2T(13f, FontWeight.Bold)
        )
    }
}

@Composable
private fun V2AmountInput(value: String, onChange: (String) -> Unit, color: Color) {
    // The digits are the anchor: the field spans the full width with
    // textAlign = Center so the number sits on the true screen centerline,
    // and the "RM" prefix *follows* the number's left edge (measured from the
    // text layout) instead of pushing it off-center from inside a Row — the
    // Cash-App/Revolut composer pattern. Previously the Row centered
    // [RM + field] as a unit, so the digits landed right of center by half
    // the prefix width.
    //
    // TextFieldValue (not plain String) so we know the cursor offset — the
    // built-in cursor is a hard 500ms on/off step that can't be restyled, so
    // it's hidden (transparent brush) and a custom caret with a smooth
    // "breathing" fade (hold → ease out → hold → ease in, the iOS feel) is
    // drawn at the same position. Reduced-motion users keep the system
    // cursor: a steady rhythm with no fades is exactly what they asked for.
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    // External reset (form reopened for a different expense): adopt the new
    // text and park the caret at the end.
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, selection = TextRange(value.length))
    }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var placeholderWidthPx by remember { mutableStateOf(0) }
    var prefixWidthPx by remember { mutableStateOf(0) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val reduced = perfMode.reducedMotion

    val caretAlpha by rememberInfiniteTransition(label = "amountCaret").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f at 0
                1f at 480                                // hold solid
                0f at 750 using FastOutSlowInEasing      // melt out
                0f at 880                                // brief rest
                1f at 1100 using FastOutSlowInEasing     // breathe back in
            }
        ),
        label = "amountCaretAlpha"
    )

    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { raw ->
                // Allow only digits + one dot, max 2 decimals
                val sanitized = sanitizeAmount(raw.text)
                fieldValue = TextFieldValue(
                    text = sanitized,
                    selection = TextRange(raw.selection.end.coerceAtMost(sanitized.length))
                )
                onChange(sanitized)
            },
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = v2N(56f, FontWeight.ExtraBold).copy(color = color, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(if (reduced) color else Color.Transparent),
            onTextLayout = { textLayout = it },
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (fieldValue.text.isBlank()) {
                        Text(
                            "0.00",
                            color = color.copy(alpha = 0.35f),
                            style = v2N(56f, FontWeight.ExtraBold),
                            onTextLayout = { placeholderWidthPx = it.size.width }
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { fieldWidthPx = it.size.width }
                .drawWithContent {
                    drawContent()
                    val layout = textLayout
                    if (focused && !reduced && layout != null) {
                        val offset = fieldValue.selection.end.coerceIn(0, fieldValue.text.length)
                        val rect = layout.getCursorRect(offset)
                        val caretWidth = 2.5.dp.toPx()
                        val x = rect.left.coerceIn(0f, size.width - caretWidth)
                        drawRoundRect(
                            color = color.copy(alpha = caretAlpha),
                            topLeft = Offset(x, rect.top + rect.height * 0.10f),
                            size = Size(caretWidth, rect.height * 0.80f),
                            cornerRadius = CornerRadius(caretWidth / 2f)
                        )
                    }
                }
        )
        // Prefix rides 12dp left of the number's (or placeholder's) leading
        // edge. Hidden until both measurements land to avoid a one-frame
        // flash at position zero.
        val anchorLeftPx = if (fieldValue.text.isBlank()) {
            (fieldWidthPx - placeholderWidthPx) / 2f
        } else {
            textLayout?.getLineLeft(0) ?: 0f
        }
        val prefixVisible = fieldWidthPx > 0 && prefixWidthPx > 0
        Text(
            "RM",
            color = color.copy(alpha = if (prefixVisible) 0.55f else 0f),
            style = v2N(22f, FontWeight.SemiBold),
            onTextLayout = { prefixWidthPx = it.size.width },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp)
                .offset {
                    val gap = with(density) { 12.dp.toPx() }
                    IntOffset((anchorLeftPx - gap - prefixWidthPx).toInt().coerceAtLeast(0), 0)
                }
        )
    }
}

private fun sanitizeAmount(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    val singleDot = if (firstDot == -1) filtered else {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
    return if (firstDot == -1) singleDot
    else {
        val intPart = singleDot.substring(0, firstDot)
        val decPart = singleDot.substring(firstDot + 1).take(2)
        if (decPart.isEmpty()) "$intPart." else "$intPart.$decPart"
    }
}

@Composable
private fun V2FieldChip(
    icon: ImageVector,
    label: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .pressableNoIndication(onClick = onClick),
        color = AppSurfaceLow,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(16.dp))
            if (label != null) {
                Text(label, color = SwInk, style = v2T(13.5f, FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun V2SheetSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = AppOnSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
        style = v2T(11.5f, FontWeight.Bold, letter = 0.8f)
    )
}

@Composable
private fun V2CategoryTile(
    name: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = AppIsDark
    val bg = when {
        selected && isDark -> SwViolet
        selected -> SwInk
        isDark -> AppSurfaceLow
        else -> Color.White
    }
    val labelColor = if (selected) Color.White else SwInk
    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else v2Hairline(),
                shape = RoundedCornerShape(18.dp)
            )
            .pressableNoIndication(scale = 0.95f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        V2Tile(color = color, icon = icon, size = 36.dp)
        Text(
            text = name,
            color = labelColor,
            style = v2T(11.5f, FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V2NewCategoryTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurfaceLow)
            .pressableNoIndication(scale = 0.95f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (AppIsDark) AppSurface else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Text("New", color = AppOnSurfaceVariant, style = v2T(11.5f, FontWeight.Bold))
    }
}

@Composable
private fun V2AccountPill(
    name: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    needsLightIcon: Boolean,
    onClick: () -> Unit
) {
    val isDark = AppIsDark
    val pillBg = when {
        selected && isDark -> SwViolet
        selected -> color
        isDark -> AppSurfaceLow
        else -> Color.White
    }
    val labelColor = if (selected && (isDark || needsLightIcon)) Color.White else SwInk
    Row(
        modifier = Modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(pillBg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else v2Hairline(),
                shape = RoundedCornerShape(percent = 50)
            )
            .pressableNoIndication(scale = 0.95f, onClick = onClick)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color.White.copy(alpha = 0.25f) else color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (selected && (isDark || needsLightIcon)) Color.White else SwInk, modifier = Modifier.size(14.dp))
        }
        Text(
            text = name,
            color = labelColor,
            style = v2T(12.5f, FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V2SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurfaceLow)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Search, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) },
            textStyle = v2T(14.5f, FontWeight.SemiBold).copy(color = SwInk),
            cursorBrush = SolidColor(SwInk),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isBlank()) {
                        Text(placeholder, color = AppOnSurfaceVariant, style = v2T(14.5f, FontWeight.Medium))
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun V2MerchantSuggestionStrip(
    suggestions: List<String>,
    selectedText: String,
    onSelect: (String) -> Unit
) {
    val visibleSuggestions = remember(suggestions, selectedText) {
        val selectedClean = MerchantNames.clean(selectedText)
        suggestions.filterNot { MerchantNames.clean(it).equals(selectedClean, ignoreCase = true) }
    }
    if (visibleSuggestions.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "From history",
            color = AppOnSurfaceVariant,
            style = v2T(11f, FontWeight.Bold, letter = 0.6f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleSuggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier.pressableNoIndication(scale = 0.96f) { onSelect(suggestion) },
                    color = AppSurfaceLow,
                    shape = RoundedCornerShape(percent = 50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, v2Hairline())
                ) {
                    Text(
                        text = suggestion,
                        color = SwInk,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = v2T(12.5f, FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun V2NotesField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = v2T(13.5f, FontWeight.Medium).copy(color = SwInk),
        cursorBrush = SolidColor(SwInk),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurfaceLow)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .heightIn(min = 56.dp),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isBlank()) {
                    Text(
                        "What was this for?",
                        color = AppOnSurfaceVariant,
                        style = v2T(13.5f, FontWeight.Medium)
                    )
                }
                inner()
            }
        }
    )
}

private fun formatFriendlyDate(dateInput: String): String {
    return runCatching {
        val d = LocalDate.parse(dateInput)
        val today = LocalDate.now(ZONE_M)
        when (d) {
            today -> "Today, ${d.format(DateTimeFormatter.ofPattern("d MMM"))}"
            today.minusDays(1) -> "Yesterday, ${d.format(DateTimeFormatter.ofPattern("d MMM"))}"
            else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
        }
    }.getOrElse { "Today" }
}

// ═════════════════════════════════════════════════════════════════════════════
//  05 · ACCOUNTS
//
//  Port of screens-2.jsx :: ScreenAccounts. Back / All accounts / + circle
//  header; net-worth eyebrow + display amount; featured gradient primary
//  account; smaller pebble rows for the rest; dashed "+ Add account" CTA;
//  collapsible Archived section with restore chips.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun V2DatePickerSheet(
    visible: Boolean,
    selectedDate: LocalDate,
    dayEntryCounts: Map<LocalDate, Int>,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now(ZONE_M)
    var draftInput by rememberSaveable(visible) { mutableStateOf(selectedDate.toString()) }
    var visibleMonth by remember(visible) { mutableStateOf(YearMonth.from(selectedDate)) }
    // Month/year jump grid — toggled by tapping the "July 2026 ▾" title, the
    // same affordance Google Calendar and the Material date picker use, so
    // reaching a far-away month doesn't mean hammering the arrow buttons.
    var monthGridOpen by remember(visible) { mutableStateOf(false) }
    var gridYear by remember(visible) { mutableStateOf(YearMonth.from(selectedDate).year) }
    val draftDate = runCatching { LocalDate.parse(draftInput) }.getOrElse { selectedDate }
    val transactionDates = dayEntryCounts.keys
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    LaunchedEffect(visible, selectedDate) {
        if (visible) {
            draftInput = selectedDate.toString()
            visibleMonth = YearMonth.from(selectedDate)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseStandard)),
        exit = fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseStandard))
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (AppIsDark) 0.62f else 0.48f))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(AppSurface)
                    .padding(top = 12.dp, bottom = 18.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = AppOnSurfaceVariant,
                        modifier = Modifier.pressableNoIndication(onClick = onDismiss),
                        style = v2T(13.5f, FontWeight.SemiBold)
                    )
                    Text("Pick a date", color = SwInk, style = v2T(15f, FontWeight.Bold))
                    Text(
                        text = "Done",
                        color = SwViolet,
                        modifier = Modifier.pressableNoIndication { onSelect(draftDate) },
                        style = v2T(13.5f, FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V2QuickDateChip("Today", draftDate == today) {
                        draftInput = today.toString()
                        visibleMonth = YearMonth.from(today)
                    }
                    V2QuickDateChip("Yesterday", draftDate == today.minusDays(1)) {
                        val d = today.minusDays(1)
                        draftInput = d.toString()
                        visibleMonth = YearMonth.from(d)
                    }
                    V2QuickDateChip("2 days ago", draftDate == today.minusDays(2)) {
                        val d = today.minusDays(2)
                        draftInput = d.toString()
                        visibleMonth = YearMonth.from(d)
                    }
                    V2QuickDateChip("This week", false) {
                        draftInput = today.toString()
                        visibleMonth = YearMonth.from(today)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chevronAngle by animateFloatAsState(
                        targetValue = if (monthGridOpen) 180f else 0f,
                        animationSpec = tween(SpendWiseMotion.Tooltip, easing = SpendWiseMotion.EaseStandard),
                        label = "monthGridChevron"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.pressableNoIndication {
                            gridYear = visibleMonth.year
                            monthGridOpen = !monthGridOpen
                        }
                    ) {
                        Text(
                            text = visibleMonth.format(monthTitleFormatter),
                            color = if (monthGridOpen) SwViolet else SwInk,
                            style = v2T(19f, FontWeight.ExtraBold)
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            "Jump to month",
                            tint = if (monthGridOpen) SwViolet else AppOnSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(chevronAngle)
                        )
                    }
                    if (monthGridOpen) {
                        // Year stepper replaces the month arrows while the
                        // jump grid is open — same slot, same button size.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V2CircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                onClick = { gridYear -= 1 },
                                contentDescription = "Previous year",
                                size = 34.dp,
                                bg = AppSurfaceLow
                            )
                            Text(
                                text = gridYear.toString(),
                                color = SwInk,
                                style = v2T(14f, FontWeight.Bold)
                            )
                            V2CircleButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = { if (gridYear < today.year) gridYear += 1 },
                                contentDescription = "Next year",
                                size = 34.dp,
                                bg = AppSurfaceLow,
                                tint = if (gridYear < today.year) SwInk
                                    else AppOnSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            V2CircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                                contentDescription = "Previous month",
                                size = 34.dp,
                                bg = AppSurfaceLow
                            )
                            V2CircleButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = {
                                    if (visibleMonth < YearMonth.from(today)) {
                                        visibleMonth = visibleMonth.plusMonths(1)
                                    }
                                },
                                contentDescription = "Next month",
                                size = 34.dp,
                                bg = AppSurfaceLow,
                                tint = if (visibleMonth < YearMonth.from(today)) SwInk else AppOnSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }

                if (monthGridOpen) {
                    // 3×4 month jump grid in the same slot as the day grid.
                    // Months after the current one (no future entries) are
                    // disabled, mirroring the day-cell rule.
                    val currentYm = YearMonth.from(today)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(4) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(3) { col ->
                                    val monthNumber = row * 3 + col + 1
                                    val ym = YearMonth.of(gridYear, monthNumber)
                                    val enabled = ym <= currentYm
                                    val isSelected = ym == visibleMonth
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                when {
                                                    isSelected -> SwInk
                                                    else -> AppSurfaceLow
                                                }
                                            )
                                            .then(
                                                if (enabled) Modifier.pressableNoIndication(scale = 0.96f) {
                                                    visibleMonth = ym
                                                    monthGridOpen = false
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ym.format(DateTimeFormatter.ofPattern("MMM")),
                                            color = when {
                                                isSelected -> if (AppIsDark) Color(0xFF0F0D1A) else Color.White
                                                enabled -> SwInk
                                                else -> AppOnSurfaceVariant.copy(alpha = 0.35f)
                                            },
                                            style = v2T(13f, FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            color = AppOnSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            style = v2T(11f, FontWeight.Bold, letter = 0.5f)
                        )
                    }
                }

                val firstDay = visibleMonth.atDay(1)
                val firstDow = firstDay.dayOfWeek.value % 7
                val daysInMonth = visibleMonth.lengthOfMonth()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(6) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(7) { col ->
                                val dayNumber = row * 7 + col - firstDow + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val date = visibleMonth.atDay(dayNumber)
                                    V2CalendarDayCell(
                                        date = date,
                                        selected = date == draftDate,
                                        today = date == today,
                                        future = date > today,
                                        hasTransactions = date in transactionDates,
                                        onClick = { draftInput = date.toString() },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V2DateSummaryBox(
                        label = "Date",
                        value = draftDate.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                        modifier = Modifier.weight(1f)
                    )
                    val entriesOnDate = dayEntryCounts[draftDate] ?: 0
                    V2DateSummaryBox(
                        label = "Entries",
                        value = entriesOnDate.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun V2QuickDateChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val isDark = AppIsDark
    Surface(
        modifier = Modifier
            .height(36.dp)
            .pressableNoIndication(scale = 0.96f, onClick = onClick),
        color = if (selected) (if (isDark) SwInk else SwInk) else AppSurfaceLow,
        shape = RoundedCornerShape(percent = 50)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) (if (isDark) Color(0xFF0F0D1A) else Color.White) else SwInk,
                style = v2T(13f, FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun V2CalendarDayCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    future: Boolean,
    hasTransactions: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) (if (AppIsDark) SwViolet else SwInk) else Color.Transparent
    val fg = when {
        selected -> Color.White
        future -> AppOnSurfaceVariant.copy(alpha = 0.5f)
        else -> SwInk
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (today && !selected) 1.5.dp else 0.dp,
                color = if (today && !selected) SwViolet else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .then(if (!future) Modifier.pressableNoIndication(scale = 0.94f, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = fg,
                style = v2N(14f, if (selected) FontWeight.ExtraBold else FontWeight.SemiBold)
            )
            if (hasTransactions) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else SwViolet)
                )
            }
        }
    }
}

@Composable
private fun V2DateSummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurfaceLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = AppOnSurfaceVariant,
            style = v2T(11f, FontWeight.Bold, letter = 0.6f)
        )
        Text(
            text = value,
            color = SwInk,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = v2T(14f, FontWeight.Bold)
        )
    }
}

@Composable
internal fun V2CategoryFormSheet(
    visible: Boolean,
    existing: Category? = null,
    initialIncome: Boolean,
    onSaveCreate: (name: String, iconName: String, color: Long, isIncome: Boolean, budgetLimitInput: String) -> String?,
    onSaveUpdate: ((id: Long, name: String, iconName: String, color: Long, isIncome: Boolean, budgetLimitInput: String) -> String?)? = null,
    onDelete: ((Long) -> DeleteCategoryResult)? = null,
    onDeleteWithStrategy: ((Long, CategoryDeletion) -> Unit)? = null,
    categories: List<Category> = emptyList(),
    budgets: List<Budget> = emptyList(),
    // "Used N× this month" for the edited category. SQL-aggregated by the
    // caller; 0 for new categories.
    usageCountThisMonth: Int = 0,
    onDismiss: () -> Unit
) {
    val isEdit = existing != null
    var name by rememberSaveable(visible, existing) { mutableStateOf(existing?.name ?: "") }
    var isIncome by rememberSaveable(visible, existing) { mutableStateOf(existing?.isIncomeAdjustment ?: initialIncome) }
    var iconName by rememberSaveable(visible, existing) {
        mutableStateOf(existing?.iconName ?: (if (initialIncome) "spark" else "restaurant"))
    }
    var color by rememberSaveable(visible, existing) {
        mutableStateOf(existing?.color ?: (if (initialIncome) 0xFF9FE3C5L else 0xFFF8C99BL))
    }
    
    // Look up budget limit for existing category
    val existingBudget = remember(existing, budgets) {
        existing?.let { cat -> budgets.find { it.categoryId == cat.id } }
    }
    var budgetLimitInput by rememberSaveable(visible, existingBudget) {
        mutableStateOf(
            if (existingBudget != null) MoneyFormatter.centsToInput(existingBudget.monthlyLimitCents)
            else ""
        )
    }
    
    var error by rememberSaveable(visible) { mutableStateOf<String?>(null) }
    
    // Dialog/strategy state for deletion
    var showDeleteStrategyDialog by remember { mutableStateOf(false) }
    var strategyExpenseCount by remember { mutableStateOf(0) }
    var selectedMigrationCategoryId by remember { mutableStateOf<Long?>(null) }

    val usageCount = if (existing == null) 0 else usageCountThisMonth


    val previewSubtext = if (isEdit) {
        "${if (isIncome) "Income" else "Expense"} · used ${usageCount}× this month"
    } else {
        "${if (isIncome) "Income" else "Expense"} · new category"
    }

    LaunchedEffect(visible, initialIncome, existing) {
        if (visible && !isEdit) {
            name = ""
            isIncome = initialIncome
            iconName = if (initialIncome) "spark" else "restaurant"
            color = if (initialIncome) 0xFF9FE3C5L else 0xFFF8C99BL
            budgetLimitInput = ""
            error = null
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseOut)
        ) + fadeIn(tween(SpendWiseMotion.Sheet)),
        exit = slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseInOut)
        ) + fadeOut(tween(SpendWiseMotion.ModalExit))
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (AppIsDark) 0.62f else 0.42f))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(AppSurface)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(top = 12.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V2CircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onDismiss,
                        contentDescription = "Back",
                        size = 38.dp,
                        bg = if (AppIsDark) AppSurfaceLow else null
                    )
                    Text(if (isEdit) "Edit category" else "New category", color = SwInk, style = v2T(15f, FontWeight.Bold))
                    Text(
                        text = "Save",
                        color = SwViolet,
                        modifier = Modifier.pressableNoIndication {
                            error = if (isEdit) {
                                onSaveUpdate?.invoke(existing!!.id, name, iconName, color, isIncome, budgetLimitInput)
                            } else {
                                onSaveCreate(name, iconName, color, isIncome, budgetLimitInput)
                            }
                        },
                        style = v2T(13.5f, FontWeight.Bold)
                    )
                }

                Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppSurfaceLow,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            V2Tile(color = Color(color), icon = iconFromName(iconName), size = 64.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                V2Eyebrow(text = "Preview")
                                BasicTextField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                        error = null
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    textStyle = v2T(22f, FontWeight.ExtraBold).copy(color = SwInk),
                                    cursorBrush = SolidColor(SwInk),
                                    decorationBox = { inner ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (name.isBlank()) {
                                                Text(
                                                    "Category name",
                                                    color = AppOnSurfaceVariant,
                                                    style = v2T(22f, FontWeight.ExtraBold)
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                                Text(
                                    text = previewSubtext,
                                    color = AppOnSurfaceVariant,
                                    style = v2T(12f, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }

                V2SheetSectionLabel("Type")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppSurfaceLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    V2SheetSeg("Expense", !isIncome, Modifier.weight(1f)) {
                        isIncome = false
                        if (iconName == "spark") iconName = "restaurant"
                    }
                    V2SheetSeg("Income", isIncome, Modifier.weight(1f)) {
                        isIncome = true
                        if (iconName == "restaurant") iconName = "spark"
                    }
                }

                V2SheetSectionLabel("Icon")
                val iconRows = remember {
                    listOf(
                        listOf("restaurant", "shopping_bag", "directions_car", "house", "phone", "movie"),
                        listOf("gift", "favorite", "box", "spark", "wallet", "cash")
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { iconKey ->
                                val selected = iconName == iconKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) (if (AppIsDark) SwViolet else SwInk) else AppSurfaceLow)
                                        .border(
                                            width = if (selected) 0.dp else 1.dp,
                                            color = if (selected) Color.Transparent else v2Hairline(),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .pressableNoIndication(scale = 0.94f) { iconName = iconKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconFromName(iconKey),
                                        contentDescription = iconKey,
                                        tint = if (selected) Color.White else SwInk,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                V2SheetSectionLabel("Tile color")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    v2CategoryPalette().forEach { c ->
                        val selected = color == c
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .pressableNoIndication(scale = 0.92f) { color = c },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 30.dp else 34.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = SwInk,
                                        shape = CircleShape
                                    )
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        tint = if (AppIsDark) Color(0xFF0F0D1A) else SwInk,
                                        modifier = Modifier.align(Alignment.Center).size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Monthly Budget limits input
                if (!isIncome) {
                    V2SheetSectionLabel("Monthly budget · optional")
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppSurfaceLow)
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("RM", color = AppOnSurfaceVariant, style = v2N(14f, FontWeight.SemiBold))
                            BasicTextField(
                                value = budgetLimitInput,
                                onValueChange = { budgetLimitInput = sanitizeAmount(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                textStyle = v2N(22f, FontWeight.ExtraBold).copy(color = SwInk),
                                cursorBrush = SolidColor(SwInk),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (budgetLimitInput.isBlank()) {
                                            Text("0.00", color = AppOnSurfaceVariant.copy(alpha = 0.5f), style = v2N(22f, FontWeight.ExtraBold))
                                        }
                                        inner()
                                    }
                                }
                            )
                            Text("/ month", color = AppOnSurfaceVariant, style = v2T(12f, FontWeight.Bold))
                        }
                    }
                    Text(
                        text = "We'll nudge you at 80% and warn at 100%.",
                        color = AppOnSurfaceVariant,
                        style = v2T(12f, FontWeight.Medium),
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp)
                    )
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error!!,
                        color = SwNeg,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
                        style = v2T(13f, FontWeight.SemiBold)
                    )
                }
                
                // Danger Zone / Archive-Delete Actions
                if (isEdit && onDelete != null) {
                    V2SheetSectionLabel("Danger zone")
                    Column(
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .pressableNoIndication(scale = 0.97f) {
                                    error = "Archiving categories is not supported in the database schema. Please use Delete instead."
                                },
                            color = AppSurfaceLow,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Archive, null, tint = SwInk, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Archive · keeps transactions", color = SwInk, style = v2T(13.5f, FontWeight.Bold))
                            }
                        }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .pressableNoIndication(scale = 0.97f) {
                                    val res = onDelete(existing!!.id)
                                    when (res) {
                                        DeleteCategoryResult.Deleted -> {
                                            onDismiss()
                                        }
                                        is DeleteCategoryResult.Blocked -> {
                                            error = res.reason
                                        }
                                        is DeleteCategoryResult.NeedsStrategy -> {
                                            strategyExpenseCount = res.expenseCount
                                            val alternatives = categories.filter { 
                                                it.id != existing.id && 
                                                it.isIncomeAdjustment == existing.isIncomeAdjustment 
                                            }
                                            selectedMigrationCategoryId = alternatives.firstOrNull()?.id
                                            showDeleteStrategyDialog = true
                                        }
                                    }
                                },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SwNeg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Close, null, tint = SwNeg, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (usageCount > 0) "Delete · moves $usageCount tx" else "Delete category",
                                    color = SwNeg,
                                    style = v2T(13.5f, FontWeight.Bold)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(28.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
            
            // Delete Strategy Confirmation Dialog overlay
            if (showDeleteStrategyDialog && existing != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showDeleteStrategyDialog = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        color = AppSurface,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Resolve deleted category",
                                color = SwInk,
                                style = v2T(16f, FontWeight.Bold)
                            )
                            Text(
                                text = "This category has $strategyExpenseCount transactions in your records. What should we do with them?",
                                color = AppOnSurfaceVariant,
                                style = v2T(13.5f, FontWeight.Medium)
                            )
                            
                            // Delete everything option
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .pressableNoIndication(scale = 0.97f) {
                                        onDeleteWithStrategy?.invoke(existing.id, CategoryDeletion.DeleteExpenses)
                                        showDeleteStrategyDialog = false
                                        onDismiss()
                                    },
                                color = SwNeg,
                                shape = RoundedCornerShape(percent = 50)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Delete category & all transactions", color = Color.White, style = v2T(13.5f, FontWeight.Bold))
                                }
                            }
                            
                            // Reassign migration option
                            val alternatives = remember(categories, existing) {
                                categories.filter { 
                                    it.id != existing.id && 
                                    it.isIncomeAdjustment == existing.isIncomeAdjustment 
                                }
                            }
                            if (alternatives.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "MOVE TRANSACTIONS TO:",
                                        color = AppOnSurfaceVariant,
                                        style = v2T(10.5f, FontWeight.Bold, letter = 0.5f)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        alternatives.forEach { altCat ->
                                            val isSelected = selectedMigrationCategoryId == altCat.id
                                            val vis = visualForCategory(altCat.name, altCat.iconName, altCat.color)
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(percent = 50))
                                                    .background(if (isSelected) AppCtaBg else AppSurfaceLow)
                                                    .clickable { selectedMigrationCategoryId = altCat.id }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                V2Tile(color = vis.color, icon = vis.icon, size = 20.dp)
                                                Text(
                                                    text = altCat.name,
                                                    color = if (isSelected) AppCtaFg else SwInk,
                                                    style = v2T(12f, FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .pressableNoIndication(scale = 0.97f) {
                                                selectedMigrationCategoryId?.let { destId ->
                                                    onDeleteWithStrategy?.invoke(existing.id, CategoryDeletion.Migrate(destId))
                                                    showDeleteStrategyDialog = false
                                                    onDismiss()
                                                }
                                            },
                                        color = AppCtaBg,
                                        shape = RoundedCornerShape(percent = 50)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("Move transactions & delete", color = AppCtaFg, style = v2T(13.5f, FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteStrategyDialog = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = AppOnSurfaceVariant,
                                    style = v2T(13.5f, FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun V2AccountsScreen(
    accounts: List<Account>,
    archivedAccounts: List<Account>,
    totalBalanceCents: Long,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRestore: (Long) -> Unit
) {
    var archivedExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp)
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
                Text("All accounts", color = SwInk, style = v2T(15f, FontWeight.Bold))
                V2CircleButton(
                    icon = Icons.Filled.Add,
                    onClick = onAdd,
                    contentDescription = "Add account"
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                V2Eyebrow(text = "Net worth · across ${accounts.size}")
                V2Amount(
                    cents = totalBalanceCents,
                    size = 42f,
                    color = SwInk,
                    animateValue = true
                )
                val negCount = accounts.count { it.currentBalanceCents < 0L }
                val archivedCount = archivedAccounts.size
                val parts = buildList {
                    if (negCount > 0) add("$negCount account${if (negCount == 1) "" else "s"} in the red")
                    if (archivedCount > 0) add("$archivedCount archived")
                }
                if (parts.isNotEmpty()) {
                    Text(
                        text = parts.joinToString(" · "),
                        color = AppOnSurfaceVariant,
                        style = v2T(13f, FontWeight.Medium)
                    )
                }
            }
        }
        if (accounts.isNotEmpty()) {
            val featured = accounts.maxByOrNull { it.currentBalanceCents } ?: accounts.first()
            val rest = accounts.filter { it.id != featured.id }
            item {
                // Featured-account headline colors must read on the hero
                // gradient (light pastel vs dark indigo). Same recipe as
                // V2HeroBalance — single isDark flip, no two-token system.
                val isDark = com.spendwise.app.ui.theme.AppIsDark
                val featTextPrimary = if (isDark) Color.White else SwInk
                val featSubtitle = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF15121F).copy(alpha = 0.65f)
                val featTileBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White
                val featTileTint = if (isDark) Color.White else SwInk
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                    V2GradientHero(onClick = { onEdit(featured.id) }) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    color = featTileBg,
                                    shape = RoundedCornerShape(14.dp),
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val vis = visualForAccount(featured)
                                        Icon(vis.icon, null, tint = featTileTint, modifier = Modifier.size(22.dp))
                                    }
                                }
                                V2Pill(
                                    text = "Edit",
                                    variant = V2PillVariant.Soft,
                                    leading = Icons.Filled.Edit,
                                    onClick = { onEdit(featured.id) }
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${featured.name} · ${featured.type.displayLabel}",
                                    color = featSubtitle,
                                    style = v2T(13f, FontWeight.SemiBold)
                                )
                                V2Amount(
                                    cents = featured.currentBalanceCents,
                                    size = 32f,
                                    color = featTextPrimary,
                                    animateValue = true
                                )
                            }
                        }
                    }
                }
            }
            items(rest, key = { it.id }) { account ->
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    V2AccountRow(account = account, onClick = { onEdit(account.id) })
                }
            }
        }
        item {
            // Add account dashed CTA
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            width = 1.5.dp,
                            color = AppSurfaceContainerHigh.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .pressableNoIndication(scale = 0.985f, onClick = onAdd)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Add, null, tint = AppOnSurface, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add account", color = AppOnSurface, style = v2T(14f, FontWeight.Bold))
                }
            }
        }
        if (archivedAccounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp)
                        .pressableNoIndication(scale = 0.99f) { archivedExpanded = !archivedExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Archive, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(13.dp))
                        Text(
                            "Archived · ${archivedAccounts.size}",
                            color = AppOnSurfaceVariant,
                            style = v2T(12f, FontWeight.Bold, letter = 0.5f).copy(),
                        )
                    }
                    Icon(
                        if (archivedExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        null,
                        tint = AppOnSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(y = if (archivedExpanded) 0.dp else 0.dp)
                    )
                }
            }
            if (archivedExpanded) {
                items(archivedAccounts, key = { it.id }) { account ->
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                        V2ArchivedRow(account = account, onRestore = { onRestore(account.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun V2AccountRow(account: Account, onClick: () -> Unit) {
    val vis = visualForAccount(account)
    val neg = account.currentBalanceCents < 0L
    val isDark = AppIsDark
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (isDark) it
                else it.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    clip = false,
                    ambientColor = Color(0xFF281E50).copy(alpha = 0.2f),
                    spotColor = Color(0xFF281E50).copy(alpha = 0.2f)
                )
            }
            .pressable(scale = 0.985f, shape = RoundedCornerShape(22.dp), onClick = onClick),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, v2Hairline()) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            V2Tile(
                color = vis.color,
                icon = vis.icon,
                size = 48.dp,
                iconTint = if (account.type == AccountType.Credit) Color.White else SwInk
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, color = SwInk, style = v2T(15f, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(account.type.displayLabel, color = AppOnSurfaceVariant, style = v2T(12.5f, FontWeight.Medium))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                V2Amount(
                    cents = account.currentBalanceCents,
                    size = 18f,
                    sign = if (neg) '−' else null,
                    color = if (neg) SwNeg else SwInk
                )
                Text(
                    text = if (neg) "owed" else "available",
                    color = AppOnSurfaceVariant,
                    style = v2T(11f, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun V2ArchivedRow(account: Account, onRestore: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceLow,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val vis = visualForAccount(account)
            V2Tile(color = vis.color, icon = vis.icon, size = 42.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name,
                    color = SwInk.copy(alpha = 0.65f),
                    style = v2T(14.5f, FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    "${account.type.displayLabel} · archived",
                    color = AppOnSurfaceVariant,
                    style = v2T(12f, FontWeight.Medium)
                )
            }
            Surface(
                modifier = Modifier.pressableNoIndication(onClick = onRestore),
                color = AppSurface,
                shape = RoundedCornerShape(percent = 50),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, v2Hairline())
            ) {
                Text(
                    "Restore",
                    color = SwInk,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = v2T(12f, FontWeight.Bold)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  06 · ACCOUNT FORM (modal sheet)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun V2AccountFormSheet(
    existing: Account?,
    onSaveCreate: (
        nameInput: String,
        type: AccountType,
        startingBalanceInput: String,
        iconName: String,
        color: Long
    ) -> String?,
    onSaveUpdate: (
        id: Long,
        nameInput: String,
        type: AccountType,
        startingBalanceInput: String,
        iconName: String,
        color: Long
    ) -> String?,
    onArchive: suspend (Long) -> String?,
    onClose: () -> Unit
) {
    val isEdit = existing != null
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.type ?: AccountType.Bank)
    }
    var startingBalance by rememberSaveable(existing?.id) {
        mutableStateOf(
            if (existing != null) MoneyFormatter.centsToInput(existing.startingBalanceCents)
            else ""
        )
    }
    var iconName by rememberSaveable(existing?.id) { mutableStateOf(existing?.iconName ?: "bank") }
    var color by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.color ?: 0xFFC5BCF8L)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isDark = AppIsDark

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background((if (isDark) Color.Black else Color(0xFF15121F)).copy(alpha = 0.35f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(AppSurface)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppSurfaceContainer)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V2CircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onClose,
                    contentDescription = "Back",
                    size = 38.dp,
                    bg = if (isDark) AppSurfaceLow else null
                )
                Text(
                    text = if (isEdit) "Edit account" else "New account",
                    color = SwInk,
                    style = v2T(15f, FontWeight.Bold)
                )
                Spacer(Modifier.size(38.dp))
            }
            // Preview row
            V2SheetSectionLabel("Preview")
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppSurfaceLow,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        V2Tile(
                            color = Color(color.toULong().toLong() or 0xFF000000L.toULong().toLong().toLong()).let {
                                Color(color)
                            },
                            icon = iconFromName(iconName),
                            size = 50.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = v2T(17f, FontWeight.Bold).copy(color = SwInk),
                                cursorBrush = SolidColor(SwInk),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (name.isBlank()) {
                                            Text("Account name", color = AppOnSurfaceVariant, style = v2T(17f, FontWeight.Bold))
                                        }
                                        inner()
                                    }
                                }
                            )
                            Text(
                                "${type.displayLabel} · MYR",
                                color = AppOnSurfaceVariant,
                                style = v2T(12f, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
            // Type grid (2x2)
            V2SheetSectionLabel("Type")
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rows = listOf(
                    listOf(AccountType.Cash, AccountType.Bank),
                    listOf(AccountType.EWallet, AccountType.Credit)
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { t ->
                            V2TypeTile(
                                type = t,
                                selected = type == t,
                                modifier = Modifier.weight(1f),
                                onClick = { type = t }
                            )
                        }
                    }
                }
            }
            // Starting balance
            V2SheetSectionLabel("Starting balance")
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppSurfaceLow)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("RM", color = AppOnSurfaceVariant, style = v2N(16f, FontWeight.SemiBold))
                    BasicTextField(
                        value = startingBalance,
                        onValueChange = { startingBalance = sanitizeAmountSimple(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = v2N(28f, FontWeight.ExtraBold).copy(color = SwInk),
                        cursorBrush = SolidColor(SwInk),
                        decorationBox = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (startingBalance.isBlank()) {
                                    Text("0.00", color = AppOnSurfaceVariant.copy(alpha = 0.5f), style = v2N(28f, FontWeight.ExtraBold))
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // Icon picker
            V2SheetSectionLabel("Icon")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("bank", "wallet", "card", "cash", "phone", "box").forEach { iconKey ->
                    val selected = iconName == iconKey
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) SwVioletSoft else AppSurfaceLow)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) (if (isDark) SwViolet else SwInk) else Color.Transparent,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .pressableNoIndication(scale = 0.94f) { iconName = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconFromName(iconKey),
                            contentDescription = iconKey,
                            tint = SwInk,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            // Color picker
            V2SheetSectionLabel("Tile color")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val palette = listOf(
                    0xFFC5BCF8L,
                    0xFFF8C99BL,
                    0xFF9FE3C5L,
                    0xFFF8C0D6L,
                    0xFFB8D5FFL,
                    0xFFFFE08AL,
                    0xFFF4A39BL
                )
                palette.forEach { c ->
                    val selected = color == c
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pressableNoIndication(scale = 0.92f) { color = c },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (selected) 28.dp else 32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = SwInk,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
            if (!error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                ) {
                    Text(error!!, color = SwNeg, style = v2T(13f, FontWeight.SemiBold))
                }
            }
            // Save + Archive
            val ctaColor = if (isDark) SwViolet else SwInk
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .let {
                            if (isDark) it.shadow(
                                elevation = 18.dp,
                                shape = RoundedCornerShape(percent = 50),
                                clip = false,
                                ambientColor = SwViolet.copy(alpha = 0.7f),
                                spotColor = SwViolet.copy(alpha = 0.7f)
                            ) else it
                        }
                        .pressableNoIndication(scale = 0.97f) {
                            val result = if (isEdit) {
                                onSaveUpdate(existing!!.id, name, type, startingBalance, iconName, color)
                            } else {
                                onSaveCreate(name, type, startingBalance, iconName, color)
                            }
                            if (result == null) onClose() else error = result
                        },
                    color = ctaColor,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "Save changes" else "Create account",
                            color = Color.White,
                            style = v2T(15f, FontWeight.Bold)
                        )
                    }
                }
                if (isEdit) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressableNoIndication(scale = 0.97f) {
                                scope.launch {
                                    val err = onArchive(existing!!.id)
                                    if (err == null) onClose() else error = err
                                }
                            },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(percent = 50),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, SwNeg)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Archive, null, tint = SwNeg, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Archive account", color = SwNeg, style = v2T(14f, FontWeight.Bold))
                        }
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun V2TypeTile(type: AccountType, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isDark = AppIsDark
    val bg = if (selected) (if (isDark) SwInk else SwInk) else AppSurfaceLow
    val fg = if (selected) (if (isDark) Color(0xFF0F0D1A) else Color.White) else SwInk
    val icon = when (type) {
        AccountType.Cash -> Icons.Filled.Payments
        AccountType.Bank -> Icons.Filled.AccountBalance
        AccountType.EWallet -> Icons.Filled.PhoneAndroid
        AccountType.Credit -> Icons.Filled.CreditCard
    }
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .pressableNoIndication(scale = 0.96f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        selected && isDark -> Color(0xFF0F0D1A).copy(alpha = 0.18f)
                        selected -> Color.White.copy(alpha = 0.15f)
                        isDark -> AppSurface
                        else -> Color.White
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Text(type.displayLabel, color = fg, style = v2T(14f, FontWeight.Bold), modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(16.dp))
    }
}

private fun sanitizeAmountSimple(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) filtered
    else filtered.substring(0, firstDot + 1) +
        filtered.substring(firstDot + 1).replace(".", "").take(2)
}

private fun iconFromName(name: String): ImageVector = when (name) {
    "bank" -> Icons.Filled.AccountBalance
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "card" -> Icons.Filled.CreditCard
    "cash" -> Icons.Filled.Payments
    "phone" -> Icons.Filled.PhoneAndroid
    "restaurant", "food" -> Icons.Filled.Restaurant
    "shopping_bag", "cart", "grocery" -> Icons.Filled.ShoppingCart
    "directions_car", "car", "transport" -> Icons.Filled.DirectionsCar
    "house", "home", "rent" -> Icons.Filled.Home
    "movie", "film" -> Icons.Filled.Movie
    "gift" -> Icons.Filled.CardGiftcard
    "favorite", "heart", "health" -> Icons.Filled.Favorite
    "box" -> Icons.Filled.Inventory2
    "spark", "salary", "bonus", "account_balance_wallet" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.Receipt
}

private fun v2CategoryPalette(): List<Long> = listOf(
    0xFFC5BCF8L,
    0xFFF8C99BL,
    0xFF9FE3C5L,
    0xFFF8C0D6L,
    0xFFB8D5FFL,
    0xFFFFE08AL,
    0xFFF4A39BL,
    0xFFFCE0BCL
)

// ═════════════════════════════════════════════════════════════════════════════
//  Month picker — bottom sheet
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Quick-chip identity for the month-picker top row. "Custom range" is the
 * only chip that opens another sheet rather than picking a single month.
 */
private enum class V2MonthQuickScope { ThisMonth, LastMonth, YTD, AllTime, CustomRange }

/**
 * Bottom-sheet month picker. Matches v2 design artboard 10 — quick-chip
 * scope row, year switcher with "X months tracked", and a 4×3 grid of
 * month *cards* (not pills) showing each month's spend total and a mini
 * bar relative to the year's max. Footer line interprets the selected
 * month vs the visible-year average ("quiet"/"busy" framing).
 *
 * The chips' semantic intent ("YTD", "All time") is broader than what a
 * per-month dashboard can express today — we approximate by jumping to
 * the natural anchor month (Jan / earliest with data). When the rest of
 * the app gains a "date range" concept these will get richer behaviour.
 */
@Composable
internal fun V2MonthPickerSheet(
    visible: Boolean,
    selected: YearMonth,
    monthsWithData: Set<YearMonth>,
    monthSpendCents: Map<YearMonth, Long>,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    onCustomRange: () -> Unit = {}
) {
    val today = remember { YearMonth.now(ZONE_M) }
    val isDark = AppIsDark
    val yearRange = remember(monthsWithData, today) {
        val earliest = monthsWithData.minOfOrNull { it.year } ?: today.year
        earliest.coerceAtMost(today.year)..today.year
    }
    var visibleYear by remember { mutableStateOf(selected.year) }
    LaunchedEffect(visible, selected) { if (visible) visibleYear = selected.year }

    val months = remember(visibleYear) { (1..12).map { YearMonth.of(visibleYear, it) } }
    val tracked = remember(visibleYear, monthsWithData) {
        monthsWithData.count { it.year == visibleYear }
    }
    val maxSpend = remember(visibleYear, monthSpendCents) {
        months.maxOfOrNull { monthSpendCents[it] ?: 0L } ?: 0L
    }
    val selectedSpendCents = monthSpendCents[selected] ?: 0L
    val avgSpend = remember(visibleYear, monthSpendCents) {
        val withSpend = months.filter { (monthSpendCents[it] ?: 0L) > 0L }
        if (withSpend.isEmpty()) 0L
        else withSpend.sumOf { monthSpendCents[it] ?: 0L } / withSpend.size
    }

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
                            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 20.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── Grabber ────────────────────────────────────────
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(AppSurfaceContainer)
                            )
                        }

                        // ── Header: close · title · empty spacer ──────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            V2CircleButton(
                                icon = Icons.Filled.Close,
                                onClick = onDismiss,
                                contentDescription = "Close",
                                size = 38.dp
                            )
                            Text("Jump to a month", color = SwInk, style = v2T(15f, FontWeight.Bold))
                            Spacer(modifier = Modifier.size(38.dp))
                        }

                        // ── Quick scope chips ──────────────────────────────
                        val earliestData = remember(monthsWithData) {
                            monthsWithData.minOrNull()
                        }
                        V2MonthQuickChipRow(
                            selected = inferQuickScope(selected, today, earliestData),
                            onPick = { scope ->
                                when (scope) {
                                    V2MonthQuickScope.ThisMonth -> onSelect(today)
                                    V2MonthQuickScope.LastMonth -> onSelect(today.minusMonths(1))
                                    V2MonthQuickScope.YTD -> onSelect(YearMonth.of(today.year, 1))
                                    V2MonthQuickScope.AllTime -> onSelect(earliestData ?: today)
                                    V2MonthQuickScope.CustomRange -> onCustomRange()
                                }
                            }
                        )

                        // ── Year switcher row ──────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    visibleYear.toString(),
                                    color = SwInk,
                                    style = v2N(28f, FontWeight.ExtraBold),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = if (tracked == 0) "· no data" else "· $tracked ${if (tracked == 1) "month" else "months"} tracked",
                                    color = AppOnSurfaceVariant,
                                    style = v2T(12f, FontWeight.SemiBold),
                                    modifier = Modifier.padding(bottom = 7.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                V2YearStepButton(
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    enabled = visibleYear > yearRange.first,
                                    onClick = { if (visibleYear > yearRange.first) visibleYear-- },
                                    contentDescription = "Previous year"
                                )
                                V2YearStepButton(
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    enabled = visibleYear < yearRange.last,
                                    onClick = { if (visibleYear < yearRange.last) visibleYear++ },
                                    contentDescription = "Next year"
                                )
                            }
                        }

                        // ── Month card grid (4 × 3) ────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            months.chunked(4).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { ym ->
                                        val ymSpend = monthSpendCents[ym] ?: 0L
                                        val isFuture = ym > today
                                        val hasData = ym in monthsWithData || ym == today
                                        val isSelected = ym == selected
                                        Box(modifier = Modifier.weight(1f)) {
                                            V2MonthCard(
                                                month = ym,
                                                spendCents = ymSpend,
                                                maxSpendCents = maxSpend,
                                                selected = isSelected,
                                                isFuture = isFuture,
                                                hasData = hasData,
                                                isDark = isDark,
                                                onClick = { onSelect(ym) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Footer insight ─────────────────────────────────
                        if (avgSpend > 0L && selectedSpendCents > 0L) {
                            val deltaPct = ((selectedSpendCents - avgSpend).toFloat() / avgSpend.toFloat() * 100f).toInt()
                            val (framing, accent) = when {
                                deltaPct <= -5 -> "a quiet month" to SwViolet
                                deltaPct >= 5 -> "a busy month" to SwNeg
                                else -> "an average month" to AppOnSurfaceVariant
                            }
                            val direction = when {
                                deltaPct == 0 -> "on avg"
                                deltaPct < 0 -> "${-deltaPct}% below avg"
                                else -> "$deltaPct% above avg"
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    buildString {
                                        append(selected.format(DateTimeFormatter.ofPattern("MMM")))
                                        append(" was ")
                                        append(framing)
                                        append(" · ")
                                        append(direction)
                                    },
                                    color = AppOnSurfaceVariant,
                                    style = v2T(12f, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2MonthQuickChipRow(
    selected: V2MonthQuickScope?,
    onPick: (V2MonthQuickScope) -> Unit
) {
    val chips = listOf(
        V2MonthQuickScope.ThisMonth to "This month",
        V2MonthQuickScope.LastMonth to "Last month",
        V2MonthQuickScope.YTD to "YTD",
        V2MonthQuickScope.AllTime to "All time"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (scope, label) ->
            V2Pill(
                text = label,
                variant = if (selected == scope) V2PillVariant.Dark else V2PillVariant.Soft,
                onClick = { onPick(scope) }
            )
        }
        V2Pill(
            text = "Custom range",
            leading = Icons.Filled.Archive,
            variant = V2PillVariant.Soft,
            onClick = { onPick(V2MonthQuickScope.CustomRange) }
        )
    }
}

/**
 * Match the selected YearMonth to one of the quick scopes for chip
 * highlighting. Falls back to null when the user is on an arbitrary
 * month (e.g. picked one from the grid).
 */
private fun inferQuickScope(
    selected: YearMonth,
    today: YearMonth,
    earliestData: YearMonth?
): V2MonthQuickScope? = when (selected) {
    today -> V2MonthQuickScope.ThisMonth
    today.minusMonths(1) -> V2MonthQuickScope.LastMonth
    YearMonth.of(today.year, 1) -> if (today.monthValue == 1) V2MonthQuickScope.ThisMonth else V2MonthQuickScope.YTD
    earliestData -> if (selected == today || selected == today.minusMonths(1)) null else V2MonthQuickScope.AllTime
    else -> null
}

/** Single month "card" in the picker grid — name, spend total, mini bar. */
@Composable
private fun V2MonthCard(
    month: YearMonth,
    spendCents: Long,
    maxSpendCents: Long,
    selected: Boolean,
    isFuture: Boolean,
    hasData: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cardBg = when {
        selected -> if (isDark) SwViolet else SwInk
        isFuture -> Color.Transparent
        else -> AppSurfaceLow
    }
    val titleColor = when {
        selected -> Color.White
        isFuture -> AppOnSurfaceVariant.copy(alpha = 0.55f)
        else -> SwInk
    }
    val subColor = when {
        selected -> Color.White.copy(alpha = 0.7f)
        else -> AppOnSurfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .then(
                if (isFuture) Modifier.border(
                    BorderStroke(1.dp, AppSurfaceContainer),
                    RoundedCornerShape(18.dp)
                ) else Modifier
            )
            .pressableNoIndication(
                scale = 0.96f,
                enabled = hasData && !isFuture,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMM")),
                color = titleColor,
                style = v2T(13f, FontWeight.Bold, letter = -0.13f)
            )
            Text(
                text = if (isFuture) "—" else compactRm(spendCents),
                color = subColor,
                style = v2N(10f, FontWeight.SemiBold)
            )
            // Mini bar — only when there's data and not a future month
            if (!isFuture) {
                val fraction = if (maxSpendCents <= 0L) 0f
                else (spendCents.toFloat() / maxSpendCents.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.22f)
                            else AppSurfaceContainer
                        )
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (selected) Color.White else SwViolet)
                        )
                    }
                }
            }
        }
    }
}

/** Small year-step icon button used in the picker year switcher. */
@Composable
private fun V2YearStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val alpha = if (enabled) 1f else 0.35f
    Surface(
        modifier = Modifier
            .size(36.dp)
            .pressableNoIndication(scale = 0.92f, enabled = enabled, onClick = onClick),
        color = AppSurfaceLow,
        shape = CircleShape,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = SwInk.copy(alpha = alpha),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Render cents as a compact "RM" amount — under 1000 ringgit shows the
 * exact value ("RM 480"), at or above 1000 shows "RM 2.7k" so the
 * 4-card-wide grid never wraps. The "k" suffix keeps one decimal place
 * (rounded) so 2680 reads as "2.7k", not "3k".
 */
private fun compactRm(cents: Long): String {
    val ringgit = cents / 100L
    return if (ringgit < 1000L) {
        "RM $ringgit"
    } else {
        val k = ringgit.toDouble() / 1000.0
        val rounded = (k * 10).toLong().toDouble() / 10.0
        if (rounded % 1.0 == 0.0) "RM ${rounded.toLong()}k"
        else "RM ${"%.1f".format(rounded)}k"
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Account picker — bottom sheet
//
//  Mirrors V2MonthPickerSheet's structure: scrim + slide-in surface + grab
//  handle + title + scrollable selectable rows. Triggered from the "Account ▾"
//  filter chip on the Activity screen. Passing `selectedAccountId = null`
//  represents the "All accounts" row (no filter).
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun V2AccountPickerSheet(
    visible: Boolean,
    accounts: List<Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = AppIsDark
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
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text("Filter by account", color = SwInk, style = v2T(15f, FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            V2AccountPickerRow(
                                title = "All accounts",
                                subtitle = "Across ${accounts.size} account${if (accounts.size == 1) "" else "s"}",
                                icon = Icons.Filled.AccountBalance,
                                tileColor = if (isDark) AppSurfaceLow else SwVioletSoft,
                                tileTint = SwInk,
                                trailingCents = null,
                                selected = selectedAccountId == null,
                                onClick = { onSelect(null) }
                            )
                            accounts.forEach { account ->
                                val vis = visualForAccount(account)
                                V2AccountPickerRow(
                                    title = account.name,
                                    subtitle = account.type.displayLabel,
                                    icon = vis.icon,
                                    tileColor = vis.color,
                                    tileTint = if (account.type == AccountType.Credit) Color.White else SwInk,
                                    trailingCents = account.currentBalanceCents,
                                    selected = selectedAccountId == account.id,
                                    onClick = { onSelect(account.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2AccountPickerRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tileColor: Color,
    tileTint: Color,
    trailingCents: Long?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, shape = RoundedCornerShape(18.dp), onClick = onClick),
        color = if (selected) AppSurfaceContainerHigh else AppSurfaceLow,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            V2Tile(color = tileColor, icon = icon, size = 40.dp, iconTint = tileTint)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SwInk,
                    style = v2T(14.5f, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = AppOnSurfaceVariant,
                    style = v2T(12f, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingCents != null) {
                val neg = trailingCents < 0L
                V2Amount(
                    cents = trailingCents,
                    size = 14f,
                    sign = if (neg) '−' else null,
                    color = if (neg) SwNeg else SwInk
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = SwViolet,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Category picker - bottom sheet used by the Activity filter strip.
@Composable
internal fun V2CategoryPickerSheet(
    visible: Boolean,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = AppIsDark
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(AppSurfaceContainer)
                                )
                            }
                        }
                        item {
                            Text("Filter by category", color = SwInk, style = v2T(15f, FontWeight.Bold))
                        }
                        item {
                            V2CategoryPickerRow(
                                title = "All categories",
                                subtitle = "${categories.size} categories",
                                icon = Icons.Filled.Receipt,
                                tileColor = if (isDark) AppSurfaceLow else SwVioletSoft,
                                selected = selectedCategoryId == null,
                                onClick = { onSelect(null) }
                            )
                        }
                        items(categories, key = { it.id }) { category ->
                            val vis = visualForCategory(category.name, category.iconName, category.color)
                            V2CategoryPickerRow(
                                title = category.name,
                                subtitle = if (category.isIncomeAdjustment) "Income" else "Expense",
                                icon = vis.icon,
                                tileColor = vis.color,
                                selected = selectedCategoryId == category.id,
                                onClick = { onSelect(category.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2CategoryPickerRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tileColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, shape = RoundedCornerShape(18.dp), onClick = onClick),
        color = if (selected) AppSurfaceContainerHigh else AppSurfaceLow,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            V2Tile(color = tileColor, icon = icon, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SwInk,
                    style = v2T(14.5f, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = AppOnSurfaceVariant,
                    style = v2T(12f, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = SwViolet,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Custom range — bottom sheet
//
//  Two endpoint cards (From / To) above a single-month calendar grid.
//  Tapping a card sets the active endpoint, tapping a day updates it. The
//  range fill is rendered per-cell: middle days get a full-width lavender
//  band, start/end days get a half-band that meets at the centered ink
//  endpoint pill. Apply returns the chosen LocalDate range; Back returns
//  control to the month picker without applying.
// ═════════════════════════════════════════════════════════════════════════════

private enum class V2RangeEnd { From, To }

@Composable
internal fun V2CustomRangeSheet(
    visible: Boolean,
    initialFrom: LocalDate,
    initialTo: LocalDate,
    // Summary-line lookup (non-income count + spend for a date range) — a
    // SQL aggregate, since a custom range can span years the UI no longer
    // keeps in memory.
    queryRangeStats: suspend (LocalDate, LocalDate) -> RangeStats,
    onApply: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    // Resetting the editable state every time the sheet is shown means the
    // user gets a clean starting point even if they cancelled mid-edit.
    // `key(visible)` would also work; this is just more explicit.
    var from by remember(visible, initialFrom) { mutableStateOf(initialFrom) }
    var to by remember(visible, initialTo) { mutableStateOf(initialTo) }
    var activeEnd by remember(visible) { mutableStateOf(V2RangeEnd.From) }
    var visibleMonth by remember(visible, initialFrom) {
        mutableStateOf(YearMonth.from(initialFrom))
    }

    // Range bookkeeping for the summary line. produceState re-queries as the
    // user drags either endpoint; each lookup is a single indexed SUM.
    val dayCount = ChronoUnit.DAYS.between(from, to).toInt() + 1
    val rangeStats by produceState(RangeStats(0, 0L), from, to, visible) {
        if (visible) value = queryRangeStats(from, to)
    }
    val rangeTxCount = rangeStats.entryCount
    val rangeSpendCents = rangeStats.spendCents

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(SpendWiseMotion.Sheet, easing = SpendWiseMotion.EaseStandard)),
        exit = fadeOut(tween(SpendWiseMotion.ModalExit, easing = SpendWiseMotion.EaseStandard))
    ) {
        BackHandler(onBack = onBack)
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
                            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 20.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── Grabber ────────────────────────────────────
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(AppSurfaceContainer)
                            )
                        }

                        // ── Header: back · title · Apply ───────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            V2CircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                onClick = onBack,
                                contentDescription = "Back to month picker",
                                size = 38.dp
                            )
                            Text("Custom range", color = SwInk, style = v2T(15f, FontWeight.Bold))
                            Text(
                                text = "Apply",
                                color = SwViolet,
                                style = v2T(13.5f, FontWeight.Bold),
                                modifier = Modifier
                                    .pressableNoIndication(
                                        scale = 0.94f,
                                        onClick = { onApply(from, to) }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }

                        // ── Endpoint cards (From / To) ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V2RangeEndCard(
                                label = "From",
                                date = from,
                                active = activeEnd == V2RangeEnd.From,
                                modifier = Modifier.weight(1f),
                                onClick = { activeEnd = V2RangeEnd.From }
                            )
                            V2RangeEndCard(
                                label = "To",
                                date = to,
                                active = activeEnd == V2RangeEnd.To,
                                modifier = Modifier.weight(1f),
                                onClick = { activeEnd = V2RangeEnd.To }
                            )
                        }

                        // ── Month header with prev/next ───────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                color = SwInk,
                                style = v2T(16f, FontWeight.ExtraBold, letter = -0.16f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                V2YearStepButton(
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    enabled = true,
                                    onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                                    contentDescription = "Previous month"
                                )
                                V2YearStepButton(
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    enabled = true,
                                    onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                                    contentDescription = "Next month"
                                )
                            }
                        }

                        // ── DOW row ───────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("S","M","T","W","T","F","S").forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = d,
                                        color = AppOnSurfaceVariant,
                                        style = v2T(11f, FontWeight.Bold, letter = 0.4f)
                                    )
                                }
                            }
                        }

                        // ── Calendar grid with range fill ─────────────
                        V2RangeCalendarGrid(
                            month = visibleMonth,
                            from = from,
                            to = to,
                            onDayClick = { d ->
                                when (activeEnd) {
                                    V2RangeEnd.From -> {
                                        if (d.isAfter(to)) {
                                            // Tapped a date past the current "to"
                                            // → swap: old to becomes from, tapped
                                            // date becomes to.
                                            from = to
                                            to = d
                                        } else {
                                            from = d
                                        }
                                        activeEnd = V2RangeEnd.To
                                    }
                                    V2RangeEnd.To -> {
                                        if (d.isBefore(from)) {
                                            to = from
                                            from = d
                                        } else {
                                            to = d
                                        }
                                    }
                                }
                            }
                        )

                        // ── Summary ───────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$dayCount ${if (dayCount == 1) "day" else "days"} · $rangeTxCount ${if (rangeTxCount == 1) "transaction" else "transactions"} in this range",
                                color = AppOnSurfaceVariant,
                                style = v2T(12f, FontWeight.Medium)
                            )
                            Text(
                                text = compactRm(rangeSpendCents),
                                color = SwInk,
                                style = v2N(13.5f, FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2RangeEndCard(
    label: String,
    date: LocalDate,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = AppIsDark
    val bg = if (active) (if (isDark) SwViolet else SwInk) else AppSurfaceLow
    val titleColor = if (active) Color.White.copy(alpha = 0.7f) else AppOnSurfaceVariant
    val valueColor = if (active) Color.White else SwInk
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (active) Modifier.border(
                    BorderStroke(2.dp, if (isDark) SwVioletSoft else SwViolet),
                    RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .background(bg)
            .pressableNoIndication(scale = 0.98f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label.uppercase(),
                color = titleColor,
                style = v2T(11f, FontWeight.Bold, letter = 0.66f)
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                color = valueColor,
                style = v2T(16f, FontWeight.ExtraBold, letter = -0.16f)
            )
        }
    }
}

/**
 * Calendar grid that draws the active range with a continuous lavender
 * band: middle days get a full-cell band, start / end days get a half-cell
 * band that meets at a centered ink endpoint pill. Days outside the
 * visible month render as blank cells so the layout stays a clean rectangle.
 */
@Composable
private fun V2RangeCalendarGrid(
    month: YearMonth,
    from: LocalDate,
    to: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val daysInMonth = month.lengthOfMonth()
    // Leading offset so that day 1 lands in the right column. Sunday-first
    // grid: convert ISO DayOfWeek (Mon=1..Sun=7) to Sun=0..Sat=6.
    val firstDow = month.atDay(1).dayOfWeek.value % 7
    val totalCells = firstDow + daysInMonth
    val rowCount = (totalCells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        for (rowIndex in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIndex = rowIndex * 7 + col
                    val dayNumber = cellIndex - firstDow + 1
                    val inMonth = dayNumber in 1..daysInMonth
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        if (inMonth) {
                            val d = month.atDay(dayNumber)
                            V2RangeDayCell(
                                date = d,
                                from = from,
                                to = to,
                                onClick = { onDayClick(d) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2RangeDayCell(
    date: LocalDate,
    from: LocalDate,
    to: LocalDate,
    onClick: () -> Unit
) {
    val isStart = date == from
    val isEnd = date == to
    val isSingleton = isStart && isEnd
    val isMiddle = !isSingleton && !date.isBefore(from) && !date.isAfter(to) && !isStart && !isEnd
    val inRange = !date.isBefore(from) && !date.isAfter(to)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pressableNoIndication(scale = 0.94f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Range fill band — drawn before any pill so the endpoint pill sits
        // on top of it. The 4dp top/bottom inset matches the design's
        // `inset: '4px 0'` band style.
        when {
            isMiddle -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(SwVioletSoft)
            )
            isStart && !isSingleton -> Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .padding(vertical = 4.dp)
                    .align(Alignment.CenterEnd)
                    .background(SwVioletSoft)
            )
            isEnd && !isSingleton -> Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .padding(vertical = 4.dp)
                    .align(Alignment.CenterStart)
                    .background(SwVioletSoft)
            )
        }
        if (isStart || isEnd) {
            // Endpoint pill sits over the range band so the band reads as
            // continuous behind it; dark mode uses Claude's violet endpoint.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (AppIsDark) SwViolet else SwInk),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = Color.White,
                    style = v2N(13f, FontWeight.ExtraBold)
                )
            }
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (inRange) SwInk else AppOnSurfaceVariant,
                style = v2N(13f, if (inRange) FontWeight.Bold else FontWeight.Medium)
            )
        }
    }
}

@Composable
internal fun V2SettingsSheet(
    visible: Boolean,
    onManageAccounts: () -> Unit,
    onManageCategories: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    // Drag-to-dismiss with the scrim dimming in lockstep — see V2TxDetailSheet
    // for the pattern notes.
    val drag = rememberSheetDragState(visible)
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
                    drawRect(Color.Black.copy(alpha = 0.45f * (1f - drag.progress)))
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
                    shadowElevation = 24.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Text("Settings", color = SwInk, style = v2T(15f, FontWeight.Bold))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(AppSurfaceLow)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Manage Accounts
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onDismiss()
                                        onManageAccounts()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                V2Tile(color = SwVioletSoft, icon = Icons.Filled.AccountBalanceWallet, size = 36.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Manage accounts", color = SwInk, style = v2T(14f, FontWeight.Bold))
                                    Text("Add, edit, or archive your wallets", color = AppOnSurfaceVariant, style = v2T(12f, FontWeight.Medium))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(v2Hairline()))
                            
                            // Manage Categories
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onDismiss()
                                        onManageCategories()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                V2Tile(color = SwPeach, icon = Icons.Filled.Receipt, size = 36.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Manage categories", color = SwInk, style = v2T(14f, FontWeight.Bold))
                                    Text("Customise spending & income categories", color = AppOnSurfaceVariant, style = v2T(12f, FontWeight.Medium))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(v2Hairline()))

                            // Back up data — exports the entire ledger as a JSON file
                            // the user picks a location for via the system file
                            // picker. Saving to Drive or Files = a portable backup
                            // they keep across reinstalls.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onDismiss()
                                        onBackup()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                V2Tile(color = SwMint, icon = Icons.Filled.SaveAlt, size = 36.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Back up data", color = SwInk, style = v2T(14f, FontWeight.Bold))
                                    Text("Save a file with everything in your ledger", color = AppOnSurfaceVariant, style = v2T(12f, FontWeight.Medium))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(v2Hairline()))

                            // Restore from backup — replaces ALL current data with
                            // the contents of a chosen backup file. The parent
                            // screen guards this behind a confirmation modal so a
                            // mis-tap can't wipe data silently.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onDismiss()
                                        onRestore()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                V2Tile(color = SwSky, icon = Icons.Filled.Restore, size = 36.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Restore from backup", color = SwInk, style = v2T(14f, FontWeight.Bold))
                                    Text("Replace your data with a backup file", color = AppOnSurfaceVariant, style = v2T(12f, FontWeight.Medium))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Confirmation modal shown when the user taps "Restore from backup". Restore
// is destructive — it wipes every category/account/budget/expense the user
// currently has — so a mis-tap cannot proceed silently. Modelled after the
// same bottom-sheet pattern as V2SettingsSheet so it visually belongs to the
// settings flow. The actual SAF picker is launched by the parent only after
// the user confirms here.
@Composable
internal fun V2RestoreConfirmSheet(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    shadowElevation = 24.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Text("Restore from backup?", color = SwInk, style = v2T(17f, FontWeight.Bold))
                        Text(
                            "This will replace every expense, category, account, and budget you currently have with what's in the backup file. Your current data can't be recovered after this.",
                            color = AppOnSurfaceVariant,
                            style = v2T(13.5f, FontWeight.Medium)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onDismiss),
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLow,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Cancel", color = SwInk, style = v2T(14f, FontWeight.Bold))
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onDismiss()
                                        onConfirm()
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = SwNeg,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Choose backup", color = Color.White, style = v2T(14f, FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
