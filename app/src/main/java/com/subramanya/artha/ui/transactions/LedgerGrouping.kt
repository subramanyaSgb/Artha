package com.subramanya.artha.ui.transactions

import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.utils.DateFormatter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object LedgerGrouping {

    fun groupByDay(
        list: List<Transaction>,
        now: Long,
        timeZone: TimeZone,
    ): List<TransactionsGroup> {
        if (list.isEmpty()) return emptyList()
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val builders = LinkedHashMap<LocalDate, MutableList<Transaction>>()
        for (txn in list) {
            val day = Instant.fromEpochMilliseconds(txn.date).toLocalDateTime(timeZone).date
            builders.getOrPut(day) { ArrayList() }.add(txn)
        }
        return builders.map { (day, txns) ->
            val display = when (day) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> DateFormatter.shortDate(day)
            }
            TransactionsGroup(headerKey = day.toString(), headerDisplay = display, transactions = txns)
        }
    }

    fun flattenRows(
        groups: List<TransactionsGroup>,
        categoriesById: Map<String, Category>,
        signedDelta: (Transaction) -> Double,
    ): List<LedgerListItem> = buildList {
        groups.forEach { group ->
            val daySum = group.transactions.sumOf { signedDelta(it) }
            add(LedgerListItem.DayHeader(group.headerKey, group.headerDisplay, daySum))
            val lastIndex = group.transactions.lastIndex
            group.transactions.forEachIndexed { i, txn ->
                add(
                    LedgerListItem.Entry(
                        txn = txn,
                        category = txn.categoryId?.let { categoriesById[it] },
                        isFirstInDay = i == 0,
                        isLastInDay = i == lastIndex,
                    ),
                )
            }
        }
    }
}
