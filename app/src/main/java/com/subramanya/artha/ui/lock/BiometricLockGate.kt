package com.subramanya.artha.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.theme.Danger
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.PlusJakartaSans
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2

/**
 * Wraps the app's UI in a biometric/device-credential prompt. Until the user
 * authenticates this composable renders an unlock screen and launches
 * BiometricPrompt; after success it shows [content].
 *
 * Failures keep the gate up so the user must successfully authenticate (or
 * kill the app) to see data — same model as banking apps. Skip the gate
 * entirely if the device has no enrolled biometric / no secure lock-screen
 * ([canPrompt]).
 */
@Composable
fun BiometricLockGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: run {
        // We're not in a FragmentActivity (unit test / preview) — render content
        // without prompting.
        content()
        return
    }

    val canPrompt = remember { canPrompt(context) }
    if (!canPrompt) {
        content()
        return
    }

    var unlocked by remember { mutableStateOf(false) }
    var promptedOnce by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }

    val promptTitle = stringResource(R.string.lock_prompt_title)
    val promptSubtitle = stringResource(R.string.lock_prompt_subtitle)

    val promptInfo = remember(promptTitle, promptSubtitle) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
    }

    fun prompt() {
        promptedOnce = true
        errorMessage = null
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorMessage = errString.toString()
                }
            },
        ).authenticate(promptInfo)
    }

    // Trigger the system prompt once on first composition. The keyed
    // LaunchedEffect (was Unit, which was correct but worth being explicit
    // about — we never want to re-prompt while the gate is on screen).
    LaunchedEffect("biometric-initial-prompt") { prompt() }

    if (unlocked) {
        content()
        return
    }

    Surface(color = Surface1, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Teal900),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = Teal300,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.lock_screen_title),
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = Text1,
                    letterSpacing = (-0.01).em,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lock_screen_body),
                style = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                ),
                color = Text2,
                modifier = Modifier.fillMaxWidth(),
            )
            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface4)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                    )
                }
            }
            if (promptedOnce) {
                Spacer(Modifier.height(24.dp))
                SavePrimaryButton(
                    label = stringResource(R.string.lock_screen_retry),
                    onClick = { prompt() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun canPrompt(context: android.content.Context): Boolean {
    val mgr = BiometricManager.from(context)
    val flag = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    return mgr.canAuthenticate(flag) == BiometricManager.BIOMETRIC_SUCCESS
}
