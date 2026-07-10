package com.spendwise.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupPreferencesDataStore by preferencesDataStore(name = "backup_preferences")

/**
 * Auto-backup configuration and status.
 *
 * @property enabled whether the daily backup job should run.
 * @property treeUri persisted SAF tree URI of the user-chosen backup folder
 *   (string form of the URI granted via ACTION_OPEN_DOCUMENT_TREE), or null
 *   when no folder has been picked yet.
 * @property lastBackupMillis epoch millis of the last successful backup.
 * @property lastBackupExpenseCount expense rows in the last successful backup.
 * @property lastError human-readable reason the last run failed, or null when
 *   the last run succeeded (or none has run). The Settings screen renders
 *   this directly, so messages must be user-facing sentences.
 */
data class BackupSettings(
    val enabled: Boolean = false,
    val treeUri: String? = null,
    val lastBackupMillis: Long? = null,
    val lastBackupExpenseCount: Int? = null,
    val lastError: String? = null
)

class BackupPreferenceStore(
    private val context: Context
) {
    val settings: Flow<BackupSettings> = context.backupPreferencesDataStore.data.map { preferences ->
        BackupSettings(
            enabled = preferences[AUTO_BACKUP_ENABLED] ?: false,
            treeUri = preferences[BACKUP_TREE_URI],
            lastBackupMillis = preferences[LAST_BACKUP_MILLIS],
            lastBackupExpenseCount = preferences[LAST_BACKUP_EXPENSE_COUNT],
            lastError = preferences[LAST_BACKUP_ERROR]
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.backupPreferencesDataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setTreeUri(uri: String?) {
        context.backupPreferencesDataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(BACKUP_TREE_URI)
            } else {
                preferences[BACKUP_TREE_URI] = uri
            }
            // A fresh folder pick invalidates any stale "folder inaccessible"
            // error — the next run re-validates against the new grant.
            preferences.remove(LAST_BACKUP_ERROR)
        }
    }

    suspend fun recordSuccess(millis: Long, expenseCount: Int) {
        context.backupPreferencesDataStore.edit { preferences ->
            preferences[LAST_BACKUP_MILLIS] = millis
            preferences[LAST_BACKUP_EXPENSE_COUNT] = expenseCount
            preferences.remove(LAST_BACKUP_ERROR)
        }
    }

    suspend fun recordError(message: String) {
        context.backupPreferencesDataStore.edit { preferences ->
            preferences[LAST_BACKUP_ERROR] = message
        }
    }

    private companion object {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        val LAST_BACKUP_EXPENSE_COUNT = intPreferencesKey("last_backup_expense_count")
        val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
    }
}
