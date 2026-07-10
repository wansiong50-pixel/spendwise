package com.spendwise.app.analytics

import com.spendwise.app.domain.CategoryTotal
import com.spendwise.app.domain.Expense
import java.time.YearMonth

data class MonthlySpendingSummary(
    val month: YearMonth,
    val currencyCode: String,
    val totalExpenseCents: Long,
    val transactionCount: Int,
    val categoryTotals: List<CategoryTotal>,
    val recentTransactions: List<Expense>,
    val dailyTotals: Map<Int, Long>,
    // Parallel "income track" — populated by SpendingAnalyzer when entries
    // sit in a category with isIncomeAdjustment = true. The existing expense
    // fields above stay untouched (chart and existing UI keep their data
    // shape). `dailyIncomeTotals` mirrors `dailyTotals` so the Cashflow line
    // chart can plot cumulative *net* (income − spend) when income exists.
    val totalIncomeCents: Long = 0L,
    val incomeTransactionCount: Int = 0,
    val incomeCategoryTotals: List<CategoryTotal> = emptyList(),
    val incomeRecentTransactions: List<Expense> = emptyList(),
    val dailyIncomeTotals: Map<Int, Long> = emptyMap()
) {
    val netCents: Long get() = totalIncomeCents - totalExpenseCents
}
