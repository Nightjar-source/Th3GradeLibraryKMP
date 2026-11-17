package com.Nightjar.Th3GradeLibraryKMP

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import java.util.concurrent.TimeUnit

/**
 * Executes delayed batch delivery of accumulated silent FCM news items
 * when the quiet interval window (e.g., 15m, 30m, 1h) elapses.
 * Zero polling, zero continuous wake-lock: scheduled via WorkManager OneTimeWorkRequest.
 */
class NewsBatchDeliveryWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "news_batch_scheduled_delivery"

        fun scheduleDelivery(context: Context, delayMs: Long) {
            try {
                val clampedDelay = delayMs.coerceAtLeast(1000L)
                val request = OneTimeWorkRequestBuilder<NewsBatchDeliveryWorker>()
                    .setInitialDelay(clampedDelay, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun cancelScheduledDelivery(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val pendingTitles = AppFirebaseMessagingService.getAndClearUnnotifiedNews()
            if (pendingTitles.isNotEmpty()) {
                val now = System.currentTimeMillis()
                AppDataStore.putLong("last_fcm_news_alert_time", now)
                val soundEnabled = SyncEngine.appSettings.value.notificationSound
                NotificationHelper.showNotifications(
                    appContext,
                    pendingTitles,
                    pendingTitles.size,
                    soundEnabled
                )
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
