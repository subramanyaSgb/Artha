package com.subramanya.artha.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.StubScreen

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    StubScreen(label = stringResource(R.string.screen_dashboard_stub), modifier = modifier)
}
