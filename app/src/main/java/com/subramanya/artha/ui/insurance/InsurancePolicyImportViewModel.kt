package com.subramanya.artha.ui.insurance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InsuranceTypeRepository
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.InsuranceTypeOption
import com.subramanya.artha.utils.PolicyDocParser
import com.subramanya.artha.utils.PolicyDocStore
import com.subramanya.artha.utils.renderPolicyPagesToBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface InsurancePolicyImportUiState {
    data object Scanning : InsurancePolicyImportUiState

    /** Pre-filled from the AI parse; every field is editable before saving. */
    data class Parsed(
        val insuranceTypes: List<InsuranceTypeOption>,
        val name: String,
        val typeId: String,
        val provider: String,
        val policyNumberText: String,
        val sumAssuredText: String,
        val premiumText: String,
        val frequency: PremiumFrequency,
        val startMillis: Long,
        val endMillis: Long?,
        val nextDueMillis: Long?,
        val nominee: String,
        val taxSection: String,
        val planName: String?,
        val policyTerm: String?,
        val lifeAssured: String?,
        val uin: String?,
        val insurerHelpline: String?,
        /** Raw rich JSON (members/riders/etc.), stored verbatim — not edited here. */
        val detailsJson: String?,
        /** Count of extra rich keys captured beyond the flat fields, for a "N extra details" note. */
        val extraDetailCount: Int,
        val isSaving: Boolean = false,
    ) : InsurancePolicyImportUiState

    data class Saved(val insuranceId: String) : InsurancePolicyImportUiState
    data class ScanError(val message: String) : InsurancePolicyImportUiState
}

