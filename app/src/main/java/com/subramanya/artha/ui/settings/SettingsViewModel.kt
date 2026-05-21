package com.subramanya.artha.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.db.AppDatabase
import com.subramanya.artha.data.db.seed.SeedCategories
import com.subramanya.artha.data.importing.BankImporter
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.preferences.SpouseTransactionDefault
import com.subramanya.artha.data.preferences.ThemeMode
import com.subramanya.artha.security.BackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SettingsUiState(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val spouseDefault: SpouseTransactionDefault = SpouseTransactionDefault.ASK,
    val dashboardShowMonthly: Boolean = true,
    val dashboardShowAccounts: Boolean = true,
    val dashboardShowCards: Boolean = true,
    val dashboardShowRecent: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val smsAutoImportEnabled: Boolean = false,
    val showFirstResetDialog: Boolean = false,
    val showFinalResetDialog: Boolean = false,
    val showWipeImportConfirm: Boolean = false,
    val isImportingBundled: Boolean = false,
    /** Set right after Export → file is ready to be shared. */
    val pendingExportFile: File? = null,
)

/** Account names the bank-statement importer creates. Wipe-imports keys off these. */
private val IMPORTED_ACCOUNT_NAMES = setOf("Federal Bank (Jupiter)", "ICICI Bank")
private const val IMPORTED_NOTES_PREFIX = "Imported from "

