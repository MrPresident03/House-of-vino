package com.houseofvino.app

import androidx.compose.ui.window.ComposeUIViewController
import com.houseofvino.app.platform.IosPlatform
import com.houseofvino.data.catalogue.openNativeCatalogue
import com.houseofvino.data.db.createNativeDriver
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/** One graph per process. */
object IosApp {
    val graph: AppGraph by lazy { AppGraph(IosPlatform(), createNativeDriver(), catalogue = {
        openNativeCatalogue(NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")?.toString() ?: "0")
    }) }
}

/** Entry point used by the SwiftUI host (iosApp/iosApp/ContentView.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController { App(IosApp.graph) }
