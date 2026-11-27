package br.com.gabriel.gastocerto.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_cancel
import gastocerto.shared.generated.resources.action_ok
import org.jetbrains.compose.resources.stringResource

/** Asks for an amount (e.g. the real amount of an expected entry), starting at [initial] cents. */
@Composable
fun AmountDialog(title: String, label: String, initial: Long?, onConfirm: (Long?) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AmountField(amount, { amount = it }, label) },
        confirmButton = { TextButton(onClick = { onConfirm(amount) }) { Text(stringResource(Res.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/** Yes/no confirmation before an action that changes a lot (closing a month, deleting). */
@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
