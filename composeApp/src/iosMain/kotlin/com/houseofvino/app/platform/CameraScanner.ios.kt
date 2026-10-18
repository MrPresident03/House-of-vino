package com.houseofvino.app.platform

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.houseofvino.core.scan.OcrLine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.QuartzCore.CATransaction
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIView
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * AVFoundation preview with on-device recognition: AVCaptureMetadataOutput decodes barcodes;
 * labels are captured as a photo and read with Vision's VNRecognizeTextRequest.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraScanner(
    mode: ScanMode,
    onResult: (ScanOutput) -> Unit,
    onPermissionDenied: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier,
) {
    var authorized by remember { mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized) }
    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> Unit
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { ok ->
                dispatch_async(dispatch_get_main_queue()) { if (ok) authorized = true else onPermissionDenied() }
            }
            else -> onPermissionDenied()
        }
    }
    if (!authorized) return
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    if (device == null) {
        LaunchedEffect(Unit) { onUnavailable() }
        return
    }
    var reading by remember { mutableStateOf(false) }
    val controller = remember(mode) { ScannerController(mode, device, onResult, onUnavailable) { reading = false } }
    DisposableEffect(controller) {
        controller.start()
        onDispose { controller.stop() }
    }
    Box(modifier) {
        UIKitView(factory = { controller.view }, modifier = Modifier.fillMaxSize())
        if (mode == ScanMode.LABEL) {
            Button(onClick = { reading = true; controller.capture() }, enabled = !reading,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) { Text(if (reading) "Reading…" else "Capture label") }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply { videoGravity = AVLayerVideoGravityResizeAspectFill }

    init { layer.addSublayer(previewLayer) }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.setFrame(bounds)
        CATransaction.commit()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ScannerController(
    private val mode: ScanMode,
    private val device: AVCaptureDevice,
    private val onResult: (ScanOutput) -> Unit,
    private val onUnavailable: () -> Unit,
    private val onReadFailed: () -> Unit,
) {
    private val session = AVCaptureSession()
    val view: UIView = CameraPreviewView(session)
    private var handled = false
    private var lastValue: String? = null
    private val photoOutput = AVCapturePhotoOutput()
    private val metadataDelegate = MetadataDelegate { value, type ->
        if (mode == ScanMode.QR) {
            // Invites: report each different code, so a wrong one doesn't stop the scanner.
            if (value != lastValue) { lastValue = value; onResult(ScanOutput.Barcode(value, symbology(type))) }
        } else if (!handled) {
            handled = true
            onResult(ScanOutput.Barcode(value, symbology(type)))
        }
    }
    private val photoDelegate = PhotoDelegate(::recognize) { onReadFailed() }

    fun start() {
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput
        if (input == null) { onUnavailable(); return }
        session.beginConfiguration()
        if (session.canAddInput(input)) session.addInput(input)
        if (mode == ScanMode.BARCODE || mode == ScanMode.QR) {
            val output = AVCaptureMetadataOutput()
            if (session.canAddOutput(output)) {
                session.addOutput(output)
                output.setMetadataObjectsDelegate(metadataDelegate, dispatch_get_main_queue())
                output.metadataObjectTypes = if (mode == ScanMode.QR) listOf(AVMetadataObjectTypeQRCode) else listOf(
                    AVMetadataObjectTypeEAN13Code, AVMetadataObjectTypeEAN8Code, AVMetadataObjectTypeUPCECode, AVMetadataObjectTypeCode128Code,
                )
            }
        } else if (session.canAddOutput(photoOutput)) {
            session.addOutput(photoOutput)
        }
        session.commitConfiguration()
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.startRunning() }
    }

    fun stop() {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.stopRunning() }
    }

    fun capture() {
        photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), photoDelegate)
    }

    private fun recognize(data: NSData) {
        val image = UIImage(data = data)
        val cg = image.CGImage ?: run { onReadFailed(); return }
        val request = VNRecognizeTextRequest { req, _ ->
            val observations = req?.results?.filterIsInstance<VNRecognizedTextObservation>().orEmpty()
            val lines = observations.mapNotNull { obs ->
                val top = obs.topCandidates(1uL).firstOrNull() as? VNRecognizedText ?: return@mapNotNull null
                val height = obs.boundingBox.useContents { size.height }
                OcrLine(top.string, (top.confidence * 100).toInt(), (height * 1000).toInt())
            }
            // Camera JPEGs are stored sideways with an EXIF flag; save them upright.
            val bytes = resizedJpeg(data) ?: data.toByteArray()
            dispatch_async(dispatch_get_main_queue()) { onResult(ScanOutput.LabelText(lines, bytes)) }
        }
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.usesLanguageCorrection = false // producer names are not dictionary words
        val orientation = when (image.imageOrientation) {
            UIImageOrientation.UIImageOrientationRight -> kCGImagePropertyOrientationRight
            UIImageOrientation.UIImageOrientationLeft -> kCGImagePropertyOrientationLeft
            UIImageOrientation.UIImageOrientationDown -> kCGImagePropertyOrientationDown
            else -> kCGImagePropertyOrientationUp
        }
        val handler = VNImageRequestHandler(cGImage = cg, orientation = orientation, options = emptyMap<Any?, Any?>())
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            handler.performRequests(listOf(request), null)
        }
    }

    private fun symbology(type: String?) = when (type) {
        AVMetadataObjectTypeEAN13Code -> "EAN_13"; AVMetadataObjectTypeEAN8Code -> "EAN_8"
        AVMetadataObjectTypeUPCECode -> "UPC_E"; AVMetadataObjectTypeCode128Code -> "CODE_128"; AVMetadataObjectTypeQRCode -> "QR"; else -> "OTHER"
    }
}

private class MetadataDelegate(private val onCode: (String, String?) -> Unit) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
        val obj = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject ?: return
        val value = obj.stringValue ?: return
        onCode(value, obj.type)
    }
}

private class PhotoDelegate(private val onData: (NSData) -> Unit, private val onFailed: () -> Unit) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
        val data = didFinishProcessingPhoto.fileDataRepresentation()
        if (error != null || data == null) onFailed() else onData(data)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
