package com.subramanya.artha.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.subramanya.artha.data.db.seed.SeedPaymentApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifies MIGRATION_5_6 (payment-app catalogue):
 *  - the `payment_app` table is created with the correct schema
 *  - all 10 built-in rows are seeded with the expected ids and labels
 *  - pre-existing `transactions.payment_app` values survive untouched (the column
 *    was already TEXT before and after — additive migration only)
 *  - schema is validated against the exported 6.json by MigrationTestHelper
 *
 * NOTE: this is an *instrumented* test and must be run on a device or emulator
 * (`./gradlew :app:connectedDebugAndroidTest`). It cannot run on the JVM.
 */
@RunWith(AndroidJUnit4::class)
class PaymentAppMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6_seedsAllBuiltinsAndPreservesTransactionPaymentApp() {
        // 1. Create a real v5 database, seed a minimal transaction whose payment_app value
        //    we'll verify is untouched after the migration.
        helper.createDatabase(TEST_DB, 5).apply {
            // Seed the bare-minimum rows that the FK graph of transactions requires.
            execSQL(
                """INSERT INTO accounts (id, name, type, institution, account_number_last4,
                    opening_balance, currency, icon, color, is_archived, display_order, created_at)
                    VALUES ('acct-1', 'Test', 'SAVINGS', NULL, NULL, 0.0, 'INR', 'bank',
                    4280000000, 0, 0, 1000)""",
            )
            execSQL(
                """INSERT INTO transactions (id, type, amount, currency, date, description,
                    category_id, sub_category_id, source_type, source_id, destination_type,
                    destination_id, payment_app, place, latitude, longitude, receipt_uri,
                    notes, tax_section, recurring_rule_id, is_split, split_group_id, source,
                    created_at, updated_at, excluded_from_expense_total)
                    VALUES ('txn-1', 'EXPENSE', 100.0, 'INR', 2000, 'Coffee',
                    NULL, NULL, 'ACCOUNT', 'acct-1', NULL, NULL, 'GPAY', NULL, NULL, NULL,
                    NULL, NULL, NULL, NULL, 0, NULL, 'MANUAL', 3000, 4000, 0)""",
            )
            close()
        }

        // 2. Run the migration and validate schema against 6.json.
        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6).use { db ->
            // 3. Verify all 10 built-in payment apps are seeded.
            val builtinIds = SeedPaymentApps.BUILTINS.map { it.first }.toSet()
            val labels = SeedPaymentApps.BUILTINS.associate { it.first to it.second }
            db.query("SELECT id, label, is_builtin FROM payment_app").use { cursor ->
                assertEquals(SeedPaymentApps.BUILTINS.size, cursor.count)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
                    val isBuiltin = cursor.getInt(cursor.getColumnIndexOrThrow("is_builtin"))
                    assertTrue("Unexpected id: $id", id in builtinIds)
                    assertEquals("Label mismatch for $id", labels[id], label)
                    assertEquals("is_builtin should be 1 for $id", 1, isBuiltin)
                }
            }

            // 4. Verify the pre-existing transaction's payment_app column is untouched.
            db.query("SELECT payment_app FROM transactions WHERE id = 'txn-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("GPAY", cursor.getString(cursor.getColumnIndexOrThrow("payment_app")))
            }
        }
    }

    companion object {
        private const val TEST_DB = "payment-app-migration-test"
    }
}
