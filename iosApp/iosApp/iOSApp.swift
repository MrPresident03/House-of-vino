import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // BGTaskScheduler handlers must be registered before launch finishes.
        MainViewControllerKt.registerBackgroundTasks()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { phase in
            if phase == .background {
                MainViewControllerKt.onEnterBackground()
            }
        }
    }
}
