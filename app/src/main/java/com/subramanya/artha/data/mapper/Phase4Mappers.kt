package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.BudgetEntity
import com.subramanya.artha.data.entity.GoalEntity
import com.subramanya.artha.data.entity.RecurringRuleEntity
import com.subramanya.artha.data.entity.SubscriptionEntity
import com.subramanya.artha.domain.model.Budget
import com.subramanya.artha.domain.model.Goal
import com.subramanya.artha.domain.model.RecurringRule
import com.subramanya.artha.domain.model.Subscription
import org.json.JSONArray

// ---- Budget ----

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id, name = name, scope = scope, categoryId = categoryId, amount = amount,
    period = period, startDate = startDate, alertThresholdPercent = alertThresholdPercent,
    isActive = isActive, createdAt = createdAt,
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id, name = name, scope = scope, categoryId = categoryId, amount = amount,
    period = period, startDate = startDate, alertThresholdPercent = alertThresholdPercent,
    isActive = isActive, createdAt = createdAt,
)

// ---- Goal ----

fun GoalEntity.toDomain(): Goal = Goal(
    id = id, name = name, targetAmount = targetAmount, targetDate = targetDate,
    linkedAccountIds = decodeStringList(linkedAccountIdsJson),
    linkedInvestmentIds = decodeStringList(linkedInvestmentIdsJson),
    icon = icon, color = color, isAchieved = isAchieved, createdAt = createdAt,
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id, name = name, targetAmount = targetAmount, targetDate = targetDate,
    linkedAccountIdsJson = encodeStringList(linkedAccountIds),
    linkedInvestmentIdsJson = encodeStringList(linkedInvestmentIds),
    icon = icon, color = color, isAchieved = isAchieved, createdAt = createdAt,
)

// ---- Subscription ----

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id, name = name, provider = provider, amount = amount, frequency = frequency,
    nextDueDate = nextDueDate, lastPaidDate = lastPaidDate, categoryId = categoryId,
    paymentMethodType = paymentMethodType, paymentMethodId = paymentMethodId,
    status = status, autoCharge = autoCharge, logoUri = logoUri, color = color,
    createdAt = createdAt,
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id, name = name, provider = provider, amount = amount, frequency = frequency,
    nextDueDate = nextDueDate, lastPaidDate = lastPaidDate, categoryId = categoryId,
    paymentMethodType = paymentMethodType, paymentMethodId = paymentMethodId,
    status = status, autoCharge = autoCharge, logoUri = logoUri, color = color,
    createdAt = createdAt,
)

// ---- RecurringRule ----

fun RecurringRuleEntity.toDomain(): RecurringRule = RecurringRule(
    id = id, name = name, transactionTemplate = transactionTemplate, frequency = frequency,
    dayOfPeriod = dayOfPeriod, nextRunDate = nextRunDate, lastRunDate = lastRunDate,
    autoConfirm = autoConfirm, isActive = isActive, createdAt = createdAt,
)

fun RecurringRule.toEntity(): RecurringRuleEntity = RecurringRuleEntity(
    id = id, name = name, transactionTemplate = transactionTemplate, frequency = frequency,
    dayOfPeriod = dayOfPeriod, nextRunDate = nextRunDate, lastRunDate = lastRunDate,
    autoConfirm = autoConfirm, isActive = isActive, createdAt = createdAt,
)

// ---- helpers ----

private fun decodeStringList(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        buildList { for (i in 0 until arr.length()) add(arr.getString(i)) }
    }.getOrDefault(emptyList())
}

private fun encodeStringList(items: List<String>): String {
    val arr = JSONArray()
    items.forEach { arr.put(it) }
    return arr.toString()
}
