package com.subramanya.artha.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.CardWithBalance
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.ReceiptStore
import com.subramanya.artha.utils.computeNextDue

private const val DUE_HIGHLIGHT_THRESHOLD_DAYS: Int = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    modifier: Modifier = Modifier,
    onOpenCard: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: CardsViewModel = viewModel(factory = CardsViewModelFactory(app.cardRepository))
    val state by vm.state.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(toastMsg) {
        toastMsg?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }
    var overflowOpen by remember { mutableStateOf(false) }
    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Card? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                CardsEditorialHeader(
                    view = state.view,
                    reorderMode = state.isReorderMode,
                    sort = state.sort,
                    onSortChanged = vm::setSort,
                    overflowOpen = overflowOpen,
                    onOverflowToggle = { overflowOpen = it },
                    onShowArchived = vm::showArchived,
                    onShowActive = vm::showActive,
                    onExitReorder = vm::exitReorderMode,
                )

                if (state.view == CardsView.ACTIVE && state.activeCards.isNotEmpty()) {
                    CardsTotalOutstandingCard(
                        rows = state.activeCards,
                        grouped = state.groupByType,
                        onToggleGroup = vm::toggleGroupByType,
                    )
                }

                val rows = state.shownRows
                if (state.isLoading) {
                    CardsSkeleton()
                } else if (rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.CreditCard,
                            title = stringResource(
                                if (state.view == CardsView.ACTIVE) R.string.cards_empty_active
                                else R.string.cards_empty_archived,
                            ),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        itemsIndexed(rows, key = { _, row -> row.card.id }) { i, row ->
                            Column {
                                if (state.groupByType &&
                                    (i == 0 || rows[i - 1].card.type != row.card.type)
                                ) {
                                    CardTypeHeader(type = row.card.type)
                                }
                                if (state.view == CardsView.ACTIVE) {
                                    ActiveCardRow(
                                        row = row,
                                        reorderMode = state.isReorderMode,
                                        canMoveUp = rows.first() != row,
                                        canMoveDown = rows.last() != row,
                                        onTap = { if (!state.isReorderMode) onOpenCard(row.card.id) },
                                        onLongPress = vm::enterReorderMode,
                                        onMoveUp = { vm.moveUp(row.card) },
                                        onMoveDown = { vm.moveDown(row.card) },
                                        onEdit = { formMode = FormMode.Edit(row.card) },
                                        onArchive = { vm.archive(row.card) },
                                        onDelete = { pendingDelete = row.card },
                                    )
                                } else {
                                    ArchivedCardRow(
                                        row = row,
                                        onRestore = { vm.restore(row.card) },
                                        onDelete = { pendingDelete = row.card },
                                    )
                                }
                            }
                        }
                        item { androidx.compose.foundation.layout.Spacer(Modifier.height(100.dp)) }
                    }
                }
            }

            if (state.view == CardsView.ACTIVE && !state.isReorderMode) {
                // HANDOFF §2 — Extended FAB anchored 20dp above the bottom nav.
                // (Was bottom=110.dp — see AccountsScreen for the same fix.)
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    containerColor = com.subramanya.artha.ui.theme.Teal700,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    icon = {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    },
                    text = {
                        Text(stringResource(R.string.cards_fab_add))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                )
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        CardFormSheet(
            editing = (mode as? FormMode.Edit)?.card,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.card_delete_confirm_title),
            text = stringResource(R.string.card_delete_confirm_body),
            confirmLabel = stringResource(R.string.card_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = {
                vm.delete(toDelete)
                pendingDelete = null
            },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = { pendingDelete = null },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val card: Card) : FormMode
}

