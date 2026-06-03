package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.TransactionType

/**
 * Aggregates per-month "income in" vs "expense out" totals for the Dashboard's
 * This-Month strip. Pure Kotlin so it tests without Room.
 *
 * Counting rules (intentionally narrow — see Phase-1 PRD §4 "rules first"):
 *   INCOME counts as +income: INCOME, REFUND, CASHBACK, INTEREST, LOAN_RECEIVED, GIFT_RECEIVED
 *   EXPENSE counts as +expense: EXPENSE, LOAN_GIVEN, GIFT_SENT
 *
 * Excluded from both totals — these would double-count or misrepresent flows:
 *   TRANSFER, CARD_PAYMENT (money moves between user's own accounts/cards)
 *   INVESTMENT_BUY, INVESTMENT_SELL (Phase 2, but rule pre-stated here)
 *   ADJUSTMENT (manual reconciliation, sign of amount only updates the balance)
 */
data class MonthlyTotals(val income: Double, val expense: Double) {
    companion object {
        val ZERO = MonthlyTotals(0.0, 0.0)
    }
}

object MonthlyAggregator {

    private val INCOME_TYPES: Set<TransactionType> = setOf(
        TransactionType.INCOME,
        TransactionType.REFUND,
        TransactionType.CASHBACK,
        TransactionType.INTEREST,
        TransactionType.LOAN_RECEIVED,
        TransactionType.GIFT_RECEIVED,
    )

    private val EXPENSE_TYPES: Set<TransactionType> = setOf(
        TransactionType.EXPENSE,
        TransactionType.LOAN_GIVEN,
        TransactionType.GIFT_SENT,
    )

    /**
     * Caller is responsible for pre-filtering [transactions] to the time window.
     * This keeps the function pure and trivially testable.
     */
    fun aggregate(transactions: List<TransactionEntity>): MonthlyTotals {
        var income = 0.0
        var expense = 0.0
        for (txn in transactions) {
            if (txn.excludedFromExpenseTotal) continue // a rule asked to omit this from the totals
            when (txn.type) {
                in INCOME_TYPES -> income += txn.amount
                in EXPENSE_TYPES -> expense += txn.amount
                else -> Unit
            }
        }
        return MonthlyTotals(income, expense)
    }
}
