package com.subramanya.artha.domain.recurring

import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import org.json.JSONObject

/**
 * The structured transaction spec stored inside [RecurringRule.transactionTemplate] as JSON.
 * Captured when the user creates/edits a rule; materialised into a real TransactionEntity
 * by [RecurringFireEngine] at fire time.
 *
 * Only the fields the user can pre-configure are here. Date, id, createdAt/updatedAt, and
 * source=RECURRING are injected at fire time.
 */
data class RecurringTemplate(
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val sourceType: SourceKind,
    val sourceId: String?,
    val destinationType: SourceKind?,
    val destinationId: String?,
    val categoryId: String?,
    val paymentApp: String,
    val notes: String?,
)

object RecurringTemplateCodec {

    fun encode(t: RecurringTemplate): String = JSONObject().apply {
        put("amount", t.amount)
        put("type", t.type.name)
        put("description", t.description)
        put("source_type", t.sourceType.name)
        putNullable("source_id", t.sourceId)
        putNullable("destination_type", t.destinationType?.name)
        putNullable("destination_id", t.destinationId)
        putNullable("category_id", t.categoryId)
        put("payment_app", t.paymentApp)
        putNullable("notes", t.notes)
    }.toString()

    fun decode(json: String): RecurringTemplate? = runCatching {
        val o = JSONObject(json)
        RecurringTemplate(
            amount = o.getDouble("amount"),
            type = TransactionType.valueOf(o.getString("type")),
            description = o.optString("description"),
            sourceType = SourceKind.valueOf(o.getString("source_type")),
            sourceId = o.stringOrNull("source_id"),
            destinationType = o.stringOrNull("destination_type")?.let { SourceKind.valueOf(it) },
            destinationId = o.stringOrNull("destination_id"),
            categoryId = o.stringOrNull("category_id"),
            paymentApp = o.optString("payment_app", "OTHER"),
            notes = o.stringOrNull("notes"),
        )
    }.getOrNull()

    private fun JSONObject.putNullable(key: String, value: Any?) =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
