package com.subramanya.artha.data.db.seed

import com.subramanya.artha.data.entity.InsuranceTypeEntity

object SeedInsuranceTypes {
    val BUILTINS: List<Pair<String, String>> = listOf(
        "HEALTH" to "Health",
        "VEHICLE" to "Vehicle",
        "LIFE_TERM" to "Life (term)",
        "LIFE_ENDOWMENT" to "Life (endowment)",
        "TRAVEL" to "Travel",
        "HOME" to "Home",
        "OTHER" to "Other",
    )

    const val DEFAULT_ID = "OTHER"

    fun all(): List<InsuranceTypeEntity> =
        BUILTINS.mapIndexed { idx, (id, label) ->
            InsuranceTypeEntity(id = id, label = label, isBuiltin = true, isHidden = false, displayOrder = idx)
        }
}
