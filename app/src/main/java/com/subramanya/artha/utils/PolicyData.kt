package com.subramanya.artha.utils

/**
 * Core policy fields the AI extracts from a policy document, plus the raw rich JSON
 * (members/riders/coverage/exclusions/contacts) stored verbatim in details_json and
 * rendered by the detail screen. All fields optional — anything the model can't read
 * comes back null and the review screen leaves it blank.
 *
 * `*Hint` fields (typeHint, premiumFrequencyHint) are raw model output — resolved against
 * the type / frequency catalogues before becoming a final Insurance, hence they don't
 * match the domain field names.
 */
data class PolicyData(
    val name: String?,
    val typeHint: String?,
    val provider: String?,
    val policyNumber: String?,
    val sumAssured: Double?,
    val premiumAmount: Double?,
    val premiumFrequencyHint: String?,
    val startDateMillis: Long?,
    val endDateMillis: Long?,
    val nextDueMillis: Long?,
    val nominee: String?,
    val taxSection: String?,
    val planName: String?,
    val policyTerm: String?,
    val lifeAssured: String?,
    val uin: String?,
    val insurerHelpline: String?,
    val detailsJson: String?,
)
