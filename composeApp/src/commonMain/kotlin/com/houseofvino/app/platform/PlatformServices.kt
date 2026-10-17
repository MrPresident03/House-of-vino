package com.houseofvino.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.houseofvino.core.money.MoneyFormatStyle
import com.houseofvino.core.scan.OcrLine
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.remote.BackendConfig

/** A text file the user picked (CSV import). */
class PickedFile(val name: String, val text: String)

/** What the camera produced. Barcodes and OCR lines are raw; matching happens in `core`. */
sealed interface ScanOutput {
    data class Barcode(val value: String, val symbology: String) : ScanOutput
    data class LabelText(val lines: List<OcrLine>, val photo: ByteArray?) : ScanOutput
}

enum class ScanMode { BARCODE, LABEL }

/**
 * Everything the shared UI needs from the platform. Implemented in androidMain (Android APIs)
 * and iosMain (UIKit/Foundation), and by a fake in tests.
 */
interface PlatformServices {
    val platformName: String
    val backend: BackendConfig?
    val appVersion: String

    fun today(): IsoDate
    fun currentYear(): Int = today().year
    fun utcOffsetMinutes(): Int
    fun moneyFormat(): MoneyFormatStyle
    fun prefersReducedMotion(): Boolean

    /** Writes [content] to a file and opens the system share sheet. Returns a user-facing error, or null. */
    suspend fun shareText(fileName: String, mimeType: String, content: String): String?

    /** Opens the system file picker for CSV/text files. [onPicked] gets null when cancelled. */
    fun pickTextFile(onPicked: (PickedFile?) -> Unit)

    /** Lets the user choose a photo from the library; JPEG bytes or null. */
    fun pickPhoto(onPicked: (ByteArray?) -> Unit)

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

    /** Asks the OS to run a background sync soon (WorkManager / BGTaskScheduler). */
    fun requestBackgroundSync()

    /** Registers a callback invoked when the network becomes available. */
    fun onNetworkAvailable(callback: () -> Unit)
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
