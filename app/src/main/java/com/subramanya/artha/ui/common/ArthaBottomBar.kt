package com.subramanya.artha.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.subramanya.artha.ui.navigation.ArthaDestination

/**
 * Bottom navigation. `More` is rendered as a tab but its tap is intercepted by the
 * caller (it opens the More sheet rather than navigating).
 */
@Composable
fun ArthaBottomBar(
    currentDestination: ArthaDestination?,
    onItemSelected: (ArthaDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        ArthaDestination.bottomNav.forEach { destination ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
                alwaysShowLabel = true,
            )
        }
    }
}
