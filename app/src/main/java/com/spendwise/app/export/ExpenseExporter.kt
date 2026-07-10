package com.spendwise.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV export for a year of transactions. Output is a UTF-8 file in the cache
 * directory under `exports/spendwise-<year>.csv`, handed to a share intent
 * via FileProvider so receiving apps (Drive, Gmail, Files…) can read it.
 *
 * We export *all* expenses regardless of the in-app filters — the CSV is a
 * full dump for the year, not a snapshot of the current view.
 */
object ExpenseExporter {

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val HEADER = "Date,Time,Type,Merchant,Category,Account,Amount (MYR),Notes"

    /**
     * Build the CSV file for [year] using the given data, share it. Returns
     * true when at least one row was written and the share intent launched.
     */
    fun exportYear(
        context: Context,
        year: Int,
        zone: ZoneId,
        expenses: List<Expense>,
        categories: List<Category>,
        accounts: List<Account>
    ): ExportResult {
        val incomeCategoryIds = categories.filter { it.isIncomeAdjustment }.map { it.id }.toSet()
        val categoriesById = categories.associateBy { it.id }
        val accountsById = accounts.associateBy { it.id }

        val rows = expenses
            .asSequence()
            .map { exp -> exp to Instant.ofEpochMilli(exp.occurredAtMillis).atZone(zone).toLocalDateTime() }
            .filter { (_, dt) -> dt.year == year }
            .sortedBy { (_, dt) -> dt }
            .toList()

        if (rows.isEmpty()) return ExportResult.Empty

        val sb = StringBuilder(64 + rows.size * 80)
        sb.append(HEADER).append('\n')
        rows.forEach { (exp, dt) ->
            val isIncome = exp.categoryId in incomeCategoryIds
            val type = if (isIncome) "Income" else "Expense"
            val amount = String.format("%d.%02d", exp.amountCents / 100L, kotlin.math.abs(exp.amountCents % 100L))
            val category = categoriesById[exp.categoryId]?.name ?: exp.categoryName
            val account = accountsById[exp.accountId]?.name ?: "Account"
            sb.append(dt.toLocalDate().format(DATE_FORMAT)).append(',')
                .append(dt.toLocalTime().format(TIME_FORMAT)).append(',')
                .append(type).append(',')
                .append(csvEscape(exp.merchant)).append(',')
                .append(csvEscape(category)).append(',')
                .append(csvEscape(account)).append(',')
                .append(amount).append(',')
                .append(csvEscape(exp.notes))
                .append('\n')
        }

        return runCatching {
            val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(dir, "spendwise-$year.csv")
            file.writeText(sb.toString(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SpendWise — $year transactions")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share $year transactions")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            ExportResult.Shared(rows.size)
        }.getOrElse { e -> ExportResult.Failed(e.message ?: "Unknown error") }
    }

    /** RFC-4180-style escape: wrap in quotes when needed, double internal quotes. */
    private fun csvEscape(raw: String): String {
        if (raw.isEmpty()) return ""
        val needsQuoting = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return raw
        return buildString(raw.length + 2) {
            append('"')
            raw.forEach { c ->
                if (c == '"') append("\"\"") else append(c)
            }
            append('"')
        }
    }

    sealed interface ExportResult {
        data class Shared(val rowCount: Int) : ExportResult
        data object Empty : ExportResult
        data class Failed(val message: String) : ExportResult
    }
}
