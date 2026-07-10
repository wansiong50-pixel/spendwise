package com.spendwise.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.spendwise.app.ExpenseTrackerApplication
import com.spendwise.app.export.BackupResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Writes a dated JSON backup into the user-chosen SAF folder and prunes old
 * auto backups down to [BackupRotation.DEFAULT_KEEP]. Runs daily via the
 * unique periodic work enqueued in [schedule]; the Settings "Back up now"
 * button funnels through the same worker via [backupNow].
 *
 * R8: androidx.work's consumer rules keep ListenableWorker subclasses'
 * (Context, WorkerParameters) constructors, so no proguard-rules.pro entry
 * is needed for this class.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ExpenseTrackerApplication).container
        val store = container.backupPreferenceStore

        return try {
            val settings = store.settings.first()
            // Stale periodic work (toggle raced the cancel) — nothing to do,
            // and failure would just spam retries. Manual "Back up now" runs
            // are honored regardless of the daily toggle.
            val isManual = inputData.getBoolean(KEY_MANUAL, false)
            if (!settings.enabled && !isManual) return Result.success()
            val treeUriString = settings.treeUri ?: return Result.success()

            val tree = resolveWritableTree(treeUriString)
            if (tree == null) {
                store.recordError(FOLDER_INACCESSIBLE_MESSAGE)
                return Result.failure()
            }

            val fileName = BackupRotation.autoBackupFileName(System.currentTimeMillis())
            val file = tree.createFile("application/json", fileName)
            if (file == null) {
                store.recordError(FOLDER_INACCESSIBLE_MESSAGE)
                return Result.failure()
            }

            when (val result = container.backupManager.exportBackup(file.uri)) {
                is BackupResult.ExportSuccess -> {
                    store.recordSuccess(System.currentTimeMillis(), result.expenseCount)
                    prune(tree)
                    Result.success()
                }
                is BackupResult.Failure -> retryOrFail(store::recordError, result.reason)
                // Import can't come out of an export call; treat defensively.
                is BackupResult.ImportSuccess -> Result.success()
            }
        } catch (t: Throwable) {
            retryOrFail(store::recordError, t.message ?: "Backup failed unexpectedly.")
        }
    }

    /**
     * The folder is writable only if the persistable grant is still held AND
     * the directory still exists — either can silently disappear (user revoked
     * in system settings, deleted the folder, uninstalled the cloud app).
     */
    private fun resolveWritableTree(treeUriString: String): DocumentFile? {
        val uri = Uri.parse(treeUriString)
        val stillGranted = applicationContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        if (!stillGranted) return null
        val tree = DocumentFile.fromTreeUri(applicationContext, uri) ?: return null
        return if (tree.exists() && tree.canWrite()) tree else null
    }

    private fun prune(tree: DocumentFile) {
        val children = tree.listFiles()
        val doomed = BackupRotation.filesToPrune(children.mapNotNull { it.name }).toSet()
        children
            .filter { it.name in doomed }
            // Individual delete failures are ignored: worst case an extra old
            // backup lingers until the next successful run prunes it.
            .forEach { runCatching { it.delete() } }
    }

    private suspend fun retryOrFail(
        recordError: suspend (String) -> Unit,
        reason: String
    ): Result {
        return if (runAttemptCount < MAX_ATTEMPTS) {
            Result.retry()
        } else {
            recordError(reason)
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC = "spendwise_auto_backup"
        private const val UNIQUE_ONESHOT = "spendwise_backup_now"
        private const val KEY_MANUAL = "manual"
        private const val MAX_ATTEMPTS = 3
        private const val FOLDER_INACCESSIBLE_MESSAGE =
            "Backup folder is no longer accessible — choose a new folder in Settings."

        /** Persistable-permission flags the folder picker must take. */
        const val URI_PERMISSION_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        /**
         * Enqueue the daily backup. KEEP preserves the period phase across
         * toggle round-trips; WorkManager persists the schedule over reboots,
         * so this only needs to be called when the user turns the toggle on.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                24, TimeUnit.HOURS,
                4, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
        }

        /** One-shot run of the same worker — the "Back up now" button. */
        fun backupNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(KEY_MANUAL to true))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_ONESHOT, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
