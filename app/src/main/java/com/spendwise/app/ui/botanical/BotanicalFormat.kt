package com.spendwise.app.ui.botanical

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Every date the app shows is Kuala Lumpur time, matching the data layer. */
val KL_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

fun todayKl(): LocalDate = LocalDate.now(KL_ZONE)

fun Long.toKlDate(): LocalDate = Instant.ofEpochMilli(this).atZone(KL_ZONE).toLocalDate()

fun LocalDate.startMillisKl(): Long = atStartOfDay(KL_ZONE).toInstant().toEpochMilli()

fun YearMonth.startMillisKl(): Long = atDay(1).startMillisKl()

/** "4,850.00" — absolute value, integer arithmetic so no locale can reorder separators. */
fun formatAmount(cents: Long): String {
    val abs = if (cents < 0) -cents else cents
    return "${groupThousands(abs / 100)}.${(abs % 100).toString().padStart(2, '0')}"
}

/** "RM 4,850.00", with "−" (U+2212) for negatives and "+" when [plusSign]. */
fun formatRm(cents: Long, plusSign: Boolean = false): String {
    val prefix = when {
        cents < 0 -> "−"
        plusSign -> "+"
        else -> ""
    }
    return "${prefix}RM ${formatAmount(cents)}"
}

fun groupThousands(value: Long): String {
    val digits = value.toString()
    if (digits.length <= 3) return digits
    val out = StringBuilder(digits.length + digits.length / 3)
    digits.forEachIndexed { index, c ->
        if (index > 0 && (digits.length - index) % 3 == 0) out.append(',')
        out.append(c)
    }
    return out.toString()
}

/**
 * Compact figure for tight summary spots: 888.89M, 1B, 12.35K — the
 * prototype's `Intl.NumberFormat('en-MY', {notation: 'compact',
 * maximumFractionDigits: 2})`. Absolute value; the caller adds sign and "RM".
 */
fun shortAmount(cents: Long): String {
    val ringgit = BigDecimal.valueOf(if (cents < 0) -cents else cents).movePointLeft(2)
    val units = listOf("" to 0, "K" to 3, "M" to 6, "B" to 9, "T" to 12)
    var index = units.indexOfLast { (_, power) -> ringgit >= BigDecimal.ONE.movePointRight(power) }
        .coerceAtLeast(0)
    while (true) {
        val (suffix, power) = units[index]
        val scaled = ringgit.movePointLeft(power).setScale(2, RoundingMode.HALF_UP)
        // 999,999 rounds to "1000K": promote to the next unit instead.
        if (index < units.lastIndex && power > 0 && scaled >= BigDecimal(1000)) {
            index++
            continue
        }
        if (index == 0 && scaled >= BigDecimal(1000)) {
            index++
            continue
        }
        return scaled.stripTrailingZeros().toPlainString() + suffix
    }
}

private val MONTH_LONG = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

// en-GB abbreviations (dates in prose and headings) and en abbreviations
// (compact chart and calendar labels) differ only for September, exactly as
// the prototype's two Intl locales did.
private val MONTH_SHORT_GB = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"
)
private val MONTH_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** "September 2026" */
fun monthLabel(month: YearMonth): String = "${MONTH_LONG[month.monthValue - 1]} ${month.year}"

/** "Sep" — charts, calendar and month grid. */
fun monthShort(month: Int): String = MONTH_SHORT[month - 1]

/** "Sept" — prose ("↑ 15% vs Sept"). */
fun monthShortGb(month: Int): String = MONTH_SHORT_GB[month - 1]

/** "7 Sept 2026" */
fun dateLabel(date: LocalDate): String =
    "${date.dayOfMonth} ${MONTH_SHORT_GB[date.monthValue - 1]} ${date.year}"

/** "7 Sept" */
fun shortDateLabel(date: LocalDate): String =
    "${date.dayOfMonth} ${MONTH_SHORT_GB[date.monthValue - 1]}"

/** Day heading in lists: "Today" or "7 Sept 2026". */
fun dayHeading(date: LocalDate, today: LocalDate = todayKl()): String =
    if (date == today) "Today" else dateLabel(date)
