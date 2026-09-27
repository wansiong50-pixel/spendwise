package com.spendwise.app.ui.botanical

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.export.BackupPreview
import java.time.LocalDate

/** Label / value lines (`.b-detail-rows`). */
@Composable
fun DetailRows(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(label, style = body(14f, lineHeight = 1.5f), color = Color(0xFF7E8596))
                Text(
                    value,
                    style = body(14f, FontWeight.Medium, lineHeight = 1.5f),
                    color = Bot.Ink,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.Rule))
        }
    }
}

/** Plain-text summary for the share sheet. */
fun shareText(item: LedgerItem, categoriesById: Map<Long, Category>, names: Map<Long, String>): String {
    val text = rowText(item, categoriesById, names)
    val accounts = when (item) {
        is LedgerItem.Entry -> names[item.expense.accountId].orEmpty()
        is LedgerItem.Move -> "${item.transfer.fromAccountName} → ${item.transfer.toAccountName}"
    }
    val notes = when (item) {
        is LedgerItem.Entry -> item.expense.notes
        is LedgerItem.Move -> item.transfer.notes
    }
    return listOf(text.title, "${item.kind.label} · RM ${formatAmount(item.cents)}", dateLabel(item.date), accounts, notes)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n")
}

/**
 * One entry: its art and signed amount, the facts, sharing, and — for
 * expenses and income — the other entries with the same name.
 */
