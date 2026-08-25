package dev.lovelace.citovision

import androidx.compose.ui.window.ComposeUIViewController
import dev.lovelace.citovision.composition.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import platform.UIKit.UIViewController

private var initialized = false

/**
 * Arranque único del proceso iOS: logging (Napier) + Koin. Lo invoca `iOSApp.init` (Swift) una sola
 * vez al lanzar la app. El guard evita reiniciar Koin si el runtime recreara el entry point, porque
 * `startKoin` lanza si ya hay una instancia. Replica el patrón de `Main.kt` (Desktop) y de
 * `CitoVisionApplication`/`MainActivity` (Android).
 *
 * Se llama `bootstrap` (no `initialize`) para no chocar con el `+initialize` de Objective-C al
 * exportarse a Swift.
 */
fun bootstrap() {
    if (initialized) return
    initialized = true
    Napier.base(DebugAntilog())
    initKoin()
}

/**
 * Punto de entrada de la UI para Xcode: envuelve la UI Compose compartida [App] en un
 * `ComposeUIViewController`. Lo consume `ContentView.swift` mediante `UIViewControllerRepresentable`.
 *
 * El nombre va en PascalCase por la convención de entry points iOS de Compose Multiplatform (así lo
 * generan los templates KMP); no es `@Composable`, de ahí el suppress puntual de la regla de ktlint.
 */
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
