package com.spendwise.app.analytics

import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryTotal
import com.spendwise.app.domain.Expense
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.TreeMap

class SpendingAnalyzer(
    private val zoneId: ZoneId = ZoneId.of("Asia/Kuala_Lumpur"),
    private val currencyCode: String = "MYR"
) {
    fun summarize(
        expenses: List<Expense>,
        categories: List<Category>,
        month: YearMonth = YearMonth.now(zoneId)
    ): MonthlySpendingSummary {
        val categoriesById = categories.associateBy { it.id }
        val expenseDailyTotals = TreeMap<Int, Long>()
        val incomeDailyTotals = TreeMap<Int, Long>()
        val expenseCategoryTotals = HashMap<Long, CategoryAccumulator>()
        val incomeCategoryTotals = HashMap<Long, CategoryAccumulator>()
        val expenseRecent = ArrayList<Expense>()
        val incomeRecent = ArrayList<Expense>()
        var totalExpenseCents = 0L
        var totalIncomeCents = 0L
        var expenseCount = 0
        var incomeCount = 0

        expenses.forEach { expense ->
            val occurredAt = Instant.ofEpochMilli(expense.occurredAtMillis).atZone(zoneId)
            if (YearMonth.from(occurredAt) != month) return@forEach

            val day = occurredAt.dayOfMonth
            val category = categoriesById[expense.categoryId]
            val isIncome = category?.isIncomeAdjustment == true
            val dailyTotals = if (isIncome) incomeDailyTotals else expenseDailyTotals
            val categoryTotals = if (isIncome) incomeCategoryTotals else expenseCategoryTotals

            dailyTotals[day] = (dailyTotals[day] ?: 0L) + expense.amountCents
            categoryTotals
                .getOrPut(expense.categoryId) { CategoryAccumulator.from(expense, category) }
                .add(expense.amountCents)

            if (isIncome) {
                totalIncomeCents += expense.amountCents
                incomeCount += 1
                incomeRecent += expense
            } else {
                totalExpenseCents += expense.amountCents
                expenseCount += 1
                expenseRecent += expense
            }
        }

        return MonthlySpendingSummary(
            month = month,
            currencyCode = currencyCode,
            totalExpenseCents = totalExpenseCents,
            transactionCount = expenseCount,
            categoryTotals = buildCategoryTotals(expenseCategoryTotals),
            recentTransactions = expenseRecent
                .sortedByDescending { it.occurredAtMillis }
                .take(8),
            dailyTotals = expenseDailyTotals,
            totalIncomeCents = totalIncomeCents,
            incomeTransactionCount = incomeCount,
            incomeCategoryTotals = buildCategoryTotals(incomeCategoryTotals),
            incomeRecentTransactions = incomeRecent
                .sortedByDescending { it.occurredAtMillis }
                .take(8),
            dailyIncomeTotals = incomeDailyTotals
        )
    }

    private fun buildCategoryTotals(totals: Map<Long, CategoryAccumulator>): List<CategoryTotal> {
        return totals
            .map { (categoryId, total) ->
                CategoryTotal(
                    categoryId = categoryId,
                    categoryName = total.categoryName,
                    totalCents = total.totalCents,
                    transactionCount = total.transactionCount,
                    categoryIconName = total.categoryIconName,
                    categoryColor = total.categoryColor
                )
            }
            .sortedByDescending { it.totalCents }
    }

    private data class CategoryAccumulator(
        val categoryName: String,
        val categoryIconName: String,
        val categoryColor: Long?,
        var totalCents: Long = 0L,
        var transactionCount: Int = 0
    ) {
        fun add(amountCents: Long) {
            totalCents += amountCents
            transactionCount += 1
        }

        companion object {
            fun from(expense: Expense, category: Category?): CategoryAccumulator {
                return CategoryAccumulator(
                    categoryName = category?.name ?: expense.categoryName,
                    categoryIconName = category?.iconName ?: expense.categoryIconName,
                    categoryColor = category?.color ?: expense.categoryColor
                )
            }
        }
    }
}
