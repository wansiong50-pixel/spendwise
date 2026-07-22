package com.spendwise.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.MoneyFormatter
import com.spendwise.app.domain.Transfer
import com.spendwise.app.ui.theme.AppOnSurfaceVariant
import com.spendwise.app.ui.theme.AppSurface
import com.spendwise.app.ui.theme.SwInk
import com.spendwise.app.ui.theme.SwNeg
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The exact ledger row awaiting destructive confirmation. Keeping the row,
 * rather than only its id, lets the dialog identify what will be removed even
 * if the underlying detail sheet recomposes while the confirmation is open.
 */
internal sealed interface V2DeleteTarget {
    data class ExpenseEntry(
        val expense: Expense,
        val isIncome: Boolean
    ) : V2DeleteTarget

    data class TransferEntry(val transfer: Transfer) : V2DeleteTarget
}

internal data class V2DeletePrompt(
    val title: String,
    val subject: String,
    val context: String,
    val impact: String
)

/** Pure copy/formatting logic, kept outside Compose so the safety-critical
 * identity shown before deletion can be covered by fast JVM tests. */
internal fun V2DeleteTarget.toDeletePrompt(): V2DeletePrompt = when (this) {
    is V2DeleteTarget.ExpenseEntry -> {
        val subject = expense.merchant.trim().ifBlank { expense.categoryName }
        val categoryPart = if (subject.equals(expense.categoryName, ignoreCase = true)) {
            ""
        } else {
            " \u00B7 ${expense.categoryName}"
        }
        V2DeletePrompt(
            title = if (isIncome) "Delete income?" else "Delete expense?",
            subject = subject,
            context = "${MoneyFormatter.formatCents(expense.amountCents)}$categoryPart \u00B7 " +
                formatDeleteDate(expense.occurredAtMillis),
            impact = "This entry will be permanently removed. Your account balance and reports will update immediately."
        )
    }

    is V2DeleteTarget.TransferEntry -> V2DeletePrompt(
        title = "Delete transfer?",
        subject = "${transfer.fromAccountName} \u2192 ${transfer.toAccountName}",
        context = "${MoneyFormatter.formatCents(transfer.amountCents)} \u00B7 " +
            formatDeleteDate(transfer.occurredAtMillis),
        impact = "This transfer will be permanently removed. Both account balances will update immediately."
    )
}

@Composable
internal fun V2DeleteEntryDialog(
    target: V2DeleteTarget?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val prompt = target?.toDeletePrompt() ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = SwNeg
            )
        },
        title = {
            Text(
                text = prompt.title,
                color = SwInk,
                style = v2T(18f, FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = prompt.subject,
                    color = SwInk,
                    style = v2T(15f, FontWeight.Bold)
                )
                Text(
                    text = prompt.context,
                    color = AppOnSurfaceVariant,
                    style = v2T(12.5f, FontWeight.SemiBold)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = prompt.impact,
                    color = AppOnSurfaceVariant,
                    style = v2T(13.5f, FontWeight.Medium)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = SwNeg)
            ) {
                Text("Delete", style = v2T(14f, FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = SwInk)
            ) {
                Text("Cancel", style = v2T(14f, FontWeight.Bold))
            }
        },
        containerColor = AppSurface,
        iconContentColor = SwNeg,
        titleContentColor = SwInk,
        textContentColor = AppOnSurfaceVariant,
        tonalElevation = 0.dp
    )
}

private val DELETE_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")
private val DELETE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatDeleteDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(DELETE_ZONE)
        .toLocalDate()
        .format(DELETE_DATE_FORMAT)
