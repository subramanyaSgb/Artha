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
 *  - every existing row gains `opening_contribution`, back-filled from `current_value`
 *  - deposit-style instruments (RD here) are flipped to valuation_mode = 'DERIVED'
 *  - market-style instruments (MUTUAL_FUND here) keep the default valuation_mode = 'MARKET'
 *  - pre-existing columns (name / current_value) survive untouched
 *
 * createDatabase(TEST_DB, 3) builds the REAL v3 schema from the exported
 * app/schemas/.../3.json asset (all tables + indices), so this is a faithful migration
 * test rather than a hand-rolled table. runMigrationsAndValidate(..., 4, ...) then runs
 * MIGRATION_3_4 and validates the result against the exported v4 schema (4.json).
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
        // 1. Create the full, real v3 database from the exported 3.json schema, then seed
        //    two investments that exercise both migration branches. At v3 the new columns
        //    (opening_contribution / valuation_mode) do not exist yet, so they are omitted.
        helper.createDatabase(TEST_DB, 3).apply {
            // RD -> DERIVED branch; current_value = 60000 becomes opening_contribution.
            execSQL(
                """
                INSERT INTO investments
                    (id, name, type, institution, current_value, units, nav, start_date,
                     maturity_date, tax_section, icon, color, linked_insurance_id,
                     is_archived, display_order, created_at)
                VALUES
                    ('inv-rd', 'HDFC RD', 'RD', 'HDFC Bank', 60000.0, NULL, NULL, 1700000000000,
                     NULL, NULL, 'savings', 4280000000, NULL, 0, 0, 1700000000000)
                """.trimIndent(),
            )
            // MUTUAL_FUND -> MARKET branch; current_value = 90000 becomes opening_contribution.
            execSQL(
                """
                INSERT INTO investments
                    (id, name, type, institution, current_value, units, nav, start_date,
                     maturity_date, tax_section, icon, color, linked_insurance_id,
                     is_archived, display_order, created_at)
                VALUES
                    ('inv-mf', 'Axis Bluechip', 'MUTUAL_FUND', 'Axis MF', 90000.0, 1000.0, 90.0,
                     1700000000000, NULL, NULL, 'trending_up', 4290000000, NULL, 0, 1, 1700000000000)
                """.trimIndent(),
            )
            close()
        }

        // 2. Run the migration and validate the resulting schema against the exported v4 JSON.
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).use { migratedDb ->
            // RD: opening_contribution back-filled, mode flipped to DERIVED, name preserved.
            migratedDb.query(
                "SELECT name, current_value, opening_contribution, valuation_mode " +
                    "FROM investments WHERE id = 'inv-rd'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(
                    "HDFC RD",
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                )
                assertEquals(
                    60_000.0,
                    cursor.getDouble(cursor.getColumnIndexOrThrow("current_value")),
                    0.0001,
                )
                assertEquals(
                    60_000.0,
                    cursor.getDouble(cursor.getColumnIndexOrThrow("opening_contribution")),
                    0.0001,
                )
                assertEquals(
                    "DERIVED",
                    cursor.getString(cursor.getColumnIndexOrThrow("valuation_mode")),
                )
            }

            // MUTUAL_FUND: opening_contribution back-filled, mode stays MARKET (the default).
            migratedDb.query(
                "SELECT opening_contribution, valuation_mode " +
                    "FROM investments WHERE id = 'inv-mf'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(
                    90_000.0,
                    cursor.getDouble(cursor.getColumnIndexOrThrow("opening_contribution")),
                    0.0001,
                )
                assertEquals(
                    "MARKET",
                    cursor.getString(cursor.getColumnIndexOrThrow("valuation_mode")),
                )
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
