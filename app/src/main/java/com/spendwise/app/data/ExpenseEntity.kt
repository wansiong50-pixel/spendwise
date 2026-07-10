package com.spendwise.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val color: Long,
    val iconName: String,
    val isIncomeAdjustment: Boolean,
    val isCustom: Boolean = false
)

/**
 * A user-owned money source — a checking account, an e-wallet, cash on hand,
 * etc. Every expense and income row is anchored to exactly one account; the
 * account's current balance is derived as
 * `startingBalanceCents + Σ(incomes) − Σ(expenses)` (computed on read so any
 * edit or delete remains consistent without write-side bookkeeping).
 *
 * No bank integration — accounts are manual. [startingBalanceCents] is the
 * balance the user enters at the moment they create the account; from then on
 * the running balance moves only as the user logs transactions against it.
 *
 * [type] uses a string column (CASH | BANK | EWALLET | CREDIT) rather than an
 * enum so future types don't require a schema migration. UI maps the string to
 * an icon and treats it as advisory metadata, not as behavior (Phase 1 treats
 * every account as an asset; CREDIT will get inverted-balance handling later).
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val startingBalanceCents: Long,
    val color: Long,
    val iconName: String,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        // RESTRICT mirrors the category FK: the UI must reassign or delete the
        // account's transactions before the account row itself can be dropped.
        // CASCADE would silently nuke spending history when a user archives a
        // card — wrong default for a financial ledger.
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("occurredAtMillis"), Index("accountId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val merchant: String,
    val notes: String,
    val occurredAtMillis: Long,
    val createdAtMillis: Long
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthlyLimitCents: Long
)

fun BudgetEntity.toDomain(): com.spendwise.app.domain.Budget {
    return com.spendwise.app.domain.Budget(
        id = id,
        categoryId = categoryId,
        monthlyLimitCents = monthlyLimitCents
    )
}
