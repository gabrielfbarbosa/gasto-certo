package br.com.gabriel.gastocerto.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.Money
import br.com.gabriel.gastocerto.domain.model.Statement
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.theme.amountColor
import br.com.gabriel.gastocerto.ui.toBrl
import br.com.gabriel.gastocerto.ui.toDisplay
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.ic_chevron_left
import gastocerto.shared.generated.resources.ic_chevron_right
import gastocerto.shared.generated.resources.month_next
import gastocerto.shared.generated.resources.month_previous
import gastocerto.shared.generated.resources.summary_all_holders
import gastocerto.shared.generated.resources.summary_by_holder
import gastocerto.shared.generated.resources.summary_by_payment_method
import gastocerto.shared.generated.resources.summary_expenses
import gastocerto.shared.generated.resources.summary_income
import gastocerto.shared.generated.resources.summary_month
import gastocerto.shared.generated.resources.summary_receivables
import gastocerto.shared.generated.resources.summary_statement_due
import gastocerto.shared.generated.resources.summary_statements
import gastocerto.shared.generated.resources.summary_surplus
import gastocerto.shared.generated.resources.summary_third_party
import gastocerto.shared.generated.resources.summary_title
import gastocerto.shared.generated.resources.summary_week
import gastocerto.shared.generated.resources.summary_year
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** UC04 + UC05 – totals of the month, week or year, receivables and card statements. */
@Composable
fun SummaryScreen(viewModel: SummaryViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    ScreenColumn(stringResource(Res.string.summary_title)) {
        val kinds = listOf(
            Res.string.summary_month to Period::monthOf,
            Res.string.summary_week to Period::weekOf,
            Res.string.summary_year to Period::yearOf,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            kinds.forEachIndexed { index, (label, kind) ->
                val selected = when (state.period) {
                    is Period.Month -> index == 0
                    is Period.Week -> index == 1
                    is Period.Year -> index == 2
                }
                SegmentedButton(
                    selected = selected,
                    onClick = { viewModel.selectPeriodKind(kind) },
                    shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                ) { Text(stringResource(label)) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { viewModel.selectPeriod(state.period.previous()) }) {
                Icon(painterResource(Res.drawable.ic_chevron_left), stringResource(Res.string.month_previous))
            }
            Text(state.period.label(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { viewModel.selectPeriod(state.period.next()) }) {
                Icon(painterResource(Res.drawable.ic_chevron_right), stringResource(Res.string.month_next))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.holder == null, onClick = { viewModel.filterHolder(null) },
                label = { Text(stringResource(Res.string.summary_all_holders)) })
            state.holders.forEach { person ->
                FilterChip(selected = state.holder == person, onClick = { viewModel.filterHolder(person) },
                    label = { Text(person.name) })
            }
        }
        state.summary?.let { summary ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.summary_surplus), style = MaterialTheme.typography.labelLarge)
                    val surplus = summary.surplus()
                    Text(surplus.toBrl(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = amountColor(!(surplus.cents < 0)))
                    AmountRow(stringResource(Res.string.summary_income), summary.income)
                    AmountRow(stringResource(Res.string.summary_expenses), summary.totalExpenses)
                    AmountRow(stringResource(Res.string.summary_third_party), summary.thirdPartyExpenses)
                }
            }
            if (summary.byPaymentMethod.isNotEmpty()) {
                Section(stringResource(Res.string.summary_by_payment_method)) {
                    summary.byPaymentMethod.entries.sortedByDescending { it.value.cents }
                        .forEach { (method, amount) -> AmountRow(method.name, amount) }
                }
            }
            if (summary.byHolder.size > 1) {
                Section(stringResource(Res.string.summary_by_holder)) {
                    summary.byHolder.entries.sortedByDescending { it.value.cents }
                        .forEach { (person, amount) -> AmountRow(person.name, amount) }
                }
            }
        }
        if (state.receivables.isNotEmpty()) {
            Section(stringResource(Res.string.summary_receivables)) {
                state.receivables.forEach { AmountRow(it.thirdParty.name, it.amount) }
            }
        }
        if (state.statements.isNotEmpty()) {
            Section(stringResource(Res.string.summary_statements)) {
                state.statements.forEach { StatementItem(it) }
            }
        }
    }
}

@Composable
private fun Period.label(): String = when (this) {
    is Period.Month -> month.toDisplay()
    is Period.Week -> "${monday.toDisplay()} – ${sunday.toDisplay()}"
    is Period.Year -> year.toString()
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Money, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(amount.toBrl(), style = MaterialTheme.typography.bodyMedium)
    }
}

/** A statement line; tapping it shows its expenses (UC05, installments labeled `k/N`). */
@Composable
private fun StatementItem(statement: Statement) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp)) {
        AmountRow(statement.card.name, statement.total())
        Text(
            stringResource(Res.string.summary_statement_due, statement.closingDate.toDisplay(), statement.dueDate.toDisplay()),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            statement.expenses.forEach { expense ->
                val label = (expense as? Installment)
                    ?.let { "${expense.description} ${it.label()}" } ?: expense.description
                AmountRow(label, expense.amount, Modifier.padding(start = 12.dp))
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}
