package com.subramanya.artha.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds the three type catalogues (account_type, card_type, insurance_type) on first
 * DB creation (new installs only — upgrades get the same rows via MIGRATION_6_7).
 */
internal class TypeCatalogueSeederCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.beginTransaction()
        try {
            for (row in SeedAccountTypes.all()) {
                db.insert("account_type", SQLiteDatabase.CONFLICT_REPLACE, row.toValues())
            }
            for (row in SeedCardTypes.all()) {
                db.insert("card_type", SQLiteDatabase.CONFLICT_REPLACE, row.toValues())
            }
            for (row in SeedInsuranceTypes.all()) {
                db.insert("insurance_type", SQLiteDatabase.CONFLICT_REPLACE, row.toValues())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun toValues(id: String, label: String, isBuiltin: Boolean, isHidden: Boolean, displayOrder: Int) =
        ContentValues().apply {
            put("id", id); put("label", label)
            put("is_builtin", if (isBuiltin) 1 else 0)
            put("is_hidden", if (isHidden) 1 else 0)
            put("display_order", displayOrder)
        }

    private fun com.subramanya.artha.data.entity.AccountTypeEntity.toValues() =
        toValues(id, label, isBuiltin, isHidden, displayOrder)

    private fun com.subramanya.artha.data.entity.CardTypeEntity.toValues() =
        toValues(id, label, isBuiltin, isHidden, displayOrder)

    private fun com.subramanya.artha.data.entity.InsuranceTypeEntity.toValues() =
        toValues(id, label, isBuiltin, isHidden, displayOrder)
}
