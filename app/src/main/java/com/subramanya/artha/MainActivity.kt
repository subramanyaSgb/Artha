package com.subramanya.artha

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subramanya.artha.data.importing.BankImporter
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.preferences.ThemeMode
import com.subramanya.artha.ui.common.ArthaBottomBar
import com.subramanya.artha.ui.common.ArthaTopBar
import com.subramanya.artha.ui.update.UpdateDialog
import com.subramanya.artha.ui.update.UpdateDialogState
import com.subramanya.artha.utils.AppUpdateChecker
import com.subramanya.artha.utils.UpdateInfo
import com.subramanya.artha.ui.lock.BiometricLockGate
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
import com.subramanya.artha.utils.ReceiptStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Switched to [FragmentActivity] in Phase 5 — BiometricPrompt requires a
 * FragmentActivity host. Behaviour is otherwise unchanged from ComponentActivity.
 */
class MainActivity : FragmentActivity() {

    // Compose-observable state — changes from onNewIntent recompose ArthaRoot automatically.
    private var pendingShareUri by mutableStateOf<Uri?>(null)

    // Bumped each time the SMS-review notification is tapped; MainApp navigates on change.
    private var openReviewToken by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingShareUri = extractShareImageUri(intent)
        if (wantsReview(intent)) openReviewToken++
        setContent { ArthaRoot(pendingShareUri = pendingShareUri, openReviewToken = openReviewToken) }
    }

    /** Handles share + review-notification intents when Artha is already running (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractShareImageUri(intent)?.let { pendingShareUri = it }
        if (wantsReview(intent)) openReviewToken++
    }

    private fun wantsReview(intent: Intent?): Boolean =
        intent?.getBooleanExtra(com.subramanya.artha.sms.PendingTransactionNotifier.EXTRA_OPEN_REVIEW, false) == true

    private fun extractShareImageUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("image/") != true) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
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

/** Bumped whenever the bundled bank-statement asset (or the schema it writes into)
 *  changes so existing installs re-run the importer after a destructive migration.
 *  Tracks AppDatabase.version for now. */
private const val CURRENT_BUNDLED_IMPORT_VERSION: Int = 3

@Composable
private fun ArthaRoot(pendingShareUri: Uri?, openReviewToken: Int) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val themeMode by app.settingsPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val useDynamicColor by app.settingsPreferences.useDynamicColor.collectAsState(initial = true)
    val biometricLock by app.settingsPreferences.biometricLockEnabled.collectAsState(initial = false)

    ArthaTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        if (biometricLock) {
            BiometricLockGate { ArthaInner(app, pendingShareUri, openReviewToken) }
        } else {
            ArthaInner(app, pendingShareUri, openReviewToken)
        }
    }
}

@Composable
private fun ArthaInner(app: ArthaApplication, pendingShareUri: Uri?, openReviewToken: Int) {
    val startup by produceState<StartupState>(initialValue = StartupState.Loading, app) {
        value = withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            app.database.categoryDao().count()
            if (app.settingsPreferences.bundledImportVersion.first() < CURRENT_BUNDLED_IMPORT_VERSION) {
                runCatching { BankImporter(app, app.database).importBundled() }
                app.settingsPreferences.setBundledImportVersion(CURRENT_BUNDLED_IMPORT_VERSION)
            }
            runCatching {
                ReceiptStore.pruneOrphans(app, app.database.transactionDao().allReceiptUris())
            }
            val name = app.settingsPreferences.userName.first()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < MIN_SPLASH_MILLIS) delay(MIN_SPLASH_MILLIS - elapsed)
            if (name.isBlank()) StartupState.NeedsOnboarding else StartupState.Ready(name)
        }
    }

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
            pendingShareUri = pendingShareUri,
            openReviewToken = openReviewToken,
        )
    }
}

@Composable
private fun MainApp(
    settingsPreferences: SettingsPreferences,
    initialName: String,
    pendingShareUri: Uri?,
    openReviewToken: Int,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = ArthaDestination.fromRoute(backStackEntry?.destination?.route)

    val userName by settingsPreferences.userName.collectAsState(initial = initialName)
    var showMoreSheet by remember { mutableStateOf(false) }

    // In-app update state
    var updateDialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    // Check for updates 4 s after the main screen settles — non-blocking, silent on failure.
    LaunchedEffect(Unit) {
        delay(4_000)
        val checker = AppUpdateChecker(context)
        val info = withContext(Dispatchers.IO) {
            runCatching { checker.checkForUpdate() }.getOrNull()
        }
        if (info != null) updateDialogState = UpdateDialogState.Available(info)
    }

    // When the user shares a UPI receipt image, navigate to the import screen.
    // Using the URI string as key so a new share from onNewIntent re-triggers.
    LaunchedEffect(pendingShareUri) {
        if (pendingShareUri != null) {
            navController.navigate(SubRoutes.shareReceipt(pendingShareUri.toString())) {
                launchSingleTop = true
            }
        }
    }

    // Tapping the SMS-review notification deep-links into the Review screen. Keyed on the
    // token so each tap re-navigates (0 is the initial "no request" value).
    LaunchedEffect(openReviewToken) {
        if (openReviewToken > 0) {
            navController.navigate(SubRoutes.REVIEW) { launchSingleTop = true }
        }
    }

    val isBottomNavRoute = currentDestination != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            if (isBottomNavRoute) {
                ArthaTopBar(
                    userName = userName,
                    onSearchClick = {
                        navController.navigate(SubRoutes.SEARCH) { launchSingleTop = true }
                    },
                )
            }
        },
        bottomBar = {
            ArthaBottomBar(
                currentDestination = currentDestination,
                onItemSelected = { destination ->
                    if (destination == ArthaDestination.More) {
                        showMoreSheet = true
                    } else {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
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

    val dialogState = updateDialogState
    if (dialogState != null) {
        val checker = AppUpdateChecker(context)
        UpdateDialog(
            state = dialogState,
            onDismiss = { updateDialogState = null },
            onDownload = { info ->
                updateDialogState = UpdateDialogState.Downloading(info, 0f)
                scope.launch {
                    val apk = checker.downloadApk(info.downloadUrl) { progress ->
                        updateDialogState = UpdateDialogState.Downloading(info, progress)
                    }
                    if (apk != null) {
                        downloadedApk = apk
                        checker.triggerInstall(apk)
                        updateDialogState = null
                    } else {
                        updateDialogState = UpdateDialogState.Failed(info)
                    }
                }
            },
            onInstall = {
                downloadedApk?.let { checker.triggerInstall(it) }
            },
        )
    }
}

/** Maps a More-drawer tile selection to a navigation push. */
private fun navigateForMoreAction(navController: NavHostController, action: MoreAction) {
    val route = when (action) {
        MoreAction.Categories -> SubRoutes.CATEGORIES
        MoreAction.Tags -> SubRoutes.TAGS
        MoreAction.Settings -> SubRoutes.SETTINGS
        MoreAction.About -> SubRoutes.ABOUT
        MoreAction.Investments -> SubRoutes.INVESTMENTS
        MoreAction.Insurance -> SubRoutes.INSURANCES
        MoreAction.Rules -> SubRoutes.RULES
        MoreAction.People -> SubRoutes.PEOPLE
        MoreAction.Budgets -> SubRoutes.BUDGETS
        MoreAction.Goals -> SubRoutes.GOALS
        MoreAction.Subscriptions -> SubRoutes.SUBSCRIPTIONS
        MoreAction.Recurring -> SubRoutes.RECURRING
        MoreAction.Reports -> SubRoutes.REPORTS
    }
    navController.navigate(route) { launchSingleTop = true }
}
