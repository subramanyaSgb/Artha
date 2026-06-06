package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.PaymentAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAppDao {
    /** All entries (incl. hidden) ordered for the manage-UI. */
    @Query("SELECT * FROM payment_app ORDER BY display_order, label")
    fun observeAll(): Flow<List<PaymentAppEntity>>

    @Query("SELECT * FROM payment_app WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PaymentAppEntity?

    @Query("SELECT COUNT(*) FROM payment_app")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PaymentAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<PaymentAppEntity>)

    @Update
    suspend fun update(entry: PaymentAppEntity)

    @Delete
    suspend fun delete(entry: PaymentAppEntity)
}
