package com.spendwise.app.ui.botanical

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BotanicalCalendarTest {

    @Test
    fun monthStartingSaturdayGetsSixWeeks() {
        // August 2026 starts on a Saturday: six leading blanks, 31 days, 42 cells.
        val cells = calendarCells(YearMonth.of(2026, 8))
        assertEquals(42, cells.size)
        assertEquals(6, cells.indexOfFirst { it != null })
        assertEquals(LocalDate.of(2026, 8, 31), cells[36])
        assertNull(cells.last())
        assertEquals(31, cells.count { it != null })
    }

    @Test
    fun fourWeekFebruaryHasNoBlanks() {
        // February 2026 starts on a Sunday and has 28 days.
        val cells = calendarCells(YearMonth.of(2026, 2))
        assertEquals(28, cells.size)
        assertEquals(LocalDate.of(2026, 2, 1), cells.first())
    }

    @Test
    fun changingMonthKeepsTheDayWhereItCan() {
        val jan31 = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), clampToMonth(jan31, YearMonth.of(2026, 2), maxDate = null))
        assertEquals(LocalDate.of(2028, 2, 29), clampToMonth(jan31, YearMonth.of(2028, 2), maxDate = null))
        assertEquals(LocalDate.of(2026, 3, 31), clampToMonth(jan31, YearMonth.of(2026, 3), maxDate = null))
    }

    @Test
    fun neverMovesPastTheLatestAllowedDate() {
        val today = LocalDate.of(2026, 9, 26)
        assertEquals(today, clampToMonth(LocalDate.of(2026, 8, 30), YearMonth.of(2026, 9), today))
        assertEquals(today, clampToMonth(LocalDate.of(2026, 8, 30), YearMonth.of(2026, 12), today))
        assertEquals(LocalDate.of(2026, 9, 3), clampToMonth(LocalDate.of(2026, 8, 3), YearMonth.of(2026, 9), today))
    }
}
