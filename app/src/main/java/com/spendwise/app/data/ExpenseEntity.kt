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

/**
 * A recurring transaction template — rent, a subscription, a salary. The app
 * materializes real [ExpenseEntity] rows from it on launch whenever
 * [nextDueEpochDay] has passed, then advances the due date by [cadence].
 *
 * Dates are stored as epoch days of the KL-time calendar date (matching how
 * the rest of the app buckets days) rather than millis — a recurrence is a
 * calendar concept ("the 1st of every month"), not an instant.
 *
 * [anchorEpochDay] is the first occurrence the user picked and never moves;
 * it preserves the intended day-of-month across short months (rule anchored
 * on the 31st fires Feb 28, then back to Mar 31 — without the anchor the day
 * would ratchet down permanently after February).
 *
 * FKs are RESTRICT like expenses'; the category-deletion flow explicitly
 * reassigns or deletes rules alongside the expenses so the RESTRICT never
 * fires in practice.
 */
@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("accountId")]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val merchant: String,
    val notes: String,
    /** WEEKLY | MONTHLY | YEARLY — string so new cadences skip a migration. */
    val cadence: String,
    val anchorEpochDay: Long,
    val nextDueEpochDay: Long,
    val isPaused: Boolean = false,
    val createdAtMillis: Long
)

/**
 * Money moved between two of the user's own accounts — a bank withdrawal, a
 * credit-card bill payment, topping up an e-wallet. One atomic row (never a
 * linked expense/income pair, which could desync under edits).
 *
 * Deliberately its own table rather than a flavor of [ExpenseEntity]: every
 * analytics query (spent/earned totals, heatmap, category stats, budgets,
 * CSV export) reads `expenses`, so transfers are excluded from all of them
 * by construction — moving your own money between pockets is not spending.
 * Only derived account balances read this table.
 *
 * FKs RESTRICT like expenses'; accounts can't be deleted, and the archive
 * flow blocks while transfers reference the account.
 */
@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("fromAccountId"), Index("toAccountId"), Index("occurredAtMillis")]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountCents: Long,
    val notes: String,
    val occurredAtMillis: Long,
    val createdAtMillis: Long
)

fun BudgetEntity.toDomain(): com.spendwise.app.domain.Budget {
    return com.spendwise.app.domain.Budget(
        id = id,
        categoryId = categoryId,
        monthlyLimitCents = monthlyLimitCents
    )
}
