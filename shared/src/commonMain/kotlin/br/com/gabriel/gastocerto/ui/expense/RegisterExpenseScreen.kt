package br.com.gabriel.gastocerto.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.ui.components.AmountField
import br.com.gabriel.gastocerto.ui.components.DateField
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.NumberInput
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.SelectField
import br.com.gabriel.gastocerto.ui.components.TextInput
import br.com.gabriel.gastocerto.ui.toBrl
import br.com.gabriel.gastocerto.ui.toDisplay
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_cancel
import gastocerto.shared.generated.resources.expense_amount
import gastocerto.shared.generated.resources.expense_confirm
import gastocerto.shared.generated.resources.expense_converted
import gastocerto.shared.generated.resources.expense_date
import gastocerto.shared.generated.resources.expense_description
import gastocerto.shared.generated.resources.expense_exchange_rate
import gastocerto.shared.generated.resources.expense_foreign_amount
import gastocerto.shared.generated.resources.expense_installment_line
import gastocerto.shared.generated.resources.expense_installments
import gastocerto.shared.generated.resources.expense_no_payment_methods
import gastocerto.shared.generated.resources.expense_no_third_party
import gastocerto.shared.generated.resources.expense_payment_method
import gastocerto.shared.generated.resources.expense_preview
import gastocerto.shared.generated.resources.expense_reopen_confirm
import gastocerto.shared.generated.resources.expense_reopen_text
import gastocerto.shared.generated.resources.expense_reopen_title
import gastocerto.shared.generated.resources.expense_saved
import gastocerto.shared.generated.resources.expense_third_party
import gastocerto.shared.generated.resources.expense_title
import gastocerto.shared.generated.resources.expense_total_amount
import gastocerto.shared.generated.resources.purchase_saved
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC01 + UC02 – register an expense, single or in installments; the screen only draws the state. */
@Composable
fun RegisterExpenseScreen(viewModel: RegisterExpenseViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    var reopenOffer by remember { mutableStateOf<MonthYear?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.reload()
        viewModel.events.collect { event ->
            when (event) {
                is RegisterExpenseEvent.ExpenseSaved ->
                    snackbar.showSnackbar(getString(Res.string.expense_saved, event.accountingMonth.toDisplay()))
                is RegisterExpenseEvent.PurchaseSaved ->
                    snackbar.showSnackbar(getString(Res.string.purchase_saved, event.count, event.firstMonth.toDisplay()))
                is RegisterExpenseEvent.OfferReopen -> reopenOffer = event.month
            }
        }
    }

    ScreenColumn(stringResource(Res.string.expense_title)) {
        if (state.paymentMethods.isEmpty()) {
            Text(stringResource(Res.string.expense_no_payment_methods))
            return@ScreenColumn
        }
        SelectField(
            label = stringResource(Res.string.expense_payment_method),
            options = state.paymentMethods,
            selected = state.paymentMethod,
            optionLabel = { it.name },
            onSelect = viewModel::selectPaymentMethod,
        )
        val wallet = state.paymentMethod as? ForeignCurrencyWallet
        if (wallet != null) {
            AmountField(state.foreignAmount, viewModel::setForeignAmount, stringResource(Res.string.expense_foreign_amount), prefix = wallet.currency)
            AmountField(state.exchangeRate, viewModel::setExchangeRate, stringResource(Res.string.expense_exchange_rate), prefix = "R$/${wallet.currency}", decimals = 4)
            state.convertedAmount?.let { Text(stringResource(Res.string.expense_converted, it.toBrl())) }
        } else {
            val label = if (state.isInstallmentPurchase) Res.string.expense_total_amount else Res.string.expense_amount
            AmountField(state.amount, viewModel::setAmount, stringResource(label))
        }
        DateField(state.date, viewModel::setDate, stringResource(Res.string.expense_date))
        TextInput(state.description, viewModel::setDescription, stringResource(Res.string.expense_description))
        SelectField<Person?>(
            label = stringResource(Res.string.expense_third_party),
            options = listOf<Person?>(null) + state.thirdParties,
            selected = state.thirdParty,
            optionLabel = { it?.name ?: stringResource(Res.string.expense_no_third_party) },
            onSelect = viewModel::selectThirdParty,
            placeholder = stringResource(Res.string.expense_no_third_party),
        )
        if (state.isCreditCard) {
            NumberInput(state.installmentCount, viewModel::setInstallmentCount, stringResource(Res.string.expense_installments))
        }
        ErrorText(state.error)
        if (state.isInstallmentPurchase) {
            OutlinedButton(onClick = viewModel::previewInstallments, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.expense_preview))
            }
        }
        if (state.preview.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.preview.forEach { installment ->
                        Text(
                            stringResource(
                                Res.string.expense_installment_line, installment.label(),
                                installment.amount.toBrl(), installment.accountingMonth.toDisplay(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        Button(onClick = viewModel::confirm, enabled = state.canConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.expense_confirm))
        }
    }

    reopenOffer?.let { month ->
        AlertDialog(
            onDismissRequest = { reopenOffer = null },
            title = { Text(stringResource(Res.string.expense_reopen_title)) },
            text = { Text(stringResource(Res.string.expense_reopen_text, month.toDisplay())) },
            confirmButton = {
                TextButton(onClick = { reopenOffer = null; viewModel.reopenAndConfirm(month) }) {
                    Text(stringResource(Res.string.expense_reopen_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { reopenOffer = null }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}


