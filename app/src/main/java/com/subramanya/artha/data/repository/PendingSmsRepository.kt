package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.PendingSmsDao
import com.subramanya.artha.data.entity.PendingSmsEntity
import com.subramanya.artha.domain.model.PendingSms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read/write access to the SMS review queue ([PendingSmsEntity]). DAO returns Flows;
 * this maps entities ↔ domain [PendingSms].
 */
class PendingSmsRepository(private val dao: PendingSmsDao) {

    fun observeAll(): Flow<List<PendingSms>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun getById(id: String): PendingSms? = dao.getById(id)?.toDomain()

    /** Cheap duplicate guard: is a pending row already queued for this ref? */
    suspend fun existsByRef(refNo: String): Boolean = dao.countByRef(refNo) > 0

    suspend fun insert(row: PendingSms) = dao.insert(row.toEntity())

    suspend fun dismiss(id: String) = dao.deleteById(id)

    suspend fun clear() = dao.clear()

    private fun PendingSmsEntity.toDomain() = PendingSms(
        id = id,
        receivedAt = receivedAt,
        sender = sender,
        rawBody = rawBody,
        amount = amount,
        isDebit = direction.equals("DEBIT", ignoreCase = true),
        merchant = merchant,
        accountHint = accountHint,
        refNo = refNo,
        occurredAt = occurredAt,
        matchedAccountId = matchedAccountId,
        suggestedCategoryId = suggestedCategoryId,
        parseSource = parseSource,
    )

    private fun PendingSms.toEntity() = PendingSmsEntity(
        id = id,
        receivedAt = receivedAt,
        sender = sender,
        rawBody = rawBody,
        amount = amount,
        direction = if (isDebit) "DEBIT" else "CREDIT",
        merchant = merchant,
        accountHint = accountHint,
        refNo = refNo,
        occurredAt = occurredAt,
        matchedAccountId = matchedAccountId,
        suggestedCategoryId = suggestedCategoryId,
        parseSource = parseSource,
    )
}
