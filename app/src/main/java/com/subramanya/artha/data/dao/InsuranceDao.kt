package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.InsuranceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsuranceDao {
    @Query("SELECT * FROM insurances ORDER BY name")
    fun observeAll(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurances WHERE is_archived = 0 ORDER BY type, name")
    fun observeActive(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurances WHERE is_archived = 1 ORDER BY name")
    fun observeArchived(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurances WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<InsuranceEntity?>

    @Query("SELECT * FROM insurances WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InsuranceEntity?

    @Query(
        "SELECT * FROM insurances " +
            "WHERE is_archived = 0 AND next_premium_date IS NOT NULL " +
            "AND next_premium_date <= :cutoffMillis " +
            "ORDER BY next_premium_date",
    )
    fun observeDueWithin(cutoffMillis: Long): Flow<List<InsuranceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(insurance: InsuranceEntity)

    @Update
    suspend fun update(insurance: InsuranceEntity)

    @Delete
    suspend fun delete(insurance: InsuranceEntity)
}
