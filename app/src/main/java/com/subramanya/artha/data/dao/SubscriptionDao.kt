package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY status, next_due_date")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE status = 'ACTIVE' ORDER BY next_due_date")
    fun observeActive(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subscription: SubscriptionEntity)

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Delete
    suspend fun delete(subscription: SubscriptionEntity)
}
