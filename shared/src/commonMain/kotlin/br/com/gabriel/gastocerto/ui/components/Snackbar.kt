package br.com.gabriel.gastocerto.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/** The app's single snackbar host (provided by `AppShell`), so every screen shows its events the same way. */
val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }
