package com.subramanya.artha.ui.more

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R

enum class MoreAction { Categories, Tags, Settings, About, Investments, Insurance }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(
    onDismiss: () -> Unit,
    onActionSelected: (MoreAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.more_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            MoreTile(
                icon = Icons.Filled.Category,
                titleRes = R.string.more_categories,
                onClick = { onActionSelected(MoreAction.Categories) },
            )
            MoreTile(
                icon = Icons.Filled.Sell,
                titleRes = R.string.more_tags,
                onClick = { onActionSelected(MoreAction.Tags) },
            )
            MoreTile(
                icon = Icons.Filled.Settings,
                titleRes = R.string.more_settings,
                onClick = { onActionSelected(MoreAction.Settings) },
            )
            MoreTile(
                icon = Icons.Filled.Info,
                titleRes = R.string.more_about,
                onClick = { onActionSelected(MoreAction.About) },
            )

            MoreTile(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                titleRes = R.string.more_investments,
                onClick = { onActionSelected(MoreAction.Investments) },
            )
            MoreTile(
                icon = Icons.Filled.Shield,
                titleRes = R.string.more_insurance,
                onClick = { onActionSelected(MoreAction.Insurance) },
            )
            DisabledTile(Icons.Filled.AccountBalanceWallet, R.string.more_budgets, R.string.more_coming_phase_4)
            DisabledTile(Icons.Filled.Flag, R.string.more_goals, R.string.more_coming_phase_4)
            DisabledTile(Icons.Filled.Subscriptions, R.string.more_subscriptions, R.string.more_coming_phase_4)
            DisabledTile(Icons.Filled.EventRepeat, R.string.more_recurring, R.string.more_coming_phase_4)
            DisabledTile(Icons.Filled.Group, R.string.more_people, R.string.more_coming_phase_4)
            DisabledTile(Icons.Filled.BarChart, R.string.more_reports, R.string.more_coming_phase_4)
        }
    }
}

@Composable
private fun MoreTile(
    icon: ImageVector,
    @StringRes titleRes: Int,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(titleRes)) },
    )
}

@Composable
private fun DisabledTile(
    icon: ImageVector,
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int,
) {
    // Disabled visual: dimmed alpha, no click handler attached.
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(DISABLED_ALPHA),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = {
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
}

private const val DISABLED_ALPHA: Float = 0.45f
