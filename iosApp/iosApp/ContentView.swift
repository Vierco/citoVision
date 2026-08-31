import SwiftUI
import UIKit
import shared

/// Puente UIKit → Compose: expone el `UIViewController` que crea la UI Compose compartida
/// (`MainViewController()` en `shared`) como una vista SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Se ignoran TODAS las regiones seguras (`.container` + `.keyboard`), no solo el teclado.
            // SwiftUI aplica por defecto los insets del dispositivo a un UIViewControllerRepresentable,
            // así que Compose recibía un lienzo recortado y el fondo blanco de la ventana asomaba como dos
            // bandas: bajo la Dynamic Island y sobre el indicador de inicio.
            //
            // Con la pantalla completa, quien aplica los insets es Compose, igual que en Android con
            // `enableEdgeToEdge()`: el degradado de `MainScreen` cubre todo y son `TopAppBar` y
            // `NavigationBar` (Material 3) las que se separan solas de las barras del sistema.
            .ignoresSafeArea()
    }
}

