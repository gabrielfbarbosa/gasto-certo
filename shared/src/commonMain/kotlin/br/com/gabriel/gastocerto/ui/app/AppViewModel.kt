package br.com.gabriel.gastocerto.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gabriel.gastocerto.ui.demoLabels
import br.com.gabriel.gastocerto.usecase.DemoLabels
import br.com.gabriel.gastocerto.usecase.SeedDemoDataUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the app shell draws: a loading indicator until the database is ready. */
data class AppUiState(val ready: Boolean = false)

/** One-off effects for the app shell. */
sealed interface AppEvent {
    /** The demo data was loaded on this run (first run) — shown once in a snackbar. */
    data object DemoDataLoaded : AppEvent
}

/**
 * Root ViewModel and **reference for the MVVM + UDF pattern** used by every screen:
 * - [uiState]: one immutable [AppUiState] exposed as `StateFlow` — everything the screen draws;
 * - [events]: one-off [AppEvent]s (snackbar, navigation) through a `Channel`, never kept as state;
 * - actions are public methods; use cases run in `viewModelScope`;
 * - the state is updated **before** the event is sent, so whoever reacts to the event sees the new state.
 *
 * On start it runs the demo seed (8.18), which does nothing when the database already has data.
 */
class AppViewModel(
    private val seed: SeedDemoDataUseCase,
    private val labels: suspend () -> DemoLabels = ::demoLabels,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val seeded = seed.execute(labels())
            _uiState.update { it.copy(ready = true) }
            if (seeded) _events.send(AppEvent.DemoDataLoaded)
        }
    }
}
