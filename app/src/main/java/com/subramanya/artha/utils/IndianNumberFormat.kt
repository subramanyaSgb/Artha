package com.subramanya.artha.utils

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Indian-style number formatting. Groups in 2,2,3 (e.g. 1,00,000 not 100,000) and
 * always prefixes the INR symbol — `Rs.` / `INR` are forbidden per CLAUDE.md.
 *
 * Whole rupees are shown without trailing `.00`; non-whole amounts keep two
 * decimal places. The sign moves outside the symbol: `-₹1,234.50`.
 *
 * Examples:
 *   100         → ₹100
 *   1_000       → ₹1,000
 *   100_000     → ₹1,00,000
 *   12_34_567.5 → ₹12,34,567.50
 */
object IndianNumberFormat {

    private const val INR_SYMBOL: String = "₹"

    /** Default display: hides `.00` on whole rupees, shows two decimals otherwise. */
    fun format(amount: Double): String = format(amount, alwaysShowDecimals = false)

    /** Use when you need consistent column widths regardless of whether amount is whole. */
    fun formatWithDecimals(amount: Double): String = format(amount, alwaysShowDecimals = true)

    private fun format(amount: Double, alwaysShowDecimals: Boolean): String {
        val isNegative = amount < 0.0
        val abs = abs(amount)
        val cents = (abs * 100.0).roundToLong()
        val whole = cents / 100L
        val fraction = (cents % 100L).toInt()

        val groupedWhole = groupIndian(whole)
        val tail = when {
            alwaysShowDecimals || fraction != 0 -> ".%02d".format(fraction)
            else -> ""
        }
        val signPrefix = if (isNegative) "-" else ""
        return "$signPrefix$INR_SYMBOL$groupedWhole$tail"
    }

    /** Public for callers that want only the grouped integer part (e.g., chips/tabs). */
    fun groupIndian(value: Long): String {
        if (value < 0L) return "-" + groupIndian(-value)
        val digits = value.toString()
        if (digits.length <= 3) return digits

        val lastThree = digits.substring(digits.length - 3)
        val remaining = digits.substring(0, digits.length - 3)
        // Walk the remaining digits in pairs from the right and join with commas.
        val grouped = StringBuilder()
        var i = remaining.length
        while (i > 2) {
            grouped.insert(0, "," + remaining.substring(i - 2, i))
            i -= 2
        }
        grouped.insert(0, remaining.substring(0, i))
        return "$grouped,$lastThree"
    }
}
