package br.com.gabriel.gastocerto.ui.investments

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.ui.components.AmountDialog
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.Section
import br.com.gabriel.gastocerto.ui.toBrl
import br.com.gabriel.gastocerto.ui.toDisplay
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.expense_amount
import gastocerto.shared.generated.resources.investments_contribution
import gastocerto.shared.generated.resources.investments_contribution_set
import gastocerto.shared.generated.resources.investments_empty
import gastocerto.shared.generated.resources.investments_no_contribution
import gastocerto.shared.generated.resources.investments_sale
import gastocerto.shared.generated.resources.investments_sale_saved
import gastocerto.shared.generated.resources.investments_set_contribution
import gastocerto.shared.generated.resources.investments_title
import gastocerto.shared.generated.resources.registry_planned_contribution
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class InvestmentAction { CONTRIBUTION, SALE }

/** UC10 – planned contribution of each brokerage (RN11) and sales as income. */
@Composable
fun InvestmentsScreen(onBack: () -> Unit, viewModel: InvestmentsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    var dialog by remember { mutableStateOf<Pair<InvestmentAccount, InvestmentAction>?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(
                when (event) {
                    is InvestmentsEvent.ContributionSet -> getString(Res.string.investments_contribution_set, event.account)
                    is InvestmentsEvent.SaleRegistered -> getString(Res.string.investments_sale_saved, event.account, event.month.toDisplay())
                },
            )
        }
    }

    ScreenColumn(stringResource(Res.string.investments_title), onBack = onBack) {
        ErrorText(state.error)
        if (state.accounts.isEmpty()) Text(stringResource(Res.string.investments_empty))
        state.accounts.forEach { account ->
            Section(account.name) {
                Text(
                    account.plannedMonthlyContribution?.let { stringResource(Res.string.investments_contribution, it.toBrl()) }
                        ?: stringResource(Res.string.investments_no_contribution),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row {
                    TextButton(onClick = { dialog = account to InvestmentAction.CONTRIBUTION }) {
                        Text(stringResource(Res.string.investments_set_contribution))
                    }
                    TextButton(onClick = { dialog = account to InvestmentAction.SALE }) {
                        Text(stringResource(Res.string.investments_sale))
                    }
                }
            }
        }
    }

    dialog?.let { (account, action) ->
        AmountDialog(
            title = account.name,
            label = stringResource(if (action == InvestmentAction.SALE) Res.string.expense_amount else Res.string.registry_planned_contribution),
            initial = if (action == InvestmentAction.CONTRIBUTION) account.plannedMonthlyContribution?.cents else null,
            onConfirm = { amount ->
                dialog = null
                if (action == InvestmentAction.SALE) viewModel.registerSale(account, amount)
                else viewModel.setPlannedContribution(account, amount)
            },
            onDismiss = { dialog = null },
        )
    }
}
