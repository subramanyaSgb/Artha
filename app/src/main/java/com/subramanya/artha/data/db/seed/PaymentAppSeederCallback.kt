package com.subramanya.artha.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subramanya.artha.data.entity.PaymentAppEntity

/**
 * Seeds the built-in payment apps on first DB creation. Same pattern as
 * [RuleSeederCallback] — raw inserts inside onCreate so the catalogue is populated
 * synchronously before any DAO read can return an empty list. Upgrades from v5 get the
 * same rows via MIGRATION_5_6 instead (onCreate only fires for a brand-new database).
 */
internal class PaymentAppSeederCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.beginTransaction()
        try {
            for (app in SeedPaymentApps.all()) {
                db.insert("payment_app", SQLiteDatabase.CONFLICT_REPLACE, app.toContentValues())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun PaymentAppEntity.toContentValues(): ContentValues =
        ContentValues().apply {
            put("id", id)
            put("label", label)
            put("is_builtin", if (isBuiltin) 1 else 0)
            put("is_hidden", if (isHidden) 1 else 0)
            put("display_order", displayOrder)
        }
}
