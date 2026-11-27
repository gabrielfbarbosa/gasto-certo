package br.com.gabriel.gastocerto.ui.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.EntryStatus
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.ui.components.AmountDialog
import br.com.gabriel.gastocerto.ui.components.AmountField
import br.com.gabriel.gastocerto.ui.components.AmountRow
import br.com.gabriel.gastocerto.ui.components.DateField
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.MonthSelector
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.Section
import br.com.gabriel.gastocerto.ui.components.TextInput
import br.com.gabriel.gastocerto.ui.title
import br.com.gabriel.gastocerto.ui.toDisplay
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_cancel
import gastocerto.shared.generated.resources.action_ok
import gastocerto.shared.generated.resources.close_month_check
import gastocerto.shared.generated.resources.close_month_real_amount
import gastocerto.shared.generated.resources.entries_corrected
import gastocerto.shared.generated.resources.entries_deleted
import gastocerto.shared.generated.resources.entries_edit
import gastocerto.shared.generated.resources.entries_empty
import gastocerto.shared.generated.resources.entries_expected
import gastocerto.shared.generated.resources.entries_income_saved
import gastocerto.shared.generated.resources.entries_list
import gastocerto.shared.generated.resources.entries_new_income
import gastocerto.shared.generated.resources.entries_title
import gastocerto.shared.generated.resources.expense_amount
import gastocerto.shared.generated.resources.expense_date
import gastocerto.shared.generated.resources.expense_description
import gastocerto.shared.generated.resources.registry_delete
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC08 + UC09 – one-off income and the entries of the month to check, correct or delete. */
@Composable
fun EntriesScreen(onBack: () -> Unit, viewModel: EntriesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    var selected by remember { mutableStateOf<Entry?>(null) }
    var checking by remember { mutableStateOf<Entry?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(
                when (event) {
                    is EntriesEvent.IncomeSaved -> getString(Res.string.entries_income_saved, event.month.toDisplay())
                    is EntriesEvent.Corrected -> getString(Res.string.entries_corrected, event.description)
                    is EntriesEvent.Deleted -> getString(Res.string.entries_deleted, event.description)
                },
            )
        }
    }

    ScreenColumn(stringResource(Res.string.entries_title), onBack = onBack) {
        ErrorText(state.error)
        Section(stringResource(Res.string.entries_new_income)) {
            val form = state.incomeForm
            AmountField(form.amount, { amount -> viewModel.updateIncomeForm { copy(amount = amount) } }, stringResource(Res.string.expense_amount))
            DateField(form.date, { date -> viewModel.updateIncomeForm { copy(date = date) } }, stringResource(Res.string.expense_date))
            TextInput(form.description, { text -> viewModel.updateIncomeForm { copy(description = text) } }, stringResource(Res.string.expense_description))
            Button(onClick = viewModel::registerIncome, enabled = form.description.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.entries_new_income))
            }
        }
        MonthSelector(state.month, viewModel::load)
        Section(stringResource(Res.string.entries_list)) {
            if (state.entries.isEmpty()) Text(stringResource(Res.string.entries_empty))
            state.entries.forEach { entry ->
                Column(Modifier.fillMaxWidth().clickable { selected = entry }.padding(vertical = 4.dp)) {
                    val label = entry.title()
                    AmountRow(if (entry is Expense) label else "+ $label", entry.amount)
                    val where = (entry as? Expense)?.paymentMethod?.name?.let { " · $it" } ?: ""
                    val expected = if (entry.status == EntryStatus.EXPECTED) " · " + stringResource(Res.string.entries_expected) else ""
                    Text(entry.date.toDisplay() + where + expected, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    selected?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onSave = { amount, description -> selected = null; viewModel.edit(entry, amount, description) },
            onCheck = { selected = null; checking = entry },
            onDelete = { selected = null; viewModel.delete(entry) },
            onDismiss = { selected = null },
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

/** UC09 – edit amount and description, check an expected entry or delete it. */
@Composable
private fun EditEntryDialog(
    entry: Entry,
    onSave: (Long?, String) -> Unit,
    onCheck: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf<Long?>(entry.amount.cents) }
    var description by remember { mutableStateOf(entry.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.entries_edit)) },
        text = {
            Column {
                AmountField(amount, { amount = it }, stringResource(Res.string.expense_amount))
                TextInput(description, { description = it }, stringResource(Res.string.expense_description))
                if (entry.status == EntryStatus.EXPECTED) {
                    TextButton(onClick = onCheck) { Text(stringResource(Res.string.close_month_check)) }
                }
                TextButton(onClick = onDelete) { Text(stringResource(Res.string.registry_delete), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(amount, description) }) { Text(stringResource(Res.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
