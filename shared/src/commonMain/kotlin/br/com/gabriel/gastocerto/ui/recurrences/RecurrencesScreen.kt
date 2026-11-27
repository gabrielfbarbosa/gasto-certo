package br.com.gabriel.gastocerto.ui.recurrences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.ui.components.AmountField
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.NumberInput
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.Section
import br.com.gabriel.gastocerto.ui.components.SelectField
import br.com.gabriel.gastocerto.ui.components.TextInput
import br.com.gabriel.gastocerto.ui.toBrl
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_add
import gastocerto.shared.generated.resources.expense_amount
import gastocerto.shared.generated.resources.expense_description
import gastocerto.shared.generated.resources.expense_payment_method
import gastocerto.shared.generated.resources.recurrences_bills
import gastocerto.shared.generated.resources.recurrences_day_line
import gastocerto.shared.generated.resources.recurrences_deactivated
import gastocerto.shared.generated.resources.recurrences_due_day
import gastocerto.shared.generated.resources.recurrences_incomes
import gastocerto.shared.generated.resources.recurrences_pay_day
import gastocerto.shared.generated.resources.recurrences_saved
import gastocerto.shared.generated.resources.recurrences_stop
import gastocerto.shared.generated.resources.recurrences_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC07 – recurring bills (RN06) and salary (RN07). */
@Composable
fun RecurrencesScreen(onBack: () -> Unit, viewModel: RecurrencesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(
                when (event) {
                    is RecurrencesEvent.Saved -> getString(Res.string.recurrences_saved, event.description)
                    is RecurrencesEvent.Deactivated -> getString(Res.string.recurrences_deactivated, event.description)
                },
            )
        }
    }

    ScreenColumn(stringResource(Res.string.recurrences_title), onBack = onBack) {
        ErrorText(state.error)
        Section(stringResource(Res.string.recurrences_bills)) {
            state.bills.forEach { bill ->
                RecurrenceRow(
                    bill.description,
                    stringResource(Res.string.recurrences_day_line, bill.expectedAmount.toBrl(), bill.dueDay) + " · " + bill.paymentMethod.name,
                ) { viewModel.deactivateBill(bill) }
            }
            val form = state.billForm
            TextInput(form.description, { text -> viewModel.updateBillForm { copy(description = text) } }, stringResource(Res.string.expense_description))
            AmountField(form.amount, { amount -> viewModel.updateBillForm { copy(amount = amount) } }, stringResource(Res.string.expense_amount))
            NumberInput(form.day, { day -> viewModel.updateBillForm { copy(day = day) } }, stringResource(Res.string.recurrences_due_day))
            SelectField(stringResource(Res.string.expense_payment_method), state.paymentMethods, form.paymentMethod, { it.name },
                { method -> viewModel.updateBillForm { copy(paymentMethod = method) } })
            Button(onClick = viewModel::addBill, enabled = form.description.isNotBlank() && form.paymentMethod != null,
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.action_add)) }
        }
        Section(stringResource(Res.string.recurrences_incomes)) {
            state.incomes.forEach { income ->
                RecurrenceRow(income.description, stringResource(Res.string.recurrences_day_line, income.amount.toBrl(), income.payDay)) {
                    viewModel.deactivateIncome(income)
                }
            }
            val form = state.incomeForm
            TextInput(form.description, { text -> viewModel.updateIncomeForm { copy(description = text) } }, stringResource(Res.string.expense_description))
            AmountField(form.amount, { amount -> viewModel.updateIncomeForm { copy(amount = amount) } }, stringResource(Res.string.expense_amount))
            NumberInput(form.day, { day -> viewModel.updateIncomeForm { copy(day = day) } }, stringResource(Res.string.recurrences_pay_day))
            Button(onClick = viewModel::addIncome, enabled = form.description.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_add))
            }
        }
    }
}

@Composable
private fun RecurrenceRow(title: String, detail: String, onStop: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onStop) { Text(stringResource(Res.string.recurrences_stop)) }
    }
}
