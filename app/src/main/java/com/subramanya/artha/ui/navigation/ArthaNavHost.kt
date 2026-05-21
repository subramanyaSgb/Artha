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
import com.subramanya.artha.ui.cards.CardDetailScreen
import com.subramanya.artha.ui.cards.CardsScreen
import com.subramanya.artha.ui.categories.CategoriesScreen
import com.subramanya.artha.ui.dashboard.DashboardScreen
import com.subramanya.artha.ui.settings.AboutScreen
import com.subramanya.artha.ui.settings.SettingsScreen
import com.subramanya.artha.ui.transactions.TransactionDetailScreen
import com.subramanya.artha.ui.transactions.TransactionsScreen

/** Sub-routes outside the bottom-nav tabs. Detail screens, More-drawer destinations land here. */
object SubRoutes {
    private const val ACCOUNT_DETAIL_BASE = "account_detail"
    const val ACCOUNT_DETAIL_ARG_ID = "accountId"
    const val ACCOUNT_DETAIL_PATTERN = "$ACCOUNT_DETAIL_BASE/{$ACCOUNT_DETAIL_ARG_ID}"
    fun accountDetail(accountId: String): String = "$ACCOUNT_DETAIL_BASE/$accountId"

    private const val CARD_DETAIL_BASE = "card_detail"
    const val CARD_DETAIL_ARG_ID = "cardId"
    const val CARD_DETAIL_PATTERN = "$CARD_DETAIL_BASE/{$CARD_DETAIL_ARG_ID}"
    fun cardDetail(cardId: String): String = "$CARD_DETAIL_BASE/$cardId"

    private const val TRANSACTION_DETAIL_BASE = "transaction_detail"
    const val TRANSACTION_DETAIL_ARG_ID = "transactionId"
    const val TRANSACTION_DETAIL_PATTERN = "$TRANSACTION_DETAIL_BASE/{$TRANSACTION_DETAIL_ARG_ID}"
    fun transactionDetail(transactionId: String): String = "$TRANSACTION_DETAIL_BASE/$transactionId"

    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
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
        composable(ArthaDestination.Transactions.route) {
            TransactionsScreen(
                onOpenTransaction = { id -> navController.navigate(SubRoutes.transactionDetail(id)) },
            )
        }
        composable(ArthaDestination.Accounts.route) {
            AccountsScreen(
                onOpenAccount = { id -> navController.navigate(SubRoutes.accountDetail(id)) },
            )
        }
        composable(ArthaDestination.Cards.route) {
            CardsScreen(
                onOpenCard = { id -> navController.navigate(SubRoutes.cardDetail(id)) },
            )
        }
        composable(
            route = SubRoutes.ACCOUNT_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.ACCOUNT_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.ACCOUNT_DETAIL_ARG_ID).orEmpty()
            AccountDetailScreen(accountId = id, onBack = { navController.popBackStack() })
        }
        composable(
            route = SubRoutes.CARD_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.CARD_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.CARD_DETAIL_ARG_ID).orEmpty()
            CardDetailScreen(cardId = id, onBack = { navController.popBackStack() })
        }
        composable(
            route = SubRoutes.TRANSACTION_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.TRANSACTION_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.TRANSACTION_DETAIL_ARG_ID).orEmpty()
            TransactionDetailScreen(transactionId = id, onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(SubRoutes.ABOUT) },
            )
        }
        composable(SubRoutes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        // No `more` route on purpose — that tap opens a sheet, not a destination.
    }
}
