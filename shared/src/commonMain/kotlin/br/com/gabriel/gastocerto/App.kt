package br.com.gabriel.gastocerto

import androidx.compose.runtime.Composable
import br.com.gabriel.gastocerto.di.appModule
import br.com.gabriel.gastocerto.di.platformModule
import br.com.gabriel.gastocerto.ui.app.AppShell
import br.com.gabriel.gastocerto.ui.theme.GastoCertoTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

/** Entry point shared by Android, iOS and Web. */
@Composable
fun App() {
    // Koin starts with the composition (Android Context injected automatically) and stops with it.
    KoinApplication(configuration = koinConfiguration { modules(appModule, platformModule) }) {
        GastoCertoTheme { AppShell() }
    }
}
