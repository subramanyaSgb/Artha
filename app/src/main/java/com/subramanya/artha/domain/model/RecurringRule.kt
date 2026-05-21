package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.RecurringFrequency

data class RecurringRule(
    val id: String,
    val name: String,
    val transactionTemplate: String,
    val frequency: RecurringFrequency,
    val dayOfPeriod: Int?,
    val nextRunDate: Long,
    val lastRunDate: Long?,
    val autoConfirm: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
)
