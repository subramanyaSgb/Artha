package com.subramanya.artha.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Inclusive-start, inclusive-end window in epoch millis. End is exclusive only if explicitly noted. */
data class MillisRange(val startMillis: Long, val endMillis: Long)

/**
 * Friendly time windows used by Dashboard chips ("Today | This Week | This Month") and
 * by the Transactions filter row. All computations are anchored to the device's current
 * time zone so day rollovers feel natural to the user.
 */
enum class TimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME,
    ;

    fun toRange(now: Long = Clock.System.now().toEpochMilliseconds(), tz: TimeZone = TimeZone.currentSystemDefault()): MillisRange {
        val nowLdt: LocalDateTime = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz)
        val today: LocalDate = nowLdt.date

        // End-of-period (last millisecond of today / this week / this month), NOT `now`.
        // Using `now` makes a freshly-saved transaction whose `date` is even a few ms
        // after the chip selection fall outside the window and silently vanish from
        // "Today" — manifesting as a "Recent Activity doesn't update" bug.
        return when (this) {
            TODAY -> {
                val start = today.atStartOfDayIn(tz).toEpochMilliseconds()
                val endExclusive = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
                MillisRange(start, endExclusive - 1)
            }
            THIS_WEEK -> {
                // Week starts on Monday — Indian convention also follows ISO weeks.
                val daysSinceMonday = (today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber)
                val weekStart = today.minus(daysSinceMonday, DateTimeUnit.DAY)
                val start = weekStart.atStartOfDayIn(tz).toEpochMilliseconds()
                val nextWeekStart = weekStart.plus(7, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
                MillisRange(start, nextWeekStart - 1)
            }
            THIS_MONTH -> {
                val firstOfMonth = LocalDate(today.year, today.monthNumber, 1)
                val start = firstOfMonth.atStartOfDayIn(tz).toEpochMilliseconds()
                val nextMonth = if (today.monthNumber == 12) {
                    LocalDate(today.year + 1, 1, 1)
                } else {
                    LocalDate(today.year, today.monthNumber + 1, 1)
                }
                val endExclusive = nextMonth.atStartOfDayIn(tz).toEpochMilliseconds()
                MillisRange(start, endExclusive - 1)
            }
            ALL_TIME -> MillisRange(0L, Long.MAX_VALUE)
        }
    }
}

/** Returns a [MillisRange] covering the calendar month containing [reference]. End is `now` clamped to end-of-month. */
fun thisCalendarMonth(
    reference: Long = Clock.System.now().toEpochMilliseconds(),
    tz: TimeZone = TimeZone.currentSystemDefault(),
): MillisRange {
    val ldt = Instant.fromEpochMilliseconds(reference).toLocalDateTime(tz)
    val start = LocalDate(ldt.year, ldt.monthNumber, 1).atStartOfDayIn(tz).toEpochMilliseconds()
    // Next month's day 1 minus 1 ms = inclusive end of current month.
    val nextMonth = if (ldt.monthNumber == 12) LocalDate(ldt.year + 1, 1, 1) else LocalDate(ldt.year, ldt.monthNumber + 1, 1)
    val endExclusive = nextMonth.atStartOfDayIn(tz).toEpochMilliseconds()
    return MillisRange(start, endExclusive - 1)
}

private val DayOfWeek.isoDayNumber: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
        else -> 1
    }
