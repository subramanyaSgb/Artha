package com.subramanya.artha.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Format helpers for human-friendly dates. We hand-format instead of using
 * java.time `DateTimeFormatter` to stay locale-stable across devices — the PRD
 * sample format is English-language "Thu, 21 May" regardless of system locale.
 */
object DateFormatter {

    fun todayShort(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        return shortDate(today)
    }

    /** Formats a date as "Thu, 21 May". */
    fun shortDate(date: LocalDate): String {
        val day = date.dayOfWeek.shortName
        val month = date.month.shortName
        return "$day, ${date.dayOfMonth} $month"
    }

    private val kotlinx.datetime.DayOfWeek.shortName: String
        get() = when (this) {
            kotlinx.datetime.DayOfWeek.MONDAY -> "Mon"
            kotlinx.datetime.DayOfWeek.TUESDAY -> "Tue"
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Wed"
            kotlinx.datetime.DayOfWeek.THURSDAY -> "Thu"
            kotlinx.datetime.DayOfWeek.FRIDAY -> "Fri"
            kotlinx.datetime.DayOfWeek.SATURDAY -> "Sat"
            kotlinx.datetime.DayOfWeek.SUNDAY -> "Sun"
            else -> ""
        }

    private val Month.shortName: String
        get() = when (this) {
            Month.JANUARY -> "Jan"
            Month.FEBRUARY -> "Feb"
            Month.MARCH -> "Mar"
            Month.APRIL -> "Apr"
            Month.MAY -> "May"
            Month.JUNE -> "Jun"
            Month.JULY -> "Jul"
            Month.AUGUST -> "Aug"
            Month.SEPTEMBER -> "Sep"
            Month.OCTOBER -> "Oct"
            Month.NOVEMBER -> "Nov"
            Month.DECEMBER -> "Dec"
            else -> ""
        }
}
