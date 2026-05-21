package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(startMillis, endMillis)
            .map { list -> list.map { it.toDomain() } }

    fun observeForAccountOrCard(id: String): Flow<List<Transaction>> =
        transactionDao.observeForSourceOrDestination(id)
            .map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.toDomain() }

    /** Hydrates peopleIds and tagIds from the cross-ref tables (single-row fetch). */
    suspend fun getById(id: String): Transaction? {
        val entity = transactionDao.getById(id) ?: return null
        val peopleIds = transactionDao.getPeopleIds(id)
        val tagIds = transactionDao.getTagIds(id)
        return entity.toDomain(peopleIds = peopleIds, tagIds = tagIds)
    }

    /**
     * Insert-or-update + replace the people/tag cross-refs atomically.
     * Use this from any flow that adds or edits a transaction.
     */
    suspend fun save(transaction: Transaction) {
        transactionDao.saveWithLinks(
            transaction = transaction.toEntity(),
            peopleIds = transaction.peopleIds,
            tagIds = transaction.tagIds,
        )
    }

    suspend fun delete(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction.toEntity())

    suspend fun deleteByIds(ids: List<String>) = transactionDao.deleteByIds(ids)
}
