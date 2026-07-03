package com.subramanya.artha.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A person plus their net balance (positive = they owe the user, negative = user owes them). */
data class PersonWithNet(
    val person: Person,
    val netBalance: Double,
)

data class PeopleUiState(
    val people: List<PersonWithNet> = emptyList(),
)

/**
 * People list state. Net balances are computed in a SINGLE pass over the transaction log
 * (O(txns + people)) off the main thread — the screen previously recomputed every person's
 * balance with an O(people × txns) scan inside the composable on every recomposition.
 */
class PeopleViewModel(
    private val personRepository: PersonRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val state: StateFlow<PeopleUiState> = combine(
        personRepository.observeAll(),
        transactionRepository.observeAll(),
    ) { people, txns ->
        val netById = netBalancesByPerson(txns)
        PeopleUiState(
            people = people.map { PersonWithNet(it, netById[it.id] ?: 0.0) },
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

    fun upsert(person: Person) {
        viewModelScope.launch { personRepository.upsert(person) }
    }

    fun delete(person: Person) {
        viewModelScope.launch { personRepository.delete(person) }
    }

    /**
     * Net balance per person id in one pass. Mirrors PRD §7.17 (People = 1-to-1 lending):
     * LOAN_GIVEN/EXPENSE tagged with a person means they owe the user (+); LOAN_RECEIVED/INCOME
     * means the user owes them (−). GIFTs are intentionally EXCLUDED — a gift carries no repayment
     * expectation, so it never moves the owe/owed ledger (it still shows in the person's history).
     * A transaction tagged with several people contributes to each.
     */
    private fun netBalancesByPerson(transactions: List<Transaction>): Map<String, Double> {
        val net = HashMap<String, Double>()
        for (txn in transactions) {
            if (txn.peopleIds.isEmpty()) continue
            val delta = when (txn.type) {
                TransactionType.LOAN_GIVEN, TransactionType.EXPENSE -> txn.amount
                TransactionType.LOAN_RECEIVED, TransactionType.INCOME -> -txn.amount
                else -> 0.0 // GIFT_SENT / GIFT_RECEIVED / TRANSFER / INVESTMENT_* — not debt
            }
            if (delta == 0.0) continue
            for (personId in txn.peopleIds) {
                net[personId] = (net[personId] ?: 0.0) + delta
            }
        }
        return net
    }
}

class PeopleViewModelFactory(
    private val personRepository: PersonRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PeopleViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return PeopleViewModel(personRepository, transactionRepository) as T
    }
}
