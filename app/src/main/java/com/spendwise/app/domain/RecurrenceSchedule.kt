package com.spendwise.app.domain

import java.time.LocalDate

/**
 * Pure calendar math for recurring rules. No Android or storage types —
 * everything here is unit-tested on the JVM.
 *
 * The anchor date is the user-picked first occurrence and is passed into
 * every step so day-of-month/day-of-year intent survives clamping: a rule
 * anchored on Jan 31 fires Feb 28 (or 29), then returns to Mar 31 — it never
 * ratchets down to the 28th permanently.
 */
object RecurrenceSchedule {

    /**
     * Safety valve for catch-up: at most this many occurrences are
     * materialized per rule per launch. A weekly rule left paused-less in a
     * drawer for two years shouldn't dump 100+ rows into the ledger; beyond
     * the cap the schedule simply continues from where the cap left off on
     * the next launch.
     */
    const val MAX_CATCH_UP = 36

    /** The occurrence after [current] for the given cadence. */
    fun nextOccurrence(current: LocalDate, cadence: RecurrenceCadence, anchor: LocalDate): LocalDate {
        return when (cadence) {
            RecurrenceCadence.Weekly -> current.plusWeeks(1)
            RecurrenceCadence.Monthly -> {
                val nextMonth = current.plusMonths(1)
                withClampedDay(nextMonth.year, nextMonth.monthValue, anchor.dayOfMonth)
            }
            RecurrenceCadence.Yearly -> {
                // Anchor month + day, clamped (Feb 29 anchors fire Feb 28 in
                // non-leap years).
                withClampedDay(current.year + 1, anchor.monthValue, anchor.dayOfMonth)
            }
        }
    }

    /**
     * All occurrences from [nextDue] (inclusive) through [today] (inclusive),
     * capped at [MAX_CATCH_UP], plus the nextDue that should be stored after
     * materializing them. When nothing is due, returns an empty list and the
     * unchanged [nextDue].
     */
    fun occurrencesDue(
        nextDue: LocalDate,
        today: LocalDate,
        cadence: RecurrenceCadence,
        anchor: LocalDate
    ): DueOccurrences {
        val due = mutableListOf<LocalDate>()
        var cursor = nextDue
        while (!cursor.isAfter(today) && due.size < MAX_CATCH_UP) {
            due.add(cursor)
            cursor = nextOccurrence(cursor, cadence, anchor)
        }
        return DueOccurrences(dates = due, newNextDue = cursor)
    }

    private fun withClampedDay(year: Int, month: Int, preferredDay: Int): LocalDate {
        val lastDay = LocalDate.of(year, month, 1).lengthOfMonth()
        return LocalDate.of(year, month, minOf(preferredDay, lastDay))
    }
}

data class DueOccurrences(
    val dates: List<java.time.LocalDate>,
    val newNextDue: java.time.LocalDate
)
