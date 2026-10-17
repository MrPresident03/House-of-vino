package com.houseofvino.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.houseofvino.app.App

class MainActivity : ComponentActivity() {
    private val hvApp: HouseOfVinoApp get() = application as HouseOfVinoApp

    // Launchers must be registered before the activity is STARTED, so they live here and the
    // platform layer calls them.
    internal val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> hvApp.platform.onDocumentPicked(uri) }
    internal val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> hvApp.platform.onPhotoPicked(uri) }
    internal val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* the worker checks at delivery time */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hvApp.platform.attach(this)
        setContent { App(hvApp.graph) }
    }
}
