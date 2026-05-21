package com.subramanya.artha.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.StubScreen

@Composable
fun TransactionsScreen(modifier: Modifier = Modifier) {
    StubScreen(label = stringResource(R.string.screen_transactions_stub), modifier = modifier)
}
