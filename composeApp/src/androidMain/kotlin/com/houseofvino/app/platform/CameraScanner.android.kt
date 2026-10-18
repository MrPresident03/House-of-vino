package com.houseofvino.app.platform

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.houseofvino.core.scan.OcrLine
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX preview with ML Kit. Everything runs on device: barcodes are decoded live from the
 * analysis stream; labels are captured as a still and read with on-device text recognition.
 */
@Composable
actual fun CameraScanner(
    mode: ScanMode,
    onResult: (ScanOutput) -> Unit,
    onPermissionDenied: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
        LaunchedEffect(Unit) { onUnavailable() }
        return
    }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) granted = true else onPermissionDenied()
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    if (!granted) return

    val previewView = remember { PreviewView(context) }
    val handled = remember { AtomicBoolean(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var reading by remember { mutableStateOf(false) }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(mode) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .apply {
                    if (mode == ScanMode.QR) setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    else setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128)
                }
                .build(),
        )
        providerFuture.addListener({
            val provider = try { providerFuture.get() } catch (e: Exception) { onUnavailable(); return@addListener }
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            try {
                provider.unbindAll()
                if (mode == ScanMode.BARCODE || mode == ScanMode.QR) {
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    val lastValue = java.util.concurrent.atomic.AtomicReference<String?>(null)
                    analysis.setAnalyzer(analysisExecutor) { proxy -> analyse(proxy, handled, scanner, mode == ScanMode.QR, lastValue, onResult) }
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } else {
                    val ic = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                    capture = ic
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, ic)
                }
            } catch (e: Exception) {
                onUnavailable()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            analysisExecutor.shutdown()
            scanner.close()
        }
    }
    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    Box(modifier) {
        AndroidView({ previewView }, Modifier.fillMaxSize())
        if (mode == ScanMode.LABEL) {
            Button(
                onClick = {
                    val ic = capture ?: return@Button
                    reading = true
                    ic.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toBitmap()
                            val rotation = image.imageInfo.rotationDegrees
                            image.close()
                            val input = InputImage.fromBitmap(bitmap, rotation)
                            recognizer.process(input)
                                .addOnSuccessListener { text ->
                                    val height = if (rotation % 180 == 0) bitmap.height else bitmap.width
                                    val lines = text.textBlocks.flatMap { it.lines }.map { line ->
                                        OcrLine(
                                            text = line.text,
                                            confidence = (line.confidence * 100).toInt().takeIf { it > 0 },
                                            heightPermille = line.boundingBox?.let { b -> (b.height() * 1000 / height.coerceAtLeast(1)) },
                                        )
                                    }
                                    onResult(ScanOutput.LabelText(lines, bitmap.uprighted(rotation).toJpeg()))
                                }
                                .addOnFailureListener { reading = false }
                        }

                        override fun onError(exception: ImageCaptureException) { reading = false }
                    })
                },
                enabled = !reading,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            ) { Text(if (reading) "Reading…" else "Capture label") }
        }
    }
}

/**
 * Barcodes are reported once. QR codes (invites) are reported each time a *different* code is
 * seen, so a wrong code doesn't stop the scanner.
 */
private fun analyse(
    proxy: ImageProxy, handled: AtomicBoolean, scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    repeatDistinct: Boolean, last: java.util.concurrent.atomic.AtomicReference<String?>, onResult: (ScanOutput) -> Unit,
) {
    val media = proxy.image
    if (media == null || (!repeatDistinct && handled.get())) { proxy.close(); return }
    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { codes ->
            val code = codes.firstOrNull { !it.rawValue.isNullOrBlank() } ?: return@addOnSuccessListener
            val value = code.rawValue!!
            if (repeatDistinct) {
                if (last.getAndSet(value) != value) onResult(ScanOutput.Barcode(value, symbology(code.format)))
            } else if (handled.compareAndSet(false, true)) {
                onResult(ScanOutput.Barcode(value, symbology(code.format)))
            }
        }
        .addOnCompleteListener { proxy.close() }
}

private fun symbology(format: Int) = when (format) {
    Barcode.FORMAT_EAN_13 -> "EAN_13"; Barcode.FORMAT_EAN_8 -> "EAN_8"; Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"; Barcode.FORMAT_CODE_128 -> "CODE_128"; Barcode.FORMAT_QR_CODE -> "QR"; else -> "OTHER"
}

/** The camera sensor's frame turned the way the phone was held (the saved photo has no EXIF). */
fun Bitmap.uprighted(rotationDegrees: Int): Bitmap {
    if (rotationDegrees % 360 == 0) return this
    val m = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}

/** Downscales to at most 2048 px on the long edge and encodes as JPEG (q 85). */
fun Bitmap.toJpeg(maxEdge: Int = 2048): ByteArray {
    val scale = maxEdge.toFloat() / maxOf(width, height)
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true) else this
    return ByteArrayOutputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
}
