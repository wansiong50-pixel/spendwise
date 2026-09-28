package com.spendwise.app.ui.botanical

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MerchantNames
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.Transfer
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
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
 *
 * The card is an iOS-style modal: [presentation] (owned by the shell, 1 when
 * up) raises and lowers it. It rests a little below the status bar and
 * expands flush when its handle is tapped or dragged, or when the form is
 * scrolled up; dragging it down past its resting place — from the handle,
 * or from anywhere once the form is at its top — lowers it toward closing,
 * and a far enough or fast enough release closes it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    target: EntryTarget,
    categories: List<Category>,
    accounts: List<Account>,
    recentExpenses: List<Expense>,
    formError: String?,
    presentation: Animatable<Float, AnimationVector1D>,
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
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        onClearError()
        // Otherwise a field on the page behind (Activity's search) keeps the keyboard, and whatever is typed, under the card.
        focusManager.clearFocus()
    }
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
    // Predictive back lowers the card with the gesture, then closes it (or asks first).
    PredictiveBackHandler(enabled = !calendarOpen && !discardOpen && !categoryFormOpen) { progress ->
        try {
            progress.collect { event -> presentation.snapTo(1f - 0.08f * event.progress) }
            if (dirty) {
                discardOpen = true
                presentation.animateTo(1f, BotMotion.SheetOpen)
            } else {
                onClose()
            }
        } catch (cancelled: CancellationException) {
            scope.launch { presentation.animateTo(1f, BotMotion.SheetOpen) }
            throw cancelled
        }
    }

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
    val restingGap = with(density) { RestingGap.toPx() }
    // How far the card sits below expanded: 0 flush under the status bar, restingGap at rest.
    val gap = remember { Animatable(if (expanded) 0f else restingGap) }
    val latestDirty by rememberUpdatedState(dirty)
    val latestClose by rememberUpdatedState(onClose)
    val drag = remember(density) { EntryCardDrag(gap, presentation, scope, density, restingGap) }
    drag.onDetent = { expanded = it }
    drag.requestClose = {
        if (latestDirty) {
            discardOpen = true
            false
        } else {
            latestClose()
            true
        }
    }
    val scroll = rememberScrollState()
    var saveHeight by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { drag.height = it.height.toFloat() }
            .graphicsLayer { translationY = (1f - presentation.value) * size.height }
            .imePadding()
            .semantics { paneTitle = if (isEdit) "Edit entry" else "New entry" }
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(statusTop))
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val g = gap.value
                        translationY = g
                        // Rounded at rest, square once flush with the status bar.
                        val radius = RestingRadius.toPx() * (g / restingGap).coerceIn(0f, 1f)
                        shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                        clip = true
                    }
                    // Pulled past its end, the form rubber-bands up; the card fills in
                    // below it with the colour the form ends on rather than opening onto
                    // the app behind.
                    .drawBehind { drawRect(formEndColor(scroll, FormGradientEnd.toPx())) }
                    .nestedScroll(drag.connection)
            ) {
                val gradientEnd = with(density) { FormGradientEnd.toPx() }
                FormColumn(
                    scroll = scroll,
                    // Save's strip, plus the part of a resting card that hangs below the bottom edge.
                    covered = { saveHeight + gap.value },
                    modifier = Modifier
                        .heightIn(min = maxHeight)
                        .background(Brush.verticalGradient(listOf(FormTop, Color.White), startY = 0f, endY = gradientEnd))
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp + navBottom + RestingGap)
                ) {
                    PullHandle(
                        expanded = expanded,
                        onToggle = { drag.settleOn(expand = !expanded) },
                        onDragStart = drag::start,
                        onDrag = drag::by,
                        onDragEnd = drag::release
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
                            AnimatedContent(
                                targetState = if (date == todayKl()) "Today" else shortDateLabel(date),
                                transitionSpec = {
                                    fadeIn(BotMotion.smooth(0.26f))
                                        .togetherWith(fadeOut(BotMotion.smooth(0.18f)))
                                        .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                                },
                                label = "entryDate"
                            ) { label ->
                                Text(label, style = body(18f, FontWeight.SemiBold), color = Color.White)
                            }
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
                    AnimatedError(amountError, Modifier.padding(bottom = 16.dp))
                    // Switching kind unfolds and folds the parts that differ.
                    Reveal(visible = kind == EntryKind.Transfer) {
                        Text(
                            "Your total balance stays the same.",
                            style = body(14f, lineHeight = 1.5f),
                            color = Color(0xD9FFFFFF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp)
                        )
                    }
                    val entryKind = rememberRetained(kind.takeIf { it != EntryKind.Transfer }) ?: EntryKind.Expense
                    Reveal(visible = kind != EntryKind.Transfer) {
                        // Expense and income offer different categories; one set crossfades into the other.
                        AnimatedContent(
                            targetState = entryKind,
                            transitionSpec = {
                                fadeIn(BotMotion.smooth(0.3f))
                                    .togetherWith(fadeOut(BotMotion.smooth(0.2f)))
                                    .using(SizeTransform(clip = false) { _, _ -> BotMotion.Resize })
                            },
                            label = "categorySet"
                        ) { shownKind ->
                            ChoiceRow(
                                label = "Category",
                                action = {
                                    ChoiceAction("Add category", BotIcons.Plus) { categoryFormOpen = true }
                                }
                            ) {
                                categories
                                    .filter { it.isIncomeAdjustment == (shownKind == EntryKind.Income) }
                                    .forEach { category ->
                                        CategoryChoice(category, selected = category.id == categoryId) {
                                            categoryId = category.id
                                            localError = null
                                        }
                                    }
                            }
                        }
                    }
                    ChoiceRow(label = if (kind == EntryKind.Transfer) "From account" else "Account") {
                        accounts.forEach { account ->
                            AccountChoice(account, selected = account.id == accountId) { accountId = account.id }
                        }
                    }
                    Reveal(visible = kind == EntryKind.Transfer) {
                        ChoiceRow(label = "To account") {
                            accounts.forEach { account ->
                                AccountChoice(account, selected = account.id == toAccountId) { toAccountId = account.id }
                            }
                        }
                    }
                    Reveal(visible = kind != EntryKind.Transfer) {
                        Column {
                            val nameLabel = if (entryKind == EntryKind.Income) "Income source" else "Merchant"
                            FieldLabel(nameLabel, FieldTone.Entry)
                            BotTextField(
                                value = merchant,
                                onValueChange = {
                                    merchant = it
                                    onClearError()
                                },
                                placeholder = if (entryKind == EntryKind.Income) "e.g. Monthly salary" else "e.g. Coffee shop",
                                tone = FieldTone.Entry,
                                maxLength = 100,
                                contentLabel = nameLabel,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            val shownSuggestions = rememberRetained(suggestions.takeIf { it.isNotEmpty() }).orEmpty()
                            Reveal(visible = suggestions.isNotEmpty()) {
                                Column(Modifier.padding(bottom = 20.dp)) {
                                    Text(
                                        if (merchant.isBlank()) "Recent entries" else "Matching previous entries",
                                        style = body(13f, FontWeight.SemiBold, lineHeight = 1.4f),
                                        color = Color(0xFF0B1740)
                                    )
                                    FlowRow(
                                        modifier = Modifier.padding(top = 8.dp).animateContentSize(BotMotion.Resize),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        shownSuggestions.forEach { name ->
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
                    AnimatedError(localError ?: formError)
                }
            }
        }
        // Save floats at the foot of the screen (above the keyboard) while the card moves between its detents.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { saveHeight = it.height }
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

/** How far the resting card sits below the status bar, and its corner radius there. */
private val RestingGap = 28.dp
private val RestingRadius = 36.dp

/** The form's gradient runs from navy at its top to white [FormGradientEnd] down. */
private val FormTop = Color(0xFF031EA5)
private val FormGradientEnd = 960.dp

/** The form's colour at its bottom edge: its gradient where the content ends, blended in sRGB like the gradient. */
private fun formEndColor(scroll: ScrollState, gradientEnd: Float): Color {
    val t = ((scroll.maxValue + scroll.viewportSize) / gradientEnd).coerceIn(0f, 1f)
    return Color(
        red = FormTop.red + (1f - FormTop.red) * t,
        green = FormTop.green + (1f - FormTop.green) * t,
        blue = FormTop.blue + (1f - FormTop.blue) * t
    )
}

/**
 * The form's scrolling column. Save floats over its foot, so while a field
 * has focus the form scrolls it — as it's tapped, and as the keyboard rises
 * under it — clear of Save rather than just inside the viewport: the bottom
 * [covered] px count as hidden. The rows and fields inside keep the usual
 * rule for their own scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormColumn(
    scroll: ScrollState,
    covered: () -> Float,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val inner = LocalBringIntoViewSpec.current
    var typing by remember { mutableStateOf(false) }
    val latestCovered by rememberUpdatedState(covered)
    val spec = remember { ClearOfFoot { if (typing) latestCovered() else 0f } }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        Column(
            Modifier
                .fillMaxSize()
                .onFocusChanged { typing = it.hasFocus }
                .verticalScroll(scroll)
                .then(modifier)
        ) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides inner) { content() }
        }
    }
}

/**
 * Compose's default scroll-into-view rule with the bottom [covered] px of
 * the viewport treated as hidden. Something taller than the room left
 * shows its top instead.
 */
private class ClearOfFoot(private val covered: () -> Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val room = max(containerSize - covered(), min(size, containerSize))
        val end = offset + size
        return when {
            offset >= 0f && end <= room -> 0f
            offset < 0f && end > room -> 0f
            abs(offset) < abs(end - room) -> offset
            else -> end - room
        }
    }
}

/**
 * Drags the entry card. Between expanded (0) and resting ([restingGap]) the
 * finger moves the card's detent; past resting it lowers the whole card
 * ([presentation] below 1) toward closing. Pulling up past expanded meets a
 * rubber band. Release settles on the nearer detent, leaning the way the
 * finger was moving, or — lowered far or fast enough — asks to close.
 * As a nested-scroll parent it also takes a pull from the form scrolled to
 * its top, and raises a resting card to expanded before the form scrolls
 * up, like an iOS sheet's detents.
 */
private class EntryCardDrag(
    private val gap: Animatable<Float, AnimationVector1D>,
    private val presentation: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val density: Density,
    private val restingGap: Float
) {
    var height = 0f
    var onDetent: (expanded: Boolean) -> Unit = {}

    /** Asks to close; false when the entry has unsaved changes and the question shows instead. */
    var requestClose: () -> Boolean = { true }

    /** The finger's position: 0 expanded, [restingGap] at rest, more when lowering. */
    private var travel = 0f
    private var dragging = false

    private fun positionNow(): Float {
        val lowered = (1f - presentation.value).coerceAtLeast(0f) * height
        if (lowered > 0f) return restingGap + lowered
        val g = gap.value
        return if (g >= 0f) g else -rubberBandInverse(-g, height)
    }

    fun start() {
        dragging = true
        travel = positionNow()
    }

    fun by(delta: Float) {
        if (height <= 0f) return
        if (!dragging) start()
        travel += delta
        val position = travel
        val shownGap = when {
            position < 0f -> -rubberBand(-position, height)
            position > restingGap -> restingGap
            else -> position
        }
        val lowered = (position - restingGap).coerceAtLeast(0f)
        scope.launch { gap.snapTo(shownGap) }
        scope.launch { presentation.snapTo(1f - lowered / height) }
    }

    fun release(velocity: Float) {
        if (!dragging) return
        dragging = false
        if (height <= 0f) return
        val lowered = (travel - restingGap).coerceAtLeast(0f)
        val scale = density.density
        if (lowered > 0f && (velocity > 900f * scale || lowered > height * 0.2f) && velocity > -400f * scale) {
            if (requestClose()) {
                scope.launch { presentation.animateTo(0f, BotMotion.SheetClose, initialVelocity = -velocity / height) }
                return
            }
            // Unsaved changes: the card rises back while the question shows.
            settle(restingGap, velocity)
            return
        }
        val projected = travel + velocity * 0.12f
        settle(if (projected < restingGap / 2f) 0f else restingGap, velocity)
    }

    /** Tap on the handle: expand or return to rest. */
    fun settleOn(expand: Boolean) {
        travel = positionNow()
        settle(if (expand) 0f else restingGap, 0f)
    }

    private fun settle(target: Float, velocity: Float) {
        onDetent(target == 0f)
        val wasLowered = travel > restingGap
        scope.launch {
            gap.animateTo(target, BotMotion.snappy(0.4f), initialVelocity = if (wasLowered) 0f else velocity)
        }
        scope.launch {
            presentation.animateTo(1f, BotMotion.SheetOpen, initialVelocity = if (wasLowered) -velocity / height else 0f)
        }
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y >= 0f) return Offset.Zero
            // Pushing up: a lowered or resting card rises to expanded before the form scrolls.
            val position = if (dragging) travel else positionNow()
            if (position <= 0f) return Offset.Zero
            val used = max(available.y, -position)
            by(used)
            return Offset(0f, used)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            by(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!dragging) return Velocity.Zero
            if (travel == 0f || travel == restingGap) {
                // Landed exactly on a detent: the form keeps its fling.
                dragging = false
                onDetent(travel == 0f)
                return Velocity.Zero
            }
            release(available.y)
            return available
        }
    }
}

/** The grab bar that expands the card to full height (tap or drag). */
@Composable
private fun PullHandle(
    expanded: Boolean,
    onToggle: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-16).dp)
            .height(44.dp)
            .draggable(
                state = rememberDraggableState(onDrag),
                orientation = Orientation.Vertical,
                onDragStarted = { onDragStart() },
                onDragStopped = { velocity -> onDragEnd(velocity) }
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

/** A choice's check: pops in with a little bounce, shrinks away. */
private val CheckIn = fadeIn(BotMotion.smooth(0.2f)) + scaleIn(BotMotion.bouncy(0.34f), initialScale = 0.4f)
private val CheckOut = fadeOut(BotMotion.smooth(0.16f)) + scaleOut(BotMotion.smooth(0.22f), targetScale = 0.6f)

@Composable
private fun CategoryChoice(category: Category, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val fill = animatedColor(if (selected) Color.Black else Color(0x33FFFFFF), "choiceFill")
    val edge = animatedColor(if (selected) Color.Black else Color(0x60FFFFFF), "choiceEdge")
    val ink = animatedColor(if (selected) Color.White else Color(0xFF0B1740), "choiceInk")
    Box(
        modifier = Modifier
            .revealWhenSelected(selected)
            .widthIn(min = 88.dp, max = 136.dp)
            .height(112.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
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
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = CheckIn,
            exit = CheckOut,
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
    val fill = animatedColor(if (selected) Color.Black else Color(0x1AFFFFFF), "accountFill")
    val edge = animatedColor(if (selected) Color.Black else Color(0x60FFFFFF), "accountEdge")
    val ink = animatedColor(if (selected) Color.White else Color(0xFF111111), "accountInk")
    Row(
        modifier = Modifier
            .revealWhenSelected(selected)
            .widthIn(max = 280.dp)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
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
            color = ink
        )
        // The chip widens smoothly to make room for its check.
        AnimatedVisibility(
            visible = selected,
            enter = CheckIn + expandHorizontally(BotMotion.snappy(0.32f, IntSize.VisibilityThreshold), expandFrom = Alignment.Start),
            exit = CheckOut + shrinkHorizontally(BotMotion.smooth(0.26f, IntSize.VisibilityThreshold), shrinkTowards = Alignment.Start)
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
