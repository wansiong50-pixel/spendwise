package com.spendwise.app.ui.botanical

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MerchantNames
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.Transfer
import com.spendwise.app.ui.theme.LocalPerfMode
import java.time.LocalDate
import kotlinx.coroutines.launch

/** What the Entry screen is editing, if anything. */
sealed interface EntryTarget {
    data class New(val kind: EntryKind, val date: LocalDate) : EntryTarget
    data class EditExpense(val expense: Expense, val isIncome: Boolean) : EntryTarget
    data class EditTransfer(val transfer: Transfer) : EntryTarget
}

/**
 * Add / edit an expense, income entry or transfer. Amounts use fixed-sen
 * entry; the date comes from the illustrated calendar and can't be in the
 * future (recurring rules cover what hasn't happened yet). Closing with
 * unsaved changes asks before discarding them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    target: EntryTarget,
    categories: List<Category>,
    accounts: List<Account>,
    recentExpenses: List<Expense>,
    formError: String?,
    onClearError: () -> Unit,
    onSaveExpense: (id: Long?, amount: String, categoryId: Long?, accountId: Long?, merchant: String, notes: String, date: String) -> Boolean,
    onSaveTransfer: (id: Long?, amount: String, fromId: Long?, toId: Long?, notes: String, date: String) -> Boolean,
    onCreateCategory: (name: String, iconName: String, color: Long, isIncome: Boolean, budget: String) -> String?,
    onClose: () -> Unit,
    onSaved: (date: LocalDate, updated: Boolean) -> Unit
) {
    val editingExpense = (target as? EntryTarget.EditExpense)?.expense
    val editingTransfer = (target as? EntryTarget.EditTransfer)?.transfer
    val isEdit = target !is EntryTarget.New
    val startKind = when (target) {
        is EntryTarget.New -> target.kind
        is EntryTarget.EditExpense -> if (target.isIncome) EntryKind.Income else EntryKind.Expense
        is EntryTarget.EditTransfer -> EntryKind.Transfer
    }
    val startDate = when (target) {
        is EntryTarget.New -> target.date
        is EntryTarget.EditExpense -> target.expense.occurredAtMillis.toKlDate()
        is EntryTarget.EditTransfer -> target.transfer.occurredAtMillis.toKlDate()
    }
    fun firstCategory(kind: EntryKind): Long? =
        categories.firstOrNull { it.isIncomeAdjustment == (kind == EntryKind.Income) }?.id

    var kind by rememberSaveable { mutableStateOf(startKind) }
    var cents by rememberSaveable { mutableStateOf(editingExpense?.amountCents ?: editingTransfer?.amountCents ?: 0L) }
    var categoryId by rememberSaveable { mutableStateOf(editingExpense?.categoryId ?: firstCategory(startKind)) }
    var accountId by rememberSaveable {
        mutableStateOf(editingExpense?.accountId ?: editingTransfer?.fromAccountId ?: accounts.firstOrNull()?.id)
    }
    var toAccountId by rememberSaveable {
        mutableStateOf(editingTransfer?.toAccountId ?: accounts.getOrNull(1)?.id)
    }
    var merchant by rememberSaveable { mutableStateOf(editingExpense?.merchant.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(editingExpense?.notes ?: editingTransfer?.notes.orEmpty()) }
    var dateText by rememberSaveable { mutableStateOf(startDate.toString()) }
    val date = LocalDate.parse(dateText)
    var amountError by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var calendarOpen by remember { mutableStateOf(false) }
    var discardOpen by remember { mutableStateOf(false) }
    var categoryFormOpen by remember { mutableStateOf(false) }
    var pendingCategoryName by remember { mutableStateOf<String?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val snapshot = listOf(kind, cents, categoryId, accountId, toAccountId, merchant, notes, dateText)
    val original = rememberSaveable { snapshot.toString() }
    val dirty = snapshot.toString() != original
    val amountFocus = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current
    val reduced = LocalPerfMode.current.reducedMotion

    LaunchedEffect(Unit) { onClearError() }
    // A category created from here is selected once it lands in the list.
    LaunchedEffect(categories, pendingCategoryName) {
        val name = pendingCategoryName ?: return@LaunchedEffect
        categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let {
            kind = if (it.isIncomeAdjustment) EntryKind.Income else EntryKind.Expense
            categoryId = it.id
            pendingCategoryName = null
        }
    }

    val close = { if (dirty) discardOpen = true else onClose() }
    BackHandler(enabled = !calendarOpen && !discardOpen && !categoryFormOpen) { close() }

    val kindCategories = categories.filter { it.isIncomeAdjustment == (kind == EntryKind.Income) }
    val incomeIds = remember(categories) { incomeCategoryIds(categories) }
    val suggestions = remember(recentExpenses, kind, merchant, incomeIds) {
        if (kind == EntryKind.Transfer) emptyList() else {
            val history = recentExpenses
                .asSequence()
                .filter { it.id != editingExpense?.id }
                .filter { (it.categoryId in incomeIds) == (kind == EntryKind.Income) }
                .sortedByDescending { it.createdAtMillis }
                .map { it.merchant }
                .filter { it.isNotBlank() }
                .toList()
            MerchantNames.suggest(merchant, history, limit = 5)
                .filterNot { it.equals(merchant.trim(), ignoreCase = true) }
        }
    }

    val save: () -> Unit = save@{
        onClearError()
        localError = null
        amountError = null
        if (cents <= 0L) {
            amountError = "Enter an amount greater than RM 0.00."
            runCatching { amountFocus.requestFocus() }
            return@save
        }
        if (date > todayKl()) {
            localError = "Choose today or an earlier date. Use a recurring schedule for future entries."
            return@save
        }
        if (accounts.isEmpty()) {
            localError = "Add an account before saving an entry."
            return@save
        }
        val amount = MoneyFormatter.centsToInput(cents)
        val ok = if (kind == EntryKind.Transfer) {
            if (toAccountId == null || toAccountId == accountId) {
                localError = "Choose two different accounts."
                return@save
            }
            onSaveTransfer(editingTransfer?.id, amount, accountId, toAccountId, notes, dateText)
        } else {
            if (kindCategories.none { it.id == categoryId }) {
                localError = "Choose a category."
                return@save
            }
            onSaveExpense(editingExpense?.id, amount, categoryId, accountId, merchant, notes, dateText)
        }
        if (ok) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSaved(date, isEdit)
        }
    }

    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var dragGap by remember { mutableStateOf<Float?>(null) }
    val collapsedGap = 28f
    val gapTarget = dragGap ?: if (expanded) 0f else collapsedGap
    val gap by animateDpAsState(gapTarget.dp, tween(if (reduced || dragGap != null) 0 else 180), label = "entryGap")
    val cardRadius by animateDpAsState(if (expanded) 0.dp else 36.dp, tween(if (reduced) 0 else 180), label = "entryRadius")
    val scroll = rememberScrollState()

    Box(
        Modifier
            .fillMaxSize()
            .background(Bot.PageDeep)
            .imePadding()
            .semantics { paneTitle = if (isEdit) "Edit entry" else "New entry" }
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(statusTop + gap))
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = cardRadius, topEnd = cardRadius))
            ) {
                val gradientEnd = with(density) { 960.dp.toPx() }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .heightIn(min = maxHeight)
                        .background(Brush.verticalGradient(listOf(Color(0xFF031EA5), Color.White), startY = 0f, endY = gradientEnd))
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp + navBottom)
                ) {
                    PullHandle(
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                        onDrag = { delta ->
                            val start = dragGap ?: if (expanded) 0f else collapsedGap
                            dragGap = (start + with(density) { delta.toDp().value }).coerceIn(0f, collapsedGap)
                        },
                        onDragEnd = {
                            val g = dragGap
                            if (g != null) expanded = g < collapsedGap / 2
                            dragGap = null
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0x33D9D9D9))
                                .botPress { calendarOpen = true }
                                .semantics { contentDescription = "Date: ${if (date == todayKl()) "Today" else dateLabel(date)}, change date" }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (date == todayKl()) "Today" else shortDateLabel(date),
                                style = body(18f, FontWeight.SemiBold),
                                color = Color.White
                            )
                            BotIcon(BotIcons.ChevronDown, size = 18.dp, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x12FFFFFF))
                                .border(1.dp, Color(0x30FFFFFF), CircleShape)
                                .botPress(onClick = close)
                                .semantics { contentDescription = "Close entry" },
                            contentAlignment = Alignment.Center
                        ) { BotIcon(BotIcons.Close, tint = Color.White) }
                    }
                    PillSegment(
                        options = EntryKind.entries,
                        selected = kind,
                        onSelect = { next ->
                            kind = next
                            if (next != EntryKind.Transfer) categoryId = firstCategory(next)
                            localError = null
                            onClearError()
                        },
                        label = { it.label },
                        tone = SegmentTone.Entry,
                        // An expense can become income and back, but a transfer
                        // lives in another table: switching across that line
                        // would mean deleting one entry and creating another.
                        isEnabled = { option ->
                            when {
                                editingTransfer != null -> option == EntryKind.Transfer
                                editingExpense != null -> option != EntryKind.Transfer
                                else -> true
                            }
                        },
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    AmountInput(
                        cents = cents,
                        onChange = {
                            cents = it
                            amountError = null
                        },
                        entry = true,
                        error = amountError,
                        focusRequester = amountFocus
                    )
                    amountError?.let { ErrorBox(it, Modifier.padding(bottom = 16.dp)) }
                    if (kind == EntryKind.Transfer) {
                        Text(
                            "Your total balance stays the same.",
                            style = body(14f, lineHeight = 1.5f),
                            color = Color(0xD9FFFFFF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp)
                        )
                    } else {
                        ChoiceRow(
                            label = "Category",
                            action = {
                                ChoiceAction("Add category", BotIcons.Plus) { categoryFormOpen = true }
                            }
                        ) {
                            kindCategories.forEach { category ->
                                CategoryChoice(category, selected = category.id == categoryId) {
                                    categoryId = category.id
                                    localError = null
                                }
                            }
                        }
                    }
                    ChoiceRow(label = if (kind == EntryKind.Transfer) "From account" else "Account") {
                        accounts.forEach { account ->
                            AccountChoice(account, selected = account.id == accountId) { accountId = account.id }
                        }
                    }
                    if (kind == EntryKind.Transfer) {
                        ChoiceRow(label = "To account") {
                            accounts.forEach { account ->
                                AccountChoice(account, selected = account.id == toAccountId) { toAccountId = account.id }
                            }
                        }
                    } else {
                        val nameLabel = if (kind == EntryKind.Income) "Income source" else "Merchant"
                        FieldLabel(nameLabel, FieldTone.Entry)
                        BotTextField(
                            value = merchant,
                            onValueChange = {
                                merchant = it
                                onClearError()
                            },
                            placeholder = if (kind == EntryKind.Income) "e.g. Monthly salary" else "e.g. Coffee shop",
                            tone = FieldTone.Entry,
                            maxLength = 100,
                            contentLabel = nameLabel,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (suggestions.isNotEmpty()) {
                            Column(Modifier.padding(bottom = 20.dp)) {
                                Text(
                                    if (merchant.isBlank()) "Recent entries" else "Matching previous entries",
                                    style = body(13f, FontWeight.SemiBold, lineHeight = 1.4f),
                                    color = Color(0xFF0B1740)
                                )
                                FlowRow(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suggestions.forEach { name ->
                                        Box(
                                            Modifier
                                                .heightIn(min = 44.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Color.White)
                                                .border(1.dp, Color(0xFFC3CDE1), RoundedCornerShape(18.dp))
                                                .botPress { merchant = name }
                                                .padding(horizontal = 14.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(name, style = body(14f, FontWeight.Medium, lineHeight = 1.4f), color = Color(0xFF203455))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FieldLabel("Notes · optional", FieldTone.Entry)
                    BotTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        tone = FieldTone.Entry,
                        singleLine = false,
                        minHeight = 84.dp,
                        maxLength = 1000,
                        contentLabel = "Notes",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    (localError ?: formError)?.takeIf { it.isNotBlank() }?.let { ErrorBox(it) }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = maxOf(16.dp, navBottom))
                ) {
                    BotButton(
                        if (isEdit) "Save changes" else "Save",
                        onClick = save,
                        dmSans = true,
                        color = Bot.ActionSolid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(99.dp), ambientColor = Color(0x5903145A), spotColor = Color(0x5903145A))
                    )
                }
            }
        }

        CalendarPicker(
            visible = calendarOpen,
            mode = CalendarMode.Date,
            initial = date,
            onCancel = { calendarOpen = false },
            onConfirm = {
                dateText = it.toString()
                calendarOpen = false
                localError = null
            }
        )
        CategoryFormSheet(
            visible = categoryFormOpen,
            existing = null,
            initialIncome = kind == EntryKind.Income,
            budgetCents = 0L,
            used = false,
            onSave = { name, iconName, color, isIncome, budget ->
                val error = onCreateCategory(name, iconName, color, isIncome, budget)
                if (error == null) {
                    pendingCategoryName = name.trim()
                    categoryFormOpen = false
                }
                error
            },
            onDelete = null,
            onDismiss = { categoryFormOpen = false }
        )
        BotSheet(visible = discardOpen, onDismiss = { discardOpen = false }, title = "Discard this entry?") {
            SheetText("Your unsaved changes will be lost.")
            ButtonRow {
                BotButton("Keep editing", onClick = { discardOpen = false }, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                BotButton(
                    "Discard entry",
                    onClick = {
                        discardOpen = false
                        onClose()
                    },
                    type = ButtonType.Danger,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

/** The grab bar that expands the card to full height (tap or drag). */
@Composable
private fun PullHandle(
    expanded: Boolean,
    onToggle: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-16).dp)
            .height(44.dp)
            .draggable(
                state = rememberDraggableState(onDrag),
                orientation = Orientation.Vertical,
                onDragStopped = { onDragEnd() }
            )
            .botPress(scale = 1f, onClick = onToggle)
            .semantics {
                contentDescription = if (expanded) "Collapse entry card" else "Expand entry card"
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0x8CFFFFFF))
        )
    }
}

