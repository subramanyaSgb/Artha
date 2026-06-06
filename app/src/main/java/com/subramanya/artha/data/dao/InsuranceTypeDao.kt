package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.InsuranceTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsuranceTypeDao {
    @Query("SELECT * FROM insurance_type ORDER BY display_order, label")
    fun observeAll(): Flow<List<InsuranceTypeEntity>>

    @Query("SELECT * FROM insurance_type WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InsuranceTypeEntity?

    @Query("SELECT COUNT(*) FROM insurance_type")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: InsuranceTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<InsuranceTypeEntity>)

    @Update
    suspend fun update(entry: InsuranceTypeEntity)

    @Delete
    suspend fun delete(entry: InsuranceTypeEntity)
}