// ---------------- rows ----------------

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ActiveCardRow(
    row: CardWithBalance,
    reorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    // Credit cards get the full editorial tile (chhatri + jaali + DUE pill).
    // Debit / prepaid keep the compact ListItem since they have no outstanding /
    // utilization to display.
    if (row.card.type == "CREDIT" && !reorderMode) {
        CreditCardTile(
            row = row,
            onClick = onTap,
            onLongPress = onLongPress,
            onEdit = onEdit,
            onArchive = onArchive,
            onDelete = onDelete,
        )
        return
    }
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        leadingContent = { CardAvatar(color = row.card.color) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.card.name, maxLines = 1)
                Spacer(Modifier.size(8.dp))
                NetworkBadge(row.card.network)
            }
        },
        supportingContent = { CardRowSupport(row = row) },
        trailingContent = {
            if (reorderMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.accounts_action_move_up))
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.accounts_action_move_down))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (row.card.type == "CREDIT") {
                        Text(
                            text = IndianNumberFormat.format(row.currentOutstanding),
                            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cards_more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.card_detail_action_edit)) },
                                onClick = { menuOpen = false; onEdit() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.card_detail_action_archive)) },
                                onClick = { menuOpen = false; onArchive() },
                                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.card_action_delete),
                                        color = com.subramanya.artha.ui.theme.Danger,
                                    )
                                },
                                onClick = { menuOpen = false; onDelete() },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = com.subramanya.artha.ui.theme.Danger)
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * Editorial credit-card tile per HANDOFF §3.4 — 190dp tall, gradient
 * (card.color → 55% mix with black), jaali overlay at 0.15 white, chhatri
 * silhouette in the top-right corner at 0.10 opacity. Two layered rows:
 * NETWORK eyebrow + card name + "DUE IN Nd" pill on top, outstanding +
 * mono last-4 + utilization bar + footer on the bottom.
 */
/**
 * Resolves the card background image in priority order:
 * 1. User-uploaded photo (cardImageUri absolute path)
 * 2. Bundled drawable matched by card name slug  (card_bg_<slug>.png)
 * 3. Bundled drawable matched by issuer keyword  (card_bg_hdfc.png etc.)
 * Returns null → fall back to gradient tile.
 */
