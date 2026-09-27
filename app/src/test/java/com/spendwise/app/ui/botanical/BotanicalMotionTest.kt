package com.spendwise.app.ui.botanical

import androidx.compose.ui.text.AnnotatedString
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BotanicalMotionTest {

    @Test
    fun amountGlyphsSpellTheFormattedAmount() {
        listOf(0L, 5L, 12L, 123L, 1_234L, 123_456L, 1_234_567L, 99_999_999_999L).forEach { cents ->
            assertEquals(formatAmount(cents), amountGlyphs(cents).joinToString("") { it.text })
        }
    }

    @Test
    fun typedDigitsKeepTheirKeysAsTheyShiftLeft() {
        // 1, 2, 3 typed: the "1" keeps its key while it moves from the cents to the ringgit.
        fun keyOf(cents: Long, text: String) = amountGlyphs(cents).single { it.text == text }.key
        assertEquals("d0", keyOf(1, "1"))
        assertEquals("d0", keyOf(12, "1"))
        assertEquals("d0", keyOf(123, "1"))
        assertEquals("d2", keyOf(123, "3"))
    }

    @Test
    fun amountGlyphKeysAreUnique() {
        listOf(0L, 7L, 1_234_567L, 99_999_999_999L).forEach { cents ->
            val keys = amountGlyphs(cents).map { it.key }
            assertEquals(keys.size, keys.toSet().size)
        }
    }

    @Test
    fun moneyGlyphsKeepSeparatorsApartAndTheCurrencyTogether() {
        val cells = moneyGlyphs(AnnotatedString("−RM 1,200.50")).map { it.text.removePrefix("​") }
        assertEquals(listOf("−RM ", "1", ",", "2", "0", "0", ".", "5", "0"), cells)
    }

    @Test
    fun rubberBandResistsAndNeverReachesTheEdge() {
        val dimension = 1000f
        assertEquals(0f, rubberBand(0f, dimension), 0f)
        // Near the edge the content follows at 55% of the finger.
        assertEquals(0.55f, rubberBand(1f, dimension), 0.001f)
        assertTrue(rubberBand(10_000f, dimension) < dimension)
        assertTrue(rubberBand(200f, dimension) < rubberBand(400f, dimension))
    }

    @Test
    fun rubberBandInverseRoundTrips() {
        val dimension = 900f
        listOf(1f, 40f, 250f, 1_600f).forEach { pull ->
            assertEquals(pull, rubberBandInverse(rubberBand(pull, dimension), dimension), pull * 0.001f)
        }
    }

    @Test
    fun stiffnessMatchesSwiftUIResponse() {
        // response 0.5 s → ω = 2π / 0.5 → k = ω².
        val omega = 2 * PI.toFloat() / 0.5f
        assertEquals(omega * omega, stiffnessFor(0.5f), 0.01f)
    }
}
