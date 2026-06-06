package com.subramanya.artha.domain.recurring

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.domain.model.RecurringRule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

/**
 * Pure, clock-injectable engine that materialises a recurring rule into a transaction
 * and computes the next fire time. No Room/Android dependencies — fully unit-testable.
 *
 * Design decisions:
 *  - Both autoConfirm and non-autoConfirm rules save the transaction with
 *    source=RECURRING. In a future version, non-autoConfirm could remain as a "pending"
 *    draft surfaced via a notification; for v1 both modes create real transactions (the
 *    user can always edit/delete from the Ledger).
 *  - nextRunDate is advanced once per firing regardless of how many periods were missed.
 *    If the worker was delayed (e.g. device was off), each rule fires exactly once for
 *    the current period and jumps to the next. This is intentional: the user manually
 *    logs any missed periods.
 */
object RecurringFireEngine {

    /**
     * Materialises [rule] into a [TransactionEntity] ready for Room insertion, and
     * returns the updated [nextRunDate] to store back on the rule.
     *
     * Returns null if the template can't be decoded (e.g. legacy plain-text rule).
     */
    fun fire(
        rule: RecurringRule,
        nowMillis: Long,
    ): FireResult? {
        val template = RecurringTemplateCodec.decode(rule.transactionTemplate) ?: return null
        val txn = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = template.type,
            amount = template.amount,
            currency = "INR",
            date = nowMillis,
            description = template.description,
            categoryId = template.categoryId,
            subCategoryId = null,
            sourceType = template.sourceType,
            sourceId = template.sourceId,
            destinationType = template.destinationType,
            destinationId = template.destinationId,
            paymentApp = template.paymentApp,
            place = null,
            latitude = null,
            longitude = null,
            receiptUri = null,
            notes = template.notes,
            taxSection = null,
            recurringRuleId = rule.id,
            isSplit = false,
            splitGroupId = null,
            source = TransactionSource.RECURRING,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )
        val nextRun = nextRunDate(rule, nowMillis)
        return FireResult(transaction = txn, nextRunDate = nextRun)
    }

    data class FireResult(
        val transaction: TransactionEntity,
        val nextRunDate: Long,
    )

    /**
     * Computes the next scheduled date after [nowMillis]. The rule's [RecurringRule.dayOfPeriod]
     * field controls day-of-month (MONTHLY) or day-of-week/1=Mon..7=Sun (WEEKLY); it is
     * ignored for DAILY and YEARLY.
     */
    fun nextRunDate(rule: RecurringRule, nowMillis: Long): Long {
        val tz = TimeZone.currentSystemDefault()
        val now = Instant.fromEpochMilliseconds(nowMillis)
        return when (rule.frequency) {
            com.subramanya.artha.data.entity.enums.RecurringFrequency.DAILY ->
                now.plus(1, DateTimeUnit.DAY, tz).toEpochMilliseconds()

            com.subramanya.artha.data.entity.enums.RecurringFrequency.WEEKLY ->
                now.plus(7, DateTimeUnit.DAY, tz).toEpochMilliseconds()

            com.subramanya.artha.data.entity.enums.RecurringFrequency.MONTHLY -> {
                val targetDay = rule.dayOfPeriod?.coerceIn(1, 28) ?: 1
                // Find the first day of next calendar month, then set the target day.
                val today = now.toLocalDateTime(tz).date
                val nextMonthYear = if (today.monthNumber == 12) today.year + 1 else today.year
                val nextMonthNum = if (today.monthNumber == 12) 1 else today.monthNumber + 1
                // Cap to 28 to avoid invalid dates in short months.
                LocalDate(nextMonthYear, nextMonthNum, targetDay.coerceIn(1, 28))
                    .atStartOfDayIn(tz)
                    .toEpochMilliseconds()
            }

            com.subramanya.artha.data.entity.enums.RecurringFrequency.YEARLY ->
                now.plus(1, DateTimeUnit.YEAR, tz).toEpochMilliseconds()
        }
    }
}
