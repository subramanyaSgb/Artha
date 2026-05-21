package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the Rules Engine (PRD §7.20 + §10). Conditions and actions are
 * persisted as JSON blobs so the schema doesn't need to grow per-rule-type.
 *
 * The JSON shape lives in [com.subramanya.artha.domain.rules.RuleSpec]; the
 * engine reifies it at execute-time without Room ever needing to know the keys.
 *
 * [isSystem] marks the 10 PRD-shipped rules — they can be disabled/edited but not
 * deleted (mirrors the system-category convention).
 */
@Entity(
    tableName = "transaction_rules",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["is_active"]),
    ],
)
data class TransactionRuleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "conditions_json")
    val conditionsJson: String,
    @ColumnInfo(name = "actions_json")
    val actionsJson: String,
    /** Lower priority runs first. Lets system rules guarantee their place. */
    val priority: Int,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
