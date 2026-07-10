package com.subramanya.artha.ui.share

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.db.seed.SeedPaymentApps
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PaymentAppRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.PaymentAppOption
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.utils.ReceiptData
import com.subramanya.artha.utils.ReceiptStore
import com.subramanya.artha.utils.UpiReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** A unified payment source entry shown in the account/card dropdown. */
sealed interface PaymentSource {
    val id: String
    val displayName: String

    data class BankAccount(val account: Account) : PaymentSource {
        override val id get() = account.id
        override val displayName get() = account.name
    }

    data class CreditCard(val card: Card) : PaymentSource {
        override val id get() = card.id
        override val displayName get() = card.name
    }
}

sealed interface ShareReceiptUiState {
    data object Scanning : ShareReceiptUiState

    /** Every field is editable; the user reviews then saves. */
    data class Parsed(
        val paymentSources: List<PaymentSource>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val paymentApps: List<PaymentAppOption>,
        val amountText: String,
        val dateTimeMillis: Long,
        val merchant: String,
        val description: String,
        val selectedAccountId: String?,
        val selectedCategoryId: String?,
        val selectedPaymentAppId: String,
        val upiRef: String?,
        val isSaving: Boolean = false,
    ) : ShareReceiptUiState

    data class Saved(val transactionId: String) : ShareReceiptUiState
    data class ScanError(val message: String) : ShareReceiptUiState
}

