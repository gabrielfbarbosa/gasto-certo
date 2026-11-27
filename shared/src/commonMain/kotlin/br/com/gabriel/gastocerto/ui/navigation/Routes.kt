package br.com.gabriel.gastocerto.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destinations of the app (Navigation 3). Serializable so the back stack survives process death
 * on every platform (iOS and Web have no reflection: see `navigationConfig`).
 */
@Serializable
sealed interface Route : NavKey {
    /** Top-level tabs. */
    @Serializable data object Summary : Route
    @Serializable data object RegisterExpense : Route
    @Serializable data object CloseMonth : Route
    @Serializable data object More : Route
}

/** The tabs of the bottom bar, in order. */
val tabs: List<Route> = listOf(Route.Summary, Route.RegisterExpense, Route.CloseMonth, Route.More)