@Composable
fun EntryDetailSheet(
    item: LedgerItem?,
    categoriesById: Map<Long, Category>,
    names: Map<Long, String>,
    loadHistory: suspend (Expense, Boolean) -> List<Expense>,
    onEdit: (LedgerItem) -> Unit,
    onDelete: (LedgerItem) -> Unit,
    onOpenHistory: (Expense) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(item)
    val text = shown?.let { rowText(it, categoriesById, names) }
    BotSheet(visible = item != null, onDismiss = onDismiss, title = text?.title.orEmpty()) {
        if (shown == null || text == null) return@BotSheet
        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (text.art != null) {
                ArtImage(text.art, size = 72.dp, modifier = Modifier.padding(bottom = 14.dp))
            } else {
                BotIcon(BotIcons.Transfer, size = 50.dp, tint = Bot.Ink, modifier = Modifier.padding(bottom = 20.dp))
            }
            EntryMoney(
                cents = shown.cents,
                kind = shown.kind,
                style = moneyStyle(53f, letterSpacingPx = -2f),
                centsScale = 0.65f,
                align = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val rows = buildList {
            add("Type" to shown.kind.label)
            add("Date" to dateLabel(shown.date))
            when (shown) {
                is LedgerItem.Entry -> {
                    add("Account" to (names[shown.expense.accountId] ?: "Account"))
                    add("Category" to (categoriesById[shown.expense.categoryId]?.name ?: shown.expense.categoryName))
                    if (shown.expense.notes.isNotBlank()) add("Notes" to shown.expense.notes)
                }
                is LedgerItem.Move -> {
                    add("From" to shown.transfer.fromAccountName)
                    add("To" to shown.transfer.toAccountName)
                    if (shown.transfer.notes.isNotBlank()) add("Notes" to shown.transfer.notes)
                }
            }
        }
        DetailRows(rows)
        BotButton(
            "Share transaction",
            onClick = { onShare(shareText(shown, categoriesById, names)) },
            type = ButtonType.Secondary,
            icon = BotIcons.Share,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
        if (shown is LedgerItem.Entry) {
            var history by remember(shown.expense.id) { mutableStateOf<List<Expense>>(emptyList()) }
            LaunchedEffect(shown.expense.id, shown.expense.merchant) {
                history = loadHistory(shown.expense, shown.isIncome)
                    .filter { it.id != shown.expense.id }
                    .filter {
                        // Same name; an unnamed income entry groups with its category.
                        val name = shown.expense.merchant.trim()
                        name.isBlank() && shown.isIncome || it.merchant.trim().equals(name, ignoreCase = true)
                    }
                    .sortedByDescending { it.occurredAtMillis }
            }
            if (history.isNotEmpty()) {
                Text(
                    "Other entries · ${text.title}",
                    style = display(24f, FontWeight.Medium, lineHeight = 1.2f),
                    color = Bot.Ink,
                    modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
                )
                Support(
                    "${history.size} other ${if (history.size == 1) "entry" else "entries"} · ${formatRm(history.sumOf { it.amountCents })} total" +
                        if (history.size > 5) " · Showing latest 5" else ""
                )
                history.take(5).forEach { other ->
                    Column(Modifier.fillMaxWidth().botRowPress { onOpenHistory(other) }) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(dateLabel(other.occurredAtMillis.toKlDate()), style = body(14f, FontWeight.Medium), color = Bot.Ink)
                                Text(
                                    names[other.accountId] ?: "Account",
                                    style = body(13f),
                                    color = Bot.SurfaceMuted,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            Money(
                                cents = other.amountCents,
                                style = moneyStyle(25f, letterSpacingPx = -0.6f),
                                color = Bot.Ink,
                                centsScale = 0.8f
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Bot.RuleSoft))
                    }
                }
            }
        }
        ButtonRow {
            BotButton("Edit", onClick = { onEdit(shown) }, type = ButtonType.Secondary, icon = BotIcons.Edit, modifier = Modifier.weight(1f).fillMaxHeight())
            BotButton("Delete", onClick = { onDelete(shown) }, type = ButtonType.Danger, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Entries on one heatmap day, with a count and money in/out before the list. */
@Composable
fun DaySheet(
    date: LocalDate?,
    items: List<LedgerItem>,
    categoriesById: Map<Long, Category>,
    names: Map<Long, String>,
    onOpen: (LedgerItem) -> Unit,
    onAdd: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(date)
    BotSheet(
        visible = date != null,
        onDismiss = onDismiss,
        title = shown?.let(::dateLabel).orEmpty(),
        maxHeightFraction = 0.85f,
        horizontalPadding = 16.dp,
        footer = {
            BotButton("Add entry", onClick = { shown?.let(onAdd) }, modifier = Modifier.fillMaxWidth())
        }
    ) {
        if (items.isNotEmpty()) {
            val totals = totalsOf(items)
            // Count first, then money out and in, emphasised so a busy day
            // reads before the list does.
            val summary = buildAnnotatedString {
                append("${items.size} ${if (items.size == 1) "entry" else "entries"}")
                val strong = SpanStyle(fontWeight = FontWeight.SemiBold, color = Bot.Navy)
                if (totals.expense > 0) {
                    append(" · ")
                    withStyle(strong) { append("Spent ${formatRm(totals.expense)}") }
                }
                if (totals.income > 0) {
                    append(" · ")
                    withStyle(strong) { append("Earned ${formatRm(totals.income)}") }
                }
            }
            Text(
                summary,
                style = body(13f, lineHeight = 1.4f).copy(fontFeatureSettings = TabularNums),
                color = Bot.SurfaceMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )
            Column(Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    val text = rowText(item, categoriesById, names)
                    TransactionRow(
                        title = text.title,
                        meta = text.meta,
                        cents = item.cents,
                        kind = item.kind,
                        art = text.art,
                        onClick = { onOpen(item) },
                        showDivider = index < items.lastIndex
                    )
                }
            }
        } else {
            EmptyState(
                title = "No entries on this date",
                body = "Add an entry to record activity here.",
                onDark = false
            )
        }
    }
}

/** A category in one month or year: total, entries, and a way into its budget. */
@Composable
fun BreakdownSheet(
    category: Category?,
    periodLabel: String,
    items: List<LedgerItem>,
    categoriesById: Map<Long, Category>,
    names: Map<Long, String>,
    onOpen: (LedgerItem) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(category)
    BotSheet(visible = category != null, onDismiss = onDismiss, title = shown?.name.orEmpty()) {
        if (shown == null) return@BotSheet
        SheetText(periodLabel)
        Money(
            cents = items.sumOf { it.cents },
            style = moneyStyle(58f, letterSpacingPx = -2f),
            color = Bot.Ink,
            modifier = Modifier.fillMaxWidth()
        )
        Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
            if (items.isEmpty()) {
                Support("No entries in ${shown.name} for this period.")
            }
            items.forEachIndexed { index, item ->
                val text = rowText(item, categoriesById, names)
                TransactionRow(
                    title = text.title,
                    meta = text.meta,
                    cents = item.cents,
                    kind = item.kind,
                    art = text.art,
                    onClick = { onOpen(item) },
                    horizontalPadding = 0.dp,
                    showDivider = index < items.lastIndex
                )
            }
        }
        BotButton(
            "Edit category & budget",
            onClick = { onEditCategory(shown) },
            type = ButtonType.Secondary,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
    }
}

/** A picked backup file, read but not yet applied. */
data class RestoreReview(val uri: Uri, val fileName: String, val preview: BackupPreview.Ready)

/** Shows what a backup holds before it replaces everything. */
@Composable
fun RestoreReviewSheet(
    review: RestoreReview?,
    currentEntries: Int,
    onConfirm: (RestoreReview) -> Unit,
    onDismiss: () -> Unit
) {
    val shown = rememberRetained(review)
    BotSheet(visible = review != null, onDismiss = onDismiss, title = "Review backup") {
        if (shown == null) return@BotSheet
        val preview = shown.preview
        SheetText("${shown.fileName} · saved ${backupTimeLabel(preview.exportedAtMillis)}")
        DetailRows(
            listOf(
                "Entries" to preview.entries.toString(),
                "Transfers" to preview.transfers.toString(),
                "Accounts" to preview.accounts.toString(),
                "Categories" to preview.categories.toString(),
                "Recurring rules" to preview.recurringRules.toString()
            )
        )
        SheetText(
            "This replaces the $currentEntries ${if (currentEntries == 1) "entry" else "entries"} currently in SpendWise, " +
                "along with your accounts, categories, budgets and rules. Active recurring rules then catch up through today.",
            Modifier.padding(top = 16.dp)
        )
        ButtonRow {
            BotButton("Cancel", onClick = onDismiss, type = ButtonType.Secondary, modifier = Modifier.weight(1f).fillMaxHeight())
            BotButton("Restore backup", onClick = { onConfirm(shown) }, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

