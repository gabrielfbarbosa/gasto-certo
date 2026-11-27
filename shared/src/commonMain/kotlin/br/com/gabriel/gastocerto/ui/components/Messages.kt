package br.com.gabriel.gastocerto.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import br.com.gabriel.gastocerto.domain.rule.RuleViolationException
import br.com.gabriel.gastocerto.ui.userMessage

/**
 * The user text of a rule violation (string resources, en/pt-BR). ViewModels keep the exception itself
 * in the UiState — the data, not the wording — so they are tested without loading resources.
 */
@Composable
fun ruleMessage(error: RuleViolationException): String {
    val text by produceState("", error) { value = error.userMessage() }
    return text
}

/** Validation error under a form, in the error color. */
@Composable
fun ErrorText(error: RuleViolationException?, modifier: Modifier = Modifier) {
    if (error != null) {
        Text(ruleMessage(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}
