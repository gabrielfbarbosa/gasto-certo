package br.com.gabriel.gastocerto.ui.closemonth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.ui.components.AmountDialog
import br.com.gabriel.gastocerto.ui.components.AmountRow
import br.com.gabriel.gastocerto.ui.components.ConfirmDialog
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.MonthSelector
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.Section
import br.com.gabriel.gastocerto.ui.toBrl
import br.com.gabriel.gastocerto.ui.title
import br.com.gabriel.gastocerto.ui.toDisplay
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.close_month_action
import gastocerto.shared.generated.resources.close_month_check
import gastocerto.shared.generated.resources.close_month_closed
import gastocerto.shared.generated.resources.close_month_confirm_text
import gastocerto.shared.generated.resources.close_month_confirmed
import gastocerto.shared.generated.resources.close_month_done
import gastocerto.shared.generated.resources.close_month_expected
import gastocerto.shared.generated.resources.close_month_no_expected
import gastocerto.shared.generated.resources.close_month_open
import gastocerto.shared.generated.resources.close_month_real_amount
import gastocerto.shared.generated.resources.close_month_reopen
import gastocerto.shared.generated.resources.close_month_reopened
import gastocerto.shared.generated.resources.close_month_title
import gastocerto.shared.generated.resources.summary_expenses
import gastocerto.shared.generated.resources.summary_income
import gastocerto.shared.generated.resources.summary_surplus
import gastocerto.shared.generated.resources.summary_third_party
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC03 – summary of the month, expected entries to check, close and reopen. */
@Composable
fun CloseMonthScreen(viewModel: CloseMonthViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    var askClose by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf<Entry?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.load(state.month)
        viewModel.events.collect { event ->
            val text = when (event) {
                is CloseMonthEvent.Closed -> getString(
                    Res.string.close_month_done, event.month.toDisplay(), event.next.toDisplay(), event.expectedCount,
                )
                is CloseMonthEvent.Reopened -> getString(Res.string.close_month_reopened, event.month.toDisplay())
                is CloseMonthEvent.Confirmed -> getString(Res.string.close_month_confirmed, event.description)
            }
            snackbar.showSnackbar(text)
        }
    }

    ScreenColumn(stringResource(Res.string.close_month_title)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MonthSelector(state.month, viewModel::load)
            AssistChip(
                onClick = {},
                label = { Text(stringResource(if (state.isClosed) Res.string.close_month_closed else Res.string.close_month_open)) },
            )
        }
        state.summary?.let { summary ->
            Section(stringResource(Res.string.summary_surplus) + ": " + summary.surplus().toBrl()) {
                AmountRow(stringResource(Res.string.summary_income), summary.income)
                AmountRow(stringResource(Res.string.summary_expenses), summary.totalExpenses)
                AmountRow(stringResource(Res.string.summary_third_party), summary.thirdPartyExpenses)
            }
        }
        Section(stringResource(Res.string.close_month_expected)) {
            if (state.expected.isEmpty()) {
                Text(stringResource(Res.string.close_month_no_expected), style = MaterialTheme.typography.bodyMedium)
            }
            state.expected.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        AmountRow(entry.title(), entry.amount)
                        val where = (entry as? Expense)?.paymentMethod?.name?.let { " · $it" } ?: ""
                        Text(entry.date.toDisplay() + where, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!state.isClosed) {
                        TextButton(onClick = { checking = entry }) { Text(stringResource(Res.string.close_month_check)) }
                    }
                }
            }
        }
        ErrorText(state.error)
        if (state.isClosed) {
            OutlinedButton(onClick = viewModel::reopen, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.close_month_reopen))
            }
        } else {
            Button(onClick = { askClose = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.close_month_action, state.month.toDisplay()))
            }
        }
    }

    if (askClose) {
        ConfirmDialog(
            title = stringResource(Res.string.close_month_action, state.month.toDisplay()),
            text = stringResource(Res.string.close_month_confirm_text, state.expected.size),
            confirmLabel = stringResource(Res.string.close_month_action, state.month.toDisplay()),
            onConfirm = { askClose = false; viewModel.close() },
            onDismiss = { askClose = false },
        )
    }
    checking?.let { entry ->
        AmountDialog(
            title = entry.title(),
            label = stringResource(Res.string.close_month_real_amount),
            initial = entry.amount.cents,
            onConfirm = { amount -> checking = null; viewModel.confirmExpected(entry, amount) },
            onDismiss = { checking = null },
        )
    }
}
