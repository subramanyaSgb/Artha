package com.subramanya.artha.utils

import org.json.JSONArray
import org.json.JSONObject

/**
 * Display model for the open-ended `detailsJson` blob an uploaded policy stores
 * alongside its flat columns. Everything is optional: an AI extraction may include
 * any subset of these, and a manually-entered policy has no blob at all.
 *
 * Parsing is deliberately tolerant (see [parsePolicyDetails]) — this drives a
 * read-only detail screen and must never crash it on malformed input.
 */
data class PolicyDetails(
    val members: List<Member> = emptyList(),
    val riders: List<Rider> = emptyList(),
    val coverage: List<CoverageItem> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val contacts: Contacts? = null,
    val premiumBreakdown: PremiumBreakdown? = null,
    val status: String? = null,
) {
    /** True when there is nothing to render — screen degrades to core facts. */
    val isEmpty: Boolean
        get() = members.isEmpty() && riders.isEmpty() && coverage.isEmpty() &&
            exclusions.isEmpty() && contacts == null && premiumBreakdown == null &&
            status.isNullOrBlank()

    data class Member(val name: String, val relation: String?, val age: String?)
    data class Rider(val name: String, val premium: String?, val note: String?)
    data class CoverageItem(val label: String, val value: String)
    data class Contacts(
        val helpline: String?,
        val claimsEmail: String?,
        val branch: String?,
        val tpa: String?,
    )
    data class PremiumBreakdown(
        val base: String?,
        val riders: String?,
        val gst: String?,
        val total: String?,
    )
}

/**
 * Parses the saved `detailsJson` into a [PolicyDetails]. Missing keys collapse to
 * empty lists / null; malformed or blank JSON returns null. Never throws.
 */
fun parsePolicyDetails(json: String?): PolicyDetails? {
    if (json.isNullOrBlank()) return null
    return runCatching {
        val root = JSONObject(json)
        PolicyDetails(
            members = root.optJSONArray("members").objects().map {
                PolicyDetails.Member(
                    name = it.optNonBlank("name") ?: return@map null,
                    relation = it.optNonBlank("relation"),
                    age = it.optNonBlank("age"),
                )
            }.filterNotNull(),
            riders = root.optJSONArray("riders").objects().map {
                PolicyDetails.Rider(
                    name = it.optNonBlank("name") ?: return@map null,
                    premium = it.optNonBlank("premium"),
                    note = it.optNonBlank("note"),
                )
            }.filterNotNull(),
            coverage = root.optJSONArray("coverage").objects().map {
                val label = it.optNonBlank("label") ?: return@map null
                val value = it.optNonBlank("value") ?: return@map null
                PolicyDetails.CoverageItem(label, value)
            }.filterNotNull(),
            exclusions = root.optJSONArray("exclusions").strings(),
            contacts = root.optJSONObject("contacts")?.let { c ->
                PolicyDetails.Contacts(
                    helpline = c.optNonBlank("helpline"),
                    claimsEmail = c.optNonBlank("claimsEmail"),
                    branch = c.optNonBlank("branch"),
                    tpa = c.optNonBlank("tpa"),
                ).takeIf { it.helpline != null || it.claimsEmail != null || it.branch != null || it.tpa != null }
            },
            premiumBreakdown = root.optJSONObject("premiumBreakdown")?.let { p ->
                PolicyDetails.PremiumBreakdown(
                    base = p.optNonBlank("base"),
                    riders = p.optNonBlank("riders"),
                    gst = p.optNonBlank("gst"),
                    total = p.optNonBlank("total"),
                ).takeIf { it.base != null || it.riders != null || it.gst != null || it.total != null }
            },
            status = root.optNonBlank("status"),
        )
    }.getOrNull()?.takeIf { !it.isEmpty }
}

// ---- org.json helpers (values may arrive as strings or numbers) ----

private fun JSONObject.optNonBlank(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

private fun JSONArray?.objects(): List<JSONObject> {
    val arr = this ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
}

private fun JSONArray?.strings(): List<String> {
    val arr = this ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
}
