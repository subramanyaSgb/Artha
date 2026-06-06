package com.subramanya.artha.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.R
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.CardWithBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardsViewModel(
    private val cardRepository: CardRepository,
) : ViewModel() {

    private val view = MutableStateFlow(CardsView.ACTIVE)
    private val reorderMode = MutableStateFlow(false)
    private val sort = MutableStateFlow(CardSort.CUSTOM)
    private val groupByType = MutableStateFlow(false)

    private val message = MutableStateFlow<Int?>(null)
    val toastMessage: StateFlow<Int?> = message.asStateFlow()
    fun consumeToast() = message.update { null }

    private data class UiBag(
        val view: CardsView,
        val reorder: Boolean,
        val sort: CardSort,
        val group: Boolean,
    )

    val state: StateFlow<CardsUiState> = combine(
        cardRepository.observeActiveWithBalances(),
        cardRepository.observeArchived(),
        combine(view, reorderMode, sort, groupByType) { v, r, s, g -> UiBag(v, r, s, g) },
    ) { active, archived, ui ->
        // Archived rows surface a frozen "0" outstanding — they're history, not live.
        val archivedWithBalance = archived.map { CardWithBalance(it, currentOutstanding = 0.0) }
        val canReorder = ui.view == CardsView.ACTIVE && ui.sort == CardSort.CUSTOM && !ui.group
        CardsUiState(
            view = ui.view,
            isReorderMode = ui.reorder && canReorder,
            activeCards = order(active, ui.sort, ui.group),
            archivedCards = order(archivedWithBalance, ui.sort, ui.group),
            isLoading = false,
            sort = ui.sort,
            groupByType = ui.group,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsUiState())

    /** Apply the chosen sort, then (if grouping) a stable secondary sort by type. CUSTOM keeps the
     *  DAO's displayOrder. */
    private fun order(list: List<CardWithBalance>, sort: CardSort, group: Boolean): List<CardWithBalance> {
        val sorted = when (sort) {
            CardSort.CUSTOM -> list
            CardSort.OUTSTANDING_DESC -> list.sortedByDescending { it.currentOutstanding }
            CardSort.NAME_ASC -> list.sortedBy { it.card.name.lowercase() }
        }
        return if (group) sorted.sortedBy { it.card.type.lowercase() } else sorted
    }

    fun setSort(value: CardSort) {
        sort.update { value }
        if (value != CardSort.CUSTOM) reorderMode.update { false }
    }

    fun toggleGroupByType() {
        groupByType.update { !it }
        reorderMode.update { false }
    }

    fun showActive() = view.update { CardsView.ACTIVE }
    fun showArchived() {
        view.update { CardsView.ARCHIVED }
        reorderMode.update { false }
    }

    fun enterReorderMode() {
        if (view.value == CardsView.ACTIVE && sort.value == CardSort.CUSTOM && !groupByType.value) {
            reorderMode.update { true }
        }
    }
    fun exitReorderMode() = reorderMode.update { false }

    fun archive(card: Card) = viewModelScope.launch { cardRepository.archive(card) }
    fun restore(card: Card) = viewModelScope.launch { cardRepository.restore(card) }
    fun delete(card: Card) = viewModelScope.launch {
        // Archive instead of orphaning referenced transactions (mirrors the detail-screen guard).
        if (cardRepository.hasReferencingTransactions(card.id)) {
            cardRepository.archive(card)
            message.update { R.string.entity_delete_archived_instead }
        } else {
            cardRepository.delete(card)
        }
    }

    fun moveUp(card: Card) = moveBy(card, -1)
    fun moveDown(card: Card) = moveBy(card, +1)

    private fun moveBy(card: Card, delta: Int) {
        val snapshot = state.value.activeCards.map { it.card }
        val index = snapshot.indexOfFirst { it.id == card.id }
        if (index < 0) return
        val targetIndex = index + delta
        if (targetIndex !in snapshot.indices) return
        val a = snapshot[index]
        val b = snapshot[targetIndex]
        viewModelScope.launch {
            cardRepository.update(a.copy(displayOrder = b.displayOrder))
            cardRepository.update(b.copy(displayOrder = a.displayOrder))
        }
    }
}

class CardsViewModelFactory(
    private val cardRepository: CardRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CardsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return CardsViewModel(cardRepository) as T
    }
}
