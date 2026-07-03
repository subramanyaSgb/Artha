package com.subramanya.artha.utils.upi

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/** Parsed fields extracted from a PhonePe payment receipt screenshot. */
data class UpiParsedReceipt(
    val amount: Double?,
    val merchantName: String?,
    val dateTimeMillis: Long?,
    val upiRef: String?,
    /** Raw string from "Paid from" label — used to fuzzy-match an account. */
    val sourceBankHint: String?,
    val paymentApp: String = "PHONEPE",
)

/**
 * Regex-based parser for PhonePe receipt OCR text.
 *
 * PhonePe receipts follow a consistent layout:
 *   PhonePe
 *   Paid successfully / Payment successful
 *   ₹500
 *   Paid to
 *   <merchant>
 *   <date> · <time>
 *   UPI Ref: <12-digit ref>
 *   Paid from
 *   <bank name>
 *
 * Returns null if the text doesn't look like a PhonePe receipt.
 */
object PhonePeReceiptParser {

    private val AMOUNT_RE = Regex("""₹\s*([\d,]+(?:\.\d{1,2})?)""")
    // Prefer UTR (short numeric) over Transaction ID (which may have letter prefixes)
    private val UTR_RE = Regex("""(?i)UTR\s*[:\-]?\s*(\d{8,20})""")
    private val UPI_REF_RE = Regex(
        """(?i)UPI\s+(?:Ref(?:erence)?(?:\s+No\.?)?|Transaction\s*ID)\s*[:\-]?\s*([A-Z]?\d{8,25})""",
    )

    // Label lines that precede the merchant name
    private val PAID_TO_LABELS = setOf("paid to", "to", "merchant", "beneficiary", "sent to")

    // Label lines that precede the source bank
    private val PAID_FROM_LABELS = setOf("paid from", "from", "debited from", "debit from", "bank")

    // Signals that this is a PhonePe receipt (case-insensitive)
    private val PHONEPE_SIGNALS = listOf(
        "phonepe",
        "paid successfully",
        "payment successful",
        "transaction successful",
        "paid to upi",
    )

    // Date pattern: "25 Jun 2026" optionally followed by time
    private val DATE_RE = Regex(
        """(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{4})""",
        RegexOption.IGNORE_CASE,
    )
    // 12-hour time: "9:14 PM"
    private val TIME_12_RE = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)
    // 24-hour time: "21:14"
    private val TIME_24_RE = Regex("""(\d{2}):(\d{2})(?!\s*[AP]M)""")

    fun parse(text: String): UpiParsedReceipt? {
        val lower = text.lowercase()
        if (PHONEPE_SIGNALS.none { lower.contains(it) }) return null

        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return UpiParsedReceipt(
            amount = extractAmount(text),
            merchantName = extractLineAfterLabel(lines, PAID_TO_LABELS),
            dateTimeMillis = extractDateTime(text, lines),
            upiRef = extractUpiRef(text),
            sourceBankHint = extractLineAfterLabel(lines, PAID_FROM_LABELS),
        )
    }

    private fun extractAmount(text: String): Double? =
        AMOUNT_RE.find(text)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

    private fun extractUpiRef(text: String): String? =
        UTR_RE.find(text)?.groupValues?.get(1)
            ?: UPI_REF_RE.find(text)?.groupValues?.get(1)

    private fun extractLineAfterLabel(lines: List<String>, labels: Set<String>): String? {
        for (i in lines.indices) {
            if (lines[i].lowercase() in labels) {
                val next = lines.getOrNull(i + 1) ?: continue
                val nextLower = next.lowercase()
                // Skip if the next line is itself a label
                if (nextLower in PAID_TO_LABELS || nextLower in PAID_FROM_LABELS) continue
                // Skip if it looks like an amount line
                if (next.startsWith("₹")) continue
                return next
            }
        }
        return null
    }

    private fun extractDateTime(text: String, lines: List<String>): Long? {
        val dateMatch = DATE_RE.find(text) ?: return null
        val day = dateMatch.groupValues[1].toIntOrNull() ?: return null
        val monthStr = dateMatch.groupValues[2]
        val year = dateMatch.groupValues[3].toIntOrNull() ?: return null

        val monthMap = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
            "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        )
        val month = monthMap[monthStr.lowercase()] ?: return null

        // Try to extract time from the region around the date — in PhonePe the time
        // appears BEFORE the date ("02:46 pm on 03 Jul 2026"), so search before and after.
        var hour = 0
        var minute = 0
        val searchStart = maxOf(0, dateMatch.range.first - 30)
        val timeText = text.substring(searchStart, minOf(text.length, dateMatch.range.last + 40))
        val time12 = TIME_12_RE.find(timeText)
        val time24 = TIME_24_RE.find(timeText)
        if (time12 != null) {
            hour = time12.groupValues[1].toInt()
            minute = time12.groupValues[2].toInt()
            if (time12.groupValues[3].uppercase() == "PM" && hour != 12) hour += 12
            if (time12.groupValues[3].uppercase() == "AM" && hour == 12) hour = 0
        } else if (time24 != null) {
            hour = time24.groupValues[1].toInt()
            minute = time24.groupValues[2].toInt()
        }

        return runCatching {
            val sdf = SimpleDateFormat("d M yyyy H m", Locale.US)
            sdf.parse("$day $month $year $hour $minute")?.time
        }.getOrNull()
    }
}
