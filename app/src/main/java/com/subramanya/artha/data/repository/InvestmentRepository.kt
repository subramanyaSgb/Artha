package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.dao.InvestmentDao
import com.subramanya.artha.data.dao.TransactionDao
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
            investments.map { entity ->
                val invested = BalanceCalculator.computeInvestmentInvested(entity.id, txns)
                val gain = entity.currentValue - invested
                val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
                InvestmentWithMetrics(
                    investment = entity.toDomain(),
                    investedAmount = invested,
                    absoluteGain = gain,
                    percentGain = pct,
                )
            }
        }

    fun observeInvested(id: String): Flow<Double> =
        transactionDao.observeAll().map { txns ->
            BalanceCalculator.computeInvestmentInvested(id, txns)
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
