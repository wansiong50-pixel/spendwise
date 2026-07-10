package com.spendwise.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun parsesRinggitAmountToCents() {
        assertEquals(2490L, MoneyFormatter.parseToCents("RM 24.90"))
        assertEquals(184200L, MoneyFormatter.parseToCents("1,842"))
        assertEquals(500L, MoneyFormatter.parseToCents("5"))
    }

    @Test
    fun rejectsInvalidOrNegativeAmounts() {
        assertNull(MoneyFormatter.parseToCents(""))
        assertNull(MoneyFormatter.parseToCents("abc"))
        assertNull(MoneyFormatter.parseToCents("-12.00"))
    }

    @Test
    fun formatsCentsAsRinggit() {
        assertEquals("RM 24.90", MoneyFormatter.formatCents(2490))
        assertEquals("RM 1,842.00", MoneyFormatter.formatCents(184200))
    }
}
