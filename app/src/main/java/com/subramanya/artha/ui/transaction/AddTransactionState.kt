package com.subramanya.artha.ui.transaction

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType

/** The three top-level tabs the user sees. Investment is Phase 2. */
enum class TransactionTab { EXPENSE, INCOME, TRANSFER }

/**
 * A source or destination selection in the From/To pickers. Keeps the display name
 * pre-resolved so the chip can render without re-querying. `isCreditCard` lets the
 * sheet auto-detect Transfer → CARD_PAYMENT without another lookup.
 */
data class FundsEndpoint(
    val kind: SourceKind,
    val id: String,
    val displayName: String,
    val isCreditCard: Boolean = false,
)

data class AddTransactionUiState(
    val tab: TransactionTab = TransactionTab.EXPENSE,
    val amountText: String = "",
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val source: FundsEndpoint? = null,
    val destination: FundsEndpoint? = null,
    val categoryId: String? = null,
    val categoryDisplay: String? = null,
    val subCategoryId: String? = null,
    val subCategoryDisplay: String? = null,
    val description: String = "",
    val paymentApp: PaymentApp = PaymentApp.OTHER,
    val peopleIds: Set<String> = emptySet(),
    val place: String = "",
    val tagIds: Set<String> = emptySet(),
    val receiptUri: String? = null,
    val notes: String = "",
    val showValidationErrors: Boolean = false,
    val isSaving: Boolean = false,
    val savedAndClose: Boolean = false,
) {
    val parsedAmount: Double?
        get() = amountText.toDoubleOrNull()?.takeIf { it > 0.0 }

    /**
     * Auto-detect CARD_PAYMENT on the Transfer tab when destination is a credit card.
     * Income/Expense always map 1:1.
     */
    val effectiveType: TransactionType
        get() = when (tab) {
            TransactionTab.EXPENSE -> TransactionType.EXPENSE
            TransactionTab.INCOME -> TransactionType.INCOME
            TransactionTab.TRANSFER ->
                if (destination?.isCreditCard == true) TransactionType.CARD_PAYMENT
                else TransactionType.TRANSFER
        }

    /** Whether the Transfer→Card_Payment hint chip should be shown. */
    val showCardPaymentHint: Boolean
        get() = tab == TransactionTab.TRANSFER && destination?.isCreditCard == true

    /** True only when all required fields validate. Drives Save-button enablement. */
    val isValid: Boolean
        get() {
            if (parsedAmount == null) return false
            if (source == null) return false
            if (description.isBlank()) return false
            return when (tab) {
                TransactionTab.EXPENSE, TransactionTab.INCOME -> categoryId != null
                TransactionTab.TRANSFER ->
                    destination != null && (source.kind != destination.kind || source.id != destination.id)
            }
        }
}
