package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.dao.InvestmentDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.entity.InvestmentEntity
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.InvestmentWithMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class InvestmentRepository(
    private val investmentDao: InvestmentDao,
    private val transactionDao: TransactionDao,
) {

    fun observeAll(): Flow<List<Investment>> =
        investmentDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Investment>> =
        investmentDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Investment>> =
        investmentDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Investment?> =
        investmentDao.observeById(id).map { it?.toDomain() }

    fun observeActiveWithMetrics(): Flow<List<InvestmentWithMetrics>> =
        combine(investmentDao.observeActive(), transactionDao.observeAll()) { investments, txns ->
            investments.map { entity -> metricsFor(entity, txns) }
        }

    /**
     * Map of EVERY investment id (active AND archived) → its computed per-mode value
     * (MARKET → manual currentValue, DERIVED → contributions + posted interest).
     *
     * Single source of truth for "what an investment is worth" so aggregate consumers
     * (net worth, goals, reports) never re-implement the formula or sum the stale raw
     * `currentValue`. Covers all investments because consumers differ in scope —
     * dashboard/reports want active-only, goals filter by linked ids, search shows all —
     * so each consumer filters the map by the ids it cares about.
     */
    fun observeValuesByInvestmentId(): Flow<Map<String, Double>> =
        combine(investmentDao.observeAll(), transactionDao.observeAll()) { investments, txns ->
            investments.associate { entity -> entity.id to valueFor(entity, txns) }
        }

    /** The one place the per-mode value formula is applied to an entity. */
    private fun valueFor(entity: InvestmentEntity, txns: List<TransactionEntity>): Double =
        BalanceCalculator.computeInvestmentValue(
            entity.valuationMode,
            entity.currentValue,
            entity.openingContribution,
            entity.id,
            txns,
        )

    /** Full metric bundle for one entity. Reuses [valueFor] so value math lives once. */
    private fun metricsFor(entity: InvestmentEntity, txns: List<TransactionEntity>): InvestmentWithMetrics {
        val invested =
            BalanceCalculator.computeInvestmentInvested(entity.id, txns, entity.openingContribution)
        val value = valueFor(entity, txns)
        val gain = value - invested
        val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
        return InvestmentWithMetrics(
            investment = entity.toDomain(),
            investedAmount = invested,
            value = value,
            absoluteGain = gain,
            percentGain = pct,
        )
    }

    fun observeInvested(id: String): Flow<Double> =
        combine(investmentDao.observeById(id), transactionDao.observeAll()) { entity, txns ->
            BalanceCalculator.computeInvestmentInvested(id, txns, entity?.openingContribution ?: 0.0)
        }

    suspend fun getById(id: String): Investment? = investmentDao.getById(id)?.toDomain()
    suspend fun findByLinkedInsurance(insuranceId: String): Investment? =
        investmentDao.findByLinkedInsurance(insuranceId)?.toDomain()

    suspend fun upsert(investment: Investment) = investmentDao.upsert(investment.toEntity())
    suspend fun update(investment: Investment) = investmentDao.update(investment.toEntity())

    suspend fun archive(investment: Investment) =
        investmentDao.update(investment.toEntity().copy(isArchived = true))

    suspend fun restore(investment: Investment) =
        investmentDao.update(investment.toEntity().copy(isArchived = false))

    suspend fun delete(investment: Investment) = investmentDao.delete(investment.toEntity())
}
