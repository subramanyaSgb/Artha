package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY display_order, name")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE is_archived = 0 ORDER BY display_order, name")
    fun observeActive(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE is_archived = 1 ORDER BY display_order, name")
    fun observeArchived(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CardEntity?

    @Query("SELECT card_image_uri FROM cards WHERE card_image_uri IS NOT NULL")
    suspend fun allImageUris(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)
}
