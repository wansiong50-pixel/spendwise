package com.spendwise.app.ui.botanical

import com.spendwise.app.domain.Budget
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.RecurrenceCadence
import com.spendwise.app.domain.RecurringRule
import com.spendwise.app.domain.Transfer
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotanicalDataTest {

    private val food = Category(1, "Food", 0, "restaurant")
    private val bills = Category(3, "Bills", 0, "receipt")
    private val shopping = Category(4, "Shopping", 0, "shopping_bag")
    private val salary = Category(6, "Salary", 0, "account_balance_wallet", isIncomeAdjustment = true)
    private val categories = listOf(food, bills, shopping, salary)
    private val byId = categories.associateBy { it.id }

    private fun expense(id: Long, category: Category, cents: Long, date: String, account: Long = 1, merchant: String = "Shop", notes: String = "") =
        Expense(
            id = id,
            amountCents = cents,
            categoryId = category.id,
            categoryName = category.name,
            accountId = account,
            merchant = merchant,
            notes = notes,
            occurredAtMillis = LocalDate.parse(date).startMillisKl(),
            createdAtMillis = id
        )

    private fun transfer(id: Long, from: Long, to: Long, cents: Long, date: String) = Transfer(
        id = id,
        fromAccountId = from,
        fromAccountName = "A$from",
        toAccountId = to,
        toAccountName = "A$to",
        amountCents = cents,
        notes = "Wallet top-up",
        occurredAtMillis = LocalDate.parse(date).startMillisKl(),
        createdAtMillis = id
    )

    @Test
    fun budgetRowsSortByHowMuchIsUsed() {
        val rows = budgetRows(
            categories,
            listOf(Budget(1, food.id, 60_000), Budget(2, bills.id, 100_000), Budget(3, salary.id, 5_000)),
            mapOf(food.id to 3_850L, bills.id to 96_590L)
        )
        assertEquals(listOf("Bills", "Food"), rows.map { it.category.name })
        assertEquals(BudgetTone.Near, rows[0].tone())
        assertEquals("Near limit · RM 34.10 left", rows[0].statusText())
        assertEquals("RM 561.50 left", rows[1].statusText())
    }

    @Test
    fun budgetStatusCoversReachedAndOver() {
        assertEquals("Budget reached · RM 0.00 left", BudgetRow(food, 5_000, 5_000).statusText())
        val over = BudgetRow(food, 7_550, 5_000)
        assertEquals(BudgetTone.Over, over.tone())
        assertEquals("RM 25.50 over budget", over.statusText())
    }

    @Test
    fun monthComparisonUsesLastMonthAsTheBaseline() {
        val totals = mapOf(YearMonth.of(2026, 8) to 237_600L, YearMonth.of(2026, 9) to 130_140L)
        val comparison = monthComparison({ totals[it] ?: 0L }, YearMonth.of(2026, 9))
        assertEquals(listOf(0L, 0L, 0L, 0L, 237_600L, 130_140L), comparison.series)
        assertEquals(-45, comparison.percent)
        assertEquals(YearMonth.of(2026, 8), comparison.previousMonth)
        assertNull(monthComparison({ if (it == YearMonth.of(2026, 9)) 10L else 0L }, YearMonth.of(2026, 9)).percent)
    }

    @Test
    fun ledgerListsNewestFirstAndKeepsTransfersOutOfTotals() {
        val items = ledgerItems(
            listOf(expense(1, food, 3_850, "2026-09-07"), expense(2, salary, 420_000, "2026-09-01")),
            listOf(transfer(3, 1, 2, 10_000, "2026-09-06")),
            setOf(salary.id)
        )
        assertEquals(listOf("e1", "t3", "e2"), items.map { it.key })
        val totals = totalsOf(items)
        assertEquals(420_000, totals.income)
        assertEquals(3_850, totals.expense)
        assertEquals(416_150, totals.net)
    }

    @Test
    fun activityFiltersMatchLikeThePrototype() {
        val lunch = LedgerItem.Entry(expense(1, food, 3_850, "2026-09-07", account = 1, merchant = "Kenny Hills", notes = "Coffee"), false)
        val pay = LedgerItem.Entry(expense(2, salary, 420_000, "2026-09-01", account = 1, merchant = "Monthly salary"), true)
        val move = LedgerItem.Move(transfer(3, 1, 2, 10_000, "2026-09-06"))

        assertTrue(matches(lunch, ActivityQuery(kind = ActivityKind.Expense), byId))
        assertFalse(matches(pay, ActivityQuery(kind = ActivityKind.Expense), byId))
        assertTrue(matches(move, ActivityQuery(kind = ActivityKind.Transfer), byId))

        // Search covers name, notes and category name.
        assertTrue(matches(lunch, ActivityQuery(search = "coffee"), byId))
        assertTrue(matches(lunch, ActivityQuery(search = "FOOD"), byId))
        assertFalse(matches(pay, ActivityQuery(search = "coffee"), byId))

        // A transfer matches an account filter from either side, never a category filter.
        assertTrue(matches(move, ActivityQuery(accountId = 2), byId))
        assertFalse(matches(move, ActivityQuery(categoryId = food.id), byId))
        assertFalse(matches(lunch, ActivityQuery(accountId = 2), byId))
    }

    @Test
    fun transfersWithoutNotesGetANeutralName() {
        assertEquals("Wallet top-up", transferTitle(transfer(1, 1, 2, 100, "2026-09-06")))
        assertEquals("Account transfer", transferTitle(transfer(1, 1, 2, 100, "2026-09-06").copy(notes = "  ")))
    }

    @Test
    fun recurringEstimateNormalisesToAMonth() {
        fun rule(cents: Long, cadence: RecurrenceCadence, paused: Boolean = false, income: Boolean = false) = RecurringRule(
            id = cents, amountCents = cents, categoryId = 1, categoryName = "Bills", accountId = 1,
            merchant = "Rule", notes = "", cadence = cadence, anchorEpochDay = 0, nextDueEpochDay = 0,
            isPaused = paused, createdAtMillis = 0, isIncome = income
        )
        val estimate = estimatedMonthlyExpenses(
            listOf(
                rule(95_000, RecurrenceCadence.Monthly),
                rule(1_200, RecurrenceCadence.Weekly),          // 1,200 × 52 / 12 = 5,200
                rule(12_000, RecurrenceCadence.Yearly),         // 1,000
                rule(50_000, RecurrenceCadence.Monthly, paused = true),
                rule(420_000, RecurrenceCadence.Monthly, income = true)
            )
        )
        assertEquals(101_200, estimate)
    }
}