class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
    private val database: AppDatabase,
) : ViewModel() {

    private val firstResetDialog = MutableStateFlow(false)
    private val finalResetDialog = MutableStateFlow(false)
    private val wipeImportConfirm = MutableStateFlow(false)
    private val importingBundled = MutableStateFlow(false)
    private val pendingExport = MutableStateFlow<File?>(null)

    private val dashboardPrefs = combine(
        settingsPreferences.dashboardShowMonthly,
        settingsPreferences.dashboardShowAccounts,
        settingsPreferences.dashboardShowCards,
        settingsPreferences.dashboardShowRecent,
    ) { monthly, accounts, cards, recent -> DashboardVisibility(monthly, accounts, cards, recent) }

    private val securityPrefs = combine(
        settingsPreferences.biometricLockEnabled,
        settingsPreferences.smsAutoImportEnabled,
    ) { lock, sms -> SecurityPrefs(lock, sms) }

    private val dialogFlags = combine(
        firstResetDialog, finalResetDialog, wipeImportConfirm, pendingExport, dashboardPrefs,
    ) { first, final, wipe, export, vis ->
        DialogsAndVisibility(first, final, wipe, export, vis, importing = false, security = SecurityPrefs(false, false))
    }.combine(importingBundled) { bag, importing ->
        bag.copy(importing = importing)
    }.combine(securityPrefs) { bag, security ->
        bag.copy(security = security)
    }

    val state: StateFlow<SettingsUiState> = combine(
        settingsPreferences.userName,
        settingsPreferences.themeMode,
        settingsPreferences.useDynamicColor,
        settingsPreferences.spouseTransactionDefault,
        dialogFlags,
    ) { name, theme, dynamic, spouse, bag ->
        SettingsUiState(
            userName = name,
            themeMode = theme,
            useDynamicColor = dynamic,
            spouseDefault = spouse,
            dashboardShowMonthly = bag.visibility.monthly,
            dashboardShowAccounts = bag.visibility.accounts,
            dashboardShowCards = bag.visibility.cards,
            dashboardShowRecent = bag.visibility.recent,
            showFirstResetDialog = bag.firstReset,
            showFinalResetDialog = bag.finalReset,
            showWipeImportConfirm = bag.wipeImport,
            isImportingBundled = bag.importing,
            pendingExportFile = bag.export,
            biometricLockEnabled = bag.security.biometric,
            smsAutoImportEnabled = bag.security.smsImport,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private data class DashboardVisibility(val monthly: Boolean, val accounts: Boolean, val cards: Boolean, val recent: Boolean)
    private data class SecurityPrefs(val biometric: Boolean, val smsImport: Boolean)
    private data class DialogsAndVisibility(
        val firstReset: Boolean,
        val finalReset: Boolean,
        val wipeImport: Boolean,
        val export: File?,
        val visibility: DashboardVisibility,
        val importing: Boolean,
        val security: SecurityPrefs,
    )

    fun onNameChanged(value: String) {
        viewModelScope.launch { settingsPreferences.setUserName(value) }
    }

    fun onThemeChanged(mode: ThemeMode) {
        viewModelScope.launch { settingsPreferences.setThemeMode(mode) }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setUseDynamicColor(enabled) }
    }

    fun onSpouseDefaultChanged(value: SpouseTransactionDefault) {
        viewModelScope.launch { settingsPreferences.setSpouseTransactionDefault(value) }
    }

    fun resetSpousePrompt() {
        viewModelScope.launch { settingsPreferences.resetSpouseTransactionDefault() }
    }

    fun onDashboardShowMonthlyChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowMonthly(enabled) }
    }
    fun onDashboardShowAccountsChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowAccounts(enabled) }
    }
    fun onDashboardShowCardsChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowCards(enabled) }
    }
    fun onDashboardShowRecentChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowRecent(enabled) }
    }

    fun onBiometricLockChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setBiometricLockEnabled(enabled) }
    }

    fun onSmsAutoImportChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setSmsAutoImportEnabled(enabled) }
    }

    // ----- export -----

    /**
     * Builds a JSON snapshot of every Room table and writes it to `context.cacheDir`.
     * The on-screen state flips to `pendingExportFile = <file>`, which the host can use
     * to launch an ACTION_SEND chooser. We use the cache dir + `FileProvider` later via
     * the host; for Phase 1 the simpler path of saving to externalCacheDir + Intent.EXTRA_STREAM
     * is enough since most chooser targets (Drive / email / Gmail) accept file:// from
     * external cache without FileProvider plumbing on modern Android share intents.
     */
    fun exportData(context: Context) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val root = JSONObject()
                root.put("exported_at", System.currentTimeMillis())
                root.put("accounts", database.accountDao().observeAll().firstSnapshot().toJsonArray { acct ->
                    JSONObject().apply {
                        put("id", acct.id); put("name", acct.name); put("type", acct.type.name)
                        put("institution", acct.institution); put("last4", acct.accountNumberLast4)
                        put("opening_balance", acct.openingBalance); put("currency", acct.currency)
                        put("icon", acct.icon); put("color", acct.color)
                        put("is_archived", acct.isArchived); put("display_order", acct.displayOrder)
                        put("created_at", acct.createdAt)
                    }
                })
                root.put("cards", database.cardDao().observeAll().firstSnapshot().toJsonArray { card ->
                    JSONObject().apply {
                        put("id", card.id); put("name", card.name); put("type", card.type.name)
                        put("issuer", card.issuer); put("network", card.network.name)
                        put("last4", card.cardNumberLast4); put("credit_limit", card.creditLimit)
                        put("statement_day", card.statementDayOfMonth); put("due_day", card.dueDayOfMonth)
                        put("linked_account_id", card.linkedAccountId)
                        put("icon", card.icon); put("color", card.color)
                        put("is_archived", card.isArchived); put("display_order", card.displayOrder)
                        put("created_at", card.createdAt)
                    }
                })
                root.put("categories", database.categoryDao().observeAll().firstSnapshot().toJsonArray { cat ->
                    JSONObject().apply {
                        put("id", cat.id); put("name", cat.name); put("parent_id", cat.parentId)
                        put("type", cat.type.name); put("icon", cat.icon); put("color", cat.color)
                        put("is_system", cat.isSystem); put("display_order", cat.displayOrder)
                    }
                })
                root.put("people", database.personDao().observeAll().firstSnapshot().toJsonArray { p ->
                    JSONObject().apply {
                        put("id", p.id); put("name", p.name); put("relation", p.relation.name)
                        put("contact", p.contact); put("avatar_uri", p.avatarUri)
                        put("created_at", p.createdAt)
                    }
                })
                root.put("tags", database.tagDao().observeAll().firstSnapshot().toJsonArray { t ->
                    JSONObject().apply { put("id", t.id); put("name", t.name); put("color", t.color) }
                })
                root.put("transactions", database.transactionDao().observeAll().firstSnapshot().toJsonArray { txn ->
                    JSONObject().apply {
                        put("id", txn.id); put("type", txn.type.name); put("amount", txn.amount)
                        put("currency", txn.currency); put("date", txn.date)
                        put("description", txn.description)
                        put("category_id", txn.categoryId); put("sub_category_id", txn.subCategoryId)
                        put("source_type", txn.sourceType.name); put("source_id", txn.sourceId)
                        put("destination_type", txn.destinationType?.name); put("destination_id", txn.destinationId)
                        put("payment_app", txn.paymentApp.name)
                        put("place", txn.place); put("latitude", txn.latitude); put("longitude", txn.longitude)
                        put("receipt_uri", txn.receiptUri); put("notes", txn.notes)
                        put("tax_section", txn.taxSection)
                        put("recurring_rule_id", txn.recurringRuleId)
                        put("is_split", txn.isSplit); put("split_group_id", txn.splitGroupId)
                        put("source", txn.source.name)
                        put("created_at", txn.createdAt); put("updated_at", txn.updatedAt)
                    }
                })

                val out = File(context.cacheDir, "artha_export_${System.currentTimeMillis()}.json")
                out.writeText(root.toString(2))
                out
            }
            pendingExport.update { file }
        }
    }

    /**
     * Same as [exportData] but PBKDF2-AES-GCM encrypts the JSON with [password] first.
     * Caller passes an in-memory CharArray (not a String) so the secret can be wiped
     * after use — best practice for passwords.
     */
    fun exportDataEncrypted(context: Context, password: CharArray) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val root = JSONObject()
                root.put("exported_at", System.currentTimeMillis())
                root.put("encrypted", true)
                // Build full JSON (same shape as plain export) then encrypt the whole blob.
                root.put("accounts", database.accountDao().observeAll().firstSnapshot().toJsonArray { acct ->
                    JSONObject().apply {
                        put("id", acct.id); put("name", acct.name); put("type", acct.type.name)
                        put("institution", acct.institution); put("last4", acct.accountNumberLast4)
                        put("opening_balance", acct.openingBalance); put("currency", acct.currency)
                        put("icon", acct.icon); put("color", acct.color)
                        put("is_archived", acct.isArchived); put("display_order", acct.displayOrder)
                        put("created_at", acct.createdAt)
                    }
                })
                root.put("transactions", database.transactionDao().observeAll().firstSnapshot().toJsonArray { txn ->
                    JSONObject().apply {
                        put("id", txn.id); put("type", txn.type.name); put("amount", txn.amount)
                        put("currency", txn.currency); put("date", txn.date)
                        put("description", txn.description)
                        put("category_id", txn.categoryId); put("sub_category_id", txn.subCategoryId)
                        put("source_type", txn.sourceType.name); put("source_id", txn.sourceId)
                        put("destination_type", txn.destinationType?.name); put("destination_id", txn.destinationId)
                        put("payment_app", txn.paymentApp.name)
                        put("notes", txn.notes); put("tax_section", txn.taxSection)
                        put("created_at", txn.createdAt); put("updated_at", txn.updatedAt)
                    }
                })
                val plain = root.toString()
                val cipherText = BackupCrypto.encrypt(plain, password)
                password.fill(' ') // wipe in-memory copy
                val out = File(context.cacheDir, "artha_backup_${System.currentTimeMillis()}.artha")
                out.writeText(cipherText)
                out
            }
            pendingExport.update { file }
        }
    }

    fun acknowledgeExport() {
        pendingExport.update { null }
    }

    // ----- reset -----

    fun requestReset() = firstResetDialog.update { true }
    fun dismissFirstReset() = firstResetDialog.update { false }
    fun proceedToFinalReset() {
        firstResetDialog.update { false }
        finalResetDialog.update { true }
    }
    fun dismissFinalReset() = finalResetDialog.update { false }

    fun confirmReset(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                // Re-seed the categories the way DatabaseProvider's callback would on first launch.
                database.categoryDao().upsertAll(SeedCategories.all())
            }
            settingsPreferences.resetSpouseTransactionDefault()
            finalResetDialog.update { false }
            onDone()
        }
    }

    // ----- import bundled bank-statement data (APK asset) -----

    /**
     * Reads `assets/seed/bank_import.json` (generated offline by the Python
     * script) and writes the 3,913 transactions + their two source accounts
     * into Room. Idempotent — calling twice is a no-op thanks to deterministic
     * UUID5 transaction IDs + IGNORE-on-conflict.
     *
     * Callers get the result via [onDone] so they can surface a toast.
     */
    fun importBundledBankData(context: Context, onDone: (BankImporter.Result) -> Unit) {
        if (importingBundled.value) return
        viewModelScope.launch {
            importingBundled.update { true }
            val result = withContext(Dispatchers.IO) {
                BankImporter(context.applicationContext, database).importBundled()
            }
            importingBundled.update { false }
            onDone(result)
        }
    }

    // ----- wipe imported bank-statement data -----

    fun requestWipeImport() = wipeImportConfirm.update { true }
    fun dismissWipeImport() = wipeImportConfirm.update { false }

    /**
     * Removes only the data the external bank-statement importer added:
     *   - every transaction whose notes start with "Imported from "
     *   - every account whose name matches one of [IMPORTED_ACCOUNT_NAMES]
     *
     * Manual entries stay untouched. Returns counts via [onDone] so the host can
     * surface a toast.
     */
    fun wipeImportedData(onDone: (deletedTxns: Int, deletedAccounts: Int) -> Unit) {
        viewModelScope.launch {
            val (txnCount, acctCount) = withContext(Dispatchers.IO) {
                val txnDao = database.transactionDao()
                val acctDao = database.accountDao()
                val allTxns = txnDao.observeAll().first()
                val importedTxnIds = allTxns
                    .filter { (it.notes ?: "").startsWith(IMPORTED_NOTES_PREFIX) }
                    .map { it.id }
                if (importedTxnIds.isNotEmpty()) txnDao.deleteByIds(importedTxnIds)

                val allAccts = acctDao.observeAll().first()
                val importedAccts = allAccts.filter { it.name in IMPORTED_ACCOUNT_NAMES }
                importedAccts.forEach { acctDao.delete(it) }

                importedTxnIds.size to importedAccts.size
            }
            wipeImportConfirm.update { false }
            onDone(txnCount, acctCount)
        }
    }
}

/** Pulls a one-shot snapshot from a Flow for the export. */
private suspend fun <T> Flow<T>.firstSnapshot(): T = first()

private inline fun <T> List<T>.toJsonArray(builder: (T) -> JSONObject): JSONArray {
    val arr = JSONArray()
    for (item in this) arr.put(builder(item))
    return arr
}

class SettingsViewModelFactory(
    private val settingsPreferences: SettingsPreferences,
    private val database: AppDatabase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return SettingsViewModel(settingsPreferences, database) as T
    }
}
