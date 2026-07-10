package com.subramanya.artha.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.data.entity.enums.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

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
    tagRepository: TagRepository,
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
            tagRepository.observeAll(),
        ) { txns, accounts, cards, categories, tags ->
            DataSnapshot(txns, accounts, cards, categories, tags)
        },
        combine(query, filter, sort, selectedIds, showDeleteConfirm) { q, f, s, sel, confirm ->
            UiSnapshot(q, f, s, sel, confirm)
        },
    ) { data, ui ->
        val filtered = applyFilters(data.transactions, ui.query, ui.filter)
        val sorted = applySort(filtered, ui.sort)
        val grouped = LedgerGrouping.groupByDay(sorted, clock(), timeZone)
        // Precompute here (off the UI thread via flowOn below) so the screen never re-derives
        // these on recomposition: the category lookup map once, and the In/Out/Net totals in a
        // single pass instead of re-summing the whole list on every keystroke/selection.
        val categoriesById = data.categories.associateBy { it.id }
        val rows = LedgerGrouping.flattenRows(grouped, categoriesById, ::signedDelta)
        var inSum = 0.0
        var outSum = 0.0
        for (txn in filtered) {
            when {
                txn.type in INCOME_LIKE -> inSum += txn.amount
                txn.type in OUTFLOW_LIKE -> outSum += txn.amount
            }
        }
        TransactionsUiState(
            query = ui.query,
            filter = ui.filter,
            sort = ui.sort,
            rows = rows,
            categoriesById = categoriesById,
            inSum = inSum,
            outSum = outSum,
            net = inSum - outSum,
            accounts = data.accounts,
            cards = data.cards,
            categories = data.categories,
            tags = data.tags,
            count = filtered.size,
            selectedIds = ui.selectedIds,
            showDeleteConfirm = ui.showDeleteConfirm,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

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
        val range = f.range.toRange(now = clock(), tz = timeZone, customStart = f.customDateStart, customEnd = f.customDateEnd)
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
            if (f.tagId != null && f.tagId !in txn.tagIds) return@filter false
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

    /** For day totals: positive for income-like, negative for outflow, zero otherwise. */
    private fun signedDelta(txn: Transaction): Double = when {
        txn.type in INCOME_LIKE -> txn.amount
        txn.type in OUTFLOW_LIKE -> -txn.amount
        else -> 0.0
    }

    private data class DataSnapshot(
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val cards: List<Card>,
        val categories: List<Category>,
        val tags: List<Tag>,
    )

    private data class UiSnapshot(
        val query: String,
        val filter: TransactionsFilter,
        val sort: TransactionSort,
        val selectedIds: Set<String>,
        val showDeleteConfirm: Boolean,
    )

    private companion object {
        /** Types that count as "money in" for the In/Out/Net strip (mirrors the row signing). */
        private val INCOME_LIKE = setOf(
            TransactionType.INCOME,
            TransactionType.REFUND,
            TransactionType.CASHBACK,
            TransactionType.INTEREST,
            TransactionType.LOAN_RECEIVED,
            TransactionType.GIFT_RECEIVED,
        )

        /** Types that count as "money out" — must mirror the row sign + day-sum + In/Out/Net strip
         *  so the three never disagree. Transfers / card payments / investment legs are net-zero here. */
        private val OUTFLOW_LIKE = setOf(
            TransactionType.EXPENSE,
            TransactionType.LOAN_GIVEN,
            TransactionType.GIFT_SENT,
        )
    }
}

class TransactionsViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
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
            tagRepository,
        ) as T
    }
}
