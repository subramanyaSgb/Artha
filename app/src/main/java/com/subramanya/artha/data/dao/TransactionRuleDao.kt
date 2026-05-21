package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.TransactionRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionRuleDao {
    @Query("SELECT * FROM transaction_rules ORDER BY priority, name")
    fun observeAll(): Flow<List<TransactionRuleEntity>>

    @Query("SELECT * FROM transaction_rules WHERE is_active = 1 ORDER BY priority, name")
    fun observeActive(): Flow<List<TransactionRuleEntity>>

    @Query("SELECT * FROM transaction_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionRuleEntity?

    @Query("SELECT COUNT(*) FROM transaction_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(rules: List<TransactionRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: TransactionRuleEntity)

    @Update
    suspend fun update(rule: TransactionRuleEntity)

    @Delete
    suspend fun delete(rule: TransactionRuleEntity)
}
