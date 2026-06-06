package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.InsuranceTypeDao
import com.subramanya.artha.data.db.seed.SeedInsuranceTypes
import com.subramanya.artha.data.entity.InsuranceTypeEntity
import com.subramanya.artha.domain.model.InsuranceTypeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class InsuranceTypeRepository(private val dao: InsuranceTypeDao) {

    fun observeAll(): Flow<List<InsuranceTypeOption>> =
        dao.observeAll().map { list -> list.map { it.toOption() } }

    fun observeVisible(): Flow<List<InsuranceTypeOption>> =
        dao.observeAll().map { list -> list.filterNot { it.isHidden }.map { it.toOption() } }

    suspend fun addCustom(label: String): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(InsuranceTypeEntity(id = id, label = label.trim(), isBuiltin = false, isHidden = false, displayOrder = dao.count()))
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

    suspend fun seedIfEmpty() {
        if (dao.count() == 0) dao.upsertAll(SeedInsuranceTypes.all())
    }

    private fun InsuranceTypeEntity.toOption() =
        InsuranceTypeOption(id, label, isBuiltin, isHidden, displayOrder)
}
