package com.subramanya.artha.ui.cards

import com.subramanya.artha.domain.model.CardWithBalance

enum class CardsView { ACTIVE, ARCHIVED }

data class CardsUiState(
    val view: CardsView = CardsView.ACTIVE,
    val isReorderMode: Boolean = false,
    val activeCards: List<CardWithBalance> = emptyList(),
    val archivedCards: List<CardWithBalance> = emptyList(),
) {
    val shownRows: List<CardWithBalance>
        get() = if (view == CardsView.ACTIVE) activeCards else archivedCards
}
