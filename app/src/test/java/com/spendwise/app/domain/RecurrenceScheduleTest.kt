package com.spendwise.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceScheduleTest {

    private val jan31 = LocalDate.of(2026, 1, 31)

    @Test
    fun `weekly advances seven days`() {
        val next = RecurrenceSchedule.nextOccurrence(
            LocalDate.of(2026, 7, 10), RecurrenceCadence.Weekly, anchor = LocalDate.of(2026, 7, 10)
        )
        assertEquals(LocalDate.of(2026, 7, 17), next)
    }

    @Test
    fun `monthly clamps short months but recovers to the anchor day`() {
        // Jan 31 → Feb 28 (2026 is not a leap year)
        val feb = RecurrenceSchedule.nextOccurrence(jan31, RecurrenceCadence.Monthly, anchor = jan31)
        assertEquals(LocalDate.of(2026, 2, 28), feb)
        // Feb 28 → Mar 31, NOT Mar 28: the anchor day survives the clamp.
        val mar = RecurrenceSchedule.nextOccurrence(feb, RecurrenceCadence.Monthly, anchor = jan31)
        assertEquals(LocalDate.of(2026, 3, 31), mar)
    }

    @Test
    fun `monthly on a plain mid-month day just advances the month`() {
        val anchor = LocalDate.of(2026, 7, 15)
        val next = RecurrenceSchedule.nextOccurrence(anchor, RecurrenceCadence.Monthly, anchor)
        assertEquals(LocalDate.of(2026, 8, 15), next)
    }

    @Test
    fun `yearly clamps Feb 29 anchors in non-leap years`() {
        val anchor = LocalDate.of(2024, 2, 29)
        val next = RecurrenceSchedule.nextOccurrence(anchor, RecurrenceCadence.Yearly, anchor)
        assertEquals(LocalDate.of(2025, 2, 28), next)
    }

    @Test
    fun `occurrencesDue returns nothing when the rule is not due yet`() {
        val result = RecurrenceSchedule.occurrencesDue(
            nextDue = LocalDate.of(2026, 8, 1),
            today = LocalDate.of(2026, 7, 10),
            cadence = RecurrenceCadence.Monthly,
            anchor = LocalDate.of(2026, 8, 1)
        )
        assertTrue(result.dates.isEmpty())
        assertEquals(LocalDate.of(2026, 8, 1), result.newNextDue)
    }

    @Test
    fun `occurrencesDue backfills every missed occurrence`() {
        // Monthly on the 1st, last processed due date May 1, opened July 10:
        // May 1, Jun 1, Jul 1 are due; next stored due date is Aug 1.
        val result = RecurrenceSchedule.occurrencesDue(
            nextDue = LocalDate.of(2026, 5, 1),
            today = LocalDate.of(2026, 7, 10),
            cadence = RecurrenceCadence.Monthly,
            anchor = LocalDate.of(2026, 5, 1)
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1)
            ),
            result.dates
        )
        assertEquals(LocalDate.of(2026, 8, 1), result.newNextDue)
    }

    @Test
    fun `occurrencesDue includes an occurrence landing exactly today`() {
        val today = LocalDate.of(2026, 7, 10)
        val result = RecurrenceSchedule.occurrencesDue(
            nextDue = today,
            today = today,
            cadence = RecurrenceCadence.Weekly,
            anchor = today
        )
        assertEquals(listOf(today), result.dates)
        assertEquals(today.plusWeeks(1), result.newNextDue)
    }

    @Test
    fun `catch-up is capped so an abandoned rule cannot flood the ledger`() {
        // A weekly rule two years stale would owe ~104 occurrences.
        val result = RecurrenceSchedule.occurrencesDue(
            nextDue = LocalDate.of(2024, 7, 10),
            today = LocalDate.of(2026, 7, 10),
            cadence = RecurrenceCadence.Weekly,
            anchor = LocalDate.of(2024, 7, 10)
        )
        assertEquals(RecurrenceSchedule.MAX_CATCH_UP, result.dates.size)
        // The schedule continues from where the cap stopped, not from today.
        assertEquals(result.dates.last().plusWeeks(1), result.newNextDue)
    }

    @Test
    fun `firstOccurrenceOnOrAfter returns a future anchor unchanged`() {
        val anchor = LocalDate.of(2026, 9, 1)
        val result = RecurrenceSchedule.firstOccurrenceOnOrAfter(
            anchor, RecurrenceCadence.Monthly, lowerBound = LocalDate.of(2026, 7, 12)
        )
        assertEquals(anchor, result)
    }

    @Test
    fun `firstOccurrenceOnOrAfter fast-forwards a stale anchor without replaying`() {
        // Rule anchored 11 May, edited/resumed on 12 Jul: next fire is 11 Aug
        // (11 Jul already passed the lower bound of 13 Jul).
        val result = RecurrenceSchedule.firstOccurrenceOnOrAfter(
            anchor = LocalDate.of(2026, 5, 11),
            cadence = RecurrenceCadence.Monthly,
            lowerBound = LocalDate.of(2026, 7, 13)
        )
        assertEquals(LocalDate.of(2026, 8, 11), result)
    }

    @Test
    fun `firstOccurrenceOnOrAfter returns the lower bound when it lands on an occurrence`() {
        // Weekly from 10 Jul; lower bound exactly two weeks later.
        val result = RecurrenceSchedule.firstOccurrenceOnOrAfter(
            anchor = LocalDate.of(2026, 7, 10),
            cadence = RecurrenceCadence.Weekly,
            lowerBound = LocalDate.of(2026, 7, 24)
        )
        assertEquals(LocalDate.of(2026, 7, 24), result)
    }

    @Test
    fun `firstOccurrenceOnOrAfter respects month-end clamping while advancing`() {
        // Anchored 31 Jan, fast-forwarded into February: fires 28 Feb.
        val result = RecurrenceSchedule.firstOccurrenceOnOrAfter(
            anchor = jan31,
            cadence = RecurrenceCadence.Monthly,
            lowerBound = LocalDate.of(2026, 2, 1)
        )
        assertEquals(LocalDate.of(2026, 2, 28), result)
    }

    @Test
    fun `month-end backfill keeps each month on its own clamped day`() {
        // Anchored Jan 31, three months behind on Apr 15: Jan 31, Feb 28, Mar 31 due.
        val result = RecurrenceSchedule.occurrencesDue(
            nextDue = jan31,
            today = LocalDate.of(2026, 4, 15),
            cadence = RecurrenceCadence.Monthly,
            anchor = jan31
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31)
            ),
            result.dates
        )
        assertEquals(LocalDate.of(2026, 4, 30), result.newNextDue)
    }
}
