package br.com.gabriel.gastocerto.ui.more

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.gabriel.gastocerto.ui.components.ScreenColumn
import br.com.gabriel.gastocerto.ui.navigation.Route
import br.com.gabriel.gastocerto.ui.navigation.moreItems
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.tab_more
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** "More" tab: the screens that are not used every day (registry, recurrences…). */
@Composable
fun MoreScreen(onOpen: (Route) -> Unit) {
    ScreenColumn(stringResource(Res.string.tab_more)) {
        moreItems.forEach { (route, title, subtitle) ->
            ListItem(
                headlineContent = { Text(stringResource(title)) },
                supportingContent = { Text(stringResource(subtitle)) },
                modifier = Modifier.clickable { onOpen(route) },
            )
            HorizontalDivider()
        }
    }
}

/** An entry of the "More" menu. */
data class MoreItem(val route: Route, val title: StringResource, val subtitle: StringResource)
