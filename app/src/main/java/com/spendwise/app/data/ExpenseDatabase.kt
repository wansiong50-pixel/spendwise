package com.spendwise.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        AccountEntity::class,
        RecurringRuleEntity::class,
        TransferEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun transferDao(): TransferDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN iconName TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL("UPDATE categories SET iconName = 'restaurant' WHERE name = 'Food'")
                database.execSQL("UPDATE categories SET iconName = 'directions_car' WHERE name = 'Transport'")
                database.execSQL("UPDATE categories SET iconName = 'receipt' WHERE name = 'Bills'")
                database.execSQL("UPDATE categories SET iconName = 'shopping_bag' WHERE name = 'Shopping'")
                database.execSQL("UPDATE categories SET iconName = 'local_hospital' WHERE name = 'Health'")
                database.execSQL("UPDATE categories SET iconName = 'account_balance_wallet' WHERE name = 'Income/Adjustment'")
            }
        }

        // 2 -> 3 historically added the ai_models.downloadUrl column. Kept here so users
        // upgrading from v2 still pass through it before MIGRATION_3_4 drops the table.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_models ADD COLUMN downloadUrl TEXT")
            }
        }

        // AI integration was removed. Drop the orphan table on upgrade; expense and category
        // data are untouched.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS ai_models")
            }
        }

        // Rename the built-in income category from "Income/Adjustment" to
        // "Salary". The id (6L) and isIncomeAdjustment flag are unchanged —
        // any expenses already logged against this category keep working
        // (they reference categoryId, not name), and the analytics' income
        // track still pulls them in via the unchanged isIncomeAdjustment
        // flag. Targets the row by name so we don't risk clobbering a
        // future user-renamed category that happens to share id 6.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE categories SET name = 'Salary' WHERE name = 'Income/Adjustment' AND isCustom = 0"
                )
            }
        }

        // Multi-account ledger foundation. Adds an `accounts` table and binds
        // every existing transaction to a seeded default "Wallet" account so
        // the new FK column is satisfied without losing history. The expenses
        // table is recreated rather than ALTERed because SQLite cannot add a
        // FOREIGN KEY to an existing table — Room's runtime validator checks
        // FK definitions on every open, so an ALTER-only migration would fail
        // the schema match on first launch.
        //
        // Default Wallet uses id = 1L deterministically (AUTOINCREMENT is
        // honored because the INSERT explicitly names the id). Subsequent
        // user-created accounts get 2, 3, … in insertion order. The slate
        // color (0xff64748b) matches the built-in Salary category — keeps
        // the system-seeded rows visually quiet against user-chosen palettes.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. budgets table. Some pre-v6 installs never received this
                //    table, but v6's Room schema expects it. Creating it here
                //    keeps old databases from crashing during schema
                //    validation on first 1.1.0 launch.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `monthlyLimitCents` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)"
                )

                // 2. accounts table.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `startingBalanceCents` INTEGER NOT NULL,
                        `color` INTEGER NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 2. Seed default Wallet (id=1) — every existing expense will
                //    point here after the backfill below. starting balance is
                //    0 because the user hasn't told us their real balance yet;
                //    they edit it from the Accounts screen in Phase 2.
                database.execSQL(
                    """
                    INSERT INTO `accounts`
                        (`id`, `name`, `type`, `startingBalanceCents`, `color`, `iconName`, `sortOrder`, `isArchived`)
                    VALUES
                        (1, 'Wallet', 'CASH', 0, ${0xff64748bL}, 'account_balance_wallet', 0, 0)
                    """.trimIndent()
                )

                // 4. Recreate expenses with the new accountId column + FK.
                //    Mirror the categoryId FK exactly: NO ACTION on update,
                //    RESTRICT on delete. Indices recreated below since CREATE
                //    TABLE doesn't carry them over.
                database.execSQL(
                    """
                    CREATE TABLE `expenses_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `occurredAtMillis` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // 5. Copy data, backfilling accountId = 1 (default Wallet).
                database.execSQL(
                    """
                    INSERT INTO `expenses_new`
                        (`id`, `amountCents`, `categoryId`, `accountId`, `merchant`, `notes`, `occurredAtMillis`, `createdAtMillis`)
                    SELECT
                        `id`, `amountCents`, `categoryId`, 1, `merchant`, `notes`, `occurredAtMillis`, `createdAtMillis`
                    FROM `expenses`
                    """.trimIndent()
                )

                database.execSQL("DROP TABLE `expenses`")
                database.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

                // 6. Indices. Names must match what Room generates from the
                //    @Index annotations on ExpenseEntity, or the validator
                //    reports a mismatch and aborts the open.
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_occurredAtMillis` ON `expenses` (`occurredAtMillis`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_accountId` ON `expenses` (`accountId`)"
                )
            }
        }

        // Recurring transactions: templates the app materializes into real
        // expense rows on launch. Column shapes and FK actions must match
        // RecurringRuleEntity exactly or Room's validator aborts the open.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `cadence` TEXT NOT NULL,
                        `anchorEpochDay` INTEGER NOT NULL,
                        `nextDueEpochDay` INTEGER NOT NULL,
                        `isPaused` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_rules_categoryId` ON `recurring_rules` (`categoryId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_rules_accountId` ON `recurring_rules` (`accountId`)"
                )
            }
        }

        // Transfers between the user's own accounts. Shapes must match
        // TransferEntity exactly for Room's open-time validator.
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transfers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fromAccountId` INTEGER NOT NULL,
                        `toAccountId` INTEGER NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `occurredAtMillis` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`fromAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transfers_fromAccountId` ON `transfers` (`fromAccountId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transfers_toAccountId` ON `transfers` (`toAccountId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transfers_occurredAtMillis` ON `transfers` (`occurredAtMillis`)"
                )
            }
        }
    }
}
