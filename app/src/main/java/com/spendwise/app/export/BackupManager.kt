package com.spendwise.app.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.spendwise.app.data.AccountDao
import com.spendwise.app.data.AccountEntity
import com.spendwise.app.data.AppearancePreferenceStore
import com.spendwise.app.data.BudgetDao
import com.spendwise.app.data.BudgetEntity
import com.spendwise.app.data.CategoryDao
import com.spendwise.app.data.CategoryEntity
import com.spendwise.app.data.ExpenseDao
import com.spendwise.app.data.ExpenseDatabase
import com.spendwise.app.data.ExpenseEntity
import com.spendwise.app.data.RecurringRuleDao
import com.spendwise.app.data.RecurringRuleEntity
import com.spendwise.app.data.TransferDao
import com.spendwise.app.data.TransferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// Result of an export or import attempt, surfaced to the UI so the settings
// sheet can show a snackbar with a clear message. Success carries a count so
// the toast can be specific ("Backed up 42 entries") rather than a vague
// "Done".
sealed interface BackupResult {
    data class ExportSuccess(val expenseCount: Int) : BackupResult
    data class ImportSuccess(val expenseCount: Int) : BackupResult
    data class Failure(val reason: String) : BackupResult
}

/** What a backup file holds, read before the user agrees to replace their data. */
sealed interface BackupPreview {
    data class Ready(
        val entries: Int,
        val transfers: Int,
        val accounts: Int,
        val categories: Int,
        val recurringRules: Int,
        val exportedAtMillis: Long
    ) : BackupPreview

    data class Invalid(val reason: String) : BackupPreview
}

// Room schema version recorded in the backup so future migrations can branch
// on it if needed. Kept as a const here (rather than reading from the
// database) because it's part of the static contract for backups produced by
// this build.
private const val APP_DB_VERSION_AT_EXPORT: Int = 8

