package com.spendwise.app.analytics

import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingAnalyzerTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")
    private val categories = listOf(
        Category(1L, "Food", 0xff2f7d6d),
        Category(2L, "Transport", 0xfff2b84b),
        Category(6L, "Salary", 0xff64748b, iconName = "account_balance_wallet", isIncomeAdjustment = true)
    )

    @Test
    fun buildsMonthlySummaryWithTotalsAndRecentTransactions() {
        val expenses = listOf(
            expense(1, 1L, 2490, "Lunch", LocalDate.of(2026, 4, 10)),
            expense(2, 2L, 1200, "Train", LocalDate.of(2026, 4, 12)),
            expense(3, 1L, 1890, "Dinner", LocalDate.of(2026, 4, 13)),
            expense(4, 1L, 3000, "Old", LocalDate.of(2026, 3, 30))
        )

        val summary = SpendingAnalyzer(zone).summarize(
            expenses = expenses,
            categories = categories,
            month = YearMonth.of(2026, 4)
        )

        assertEquals(5580L, summary.totalExpenseCents)
        assertEquals(3, summary.transactionCount)
        assertEquals(4380L, summary.categoryTotals.first { it.categoryName == "Food" }.totalCents)
        assertEquals(3L, summary.recentTransactions.first().id)
    }

    @Test
    fun summarize_separatesIncomeFromExpenses() {
        val expenses = listOf(
            expense(1, 1L, 2490, "Lunch", LocalDate.of(2026, 4, 10)),
            expense(2, 6L, 500_000, "Salary", LocalDate.of(2026, 4, 1))
        )

        val summary = SpendingAnalyzer(zone).summarize(
            expenses = expenses,
            categories = categories,
            month = YearMonth.of(2026, 4)
        )

        // Expense track only contains the lunch.
        assertEquals(2490L, summary.totalExpenseCents)
        assertEquals(1, summary.transactionCount)
        // Income track only contains the salary.
        assertEquals(500_000L, summary.totalIncomeCents)
        assertEquals(1, summary.incomeTransactionCount)
        assertEquals(1, summary.incomeCategoryTotals.size)
        assertEquals("Salary", summary.incomeCategoryTotals.first().categoryName)
        assertEquals("account_balance_wallet", summary.incomeCategoryTotals.first().categoryIconName)
        assertEquals(0xff64748b, summary.incomeCategoryTotals.first().categoryColor)
        // Net = income - expense.
        assertEquals(497_510L, summary.netCents)
    }

    @Test
    fun summarize_emptyIncome_returnsZeroIncomeFields() {
        val expenses = listOf(
            expense(1, 1L, 2490, "Lunch", LocalDate.of(2026, 4, 10))
        )

        val summary = SpendingAnalyzer(zone).summarize(
            expenses = expenses,
            categories = categories,
            month = YearMonth.of(2026, 4)
        )

        // First-run / no-income path stays zeroed and net = -expense.
        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(0, summary.incomeTransactionCount)
        assertEquals(emptyList<Any>(), summary.incomeCategoryTotals)
        assertEquals(emptyList<Any>(), summary.incomeRecentTransactions)
        assertEquals(-2490L, summary.netCents)
    }

    @Test
    fun summarize_populatesDailyIncomeTotalsAlongsideDailyExpenseTotals() {
        // The Cashflow line chart needs daily income just like it has daily
        // spending, so it can plot cumulative *net* when income exists.
        val expenses = listOf(
            expense(1, 6L, 500_000, "Salary", LocalDate.of(2026, 4, 1)),
            expense(2, 1L, 2490, "Lunch", LocalDate.of(2026, 4, 10)),
            expense(3, 1L, 1500, "Coffee", LocalDate.of(2026, 4, 10)),
            expense(4, 6L, 100_000, "Bonus", LocalDate.of(2026, 4, 15))
        )

        val summary = SpendingAnalyzer(zone).summarize(
            expenses = expenses,
            categories = categories,
            month = YearMonth.of(2026, 4)
        )

        // Daily income: 5,000 on day 1, 1,000 on day 15.
        assertEquals(500_000L, summary.dailyIncomeTotals[1])
        assertEquals(100_000L, summary.dailyIncomeTotals[15])
        assertEquals(null, summary.dailyIncomeTotals[10])
        // Daily expense unchanged: 39.90 on day 10.
        assertEquals(3990L, summary.dailyTotals[10])
    }

    @Test
    fun summarize_customIncomeCategoryFlowsToIncomeTrack() {
        // Simulates a user-created custom income category — e.g. "Salary"
        // with isIncomeAdjustment = true. Must flow into income track,
        // not expense track, just like the built-in income/adjustment cat.
        val withCustomIncome = categories + Category(
            id = 7L,
            name = "Salary",
            color = 0xff426b55,
            isIncomeAdjustment = true,
            isCustom = true
        )
        val expenses = listOf(
            expense(1, 1L, 2490, "Lunch", LocalDate.of(2026, 4, 10)),
            expense(2, 7L, 750_000, "Payday", LocalDate.of(2026, 4, 1))
        )

        val summary = SpendingAnalyzer(zone).summarize(
            expenses = expenses,
            categories = withCustomIncome,
            month = YearMonth.of(2026, 4)
        )

        assertEquals(750_000L, summary.totalIncomeCents)
        assertEquals(2490L, summary.totalExpenseCents)
        assertEquals("Salary", summary.incomeCategoryTotals.first().categoryName)
    }

    private fun expense(
        id: Long,
        categoryId: Long,
        amountCents: Long,
        merchant: String,
        date: LocalDate
    ): Expense {
        return Expense(
            id = id,
            amountCents = amountCents,
            categoryId = categoryId,
            // Tolerate ids that aren't in the class-level `categories` list —
            // tests can pass a custom category id (e.g. 7L for "Salary") and
            // the analyzer resolves the display name from the category list
            // it's given, not from this snapshot.
            categoryName = categories.firstOrNull { it.id == categoryId }?.name.orEmpty(),
            // Account isn't part of what SpendingAnalyzer tests today; pin to
            // the seeded default Wallet id so the fixture stays valid against
            // the new required field without affecting any monthly-summary
            // assertion below.
            accountId = 1L,
            merchant = merchant,
            notes = "",
            occurredAtMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            createdAtMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }
}
