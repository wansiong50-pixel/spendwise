package com.spendwise.app.domain

enum class ExpenseValidationError {
    InvalidAmount,
    MissingMerchant,
    MissingCategory
}

object ExpenseValidator {
    /**
     * Validates a draft expense. Pass [isIncome] = true when the chosen category
     * is an income/adjustment — the merchant requirement is dropped because
     * users describing payday or refunds shouldn't be forced to type filler.
     */
    fun validate(
        amountInput: String,
        merchant: String,
        categoryId: Long?,
        isIncome: Boolean = false
    ): List<ExpenseValidationError> {
        val errors = mutableListOf<ExpenseValidationError>()

        if (MoneyFormatter.parseToCents(amountInput) == null) {
            errors += ExpenseValidationError.InvalidAmount
        }
        if (!isIncome && merchant.isBlank()) {
            errors += ExpenseValidationError.MissingMerchant
        }
        if (categoryId == null) {
            errors += ExpenseValidationError.MissingCategory
        }

        return errors
    }
}
