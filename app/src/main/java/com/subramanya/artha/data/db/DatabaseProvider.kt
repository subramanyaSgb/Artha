package com.subramanya.artha.data.db

import android.content.Context
import androidx.room.Room
import com.subramanya.artha.data.db.seed.CategorySeederCallback

/**
 * Process-wide singleton for AppDatabase. No DI framework in Phase 1; callers retrieve
 * the database via [get]. ArthaApplication wires the application context.
 */
object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addCallback(CategorySeederCallback())
            // Debug-friendly: schema changes wipe local data. Phase 1 has no migration plan yet.
            .fallbackToDestructiveMigration()
            .build()
}
