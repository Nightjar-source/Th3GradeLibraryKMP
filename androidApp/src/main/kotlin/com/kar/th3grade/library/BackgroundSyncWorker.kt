package com.Nightjar.gradeiraqi3library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Nightjar.gradeiraqi3library.network.SyncEngine

class BackgroundSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notification = com.Nightjar.gradeiraqi3library.NotificationHelper.getSyncNotification(appContext)
        var type = 0
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            type = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
        return androidx.work.ForegroundInfo(1001, notification, type)
    }

    override suspend fun doWork(): Result {
        // Force the CPU awake for normal WorkManager executions as well (if not already held by Alarm)
        if (WakeLockManager.activeWakeLock == null) {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Th3Grade:WorkerWakeLock")
            wakeLock.acquire(3 * 60 * 1000L) // 3 mins max
            WakeLockManager.activeWakeLock = wakeLock
        }
        
        return try {
            if (SyncEngine.platformActionHandler == null) {
                SyncEngine.platformActionHandler = object : com.Nightjar.gradeiraqi3library.data.PlatformActionHandler {
                    override fun showNewsNotification(titles: List<String>, count: Int) {
                        val withSound = SyncEngine.appSettings.value.notificationSound
                        NotificationHelper.showNotifications(appContext, titles, count, withSound)
                    }
                    override fun areNotificationsEnabled(): Boolean {
                        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            androidx.core.app.NotificationManagerCompat.from(appContext).areNotificationsEnabled()
                        }
                    }
                    override fun playNotificationSound() {
                        // Sound is already handled locally via NotificationChannel and R.raw.custom_sound
                    }
                    
                    override fun addHomeScreenShortcut(item: com.Nightjar.gradeiraqi3library.data.BookItem) {}
                    override fun showToast(message: String) {}
                    override fun openUrl(url: String) {}
                    override fun startVoiceSearch(onResult: (String) -> Unit, onEnd: () -> Unit) { onEnd() }
                    override fun rescheduleBackgroundSync() {}
                    override fun openNotificationSettings() {}
                    override fun requestNotificationPermission() {}
                    override fun isBatteryOptimizationIgnored(): Boolean = true
                    override fun requestBatteryOptimizationExemption() {}
                    override fun canScheduleExactAlarms(): Boolean = true
                    override fun requestExactAlarmPermission() {}
                }
            }

            // SyncEngine internally checks time intervals and sends notifications via PlatformActionHandler
            val fetchedCount = SyncEngine.fetchAndSync(forced = false)
            
            // Dead Man's Switch: If the WorkManager succeeds in running, cancel the old alarm and schedule a new one!
            // This prevents the Alarm from ever waking the device as long as WorkManager is healthy.
            HeartbeatScheduler.cancelHeartbeat(appContext)
            HeartbeatScheduler.scheduleHeartbeat(appContext)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        } finally {
            // Dynamically release the heartbeat WakeLock as soon as the sync finishes or errors out
            try {
                WakeLockManager.activeWakeLock?.let {
                    if (it.isHeld) it.release()
                }
                WakeLockManager.activeWakeLock = null
            } catch (e: Exception) {}
        }
    }
}
