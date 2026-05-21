package com.subramanya.artha.ui.insurance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.InsuranceType
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsurancesScreen(
    onBack: () -> Unit,
    onOpenInsurance: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InsurancesViewModel = viewModel(
        factory = InsurancesViewModelFactory(app.insuranceRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Insurance? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.insurance_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.about_back),
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { formMode = FormMode.Add }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.insurance_fab_add))
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (state.activeCount > 0) {
                    HeroCard(
                        annualTotal = state.annualPremiumTotal,
                        count = state.activeCount,
                        dueSoonCount = state.dueWithin30Days.size,
                    )
                }
                if (state.dueWithin30Days.isNotEmpty()) {
                    DueSoonBanner(state.dueWithin30Days)
                }

                if (state.grouped.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Shield,
                            title = stringResource(R.string.insurance_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        state.grouped.forEach { (type, rows) ->
                            item(key = "header-${type.name}") { TypeHeader(type) }
                            items(rows, key = { it.id }) { policy ->
                                InsuranceRow(
                                    insurance = policy,
                                    onTap = { onOpenInsurance(policy.id) },
                                    onEdit = { formMode = FormMode.Edit(policy) },
                                    onArchive = { vm.archive(policy) },
                                    onDelete = { pendingDelete = policy },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        InsuranceFormSheet(
            editing = (mode as? FormMode.Edit)?.insurance,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.insurance_delete_confirm_title)) },
            text = { Text(stringResource(R.string.insurance_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(toDelete)
                    pendingDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.insurance_delete_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val insurance: Insurance) : FormMode
}

// ---------------- hero ----------------

@Composable
private fun HeroCard(annualTotal: Double, count: Int, dueSoonCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.insurance_hero_annual_premium),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = IndianNumberFormat.format(annualTotal),
                style = ArthaAmountStyles.title,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            val subtitle = stringResource(R.string.insurance_hero_subtitle, count)
            val withDue = if (dueSoonCount > 0) {
                subtitle + " · " + stringResource(R.string.insurance_hero_due_soon, dueSoonCount)
            } else subtitle
            Text(
                text = withDue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DueSoonBanner(items: List<Insurance>) {
    val nearest = items.first()
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.insurance_due_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                val due = nearest.nextPremiumDate?.let { DateFormatter.longDate(it) }.orEmpty()
                Text(
                    text = "${nearest.name} · $due",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun TypeHeader(type: InsuranceType) {
    Text(
        text = type.displayName(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 4.dp),
    )
}

// ---------------- row ----------------

@Composable
private fun InsuranceRow(
    insurance: Insurance,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val due = insurance.nextPremiumDate
    val daysUntil = due?.let {
        val now = Clock.System.now().toEpochMilliseconds()
        ((it - now) / (1000L * 60 * 60 * 24)).toInt()
    }
    val dueSoon = daysUntil != null && daysUntil <= 30
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        leadingContent = { InsuranceAvatar(color = insurance.color) },
        headlineContent = { Text(insurance.name, maxLines = 1) },
        supportingContent = {
            val parts = buildList {
                add(insurance.provider)
                add(stringResource(R.string.insurance_row_premium_fmt,
                    IndianNumberFormat.format(insurance.premiumAmount)))
                due?.let {
                    val label = when {
                        daysUntil == null -> ""
                        daysUntil < 0 -> stringResource(R.string.insurance_row_overdue)
                        daysUntil == 0 -> stringResource(R.string.insurance_row_due_today)
                        daysUntil == 1 -> stringResource(R.string.insurance_row_due_tomorrow)
                        else -> stringResource(R.string.insurance_row_due_in_days, daysUntil)
                    }
                    if (label.isNotBlank()) add(label)
                }
            }
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (dueSoon) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (dueSoon) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.insurance_row_sum_assured_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = IndianNumberFormat.format(insurance.sumAssured),
                        style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_detail_action_edit)) },
                            onClick = { menuOpen = false; onEdit() },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_detail_action_archive)) },
                            onClick = { menuOpen = false; onArchive() },
                            leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.account_action_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = { menuOpen = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun InsuranceAvatar(color: Long) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White)
    }
}

@Composable
internal fun InsuranceType.displayName(): String = when (this) {
    InsuranceType.HEALTH -> stringResource(R.string.insurance_type_health)
    InsuranceType.VEHICLE -> stringResource(R.string.insurance_type_vehicle)
    InsuranceType.LIFE_TERM -> stringResource(R.string.insurance_type_life_term)
    InsuranceType.LIFE_ENDOWMENT -> stringResource(R.string.insurance_type_life_endowment)
    InsuranceType.TRAVEL -> stringResource(R.string.insurance_type_travel)
    InsuranceType.HOME -> stringResource(R.string.insurance_type_home)
    InsuranceType.OTHER -> stringResource(R.string.insurance_type_other)
}
