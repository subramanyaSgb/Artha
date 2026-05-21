package com.subramanya.artha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.preferences.ThemeMode
import com.subramanya.artha.ui.common.ArthaBottomBar
import com.subramanya.artha.ui.common.ArthaTopBar
import com.subramanya.artha.ui.more.MoreAction
import com.subramanya.artha.ui.more.MoreSheet
import com.subramanya.artha.ui.navigation.ArthaDestination
import com.subramanya.artha.ui.navigation.ArthaNavHost
import com.subramanya.artha.ui.navigation.SubRoutes
import com.subramanya.artha.ui.onboarding.OnboardingFlow
import com.subramanya.artha.ui.onboarding.OnboardingViewModel
import com.subramanya.artha.ui.onboarding.OnboardingViewModelFactory
import com.subramanya.artha.ui.splash.SplashScreen
import com.subramanya.artha.ui.theme.ArthaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArthaRoot() }
    }
}

/**
 * Top-level state machine: Splash gates first launch on a real DB init (forces
 * Room schema + CategorySeeder to run) and on reading `userName` from DataStore.
 * Once ready, we show either Onboarding (first run) or MainApp (returning user).
 *
 * Theme mode + dynamic-color come from SettingsPreferences so Settings → Appearance
 * changes recompose immediately.
 */
private sealed interface StartupState {
    data object Loading : StartupState
    data object NeedsOnboarding : StartupState
    data class Ready(val userName: String) : StartupState
}

private const val MIN_SPLASH_MILLIS: Long = 500L

@Composable
private fun ArthaRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val themeMode by app.settingsPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val useDynamicColor by app.settingsPreferences.useDynamicColor.collectAsState(initial = true)

    ArthaTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        // Triggers DB init + reads userName once; emits a Ready/NeedsOnboarding terminal state.
        val startup by produceState<StartupState>(initialValue = StartupState.Loading, app) {
            value = withContext(Dispatchers.IO) {
                val started = System.currentTimeMillis()
                app.database.categoryDao().count()
                val name = app.settingsPreferences.userName.first()
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < MIN_SPLASH_MILLIS) delay(MIN_SPLASH_MILLIS - elapsed)
                if (name.isBlank()) StartupState.NeedsOnboarding else StartupState.Ready(name)
            }
        }

        // Local override so completing onboarding can transition us into MainApp without
        // re-running the produceState block.
        var override: StartupState? by remember { mutableStateOf(null) }
        val current = override ?: startup

        when (val state = current) {
            StartupState.Loading -> SplashScreen()
            StartupState.NeedsOnboarding -> {
                val vm: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModelFactory(app.accountRepository, app.settingsPreferences),
                )
                OnboardingFlow(
                    viewModel = vm,
                    onCompleted = {
                        val saved = vm.state.value.name.trim()
                        override = StartupState.Ready(saved)
                    },
                )
            }
            is StartupState.Ready -> MainApp(
                settingsPreferences = app.settingsPreferences,
                initialName = state.userName,
            )
        }
    }
}

@Composable
private fun MainApp(
    settingsPreferences: SettingsPreferences,
    initialName: String,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = ArthaDestination.fromRoute(backStackEntry?.destination?.route)

    val userName by settingsPreferences.userName.collectAsState(initial = initialName)
    var showMoreSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ArthaTopBar(userName = userName) },
        bottomBar = {
            ArthaBottomBar(
                currentDestination = currentDestination,
                onItemSelected = { destination ->
                    if (destination == ArthaDestination.More) {
                        showMoreSheet = true
                    } else if (destination != currentDestination) {
                        val startDestId = navController.graph.findStartDestination().id
                        navController.navigate(destination.route) {
                            popUpTo(startDestId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        ArthaNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showMoreSheet) {
        MoreSheet(
            onDismiss = { showMoreSheet = false },
            onActionSelected = { action ->
                showMoreSheet = false
                navigateForMoreAction(navController, action)
            },
        )
    }
}

/** Maps a More-drawer tile selection to a navigation push. */
private fun navigateForMoreAction(navController: NavHostController, action: MoreAction) {
    val route = when (action) {
        MoreAction.Categories -> SubRoutes.CATEGORIES
        MoreAction.Settings -> SubRoutes.SETTINGS
        MoreAction.About -> SubRoutes.ABOUT
    }
    navController.navigate(route) { launchSingleTop = true }
}
