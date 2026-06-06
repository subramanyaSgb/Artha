package com.subramanya.artha.data.db.seed

import com.subramanya.artha.data.entity.CardTypeEntity

object SeedCardTypes {
    val BUILTINS: List<Pair<String, String>> = listOf(
        "CREDIT" to "Credit",
        "DEBIT" to "Debit",
        "PREPAID" to "Prepaid",
    )

    /** The CREDIT id is still used in logic (isCreditCard / resolveCardAlias). */
    const val CREDIT_ID = "CREDIT"

    fun all(): List<CardTypeEntity> =
        BUILTINS.mapIndexed { idx, (id, label) ->
            CardTypeEntity(id = id, label = label, isBuiltin = true, isHidden = false, displayOrder = idx)
        }
}
