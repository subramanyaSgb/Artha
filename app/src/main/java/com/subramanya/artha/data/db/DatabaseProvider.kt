package com.subramanya.artha.data.db

import android.content.Context
import androidx.room.Room
import com.subramanya.artha.data.db.seed.CategorySeederCallback
import com.subramanya.artha.data.db.seed.RuleSeederCallback

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
            .addCallback(RuleSeederCallback())
            // Real, data-preserving migrations run first. v3->v4 adds investment valuation
            // columns and back-fills existing rows (see MIGRATION_3_4).
            .addMigrations(MIGRATION_3_4)
            // Last-resort backstop: any schema gap NOT covered by an explicit migration wipes
            // local data rather than crashing. Kept intentionally below addMigrations so a
            // declared migration always takes precedence over destruction.
            //
            // WARNING: this is a last-resort backstop. Any FUTURE @Database version bump that
            // ships WITHOUT a matching Migration here will SILENTLY WIPE all user data (no cloud
            // backup exists). Always add the MIGRATION_n_n+1 to addMigrations(...) above before
            // bumping the version.
            .fallbackToDestructiveMigration()
            .build()
}
