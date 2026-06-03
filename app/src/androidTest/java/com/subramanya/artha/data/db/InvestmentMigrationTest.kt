package com.subramanya.artha.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifies the v3 -> v4 investment-valuation migration is data-preserving:
 *  - existing rows gain `opening_contribution` back-filled from `current_value`
 *  - deposit-style instruments (RD here) are flipped to valuation_mode = 'DERIVED'
 *
 * The v3 table is created with raw SQL (matching the schema BEFORE the new columns existed)
 * so the test does not depend on a v3 schema JSON being present. runMigrationsAndValidate
 * then validates the migrated DB against the exported v4 schema (app/schemas/...4.json),
 * which is generated at build time because @Database has exportSchema = true.
 */
@RunWith(AndroidJUnit4::class)
class InvestmentMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4_backfillsContributionAndDerivesDepositMode() {
        // 1. Create the v3 database. MigrationTestHelper.createDatabase needs the v3 schema
        //    JSON; since v3 was never exported we build the `investments` table by hand to
        //    match the pre-migration schema (no opening_contribution / valuation_mode yet).
        val db = helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS investments (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    institution TEXT,
                    current_value REAL NOT NULL,
                    units REAL,
                    nav REAL,
                    start_date INTEGER NOT NULL,
                    maturity_date INTEGER,
                    tax_section TEXT,
                    icon TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    linked_insurance_id TEXT,
                    is_archived INTEGER NOT NULL,
                    display_order INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )

            // One RD with current_value = 60000; new columns are absent at v3.
            execSQL(
                """
                INSERT INTO investments
                    (id, name, type, institution, current_value, units, nav, start_date,
                     maturity_date, tax_section, icon, color, linked_insurance_id,
                     is_archived, display_order, created_at)
                VALUES
                    ('inv-1', 'HDFC RD', 'RD', 'HDFC Bank', 60000.0, NULL, NULL, 1700000000000,
                     NULL, NULL, 'savings', 4280000000, NULL, 0, 0, 1700000000000)
                """.trimIndent(),
            )
            close()
        }

        // 2. Run the migration and validate the resulting schema against the exported v4 JSON.
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).use { migratedDb ->
            migratedDb.query(
                "SELECT opening_contribution, valuation_mode FROM investments WHERE id = 'inv-1'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()

                val openingContribution =
                    cursor.getDouble(cursor.getColumnIndexOrThrow("opening_contribution"))
                val valuationMode =
                    cursor.getString(cursor.getColumnIndexOrThrow("valuation_mode"))

                // opening_contribution back-filled from current_value (60000).
                assertEquals(60_000.0, openingContribution, 0.0001)
                // RD is a deposit instrument -> DERIVED.
                assertEquals("DERIVED", valuationMode)
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
