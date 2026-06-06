package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.CardTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardTypeDao {
    @Query("SELECT * FROM card_type ORDER BY display_order, label")
    fun observeAll(): Flow<List<CardTypeEntity>>

    @Query("SELECT * FROM card_type WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CardTypeEntity?

    @Query("SELECT COUNT(*) FROM card_type")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CardTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<CardTypeEntity>)

    @Update
    suspend fun update(entry: CardTypeEntity)

    @Delete
    suspend fun delete(entry: CardTypeEntity)
}
