package com.spendwise.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseValidatorTest {
    @Test
    fun validExpenseHasNoErrors() {
        val errors = ExpenseValidator.validate(
            amountInput = "24.90",
            merchant = "Nasi Kandar",
            categoryId = 1L
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidExpenseReportsAmountMerchantAndCategoryErrors() {
        val errors = ExpenseValidator.validate(
            amountInput = "0",
            merchant = "",
            categoryId = null
        )

        assertEquals(
            listOf(
                ExpenseValidationError.InvalidAmount,
                ExpenseValidationError.MissingMerchant,
                ExpenseValidationError.MissingCategory
            ),
            errors
        )
    }
}
