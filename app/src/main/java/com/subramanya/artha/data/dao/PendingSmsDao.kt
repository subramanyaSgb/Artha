package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.subramanya.artha.data.entity.PendingSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSmsDao {
    @Query("SELECT * FROM pending_sms ORDER BY received_at DESC")
    fun observeAll(): Flow<List<PendingSmsEntity>>

    /** Live count for the review-queue badge. */
    @Query("SELECT COUNT(*) FROM pending_sms")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pending_sms WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingSmsEntity?

    /** True if a pending row already exists for this ref — cheap duplicate guard. */
    @Query("SELECT COUNT(*) FROM pending_sms WHERE ref_no = :refNo AND ref_no IS NOT NULL")
    suspend fun countByRef(refNo: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: PendingSmsEntity)

    @Query("DELETE FROM pending_sms WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_sms")
    suspend fun clear()
}
