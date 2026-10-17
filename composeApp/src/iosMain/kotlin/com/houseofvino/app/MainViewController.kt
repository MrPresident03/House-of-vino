package com.houseofvino.app

import androidx.compose.ui.window.ComposeUIViewController
import com.houseofvino.app.platform.IosPlatform
import com.houseofvino.data.db.createNativeDriver
import platform.UIKit.UIViewController

/** One graph per process. */
object IosApp {
    val graph: AppGraph by lazy { AppGraph(IosPlatform(), createNativeDriver()) }
}

/** Entry point used by the SwiftUI host (iosApp/iosApp/ContentView.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController { App(IosApp.graph) }
