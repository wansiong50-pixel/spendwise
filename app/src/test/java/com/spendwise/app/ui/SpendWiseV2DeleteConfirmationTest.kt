package com.spendwise.app.ui

import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Transfer
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendWiseV2DeleteConfirmationTest {

    @Test
    fun expensePromptIdentifiesTheExactLedgerEntry() {
        val prompt = V2DeleteTarget.ExpenseEntry(
            expense = expense(merchant = "Grab", categoryName = "Transport"),
            isIncome = false
        ).toDeletePrompt()

        assertEquals("Delete expense?", prompt.title)
        assertEquals("Grab", prompt.subject)
        assertEquals("RM 42.50 \u00B7 Transport \u00B7 22 Jul 2026", prompt.context)
        assertTrue(prompt.impact.contains("permanently removed"))
    }

    @Test
    fun incomePromptFallsBackToCategoryWhenDescriptionIsBlank() {
        val prompt = V2DeleteTarget.ExpenseEntry(
            expense = expense(merchant = "", categoryName = "Salary"),
            isIncome = true
        ).toDeletePrompt()

        assertEquals("Delete income?", prompt.title)
        assertEquals("Salary", prompt.subject)
        assertEquals("RM 42.50 \u00B7 22 Jul 2026", prompt.context)
    }

    @Test
    fun transferPromptExplainsBothBalanceImpact() {
        val prompt = V2DeleteTarget.TransferEntry(
            Transfer(
                id = 9L,
                fromAccountId = 1L,
                fromAccountName = "Maybank",
                toAccountId = 2L,
                toAccountName = "Wallet",
                amountCents = 10_000L,
                notes = "",
                occurredAtMillis = dateMillis(),
                createdAtMillis = dateMillis()
            )
        ).toDeletePrompt()

        assertEquals("Delete transfer?", prompt.title)
        assertEquals("Maybank \u2192 Wallet", prompt.subject)
        assertEquals("RM 100.00 \u00B7 22 Jul 2026", prompt.context)
        assertTrue(prompt.impact.contains("Both account balances"))
    }

    private fun expense(merchant: String, categoryName: String) = Expense(
        id = 7L,
        amountCents = 4_250L,
        categoryId = 2L,
        categoryName = categoryName,
        accountId = 1L,
        merchant = merchant,
        notes = "",
        occurredAtMillis = dateMillis(),
        createdAtMillis = dateMillis()
    )

    private fun dateMillis(): Long = LocalDate.of(2026, 7, 22)
        .atStartOfDay(ZoneId.of("Asia/Kuala_Lumpur"))
        .toInstant()
        .toEpochMilli()
}
