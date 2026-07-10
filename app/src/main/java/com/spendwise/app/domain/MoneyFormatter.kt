package com.spendwise.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private val formatSymbols = DecimalFormatSymbols(Locale.US)
    private val amountFormat = DecimalFormat("#,##0.00", formatSymbols)

    /**
     * Parses user input to cents. Rejects negatives always; rejects zero
     * unless [allowZero] — expenses must be above RM 0.00, but account
     * starting balances legitimately start at zero (typing "0" should behave
     * exactly like leaving the field blank).
     */
    fun parseToCents(input: String, allowZero: Boolean = false): Long? {
        val normalized = input
            .trim()
            .replace("RM", "", ignoreCase = true)
            .replace(",", "")
            .replace(" ", "")

        if (normalized.isBlank()) return null

        val amount = normalized.toBigDecimalOrNull() ?: return null
        if (amount.signum() < 0) return null
        if (amount.signum() == 0 && !allowZero) return null

        return try {
            amount
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .longValueExact()
        } catch (_: ArithmeticException) {
            // Overflow — the cents value doesn't fit in a Long. Treat like any
            // other unparseable amount instead of crashing the save path.
            null
        }
    }

    fun formatCents(cents: Long): String {
        return "RM ${amountFormat.format(cents / 100.0)}"
    }

    /**
     * Renders cents as a plain "12.34" string for prefilling editable amount
     * fields. Built from integer arithmetic — deliberately NOT printf/locale
     * formatting, because a comma-decimal device locale would produce "12,34",
     * which [parseToCents] reads back as 1234 (it strips commas as thousands
     * separators): a silent ×100 corruption on every edit-and-save.
     */
    fun centsToInput(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = if (cents < 0) -cents else cents
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }
}
