package com.subramanya.artha.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.utils.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArthaTopBar(
    userName: String? = null,
    modifier: Modifier = Modifier,
) {
    // Recomputed once per composition lifetime; for Phase 1 this is fine — a real
    // date refresh on day-change is deferred to a later session.
    val today = remember { DateFormatter.todayShort() }
    val greeting = if (userName.isNullOrBlank()) {
        stringResource(R.string.greeting_guest)
    } else {
        stringResource(R.string.greeting_named, userName)
    }

    TopAppBar(
        modifier = modifier,
        title = { Text(text = greeting, style = MaterialTheme.typography.titleMedium) },
        actions = {
            Text(
                text = today,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 16.dp),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
