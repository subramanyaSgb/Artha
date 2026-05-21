package com.subramanya.artha

import android.app.Application
import com.subramanya.artha.ai.AiQuickEntryParser
import com.subramanya.artha.ai.GeminiQuickEntryParser
import com.subramanya.artha.data.db.AppDatabase
import com.subramanya.artha.data.db.DatabaseProvider
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.data.repository.TransactionRuleRepository

/**
 * No DI framework in Phase 1. Repositories are lazy-built singletons attached to the
 * Application instance — Composables/ViewModels can reach them via
 * `LocalContext.current.applicationContext as ArthaApplication`.
 */
class ArthaApplication : Application() {

    val database: AppDatabase by lazy { DatabaseProvider.get(this) }

    val settingsPreferences: SettingsPreferences by lazy { SettingsPreferences(this) }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(database.accountDao(), database.transactionDao())
    }
    val cardRepository: CardRepository by lazy {
        CardRepository(database.cardDao(), database.transactionDao())
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
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }
    val investmentRepository: InvestmentRepository by lazy {
        InvestmentRepository(database.investmentDao(), database.transactionDao())
    }
    val insuranceRepository: InsuranceRepository by lazy {
        InsuranceRepository(database.insuranceDao())
    }
    val transactionRuleRepository: TransactionRuleRepository by lazy {
        TransactionRuleRepository(database.transactionRuleDao())
    }

    /** Backed by [GeminiQuickEntryParser] reading BuildConfig.GEMINI_API_KEY.
     *  An empty key triggers the parser's NoApiKey short-circuit so the UI shows a
     *  hint instead of crashing. */
    val aiQuickEntryParser: AiQuickEntryParser by lazy {
        GeminiQuickEntryParser(apiKey = BuildConfig.GEMINI_API_KEY)
    }
}
