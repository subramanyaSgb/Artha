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

@Composable
fun ArthaTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
