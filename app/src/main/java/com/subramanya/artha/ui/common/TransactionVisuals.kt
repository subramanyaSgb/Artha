package com.subramanya.artha.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.incomeSoftFill
import com.subramanya.artha.utils.MaterialIcons

/**
 * Shared transaction visuals — the single source for "which icon does this
 * transaction get". Every screen that lists transactions should render rows
 * through these so a category's icon/colour looks identical everywhere
 * (Dashboard, Ledger, Account/Card detail, Transaction detail).
 */

/** Income-like types: money came in. Mirrors the money-flow classification. */
fun TransactionType.isIncomeLike(): Boolean = this in setOf(
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
)

/** Fallback glyph for category-less transactions (transfers, card payments, …). */
fun transactionTypeIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> Icons.AutoMirrored.Filled.ShowChart
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
    -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
    -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.ADJUSTMENT -> Icons.AutoMirrored.Filled.ReceiptLong
}

/** "Investment Buy" instead of the raw SHOUTING enum name. */
fun transactionTypeLabel(type: TransactionType): String =
    type.name.split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

/**
 * The rounded category avatar from the Dashboard recent list: the category's real
 * icon on its colour, falling back to a type icon on a soft fill. [category] is
 * `categoriesById[txn.categoryId]`.
 */
@Composable
fun TransactionCategoryAvatar(
    category: Category?,
    type: TransactionType,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = 11.dp,
    iconSize: Dp = 17.dp,
) {
    val isIncome = type.isIncomeLike()
    val avatarColor = category?.let { Color(it.color) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                avatarColor
                    ?: if (isIncome) incomeSoftFill() else MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category?.let { MaterialIcons.resolve(it.icon) } ?: transactionTypeIcon(type),
            contentDescription = null,
            tint = when {
                avatarColor != null -> Color.White
                isIncome -> Income
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(iconSize),
        )
    }
}
