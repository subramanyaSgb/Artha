package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Bulk-delete queries used ONLY by the restore path ([com.subramanya.artha.data.backup.BackupRepository]).
 *
 * Deletes run in child -> parent order (cross-refs first) so foreign-key constraints
 * never trip mid-wipe, even though most child rows also cascade. We delete explicitly
 * rather than via [androidx.room.RoomDatabase.clearAllTables] because clearAllTables
 * opens its own transaction and can't be nested inside the single restore transaction
 * that guarantees all-or-nothing atomicity.
 */
@Dao
interface BackupDao {
    @Query("DELETE FROM transaction_tags")
    suspend fun deleteAllTransactionTags()

    @Query("DELETE FROM transaction_people")
    suspend fun deleteAllTransactionPeople()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAllRecurringRules()

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAllSubscriptions()

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM transaction_rules")
    suspend fun deleteAllTransactionRules()

    @Query("DELETE FROM investments")
    suspend fun deleteAllInvestments()

    @Query("DELETE FROM insurances")
    suspend fun deleteAllInsurances()

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    /** Phase 2: delete all payment-app catalogue rows (for restore — re-inserted from backup). */
    @Query("DELETE FROM payment_app")
    suspend fun deleteAllPaymentApps()
}
