package com.spendwise.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantNamesTest {
    @Test
    fun canonicalizesWhitespaceAndCaseToExistingMerchant() {
        val existing = listOf("Village Park", "Grab")

        assertEquals("Village Park", MerchantNames.canonicalize("  village   park ", existing))
    }

    @Test
    fun canonicalizesSmallTypoToExistingMerchant() {
        val existing = listOf("Starbucks Reserve", "TNB")

        assertEquals("Starbucks Reserve", MerchantNames.canonicalize("starbuck reserve", existing))
    }

    @Test
    fun leavesNewMerchantCleanedButUnchanged() {
        assertEquals("Nasi Lemak Antarabangsa", MerchantNames.canonicalize("  Nasi   Lemak Antarabangsa  ", emptyList()))
    }

    @Test
    fun ranksExactPrefixBeforeFuzzyMatches() {
        val suggestions = MerchantNames.suggest(
            query = "vil",
            existing = listOf("Watsons", "Village Park", "Villa Grocer")
        )

        assertEquals(listOf("Village Park", "Villa Grocer"), suggestions)
    }

    @Test
    fun blankInputHasNoCanonicalMerchant() {
        assertNull(MerchantNames.canonicalizeOrNull("   ", listOf("Grab")))
    }
}
