package com.subramanya.artha.domain.model


import com.subramanya.artha.data.entity.enums.PremiumFrequency

data class Insurance(
    val id: String,
    val name: String,
    val type: String,
    val provider: String,
    val policyNumber: String?,
    val sumAssured: Double,
    val premiumAmount: Double,
    val premiumFrequency: PremiumFrequency,
    val nextPremiumDate: Long?,
    val startDate: Long,
    val endDate: Long?,
    val nominee: String?,
    val agentContact: String?,
    val policyDocUri: String?,
    val taxSection: String?,
    // Insurance redesign (v12): rich policy metadata extracted from an uploaded PDF.
    // detailsJson holds open-ended extras (members/riders/coverage/exclusions/contacts).
    val planName: String? = null,
    val policyTerm: String? = null,
    val lifeAssured: String? = null,
    val uin: String? = null,
    val insurerHelpline: String? = null,
    val detailsJson: String? = null,
    val icon: String,
    val color: Long,
    val isArchived: Boolean,
    val createdAt: Long,
)
