package com.spendwise.app.ui.botanical

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.MoneyFormatter
import java.time.YearMonth

private enum class CategoryKind(val label: String) { Expense("Expense"), Income("Income") }

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    budgets: List<Budget>,
    month: YearMonth,
    spentByCategory: Map<Long, Long>,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onAdd: (income: Boolean) -> Unit,
    onEdit: (Category) -> Unit
) {
    var kind by rememberSaveable { mutableStateOf(CategoryKind.Expense) }
    val limits = remember(budgets) { budgets.associate { it.categoryId to it.monthlyLimitCents } }
    val shown = categories.filter { it.isIncomeAdjustment == (kind == CategoryKind.Income) }
    DarkPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTopPadding(), bottom = bottomPadding)
        ) {
            item {
                TopBar("Categories", "Back to settings", onBack) {
                    CircleIconButton(BotIcons.Plus, "Add category", onClick = { onAdd(kind == CategoryKind.Income) }, size = 46.dp)
                }
            }
            item {
                Kicker("Expense and income")
                Subtitle("Edit category names, icons, and budgets.", Modifier.padding(top = 10.dp))
                PillSegment(
                    options = CategoryKind.entries,
                    selected = kind,
                    onSelect = { kind = it },
                    label = { it.label },
                    modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
                )
            }
            shown.chunked(2).forEach { pair ->
                item(key = pair.joinToString { it.id.toString() }) {
                    EqualHeightRow(
                        cells = pair.map { category ->
                            @Composable {
                                CategoryCard(
                                    category = category,
                                    spent = spentByCategory[category.id] ?: 0L,
                                    limit = limits[category.id] ?: 0L,
                                    onClick = { onEdit(category) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        },
                        columns = 2,
                        gap = 12.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            item {
                Note(
                    "${monthLabel(month)} · Select a category to edit its name, icon, or budget.",
                    Modifier.padding(top = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, spent: Long, limit: Long, onClick: () -> Unit, modifier: Modifier) {
    val art = category.art
    val over = limit in 1 until spent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(Color.White)
            .botPress(scale = 0.98f, onClick = onClick)
            .semantics { contentDescription = "${category.name}, ${formatRm(spent)} this month" }
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArtImage(art.drawable, size = 62.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            category.name,
            style = display(24f, FontWeight.Medium, lineHeight = 1.2f),
            color = Bot.Ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Money(
            cents = spent,
            style = moneyStyle(21f, letterSpacingPx = -0.6f),
            color = Color(0xFF7A8293),
            centsScale = 0.8f,
            abbreviate = true,
            align = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp)
        )
        Spacer(Modifier.weight(1f))
        if (limit > 0) {
            Track(spent.toFloat() / limit, art.color)
            Text(
                (if (over) "Over budget" else "of RM ${formatAmount(limit)}") + " · this month",
                style = body(10f, lineHeight = 1.5f),
                color = if (over) Bot.Negative else Color(0xFF798293),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        } else {
            Text(
                if (category.isIncomeAdjustment) "Income category" else "Select to set a monthly budget",
                style = body(10f, lineHeight = 1.5f),
                color = Color(0xFF798293),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * New / edit category. The kind can change only while nothing is filed under
 * the category (moving entries between income and spending by accident would
 * rewrite every total). Built-ins can't be deleted.
 */
@Composable
fun CategoryFormSheet(
    visible: Boolean,
    existing: Category?,
    initialIncome: Boolean,
    budgetCents: Long,
    used: Boolean,
    onSave: (name: String, iconName: String, color: Long, isIncome: Boolean, budget: String) -> String?,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(if (visible) FormKey(existing, initialIncome) else null)
    BotSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = if (shown?.category != null) "Edit category" else "New category"
    ) {
        if (shown != null) {
            CategoryFormBody(
                existing = shown.category,
                initialIncome = shown.income,
                budgetCents = budgetCents,
                used = used,
                onSave = onSave,
                onDelete = onDelete
            )
        }
    }
}

private data class FormKey(val category: Category?, val income: Boolean)

@Composable
private fun CategoryFormBody(
    existing: Category?,
    initialIncome: Boolean,
    budgetCents: Long,
    used: Boolean,
    onSave: (name: String, iconName: String, color: Long, isIncome: Boolean, budget: String) -> String?,
    onDelete: (() -> Unit)?
) {
    val builtIn = existing != null && !existing.isCustom
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var income by remember(existing, initialIncome) { mutableStateOf(existing?.isIncomeAdjustment ?: initialIncome) }
    var art by remember(existing, initialIncome) {
        mutableStateOf(existing?.art ?: if (initialIncome) CategoryArt.Salary else CategoryArt.Food)
    }
    var budget by remember(existing, budgetCents) { mutableStateOf(budgetCents) }
    var error by remember(existing) { mutableStateOf<String?>(null) }

    if (existing == null || (!builtIn && !used)) {
        PillSegment(
            options = CategoryKind.entries,
            selected = if (income) CategoryKind.Income else CategoryKind.Expense,
            onSelect = { income = it == CategoryKind.Income },
            label = { it.label },
            tone = SegmentTone.OnLight,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
    FieldLabel("Category name")
    BotTextField(
        value = name,
        onValueChange = {
            name = it
            error = null
        },
        placeholder = "e.g. Coffee",
        maxLength = 24,
        isError = error != null,
        contentLabel = "Category name"
    )
    FormCaption("Icon", Modifier.padding(top = 20.dp, bottom = 8.dp))
    ArtGrid(selected = art, onSelect = { art = it })
    if (!income) {
        FormCaption("Monthly budget · optional", Modifier.padding(top = 20.dp))
        AmountInput(cents = budget, onChange = { budget = it }, label = "Monthly budget")
    }
    error?.let { ErrorBox(it, Modifier.padding(vertical = 16.dp)) }
    BotButton(
        "Save category",
        onClick = {
            error = onSave(
                name,
                art.key,
                art.storedColor,
                income,
                if (!income && budget > 0) MoneyFormatter.centsToInput(budget) else ""
            )
        },
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
    )
    if (builtIn) {
        Text(
            "Built-in category. You can rename it or change its icon and budget, but it can’t be deleted.",
            style = body(12f, lineHeight = 1.6f),
            color = Bot.SheetMuted,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    if (existing != null && existing.isCustom && onDelete != null) {
        TextLinkButton("Delete category", onClick = onDelete, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        if (used) {
            FormCaption("Choose whether to move or delete the linked entries and recurring rules before removing this category.")
        }
    }
}

/** The sixteen illustrations, six to a row (four on narrow phones). */
@Composable
private fun ArtGrid(selected: CategoryArt, onSelect: (CategoryArt) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 300.dp) 4 else 6
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryArt.entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { art ->
                        val isSelected = art == selected
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 64.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, Bot.ActionSolid, RoundedCornerShape(18.dp)).padding(3.dp)
                                        else Modifier.padding(3.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFFE6EFFA) else Color(0xFFF0F2F6))
                                    .botPress { onSelect(art) }
                                    .semantics {
                                        contentDescription = "${art.label} icon"
                                        this.selected = isSelected
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painterResource(art.drawable),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(0.92f)
                                )
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private enum class MigrationMode(val label: String) { Move("Move entries"), Delete("Delete entries") }

/**
 * Deleting a category that still has entries or recurring rules: move them
 * to another category of the same kind, or delete them too. Deleting needs
 * an explicit tick; nothing happens by default.
 */
@Composable
fun CategoryMigrationSheet(
    category: Category?,
    entryCount: Int,
    ruleCount: Int,
    destinations: List<Category>,
    hasBudget: Boolean,
    onCancel: () -> Unit,
    onMove: (Long) -> Unit,
    onDeleteAll: () -> Unit
) {
    val shown = rememberRetained(category)
    BotSheet(visible = category != null, onDismiss = onCancel, title = "Delete ${shown?.name.orEmpty()}?") {
        if (shown != null) {
            var mode by remember(shown) { mutableStateOf(MigrationMode.Move) }
            var destination by remember(shown) { mutableStateOf<Category?>(null) }
            var confirmed by remember(shown) { mutableStateOf(false) }
            val entries = "$entryCount ${if (entryCount == 1) "entry" else "entries"}"
            val rules = "$ruleCount recurring ${if (ruleCount == 1) "rule" else "rules"}"
            SheetText("${shown.name} has $entries and $rules across all dates.")
            PillSegment(
                options = MigrationMode.entries,
                selected = mode,
                onSelect = {
                    mode = it
                    confirmed = false
                },
                label = { it.label },
                tone = SegmentTone.OnLight
            )
            if (mode == MigrationMode.Move) {
                Support(
                    "Keep your history by moving it to another category. Amounts and account balances stay the same.",
                    Modifier.padding(top = 16.dp)
                )
                if (destinations.isNotEmpty()) {
                    FieldLabel("Move to category", modifier = Modifier.padding(top = 16.dp))
                    SelectField(
                        options = destinations,
                        selected = destination,
                        onSelect = { destination = it },
                        label = { it.name },
                        placeholder = "Choose a category",
                        contentLabel = "Move to category"
                    )
                } else {
                    Support(
                        "Create another ${if (shown.isIncomeAdjustment) "income" else "expense"} category first, then return here.",
                        Modifier.padding(top = 12.dp)
                    )
                }
                if (hasBudget) {
                    Support(
                        "This category’s budget will be removed. The destination budget stays unchanged.",
                        Modifier.padding(top = 12.dp)
                    )
                }
                ButtonRow {
                    BotButton("Cancel", onClick = onCancel, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                    BotButton(
                        "Move & delete category",
                        onClick = { destination?.let { onMove(it.id) } },
                        enabled = destination != null,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                Support(
                    "Delete this category, its budget, $entries and $rules. Account balances and reports will be recalculated. Linked rules will stop creating entries.",
                    Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Checkbox) { confirmed = !confirmed }
                        .semantics { stateDescription = if (confirmed) "Checked" else "Not checked" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (confirmed) Bot.DangerInk else Color.White)
                            .border(1.5.dp, if (confirmed) Bot.DangerInk else Color(0xFF8D94A4), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (confirmed) BotIcon(BotIcons.Check, size = 14.dp, tint = Color.White)
                    }
                    Text(
                        "Delete all entries and rules in ${shown.name}",
                        style = body(14f, FontWeight.Medium, lineHeight = 1.5f),
                        color = Bot.Ink
                    )
                }
                Support("This can’t be undone. Back up first if you might need these entries.")
                ButtonRow {
                    BotButton("Cancel", onClick = onCancel, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                    BotButton(
                        "Delete category & entries",
                        onClick = onDeleteAll,
                        type = ButtonType.Danger,
                        enabled = confirmed,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
