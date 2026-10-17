package com.houseofvino.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/** The desktop JVM target exists only for UI tests; it has no camera. */
@Composable
actual fun CameraScanner(
    mode: ScanMode,
    onResult: (ScanOutput) -> Unit,
    onPermissionDenied: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) { onUnavailable() }
}
