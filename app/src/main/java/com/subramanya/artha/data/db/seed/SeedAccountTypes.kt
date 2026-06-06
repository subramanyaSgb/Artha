package com.subramanya.artha.data.db.seed

import com.subramanya.artha.data.entity.AccountTypeEntity

object SeedAccountTypes {
    val BUILTINS: List<Pair<String, String>> = listOf(
        "SAVINGS" to "Savings",
        "CURRENT" to "Current",
        "CASH" to "Cash",
        "WALLET" to "Wallet",
    )

    const val DEFAULT_ID = "SAVINGS"

    fun all(): List<AccountTypeEntity> =
        BUILTINS.mapIndexed { idx, (id, label) ->
            AccountTypeEntity(id = id, label = label, isBuiltin = true, isHidden = false, displayOrder = idx)
        }
}
