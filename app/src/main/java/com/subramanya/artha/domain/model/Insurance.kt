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
    val icon: String,
    val color: Long,
    val isArchived: Boolean,
    val createdAt: Long,
)
