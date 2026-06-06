package com.subramanya.artha.ui.cards

import com.subramanya.artha.domain.model.CardWithBalance

enum class CardsView { ACTIVE, ARCHIVED }

/** Ordering of the card list. CUSTOM = the user's manual drag order (displayOrder). */
enum class CardSort { CUSTOM, OUTSTANDING_DESC, NAME_ASC }

data class CardsUiState(
    val view: CardsView = CardsView.ACTIVE,
    val isReorderMode: Boolean = false,
    val activeCards: List<CardWithBalance> = emptyList(),
    val archivedCards: List<CardWithBalance> = emptyList(),
    /** True until the first data emission, so the screen can show a skeleton instead of an empty flash. */
    val isLoading: Boolean = true,
    val sort: CardSort = CardSort.CUSTOM,
    /** When true, rows are grouped under their card-type sub-header (Credit / Debit / Prepaid). */
    val groupByType: Boolean = false,
) {
    val shownRows: List<CardWithBalance>
        get() = if (view == CardsView.ACTIVE) activeCards else archivedCards

    /** Manual long-press reorder only makes sense on the active list in custom, ungrouped order. */
    val canReorder: Boolean
        get() = view == CardsView.ACTIVE && sort == CardSort.CUSTOM && !groupByType
}
