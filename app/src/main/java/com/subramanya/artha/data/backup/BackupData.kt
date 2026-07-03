package com.subramanya.artha.data.backup

import com.subramanya.artha.data.entity.AccountEntity
import com.subramanya.artha.data.entity.BudgetEntity
import com.subramanya.artha.data.entity.CardEntity
import com.subramanya.artha.data.entity.CategoryEntity
import com.subramanya.artha.data.entity.GoalEntity
import com.subramanya.artha.data.entity.InsuranceEntity
import com.subramanya.artha.data.entity.InvestmentEntity
import com.subramanya.artha.data.entity.PersonEntity
import com.subramanya.artha.data.entity.RecurringRuleEntity
import com.subramanya.artha.data.entity.SubscriptionEntity
import com.subramanya.artha.data.entity.TagEntity
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.AccountTypeEntity
import com.subramanya.artha.data.entity.CardTypeEntity
import com.subramanya.artha.data.entity.InsuranceTypeEntity
import com.subramanya.artha.data.entity.PaymentAppEntity
import com.subramanya.artha.data.entity.TransactionPersonCrossRef
import com.subramanya.artha.data.entity.TransactionRuleEntity
import com.subramanya.artha.data.entity.TransactionTagCrossRef

/**
 * An in-memory snapshot of EVERY Room table — the unit that [BackupCodec] encodes and
 * decodes and that [BackupRepository] reads from / writes to the database.
 *
 * The field order mirrors the FK dependency order used on restore (parents first,
 * transactions next, cross-refs last); see [BackupRepository.restore]. All entity
 * classes are Kotlin data classes, so structural equality holds — that's what the
 * codec round-trip test asserts on.
 */
data class BackupData(
    val accounts: List<AccountEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val insurances: List<InsuranceEntity> = emptyList(),
    val transactionRules: List<TransactionRuleEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionPeople: List<TransactionPersonCrossRef> = emptyList(),
    val transactionTags: List<TransactionTagCrossRef> = emptyList(),
    /** Phase 2: payment-app catalogue — only user-added/modified rows need backup;
     *  built-ins are re-seeded on fresh install and the migration, so we serialise all
     *  rows to restore user customisations (hidden built-ins, custom apps). */
    val paymentApps: List<PaymentAppEntity> = emptyList(),
    val accountTypes: List<AccountTypeEntity> = emptyList(),
    val cardTypes: List<CardTypeEntity> = emptyList(),
    val insuranceTypes: List<InsuranceTypeEntity> = emptyList(),
    /** Schema v2: DataStore settings snapshot. Null when restoring a v1 backup —
     *  the restorer then leaves the device's current settings untouched. */
    val settings: BackupSettings? = null,
)
