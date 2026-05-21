package com.subramanya.artha.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    var showSheet by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.screen_dashboard_stub),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center),
            )

            // FAB: tap → manual sheet. Long-press → AI Quick Entry toast (Phase 3).
            // detectTapGestures owns both events so the FAB's own onClick is left as a no-op.
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.dashboard_fab_add)) },
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showSheet = true },
                            onLongPress = {
                                Toast.makeText(
                                    context,
                                    R.string.dashboard_fab_ai_toast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    },
            )
        }
    }

    if (showSheet) {
        val vm: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModelFactory(
                accountRepository = app.accountRepository,
                cardRepository = app.cardRepository,
                categoryRepository = app.categoryRepository,
                personRepository = app.personRepository,
                tagRepository = app.tagRepository,
                transactionRepository = app.transactionRepository,
            ),
        )
        AddTransactionSheet(viewModel = vm, onDismiss = { showSheet = false })
    }
}
