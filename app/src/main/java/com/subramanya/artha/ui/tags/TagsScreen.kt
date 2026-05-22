package com.subramanya.artha.ui.tags

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.ui.common.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: TagsViewModel = viewModel(factory = TagsViewModelFactory(app.tagRepository))
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var formMode: TagFormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Tag? by remember { mutableStateOf(null) }
    var blockedMessage: String? by remember { mutableStateOf(null) }

    LaunchedEffect(blockedMessage) {
        blockedMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            blockedMessage = null
        }
    }

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
                    title = { Text(stringResource(R.string.tags_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = TagFormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.tags_fab_add)) },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.tags_eyebrow).uppercase(),
                        style = EyebrowStyle,
                        color = Teal300,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tags_title),
                        style = TextStyle(
                            fontFamily = InstrumentSerif,
                            fontSize = 26.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Normal,
                            color = Text1,
                        ),
                    )
                }
                if (state.tags.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Sell,
                            title = stringResource(R.string.tags_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.tags, key = { it.id }) { tag ->
                        TagRow(
                            tag = tag,
                            onEdit = { formMode = TagFormMode.Edit(tag) },
                            onDelete = {
                                scope.launch {
                                    val count = vm.usageCount(tag.id)
                                    if (count > 0) {
                                        blockedMessage = context.getString(R.string.tags_in_use_toast, count)
                                    } else {
                                        pendingDelete = tag
                                    }
                                }
                            },
                        )
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        TagFormSheet(
            editing = (mode as? TagFormMode.Edit)?.tag,
            onSave = { saved ->
                vm.upsert(saved)
                formMode = null
            },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.tags_delete_confirm_title)) },
            text = { Text(stringResource(R.string.tags_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { vm.delete(toDelete); pendingDelete = null }) {
                    Text(
                        text = stringResource(R.string.tags_delete_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private sealed interface TagFormMode {
    data object Add : TagFormMode
    data class Edit(val tag: Tag) : TagFormMode
}

@Composable
private fun TagRow(tag: Tag, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(tag.color)),
            ) { }
        },
        headlineContent = { Text(tag.name) },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.tags_action_edit))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tags_action_edit)) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.tags_action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                    )
                }
            }
        },
    )
}