class InsurancePolicyImportViewModel(
    private val imageUri: Uri,
    private val policyDocParser: PolicyDocParser,
    private val insuranceRepository: InsuranceRepository,
    private val insuranceTypeRepository: InsuranceTypeRepository,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<InsurancePolicyImportUiState>(InsurancePolicyImportUiState.Scanning)
    val state: StateFlow<InsurancePolicyImportUiState> = _state.asStateFlow()

    init { scan() }

    private fun scan() {
        _state.value = InsurancePolicyImportUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val images = renderPolicyPagesToBase64(context, imageUri)
                if (images.isEmpty()) {
                    _state.value = InsurancePolicyImportUiState.ScanError(
                        "Couldn't read this PDF. Make sure it's a policy document (PDF or image), or add the policy manually.",
                    )
                    return@launch
                }
                val data = policyDocParser.parse(images)
                val types = insuranceTypeRepository.observeVisible().first()

                if (data == null) {
                    _state.value = InsurancePolicyImportUiState.ScanError(
                        "Couldn't read this policy. Check your connection and AI key, or add the policy manually.",
                    )
                    return@launch
                }

                _state.value = InsurancePolicyImportUiState.Parsed(
                    insuranceTypes = types,
                    name = data.name.orEmpty(),
                    typeId = resolveTypeId(data.typeHint, types),
                    provider = data.provider.orEmpty(),
                    policyNumberText = data.policyNumber.orEmpty(),
                    sumAssuredText = data.sumAssured?.let(::plainAmount).orEmpty(),
                    premiumText = data.premiumAmount?.let(::plainAmount).orEmpty(),
                    frequency = resolveFrequency(data.premiumFrequencyHint),
                    startMillis = data.startDateMillis ?: System.currentTimeMillis(),
                    endMillis = data.endDateMillis,
                    nextDueMillis = data.nextDueMillis,
                    nominee = data.nominee.orEmpty(),
                    taxSection = data.taxSection.orEmpty(),
                    planName = data.planName,
                    policyTerm = data.policyTerm,
                    lifeAssured = data.lifeAssured,
                    uin = data.uin,
                    insurerHelpline = data.insurerHelpline,
                    detailsJson = data.detailsJson,
                    extraDetailCount = countExtraDetails(data.detailsJson),
                )
            } catch (e: Exception) {
                _state.value = InsurancePolicyImportUiState.ScanError(friendlyError(e))
            }
        }
    }

    fun updateName(text: String) = updateParsed { it.copy(name = text) }
    fun selectType(id: String) = updateParsed { it.copy(typeId = id) }
    fun updateProvider(text: String) = updateParsed { it.copy(provider = text) }
    fun updatePolicyNumber(text: String) = updateParsed { it.copy(policyNumberText = text) }
    fun updateSumAssured(text: String) = updateParsed { it.copy(sumAssuredText = text) }
    fun updatePremium(text: String) = updateParsed { it.copy(premiumText = text) }
    fun selectFrequency(freq: PremiumFrequency) = updateParsed { it.copy(frequency = freq) }
    fun updateStart(millis: Long) = updateParsed { it.copy(startMillis = millis) }
    fun updateEnd(millis: Long?) = updateParsed { it.copy(endMillis = millis) }
    fun updateNextDue(millis: Long?) = updateParsed { it.copy(nextDueMillis = millis) }
    fun updateNominee(text: String) = updateParsed { it.copy(nominee = text) }
    fun updateTaxSection(text: String) = updateParsed { it.copy(taxSection = text) }

    fun save() {
        val current = _state.value as? InsurancePolicyImportUiState.Parsed ?: return
        if (current.isSaving) return
        updateParsed { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Persist the source doc VERBATIM into filesDir/policy_docs — keeps a real
            // multi-page PDF (not a re-encoded 1-page JPEG) so the user can view all pages.
            val savedDocUri = PolicyDocStore.persist(context, imageUri)
            val insurance = Insurance(
                id = UUID.randomUUID().toString(),
                name = current.name.trim(),
                type = current.typeId,
                provider = current.provider.trim(),
                policyNumber = current.policyNumberText.trim().takeIf { it.isNotBlank() },
                sumAssured = current.sumAssuredText.replace(",", "").toDoubleOrNull() ?: 0.0,
                premiumAmount = current.premiumText.replace(",", "").toDoubleOrNull() ?: 0.0,
                premiumFrequency = current.frequency,
                nextPremiumDate = current.nextDueMillis,
                startDate = current.startMillis,
                endDate = current.endMillis,
                nominee = current.nominee.trim().takeIf { it.isNotBlank() },
                agentContact = null,
                policyDocUri = savedDocUri,
                taxSection = current.taxSection.trim().takeIf { it.isNotBlank() },
                planName = current.planName,
                policyTerm = current.policyTerm,
                lifeAssured = current.lifeAssured,
                uin = current.uin,
                insurerHelpline = current.insurerHelpline,
                detailsJson = current.detailsJson,
                icon = "shield", // same default as the manual InsuranceFormSheet
                color = DEFAULT_COLOR,
                isArchived = false,
                createdAt = now,
            )
            insuranceRepository.upsert(insurance)
            _state.value = InsurancePolicyImportUiState.Saved(insurance.id)
        }
    }

    fun retry() = scan()

    private inline fun updateParsed(
        block: (InsurancePolicyImportUiState.Parsed) -> InsurancePolicyImportUiState.Parsed,
    ) {
        _state.update { if (it is InsurancePolicyImportUiState.Parsed) block(it) else it }
    }

    /** Match the AI's free-text type hint to a catalogue id (by id or label); else first/none. */
    private fun resolveTypeId(hint: String?, types: List<InsuranceTypeOption>): String {
        val h = hint?.trim().orEmpty()
        if (h.isNotEmpty()) {
            types.firstOrNull { it.id.equals(h, ignoreCase = true) || it.label.equals(h, ignoreCase = true) }
                ?.let { return it.id }
            types.firstOrNull { it.label.contains(h, ignoreCase = true) || h.contains(it.label, ignoreCase = true) }
                ?.let { return it.id }
        }
        return types.firstOrNull { it.id == "OTHER" }?.id ?: types.firstOrNull()?.id ?: "OTHER"
    }

    /** Map the AI's frequency hint to the enum; default YEARLY (the manual form's default). */
    private fun resolveFrequency(hint: String?): PremiumFrequency {
        val h = hint?.trim()?.uppercase().orEmpty()
        return PremiumFrequency.entries.firstOrNull { it.name == h } ?: PremiumFrequency.YEARLY
    }

    private fun plainAmount(amount: Double): String =
        // %.2f for the fractional case, not Double.toString() — sum-assured values are often in
        // lakhs/crores and toString() would render them in scientific notation (e.g. 1.2E7).
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else "%.2f".format(amount)

    /** How many rich keys (members/riders/coverage/exclusions/contacts/…) the parse captured. */
    private fun countExtraDetails(detailsJson: String?): Int {
        if (detailsJson.isNullOrBlank()) return 0
        return runCatching {
            val obj = org.json.JSONObject(detailsJson)
            RICH_KEYS.count { obj.has(it) && !obj.isNull(it) && obj.optJSONArray(it)?.length() != 0 }
        }.getOrDefault(0)
    }

    /** Turns raw network exceptions into plain-English guidance (mirrors ShareReceiptViewModel). */
    private fun friendlyError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            e is java.net.UnknownHostException ||
                "No address associated" in msg || "Unable to resolve host" in msg ->
                "Couldn't reach the AI service. If you're on mobile data, try switching to Wi-Fi — or tap Try again in a moment. You can also add the policy manually."
            e is java.net.SocketTimeoutException || "timeout" in msg.lowercase() ->
                "The connection timed out. Check your internet and try again."
            "HTTP 401" in msg || "HTTP 403" in msg ->
                "The AI key was rejected. It may need to be updated."
            "HTTP 429" in msg ->
                "The AI service is busy (rate limit). Wait a moment and try again."
            else -> msg.ifBlank { "Failed to read policy." }
        }
    }

    private companion object {
        // Matches the manual form's first palette colour (0xFF0F766E).
        const val DEFAULT_COLOR = 0xFF0F766EL
        val RICH_KEYS = listOf("members", "riders", "coverage", "exclusions", "contacts", "premiumBreakdown")
    }
}

class InsurancePolicyImportViewModelFactory(
    private val imageUri: Uri,
    private val policyDocParser: PolicyDocParser,
    private val insuranceRepository: InsuranceRepository,
    private val insuranceTypeRepository: InsuranceTypeRepository,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        InsurancePolicyImportViewModel(
            imageUri,
            policyDocParser,
            insuranceRepository,
            insuranceTypeRepository,
            context,
        ) as T
}
