package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.subramanya.artha.data.entity.enums.PremiumFrequency

/**
 * An insurance policy. For endowment / ULIP / money-back policies the linked
 * [InvestmentEntity] (looked up via `InvestmentEntity.linkedInsuranceId`) tracks the
 * cost/return side; this entity owns the policy metadata + premium cadence.
 */
@Entity(
    tableName = "insurances",
    indices = [
        Index(value = ["type"]),
        Index(value = ["next_premium_date"]),
        Index(value = ["is_archived"]),
    ],
)
data class InsuranceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val provider: String,
    @ColumnInfo(name = "policy_number")
    val policyNumber: String?,
    @ColumnInfo(name = "sum_assured")
    val sumAssured: Double,
    @ColumnInfo(name = "premium_amount")
    val premiumAmount: Double,
    @ColumnInfo(name = "premium_frequency")
    val premiumFrequency: PremiumFrequency,
    /** Next due date (epoch millis). Recomputed when a premium is recorded. */
    @ColumnInfo(name = "next_premium_date")
    val nextPremiumDate: Long?,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "end_date")
    val endDate: Long?,
    val nominee: String?,
    @ColumnInfo(name = "agent_contact")
    val agentContact: String?,
    @ColumnInfo(name = "policy_doc_uri")
    val policyDocUri: String?,
    @ColumnInfo(name = "tax_section")
    val taxSection: String?,
    // Insurance redesign (v11->v12): rich policy fields extracted from an uploaded PDF.
    // details_json holds open-ended extras the parser can't map to a dedicated column.
    @ColumnInfo(name = "plan_name")
    val planName: String? = null,
    @ColumnInfo(name = "policy_term")
    val policyTerm: String? = null,
    @ColumnInfo(name = "life_assured")
    val lifeAssured: String? = null,
    @ColumnInfo(name = "uin")
    val uin: String? = null,
    @ColumnInfo(name = "insurer_helpline")
    val insurerHelpline: String? = null,
    @ColumnInfo(name = "details_json")
    val detailsJson: String? = null,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
