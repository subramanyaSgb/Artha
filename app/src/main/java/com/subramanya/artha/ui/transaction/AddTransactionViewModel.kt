package com.subramanya.artha.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State for the Add Transaction sheet.
 *
 * Save flow goes through [interceptSaveIfNeeded] → [commitSave]. The intercept hook
 * is the dedicated extension point for the spouse-prompt dialog (deferred); when it
 * lands, that logic will short-circuit here without surgery elsewhere.
 */
class AddTransactionViewModel(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionUiState())
    val state: StateFlow<AddTransactionUiState> = _state.asStateFlow()

    /** Live lists for the From/To pickers; merged into a single [FundsEndpoint] catalogue. */
    val fundsCatalogue: StateFlow<List<FundsEndpoint>> = combine(
        accountRepository.observeActive(),
        cardRepository.observeActive(),
    ) { accounts: List<Account>, cards: List<Card> ->
        buildList {
            accounts.forEach {
                add(FundsEndpoint(kind = SourceKind.ACCOUNT, id = it.id, displayName = it.name))
            }
            cards.forEach {
                add(
                    FundsEndpoint(
                        kind = SourceKind.CARD,
                        id = it.id,
                        displayName = it.name,
                        isCreditCard = it.type == com.subramanya.artha.data.entity.enums.CardType.CREDIT,
                    ),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<Category>> =
        categoryRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val people: StateFlow<List<Person>> =
        personRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<Tag>> =
        tagRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- field setters ----------

    fun onTabChanged(tab: TransactionTab) {
        // Switching tabs clears category/destination because they don't make sense across types.
        _state.update {
            it.copy(
                tab = tab,
                categoryId = null,
                categoryDisplay = null,
                subCategoryId = null,
                subCategoryDisplay = null,
                destination = if (tab == TransactionTab.TRANSFER) it.destination else null,
                showValidationErrors = false,
            )
        }
    }

    fun onAmountChanged(value: String) {
        val sanitised = value.filterIndexed { index, c ->
            c.isDigit() || (c == '.' && value.indexOf('.') == index)
        }
        _state.update { it.copy(amountText = sanitised) }
    }

    fun onDateTimeChanged(millis: Long) {
        _state.update { it.copy(dateTimeMillis = millis) }
    }

    fun onSourceSelected(endpoint: FundsEndpoint?) {
        _state.update { it.copy(source = endpoint) }
    }

    fun onDestinationSelected(endpoint: FundsEndpoint?) {
        _state.update { it.copy(destination = endpoint) }
    }

    fun onCategorySelected(category: Category) {
        _state.update {
            it.copy(
                categoryId = category.id,
                categoryDisplay = category.name,
                // Reset sub-category whenever parent changes — old selection may no longer be a child.
                subCategoryId = null,
                subCategoryDisplay = null,
            )
        }
    }

    fun onSubCategorySelected(category: Category?) {
        _state.update {
            it.copy(
                subCategoryId = category?.id,
                subCategoryDisplay = category?.name,
            )
        }
    }

    fun onDescriptionChanged(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun onPaymentAppChanged(app: PaymentApp) {
        _state.update { it.copy(paymentApp = app) }
    }

    fun togglePerson(personId: String) {
        _state.update {
            val updated = if (personId in it.peopleIds) it.peopleIds - personId else it.peopleIds + personId
            it.copy(peopleIds = updated)
        }
    }

    fun onPlaceChanged(value: String) {
        _state.update { it.copy(place = value) }
    }

    fun toggleTag(tagId: String) {
        _state.update {
            val updated = if (tagId in it.tagIds) it.tagIds - tagId else it.tagIds + tagId
            it.copy(tagIds = updated)
        }
    }

    fun onReceiptPicked(uri: String?) {
        _state.update { it.copy(receiptUri = uri) }
    }

    fun onNotesChanged(value: String) {
        _state.update { it.copy(notes = value) }
    }

    // ---------- inline adders ----------

    fun addPersonInline(name: String, relation: PersonRelation, autoSelect: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            personRepository.upsert(
                Person(
                    id = newId,
                    name = name.trim(),
                    relation = relation,
                    contact = null,
                    avatarUri = null,
                    createdAt = clock(),
                ),
            )
            if (autoSelect) {
                _state.update { it.copy(peopleIds = it.peopleIds + newId) }
            }
        }
    }

    fun addTagInline(name: String, autoSelect: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            tagRepository.upsert(
                Tag(id = newId, name = name.trim(), color = DEFAULT_TAG_COLOR),
            )
            if (autoSelect) {
                _state.update { it.copy(tagIds = it.tagIds + newId) }
            }
        }
    }

    // ---------- save ----------

    /**
     * Public save entry-point. Today it just commits; later it will branch out to the
     * spouse-prompt dialog when EXPENSE + a person with PersonRelation.SPOUSE is tagged
     * AND the user hasn't set a permanent default.
     */
    fun trySave() {
        val snapshot = _state.value
        if (!snapshot.isValid || snapshot.isSaving) {
            _state.update { it.copy(showValidationErrors = true) }
            return
        }
        if (interceptSaveIfNeeded(snapshot)) return
        commitSave(snapshot)
    }

    /** Extension point for the spouse-prompt dialog (Phase 1 follow-up). */
    @Suppress("UNUSED_PARAMETER")
    private fun interceptSaveIfNeeded(snapshot: AddTransactionUiState): Boolean = false

    private fun commitSave(snapshot: AddTransactionUiState) {
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = clock()
            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                type = snapshot.effectiveType,
                amount = snapshot.parsedAmount ?: 0.0,
                currency = "INR",
                date = snapshot.dateTimeMillis,
                description = snapshot.description.trim(),
                categoryId = snapshot.categoryId,
                subCategoryId = snapshot.subCategoryId,
                sourceType = snapshot.source!!.kind,
                sourceId = snapshot.source.id,
                destinationType = snapshot.destination?.kind,
                destinationId = snapshot.destination?.id,
                paymentApp = snapshot.paymentApp,
                place = snapshot.place.trim().takeIf { it.isNotBlank() },
                latitude = null,
                longitude = null,
                peopleIds = snapshot.peopleIds.toList(),
                tagIds = snapshot.tagIds.toList(),
                receiptUri = snapshot.receiptUri,
                notes = snapshot.notes.trim().takeIf { it.isNotBlank() },
                taxSection = null,
                recurringRuleId = null,
                isSplit = false,
                splitGroupId = null,
                source = TransactionSource.MANUAL,
                createdAt = now,
                updatedAt = now,
            )
            transactionRepository.save(txn)
            _state.update { it.copy(isSaving = false, savedAndClose = true) }
        }
    }

    /** Called by the host after consuming `savedAndClose`, in case the sheet is reused. */
    fun acknowledgeClose() {
        _state.value = AddTransactionUiState()
    }

    /** Returns the category-children list for the current categoryId, used by sub-cat picker. */
    fun childrenOf(parentId: String?, type: CategoryType): List<Category> {
        if (parentId == null) return emptyList()
        return allCategories.value.filter { it.parentId == parentId && it.type == type }
    }

    private companion object {
        private const val DEFAULT_TAG_COLOR: Long = 0xFF6366F1
    }
}

class AddTransactionViewModelFactory(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AddTransactionViewModel(
            accountRepository,
            cardRepository,
            categoryRepository,
            personRepository,
            tagRepository,
            transactionRepository,
        ) as T
    }
}
