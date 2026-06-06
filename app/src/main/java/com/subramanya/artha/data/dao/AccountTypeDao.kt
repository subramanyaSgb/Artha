package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.subramanya.artha.data.entity.AccountTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountTypeDao {
    @Query("SELECT * FROM account_type ORDER BY display_order, label")
    fun observeAll(): Flow<List<AccountTypeEntity>>

    @Query("SELECT * FROM account_type WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountTypeEntity?

    @Query("SELECT COUNT(*) FROM account_type")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AccountTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<AccountTypeEntity>)

    @Update
    suspend fun update(entry: AccountTypeEntity)

    @Delete
    suspend fun delete(entry: AccountTypeEntity)
}
