package com.subramanya.artha.domain.model

import com.subramanya.artha.domain.rules.RuleActions
import com.subramanya.artha.domain.rules.RuleConditions

/** Domain view of a rules-engine rule. JSON in the entity is decoded into [conditions]
 *  and [actions] so callers work with typed objects. */
data class TransactionRule(
    val id: String,
    val name: String,
    val conditions: RuleConditions,
    val actions: RuleActions,
    val priority: Int,
    val isActive: Boolean,
    val isSystem: Boolean,
    val createdAt: Long,
)