class ShareReceiptViewModel(
    private val imageUri: Uri,
    private val upiReceiptParser: UpiReceiptParser,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentAppRepository: PaymentAppRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareReceiptUiState>(ShareReceiptUiState.Scanning)
    val state: StateFlow<ShareReceiptUiState> = _state.asStateFlow()

    init { scan() }

    private fun scan() {
        _state.value = ShareReceiptUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val receipt = upiReceiptParser.parse(context, imageUri)
                val accounts = accountRepository.observeActive().first()
                val cards = cardRepository.observeActive().first()
                val categories = categoryRepository.observeAll().first()
                val paymentApps = paymentAppRepository.observeVisible().first()

                val sources: List<PaymentSource> =
                    accounts.map { PaymentSource.BankAccount(it) } +
                    cards.map { PaymentSource.CreditCard(it) }

                if (receipt == null) {
                    _state.value = ShareReceiptUiState.ScanError(
                        "Couldn't read this receipt. Check your connection and AI key, or add the transaction manually.",
                    )
                    return@launch
                }

                _state.value = ShareReceiptUiState.Parsed(
                    paymentSources = sources,
                    accounts = accounts,
                    categories = categories,
                    paymentApps = paymentApps,
                    amountText = receipt.amount?.let(::formatAmount).orEmpty(),
                    dateTimeMillis = receipt.dateTimeMillis ?: System.currentTimeMillis(),
                    merchant = receipt.merchant.orEmpty(),
                    description = receipt.description.orEmpty(),
                    selectedAccountId = matchSource(receipt, sources),
                    selectedCategoryId = matchCategory(receipt, categories),
                    selectedPaymentAppId = matchPaymentApp(receipt, paymentApps),
                    upiRef = receipt.upiRef,
                )
            } catch (e: Exception) {
                _state.value = ShareReceiptUiState.ScanError(friendlyError(e))
            }
        }
    }

    /** Turns raw network exceptions into plain-English guidance. */
    private fun friendlyError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            e is java.net.UnknownHostException ||
                "No address associated" in msg || "Unable to resolve host" in msg ->
                "Couldn't reach the AI service. If you're on mobile data, try switching to Wi-Fi — or tap Try again in a moment. You can also add the transaction manually."
            e is java.net.SocketTimeoutException || "timeout" in msg.lowercase() ->
                "The connection timed out. Check your internet and try again."
            "HTTP 401" in msg || "HTTP 403" in msg ->
                "The AI key was rejected. It may need to be updated."
            "HTTP 429" in msg ->
                "The AI service is busy (rate limit). Wait a moment and try again."
            else -> msg.ifBlank { "Failed to scan receipt." }
        }
    }

    fun updateAmount(text: String) = updateParsed { it.copy(amountText = text) }
    fun updateMerchant(text: String) = updateParsed { it.copy(merchant = text) }
    fun updateDescription(text: String) = updateParsed { it.copy(description = text) }
    fun updateDateTime(millis: Long) = updateParsed { it.copy(dateTimeMillis = millis) }
    fun selectAccount(id: String) = updateParsed { it.copy(selectedAccountId = id) }
    fun selectCategory(id: String) = updateParsed { it.copy(selectedCategoryId = id) }
    fun selectPaymentApp(id: String) = updateParsed { it.copy(selectedPaymentAppId = id) }

    fun save() {
        val current = _state.value as? ShareReceiptUiState.Parsed ?: return
        val sourceId = current.selectedAccountId ?: return
        if (current.isSaving) return
        val amount = current.amountText.replace(",", "").toDoubleOrNull() ?: return

        // Determine if the selected source is a card or bank account.
        val selectedSource = current.paymentSources.firstOrNull { it.id == sourceId }
        val sourceKind = if (selectedSource is PaymentSource.CreditCard) SourceKind.CARD else SourceKind.ACCOUNT

        updateParsed { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val savedReceiptUri = ReceiptStore.persist(context, imageUri)
            // Merchant maps to `description` — the SAME field the manual Add-Transaction form
            // labels "Merchant" (its primary title). The Description field + UPI ref go to notes.
            val notes = buildString {
                if (current.description.isNotBlank()) append(current.description.trim())
                current.upiRef?.let {
                    if (isNotEmpty()) append(" · ")
                    append("UPI Ref: $it")
                }
            }.takeIf { it.isNotBlank() }

            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.EXPENSE,
                amount = amount,
                currency = "INR",
                date = current.dateTimeMillis,
                description = current.merchant.ifBlank { current.description }.trim(),
                categoryId = current.selectedCategoryId,
                subCategoryId = null,
                sourceType = sourceKind,
                sourceId = sourceId,
                destinationType = null,
                destinationId = null,
                paymentApp = current.selectedPaymentAppId,
                place = null,
                latitude = null,
                longitude = null,
                peopleIds = emptyList(),
                tagIds = emptyList(),
                receiptUri = savedReceiptUri,
                notes = notes,
                taxSection = null,
                recurringRuleId = null,
                isSplit = false,
                splitGroupId = null,
                source = TransactionSource.OCR,
                createdAt = now,
                updatedAt = now,
            )
            transactionRepository.save(txn)
            _state.value = ShareReceiptUiState.Saved(txn.id)
        }
    }

    fun retry() = scan()

    private inline fun updateParsed(block: (ShareReceiptUiState.Parsed) -> ShareReceiptUiState.Parsed) {
        _state.update { if (it is ShareReceiptUiState.Parsed) block(it) else it }
    }

    /** Match the payer bank hint to an account or card by name overlap; else the first source. */
    private fun matchSource(receipt: ReceiptData, sources: List<PaymentSource>): String? {
        val hint = receipt.bankHint?.trim()
        if (hint != null) {
            sources.firstOrNull { src ->
                when (src) {
                    is PaymentSource.BankAccount -> {
                        val a = src.account
                        a.name.contains(hint, ignoreCase = true) ||
                            hint.contains(a.name, ignoreCase = true) ||
                            a.institution?.contains(hint, ignoreCase = true) == true
                    }
                    is PaymentSource.CreditCard -> {
                        val c = src.card
                        c.name.contains(hint, ignoreCase = true) ||
                            hint.contains(c.name, ignoreCase = true) ||
                            c.issuer?.contains(hint, ignoreCase = true) == true
                    }
                }
            }?.let { return it.id }
        }
        return sources.firstOrNull()?.id
    }

    /** Prefer the model's category hint, else fuzzy-match the merchant words to a category. */
    private fun matchCategory(receipt: ReceiptData, categories: List<Category>): String? {
        val terms = listOfNotNull(receipt.categoryHint, receipt.merchant, receipt.description)
            .flatMap { it.split(Regex("[\\s,./\\-_]+")) }
            .filter { it.length >= 3 }
        return categories.firstOrNull { cat ->
            terms.any { t -> cat.name.contains(t, ignoreCase = true) || t.contains(cat.name, ignoreCase = true) }
        }?.id
    }

    /** Map the model's payment-app string to a catalogue id (by id or label); else OTHER. */
    private fun matchPaymentApp(receipt: ReceiptData, apps: List<PaymentAppOption>): String {
        val hint = receipt.paymentAppHint?.trim().orEmpty()
        if (hint.isNotEmpty()) {
            apps.firstOrNull { it.id.equals(hint, ignoreCase = true) || it.label.equals(hint, ignoreCase = true) }
                ?.let { return it.id }
        }
        return apps.firstOrNull { it.id == SeedPaymentApps.DEFAULT_ID }?.id ?: SeedPaymentApps.DEFAULT_ID
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else "%.2f".format(amount)
}

class ShareReceiptViewModelFactory(
    private val imageUri: Uri,
    private val upiReceiptParser: UpiReceiptParser,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentAppRepository: PaymentAppRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ShareReceiptViewModel(
            imageUri,
            upiReceiptParser,
            accountRepository,
            cardRepository,
            categoryRepository,
            paymentAppRepository,
            transactionRepository,
            context,
        ) as T
}
