package com.subramanya.artha.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.subramanya.artha.data.preferences.ThemeMode

private val ArthaLightColors = lightColorScheme(
    primary = ArthaTealPrimary,
    onPrimary = ArthaTealOnPrimary,
    primaryContainer = ArthaTealPrimaryContainer,
    onPrimaryContainer = ArthaTealOnPrimaryContainer,
    secondary = ArthaTealSecondary,
    onSecondary = ArthaTealOnSecondary,
    secondaryContainer = ArthaTealSecondaryContainer,
    onSecondaryContainer = ArthaTealOnSecondaryContainer,
    tertiary = ArthaTealTertiary,
    onTertiary = ArthaTealOnTertiary,
    tertiaryContainer = ArthaTealTertiaryContainer,
    onTertiaryContainer = ArthaTealOnTertiaryContainer,
)

private val ArthaDarkColors = darkColorScheme(
    primary = ArthaTealPrimaryDark,
    onPrimary = ArthaTealOnPrimaryDark,
    primaryContainer = ArthaTealPrimaryContainerDark,
    onPrimaryContainer = ArthaTealOnPrimaryContainerDark,
)

/**
 * Resolves dark vs light from [themeMode] (SYSTEM falls back to the device setting)
 * and dynamic-color from [useDynamicColor]. Material You is only available on
 * Android 12+; older devices ignore the toggle and fall back to the seed colors.
 */
@Composable
fun ArthaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> ArthaDarkColors
        else -> ArthaLightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = ArthaTypography,
        content = content,
    )
}
