package com.subramanya.artha.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.BuildConfig
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.BrandMark
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.TiroDevanagariHindi
import com.subramanya.artha.ui.update.UpdateDialog
import com.subramanya.artha.ui.update.UpdateDialogState
import com.subramanya.artha.utils.AppUpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data object Error : UpdateCheckState
}

/**
 * HANDOFF §3.7 About — 88dp BrandMark (22dp radius), Devanagari "अर्थ"
 * caption in Tiro 18sp teal-300, followed by the four-puruṣārthas essay.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var updateDialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            com.subramanya.artha.ui.common.InlineTopBar(
                title = stringResource(R.string.about_title),
                onBack = onBack,
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 1.dp)
                            .background(LineTeal),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.about_eyebrow).uppercase(),
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(16.dp))

                BrandMark(
                    size = 88.dp,
                    cornerRadiusDp = 22.dp,
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.about_devanagari),
                    style = TextStyle(
                        fontFamily = TiroDevanagariHindi,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.about_artha_caption),
                    style = TextStyle(
                        fontFamily = InstrumentSerif,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_one_of_four),
                    style = MaterialTheme.typography.bodySmall,
                    color = Text3,
                )

                Spacer(Modifier.height(20.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                ) {
                    Text(
                        text = stringResource(R.string.about_essay),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                FeaturesSection()

                Spacer(Modifier.height(24.dp))
                AboutMetaRow(
                    eyebrow = stringResource(R.string.about_version_eyebrow),
                    value = BuildConfig.VERSION_NAME,
                )
                Spacer(Modifier.height(10.dp))
                AboutMetaRow(
                    eyebrow = stringResource(R.string.about_credit_title),
                    value = stringResource(R.string.about_credit_value),
                )
                Spacer(Modifier.height(10.dp))

                // Manual update check row
                val checker = remember { AppUpdateChecker(context) }
                UpdateCheckRow(
                    state = checkState,
                    onClick = {
                        if (checkState == UpdateCheckState.Checking) return@UpdateCheckRow
                        checkState = UpdateCheckState.Checking
                        scope.launch {
                            val info = withContext(Dispatchers.IO) {
                                runCatching { checker.checkForUpdate() }.getOrNull()
                            }
                            when {
                                info == null && checkState == UpdateCheckState.Checking ->
                                    checkState = UpdateCheckState.Error
                                info != null -> {
                                    checkState = UpdateCheckState.Idle
                                    updateDialogState = UpdateDialogState.Available(info)
                                }
                                else -> checkState = UpdateCheckState.UpToDate
                            }
                        }
                    },
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Update dialog — same download + install flow as the auto-check in MainApp
    val dialogState = updateDialogState
    if (dialogState != null) {
        val checker = remember { AppUpdateChecker(context) }
        UpdateDialog(
            state = dialogState,
            onDismiss = { updateDialogState = null },
            onDownload = { info ->
                updateDialogState = UpdateDialogState.Downloading(info, 0f)
                scope.launch {
                    val apk = checker.downloadApk(info.downloadUrl) { progress ->
                        updateDialogState = UpdateDialogState.Downloading(info, progress)
                    }
                    if (apk != null) {
                        downloadedApk = apk
                        checker.triggerInstall(apk)
                        updateDialogState = null
                    } else {
                        updateDialogState = UpdateDialogState.Failed(info)
                    }
                }
            },
            onInstall = {
                downloadedApk?.let { checker.triggerInstall(it) }
            },
        )
    }
}

@Composable
private fun UpdateCheckRow(
    state: UpdateCheckState,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = state != UpdateCheckState.Checking, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.about_check_updates).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            when (state) {
                UpdateCheckState.Idle -> Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                UpdateCheckState.Checking -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                UpdateCheckState.UpToDate -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                UpdateCheckState.Error -> Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * A grouped, checkmarked list of everything the app does — shown on the About page so the
 * whole feature surface is visible in one place. Groups + items are string resources.
 */
@Composable
private fun FeaturesSection() {
    val groups = listOf(
        R.string.about_features_group_money to listOf(
            R.string.about_features_money_1, R.string.about_features_money_2,
            R.string.about_features_money_3, R.string.about_features_money_4,
        ),
        R.string.about_features_group_ai to listOf(
            R.string.about_features_ai_1, R.string.about_features_ai_2,
        ),
        R.string.about_features_group_grow to listOf(
            R.string.about_features_grow_1, R.string.about_features_grow_2, R.string.about_features_grow_3,
        ),
        R.string.about_features_group_plan to listOf(
            R.string.about_features_plan_1, R.string.about_features_plan_2, R.string.about_features_plan_3,
        ),
        R.string.about_features_group_understand to listOf(
            R.string.about_features_understand_1, R.string.about_features_understand_2,
        ),
        R.string.about_features_group_private to listOf(
            R.string.about_features_private_1, R.string.about_features_private_2,
            R.string.about_features_private_3, R.string.about_features_private_4,
        ),
        R.string.about_features_group_custom to listOf(
            R.string.about_features_custom_1, R.string.about_features_custom_2, R.string.about_features_custom_3,
        ),
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 1.dp)
                .background(LineTeal),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.about_features_eyebrow).uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(Modifier.height(12.dp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            groups.forEachIndexed { index, (groupRes, items) ->
                if (index > 0) Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(groupRes).uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                )
                Spacer(Modifier.height(8.dp))
                items.forEach { itemRes ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = stringResource(itemRes),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutMetaRow(eyebrow: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = EyebrowStyle,
                color = Text3,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
        }
    }
}
