package com.subramanya.artha.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps the app's UI in a biometric/device-credential prompt. Until the user
 * authenticates this composable renders a small "Unlock to continue" screen and
 * launches BiometricPrompt; after success it shows [content].
 *
 * Failures keep the gate up so the user must successfully authenticate (or kill
 * the app) to see data — same model as banking apps. Skip the gate entirely if
 * the device has no enrolled biometric / no secure lock-screen ([canPrompt]).
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

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Artha")
            .setSubtitle("Your money. Your rules.")
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

    LaunchedEffect(Unit) { prompt() }

    if (unlocked) {
        content()
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text("Artha is locked", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Unlock with biometric or device credential to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (promptedOnce) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = { prompt() }) { Text("Try again") }
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
