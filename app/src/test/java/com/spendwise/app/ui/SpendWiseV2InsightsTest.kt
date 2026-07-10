package com.spendwise.app.ui

import com.spendwise.app.domain.Expense
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendWiseV2InsightsTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")

    @Test
    fun filtersInsightsCategoriesToSelectedMonthAndMode() {
        val expenses = listOf(
            expense(id = 1, categoryId = 10, categoryName = "Food", cents = 1_200, date = "2026-05-03"),
            expense(id = 2, categoryId = 10, categoryName = "Food", cents = 800, date = "2026-05-04"),
            expense(id = 3, categoryId = 20, categoryName = "Salary", cents = 500_000, date = "2026-05-01"),
            expense(id = 4, categoryId = 10, categoryName = "Food", cents = 9_999, date = "2026-06-01")
        )

        val spend = insightsCategoryTotalsForMonth(
            expenses = expenses,
            selectedYear = 2026,
            selectedMonth = 5,
            incomeCategoryIds = setOf(20),
            mode = InsightsCategoryMode.Spend
        )
        val income = insightsCategoryTotalsForMonth(
            expenses = expenses,
            selectedYear = 2026,
            selectedMonth = 5,
            incomeCategoryIds = setOf(20),
            mode = InsightsCategoryMode.Income
        )

        assertEquals(1, spend.size)
        assertEquals("Food", spend.single().categoryName)
        assertEquals(2_000, spend.single().totalCents)
        assertEquals(1, income.size)
        assertEquals("Salary", income.single().categoryName)
        assertEquals(500_000, income.single().totalCents)
    }

    @Test
    fun clampsSelectedInsightsMonthWhenYearChanges() {
        val current = LocalDate.of(2026, 5, 25)

        assertEquals(
            5,
            coerceInsightsMonthForYear(selectedYear = 2026, selectedMonth = 12, today = current)
        )
        assertEquals(
            12,
            coerceInsightsMonthForYear(selectedYear = 2025, selectedMonth = 12, today = current)
        )
        assertEquals(
            1,
            coerceInsightsMonthForYear(selectedYear = 2026, selectedMonth = 0, today = current)
        )
    }

    private fun expense(
        id: Long,
        categoryId: Long,
        categoryName: String,
        cents: Long,
        date: String
    ): Expense {
        val millis = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
        return Expense(
            id = id,
            amountCents = cents,
            categoryId = categoryId,
            categoryName = categoryName,
            accountId = 1,
            merchant = categoryName,
            notes = "",
            occurredAtMillis = millis,
            createdAtMillis = millis
        )
    }
}
