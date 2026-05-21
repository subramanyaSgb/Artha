package com.subramanya.artha.ui.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ai.AiQuickEntryParsed
import com.subramanya.artha.ai.Confidence
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * PRD §11.1 AI Quick Entry — long-press FAB → one bottom sheet with three
 * input modalities (text, voice, photo). Submits to [GeminiQuickEntryParser]
 * and renders a confidence-tagged preview before handing off to the existing
 * Add Transaction flow.
 *
 * Voice is implemented via the system [RecognizerIntent] (no extra permission
 * dance — Android handles the dialog). Photo is via [ActivityResultContracts.PickVisualMedia]
 * — gallery only for now, no camera. The user can extend to CameraX later
 * without changing this UI contract.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuickEntrySheet(
    onDismiss: () -> Unit,
    onConfirmed: (AiQuickEntryParsed) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: AiQuickEntryViewModel = viewModel(
        factory = AiQuickEntryViewModelFactory(app.aiQuickEntryParser),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Photo picker — gallery only. Returns a Uri we decode into a Bitmap so the
    // parser can ship it to Gemini's multi-modal API in one call.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrNull()?.let(vm::onPhotoPicked)
    }

    // Voice — system RecognizerIntent gives us free dialog + permission handling.
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val transcripts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        transcripts?.firstOrNull()?.let(vm::appendTranscript)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_quick_entry_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.ai_quick_entry_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.text,
                onValueChange = vm::onTextChanged,
                label = { Text(stringResource(R.string.ai_quick_entry_text_label)) },
                placeholder = { Text(stringResource(R.string.ai_quick_entry_text_placeholder)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                        putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            context.getString(R.string.ai_quick_entry_voice_prompt),
                        )
                    }
                    runCatching { voiceLauncher.launch(intent) }
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.ai_quick_entry_voice))
                }
                IconButton(onClick = {
                    pickPhoto.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                }) {
                    Icon(Icons.Filled.Image, contentDescription = stringResource(R.string.ai_quick_entry_photo))
                }
                if (state.photo != null) {
                    AssistChip(
                        onClick = { vm.onPhotoPicked(null) },
                        label = { Text(stringResource(R.string.ai_quick_entry_photo_attached)) },
                    )
                }
                Spacer(Modifier.size(1.dp))
                IconButton(
                    onClick = vm::submit,
                    enabled = !state.isParsing && (state.text.isNotBlank() || state.photo != null),
                ) {
                    if (state.isParsing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.ai_quick_entry_submit))
                    }
                }
            }

            if (state.noApiKey) {
                Spacer(Modifier.height(12.dp))
                ErrorBlock(
                    title = stringResource(R.string.ai_quick_entry_no_key_title),
                    body = stringResource(R.string.ai_quick_entry_no_key_body),
                )
            }
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                ErrorBlock(title = stringResource(R.string.ai_quick_entry_error_title), body = msg)
            }

            state.parsed?.let { parsed ->
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ai_quick_entry_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_amount), field = parsed.amount,
                    render = { it?.let(IndianNumberFormat::format) ?: "—" })
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_type), field = parsed.type,
                    render = { it?.name ?: "—" })
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_description), field = parsed.description,
                    render = { it ?: "—" })
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_category), field = parsed.categoryHint,
                    render = { it ?: "—" })
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_payment_app), field = parsed.paymentApp,
                    render = { it?.name ?: "—" })
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_date), field = parsed.dateMillis,
                    render = { it?.let(DateFormatter::longDate) ?: "—" })
                ParsedRow(label = stringResource(R.string.ai_quick_entry_preview_place), field = parsed.place,
                    render = { it ?: "—" })

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onConfirmed(parsed) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.ai_quick_entry_confirm)) }
            }
        }
    }
}

@Composable
private fun <T> ParsedRow(label: String, field: com.subramanya.artha.ai.AiField<T>, render: (T?) -> String) {
    val color = when (field.confidence) {
        Confidence.LOW -> MaterialTheme.colorScheme.error
        Confidence.MEDIUM -> MaterialTheme.colorScheme.onSurfaceVariant
        Confidence.HIGH -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(render(field.value), style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun ErrorBlock(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
