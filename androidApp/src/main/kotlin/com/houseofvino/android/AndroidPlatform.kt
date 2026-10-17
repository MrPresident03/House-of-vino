package com.houseofvino.android

import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.houseofvino.app.platform.PickedFile
import com.houseofvino.app.platform.PlatformServices
import com.houseofvino.core.money.MoneyFormatStyle
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.remote.BackendConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone

/**
 * Android implementation of the shared platform contract. Activity-bound features (pickers,
 * share sheet, permission prompts) go through the currently attached [MainActivity].
 */
class AndroidPlatform(private val app: Application) : PlatformServices {
    private var activityRef: WeakReference<MainActivity>? = null
    private val activity: MainActivity? get() = activityRef?.get()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var pendingText: ((PickedFile?) -> Unit)? = null
    private var pendingPhoto: ((ByteArray?) -> Unit)? = null

    fun attach(activity: MainActivity) {
        activityRef = WeakReference(activity)
    }

    override val platformName = "Android"

    override val backend: BackendConfig? =
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) null
        else BackendConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)

    override val appVersion: String = BuildConfig.VERSION_NAME

    override fun today(): IsoDate = LocalDate.now().let { IsoDate.of(it.year, it.monthValue, it.dayOfMonth) }

    override fun utcOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000

    override fun moneyFormat(): MoneyFormatStyle {
        val locale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val sample = NumberFormat.getCurrencyInstance(locale).format(1)
        return MoneyFormatStyle(
            decimalSeparator = symbols.monetaryDecimalSeparator,
            groupSeparator = symbols.groupingSeparator,
            symbolBefore = sample.firstOrNull()?.let { !it.isDigit() && it != '-' } ?: true,
            spaceBetween = sample.any { it == ' ' || it == ' ' || it == ' ' },
        )
    }

    override fun prefersReducedMotion(): Boolean =
        Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    // ---- Files -------------------------------------------------------------------------------

    override suspend fun shareText(fileName: String, mimeType: String, content: String): String? {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                File(app.cacheDir, "exports").apply { mkdirs() }.resolve(fileName).apply { writeText(content, Charsets.UTF_8) }
            }.getOrNull()
        } ?: return "Couldn't write the export file."
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.exports", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, fileName)
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, fileName)
        return try {
            val host = activity
            if (host != null) host.startActivity(chooser) else app.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        } catch (e: ActivityNotFoundException) {
            "No app on this device can receive the file."
        }
    }

    override fun pickTextFile(onPicked: (PickedFile?) -> Unit) {
        val host = activity ?: return onPicked(null)
        pendingText = onPicked
        host.openDocument.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel"))
    }

    internal fun onDocumentPicked(uri: Uri?) {
        val callback = pendingText ?: return
        pendingText = null
        if (uri == null) return callback(null)
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val size = app.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                        if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
                    }
                    require(size == null || size <= MAX_IMPORT_BYTES) { "too large" }
                    val name = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    } ?: "import.csv"
                    val text = app.contentResolver.openInputStream(uri)!!.use { it.readBytes() }.toString(Charsets.UTF_8)
                    PickedFile(name, text)
                }.getOrNull()
            }
            callback(picked)
        }
    }

    override fun pickPhoto(onPicked: (ByteArray?) -> Unit) {
        val host = activity ?: return onPicked(null)
        pendingPhoto = onPicked
        host.pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    internal fun onPhotoPicked(uri: Uri?) {
        val callback = pendingPhoto ?: return
        pendingPhoto = null
        if (uri == null) return callback(null)
        scope.launch {
            callback(withContext(Dispatchers.IO) { runCatching { decodeResized(uri) }.getOrNull() })
        }
    }

    /** Decodes at most [MAX_EDGE] px on the long edge (EXIF orientation applied) and re-encodes as JPEG. */
    private fun decodeResized(uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(app.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = minOf(1.0, MAX_EDGE.toDouble() / maxOf(w, h))
            if (scale < 1.0) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            activity?.startActivity(intent) ?: app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No browser installed; nothing sensible to do.
        }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
        activity?.startActivity(intent) ?: app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private val photoDir: File get() = File(app.filesDir, "photos").apply { mkdirs() }

    override fun savePhoto(id: String, jpeg: ByteArray): String {
        val file = photoDir.resolve("$id.jpg")
        val tmp = photoDir.resolve("$id.jpg.tmp")
        tmp.writeBytes(jpeg)
        if (!tmp.renameTo(file)) { file.writeBytes(jpeg); tmp.delete() }
        return file.absolutePath
    }

    override fun readPhoto(path: String): ByteArray? = File(path).takeIf { it.isFile }?.readBytes()

    override fun deletePhoto(path: String) {
        File(path).delete()
    }

    override fun scanningAvailable(): Boolean = app.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    // ---- Notifications, background work, network ---------------------------------------------

    override fun setWindowReminders(enabled: Boolean, dueCount: Int) {
        // [dueCount] is not used here: the worker recounts at delivery time so the text is never stale.
        if (enabled) {
            Notifications.ensureChannel(app)
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                activity?.requestNotifications?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Work.scheduleReminders(app)
        } else {
            Work.cancelReminders(app)
        }
    }

    override fun requestBackgroundSync() {
        if (backend?.isConfigured == true) Work.syncSoon(app)
    }

    override fun onNetworkAvailable(callback: () -> Unit) {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = callback()
        })
    }

    private companion object {
        const val MAX_EDGE = 2048
        const val MAX_IMPORT_BYTES = 25L * 1024 * 1024
    }
}