/**
 * A labelled horizontal choice row. When the choices overflow, arrows page
 * through them (75% of the visible width) and dim at either end; swiping
 * works as usual.
 */
@Composable
private fun ChoiceRow(
    label: String,
    action: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val overflow = scroll.maxValue > 2
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = body(13f, FontWeight.SemiBold, lineHeight = 1.4f),
                color = Color(0xFF0B1740),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xB8FFFFFF))
                    .border(1.dp, Color(0xD9FFFFFF), RoundedCornerShape(99.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .semantics { heading() }
            )
            Spacer(Modifier.weight(1f))
            action?.invoke(this)
            if (overflow) {
                PagingButton(BotIcons.Back, "Previous ${label.lowercase()} choices", enabled = scroll.value > 2) {
                    scope.launch { scroll.page(-1) }
                }
                PagingButton(BotIcons.Next, "More ${label.lowercase()} choices", enabled = scroll.value < scroll.maxValue - 2) {
                    scope.launch { scroll.page(1) }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private suspend fun ScrollState.page(direction: Int) {
    val step = (viewportSize * 0.75f).toInt().coerceAtLeast(1)
    animateScrollTo((value + direction * step).coerceIn(0, maxValue))
}

@Composable
private fun PagingButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(Color(0x80FFFFFF))
            .border(1.dp, Color(0x90FFFFFF), CircleShape)
            .botPress(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        BotIcon(icon, size = 16.dp, tint = Color(0xFF17294D))
    }
}

@Composable
private fun ChoiceAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0x80FFFFFF))
            .border(1.dp, Color(0x90FFFFFF), RoundedCornerShape(99.dp))
            .botPress(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        BotIcon(icon, size = 16.dp, tint = Color(0xFF17294D))
        Text(label, style = body(12f, FontWeight.Medium, lineHeight = 1.3f), color = Color(0xFF17294D))
    }
}

