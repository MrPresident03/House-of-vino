package com.houseofvino.app

import com.houseofvino.app.platform.PickedFile
import com.houseofvino.app.platform.PlatformServices
import com.houseofvino.core.money.MoneyFormatStyle
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.createJvmDriver
import com.houseofvino.data.integrations.UnavailableBarcodeLookup
import java.io.File

/** Deterministic platform for UI tests: no network, no files outside memory, a fixed date. */
class FakePlatform(var todayValue: IsoDate = IsoDate.of(2026, 10, 17)) : PlatformServices {
    override val platformName = "Test"
    override val backend = null
    override val appVersion = "test"

    override fun today(): IsoDate = todayValue
    override fun utcOffsetMinutes(): Int = 0
    override fun moneyFormat(): MoneyFormatStyle = MoneyFormatStyle()
    override fun prefersReducedMotion(): Boolean = true

    val shared = mutableListOf<Pair<String, String>>()
    override suspend fun shareText(fileName: String, mimeType: String, content: String): String? {
        shared += fileName to content
        return null
    }

    var nextTextFile: PickedFile? = null
    override fun pickTextFile(onPicked: (PickedFile?) -> Unit) = onPicked(nextTextFile)

    var nextPhoto: ByteArray? = null
    override fun pickPhoto(onPicked: (ByteArray?) -> Unit) = onPicked(nextPhoto)

    val openedUrls = mutableListOf<String>()
    override fun openUrl(url: String) { openedUrls += url }
    override fun openAppSettings() {}

    val photos = HashMap<String, ByteArray>()
    override fun savePhoto(id: String, jpeg: ByteArray): String = "mem://$id".also { photos[it] = jpeg }
    override fun readPhoto(path: String): ByteArray? = photos[path]
    override fun deletePhoto(path: String) { photos.remove(path) }

    override fun scanningAvailable(): Boolean = false

    var remindersOn: Boolean? = null
    override fun setWindowReminders(enabled: Boolean, dueCount: Int) { remindersOn = enabled }
    override fun requestBackgroundSync() {}
    override fun onNetworkAvailable(callback: () -> Unit) {}
}

/** A graph on a fresh file-backed database (in-memory JDBC databases are per-connection). */
fun testGraph(platform: FakePlatform = FakePlatform()): AppGraph {
    val file = File.createTempFile("hv-ui-", ".db").apply { delete(); deleteOnExit() }
    return AppGraph(platform, createJvmDriver(file.absolutePath), barcodeLookup = UnavailableBarcodeLookup)
}
