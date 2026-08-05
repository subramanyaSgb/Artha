package com.subramanya.artha.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifies the v11 -> v12 insurance-redesign migration is data-preserving:
 *  - the six new nullable TEXT columns (plan_name, policy_term, life_assured, uin,
 *    insurer_helpline, details_json) exist after migration and default to NULL on old rows
 *  - the pre-existing insurance row's key data survives untouched
 *
 * createDatabase(TEST_DB, 11) builds the REAL v11 schema from the exported 11.json asset,
 * so this is a faithful migration test rather than a hand-rolled table.
 */
@RunWith(AndroidJUnit4::class)
class InsuranceMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate11To12_addsPolicyColumns_preservesExistingRow() {
        // 1. Create the full, real v11 database and seed one insurance row. At v11 the six
        //    new columns do not exist yet, so they are omitted from the INSERT.
        helper.createDatabase(TEST_DB, 11).apply {
            execSQL(
                """
                INSERT INTO insurances
                    (id, name, type, provider, policy_number, sum_assured, premium_amount,
                     premium_frequency, next_premium_date, start_date, end_date, nominee,
                     agent_contact, policy_doc_uri, tax_section, icon, color, is_archived, created_at)
                VALUES
                    ('ins-1', 'HDFC Life Term', 'TERM', 'HDFC Life', 'POL123', 5000000.0, 12000.0,
                     'YEARLY', 1700000000000, 1600000000000, NULL, 'Spouse',
                     NULL, NULL, '80C', 'shield', 4280000000, 0, 1600000000000)
                """.trimIndent(),
            )
            close()
        }

        // 2. Run the migration and validate the resulting schema against the exported v12 JSON.
        helper.runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12).use { migratedDb ->
            migratedDb.query(
                "SELECT name, provider, sum_assured, plan_name, policy_term, life_assured, " +
                    "uin, insurer_helpline, details_json FROM insurances WHERE id = 'ins-1'",
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                // Pre-existing data survives.
                assertEquals("HDFC Life Term", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                assertEquals("HDFC Life", cursor.getString(cursor.getColumnIndexOrThrow("provider")))
                assertEquals(
                    5_000_000.0,
                    cursor.getDouble(cursor.getColumnIndexOrThrow("sum_assured")),
                    0.0001,
                )
                // New columns exist and default to NULL on the migrated row.
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("plan_name")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("policy_term")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("life_assured")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("uin")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("insurer_helpline")))
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow("details_json")))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
