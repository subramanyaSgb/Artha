package com.subramanya.artha.ui.ai

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ai.AiField
import com.subramanya.artha.ai.AiQuickEntryParsed
import com.subramanya.artha.ai.Confidence
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.theme.Danger
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal950
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HANDOFF §3.10 AI Quick Entry sheet — long-press FAB destination.
 *
 * Hero header: 48dp gradient-teal icon container, "AI Quick Entry" title,
 * "Type, dictate, or photograph…" subtitle. Composer: Surface2 multiline
 * box whose border swaps Line1→LineTeal when text is present; floating
 * mic / image / send actions; example chips visible while empty.
 * While parsing a Teal500 dot pulses next to "Reading your entry…".
 * Parsed preview surfaces as a chip card with Cancel + Save footer.
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

    val scope = rememberCoroutineScope()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Decode off the main thread — receipt photos can take 100-300ms to decode.
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
            bitmap?.let(vm::onPhotoPicked)
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val transcripts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        transcripts?.firstOrNull()?.let(vm::appendTranscript)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            AiHero()
            Spacer(Modifier.height(20.dp))

            AiComposer(
                text = state.text,
                onTextChanged = vm::onTextChanged,
                photoAttached = state.photo != null,
                onClearPhoto = { vm.onPhotoPicked(null) },
                onMic = {
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
                },
                onAttach = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onSend = vm::submit,
                sendEnabled = !state.isParsing && (state.text.isNotBlank() || state.photo != null),
                isParsing = state.isParsing,
            )

            // Example chips — visible only while the input is empty and we
            // haven't parsed anything yet. They hint at the kind of prose
            // Gemini understands well so the user isn't typing into the void.
            if (state.text.isBlank() && state.parsed == null && !state.isParsing) {
                Spacer(Modifier.height(16.dp))
                ExampleChips(onPick = vm::onTextChanged)
            }

            if (state.isParsing) {
                Spacer(Modifier.height(16.dp))
                ParsingIndicator()
            }

            if (state.noApiKey) {
                Spacer(Modifier.height(16.dp))
                AiAlert(
                    title = stringResource(R.string.ai_quick_entry_no_key_title),
                    body = stringResource(R.string.ai_quick_entry_no_key_body),
                )
            }
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(16.dp))
                AiAlert(title = stringResource(R.string.ai_quick_entry_error_title), body = msg)
            }

            state.parsed?.let { parsed ->
                Spacer(Modifier.height(20.dp))
                ParsedCard(
                    parsed = parsed,
                    onCancel = vm::reset,
                    onSave = { onConfirmed(parsed) },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * HANDOFF §3.10 header: 48dp gradient-teal icon container + eyebrow + display
 * title + soft subtitle. Sparkles glyph reinforces the "AI" affordance without
 * leaning on emoji.
 */
@Composable
private fun AiHero() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Teal700, Teal500),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                text = stringResource(R.string.ai_quick_entry_eyebrow).uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ai_quick_entry_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.ai_quick_entry_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Surface2 multiline composer with mic / image / send floating actions. Border
 * switches from Line1 to LineTeal when the user has typed something — a quiet
 * "I see you" affordance the design calls out in §3.10.
 */
@Composable
private fun AiComposer(
    text: String,
    onTextChanged: (String) -> Unit,
    photoAttached: Boolean,
    onClearPhoto: () -> Unit,
    onMic: () -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
    isParsing: Boolean,
) {
    val borderColor = if (text.isNotBlank() || photoAttached) LineTeal else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChanged,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.ai_quick_entry_input_placeholder),
                            color = Text3,
                            style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ComposerIconButton(onClick = onMic) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.ai_quick_entry_voice),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            ComposerIconButton(onClick = onAttach) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = stringResource(R.string.ai_quick_entry_photo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (photoAttached) {
                Spacer(Modifier.size(8.dp))
                Surface(
                    color = Teal950,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable(onClick = onClearPhoto),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_quick_entry_photo_attached),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Send button — solid teal when armed, muted Surface4 otherwise.
            val container = if (sendEnabled) Teal700 else MaterialTheme.colorScheme.surfaceContainerHighest
            val tint = if (sendEnabled) MaterialTheme.colorScheme.onSurface else Text3
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(container)
                    .clickable(enabled = sendEnabled && !isParsing, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.ai_quick_entry_submit),
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ComposerIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExampleChips(onPick: (String) -> Unit) {
    val examples = listOf(
        stringResource(R.string.ai_quick_entry_example_1),
        stringResource(R.string.ai_quick_entry_example_2),
        stringResource(R.string.ai_quick_entry_example_3),
    )
    Column {
        Text(
            text = stringResource(R.string.ai_quick_entry_examples_label).uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            examples.forEach { example ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .clickable { onPick(example) }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp)),
                ) {
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum, lnum",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * HANDOFF §6 — pulsing Teal500 dot beside "Reading your entry…" while the
 * Gemini parser is in flight. 1.2s breathing animation so it feels alive but
 * doesn't strobe.
 */
@Composable
private fun ParsingIndicator() {
    val transition = rememberInfiniteTransition(label = "ai-parse-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai-parse-alpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Teal500)
                .alpha(alpha),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.ai_quick_entry_parsing_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Parsed preview — clean list layout showing amount hero, then labelled field
 * rows. Red text only appears when confidence is LOW and a value exists
 * (genuinely suspicious). Missing optional fields are omitted, not shown as "—".
 */
@Composable
private fun ParsedCard(
    parsed: AiQuickEntryParsed,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, LineTeal, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        // Amount hero
        val amount = parsed.amount.value
        val amountColor = if (parsed.amount.confidence == Confidence.LOW && amount != null) Danger
            else MaterialTheme.colorScheme.onSurface
        Text(
            text = if (amount != null) "₹${IndianNumberFormat.format(amount)}" else "Amount not detected",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = IbmPlexMono,
                fontFeatureSettings = "tnum, lnum",
            ),
            color = if (amount != null) amountColor else Danger,
        )

        // Type badge
        parsed.type.value?.let { txnType ->
            Spacer(Modifier.height(6.dp))
            Surface(
                color = when (txnType.name) {
                    "INCOME" -> Income.copy(alpha = 0.15f)
                    "TRANSFER" -> MaterialTheme.colorScheme.primaryContainer
                    else -> Danger.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = txnType.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (txnType.name) {
                        "INCOME" -> Income
                        "TRANSFER" -> MaterialTheme.colorScheme.primary
                        else -> Danger
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // Field rows — only shown when a value is present
        parsed.description.value?.let {
            ParsedRow("Description", it, parsed.description.confidence)
            Spacer(Modifier.height(10.dp))
        }
        parsed.dateMillis.value?.let {
            ParsedRow("Date", DateFormatter.longDate(it), parsed.dateMillis.confidence)
            Spacer(Modifier.height(10.dp))
        }
        parsed.categoryHint.value?.let {
            ParsedRow("Category", it, parsed.categoryHint.confidence)
            Spacer(Modifier.height(10.dp))
        }
        // Payment app: only show when it's not the generic "OTHER" fallback
        parsed.paymentApp.value?.takeIf { it != "OTHER" }?.let { id ->
            val label = id.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
            ParsedRow("Via", label, parsed.paymentApp.confidence)
            Spacer(Modifier.height(10.dp))
        }
        // Place: skip if already in description
        val descLower = parsed.description.value.orEmpty().lowercase()
        parsed.place.value?.takeIf { it.isNotBlank() && !descLower.contains(it.lowercase()) }?.let {
            ParsedRow("Place", it, parsed.place.confidence)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.ai_quick_entry_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Income,
                    contentColor = Color(0xFF06281C),
                ),
            ) {
                Text(
                    text = stringResource(R.string.ai_quick_entry_save_confirm),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ParsedRow(label: String, value: String, confidence: Confidence) {
    val valueColor = when (confidence) {
        Confidence.LOW -> Danger
        Confidence.MEDIUM -> MaterialTheme.colorScheme.onSurface
        Confidence.HIGH -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Text3,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
            modifier = Modifier.weight(0.65f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun AiAlert(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Danger,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
