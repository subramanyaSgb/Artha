package com.subramanya.artha.data.db

import androidx.room.TypeConverter
import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType

class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromCardType(v: CardType): String = v.name
    @TypeConverter fun toCardType(v: String): CardType = CardType.valueOf(v)

    @TypeConverter fun fromCardNetwork(v: CardNetwork): String = v.name
    @TypeConverter fun toCardNetwork(v: String): CardNetwork = CardNetwork.valueOf(v)

    @TypeConverter fun fromCategoryType(v: CategoryType): String = v.name
    @TypeConverter fun toCategoryType(v: String): CategoryType = CategoryType.valueOf(v)

    @TypeConverter fun fromPaymentApp(v: PaymentApp): String = v.name
    @TypeConverter fun toPaymentApp(v: String): PaymentApp = PaymentApp.valueOf(v)

    @TypeConverter fun fromPersonRelation(v: PersonRelation): String = v.name
    @TypeConverter fun toPersonRelation(v: String): PersonRelation = PersonRelation.valueOf(v)

    @TypeConverter fun fromSourceKind(v: SourceKind): String = v.name
    @TypeConverter fun toSourceKind(v: String): SourceKind = SourceKind.valueOf(v)

    @TypeConverter fun fromSourceKindNullable(v: SourceKind?): String? = v?.name
    @TypeConverter fun toSourceKindNullable(v: String?): SourceKind? = v?.let(SourceKind::valueOf)

    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromTransactionSource(v: TransactionSource): String = v.name
    @TypeConverter fun toTransactionSource(v: String): TransactionSource = TransactionSource.valueOf(v)
}
