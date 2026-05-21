package com.subramanya.artha.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.subramanya.artha.data.dao.AccountDao
import com.subramanya.artha.data.dao.CardDao
import com.subramanya.artha.data.dao.CategoryDao
import com.subramanya.artha.data.dao.PersonDao
import com.subramanya.artha.data.dao.TagDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.entity.AccountEntity
import com.subramanya.artha.data.entity.CardEntity
import com.subramanya.artha.data.entity.CategoryEntity
import com.subramanya.artha.data.entity.PersonEntity
import com.subramanya.artha.data.entity.TagEntity
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.TransactionPersonCrossRef
import com.subramanya.artha.data.entity.TransactionTagCrossRef

@Database(
    entities = [
        AccountEntity::class,
        CardEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
        TagEntity::class,
        TransactionEntity::class,
        TransactionPersonCrossRef::class,
        TransactionTagCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun personDao(): PersonDao
    abstract fun tagDao(): TagDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DB_NAME = "artha.db"
    }
}
