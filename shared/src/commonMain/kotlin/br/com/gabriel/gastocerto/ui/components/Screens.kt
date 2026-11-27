package br.com.gabriel.gastocerto.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.action_back
import gastocerto.shared.generated.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Standard screen body: title (with a back arrow when [onBack] is given) and a scrolling column. */
@Composable
fun ScreenColumn(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(painterResource(Res.drawable.ic_back), stringResource(Res.string.action_back)) }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        content()
    }
}
