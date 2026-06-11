package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.TransactionCategoryAvatar
import com.subramanya.artha.ui.common.isIncomeLike
import com.subramanya.artha.ui.common.transactionTypeLabel
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.ui.transaction.FundsEndpoint
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.ReceiptStore
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: TransactionDetailViewModel = viewModel(
        factory = TransactionDetailViewModelFactory(
            transactionId = transactionId,
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            categoryRepository = app.categoryRepository,
            personRepository = app.personRepository,
            tagRepository = app.tagRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val txn = state.transaction
            com.subramanya.artha.ui.common.InlineTopBar(
                title = stringResource(R.string.txn_detail_title),
                onBack = onBack,
                trailing = {
                    if (txn != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.txn_detail_action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { vm.duplicate(onDuplicated = onBack) }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.txn_detail_action_duplicate),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.txn_detail_action_delete),
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        }
                    }
                },
            )
            if (txn == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Hero(state = state, txn = txn)
                    Spacer(Modifier.height(20.dp))
                    FlowCard(state = state, txn = txn)
                    DetailsCard(state = state, txn = txn)
                    ReceiptSection(txn = txn)
                    NotesSection(txn = txn)
                    Spacer(Modifier.height(20.dp))
                    AuditRow(txn = txn)
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.txn_detail_delete_confirm_title),
            text = stringResource(R.string.txn_detail_delete_confirm_body),
            confirmLabel = stringResource(R.string.txn_detail_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = { vm.confirmDelete(onDeleted = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }

    if (editing) {
        val txn = state.transaction
        val source = txn?.let {
            it.sourceId?.let { id ->
                FundsEndpoint(
                    kind = it.sourceType,
                    id = id,
                    displayName = state.sourceName ?: id,
                    isCreditCard = state.sourceIsCreditCard,
                )
            }
        }
        val destination = txn?.let { t ->
            val id = t.destinationId
            val kind = t.destinationType
            if (id != null && kind != null) {
                FundsEndpoint(
                    kind = kind,
                    id = id,
                    displayName = state.destinationName ?: id,
                    isCreditCard = state.destinationIsCreditCard,
                )
            } else null
        }
        val txnVm: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModelFactory(
                accountRepository = app.accountRepository,
                cardRepository = app.cardRepository,
                categoryRepository = app.categoryRepository,
                personRepository = app.personRepository,
                tagRepository = app.tagRepository,
                transactionRepository = app.transactionRepository,
                transactionRuleRepository = app.transactionRuleRepository,
                investmentRepository = app.investmentRepository,
                settingsPreferences = app.settingsPreferences,
                paymentAppRepository = app.paymentAppRepository,
            ),
        )
        LaunchedEffect(txn?.id) {
            if (txn != null) {
                txnVm.applyEditPrefill(
                    transaction = txn,
                    source = source,
                    destination = destination,
                    categoryDisplay = state.categoryName,
                    subCategoryDisplay = state.subCategoryName,
                )
            }
        }
        AddTransactionSheet(viewModel = txnVm, onDismiss = { editing = false })
    }
}

// ---------------- pieces ----------------

/**
 * Centered hero: the category avatar (real icon + colour), the signed amount,
 * the description, and a type · date meta line. Mirrors how the row looked in
 * the list the user tapped, just bigger.
 */
@Composable
private fun Hero(state: TransactionDetailUiState, txn: Transaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TransactionCategoryAvatar(
            category = state.category,
            type = txn.type,
            size = 64.dp,
            cornerRadius = 20.dp,
            iconSize = 30.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = signedAmount(txn),
            style = ArthaAmountStyles.display.copy(fontWeight = FontWeight.SemiBold),
            color = if (txn.type.isIncomeLike()) Income else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = txn.description.ifBlank { transactionTypeLabel(txn.type) },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${transactionTypeLabel(txn.type)} · ${formatDate(txn.date)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** From → To strip for anything that moved money between named endpoints. */
@Composable
private fun FlowCard(state: TransactionDetailUiState, txn: Transaction) {
    val from = state.sourceName ?: return
    val to = state.destinationName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.txn_detail_field_source).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = from,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (to != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(18.dp),
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.txn_detail_field_destination).uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = to,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** Icon-led key/value rows for everything else on the transaction. */
@Composable
private fun DetailsCard(state: TransactionDetailUiState, txn: Transaction) {
    val rows = buildList {
        state.categoryName?.let { name ->
            val value = state.subCategoryName?.let { "$name › $it" } ?: name
            add(DetailItem(Icons.Filled.Category, R.string.txn_detail_field_category, value))
        }
        add(
            DetailItem(
                Icons.Filled.Payments,
                R.string.txn_detail_field_payment_app,
                txn.paymentApp.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
            ),
        )
        if (state.peopleNames.isNotEmpty()) {
            add(DetailItem(Icons.Filled.Group, R.string.txn_detail_field_people, state.peopleNames.joinToString(", ")))
        }
        if (!txn.place.isNullOrBlank()) {
            add(DetailItem(Icons.Filled.Place, R.string.txn_detail_field_place, txn.place))
        }
        if (state.tagNames.isNotEmpty()) {
            add(DetailItem(Icons.Filled.Sell, R.string.txn_detail_field_tags, state.tagNames.joinToString(", ")))
        }
    }
    if (rows.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        rows.forEachIndexed { i, row ->
            DetailRowItem(row)
            if (i < rows.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

private data class DetailItem(
    val icon: ImageVector,
    val labelRes: Int,
    val value: String,
)

@Composable
private fun DetailRowItem(item: DetailItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun ReceiptSection(txn: Transaction) {
    val uri = txn.receiptUri
    if (uri.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.txn_detail_section_receipt).uppercase(),
            style = EyebrowStyle,
            color = Text3,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        ReceiptImage(uri = uri)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun NotesSection(txn: Transaction) {
    val notes = txn.notes
    if (notes.isNullOrBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.txn_detail_field_notes).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AuditRow(txn: Transaction) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.txn_detail_audit_created, formatDate(txn.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (txn.updatedAt != txn.createdAt) {
            Text(
                text = stringResource(R.string.txn_detail_audit_updated, formatDate(txn.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.txn_detail_audit_source, txn.source.name.lowercase().replaceFirstChar { it.titlecase() }),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------- helpers ----------------

private fun formatDate(millis: Long): String {
    val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val date = DateFormatter.shortDate(ldt.date)
    return "$date %02d:%02d".format(ldt.hour, ldt.minute)
}

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when (txn.type) {
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
        TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
        -> "+$abs"
        TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
        -> "−$abs"
        else -> abs
    }
}

/**
 * Shows the receipt image, decoded OFF the main thread. Falls back to a quiet
 * "preview unavailable" row when the bytes can't be read (e.g. a legacy
 * content:// URI whose grant died with the process — new receipts are copied
 * into app storage by [ReceiptStore] and always resolve).
 */
@Composable
private fun ReceiptImage(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = ReceiptStore.loadBitmap(context, uri)
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = stringResource(R.string.txn_detail_section_receipt),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.txn_detail_receipt_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
