package com.subramanya.artha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subramanya.artha.ui.common.ArthaBottomBar
import com.subramanya.artha.ui.common.ArthaTopBar
import com.subramanya.artha.ui.more.MoreAction
import com.subramanya.artha.ui.more.MoreSheet
import com.subramanya.artha.ui.navigation.ArthaDestination
import com.subramanya.artha.ui.navigation.ArthaNavHost
import com.subramanya.artha.ui.theme.ArthaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArthaTheme {
                ArthaApp()
            }
        }
    }
}

@Composable
private fun ArthaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = ArthaDestination.fromRoute(backStackEntry?.destination?.route)

    var showMoreSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ArthaTopBar() },
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
                // Phase 1: destinations for these actions don't exist yet (Categories/Settings/About
                // land in Sessions 9 and 10). Close the sheet for now — wiring follows in those sessions.
                showMoreSheet = false
                when (action) {
                    MoreAction.Categories,
                    MoreAction.Settings,
                    MoreAction.About,
                    -> Unit
                }
            },
        )
    }
}
