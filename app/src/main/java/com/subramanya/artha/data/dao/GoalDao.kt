package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY is_achieved, target_date")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)
}
