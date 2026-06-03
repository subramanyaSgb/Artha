package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY display_order, name")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE is_archived = 0 ORDER BY display_order, name")
    fun observeActive(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE is_archived = 1 ORDER BY display_order, name")
    fun observeArchived(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<InvestmentEntity?>

    @Query("SELECT * FROM investments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvestmentEntity?

    @Query("SELECT * FROM investments WHERE linked_insurance_id = :insuranceId LIMIT 1")
    suspend fun findByLinkedInsurance(insuranceId: String): InvestmentEntity?

    /** Clear the back-link when the linked insurance is deleted, so the investment isn't left
     *  pointing at a non-existent policy (the investment row itself is kept). */
    @Query("UPDATE investments SET linked_insurance_id = NULL WHERE linked_insurance_id = :insuranceId")
    suspend fun unlinkInsurance(insuranceId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(investment: InvestmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(investments: List<InvestmentEntity>)

    @Update
    suspend fun update(investment: InvestmentEntity)

    @Delete
    suspend fun delete(investment: InvestmentEntity)
}
