package dev.lovelace.citovision

import androidx.compose.runtime.Composable
import dev.lovelace.citovision.composition.di.presentationModule
import dev.lovelace.citovision.presentation.navigation.AppNavHost
import dev.lovelace.citovision.ui.theme.citoVisionTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(presentationModule)
    }) {
        citoVisionTheme {
            AppNavHost()
        }
    }
}
