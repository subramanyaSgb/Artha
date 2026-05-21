package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY name")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE is_active = 1 ORDER BY name")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}
