package com.spendwise.app.ui.botanical

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class BotanicalFormatTest {

    @Test
    fun formatsAmountsWithGroupingAndSen() {
        assertEquals("0.00", formatAmount(0))
        assertEquals("0.05", formatAmount(5))
        assertEquals("4,850.00", formatAmount(485_000))
        assertEquals("999,999,999.99", formatAmount(99_999_999_999))
        assertEquals("1,234.56", formatAmount(-123_456))
    }

    @Test
    fun signsUseTrueMinusAndOptionalPlus() {
        assertEquals("−RM 38.50", formatRm(-3_850))
        assertEquals("+RM 4,200.00", formatRm(420_000, plusSign = true))
        assertEquals("RM 0.00", formatRm(0))
    }

    @Test
    fun groupsThousands() {
        assertEquals("0", groupThousands(0))
        assertEquals("999", groupThousands(999))
        assertEquals("1,000", groupThousands(1_000))
        assertEquals("12,345,678", groupThousands(12_345_678))
    }

    @Test
    fun shortensLikeIntlCompactNotation() {
        assertEquals("999.99", shortAmount(99_999))
        assertEquals("12.35K", shortAmount(1_234_567))
        assertEquals("100K", shortAmount(10_000_000))
        assertEquals("888.89M", shortAmount(88_888_888_888))
        // Rounding up across a unit boundary promotes the unit instead of "1000K".
        assertEquals("1M", shortAmount(99_999_999))
        assertEquals("1B", shortAmount(99_999_999_999))
        assertEquals("12.35K", shortAmount(-1_234_567))
    }

    @Test
    fun datesReadLikeTheRedesign() {
        assertEquals("7 Sept 2026", dateLabel(LocalDate.of(2026, 9, 7)))
        assertEquals("1 Oct", shortDateLabel(LocalDate.of(2026, 10, 1)))
        assertEquals("September 2026", monthLabel(YearMonth.of(2026, 9)))
        assertEquals("Sep", monthShort(9))
        assertEquals("Sept", monthShortGb(9))
        val today = LocalDate.of(2026, 9, 26)
        assertEquals("Today", dayHeading(today, today))
        assertEquals("25 Sept 2026", dayHeading(today.minusDays(1), today))
    }
}
