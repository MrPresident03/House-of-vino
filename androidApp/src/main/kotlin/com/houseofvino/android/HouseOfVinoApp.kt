package com.houseofvino.android

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.houseofvino.app.AppGraph
import com.houseofvino.data.catalogue.openAndroidCatalogue
import com.houseofvino.data.db.createAndroidDriver
import com.houseofvino.data.repo.SettingsRepository

class HouseOfVinoApp : Application(), Configuration.Provider {
    val platform: AndroidPlatform by lazy { AndroidPlatform(this) }

    /** One graph per process, shared by the activity and the workers. */
    val graph: AppGraph by lazy { AppGraph(platform, createAndroidDriver(this), catalogue = { openAndroidCatalogue(this, catalogueTag()) }) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN).build()

    override fun onCreate() {
        super.onCreate()
        if (graph.settings.get(SettingsRepository.KEY_WINDOW_REMINDERS) == "true") Work.scheduleReminders(this)
    }

    /** Changes with every install or update, so a new APK's catalogue replaces the old copy. */
    private fun catalogueTag(): String {
        val info = packageManager.getPackageInfo(packageName, 0)
        return "${BuildConfig.VERSION_CODE}-${info.lastUpdateTime}"
    }
}
