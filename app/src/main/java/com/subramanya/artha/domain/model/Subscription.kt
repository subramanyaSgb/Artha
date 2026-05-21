package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.SubscriptionFrequency
import com.subramanya.artha.data.entity.enums.SubscriptionStatus

data class Subscription(
    val id: String,
    val name: String,
    val provider: String?,
    val amount: Double,
    val frequency: SubscriptionFrequency,
    val nextDueDate: Long,
    val lastPaidDate: Long?,
    val categoryId: String?,
    val paymentMethodType: String?,
    val paymentMethodId: String?,
    val status: SubscriptionStatus,
    val autoCharge: Boolean,
    val logoUri: String?,
    val color: Long,
    val createdAt: Long,
)
