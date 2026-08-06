package com.subramanya.artha

import android.app.Application
import com.subramanya.artha.ai.AiQuickEntryParser
import com.subramanya.artha.worker.RecurringFireWorker
import com.subramanya.artha.ai.NvidiaNimQuickEntryParser
import com.subramanya.artha.data.db.AppDatabase
import com.subramanya.artha.data.db.DatabaseProvider
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.BudgetRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.GoalRepository
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.AccountTypeRepository
import com.subramanya.artha.data.repository.CardTypeRepository
import com.subramanya.artha.data.repository.InsuranceTypeRepository
import com.subramanya.artha.data.repository.PaymentAppRepository
import com.subramanya.artha.data.repository.PendingTransactionRepository
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.RecurringRuleRepository
import com.subramanya.artha.data.repository.SubscriptionRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.data.repository.TransactionRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/**
 * No DI framework in Phase 1. Repositories are lazy-built singletons attached to the
 * Application instance — Composables/ViewModels can reach them via
 * `LocalContext.current.applicationContext as ArthaApplication`.
 */
class ArthaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        RecurringFireWorker.schedule(this)
        // Notification channel for the SMS review "N to review" ongoing notification.
        com.subramanya.artha.sms.PendingTransactionNotifier.ensureChannel(this)
    }

    val database: AppDatabase by lazy { DatabaseProvider.get(this) }

    val settingsPreferences: SettingsPreferences by lazy { SettingsPreferences(this) }

    /**
     * Process-lifetime scope for repository-held shared flows (`shareIn`). Lives as long as the
     * app does — the shared balance flows use `WhileSubscribed`, so they only do work while a
     * screen is collecting, and idle otherwise.
     */
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val accountRepository: AccountRepository by lazy {
        AccountRepository(database.accountDao(), database.transactionDao(), appScope)
    }
    val cardRepository: CardRepository by lazy {
        CardRepository(database.cardDao(), database.transactionDao(), appScope)
    }
    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }
    val personRepository: PersonRepository by lazy {
        PersonRepository(database.personDao())
    }
    val tagRepository: TagRepository by lazy {
        TagRepository(database.tagDao())
    }
    val paymentAppRepository: PaymentAppRepository by lazy {
        PaymentAppRepository(database.paymentAppDao())
    }
    val accountTypeRepository: AccountTypeRepository by lazy {
        AccountTypeRepository(database.accountTypeDao())
    }
    val cardTypeRepository: CardTypeRepository by lazy {
        CardTypeRepository(database.cardTypeDao())
    }
    val insuranceTypeRepository: InsuranceTypeRepository by lazy {
        InsuranceTypeRepository(database.insuranceTypeDao())
    }
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }
    val investmentRepository: InvestmentRepository by lazy {
        InvestmentRepository(database.investmentDao(), database.transactionDao(), appScope)
    }
    val insuranceRepository: InsuranceRepository by lazy {
        InsuranceRepository(database.insuranceDao(), database.investmentDao())
    }
    val transactionRuleRepository: TransactionRuleRepository by lazy {
        TransactionRuleRepository(database.transactionRuleDao())
    }

    // Phase 4
    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao(), transactionRepository)
    }
    val goalRepository: GoalRepository by lazy {
        GoalRepository(database.goalDao(), accountRepository, investmentRepository)
    }
    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(database.subscriptionDao())
    }
    val recurringRuleRepository: RecurringRuleRepository by lazy {
        RecurringRuleRepository(database.recurringRuleDao())
    }

    // SMS auto-import (ported from fork) — review queue
    val pendingTransactionRepository: PendingTransactionRepository by lazy {
        PendingTransactionRepository(database.pendingTransactionDao())
    }

    /**
     * The NVIDIA NIM API key. Baked from local.properties → [BuildConfig.NIM_API_KEY].
     * A non-blank DataStore value still wins (legacy override), but there is no in-app key UI.
     */
    suspend fun nimApiKey(): String {
        val stored = settingsPreferences.geminiApiKey.first()
        return stored.ifBlank { BuildConfig.NIM_API_KEY }
    }

    /** OpenRouter fallback key — baked from local.properties → [BuildConfig.OPENROUTER_API_KEY]. */
    fun openRouterApiKey(): String = BuildConfig.OPENROUTER_API_KEY

    /** Groq key (primary receipt-vision provider) — baked from local.properties → [BuildConfig.GROQ_API_KEY]. */
    fun groqApiKey(): String = BuildConfig.GROQ_API_KEY

    /**
     * Backup Groq keys from separate accounts — each has its own 8K-tokens/min quota. A
     * multi-page policy PDF can exhaust one key's per-minute budget, so the parser falls
     * through these before the slower non-Groq providers. Blank ones are skipped.
     */
    fun groqApiKeysBackup(): List<String> =
        listOf(BuildConfig.GROQ_API_KEY_2, BuildConfig.GROQ_API_KEY_3, BuildConfig.GROQ_API_KEY_4)
            .filter { it.isNotBlank() }

    /** RoutesMe key (second receipt-vision provider) — baked from local.properties → [BuildConfig.ROUTESME_API_KEY]. */
    fun routesMeApiKey(): String = BuildConfig.ROUTESME_API_KEY

    /** Backed by [NvidiaNimQuickEntryParser], keyed by the single baked [nimApiKey]. */
    val aiQuickEntryParser: AiQuickEntryParser by lazy {
        NvidiaNimQuickEntryParser(keyProvider = { nimApiKey() })
    }
}
