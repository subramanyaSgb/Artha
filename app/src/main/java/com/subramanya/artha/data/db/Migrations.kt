package com.subramanya.artha.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subramanya.artha.data.db.seed.SeedAccountTypes
import com.subramanya.artha.data.db.seed.SeedCardTypes
import com.subramanya.artha.data.db.seed.SeedInsuranceTypes
import com.subramanya.artha.data.db.seed.SeedPaymentApps

/**
 * v3 -> v4: investment valuation redesign.
 *
 * Adds two columns to `investments` and back-fills existing rows so no data is lost:
 *  - `opening_contribution` (REAL): seeded from each row's existing `current_value` so a
 *    DERIVED instrument's contribution base starts at its last-known value.
 *  - `valuation_mode` (TEXT): defaults to 'MARKET'; deposit-style instruments
 *    (FD/RD/PPF/EPF/BONDS) are switched to 'DERIVED' to match
 *    InvestmentType.defaultValuationMode().
 *
 * Columns are NOT NULL with defaults so the ALTER succeeds on existing rows, and the
 * UPDATEs run inside the same migration to give every pre-existing row a sensible value.
 * The `type` column stores InvestmentType.name (enum literals), so the IN-list matches the
 * stored strings exactly — getting these literals wrong would silently no-op the back-fill.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE investments ADD COLUMN opening_contribution REAL NOT NULL DEFAULT 0",
        )
        db.execSQL("UPDATE investments SET opening_contribution = current_value")
        db.execSQL(
            "ALTER TABLE investments ADD COLUMN valuation_mode TEXT NOT NULL DEFAULT 'MARKET'",
        )
        db.execSQL(
            "UPDATE investments SET valuation_mode = 'DERIVED' " +
                "WHERE type IN ('FD','RD','PPF','EPF','BONDS')",
        )
    }
}

/**
 * v4 -> v5: add `excluded_from_expense_total` to `transactions` so a rule's
 * ExcludeFromExpenseTotal action can be honored by the monthly aggregator. NOT NULL DEFAULT 0
 * (false) so existing rows are unaffected; matches the entity's `defaultValue = "0"`.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transactions ADD COLUMN excluded_from_expense_total INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/**
 * v5 -> v6: configurable pick-lists, Phase 2 — the `PaymentApp` enum becomes a user-editable
 * catalogue.
 *
 * This migration is purely ADDITIVE:
 *  - `transactions.payment_app` was already `TEXT NOT NULL` (the enum was stored via a converter
 *    as `enum.name`). As a plain `String` column it is byte-identical, so NO column change is
 *    needed and existing rows keep their value.
 *  - We only CREATE the new `payment_app` catalogue table and seed the 10 built-ins, whose ids
 *    are the former enum names — so every existing `transactions.payment_app` value resolves to
 *    a built-in row.
 *
 * The CREATE TABLE statement MUST match Room's generated schema for [PaymentAppEntity] exactly
 * (column order, types, defaults), or Room's post-migration schema validation throws. It is kept
 * in sync with `app/schemas/com.subramanya.artha.data.db.AppDatabase/6.json`.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payment_app` (" +
                "`id` TEXT NOT NULL, " +
                "`label` TEXT NOT NULL, " +
                "`is_builtin` INTEGER NOT NULL, " +
                "`is_hidden` INTEGER NOT NULL DEFAULT 0, " +
                "`display_order` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        for (app in SeedPaymentApps.all()) {
            db.execSQL(
                "INSERT OR REPLACE INTO `payment_app` " +
                    "(`id`, `label`, `is_builtin`, `is_hidden`, `display_order`) VALUES (?, ?, ?, ?, ?)",
                arrayOf(app.id, app.label, if (app.isBuiltin) 1 else 0, if (app.isHidden) 1 else 0, app.displayOrder),
            )
        }
    }
}

/**
 * v6 -> v7: configurable pick-lists, Phase 3 — three type enums become user-editable catalogues.
 *
 * Additive migration: AccountEntity.type, CardEntity.type, InsuranceEntity.type were already
 * TEXT columns (enum stored via TypeConverters as .name), so they are byte-compatible as plain
 * Strings. Only CREATE the three catalogue tables and seed the built-ins.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for ((table, label) in listOf("account_type" to "account", "card_type" to "card", "insurance_type" to "insurance")) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `$table` (" +
                    "`id` TEXT NOT NULL, " +
                    "`label` TEXT NOT NULL, " +
                    "`is_builtin` INTEGER NOT NULL, " +
                    "`is_hidden` INTEGER NOT NULL DEFAULT 0, " +
                    "`display_order` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
        }
        val insertSql = "INSERT OR REPLACE INTO `%s` (`id`, `label`, `is_builtin`, `is_hidden`, `display_order`) VALUES (?, ?, ?, ?, ?)"
        for (row in SeedAccountTypes.all()) {
            db.execSQL(insertSql.format("account_type"), arrayOf(row.id, row.label, if (row.isBuiltin) 1 else 0, 0, row.displayOrder))
        }
        for (row in SeedCardTypes.all()) {
            db.execSQL(insertSql.format("card_type"), arrayOf(row.id, row.label, if (row.isBuiltin) 1 else 0, 0, row.displayOrder))
        }
        for (row in SeedInsuranceTypes.all()) {
            db.execSQL(insertSql.format("insurance_type"), arrayOf(row.id, row.label, if (row.isBuiltin) 1 else 0, 0, row.displayOrder))
        }
    }
}

/**
 * v7 -> v8: SMS auto-import review queue.
 *
 * Purely additive: CREATE the new `pending_sms` table that holds bank-SMS-derived
 * transactions awaiting user review. No existing table is touched. The CREATE TABLE
 * must match Room's generated schema for [com.subramanya.artha.data.entity.PendingSmsEntity]
 * exactly (kept in sync with app/schemas/…/8.json), or post-migration validation throws.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_sms` (" +
                "`id` TEXT NOT NULL, " +
                "`received_at` INTEGER NOT NULL, " +
                "`sender` TEXT NOT NULL, " +
                "`raw_body` TEXT NOT NULL, " +
                "`amount` REAL, " +
                "`direction` TEXT NOT NULL, " +
                "`merchant` TEXT, " +
                "`account_hint` TEXT, " +
                "`ref_no` TEXT, " +
                "`occurred_at` INTEGER, " +
                "`matched_account_id` TEXT, " +
                "`suggested_category_id` TEXT, " +
                "`parse_source` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_sms_received_at` ON `pending_sms` (`received_at`)",
        )
    }
}

/**
 * v8 -> v9: the first SMS auto-import feature was removed. Drop its `pending_sms` review-queue
 * table. FORWARD-ONLY and data-safe: it only touches the SMS table — accounts, transactions,
 * and everything else are untouched. Kept in the chain (not a downgrade) so an installed v8 DB
 * with real data upgrades cleanly without a destructive wipe.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `pending_sms`")
    }
}

/**
 * v9 -> v10: the (fork-ported) SMS auto-import review queue. Additive — CREATE the
 * `pending_sms_transactions` table. Must match Room's generated schema for
 * [com.subramanya.artha.data.entity.PendingSmsTransactionEntity] exactly (kept in sync with
 * app/schemas/…/10.json), or post-migration validation throws.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_sms_transactions` (" +
                "`id` TEXT NOT NULL, " +
                "`raw_sms_body` TEXT NOT NULL, " +
                "`sender` TEXT NOT NULL, " +
                "`received_at` INTEGER NOT NULL, " +
                "`direction` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`account_hint` TEXT, " +
                "`merchant` TEXT, " +
                "`suggested_category_id` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cards ADD COLUMN card_image_uri TEXT")
    }
}
