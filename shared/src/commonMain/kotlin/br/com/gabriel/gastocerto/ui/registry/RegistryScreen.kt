package br.com.gabriel.gastocerto.ui.registry

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.ui.components.AmountField
import br.com.gabriel.gastocerto.ui.components.ConfirmDialog
import br.com.gabriel.gastocerto.ui.components.ErrorText
import br.com.gabriel.gastocerto.ui.components.LocalSnackbarHost
import br.com.gabriel.gastocerto.ui.components.NumberInput
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.components.Section
import br.com.gabriel.gastocerto.ui.components.SelectField
import br.com.gabriel.gastocerto.ui.components.TextInput
import br.com.gabriel.gastocerto.ui.kindLabel
import br.com.gabriel.gastocerto.ui.label
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_add
import gastocerto.shared.generated.resources.registry_card_days
import gastocerto.shared.generated.resources.registry_closing_day
import gastocerto.shared.generated.resources.registry_currency
import gastocerto.shared.generated.resources.registry_deactivate
import gastocerto.shared.generated.resources.registry_deactivated
import gastocerto.shared.generated.resources.registry_delete
import gastocerto.shared.generated.resources.registry_delete_confirm
import gastocerto.shared.generated.resources.registry_deleted
import gastocerto.shared.generated.resources.registry_due_day
import gastocerto.shared.generated.resources.registry_holder
import gastocerto.shared.generated.resources.registry_institution
import gastocerto.shared.generated.resources.registry_institutions
import gastocerto.shared.generated.resources.registry_kind
import gastocerto.shared.generated.resources.registry_name
import gastocerto.shared.generated.resources.registry_payment_methods
import gastocerto.shared.generated.resources.registry_people
import gastocerto.shared.generated.resources.registry_planned_contribution
import gastocerto.shared.generated.resources.registry_role
import gastocerto.shared.generated.resources.registry_saved
import gastocerto.shared.generated.resources.registry_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC06 – people, institutions and payment methods. */
@Composable
fun RegistryScreen(onBack: () -> Unit, viewModel: RegistryViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    var deleting by remember { mutableStateOf<PaymentMethod?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(
                when (event) {
                    is RegistryEvent.Saved -> getString(Res.string.registry_saved, event.name)
                    is RegistryEvent.Deactivated -> getString(Res.string.registry_deactivated, event.name)
                    is RegistryEvent.Deleted -> getString(Res.string.registry_deleted, event.name)
                },
            )
        }
    }

    ScreenColumn(stringResource(Res.string.registry_title), onBack = onBack) {
        ErrorText(state.error)
        Section(stringResource(Res.string.registry_payment_methods)) {
            state.paymentMethods.forEach { method ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(method.name, style = MaterialTheme.typography.bodyLarge)
                        val days = (method as? CreditCard)?.let {
                            " · " + stringResource(Res.string.registry_card_days, it.closingDay, it.dueDay)
                        } ?: ""
                        Text("${method.kindLabel()} · ${method.holder.name}$days", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { viewModel.deactivate(method) }) { Text(stringResource(Res.string.registry_deactivate)) }
                    TextButton(onClick = { deleting = method }) { Text(stringResource(Res.string.registry_delete)) }
                }
            }
            val form = state.form
            SelectField(stringResource(Res.string.registry_kind), PaymentMethodType.entries, form.type, { it.label() },
                { type -> viewModel.updateForm { copy(type = type) } })
            TextInput(form.name, { name -> viewModel.updateForm { copy(name = name) } }, stringResource(Res.string.registry_name))
            SelectField(stringResource(Res.string.registry_holder), state.holders, form.holder, { it.name },
                { holder -> viewModel.updateForm { copy(holder = holder) } })
            SelectField(stringResource(Res.string.registry_institution), state.institutions, form.institution, { it.name },
                { institution -> viewModel.updateForm { copy(institution = institution) } })
            when (form.type) {
                PaymentMethodType.CREDIT_CARD -> {
                    NumberInput(form.closingDay, { day -> viewModel.updateForm { copy(closingDay = day) } }, stringResource(Res.string.registry_closing_day))
                    NumberInput(form.dueDay, { day -> viewModel.updateForm { copy(dueDay = day) } }, stringResource(Res.string.registry_due_day))
                }
                PaymentMethodType.FOREIGN_CURRENCY_WALLET ->
                    TextInput(form.currency, { currency -> viewModel.updateForm { copy(currency = currency.take(3)) } }, stringResource(Res.string.registry_currency))
                PaymentMethodType.INVESTMENT_ACCOUNT ->
                    AmountField(form.plannedContribution, { amount -> viewModel.updateForm { copy(plannedContribution = amount) } },
                        stringResource(Res.string.registry_planned_contribution))
                PaymentMethodType.CHECKING_ACCOUNT -> Unit
            }
            Button(onClick = viewModel::addPaymentMethod, enabled = form.isComplete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_add))
            }
        }
        Section(stringResource(Res.string.registry_people)) {
            state.people.forEach { person -> Text("${person.name} · ${person.role.label()}", style = MaterialTheme.typography.bodyMedium) }
            TextInput(state.personName, viewModel::setPersonName, stringResource(Res.string.registry_name))
            SelectField(stringResource(Res.string.registry_role), Role.entries, state.personRole, { it.label() }, viewModel::setPersonRole)
            Button(onClick = viewModel::addPerson, enabled = state.personName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_add))
            }
        }
        Section(stringResource(Res.string.registry_institutions)) {
            Text(state.institutions.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyMedium)
            TextInput(state.institutionName, viewModel::setInstitutionName, stringResource(Res.string.registry_name))
            Button(onClick = viewModel::addInstitution, enabled = state.institutionName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_add))
            }
        }
    }
    deleting?.let { method ->
        ConfirmDialog(
            title = stringResource(Res.string.registry_delete),
            text = stringResource(Res.string.registry_delete_confirm, method.name),
            confirmLabel = stringResource(Res.string.registry_delete),
            onConfirm = { deleting = null; viewModel.delete(method) },
            onDismiss = { deleting = null },
        )
    }
}
