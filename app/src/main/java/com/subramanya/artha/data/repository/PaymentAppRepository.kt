package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.PaymentAppDao
import com.subramanya.artha.data.db.seed.SeedPaymentApps
import com.subramanya.artha.data.entity.PaymentAppEntity
import com.subramanya.artha.domain.model.PaymentAppOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Reads/writes the payment-app catalogue (Phase 2 of configurable pick-lists). Pickers use
 * [observeVisible]; the manage-UI uses [observeAll]. Built-ins can be hidden but not deleted,
 * so a transaction or backup that references one always resolves a label via [labelFor].
 */
class PaymentAppRepository(private val dao: PaymentAppDao) {

    /** All entries incl. hidden (manage-UI). */
    fun observeAll(): Flow<List<PaymentAppOption>> =
        dao.observeAll().map { list -> list.map { it.toOption() } }

    /** Only entries that should appear in a picker (hidden ones removed). */
    fun observeVisible(): Flow<List<PaymentAppOption>> =
        dao.observeAll().map { list -> list.filterNot { it.isHidden }.map { it.toOption() } }

    /** Adds a user-defined app and returns its new id. */
    suspend fun addCustom(label: String): String {
        val id = UUID.randomUUID().toString()
        val order = (dao.count())
        dao.upsert(
            PaymentAppEntity(
                id = id,
                label = label.trim(),
                isBuiltin = false,
                isHidden = false,
                displayOrder = order,
            ),
        )
        return id
    }

    /** Hides/shows a built-in, or hides a custom one (manage-UI toggle). */
    suspend fun setHidden(id: String, hidden: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isHidden = hidden))
    }

    /** Permanently removes a CUSTOM entry. Built-ins are never deletable (only hideable). */
    suspend fun deleteCustom(id: String) {
        val existing = dao.getById(id) ?: return
        if (existing.isBuiltin) return
        dao.delete(existing)
    }

    /** Resolves an id to its label for display; falls back to the raw id if unknown. */
    suspend fun labelFor(id: String): String = dao.getById(id)?.label ?: id

    /** Ensures the catalogue is non-empty (defensive — the seeder/migration normally fill it). */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) dao.upsertAll(SeedPaymentApps.all())
    }

    private fun PaymentAppEntity.toOption() =
        PaymentAppOption(
            id = id,
            label = label,
            isBuiltin = isBuiltin,
            isHidden = isHidden,
            displayOrder = displayOrder,
        )
}