@Composable
private fun CategoryChoice(category: Category, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .revealWhenSelected(selected)
            .widthIn(min = 88.dp, max = 136.dp)
            .height(112.dp)
            .clip(shape)
            .background(if (selected) Color.Black else Color(0x33FFFFFF))
            .border(1.dp, if (selected) Color.Black else Color(0x60FFFFFF), shape)
            .botPress(onClick = onClick)
            .semantics {
                contentDescription = category.name
                this.selected = selected
            }
    ) {
        // Hugs its label (88dp minimum, 136dp maximum) instead of stretching,
        // so short names like "Food" stay compact tiles.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ArtImage(category.art.drawable, size = 52.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                category.name,
                style = body(14f, FontWeight.Medium, lineHeight = 1.3f),
                color = if (selected) Color.White else Color(0xFF0B1740),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(150)) + scaleIn(tween(150, easing = BotEase), initialScale = 0.25f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150, easing = BotEase), targetScale = 0.25f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                BotIcon(BotIcons.Check, size = 12.dp, tint = Bot.Ink)
            }
        }
    }
}

@Composable
private fun AccountChoice(account: Account, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier = Modifier
            .revealWhenSelected(selected)
            .widthIn(max = 280.dp)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) Color.Black else Color(0x1AFFFFFF))
            .border(1.dp, if (selected) Color.Black else Color(0x60FFFFFF), shape)
            .botPress(onClick = onClick)
            .semantics {
                contentDescription = account.name
                this.selected = selected
            }
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ArtImage(accountIconFor(account.type), size = 40.dp)
        Text(
            account.name,
            style = body(14f, FontWeight.Medium, lineHeight = 1.4f),
            color = if (selected) Color.White else Color(0xFF111111)
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(150)) + scaleIn(tween(150, easing = BotEase), initialScale = 0.25f),
            exit = fadeOut(tween(100))
        ) {
            BotIcon(BotIcons.Check, size = 16.dp, tint = Color.White, modifier = Modifier.width(16.dp))
        }
    }
}

/**
 * Scrolls a choice into view once it becomes selected — a category just
 * created from this form lands at the end of its row, and an edited entry's
 * account may sit past the edge. Waits a frame so the row has been laid out.
 */
@Composable
private fun Modifier.revealWhenSelected(selected: Boolean): Modifier {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if (selected) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    return bringIntoViewRequester(requester)
}
