package com.subramanya.artha.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.BuildConfig
import com.subramanya.artha.R
import com.subramanya.artha.data.preferences.SpouseTransactionDefault
import com.subramanya.artha.data.preferences.ThemeMode
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(app.settingsPreferences, app.database),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    // When an export file becomes available, fire a share chooser then acknowledge.
    LaunchedEffect(state.pendingExportFile) {
        val file = state.pendingExportFile ?: return@LaunchedEffect
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.settings_data_export_share_chooser))
        context.startActivity(chooser)
        Toast.makeText(context, R.string.settings_data_export_done, Toast.LENGTH_SHORT).show()
        vm.acknowledgeExport()
    }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            com.subramanya.artha.ui.common.InlineTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
            SectionHeader(stringResource(R.string.settings_section_profile))
                ProfileSection(
                    name = state.userName,
                    onNameChanged = vm::onNameChanged,
                )

                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_dashboard))
                DashboardSectionsBlock(
                    showMonthly = state.dashboardShowMonthly,
                    showAccounts = state.dashboardShowAccounts,
                    showCards = state.dashboardShowCards,
                    showRecent = state.dashboardShowRecent,
                    onMonthlyChanged = vm::onDashboardShowMonthlyChanged,
                    onAccountsChanged = vm::onDashboardShowAccountsChanged,
                    onCardsChanged = vm::onDashboardShowCardsChanged,
                    onRecentChanged = vm::onDashboardShowRecentChanged,
                )

                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_appearance))
                AppearanceSection(
                    themeMode = state.themeMode,
                    useDynamicColor = state.useDynamicColor,
                    onThemeChanged = vm::onThemeChanged,
                    onDynamicColorChanged = vm::onDynamicColorChanged,
                )

                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_behavior))
                BehaviorSection(
                    spouseDefault = state.spouseDefault,
                    onChange = vm::onSpouseDefaultChanged,
                    onResetSpouse = {
                        vm.resetSpousePrompt()
                        Toast.makeText(
                            context,
                            R.string.settings_behavior_spouse_reset_done,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )

                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_security))
                SecuritySection(
                    biometric = state.biometricLockEnabled,
                    smsImport = state.smsAutoImportEnabled,
                    onBiometricChanged = vm::onBiometricLockChanged,
                    onSmsImportChanged = vm::onSmsAutoImportChanged,
                )

                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_data))
                var passwordDialog by remember { mutableStateOf(false) }
                DataSection(
                    onExport = { vm.exportData(context) },
                    onEncryptedExport = { passwordDialog = true },
                    onReset = vm::requestReset,
                )
                if (passwordDialog) {
                    EncryptedExportPasswordDialog(
                        onConfirm = { pwd ->
                            vm.exportDataEncrypted(context, pwd.toCharArray())
                            passwordDialog = false
                        },
                        onDismiss = { passwordDialog = false },
                    )
                }

                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_about))
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAbout),
                    headlineContent = { Text(stringResource(R.string.about_title)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showFirstResetDialog) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissFirstReset,
            title = stringResource(R.string.settings_data_reset_confirm_title),
            text = stringResource(R.string.settings_data_reset_confirm_body),
            confirmLabel = stringResource(R.string.settings_data_reset_confirm_yes),
            confirmDestructive = true,
            onConfirm = vm::proceedToFinalReset,
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissFirstReset,
        )
    }

    if (state.showFinalResetDialog) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissFinalReset,
            title = stringResource(R.string.settings_data_reset_final_title),
            text = stringResource(R.string.settings_data_reset_final_body),
            confirmLabel = stringResource(R.string.settings_data_reset_final_yes),
            confirmDestructive = true,
            onConfirm = {
                vm.confirmReset(onDone = {
                    Toast.makeText(context, R.string.settings_data_reset_done, Toast.LENGTH_LONG).show()
                })
            },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissFinalReset,
        )
    }

}

// ---------------- sections ----------------

/** §3.5 — eyebrow + 14dp Teal500 hairline on the left, matching SectionHeader spec. */
@Composable
private fun SectionHeader(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 1.dp)
                .background(Teal500),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
    }
}

@Composable
private fun ProfileSection(name: String, onNameChanged: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_profile_name)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_profile_currency) + ": " +
                stringResource(R.string.settings_profile_currency_locked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    onThemeChanged: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.settings_appearance_theme), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip(themeMode, ThemeMode.SYSTEM, R.string.settings_appearance_theme_system, onThemeChanged)
            ThemeChip(themeMode, ThemeMode.LIGHT, R.string.settings_appearance_theme_light, onThemeChanged)
            ThemeChip(themeMode, ThemeMode.DARK, R.string.settings_appearance_theme_dark, onThemeChanged)
        }

        Spacer(Modifier.height(16.dp))
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text(stringResource(R.string.settings_appearance_dynamic)) },
            supportingContent = { Text(stringResource(R.string.settings_appearance_dynamic_body)) },
            trailingContent = {
                com.subramanya.artha.ui.common.ArthaSwitch(checked = useDynamicColor, onCheckedChange = onDynamicColorChanged)
            },
        )
    }
}