private fun resolveCardDrawable(card: Card, context: android.content.Context): Int? {
    // Priority 2: name slug (e.g. "ICICI Mine" → card_bg_icici_mine)
    val slug = card.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    val byName = context.resources.getIdentifier("card_bg_$slug", "drawable", context.packageName)
    if (byName != 0) return byName

    // Priority 3: issuer keyword
    val issuer = card.issuer?.lowercase() ?: return null
    val byIssuer = when {
        "hdfc"   in issuer -> context.resources.getIdentifier("card_bg_hdfc",   "drawable", context.packageName)
        "icici"  in issuer -> context.resources.getIdentifier("card_bg_icici",  "drawable", context.packageName)
        "sbi"    in issuer -> context.resources.getIdentifier("card_bg_sbi",    "drawable", context.packageName)
        "axis"   in issuer -> context.resources.getIdentifier("card_bg_axis",   "drawable", context.packageName)
        "kotak"  in issuer -> context.resources.getIdentifier("card_bg_kotak",  "drawable", context.packageName)
        "yes"    in issuer -> context.resources.getIdentifier("card_bg_yes",    "drawable", context.packageName)
        "federal" in issuer -> context.resources.getIdentifier("card_bg_federal","drawable", context.packageName)
        else -> 0
    }
    return if (byIssuer != 0) byIssuer else null
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CreditCardTile(
    row: CardWithBalance,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val limit = row.card.creditLimit ?: 0.0
    val utilPct = if (limit > 0) (row.currentOutstanding / limit * 100).toInt() else 0
    val available = (limit - row.currentOutstanding).coerceAtLeast(0.0)
    val due = row.card.dueDayOfMonth
    val daysToDue: Int? = due?.let { computeNextDue(it)?.daysUntil }
    val context = LocalContext.current

    // Resolve background: user photo > bundled drawable > gradient
    val userBitmap by produceState<ImageBitmap?>(initialValue = null, row.card.cardImageUri) {
        value = row.card.cardImageUri?.let { ReceiptStore.loadBitmap(context, it) }
    }
    val fallbackDrawable = if (userBitmap == null) resolveCardDrawable(row.card, context) else null

    // Shared overlay content (top bar + bottom stats) — used in both branches
    @Composable
    fun androidx.compose.foundation.layout.BoxScope.CardOverlay() {
        // Top row: network + name + DUE pill + menu
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 22.dp, top = 20.dp, end = 22.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.card.network.displayName().uppercase(),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    style = com.subramanya.artha.ui.theme.EyebrowStyle,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = row.card.name,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                )
            }
            if (daysToDue != null) {
                val pillLabel = when (daysToDue) {
                    0 -> stringResource(R.string.cards_due_today)
                    1 -> stringResource(R.string.cards_due_tomorrow)
                    else -> stringResource(R.string.cards_due_in_short, daysToDue)
                }
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = pillLabel.uppercase(),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.05.em,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.cards_more_options),
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.card_detail_action_edit)) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.card_detail_action_archive)) },
                        onClick = { menuOpen = false; onArchive() },
                        leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.card_action_delete), color = com.subramanya.artha.ui.theme.Danger) },
                        onClick = { menuOpen = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = com.subramanya.artha.ui.theme.Danger) },
                    )
                }
            }
        }
        // Bottom: card number + name (image tiles) or outstanding + util bar (gradient tile)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 20.dp),
        ) {
            if (userBitmap != null || fallbackDrawable != null) {
                // Image tile bottom: masked number + cardholder name
                Text(
                    text = "•••• •••• •••• ${row.card.cardNumberLast4 ?: "••••"}",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                        fontSize = 15.sp,
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.08.em,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = row.card.name,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                // Gradient tile bottom: outstanding + util
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.cards_outstanding_label).uppercase(),
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            style = com.subramanya.artha.ui.theme.EyebrowStyle,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = IndianNumberFormat.format(row.currentOutstanding),
                            color = androidx.compose.ui.graphics.Color.White,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = com.subramanya.artha.ui.theme.InstrumentSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 28.sp,
                                letterSpacing = (-0.01).em,
                                fontFeatureSettings = "tnum",
                            ),
                        )
                    }
                    row.card.cardNumberLast4?.let {
                        Text(
                            text = "•••• $it",
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum",
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                com.subramanya.artha.ui.common.LinearMeter(
                    fraction = (utilPct / 100f).coerceIn(0f, 1f),
                    fillColor = androidx.compose.ui.graphics.Color.White,
                    trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                    heightDp = 4,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.cards_util_of_limit, utilPct, IndianNumberFormat.formatCompact(limit)),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                            fontSize = 10.sp,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        text = stringResource(R.string.cards_available, IndianNumberFormat.formatCompact(available)),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                            fontSize = 10.sp,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
            }
        }
    }

    val tileModifier = Modifier
        .fillMaxWidth()
        .height(190.dp)
        .padding(bottom = 14.dp)
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongPress)

    when {
        userBitmap != null -> {
            // User-uploaded photo
            Box(modifier = tileModifier) {
                androidx.compose.foundation.Image(
                    bitmap = userBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.22f)))
                CardOverlay()
            }
        }
        fallbackDrawable != null -> {
            // Bundled PNG matched by name or issuer
            Box(modifier = tileModifier) {
                androidx.compose.foundation.Image(
                    painter = painterResource(fallbackDrawable),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.22f)))
                CardOverlay()
            }
        }
        else -> {
            // Gradient tile (original)
            val tone = androidx.compose.ui.graphics.Color(row.card.color)
            val toneDeep = androidx.compose.ui.graphics.Color(
                red = tone.red * 0.45f,
                green = tone.green * 0.45f,
                blue = tone.blue * 0.45f,
                alpha = 1f,
            )
            Box(
                modifier = tileModifier.background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(tone, toneDeep)),
                ),
            ) {
                com.subramanya.artha.ui.common.JaaliOverlay(
                    modifier = Modifier.matchParentSize(),
                    tint = androidx.compose.ui.graphics.Color.White,
                    alpha = 0.15f,
                )
                com.subramanya.artha.ui.common.Chhatri(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                        .size(120.dp)
                        .alpha(0.10f),
                    tint = androidx.compose.ui.graphics.Color.White,
                )
                CardOverlay()
            }
        }
    }
}

