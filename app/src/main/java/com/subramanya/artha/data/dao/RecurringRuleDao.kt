package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY is_active DESC, next_run_date")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE is_active = 1 AND next_run_date <= :cutoffMillis")
    suspend fun dueBy(cutoffMillis: Long): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)
}
