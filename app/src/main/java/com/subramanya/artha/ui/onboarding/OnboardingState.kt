package com.subramanya.artha.ui.onboarding

data class AccountDraft(
    val name: String = "",
    val type: String = "SAVINGS",
    val institution: String = "",
    val openingBalanceText: String = "",
) {
    val isValid: Boolean get() = name.isNotBlank() && parsedBalance != null
    val parsedBalance: Double?
        get() = if (openingBalanceText.isBlank()) 0.0 else openingBalanceText.toDoubleOrNull()
}

data class PendingAccount(
    val name: String,
    val type: String,
    val institution: String?,
    val openingBalance: Double,
)

data class OnboardingUiState(
    val name: String = "",
    val accountDraft: AccountDraft = AccountDraft(),
    val pendingAccounts: List<PendingAccount> = emptyList(),
    val isSaving: Boolean = false,
    val savedAndReady: Boolean = false,
) {
    val canFinishOnboarding: Boolean
        get() = name.isNotBlank() && (pendingAccounts.isNotEmpty() || accountDraft.isValid)
}
