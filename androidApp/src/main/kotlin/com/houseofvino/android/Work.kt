package com.houseofvino.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.houseofvino.data.repo.SettingsRepository
import java.util.concurrent.TimeUnit

/** WorkManager scheduling. Sync work needs a network; reminders don't. */
object Work {
    private const val SYNC_NOW = "sync-now"
    private const val SYNC_PERIODIC = "sync-periodic"
    private const val REMINDERS = "window-reminders"

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun syncSoon(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SYNC_NOW, ExistingWorkPolicy.KEEP, request)
    }

    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(network).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(SYNC_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.DAYS).setInitialDelay(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(REMINDERS, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDERS)
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as HouseOfVinoApp).graph
        if (graph.syncEngine == null) return Result.success()
        return if (graph.sync.syncNow()) Result.success() else if (runAttemptCount < 5) Result.retry() else Result.failure()
    }
}

/** Posts a monthly note when bottles are in the last year of their window. Counts are computed at delivery. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as HouseOfVinoApp).graph
        if (graph.settings.get(SettingsRepository.KEY_WINDOW_REMINDERS) != "true") return Result.success()
        val due = graph.insights.quickTotals(graph.collectionId, graph.platform.currentYear()).drinkSoonBottles
        if (due > 0) Notifications.windowReminder(applicationContext, due)
        return Result.success()
    }
}

object Notifications {
    private const val CHANNEL = "reminders"
    private const val ID_WINDOW = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_reminders_description)
            },
        )
    }

    @SuppressLint("MissingPermission") // checked on the first line
    fun windowReminder(context: Context, due: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (due == 1) "1 bottle is in the last year of its drinking window." else "$due bottles are in the last year of their drinking window."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_WINDOW, notification)
    }
}