class BackupManager(
    private val context: Context,
    private val database: ExpenseDatabase,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val recurringRuleDao: RecurringRuleDao,
    private val transferDao: TransferDao,
    private val appearancePreferenceStore: AppearancePreferenceStore
) {
    // prettyPrint makes the file inspectable by a curious user opening it in
    // a text editor; ignoreUnknownKeys is forward-compat so an older app can
    // open a backup written by a newer one (as long as schemaVersion still
    // matches).
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val categories = categoryDao.allOnce()
            val accounts = accountDao.allOnce()
            val budgets = budgetDao.allOnce()
            val expenses = expenseDao.allOnce()
            val recurringRules = recurringRuleDao.allOnce()
            val transfers = transferDao.allOnce()
            val darkMode = appearancePreferenceStore.startupDarkModePreference.first()

            val envelope = BackupEnvelope(
                appDbVersion = APP_DB_VERSION_AT_EXPORT,
                exportedAtMillis = System.currentTimeMillis(),
                darkModeEnabled = darkMode,
                categories = categories.map { it.toBackup() },
                accounts = accounts.map { it.toBackup() },
                budgets = budgets.map { it.toBackup() },
                expenses = expenses.map { it.toBackup() },
                recurringRules = recurringRules.map { it.toBackup() },
                transfers = transfers.map { it.toBackup() }
            )

            val payload = json.encodeToString(BackupEnvelope.serializer(), envelope)
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext BackupResult.Failure("Couldn't open the chosen file.")
            stream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            BackupResult.ExportSuccess(expenseCount = expenses.size)
        } catch (e: Exception) {
            BackupResult.Failure("Backup failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Reads [uri] without touching the database so the restore review can
     * show what the file holds before anything is replaced.
     */
    suspend fun previewBackup(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        try {
            when (val read = readEnvelope(uri)) {
                is EnvelopeRead.Failed -> BackupPreview.Invalid(read.reason)
                is EnvelopeRead.Ok -> with(read.envelope) {
                    BackupPreview.Ready(
                        entries = expenses.size,
                        transfers = transfers.size,
                        accounts = accounts.size,
                        categories = categories.size,
                        recurringRules = recurringRules.size,
                        exportedAtMillis = exportedAtMillis
                    )
                }
            }
        } catch (e: Exception) {
            BackupPreview.Invalid("Couldn't read this backup: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private sealed interface EnvelopeRead {
        data class Ok(val envelope: BackupEnvelope) : EnvelopeRead
        data class Failed(val reason: String) : EnvelopeRead
    }

    private fun readEnvelope(uri: Uri): EnvelopeRead {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: return EnvelopeRead.Failed("Couldn't open the chosen file.")

        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), text)
        } catch (_: Exception) {
            return EnvelopeRead.Failed("This file isn't a valid SpendWise backup.")
        }

        if (envelope.schemaVersion > CURRENT_BACKUP_SCHEMA_VERSION) {
            return EnvelopeRead.Failed(
                "This backup was made with a newer version of the app — please update first."
            )
        }
        return EnvelopeRead.Ok(envelope)
    }

    suspend fun importBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val envelope = when (val read = readEnvelope(uri)) {
                is EnvelopeRead.Failed -> return@withContext BackupResult.Failure(read.reason)
                is EnvelopeRead.Ok -> read.envelope
            }

            // Replace-all in a single transaction. Wipe children before
            // parents (RESTRICT FKs on expenses block category/account
            // deletes if expenses still reference them), then re-insert
            // parent-first so each row's FK targets already exist.
            //
            // Any failure inside the block rolls back the whole thing — the
            // user's current data is preserved if the import explodes
            // midway.
            database.withTransaction {
                transferDao.deleteAll()
                recurringRuleDao.deleteAll()
                expenseDao.deleteAll()
                budgetDao.deleteAll()
                accountDao.deleteAll()
                categoryDao.deleteAll()

                categoryDao.insertAll(envelope.categories.map { it.toEntity() })
                accountDao.insertAll(envelope.accounts.map { it.toEntity() })
                budgetDao.insertAll(envelope.budgets.map { it.toEntity() })
                expenseDao.insertAll(envelope.expenses.map { it.toEntity() })
                recurringRuleDao.insertAll(envelope.recurringRules.map { it.toEntity() })
                transferDao.insertAll(envelope.transfers.map { it.toEntity() })
            }

            // DataStore lives outside the Room DB transaction, so we apply
            // the dark-mode pref after the data restore commits. If the user
            // had no preference saved in the backup (darkModeEnabled = null),
            // we leave the current setting alone instead of forcing a state.
            envelope.darkModeEnabled?.let { appearancePreferenceStore.setDarkMode(it) }

            BackupResult.ImportSuccess(expenseCount = envelope.expenses.size)
        } catch (e: Exception) {
            BackupResult.Failure("Restore failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

private fun CategoryEntity.toBackup() = BackupCategory(
    id = id,
    name = name,
    color = color,
    iconName = iconName,
    isIncomeAdjustment = isIncomeAdjustment,
    isCustom = isCustom
)

private fun BackupCategory.toEntity() = CategoryEntity(
    id = id,
    name = name,
    color = color,
    iconName = iconName,
    isIncomeAdjustment = isIncomeAdjustment,
    isCustom = isCustom
)

private fun AccountEntity.toBackup() = BackupAccount(
    id = id,
    name = name,
    type = type,
    startingBalanceCents = startingBalanceCents,
    color = color,
    iconName = iconName,
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun BackupAccount.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type,
    startingBalanceCents = startingBalanceCents,
    color = color,
    iconName = iconName,
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun BudgetEntity.toBackup() = BackupBudget(
    id = id,
    categoryId = categoryId,
    monthlyLimitCents = monthlyLimitCents
)

private fun BackupBudget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    monthlyLimitCents = monthlyLimitCents
)

private fun ExpenseEntity.toBackup() = BackupExpense(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun BackupExpense.toEntity() = ExpenseEntity(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun TransferEntity.toBackup() = BackupTransfer(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amountCents = amountCents,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun BackupTransfer.toEntity() = TransferEntity(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amountCents = amountCents,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun RecurringRuleEntity.toBackup() = BackupRecurringRule(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    cadence = cadence,
    anchorEpochDay = anchorEpochDay,
    nextDueEpochDay = nextDueEpochDay,
    isPaused = isPaused,
    createdAtMillis = createdAtMillis
)

private fun BackupRecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    cadence = cadence,
    anchorEpochDay = anchorEpochDay,
    nextDueEpochDay = nextDueEpochDay,
    isPaused = isPaused,
    createdAtMillis = createdAtMillis
)
