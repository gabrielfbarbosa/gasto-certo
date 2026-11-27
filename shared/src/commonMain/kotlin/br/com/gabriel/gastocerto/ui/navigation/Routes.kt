package br.com.gabriel.gastocerto.ui.navigation

import androidx.navigation3.runtime.NavKey
import br.com.gabriel.gastocerto.ui.more.MoreItem
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.more_recurrences
import gastocerto.shared.generated.resources.more_recurrences_detail
import gastocerto.shared.generated.resources.more_registry
import gastocerto.shared.generated.resources.more_registry_detail
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

    /** Screens opened from "More". */
    @Serializable data object Registry : Route
    @Serializable data object Recurrences : Route
}

/** The tabs of the bottom bar, in order. */
val tabs: List<Route> = listOf(Route.Summary, Route.RegisterExpense, Route.CloseMonth, Route.More)

/** The screens listed in the "More" tab, in order. */
val moreItems: List<MoreItem> = listOf(
    MoreItem(Route.Registry, Res.string.more_registry, Res.string.more_registry_detail),
    MoreItem(Route.Recurrences, Res.string.more_recurrences, Res.string.more_recurrences_detail),
)
