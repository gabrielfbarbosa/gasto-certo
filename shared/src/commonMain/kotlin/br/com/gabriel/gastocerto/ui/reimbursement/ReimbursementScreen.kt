package br.com.gabriel.gastocerto.ui.reimbursement

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.ui.components.AmountField
import br.com.gabriel.gastocerto.ui.components.AmountRow
import br.com.gabriel.gastocerto.ui.components.DateField
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.Section
import br.com.gabriel.gastocerto.ui.components.SelectField
import br.com.gabriel.gastocerto.ui.toBrl
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.expense_amount
import gastocerto.shared.generated.resources.expense_date
import gastocerto.shared.generated.resources.reimbursement_balances
import gastocerto.shared.generated.resources.reimbursement_register
import gastocerto.shared.generated.resources.reimbursement_saved
import gastocerto.shared.generated.resources.reimbursement_third_party
import gastocerto.shared.generated.resources.reimbursement_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC11 – what each third party owes and the reimbursements received (RN03). */
@Composable
fun ReimbursementScreen(onBack: () -> Unit, viewModel: ReimbursementViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is ReimbursementEvent.Registered) {
                snackbar.showSnackbar(getString(Res.string.reimbursement_saved, event.thirdParty, event.remaining.toBrl()))
            }
        }
    }

    ScreenColumn(stringResource(Res.string.reimbursement_title), onBack = onBack) {
        Section(stringResource(Res.string.reimbursement_balances)) {
            state.balances.forEach { AmountRow(it.thirdParty.name, it.amount) }
        }
        SelectField(
            stringResource(Res.string.reimbursement_third_party), state.balances.map { it.thirdParty }, state.thirdParty,
            { it.name }, viewModel::selectThirdParty,
        )
        AmountField(state.amount, viewModel::setAmount, stringResource(Res.string.expense_amount))
        DateField(state.date, viewModel::setDate, stringResource(Res.string.expense_date))
        ErrorText(state.error)
        Button(onClick = viewModel::register, enabled = state.thirdParty != null, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.reimbursement_register))
        }
    }
}
