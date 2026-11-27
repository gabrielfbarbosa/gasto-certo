package br.com.gabriel.gastocerto.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import br.com.gabriel.gastocerto.domain.model.MonthYear
import br.com.gabriel.gastocerto.ui.digitsToValue
import br.com.gabriel.gastocerto.ui.formatFixed
import br.com.gabriel.gastocerto.ui.toDisplay
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_cancel
import gastocerto.shared.generated.resources.action_ok
import gastocerto.shared.generated.resources.ic_chevron_left
import gastocerto.shared.generated.resources.ic_chevron_right
import gastocerto.shared.generated.resources.month_next
import gastocerto.shared.generated.resources.month_previous
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Field for an amount with [decimals] implied decimal places: the user only types digits
 * (`1`, `2`, `3` → `1,23`), so there is no decimal separator to get wrong. Money uses 2 decimals
 * (cents, RNF04); the exchange rate uses 4 (D1).
 */
@Composable
fun AmountField(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    prefix: String = "R$",
    decimals: Int = 2,
    isError: Boolean = false,
) {
    val text = value?.let { formatFixed(it, decimals) } ?: ""
    OutlinedTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { onValueChange(digitsToValue(it.text)) },
        label = { Text(label) },
        prefix = { Text("$prefix ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Plain text field (description, name). */
@Composable
fun TextInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        singleLine = true, modifier = modifier.fillMaxWidth(),
    )
}

/** Whole number field (day, number of installments). */
@Composable
fun NumberInput(value: Int?, onValueChange: (Int?) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { text -> onValueChange(text.filter(Char::isDigit).take(3).toIntOrNull()) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

private const val MILLIS_PER_DAY = 86_400_000L

/** Read-only date field that opens the Material date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: LocalDate, onDateChange: (LocalDate) -> Unit, label: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) open = true }
    }
    OutlinedTextField(
        value = date.toDisplay(), onValueChange = {}, readOnly = true, label = { Text(label) },
        interactionSource = interaction, singleLine = true, modifier = modifier.fillMaxWidth(),
    )
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(LocalDate.fromEpochDays(it / MILLIS_PER_DAY)) }
                    open = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(Res.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** Dropdown to pick one of [options] (payment method, person…); [optionLabel] is how each one is shown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let { optionLabel(it) } ?: placeholder,
            onValueChange = {}, readOnly = true, label = { Text(label) }, singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

/** `‹ 11/2026 ›` — moves between accounting months. */
@Composable
fun MonthSelector(month: MonthYear, onChange: (MonthYear) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.previous()) }) {
            Icon(painterResource(Res.drawable.ic_chevron_left), stringResource(Res.string.month_previous))
        }
        Text(month.toDisplay(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onChange(month.next()) }) {
            Icon(painterResource(Res.drawable.ic_chevron_right), stringResource(Res.string.month_next))
        }
    }
}
