package com.subramanya.artha.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Transactions list state: filter / sort / select. The full transaction stream is
 * pulled from Room, then filtered, sorted and day-grouped in Kotlin. For Phase 1
 * (< 1k transactions expected) in-memory processing is plenty fast; revisit if the
 * working set grows.
 */
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    cardRepository: CardRepository,
    categoryRepository: CategoryRepository,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TransactionsFilter())
    private val sort = MutableStateFlow(TransactionSort.DATE_DESC)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val showDeleteConfirm = MutableStateFlow(false)

    /** Two-layer combine: data flows from Room collapse into [DataSnapshot]; UI-state flows into [UiSnapshot]. */
    val state: StateFlow<TransactionsUiState> = combine(
        combine(
            transactionRepository.observeAll(),
            accountRepository.observeActive(),
            cardRepository.observeActive(),
            categoryRepository.observeAll(),
        ) { txns, accounts, cards, categories ->
            DataSnapshot(txns, accounts, cards, categories)
        },
        combine(query, filter, sort, selectedIds, showDeleteConfirm) { q, f, s, sel, confirm ->
            UiSnapshot(q, f, s, sel, confirm)
        },
    ) { data, ui ->
        val filtered = applyFilters(data.transactions, ui.query, ui.filter)
        val sorted = applySort(filtered, ui.sort)
        val grouped = groupByDay(sorted)
        TransactionsUiState(
            query = ui.query,
            filter = ui.filter,
            sort = ui.sort,
            grouped = grouped,
            accounts = data.accounts,
            cards = data.cards,
            categories = data.categories,
            selectedIds = ui.selectedIds,
            showDeleteConfirm = ui.showDeleteConfirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    // ---------- mutators ----------

    fun onQueryChanged(value: String) = query.update { value }
    fun onFilterChanged(updater: (TransactionsFilter) -> TransactionsFilter) = filter.update(updater)
    fun onSortChanged(value: TransactionSort) = sort.update { value }
    fun clearFilters() {
        query.update { "" }
        filter.update { TransactionsFilter() }
    }

    fun toggleSelected(id: String) {
        selectedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        selectedIds.update { emptySet() }
    }

    fun requestDelete() {
        if (selectedIds.value.isEmpty()) return
        showDeleteConfirm.update { true }
    }

    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }

    fun confirmDelete() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) {
            showDeleteConfirm.update { false }
            return
        }
        viewModelScope.launch {
            transactionRepository.deleteByIds(ids)
            selectedIds.update { emptySet() }
            showDeleteConfirm.update { false }
        }
    }

    // ---------- pure helpers ----------

    private fun applyFilters(
        all: List<Transaction>,
        q: String,
        f: TransactionsFilter,
    ): List<Transaction> {
        val range = f.range.toRange(now = clock(), tz = timeZone)
        val needle = q.trim().lowercase()
        return all.filter { txn ->
            if (txn.date !in range.startMillis..range.endMillis) return@filter false
            if (f.typeFilter != null && txn.type != f.typeFilter) return@filter false
            if (f.accountId != null) {
                val touches =
                    (txn.sourceType == SourceKind.ACCOUNT && txn.sourceId == f.accountId) ||
                        (txn.destinationType == SourceKind.ACCOUNT && txn.destinationId == f.accountId)
                if (!touches) return@filter false
            }
            if (f.cardId != null) {
                val touches =
                    (txn.sourceType == SourceKind.CARD && txn.sourceId == f.cardId) ||
                        (txn.destinationType == SourceKind.CARD && txn.destinationId == f.cardId)
                if (!touches) return@filter false
            }
            if (f.categoryId != null &&
                txn.categoryId != f.categoryId &&
                txn.subCategoryId != f.categoryId
            ) {
                return@filter false
            }
            if (needle.isNotEmpty() &&
                !txn.description.lowercase().contains(needle) &&
                !(txn.notes?.lowercase()?.contains(needle) ?: false) &&
                !(txn.place?.lowercase()?.contains(needle) ?: false)
            ) {
                return@filter false
            }
            true
        }
    }

    private fun applySort(list: List<Transaction>, sort: TransactionSort): List<Transaction> = when (sort) {
        TransactionSort.DATE_DESC -> list.sortedWith(
            compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt },
        )
        TransactionSort.DATE_ASC -> list.sortedWith(
            compareBy<Transaction> { it.date }.thenBy { it.createdAt },
        )
        TransactionSort.AMOUNT_DESC -> list.sortedByDescending { it.amount }
        TransactionSort.AMOUNT_ASC -> list.sortedBy { it.amount }
    }

    private fun groupByDay(list: List<Transaction>): List<TransactionsGroup> {
        if (list.isEmpty()) return emptyList()
        val today = Instant.fromEpochMilliseconds(clock()).toLocalDateTime(timeZone).date
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        // Seed sections in iteration order so the chosen sort drives section order too.
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

    private data class DataSnapshot(
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val cards: List<Card>,
        val categories: List<Category>,
    )

    private data class UiSnapshot(
        val query: String,
        val filter: TransactionsFilter,
        val sort: TransactionSort,
        val selectedIds: Set<String>,
        val showDeleteConfirm: Boolean,
    )
}

class TransactionsViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return TransactionsViewModel(
            transactionRepository,
            accountRepository,
            cardRepository,
            categoryRepository,
        ) as T
    }
}
