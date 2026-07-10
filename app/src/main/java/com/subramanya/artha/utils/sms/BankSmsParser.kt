package com.subramanya.artha.utils.sms

import java.text.SimpleDateFormat
import java.util.Locale

/** Structured fields extracted from a bank transaction SMS. */
data class ParsedSms(
    val amount: Double?,
    val isDebit: Boolean,
    val merchant: String?,
    val accountHint: String?,
    val refNo: String?,
    val occurredAt: Long?,
)

/**
 * Regex-based parser for Indian bank transaction SMS.
 *
 * Rather than a per-bank branch, this uses ONE set of patterns that covers the shared
 * structure of HDFC / SBI / ICICI / Axis / Kotak / Jupiter / Yes Bank / most UPI alerts
 * (they all say "Rs X debited/credited … a/c XX1234 … ref …"). Anything this misses is
 * handled by the NIM text fallback in the receiver (hybrid parsing).
 *
 * [isTransactional] is the gate: it rejects OTPs, promos, and balance-only alerts so the
 * review queue isn't flooded. Only messages with an amount AND a debit/credit verb pass.
 */
object BankSmsParser {

    private val DEBIT_WORDS = listOf("debited", "debit", "spent", "withdrawn", "paid", "sent", "purchase", "deducted")
    private val CREDIT_WORDS = listOf("credited", "credit", "received", "deposited", "added", "refund")

    // Words that mark a message as NOT a posted transaction (OTP / mandate / promo).
    private val REJECT_WORDS = listOf(
        "otp", "one time password", "one-time password", "verification code", "wont be shared",
        "will be debited", "is requested", "e-mandate", "e mandate", "auto pay", "reward point",
        "offer", "cashback offer", "apply now", "click", "http://", "https://",
    )

    // Rs / INR / ₹ followed by an Indian-formatted number (1,234.56).
    private val AMOUNT_RE = Regex("""(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)

    // "a/c XX1234", "ac no XXXXXX1234", "card ending 1234", "account no. 1234"
    private val ACCOUNT_RE = Regex("""(?:a/?c|acct|account|card)\s*(?:no\.?|ending|number)?\s*[:x*]*(\d{3,4})\b""", RegexOption.IGNORE_CASE)

    // "ref no 123456789012", "UPI Ref 123...", "UTR 12345678", "txn id AB1234..."
    private val REF_RE = Regex("""(?:upi\s*ref(?:\s*no)?|ref(?:erence)?\s*(?:no\.?)?|utr|txn\s*id|transaction\s*id)\s*[:.\-]?\s*([A-Za-z0-9]{6,25})""", RegexOption.IGNORE_CASE)

    // Merchant/payee: "to JOHN DOE", "at BIGBAZAAR", "trf to ...", stopping at the next clause word.
    private val MERCHANT_RE = Regex("""\b(?:to|at|trf to|towards)\s+([A-Za-z0-9][A-Za-z0-9 &._@'-]{1,40}?)(?=\s+(?:on|ref|via|upi|a/?c|avl|bal|not|is|dt|date|,|\.|;|$))""", RegexOption.IGNORE_CASE)

    private val MONTHS = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    // Named-month date: "03-Jul-26", "03 Jul 2026", "03/Jul/2026"
    private val NAMED_DATE_RE = Regex("""\b(\d{1,2})[-/ ]([A-Za-z]{3})[-/ ](\d{2,4})\b""")
    // ISO date: "2026-07-03" (matched before the numeric d-m-y form)
    private val ISO_DATE_RE = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
    // Numeric day-month-year: "03-07-26", "03/07/2026"
    private val NUMERIC_DATE_RE = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})\b""")

    /** 2000-01-01 UTC in epoch millis — the sanity floor for a parsed transaction date. */
    private const val YEAR_2000_MILLIS = 946_684_800_000L

    /** True only for messages that look like a POSTED debit/credit — the review-queue gate. */
    fun isTransactional(body: String): Boolean {
        val lower = body.lowercase()
        if (REJECT_WORDS.any { it in lower }) return false
        val hasAmount = AMOUNT_RE.containsMatchIn(body)
        val hasVerb = DEBIT_WORDS.any { it in lower } || CREDIT_WORDS.any { it in lower }
        return hasAmount && hasVerb
    }

    /** Parse a transaction SMS. Returns null if it doesn't pass [isTransactional]. */
    fun parse(body: String, receivedAt: Long): ParsedSms? {
        if (!isTransactional(body)) return null
        val lower = body.lowercase()

        // Direction — debit verbs win when both appear (e.g. "debited … to VPA … credited to payee").
        val isDebit = DEBIT_WORDS.any { it in lower } || CREDIT_WORDS.none { it in lower }

        return ParsedSms(
            amount = AMOUNT_RE.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            isDebit = isDebit,
            merchant = MERCHANT_RE.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() },
            accountHint = ACCOUNT_RE.find(body)?.groupValues?.get(1),
            refNo = REF_RE.find(body)?.groupValues?.get(1),
            occurredAt = sanitizeDate(parseDate(body), receivedAt),
        )
    }

    /**
     * Defense-in-depth: a parsed date that lands before 2000 or more than a week after the
     * SMS arrived is almost certainly a mis-parse — fall back to [receivedAt] rather than
     * stamping a transaction with a date that would hide it from every current-time view.
     */
    private fun sanitizeDate(parsed: Long?, receivedAt: Long): Long {
        if (parsed == null) return receivedAt
        val upperBound = receivedAt + 7L * 86_400_000L
        return if (parsed in YEAR_2000_MILLIS..upperBound) parsed else receivedAt
    }

    /**
     * Parses the transaction date from the SMS body. Builds the date from integer
     * components rather than letting SimpleDateFormat's `yyyy` field greedily swallow a
     * 2-digit year — that bug turned "03-07-26" into year 0026, hiding the confirmed
     * transaction ~2000 years in the past. 2-digit years are normalised to the 2000s.
     */
    private fun parseDate(body: String): Long? {
        NAMED_DATE_RE.find(body)?.let { m ->
            MONTHS[m.groupValues[2].lowercase()]?.let { month ->
                return buildDate(normalizeYear(m.groupValues[3]), month, m.groupValues[1].toInt())
            }
        }
        ISO_DATE_RE.find(body)?.let { m ->
            return buildDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        NUMERIC_DATE_RE.find(body)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            if (month in 1..12 && day in 1..31) {
                return buildDate(normalizeYear(m.groupValues[3]), month, day)
            }
        }
        return null
    }

    /** Two-digit years mean this century (26 → 2026), four-digit years pass through. */
    private fun normalizeYear(token: String): Int {
        val n = token.toIntOrNull() ?: return 0
        return if (n < 100) 2000 + n else n
    }

    private fun buildDate(year: Int, month: Int, day: Int): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            .parse("%04d-%02d-%02d".format(year, month, day))?.time
    }.getOrNull()
}