@Composable
private fun ArchivedCardRow(
    row: CardWithBalance,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { CardAvatar(color = row.card.color) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.card.name, maxLines = 1)
                Spacer(Modifier.size(8.dp))
                NetworkBadge(row.card.network)
            }
        },
        supportingContent = { CardRowSupport(row = row, showDueChip = false, showUtilization = false) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRestore) {
                    Icon(Icons.Filled.Unarchive, contentDescription = null)
                    Text(
                        text = stringResource(R.string.accounts_action_restore),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cards_more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.card_action_delete),
                                    color = com.subramanya.artha.ui.theme.Danger,
                                )
                            },
                            onClick = { menuOpen = false; onDelete() },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = com.subramanya.artha.ui.theme.Danger)
                            },
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardRowSupport(
    row: CardWithBalance,
    showDueChip: Boolean = true,
    showUtilization: Boolean = true,
) {
    Column {
        val subtitle = formatSubtitle(row.card)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showUtilization && row.card.type == "CREDIT") {
            val limit = row.card.creditLimit
            if (limit != null && limit > 0.0) {
                val utilFraction = (row.currentOutstanding / limit).coerceIn(0.0, 1.0).toFloat()
                Spacer(Modifier.height(6.dp))
                com.subramanya.artha.ui.common.LinearMeter(
                    fraction = utilFraction,
                    fillColor = if (utilFraction > 0.3f)
                        com.subramanya.artha.ui.theme.Ochre
                    else
                        com.subramanya.artha.ui.theme.Teal500,
                )
                Text(
                    text = stringResource(R.string.cards_utilization_label, (utilFraction * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (showDueChip && row.card.type == "CREDIT") {
            val dueDay = row.card.dueDayOfMonth
            if (dueDay != null) {
                val due = computeNextDue(dueDay)
                if (due != null && due.daysUntil in 0..DUE_HIGHLIGHT_THRESHOLD_DAYS) {
                    Spacer(Modifier.height(4.dp))
                    val label = when (due.daysUntil) {
                        0 -> stringResource(R.string.cards_due_today)
                        1 -> stringResource(R.string.cards_due_tomorrow)
                        else -> stringResource(R.string.cards_due_in_days, due.daysUntil)
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onErrorContainer,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardAvatar(color: Long) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun CardNetwork.displayName(): String = when (this) {
    CardNetwork.VISA -> stringResource(R.string.card_form_network_visa)
    CardNetwork.MASTERCARD -> stringResource(R.string.card_form_network_mastercard)
    CardNetwork.RUPAY -> stringResource(R.string.card_form_network_rupay)
    CardNetwork.AMEX -> stringResource(R.string.card_form_network_amex)
    CardNetwork.DINERS -> stringResource(R.string.card_form_network_diners)
}

@Composable
private fun NetworkBadge(network: CardNetwork) {
    Text(
        text = network.displayName(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun formatSubtitle(card: Card): String? {
    val pieces = buildList {
        card.issuer?.takeIf { it.isNotBlank() }?.let { add(it) }
        card.cardNumberLast4?.takeIf { it.isNotBlank() }?.let { add("••$it") }
    }
    return pieces.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

// ───────────────────────────── Editorial header + Hero ───────────────────────

@Composable
private fun CardsEditorialHeader(
    view: CardsView,
    reorderMode: Boolean,
    sort: CardSort,
    onSortChanged: (CardSort) -> Unit,
    overflowOpen: Boolean,
    onOverflowToggle: (Boolean) -> Unit,
    onShowArchived: () -> Unit,
    onShowActive: () -> Unit,
    onExitReorder: () -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.cards_eyebrow).uppercase(),
                style = com.subramanya.artha.ui.theme.EyebrowStyle,
                color = com.subramanya.artha.ui.theme.Text3,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    reorderMode -> stringResource(R.string.cards_reorder_hint)
                    view == CardsView.ARCHIVED -> stringResource(R.string.cards_section_archived)
                    else -> stringResource(R.string.cards_title)
                },
                fontFamily = com.subramanya.artha.ui.theme.InstrumentSerif,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp),
                lineHeight = androidx.compose.ui.unit.TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (reorderMode) {
            TextButton(onClick = onExitReorder) {
                Icon(Icons.Filled.Done, contentDescription = null)
                Text(
                    text = stringResource(R.string.accounts_reorder_done),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { sortOpen = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.cards_sort_label),
                        )
                    }
                    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                        CardSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.sortLabel()) },
                                onClick = { onSortChanged(option); sortOpen = false },
                                leadingIcon = if (sort == option) {
                                    { Icon(Icons.Filled.Done, contentDescription = null) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { onOverflowToggle(true) }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cards_more_options))
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { onOverflowToggle(false) }) {
                        if (view == CardsView.ACTIVE) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cards_menu_show_archived)) },
                                onClick = { onShowArchived(); onOverflowToggle(false) },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cards_menu_back_active)) },
                                onClick = { onShowActive(); onOverflowToggle(false) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsTotalOutstandingCard(
    rows: List<com.subramanya.artha.domain.model.CardWithBalance>,
    grouped: Boolean = false,
    onToggleGroup: () -> Unit = {},
) {
    val totalOut = rows.sumOf { it.currentOutstanding }
    val totalLimit = rows.sumOf { it.card.creditLimit ?: 0.0 }
    val util = if (totalLimit > 0.0) (totalOut / totalLimit * 100).toInt() else 0
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
            // Tap to group/ungroup the list by card type.
            .clickable(onClick = onToggleGroup),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cards_total_outstanding).uppercase(),
                    style = com.subramanya.artha.ui.theme.EyebrowStyle,
                    color = com.subramanya.artha.ui.theme.Text3,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.cards_util_short, util),
                    color = com.subramanya.artha.ui.theme.Text3,
                    fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                        fontFeatureSettings = "tnum",
                    ),
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
            com.subramanya.artha.ui.common.AutoShrinkAmountText(
                text = IndianNumberFormat.format(totalOut),
                style = ArthaAmountStyles.hero.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(32f, androidx.compose.ui.unit.TextUnitType.Sp),
                    lineHeight = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.cards_of_limit, IndianNumberFormat.format(totalLimit)),
                color = com.subramanya.artha.ui.theme.Text3,
                fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                    fontFeatureSettings = "tnum",
                ),
            )
            if (grouped) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.cards_grouped_hint),
                    style = com.subramanya.artha.ui.theme.EyebrowStyle,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
}

@Composable
private fun CardSort.sortLabel(): String = when (this) {
    CardSort.CUSTOM -> stringResource(R.string.cards_sort_custom)
    CardSort.OUTSTANDING_DESC -> stringResource(R.string.cards_sort_outstanding_desc)
    CardSort.NAME_ASC -> stringResource(R.string.cards_sort_name)
}

/** Sub-header shown above the first row of each card type when grouping is on. */
@Composable
private fun CardTypeHeader(type: String) {
    Text(
        text = type.replace('_', ' ').uppercase(),
        style = com.subramanya.artha.ui.theme.EyebrowStyle,
        color = com.subramanya.artha.ui.theme.Text3,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
    )
}

/** Cold-open placeholder so the list doesn't flash an empty state before data arrives. */
@Composable
private fun CardsSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
        }
    }
}
