package com.houseofvino.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.houseofvino.core.money.MoneyFormatStyle
import com.houseofvino.core.scan.OcrLine
import com.houseofvino.core.time.IsoDate

/** A text file the user picked (CSV import). */
class PickedFile(val name: String, val text: String)

/** What the camera produced. Barcodes and OCR lines are raw; matching happens in `core`. */
sealed interface ScanOutput {
    data class Barcode(val value: String, val symbology: String) : ScanOutput
    data class LabelText(val lines: List<OcrLine>, val photo: ByteArray?) : ScanOutput
}

enum class ScanMode { BARCODE, LABEL, QR }

/** What kind of file the user is picking. */
enum class PickKind { CSV, SYNC }

/**
 * Everything the shared UI needs from the platform. Implemented in androidMain (Android APIs)
 * and iosMain (UIKit/Foundation), and by a fake in tests.
 */
interface PlatformServices {
    val platformName: String
    val appVersion: String

    fun today(): IsoDate
    fun currentYear(): Int = today().year
    fun utcOffsetMinutes(): Int
    fun moneyFormat(): MoneyFormatStyle
    fun prefersReducedMotion(): Boolean

    /** Writes [content] to a file and opens the system share sheet. Returns a user-facing error, or null. */
    suspend fun shareText(fileName: String, mimeType: String, content: String): String?

    /** Opens the system file picker for a text file ([PickKind.CSV] or a sync file). [onPicked] gets null when cancelled. */
    fun pickTextFile(kind: PickKind = PickKind.CSV, onPicked: (PickedFile?) -> Unit)

    /** Lets the user choose a photo from the library; JPEG bytes or null. */
    fun pickPhoto(onPicked: (ByteArray?) -> Unit)

    /** Takes a photo with the camera, upright; JPEG bytes or null if cancelled or unavailable. */
    fun takePhoto(onTaken: (ByteArray?) -> Unit) = onTaken(null)
    fun cameraAvailable(): Boolean = false

    fun openUrl(url: String)
    fun openAppSettings()

    /** App-private photo storage. Returns the stored path. */
    fun savePhoto(id: String, jpeg: ByteArray): String
    fun readPhoto(path: String): ByteArray?
    fun deletePhoto(path: String)

    /** Whether a camera is present and the platform scanner is available on this device. */
    fun scanningAvailable(): Boolean

    /** Schedules (or clears, when [enabled] is false) drinking-window reminders. */
    fun setWindowReminders(enabled: Boolean, dueCount: Int)
}

/**
 * Camera preview with live barcode detection or label capture. Each platform provides its own
 * implementation (CameraX + ML Kit on Android, AVFoundation + Vision on iOS).
 */
@Composable
expect fun CameraScanner(
    mode: ScanMode,
    onResult: (ScanOutput) -> Unit,
    onPermissionDenied: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier,
)
