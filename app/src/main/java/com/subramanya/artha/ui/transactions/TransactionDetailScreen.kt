@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import com.subramanya.artha.ui.theme.Danger
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.ExpenseSoft
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Indigo
import com.subramanya.artha.ui.theme.IndigoDeep
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.IncomeSoft
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line2
import com.subramanya.artha.ui.theme.Line3
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.OchreSoft
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal950
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.TiroDevanagariHindi
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

// Paper color — the receipt note background, #14211b
private val Paper = Color(0xFF14211B)

// Per-type stamp metadata
private data class TypeMeta(
    val stampWord: String,
    val amountColor: Color,
    val accentColor: Color,
    val softColor: Color,
)

@Composable
private fun typeMeta(type: TransactionType): TypeMeta = when {
    type.isIncomeLike() -> TypeMeta("Credited", Income, Income, IncomeSoft)
    type == TransactionType.TRANSFER || type == TransactionType.CARD_PAYMENT ->
        TypeMeta("Transferred", Indigo, Indigo, IndigoDeep)
    type == TransactionType.INVESTMENT_BUY || type == TransactionType.INVESTMENT_SELL ->
        TypeMeta("Invested", OchreSoft, Ochre, Teal950)
    else -> TypeMeta("Debited", Text1, com.subramanya.artha.ui.theme.Expense, ExpenseSoft)
}

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

    // Photo picker must live at screen level so it survives recomposition of child composables.
    val scope = rememberCoroutineScope()
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = com.subramanya.artha.utils.ReceiptStore.persist(context, uri)
                if (saved != null) vm.attachReceipt(saved)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.txn_detail_back),
                        tint = Text1,
                    )
                }
                Text(
                    text = stringResource(R.string.txn_receipt_eyebrow).uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                // Balancing spacer so the title stays centred
                Spacer(Modifier.size(48.dp))
            }

            val txn = state.transaction
            if (txn == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    val meta = typeMeta(txn.type)
                    ReceiptCard(txn = txn, state = state, meta = meta)
                    ActionRow(
                        onEdit = { editing = true },
                        onDelete = vm::requestDelete,
                        onAttachPhoto = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
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

// ─────────────────────────────────────────────────────────────
// Receipt card — the scalloped note-paper container
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReceiptCard(
    txn: Transaction,
    state: TransactionDetailUiState,
    meta: TypeMeta,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            // Drop shadow via graphicsLayer
            .graphicsLayer {
                shadowElevation = 38.dp.toPx()
                shape = RoundedCornerShape(4.dp)
                clip = false
            }
            .scalloped(pageColor = Surface1)
            .background(Paper),
    ) {
        Column {
            // ── Header stub ──────────────────────────────────
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 8.dp)) {
                MerchantStub(txn = txn, state = state, meta = meta)
                Spacer(Modifier.height(22.dp))
                HeroRow(txn = txn, meta = meta)
            }

            // ── Perforation ──────────────────────────────────
            Perforation(pageColor = Surface1)

            // ── Itemized body ────────────────────────────────
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 4.dp, bottom = 8.dp)) {
                val ldt = remember(txn.date) {
                    Instant.fromEpochMilliseconds(txn.date).toLocalDateTime(TimeZone.currentSystemDefault())
                }
                val dateStr = remember(ldt) { DateFormatter.shortDate(ldt.date) }
                val timeStr = remember(ldt) { "%02d:%02d".format(ldt.hour, ldt.minute) }
                val longDate = remember(ldt) {
                    "${ldt.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, " +
                        "${ldt.dayOfMonth} " +
                        "${ldt.month.name.lowercase().replaceFirstChar { it.uppercase() }} " +
                        "${ldt.year}"
                }

                PrintRow(label = stringResource(R.string.txn_receipt_row_date), value = longDate)
                PrintRow(label = stringResource(R.string.txn_receipt_row_time), value = timeStr)
                DottedRule()

                val fromLabel = when (txn.type) {
                    TransactionType.TRANSFER, TransactionType.CARD_PAYMENT ->
                        stringResource(R.string.txn_receipt_row_route)
                    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
                    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
                    -> stringResource(R.string.txn_receipt_row_credited_to)
                    else -> stringResource(R.string.txn_receipt_row_paid_from)
                }
                val accountDisplay = state.sourceName
                    ?: stringResource(R.string.txn_receipt_unknown_account)
                PrintRow(label = fromLabel, value = accountDisplay)
                PrintRow(
                    label = stringResource(R.string.txn_receipt_row_method),
                    value = txn.paymentApp.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                )
                DottedRule()

                // Signed amount
                val signedAmt = signedAmount(txn)
                PrintRow(
                    label = stringResource(R.string.txn_detail_field_amount),
                    value = signedAmt,
                    valueColor = meta.amountColor,
                )

                // Destination if present (transfer/card payment)
                if (state.destinationName != null) {
                    DottedRule()
                    PrintRow(
                        label = stringResource(R.string.txn_detail_field_destination),
                        value = state.destinationName,
                    )
                }
            }

            // ── Tags & people ────────────────────────────────
            val tags = state.tagNames
            val people = state.peopleNames
            if (tags.isNotEmpty() || people.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 26.dp, vertical = 4.dp)) {
                    DottedRule(spaceTop = 4.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.txn_receipt_section_tags_people).uppercase(),
                        style = EyebrowStyle,
                        color = Text3,
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        tags.forEach { tag ->
                            TagChip(label = "# $tag")
                        }
                        people.forEach { person ->
                            PersonChip(name = person)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Notes ────────────────────────────────────────
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 4.dp)) {
                DottedRule(spaceTop = 4.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.txn_detail_field_notes).uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                )
                Spacer(Modifier.height(8.dp))
                val notes = txn.notes
                if (!notes.isNullOrBlank()) {
                    Text(
                        text = notes,
                        style = TextStyle(
                            fontFamily = InstrumentSerif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = Text1,
                        ),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.txn_receipt_no_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Text3,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Receipt image ─────────────────────────────────
            if (!txn.receiptUri.isNullOrBlank()) {
                ReceiptImageSection(uri = txn.receiptUri)
            }

            // ── Seal footer ───────────────────────────────────
            SealFooter(txnId = txn.id)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────

@Composable
private fun MerchantStub(txn: Transaction, state: TransactionDetailUiState, meta: TypeMeta) {
    Row(verticalAlignment = Alignment.Top) {
        TransactionCategoryAvatar(
            category = state.category,
            type = txn.type,
            size = 46.dp,
            cornerRadius = 13.dp,
            iconSize = 22.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { transactionTypeLabel(txn.type) },
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.01).em,
                    color = Text1,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.categoryName != null) {
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val catColor = state.category?.let { Color(it.color) } ?: Text3
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(catColor),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = state.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Text2,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroRow(txn: Transaction, meta: TypeMeta) {
    val amountLabel = when {
        txn.type.isIncomeLike() -> stringResource(R.string.txn_receipt_amount_received)
        txn.type == TransactionType.TRANSFER || txn.type == TransactionType.CARD_PAYMENT ->
            stringResource(R.string.txn_receipt_amount_moved)
        txn.type == TransactionType.INVESTMENT_BUY || txn.type == TransactionType.INVESTMENT_SELL ->
            stringResource(R.string.txn_receipt_amount_invested)
        else -> stringResource(R.string.txn_receipt_amount_paid)
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = amountLabel.uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            // Rubber stamp — rotated, double-border box
            RubberStamp(word = meta.stampWord, accentColor = meta.accentColor)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = signedAmount(txn),
            style = ArthaAmountStyles.hero.copy(fontSize = 50.sp, lineHeight = 54.sp),
            color = meta.amountColor,
        )
    }
}

/** Rotated double-border rubber-stamp badge. */
@Composable
private fun RubberStamp(word: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .rotate(-9f)
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(1.5.dp)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(5.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = word.uppercase(),
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.16.em,
                color = accentColor.copy(alpha = 0.6f),
            ),
        )
    }
}

/** Horizontal dashed rule between receipt sections. */
@Composable
private fun DottedRule(spaceTop: androidx.compose.ui.unit.Dp = 2.dp) {
    val lineColor = Line3
    Spacer(Modifier.height(spaceTop))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(3.dp.toPx(), 5.dp.toPx()),
                        phase = 0f,
                    ),
                )
            },
    )
    Spacer(Modifier.height(spaceTop))
}

