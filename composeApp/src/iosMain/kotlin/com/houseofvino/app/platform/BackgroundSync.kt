package com.houseofvino.app.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

/** BGTaskScheduler wiring. The identifier is declared in Info.plist (BGTaskSchedulerPermittedIdentifiers). */
object BackgroundSync {
    const val TASK_ID = "com.houseofvino.sync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun register(sync: suspend () -> Boolean) {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(TASK_ID, usingQueue = null) { task ->
            val refresh = task as? BGAppRefreshTask ?: return@registerForTaskWithIdentifier
            schedule() // keep the chain going
            val job = scope.launch {
                val ok = runCatching { sync() }.getOrDefault(false)
                refresh.setTaskCompletedWithSuccess(ok)
            }
            refresh.expirationHandler = { job.cancel(); refresh.setTaskCompletedWithSuccess(false) }
        }
    }

    fun schedule() {
        val request = BGAppRefreshTaskRequest(identifier = TASK_ID).apply { earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(15.0 * 60) }
        runCatching { BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null) }
    }
}
