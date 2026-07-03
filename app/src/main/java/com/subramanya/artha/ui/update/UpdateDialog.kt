package com.subramanya.artha.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subramanya.artha.R
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.UpdateInfo

sealed interface UpdateDialogState {
    data class Available(val info: UpdateInfo) : UpdateDialogState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateDialogState
    data class Failed(val info: UpdateInfo) : UpdateDialogState
}

@Composable
fun UpdateDialog(
    state: UpdateDialogState,
    onDismiss: () -> Unit,
    onDownload: (UpdateInfo) -> Unit,
    onInstall: () -> Unit,
) {
    Dialog(
        onDismissRequest = {
            // Don't allow dismissing while downloading
            if (state !is UpdateDialogState.Downloading) onDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = state !is UpdateDialogState.Downloading),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LineTeal, RoundedCornerShape(20.dp)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdateAlt,
                        contentDescription = null,
                        tint = Teal500,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_eyebrow).uppercase(),
                        style = EyebrowStyle,
                        color = Teal500,
                    )
                }

                Spacer(Modifier.height(12.dp))

                val info = when (state) {
                    is UpdateDialogState.Available -> state.info
                    is UpdateDialogState.Downloading -> state.info
                    is UpdateDialogState.Failed -> state.info
                }

                Text(
                    text = stringResource(R.string.update_dialog_title, info.versionName),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (info.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_whats_new).uppercase(),
                        style = EyebrowStyle,
                        color = Text3,
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)   // viewport cap — must come before verticalScroll
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    ) {
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Text2,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                when (state) {
                    is UpdateDialogState.Available -> {
                        Button(
                            onClick = { onDownload(info) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        ) {
                            Text(
                                text = stringResource(R.string.update_dialog_download),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.update_dialog_later),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is UpdateDialogState.Downloading -> {
                        val pct = (state.progress * 100).toInt()
                        Text(
                            text = stringResource(R.string.update_dialog_downloading, pct),
                            style = MaterialTheme.typography.bodySmall,
                            color = Text2,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = Income,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.update_dialog_downloading_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Text3,
                        )
                    }

                    is UpdateDialogState.Failed -> {
                        Text(
                            text = stringResource(R.string.update_dialog_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onDownload(info) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        ) {
                            Text(stringResource(R.string.update_dialog_retry))
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.update_dialog_later),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
