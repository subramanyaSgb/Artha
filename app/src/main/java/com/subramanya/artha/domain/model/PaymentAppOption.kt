package com.subramanya.artha.domain.model

/**
 * A pickable payment app (catalogue-backed; Phase 2 of configurable pick-lists). `id` is what
 * a transaction stores in `payment_app`; built-ins use the former enum name as id. Custom
 * entries use a UUID. Hidden built-ins are dropped from pickers but kept so old rows resolve.
 */
data class PaymentAppOption(
    val id: String,
    val label: String,
    val isBuiltin: Boolean,
    val isHidden: Boolean,
    val displayOrder: Int,
)
