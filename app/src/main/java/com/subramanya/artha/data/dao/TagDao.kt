package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/** Per-tag transaction count projection for the batch usage query. */
data class TagUsageCount(val tagId: String, val count: Int)

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT COUNT(*) FROM transaction_tags WHERE tag_id = :tagId")
    suspend fun usageCount(tagId: String): Int

    /** One row per used tag — drives the "N transactions" count on the list without N queries. */
    @Query("SELECT tag_id AS tagId, COUNT(*) AS count FROM transaction_tags GROUP BY tag_id")
    fun observeUsageCounts(): Flow<List<TagUsageCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)
}
