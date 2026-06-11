package com.subramanya.artha.ui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * App-wide haptic accents. View-level constants (not Compose's
 * LocalHapticFeedback) because they map to the light system "tick"/"confirm"
 * effects users know, and they respect the system haptics toggle.
 */

/** Light tick — tab switches, pickers, reorder steps. */
@Composable
fun rememberHapticTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }
}

/** Positive confirm — saves and other completed actions. */
@Composable
fun rememberHapticConfirm(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(constant)
        }
    }
}

/** Heavy thunk — destructive confirmations (delete, wipe). */
@Composable
fun rememberHapticHeavy(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    }
}
