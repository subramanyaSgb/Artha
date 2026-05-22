package com.subramanya.artha.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.navigation.ArthaDestination
import com.subramanya.artha.ui.theme.PlusJakartaSans
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal900

/**
 * Pill-highlight bottom navigation per the design's [BottomTabs] component.
 * Inactive items are a single dim icon; active is a teal-900 capsule behind a
 * teal-300 icon with a small label below.
 */
@Composable
fun ArthaBottomBar(
    currentDestination: ArthaDestination?,
    onItemSelected: (ArthaDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Surface1.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(0.dp),
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArthaDestination.bottomNav.forEach { dest ->
                BottomTabItem(
                    destination = dest,
                    selected = dest == currentDestination,
                    onClick = { onItemSelected(dest) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    destination: ArthaDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pillBg = if (selected) Teal900 else androidx.compose.ui.graphics.Color.Transparent
    val color = if (selected) Teal300 else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = stringResource(destination.labelRes),
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(destination.labelRes),
            color = color,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
