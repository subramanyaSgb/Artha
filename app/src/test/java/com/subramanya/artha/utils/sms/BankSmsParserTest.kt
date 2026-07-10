package com.subramanya.artha.utils.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Samples mirror the shape of real Indian bank / UPI SMS. We assert the gate rejects
 * noise and that amount/direction/account/ref extraction holds across banks.
 */
class BankSmsParserTest {

    // A realistic "SMS arrived now" timestamp in mid-2026 — SMS about a 2026 transaction
    // arrives in 2026, so receivedAt is near the parsed dates (matters for the sanity clamp).
    private val now = 1_785_500_000_000L

    @Test
    fun hdfc_debit_upi() {
        val sms = "Sent Rs.434.00 From HDFC Bank A/C x1234 To HARISHKUMAR K On 03-07-26 Ref 540548535287. Not You? Call 18002586161"
        val p = BankSmsParser.parse(sms, now)
        assertNotNull(p)
        requireNotNull(p)
        assertEquals(434.0, p.amount!!, 0.001)
        assertTrue(p.isDebit)
        assertEquals("1234", p.accountHint)
        assertEquals("540548535287", p.refNo)
    }

    @Test
    fun sbi_credit() {
        val sms = "Dear Customer, Rs.5,000.00 credited to your A/c XX7890 on 02-Jul-26 by UPI Ref No 112233445566. -SBI"
        val p = BankSmsParser.parse(sms, now)
        requireNotNull(p)
        assertEquals(5000.0, p.amount!!, 0.001)
        assertFalse(p.isDebit)
        assertEquals("7890", p.accountHint)
        assertEquals("112233445566", p.refNo)
    }

    @Test
    fun icici_debit_at_merchant() {
        val sms = "INR 1,299.50 spent on ICICI Bank Card XX4321 at AMAZON on 01/07/2026. Avl Lmt INR 45,000."
        val p = BankSmsParser.parse(sms, now)
        requireNotNull(p)
        assertEquals(1299.5, p.amount!!, 0.001)
        assertTrue(p.isDebit)
        assertEquals("4321", p.accountHint)
    }

    @Test
    fun jupiter_debit() {
        val sms = "Rs 250 debited from Jupiter a/c 5678 to zomato via UPI ref 998877665544 on 03-07-2026"
        val p = BankSmsParser.parse(sms, now)
        requireNotNull(p)
        assertEquals(250.0, p.amount!!, 0.001)
        assertTrue(p.isDebit)
        assertEquals("5678", p.accountHint)
        assertEquals("998877665544", p.refNo)
    }

    private fun yearOf(millis: Long): Int =
        java.util.Calendar.getInstance().apply { timeInMillis = millis }.get(java.util.Calendar.YEAR)

    private fun monthOf(millis: Long): Int =
        java.util.Calendar.getInstance().apply { timeInMillis = millis }.get(java.util.Calendar.MONTH) + 1

    private fun dayOf(millis: Long): Int =
        java.util.Calendar.getInstance().apply { timeInMillis = millis }.get(java.util.Calendar.DAY_OF_MONTH)

    @Test
    fun date_two_digit_year_dash_resolves_to_2000s() {
        // "03-07-26" must be 03 Jul 2026 — not year 0026 (the greedy-yyyy bug).
        val p = BankSmsParser.parse("Sent Rs.434 From HDFC A/C x1234 On 03-07-26 Ref 540548535287", now)
        requireNotNull(p)
        assertEquals(2026, yearOf(p.occurredAt!!))
        assertEquals(7, monthOf(p.occurredAt!!))
        assertEquals(3, dayOf(p.occurredAt!!))
    }

    @Test
    fun date_named_month_two_digit_year() {
        val p = BankSmsParser.parse("Rs.5000 credited to A/c XX7890 on 02-Jul-26 UPI Ref No 112233445566", now)
        requireNotNull(p)
        assertEquals(2026, yearOf(p.occurredAt!!))
        assertEquals(7, monthOf(p.occurredAt!!))
        assertEquals(2, dayOf(p.occurredAt!!))
    }

    @Test
    fun date_four_digit_year_slash() {
        val p = BankSmsParser.parse("INR 1299 spent on Card XX4321 at AMAZON on 01/07/2026", now)
        requireNotNull(p)
        assertEquals(2026, yearOf(p.occurredAt!!))
        assertEquals(7, monthOf(p.occurredAt!!))
        assertEquals(1, dayOf(p.occurredAt!!))
    }

    @Test
    fun date_absent_falls_back_to_received_at() {
        val p = BankSmsParser.parse("Rs.250 debited from a/c 5678 to zomato via UPI ref 998877665544", now)
        requireNotNull(p)
        assertEquals(now, p.occurredAt)
    }

    @Test
    fun absurd_past_date_falls_back_to_received_at() {
        // A pre-2000 parse (or any mis-parse) must NOT stamp the transaction — fall back to now,
        // so it can never be buried ~decades in the past and hidden from every view.
        val p = BankSmsParser.parse("Rs.100 spent at SHOP on 01-07-1995 via card XX1234", now)
        requireNotNull(p)
        assertEquals(now, p.occurredAt)
    }

    @Test
    fun otp_is_rejected() {
        val sms = "123456 is your OTP for a transaction of Rs.999 on your HDFC Card. Do not share the OTP with anyone."
        assertFalse(BankSmsParser.isTransactional(sms))
        assertNull(BankSmsParser.parse(sms, now))
    }

    @Test
    fun promo_is_rejected() {
        val sms = "Get 10% cashback offer on your next spend of Rs.2000! Apply now https://bank.example/offer"
        assertFalse(BankSmsParser.isTransactional(sms))
    }

    @Test
    fun mandate_notice_is_rejected() {
        val sms = "Rs.499 will be debited from your A/c XX1234 on 05-Jul-26 towards Netflix e-mandate."
        assertFalse(BankSmsParser.isTransactional(sms))
    }

    @Test
    fun balance_only_is_rejected() {
        val sms = "Avl bal in your A/c XX1234 is Rs.12,345.67 as on 03-Jul-26. -Bank"
        // No debit/credit verb → not a posted transaction.
        assertFalse(BankSmsParser.isTransactional(sms))
    }
}
