package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.dao.AccountDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) {

    fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Account>> =
        accountDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Account>> =
        accountDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Account?> =
        accountDao.observeById(id).map { it?.toDomain() }

    fun observeCurrentBalance(accountId: String): Flow<Double> =
        combine(accountDao.observeById(accountId), transactionDao.observeAll()) { account, txns ->
            if (account == null) 0.0
            else BalanceCalculator.computeAccountBalance(account.openingBalance, accountId, txns)
        }

    fun observeActiveWithBalances(): Flow<List<AccountWithBalance>> =
        combine(accountDao.observeActive(), transactionDao.observeAll()) { accounts, txns ->
            accounts.map { entity ->
                AccountWithBalance(
                    account = entity.toDomain(),
                    currentBalance = BalanceCalculator.computeAccountBalance(
                        openingBalance = entity.openingBalance,
                        accountId = entity.id,
                        transactions = txns,
                    ),
                )
            }
        }

    suspend fun getById(id: String): Account? = accountDao.getById(id)?.toDomain()

    suspend fun upsert(account: Account) = accountDao.upsert(account.toEntity())

    suspend fun upsertAll(accounts: List<Account>) =
        accountDao.upsertAll(accounts.map(Account::toEntity))

    suspend fun update(account: Account) = accountDao.update(account.toEntity())

    suspend fun archive(account: Account) =
        accountDao.update(account.toEntity().copy(isArchived = true))

    suspend fun restore(account: Account) =
        accountDao.update(account.toEntity().copy(isArchived = false))

    suspend fun delete(account: Account) = accountDao.delete(account.toEntity())
}
