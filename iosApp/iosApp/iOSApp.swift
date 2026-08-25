import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // Arranque único de Koin + Napier en el lado Kotlin (equivalente a Application/Main).
        MainViewControllerKt.bootstrap()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
