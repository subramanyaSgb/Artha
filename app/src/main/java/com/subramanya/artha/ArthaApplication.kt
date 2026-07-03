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

    /**
     * The NVIDIA NIM API key used by EVERY AI task (AI Quick Entry + UPI receipt parsing).
     * Baked into the build from local.properties → [BuildConfig.NIM_API_KEY]. A non-blank
     * value stored in DataStore still wins (legacy/override path), but there is no in-app
     * key UI anymore, so in practice this returns the baked key.
     */
    suspend fun nimApiKey(): String {
        val stored = settingsPreferences.geminiApiKey.first()
        return stored.ifBlank { BuildConfig.NIM_API_KEY }
    }

    /** Backed by [NvidiaNimQuickEntryParser], keyed by the single baked [nimApiKey]. */
    val aiQuickEntryParser: AiQuickEntryParser by lazy {
        NvidiaNimQuickEntryParser(keyProvider = { nimApiKey() })
    }
}
