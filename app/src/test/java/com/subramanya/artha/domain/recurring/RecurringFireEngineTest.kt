package com.subramanya.artha.domain.recurring

import com.subramanya.artha.data.entity.enums.RecurringFrequency
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.RecurringRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecurringFireEngineTest {

    // ---- template codec round-trip ----

    @Test fun `codec encode-decode round-trips all fields`() {
        val t = RecurringTemplate(
            amount = 20_000.0,
            type = TransactionType.EXPENSE,
            description = "Rent",
            sourceType = SourceKind.ACCOUNT,
            sourceId = "acct-1",
            destinationType = null,
            destinationId = null,
            categoryId = "cat-rent",
            paymentApp = "NETBANKING",
            notes = "Bangalore flat",
        )
        val decoded = RecurringTemplateCodec.decode(RecurringTemplateCodec.encode(t))
        assertNotNull(decoded)
        assertEquals(t, decoded)
    }

    @Test fun `codec decode returns null for blank json`() {
        assertNull(RecurringTemplateCodec.decode(""))
    }

    @Test fun `codec decode returns null for plain-text legacy template`() {
        assertNull(RecurringTemplateCodec.decode("Rent — Bangalore flat"))
    }

    // ---- fire ----

    @Test fun `fire produces transaction with RECURRING source and correct amount`() {
        val rule = rule(template = encode(amount = 5_000.0))
        val result = RecurringFireEngine.fire(rule, NOW_MILLIS)
        assertNotNull(result)
        assertEquals(5_000.0, result!!.transaction.amount, 0.0)
        assertEquals(TransactionSource.RECURRING, result.transaction.source)
        assertEquals(rule.id, result.transaction.recurringRuleId)
    }

    @Test fun `fire returns null when template is legacy plain-text`() {
        val rule = rule(template = "Rent")
        assertNull(RecurringFireEngine.fire(rule, NOW_MILLIS))
    }

    // ---- nextRunDate ----

    @Test fun `daily advances by one day`() {
        val rule = rule(freq = RecurringFrequency.DAILY)
        val next = RecurringFireEngine.nextRunDate(rule, NOW_MILLIS)
        val expected = NOW_MILLIS + ONE_DAY_MS
        val tolerance = ONE_DAY_MS / 10 // 10% tolerance for DST
        assert(next in (expected - tolerance)..(expected + tolerance)) {
            "Expected $expected ± $tolerance but was $next"
        }
    }

    @Test fun `weekly advances by seven days`() {
        val rule = rule(freq = RecurringFrequency.WEEKLY)
        val next = RecurringFireEngine.nextRunDate(rule, NOW_MILLIS)
        val expected = NOW_MILLIS + 7 * ONE_DAY_MS
        val tolerance = ONE_DAY_MS / 10
        assert(next in (expected - tolerance)..(expected + tolerance)) {
            "Expected $expected ± $tolerance but was $next"
        }
    }

    @Test fun `yearly advances by approximately one year`() {
        val rule = rule(freq = RecurringFrequency.YEARLY)
        val next = RecurringFireEngine.nextRunDate(rule, NOW_MILLIS)
        val approxYear = 365L * ONE_DAY_MS
        val expected = NOW_MILLIS + approxYear
        val tolerance = approxYear / 20 // 5% tolerance for leap year
        assert(next in (expected - tolerance)..(expected + tolerance)) {
            "Expected ~1 year forward, got $next"
        }
    }

    @Test fun `monthly next run is in the next calendar month`() {
        // NOW_MILLIS ≈ 2025-05-23. dayOfPeriod=1 → next run = 2025-06-01 (≈9 days away).
        val rule = rule(freq = RecurringFrequency.MONTHLY, dayOfPeriod = 1)
        val next = RecurringFireEngine.nextRunDate(rule, NOW_MILLIS)
        // Must be strictly in the future, and no more than 35 days away.
        assert(next > NOW_MILLIS) { "Next run must be in the future" }
        assert(next < NOW_MILLIS + 35 * ONE_DAY_MS) { "Next run should be < 35 days away, was $next" }
        // Firing NOW_MILLIS again should advance to the month after (July 1 for dayOfPeriod=1).
        val nextNext = RecurringFireEngine.nextRunDate(rule.copy(nextRunDate = next), next)
        assert(nextNext > next + 25 * ONE_DAY_MS) { "Second fire should be ~1 month later" }
        assert(nextNext < next + 35 * ONE_DAY_MS) { "Second fire should be < 35 days from first" }
    }

    // ---- firstRunDate (new-rule seeding) ----

    @Test fun `firstRunDate daily is the creation time`() {
        assertEquals(NOW_MILLIS, RecurringFireEngine.firstRunDate(RecurringFrequency.DAILY, null, NOW_MILLIS))
    }

    @Test fun `firstRunDate yearly is the creation time`() {
        assertEquals(NOW_MILLIS, RecurringFireEngine.firstRunDate(RecurringFrequency.YEARLY, 1, NOW_MILLIS))
    }

    @Test fun `firstRunDate monthly uses this month when the day has not passed`() {
        // NOW ≈ 2025-05-23; day-of-month 28 is still ahead → this month's 28th (a few days away).
        val first = RecurringFireEngine.firstRunDate(RecurringFrequency.MONTHLY, 28, NOW_MILLIS)
        assert(first > NOW_MILLIS) { "The 28th should be after the 23rd" }
        assert(first < NOW_MILLIS + 10 * ONE_DAY_MS) { "Should land within this month, was $first" }
    }

    @Test fun `firstRunDate monthly rolls to next month when the day already passed`() {
        // Day 1 has already passed on the 23rd → next month's 1st, which is after this month's 28th.
        val passed = RecurringFireEngine.firstRunDate(RecurringFrequency.MONTHLY, 1, NOW_MILLIS)
        val thisMonth = RecurringFireEngine.firstRunDate(RecurringFrequency.MONTHLY, 28, NOW_MILLIS)
        assert(passed > thisMonth) { "Next-month 1st must be after this-month 28th" }
    }

    @Test fun `firstRunDate weekly lands within the coming week`() {
        val first = RecurringFireEngine.firstRunDate(RecurringFrequency.WEEKLY, 3, NOW_MILLIS)
        assert(first in (NOW_MILLIS - ONE_DAY_MS)..(NOW_MILLIS + 7 * ONE_DAY_MS)) {
            "Weekly first run should be within the coming week, was $first"
        }
    }

    // ---- helpers ----

    private val NOW_MILLIS = 1_748_000_000_000L // ~2025-05-23, a fixed reference point
    private val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    private fun encode(amount: Double = 1_000.0): String = RecurringTemplateCodec.encode(
        RecurringTemplate(
            amount = amount,
            type = TransactionType.EXPENSE,
            description = "Test",
            sourceType = SourceKind.ACCOUNT,
            sourceId = "acct-1",
            destinationType = null,
            destinationId = null,
            categoryId = null,
            paymentApp = "OTHER",
            notes = null,
        ),
    )

    private fun rule(
        template: String = encode(),
        freq: RecurringFrequency = RecurringFrequency.MONTHLY,
        dayOfPeriod: Int? = 1,
    ) = RecurringRule(
        id = "rule-1",
        name = "Test rule",
        transactionTemplate = template,
        frequency = freq,
        dayOfPeriod = dayOfPeriod,
        nextRunDate = NOW_MILLIS - ONE_DAY_MS,
        lastRunDate = null,
        autoConfirm = true,
        isActive = true,
        createdAt = NOW_MILLIS - 7 * ONE_DAY_MS,
    )
}
