package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.CardTypeDao
import com.subramanya.artha.data.db.seed.SeedCardTypes
import com.subramanya.artha.data.entity.CardTypeEntity
import com.subramanya.artha.domain.model.CardTypeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CardTypeRepository(private val dao: CardTypeDao) {

    fun observeAll(): Flow<List<CardTypeOption>> =
        dao.observeAll().map { list -> list.map { it.toOption() } }

    fun observeVisible(): Flow<List<CardTypeOption>> =
        dao.observeAll().map { list -> list.filterNot { it.isHidden }.map { it.toOption() } }

    suspend fun addCustom(label: String): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(CardTypeEntity(id = id, label = label.trim(), isBuiltin = false, isHidden = false, displayOrder = dao.count()))
        return id
    }

    suspend fun setHidden(id: String, hidden: Boolean) {
        dao.getById(id)?.let { dao.update(it.copy(isHidden = hidden)) }
    }

    suspend fun deleteCustom(id: String) {
        val entry = dao.getById(id) ?: return
        if (!entry.isBuiltin) dao.delete(entry)
    }

    suspend fun labelFor(id: String): String = dao.getById(id)?.label ?: id

    /** Whether this id corresponds to the CREDIT built-in (used for card logic). */
    fun isCreditId(id: String): Boolean = id == SeedCardTypes.CREDIT_ID

    suspend fun seedIfEmpty() {
        if (dao.count() == 0) dao.upsertAll(SeedCardTypes.all())
    }

    private fun CardTypeEntity.toOption() =
        CardTypeOption(id, label, isBuiltin, isHidden, displayOrder)
}