@Composable
private fun ThemeChip(
    current: ThemeMode,
    target: ThemeMode,
    labelRes: Int,
    onSelect: (ThemeMode) -> Unit,
) {
    FilterChip(
        selected = current == target,
        onClick = { onSelect(target) },
        label = { Text(stringResource(labelRes)) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BehaviorSection(
    spouseDefault: SpouseTransactionDefault,
    onChange: (SpouseTransactionDefault) -> Unit,
    onResetSpouse: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.settings_behavior_spouse), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpouseChip(spouseDefault, SpouseTransactionDefault.ASK, R.string.settings_behavior_spouse_ask, onChange)
            SpouseChip(spouseDefault, SpouseTransactionDefault.TRANSFER, R.string.settings_behavior_spouse_transfer, onChange)
            SpouseChip(spouseDefault, SpouseTransactionDefault.EXPENSE, R.string.settings_behavior_spouse_expense, onChange)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onResetSpouse) {
            Text(stringResource(R.string.settings_behavior_spouse_reset))
        }
    }
}

@Composable
private fun SpouseChip(
    current: SpouseTransactionDefault,
    target: SpouseTransactionDefault,
    labelRes: Int,
    onSelect: (SpouseTransactionDefault) -> Unit,
) {
    FilterChip(
        selected = current == target,
        onClick = { onSelect(target) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun DashboardSectionsBlock(
    showMonthly: Boolean,
    showAccounts: Boolean,
    showCards: Boolean,
    showRecent: Boolean,
    onMonthlyChanged: (Boolean) -> Unit,
    onAccountsChanged: (Boolean) -> Unit,
    onCardsChanged: (Boolean) -> Unit,
    onRecentChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.settings_dashboard_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        DashboardSwitchRow(stringResource(R.string.settings_dashboard_monthly), showMonthly, onMonthlyChanged)
        DashboardSwitchRow(stringResource(R.string.settings_dashboard_accounts), showAccounts, onAccountsChanged)
        DashboardSwitchRow(stringResource(R.string.settings_dashboard_cards), showCards, onCardsChanged)
        DashboardSwitchRow(stringResource(R.string.settings_dashboard_recent), showRecent, onRecentChanged)
    }
}

@Composable
private fun DashboardSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = { Text(label) },
        trailingContent = { com.subramanya.artha.ui.common.ArthaSwitch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun SecuritySection(
    biometric: Boolean,
    smsImport: Boolean,
    onBiometricChanged: (Boolean) -> Unit,
    onSmsImportChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text(stringResource(R.string.settings_security_biometric)) },
            supportingContent = { Text(stringResource(R.string.settings_security_biometric_body)) },
            trailingContent = { com.subramanya.artha.ui.common.ArthaSwitch(checked = biometric, onCheckedChange = onBiometricChanged) },
        )
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text(stringResource(R.string.settings_security_sms)) },
            supportingContent = { Text(stringResource(R.string.settings_security_sms_body)) },
            trailingContent = { com.subramanya.artha.ui.common.ArthaSwitch(checked = smsImport, onCheckedChange = onSmsImportChanged) },
        )
    }
}

@Composable
private fun DataSection(onExport: () -> Unit, onEncryptedExport: () -> Unit, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExport),
            headlineContent = { Text(stringResource(R.string.settings_data_export)) },
            supportingContent = { Text(stringResource(R.string.settings_data_export_subtitle)) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
        )
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEncryptedExport),
            headlineContent = { Text(stringResource(R.string.settings_data_export_encrypted)) },
            supportingContent = { Text(stringResource(R.string.settings_data_export_encrypted_subtitle)) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
        )
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onReset),
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_data_reset),
                    color = com.subramanya.artha.ui.theme.Danger,
                )
            },
            supportingContent = { Text(stringResource(R.string.settings_data_reset_subtitle)) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = com.subramanya.artha.ui.theme.Danger) },
        )
    }
}

@Composable
private fun EncryptedExportPasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val match = password.isNotBlank() && password == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_data_export_encrypted_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_data_export_encrypted_dialog_body),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_data_export_encrypted_password)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_data_export_encrypted_password_confirm)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = confirm.isNotEmpty() && !match,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = match,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
