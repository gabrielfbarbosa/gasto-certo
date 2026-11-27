package br.com.gabriel.gastocerto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import br.com.gabriel.gastocerto.di.appModule
import br.com.gabriel.gastocerto.di.platformModule
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import br.com.gabriel.gastocerto.ui.demoLabels
import br.com.gabriel.gastocerto.usecase.SeedDemoDataUseCase
import org.koin.dsl.koinConfiguration

import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    // Koin starts with the composition (Android Context injected automatically) and stops with it.
    KoinApplication(configuration = koinConfiguration { modules(appModule, platformModule) }) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    // 8.18: demo data on the first run (idempotent). Moves to the root ViewModel with the UI (8.19).
    val seed = koinInject<SeedDemoDataUseCase>()
    LaunchedEffect(Unit) { seed.execute(demoLabels()) }
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}