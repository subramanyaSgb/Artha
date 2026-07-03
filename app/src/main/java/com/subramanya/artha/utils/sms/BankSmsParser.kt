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

    // Dates seen in bank SMS: 03-Jul-26, 03/07/2026, 03-07-26, 2026-07-03
    private val DATE_RES = listOf(
        Regex("""\b(\d{1,2}[-/][A-Za-z]{3}[-/]\d{2,4})\b"""),
        Regex("""\b(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\b"""),
        Regex("""\b(\d{4}-\d{2}-\d{2})\b"""),
    )
    private val DATE_FORMATS = listOf(
        "dd-MMM-yy", "dd-MMM-yyyy", "dd/MM/yyyy", "dd/MM/yy", "dd-MM-yyyy", "dd-MM-yy", "yyyy-MM-dd",
    )

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
            occurredAt = parseDate(body) ?: receivedAt,
        )
    }

    private fun parseDate(body: String): Long? {
        for (re in DATE_RES) {
            val hit = re.find(body)?.groupValues?.get(1) ?: continue
            for (fmt in DATE_FORMATS) {
                runCatching {
                    SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }.parse(hit)?.time
                }.getOrNull()?.let { return it }
            }
        }
        return null
    }
}
