package com.subramanya.artha.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val people by app.personRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by app.transactionRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: PersonFormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Person? by remember { mutableStateOf(null) }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface1,
                        titleContentColor = Text1,
                        navigationIconContentColor = Text2,
                    ),
                    title = { Text(stringResource(R.string.people_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.about_back))
                        }
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = PersonFormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.people_fab_add)) },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.people_eyebrow).uppercase(),
                            style = EyebrowStyle,
                            color = Teal300,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.people_title),
                            style = TextStyle(
                                fontFamily = InstrumentSerif,
                                fontSize = 26.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Normal,
                                color = Text1,
                            ),
                        )
                    }
                }
                if (people.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Filled.Group,
                                title = stringResource(R.string.people_empty),
                            )
                        }
                    }
                } else {
                    items(people, key = { it.id }) { person ->
                        val net = computeNetBalance(person, transactions)
                        PersonRow(
                            person = person,
                            netBalance = net,
                            onEdit = { formMode = PersonFormMode.Edit(person) },
                            onDelete = { pendingDelete = person },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        PersonFormSheet(
            editing = (mode as? PersonFormMode.Edit)?.person,
            onSave = { resolved ->
                scope.launch { app.personRepository.upsert(resolved); formMode = null }
            },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.people_delete_confirm_title)) },
            text = { Text(stringResource(R.string.people_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.personRepository.delete(toDelete); pendingDelete = null }
                }) {
                    Text(stringResource(R.string.people_delete_confirm_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

private sealed interface PersonFormMode {
    data object Add : PersonFormMode
    data class Edit(val person: Person) : PersonFormMode
}

@Composable
private fun PersonRow(
    person: Person,
    netBalance: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val positive = netBalance >= 0.0
    val amountColor = when {
        netBalance == 0.0 -> Text3
        positive -> Income
        else -> Expense
    }
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(name = person.name)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Text1,
                )
                Spacer(Modifier.height(2.dp))
                val meta = buildList {
                    add(person.relation.label())
                    person.contact?.takeIf { it.isNotBlank() }?.let { add(it) }
                    add(statusLabel(netBalance))
                }.joinToString(" · ")
                Text(
                    text = meta,
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = Text3,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
            Text(
                text = IndianNumberFormat.format(kotlin.math.abs(netBalance)),
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 18.sp,
                    color = amountColor,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Text3,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Small monogram tile derived from the person's name initials. */
@Composable
private fun PersonAvatar(name: String) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Teal900),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = Teal300,
        )
    }
}

/**
 * Per PRD §7.17 — net balance from this person's perspective.
 * LOAN_GIVEN to them, or any EXPENSE/GIFT_SENT tagged with them, means they owe the user.
 * LOAN_RECEIVED from them, or any INCOME/GIFT_RECEIVED tagged with them, means the user owes them.
 * Positive return = they owe the user; negative = user owes them.
 */
private fun computeNetBalance(
    person: Person,
    transactions: List<com.subramanya.artha.domain.model.Transaction>,
): Double {
    var net = 0.0
    for (t in transactions) {
        if (person.id !in t.peopleIds) continue
        when (t.type) {
            TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT, TransactionType.EXPENSE -> net += t.amount
            TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED, TransactionType.INCOME -> net -= t.amount
            else -> Unit
        }
    }
    return net
}

@Composable
private fun statusLabel(net: Double): String = when {
    kotlin.math.abs(net) < 0.005 -> stringResource(R.string.people_settled)
    net > 0 -> stringResource(R.string.people_owes_you)
    else -> stringResource(R.string.people_you_owe)
}

@Composable
private fun PersonRelation.label(): String = when (this) {
    PersonRelation.SPOUSE -> stringResource(R.string.person_relation_spouse)
    PersonRelation.PARENT -> stringResource(R.string.person_relation_parent)
    PersonRelation.SIBLING -> stringResource(R.string.person_relation_sibling)
    PersonRelation.CHILD -> stringResource(R.string.person_relation_child)
    PersonRelation.FRIEND -> stringResource(R.string.person_relation_friend)
    PersonRelation.COLLEAGUE -> stringResource(R.string.person_relation_colleague)
    PersonRelation.BUSINESS -> stringResource(R.string.person_relation_business)
    PersonRelation.OTHER -> stringResource(R.string.person_relation_other)
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PersonFormSheet(
    editing: Person?,
    onSave: (Person) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var relation by remember(editing) { mutableStateOf(editing?.relation ?: PersonRelation.FRIEND) }
    var contact by remember(editing) { mutableStateOf(editing?.contact.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                text = stringResource(
                    if (editing == null) R.string.people_form_add_title else R.string.people_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.person_picker_name_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Text(
                text = stringResource(R.string.person_picker_relation_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                PersonRelation.entries.forEach { rel ->
                    FilterChip(
                        selected = relation == rel,
                        onClick = { relation = rel },
                        label = { Text(rel.label()) },
                    )
                }
            }

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                singleLine = true,
                label = { Text(stringResource(R.string.people_form_contact_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val now = System.currentTimeMillis()
                    onSave(
                        Person(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            relation = relation,
                            contact = contact.trim().takeIf { it.isNotBlank() },
                            avatarUri = editing?.avatarUri,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text(stringResource(R.string.common_save)) }
        }
    }
}
