package com.subramanya.artha.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.R

/**
 * The splash is intentionally simple — a teal circle with the Devanagari glyph "अ",
 * the app name, and the tagline. Dismissal timing (≥500 ms gated on DB init) is
 * orchestrated outside this composable, in [com.subramanya.artha.MainActivity].
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LogoMark()
            Text(
                text = stringResource(R.string.splash_app_name),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun LogoMark() {
    // Fixed teal seed so the splash mark is recognisable regardless of Material You.
    val tealSeed = Color(0xFF0F766E)
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(tealSeed),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.splash_logo_glyph),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
