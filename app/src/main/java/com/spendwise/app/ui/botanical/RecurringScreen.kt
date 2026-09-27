package com.spendwise.app.ui.botanical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurrenceSchedule
import com.spendwise.app.domain.RecurringRule
import java.time.LocalDate
import kotlin.math.roundToLong

private enum class RuleFilter(val label: String) { Active("Active"), Paused("Paused") }

/** What active recurring expenses add up to in an average month. */
fun estimatedMonthlyExpenses(rules: List<RecurringRule>): Long = rules
    .filter { !it.isPaused && !it.isIncome }
    .sumOf {
        when (it.cadence) {
            RecurrenceCadence.Weekly -> it.amountCents * 52.0 / 12.0
            RecurrenceCadence.Monthly -> it.amountCents.toDouble()
            RecurrenceCadence.Yearly -> it.amountCents / 12.0
        }
    }
    .roundToLong()

@Composable
fun RecurringScreen(
    rules: List<RecurringRule>,
    categories: List<Category>,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (RecurringRule) -> Unit,
    onToggle: (RecurringRule) -> Unit,
    onCheck: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(RuleFilter.Active) }
    val shown = rules
        .filter { if (filter == RuleFilter.Active) !it.isPaused else it.isPaused }
        .sortedBy { it.nextDueEpochDay }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item {
                TopBar("Recurring", "Back to settings", onBack) {
                    CircleIconButton(BotIcons.Plus, "Add recurring entry", onClick = onAdd, size = 46.dp)
                }
            }
            item {
                Kicker("Estimated monthly expenses", Modifier.padding(bottom = 8.dp))
                Money(
                    cents = estimatedMonthlyExpenses(rules),
                    style = moneyStyle(58f, letterSpacingPx = -2f),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                Subtitle("From active recurring expense rules.", Modifier.padding(top = 10.dp))
                PillSegment(
                    options = RuleFilter.entries,
                    selected = filter,
                    onSelect = { filter = it },
                    label = { it.label },
                    modifier = Modifier.padding(top = 20.dp, bottom = 3.dp)
                )
            }
            items(shown, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    category = categoriesById[rule.categoryId],
                    onToggle = { onToggle(rule) },
                    onEdit = { onEdit(rule) }
                )
            }
            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        title = if (filter == RuleFilter.Active) "No active recurring entries" else "No paused recurring entries",
                        body = if (filter == RuleFilter.Active) "Add a recurring bill or income entry." else "Pause an active entry to see it here.",
                        action = if (filter == RuleFilter.Active) "Add recurring entry" else null,
                        onAction = if (filter == RuleFilter.Active) onAdd else null
                    )
                }
            }
            item {
                BotButton(
                    "Check due entries",
                    onClick = onCheck,
                    type = ButtonType.Secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                )
                Note(
                    "Due entries are added automatically when you open SpendWise. Pausing skips those dates; resuming starts from the next due date.",
                    Modifier.padding(top = 25.dp)
                )
            }
        }
    }
}

