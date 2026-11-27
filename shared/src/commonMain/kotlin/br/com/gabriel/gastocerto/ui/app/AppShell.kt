package br.com.gabriel.gastocerto.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import br.com.gabriel.gastocerto.ui.components.PlaceholderScreen
import br.com.gabriel.gastocerto.ui.navigation.Route
import br.com.gabriel.gastocerto.ui.navigation.tabs
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.demo_data_loaded
import gastocerto.shared.generated.resources.ic_add
import gastocerto.shared.generated.resources.ic_lock
import gastocerto.shared.generated.resources.ic_menu
import gastocerto.shared.generated.resources.ic_summary
import gastocerto.shared.generated.resources.tab_close_month
import gastocerto.shared.generated.resources.tab_more
import gastocerto.shared.generated.resources.tab_register
import gastocerto.shared.generated.resources.tab_summary
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Serializers of the routes for the back stack (needed on iOS and Web, which have no reflection). */
private val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { subclassesOfSealed<Route>() }
    }
}

/** App shell: bottom bar with the 4 tabs, the navigation display and the snackbar for one-off events. */
@Composable
fun AppShell(viewModel: AppViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AppEvent.DemoDataLoaded -> snackbar.showSnackbar(getString(Res.string.demo_data_loaded))
            }
        }
    }
    val backStack = rememberNavBackStack(navigationConfig, Route.Summary)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BottomBar(backStack) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (!state.ready) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<Route.Summary> { PlaceholderScreen(stringResource(Res.string.tab_summary)) }
                        entry<Route.RegisterExpense> { PlaceholderScreen(stringResource(Res.string.tab_register)) }
                        entry<Route.CloseMonth> { PlaceholderScreen(stringResource(Res.string.tab_close_month)) }
                        entry<Route.More> { PlaceholderScreen(stringResource(Res.string.tab_more)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(backStack: NavBackStack<NavKey>) {
    val current = backStack.firstOrNull()
    NavigationBar {
        tabs.forEach { tab ->
            val (icon, label) = tab.tabInfo()
            NavigationBarItem(
                selected = current == tab,
                onClick = {
                    // Top-level tab: the back stack restarts at the tab.
                    if (backStack.lastOrNull() != tab) {
                        backStack.clear()
                        backStack.add(tab)
                    }
                },
                icon = { Icon(painterResource(icon), null) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

private fun Route.tabInfo(): Pair<DrawableResource, StringResource> = when (this) {
    Route.Summary -> Res.drawable.ic_summary to Res.string.tab_summary
    Route.RegisterExpense -> Res.drawable.ic_add to Res.string.tab_register
    Route.CloseMonth -> Res.drawable.ic_lock to Res.string.tab_close_month
    else -> Res.drawable.ic_menu to Res.string.tab_more
}
