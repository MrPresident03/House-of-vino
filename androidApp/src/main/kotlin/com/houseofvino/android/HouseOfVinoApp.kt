package com.houseofvino.android

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.houseofvino.app.AppGraph
import com.houseofvino.data.db.createAndroidDriver
import com.houseofvino.data.repo.SettingsRepository

class HouseOfVinoApp : Application(), Configuration.Provider {
    val platform: AndroidPlatform by lazy { AndroidPlatform(this) }

    /** One graph per process, shared by the activity and the workers. */
    val graph: AppGraph by lazy { AppGraph(platform, createAndroidDriver(this)) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN).build()

    override fun onCreate() {
        super.onCreate()
        if (platform.backend?.isConfigured == true) Work.schedulePeriodicSync(this)
        if (graph.settings.get(SettingsRepository.KEY_WINDOW_REMINDERS) == "true") Work.scheduleReminders(this)
    }
}
