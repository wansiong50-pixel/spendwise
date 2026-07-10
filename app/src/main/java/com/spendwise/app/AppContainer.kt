package com.spendwise.app

import android.content.Context
import androidx.room.Room
import com.spendwise.app.analytics.SpendingAnalyzer
import com.spendwise.app.data.AppearancePreferenceStore
import com.spendwise.app.data.BackupPreferenceStore
import com.spendwise.app.data.DefaultExpenseRepository
import com.spendwise.app.data.ExpenseDatabase
import com.spendwise.app.export.BackupManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database = Room.databaseBuilder(
        appContext,
        ExpenseDatabase::class.java,
        "expense_tracker.db"
    ).addMigrations(
        ExpenseDatabase.MIGRATION_1_2,
        ExpenseDatabase.MIGRATION_2_3,
        ExpenseDatabase.MIGRATION_3_4,
        ExpenseDatabase.MIGRATION_4_5,
        ExpenseDatabase.MIGRATION_5_6
    ).build()

    val appearancePreferenceStore = AppearancePreferenceStore(appContext)
    val backupPreferenceStore = BackupPreferenceStore(appContext)
    val expenseRepository = DefaultExpenseRepository(
        database = database,
        expenseDao = database.expenseDao(),
        categoryDao = database.categoryDao(),
        accountDao = database.accountDao(),
        budgetDao = database.budgetDao()
    )
    val spendingAnalyzer = SpendingAnalyzer()
    val backupManager = BackupManager(
        context = appContext,
        database = database,
        expenseDao = database.expenseDao(),
        categoryDao = database.categoryDao(),
        accountDao = database.accountDao(),
        budgetDao = database.budgetDao(),
        appearancePreferenceStore = appearancePreferenceStore
    )
}
