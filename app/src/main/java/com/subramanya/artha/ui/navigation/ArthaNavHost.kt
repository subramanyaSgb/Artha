package com.subramanya.artha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.subramanya.artha.ui.accounts.AccountsScreen
import com.subramanya.artha.ui.cards.CardsScreen
import com.subramanya.artha.ui.dashboard.DashboardScreen
import com.subramanya.artha.ui.transactions.TransactionsScreen

@Composable
fun ArthaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ArthaDestination.Dashboard.route,
        modifier = modifier,
    ) {
        composable(ArthaDestination.Dashboard.route) { DashboardScreen() }
        composable(ArthaDestination.Transactions.route) { TransactionsScreen() }
        composable(ArthaDestination.Accounts.route) { AccountsScreen() }
        composable(ArthaDestination.Cards.route) { CardsScreen() }
        // No `more` route on purpose — that tap opens a sheet, not a destination.
    }
}
