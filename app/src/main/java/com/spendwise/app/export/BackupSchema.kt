package com.spendwise.app.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Backup format version. Bump only when the JSON shape changes in a way that
// older code can't read. Adding optional fields (with defaults) does NOT
// require a bump — kotlinx.serialization tolerates missing keys when a
// default is declared, so older backups still load on newer apps.
internal const val CURRENT_BACKUP_SCHEMA_VERSION: Int = 1

// Top-level envelope. Intentionally a copy of (not a reference to) the Room
// entity classes — the backup file is a stable contract that must survive
// future @Entity changes (column adds/renames). If a Room field is added,
// extend the matching backup class here with a default value and write a
// mapper in BackupManager; existing backups will continue to deserialize.
@Serializable
data class BackupEnvelope(
    @SerialName("schemaVersion") val schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
    @SerialName("appDbVersion") val appDbVersion: Int,
    @SerialName("exportedAtMillis") val exportedAtMillis: Long,
    @SerialName("darkModeEnabled") val darkModeEnabled: Boolean? = null,
    @SerialName("categories") val categories: List<BackupCategory> = emptyList(),
    @SerialName("accounts") val accounts: List<BackupAccount> = emptyList(),
    @SerialName("budgets") val budgets: List<BackupBudget> = emptyList(),
    @SerialName("expenses") val expenses: List<BackupExpense> = emptyList()
)

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val color: Long,
    val iconName: String,
    val isIncomeAdjustment: Boolean,
    val isCustom: Boolean = false
)

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val type: String,
    val startingBalanceCents: Long,
    val color: Long,
    val iconName: String,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)

@Serializable
data class BackupBudget(
    val id: Long,
    val categoryId: Long,
    val monthlyLimitCents: Long
)

@Serializable
data class BackupExpense(
    val id: Long,
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val merchant: String,
    val notes: String,
    val occurredAtMillis: Long,
    val createdAtMillis: Long
)