/** One label + value row in monospace receipt style. */
@Composable
private fun PrintRow(
    label: String,
    value: String,
    valueColor: Color = Text1,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.14.em,
                color = Text3,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = value,
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = (-0.01).em,
                color = valueColor,
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

/** Tag chip — monospace, bordered. */
@Composable
private fun TagChip(label: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Line2, RoundedCornerShape(14.dp))
            .background(Surface2, RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 12.sp,
                color = Text2,
            ),
        )
    }
}

/** Person chip — initial avatar + name. */
@Composable
private fun PersonChip(name: String) {
    Row(
        modifier = Modifier
            .border(1.dp, Line2, RoundedCornerShape(14.dp))
            .background(Surface2, RoundedCornerShape(14.dp))
            .padding(start = 5.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(com.subramanya.artha.ui.theme.AccIndigo),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White,
                ),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = TextStyle(
                fontSize = 12.sp,
                color = Text1,
            ),
        )
    }
}

/** Receipt image full-width in the card — tap to view fullscreen. */
@Composable
private fun ReceiptImageSection(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = ReceiptStore.loadBitmap(context, uri)
    }
    val loaded = bitmap
    var fullscreen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp)) {
        DottedRule(spaceTop = 4.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.txn_detail_section_receipt).uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(Modifier.height(8.dp))
        if (loaded != null) {
            androidx.compose.foundation.Image(
                bitmap = loaded,
                contentDescription = stringResource(R.string.txn_detail_section_receipt),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreen = true },
            )
        }
        Spacer(Modifier.height(4.dp))
    }

    if (fullscreen && loaded != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { fullscreen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { fullscreen = false },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    bitmap = loaded,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** अ seal with embossed double-ring border — Ochre on Paper. */
@Composable
private fun SealFooter(txnId: String) {
    Column(modifier = Modifier.padding(horizontal = 26.dp)) {
        DottedRule(spaceTop = 0.dp)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // अ seal circle with double inset border
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .drawBehind {
                        // Outer border
                        drawCircle(
                            color = Ochre.copy(alpha = 0.78f),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                        // Inner border (inset 4dp from outer)
                        drawCircle(
                            color = Ochre.copy(alpha = 0.78f),
                            radius = size.minDimension / 2f - 4.dp.toPx(),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "अ",
                    style = TextStyle(
                        fontFamily = TiroDevanagariHindi,
                        fontSize = 19.sp,
                        color = OchreSoft.copy(alpha = 0.78f),
                    ),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(
                    text = stringResource(R.string.txn_receipt_recorded),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Text2,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "txn ${txnId.take(8).uppercase()} · synced locally",
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 10.sp,
                        letterSpacing = 0.02.em,
                        color = Text3,
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Perforation divider
// ─────────────────────────────────────────────────────────────

@Composable
private fun Perforation(pageColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        // Dashed horizontal line spanning between the punched holes
        val lineColor = Line3
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(2.dp)
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                            phase = 0f,
                        ),
                    )
                },
        )
        // Left punch hole — page-colored circle that bleeds off the edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-11).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(pageColor),
        )
        // Right punch hole
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 11.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(pageColor),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Scalloped ticket edge — custom Modifier
// Implemented by clipping with a path that alternates between
// concave arcs (notches) along the top and bottom edges.
// ─────────────────────────────────────────────────────────────

private fun Modifier.scalloped(pageColor: Color): Modifier = this.drawWithContent {
    // Draw paper background before clipping content
    drawContent()
}.clip(ScallopedShape())

/** Shape that cuts scalloped notches (radius ~7dp, spacing ~17dp) into top & bottom edges. */
private class ScallopedShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val notchR = with(density) { 7.dp.toPx() }
        val tile = with(density) { 17.dp.toPx() }
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path()

        // Bottom-left corner
        path.moveTo(0f, h)

        // Bottom edge — scalloped (notches cut upward)
        var x = 0f
        while (x < w) {
            val cx = x + tile / 2f
            path.lineTo(cx - notchR, h)
            path.quadraticTo(cx, h - notchR, cx + notchR, h)
            x += tile
        }
        path.lineTo(w, h)

        // Right edge straight
        path.lineTo(w, 0f)

        // Top edge — scalloped (notches cut downward)
        x = w
        while (x > 0f) {
            val cx = x - tile / 2f
            path.lineTo(cx + notchR, 0f)
            path.quadraticTo(cx, notchR, cx - notchR, 0f)
            x -= tile
        }
        path.lineTo(0f, 0f)
        path.close()

        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

// ─────────────────────────────────────────────────────────────
// Action rows
// ─────────────────────────────────────────────────────────────

@Composable
private fun ActionRow(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttachPhoto: () -> Unit,
) {
    // Primary Edit button
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.txn_receipt_action_edit),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    // Secondary chip row — Attach photo + Delete
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SecondaryChip(
            icon = Icons.Filled.AddAPhoto,
            label = stringResource(R.string.txn_receipt_action_attach),
            onClick = onAttachPhoto,
            modifier = Modifier.weight(1f),
        )
        SecondaryChip(
            icon = Icons.Filled.Delete,
            label = stringResource(R.string.txn_receipt_action_delete),
            onClick = onDelete,
            danger = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SecondaryChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (danger) Danger.copy(alpha = 0.32f) else Line2
    val contentColor = if (danger) Danger else Text1
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Surface2,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier.height(40.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
    }
}

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when (txn.type) {
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
        TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
        -> "+$abs"
        TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
        -> "−$abs"
        TransactionType.TRANSFER, TransactionType.CARD_PAYMENT -> "−$abs"
        TransactionType.INVESTMENT_BUY -> "−$abs"
        TransactionType.INVESTMENT_SELL -> "+$abs"
        else -> abs
    }
}
