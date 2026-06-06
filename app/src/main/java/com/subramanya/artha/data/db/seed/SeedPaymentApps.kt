package com.subramanya.artha.data.db.seed

import com.subramanya.artha.data.entity.PaymentAppEntity

/**
 * The built-in payment apps seeded on first DB creation AND back-filled by MIGRATION_5_6.
 *
 * The ids are the EXACT names of the former `PaymentApp` enum so existing
 * `transactions.payment_app` rows (stored as `enum.name`) keep resolving. Labels match what
 * the old `ReportsViewModel.PaymentApp.label()` rendered. Keep this list and the migration's
 * INSERTs in lock-step — both seed the same rows, one for fresh installs and one for upgrades.
 */
object SeedPaymentApps {

    /** id (== old enum name) to display label, in display order. */
    val BUILTINS: List<Pair<String, String>> = listOf(
        "GPAY" to "GPay",
        "PHONEPE" to "PhonePe",
        "PAYTM" to "Paytm",
        "CRED" to "CRED",
        "BHIM" to "BHIM",
        "BANK_APP" to "Bank app",
        "CARD_SWIPE" to "Card swipe",
        "CASH" to "Cash",
        "NETBANKING" to "Netbanking",
        "OTHER" to "Other",
    )

    /** Fallback id used whenever a value can't be resolved to a known app. */
    const val DEFAULT_ID = "OTHER"

    fun all(): List<PaymentAppEntity> =
        BUILTINS.mapIndexed { index, (id, label) ->
            PaymentAppEntity(
                id = id,
                label = label,
                isBuiltin = true,
                isHidden = false,
                displayOrder = index,
            )
        }
}