@Composable
private fun RuleCard(rule: RecurringRule, category: Category?, onToggle: () -> Unit, onEdit: () -> Unit) {
    val art = category?.art ?: categoryArtFor(rule.categoryName, rule.categoryIconName, rule.isIncome)
    Column(
        Modifier
            .padding(top = 13.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(19.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtImage(art.drawable, size = 45.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    rule.merchant.ifBlank { rule.categoryName },
                    style = display(24f, FontWeight.Medium, lineHeight = 1.2f),
                    color = Bot.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${rule.cadence.displayLabel} · ${if (rule.isIncome) "Income" else "Expense"}",
                    style = body(11f),
                    color = Color(0xFF7C8293)
                )
            }
            BotToggle(
                checked = !rule.isPaused,
                onToggle = onToggle,
                label = "Enable ${rule.merchant.ifBlank { rule.categoryName }}",
                offColor = Color(0xFFD9DCE3)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Money(
                cents = rule.amountCents,
                style = moneyStyle(38f, letterSpacingPx = -1f),
                color = if (rule.isIncome) Bot.Positive else Bot.Ink,
                modifier = Modifier.weight(1f)
            )
            TextLinkButton("Edit", onClick = onEdit, color = Color(0xFF667184))
        }
        Box(Modifier.fillMaxWidth().padding(top = 20.dp).height(1.dp).background(Bot.Rule))
        Row(Modifier.fillMaxWidth().padding(top = 15.dp)) {
            Text(
                "Next · ${dateLabel(LocalDate.ofEpochDay(rule.nextDueEpochDay))}",
                style = body(11f),
                color = Bot.Ink,
                modifier = Modifier.weight(1f)
            )
            Text(if (rule.isPaused) "Paused" else "Automatic", style = body(11f), color = Bot.Ink)
        }
    }
}

private enum class RuleKind(val label: String) { Expense("Expense"), Income("Income") }

/**
 * New / edit recurring rule. A new rule starts on its first date (a past date
 * backfills once). Editing shows the next date; picking another date moves
 * the schedule, and it continues from the next occurrence after today.
 */
@Composable
fun RuleFormSheet(
    visible: Boolean,
    existing: RecurringRule?,
    categories: List<Category>,
    accounts: List<Account>,
    onSave: (id: Long?, amount: String, categoryId: Long?, accountId: Long?, name: String, notes: String, cadence: RecurrenceCadence, firstDate: String) -> String?,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(if (visible) RuleKey(existing) else null)
    var picking by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var calendarStart by remember { mutableStateOf(todayKl()) }
    Box(Modifier.fillMaxSize()) {
        BotSheet(visible = visible, onDismiss = onDismiss, title = if (shown?.rule != null) "Edit recurring entry" else "New recurring entry") {
            if (shown != null) {
                RuleFormBody(
                    existing = shown.rule,
                    categories = categories,
                    accounts = accounts,
                    pickedDate = pickedDate,
                    onPickDate = { current ->
                        calendarStart = current
                        picking = true
                    },
                    onConsumePicked = { pickedDate = null },
                    onSave = onSave,
                    onDelete = onDelete
                )
            }
        }
        CalendarPicker(
            visible = visible && picking,
            mode = CalendarMode.Date,
            initial = calendarStart,
            maxDate = null,
            onCancel = { picking = false },
            onConfirm = {
                pickedDate = it
                picking = false
            }
        )
    }
}

private data class RuleKey(val rule: RecurringRule?)

@Composable
private fun RuleFormBody(
    existing: RecurringRule?,
    categories: List<Category>,
    accounts: List<Account>,
    pickedDate: LocalDate?,
    onPickDate: (current: LocalDate) -> Unit,
    onConsumePicked: () -> Unit,
    onSave: (id: Long?, amount: String, categoryId: Long?, accountId: Long?, name: String, notes: String, cadence: RecurrenceCadence, firstDate: String) -> String?,
    onDelete: (() -> Unit)?
) {
    val today = todayKl()
    var kind by remember(existing) { mutableStateOf(if (existing?.isIncome == true) RuleKind.Income else RuleKind.Expense) }
    var name by remember(existing) { mutableStateOf(existing?.merchant.orEmpty()) }
    var cents by remember(existing) { mutableStateOf(existing?.amountCents ?: 0L) }
    var cadence by remember(existing) { mutableStateOf(existing?.cadence ?: RecurrenceCadence.Monthly) }
    val shownStart = existing?.let { LocalDate.ofEpochDay(it.nextDueEpochDay) } ?: today
    var date by remember(existing) { mutableStateOf(shownStart) }
    var dateChanged by remember(existing) { mutableStateOf(false) }
    var accountId by remember(existing) { mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember(existing) {
        mutableStateOf(existing?.categoryId ?: categories.firstOrNull { !it.isIncomeAdjustment }?.id)
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember(existing) { mutableStateOf<String?>(null) }
    var errorField by remember(existing) { mutableStateOf<String?>(null) }

    LaunchedEffect(pickedDate) {
        val picked = pickedDate ?: return@LaunchedEffect
        date = picked
        dateChanged = picked != shownStart
        onConsumePicked()
    }
    val kindCategories = categories.filter { it.isIncomeAdjustment == (kind == RuleKind.Income) }
    // Unchanged date + cadence keeps the original anchor, so a Jan-31 rule
    // doesn't collapse onto Feb 28 just because the form was saved.
    val anchor = if (existing != null && !dateChanged) LocalDate.ofEpochDay(existing.anchorEpochDay) else date
    val following = RecurrenceSchedule.nextOccurrence(date, cadence, anchor)

    PillSegment(
        options = RuleKind.entries,
        selected = kind,
        onSelect = {
            kind = it
            categoryId = categories.firstOrNull { c -> c.isIncomeAdjustment == (it == RuleKind.Income) }?.id
        },
        label = { it.label },
        tone = SegmentTone.OnLight,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    FieldLabel("Name")
    BotTextField(
        value = name,
        onValueChange = {
            name = it
            error = null
        },
        placeholder = "e.g. Apartment rent",
        maxLength = 80,
        isError = errorField == "name",
        contentLabel = "Name"
    )
    AmountInput(
        cents = cents,
        onChange = {
            cents = it
            error = null
        },
        error = if (errorField == "amount") error else null
    )
    if (errorField == "amount" && error != null) ErrorBox(error!!, Modifier.padding(bottom = 12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            FieldLabel("Repeats")
            SelectField(
                options = RecurrenceCadence.entries,
                selected = cadence,
                onSelect = { cadence = it },
                label = { it.displayLabel },
                contentLabel = "Repeats"
            )
        }
        DateField(if (existing != null) "Next date" else "First date", date, Modifier.weight(1f)) { onPickDate(date) }
    }
    FormCaption(
        "After this date: ${dateLabel(following)}." +
            if (cadence != RecurrenceCadence.Weekly) " Short months use the last available day, then return to your chosen day." else "",
        Modifier.padding(top = 12.dp)
    )
    if (existing == null && date <= today) {
        InlineNote("Saving adds entries due through today. Missed dates are included once.", Modifier.padding(top = 12.dp))
    } else if (existing != null && dateChanged && date <= today) {
        InlineNote("Past dates aren’t added when you edit a rule. It continues from the next date after today.", Modifier.padding(top = 12.dp))
    }
    FieldLabel("Account", modifier = Modifier.padding(top = 16.dp))
    SelectField(
        options = accounts,
        selected = accounts.firstOrNull { it.id == accountId },
        onSelect = { accountId = it.id },
        label = { it.name },
        placeholder = "Choose account",
        isError = errorField == "account",
        contentLabel = "Account"
    )
    FieldLabel("Category", modifier = Modifier.padding(top = 16.dp))
    SelectField(
        options = kindCategories,
        selected = kindCategories.firstOrNull { it.id == categoryId },
        onSelect = { categoryId = it.id },
        label = { it.name },
        placeholder = "Choose a category",
        isError = errorField == "category",
        contentLabel = "Category"
    )
    FieldLabel("Notes · optional", modifier = Modifier.padding(top = 16.dp))
    BotTextField(
        value = notes,
        onValueChange = { notes = it },
        singleLine = false,
        minHeight = 80.dp,
        maxLength = 1000,
        contentLabel = "Notes"
    )
    if (error != null && errorField != "amount") ErrorBox(error!!, Modifier.padding(top = 16.dp))
    BotButton(
        "Save recurring entry",
        onClick = {
            fun fail(message: String, field: String) {
                error = message
                errorField = field
            }
            when {
                name.isBlank() -> fail("Name this recurring entry.", "name")
                cents <= 0L -> fail("Enter an amount greater than RM 0.00.", "amount")
                accounts.none { it.id == accountId } -> fail("Choose an active account.", "account")
                kindCategories.none { it.id == categoryId } -> fail("Choose a category.", "category")
                else -> {
                    val result = onSave(
                        existing?.id,
                        MoneyFormatter.centsToInput(cents),
                        categoryId,
                        accountId,
                        name,
                        notes,
                        cadence,
                        anchor.toString()
                    )
                    if (result != null) fail(result, "form")
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
    )
    if (existing != null && onDelete != null) {
        TextLinkButton("Delete recurring entry", onClick = onDelete, modifier = Modifier.fillMaxWidth())
    }
}
