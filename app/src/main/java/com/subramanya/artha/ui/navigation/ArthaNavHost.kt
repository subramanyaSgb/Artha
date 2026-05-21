package com.subramanya.artha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.subramanya.artha.ui.accounts.AccountDetailScreen
import com.subramanya.artha.ui.accounts.AccountsScreen
import com.subramanya.artha.ui.cards.CardsScreen
import com.subramanya.artha.ui.dashboard.DashboardScreen
import com.subramanya.artha.ui.transactions.TransactionsScreen

/** Sub-routes outside the bottom-nav tabs. Account/Card detail screens land here. */
object SubRoutes {
    private const val ACCOUNT_DETAIL_BASE = "account_detail"
    const val ACCOUNT_DETAIL_ARG_ID = "accountId"
    const val ACCOUNT_DETAIL_PATTERN = "$ACCOUNT_DETAIL_BASE/{$ACCOUNT_DETAIL_ARG_ID}"

    fun accountDetail(accountId: String): String = "$ACCOUNT_DETAIL_BASE/$accountId"
}

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
        composable(ArthaDestination.Accounts.route) {
            AccountsScreen(
                onOpenAccount = { id -> navController.navigate(SubRoutes.accountDetail(id)) },
            )
        }
        composable(ArthaDestination.Cards.route) { CardsScreen() }
        composable(
            route = SubRoutes.ACCOUNT_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.ACCOUNT_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val accountId = entry.arguments?.getString(SubRoutes.ACCOUNT_DETAIL_ARG_ID).orEmpty()
            AccountDetailScreen(
                accountId = accountId,
                onBack = { navController.popBackStack() },
            )
        }
        // No `more` route on purpose — that tap opens a sheet, not a destination.
    }
}
