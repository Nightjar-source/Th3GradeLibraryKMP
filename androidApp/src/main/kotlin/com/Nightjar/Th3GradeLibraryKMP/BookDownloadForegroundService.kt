package com.Nightjar.Th3GradeLibraryKMP

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager

class BookDownloadForegroundService : Service() {

    companion object {
        const val ACTION_STOP = "com.Nightjar.Th3GradeLibraryKMP.ACTION_STOP_DOWNLOAD_SERVICE"
    }

    private var isForegroundActive = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ALWAYS satisfy Android's foreground service contract immediately
        ensureForegroundNotification()

        if (intent?.action == ACTION_STOP) {
            // Post stop slightly delayed so the Android OS acknowledges startForeground first
            mainHandler.postDelayed({
                stopSelfSafely()
            }, 350L)
            return START_NOT_STICKY
        }
        // Allow the Android OS to resurrect this service if terminated under low memory:
        return START_STICKY
    }

    private fun ensureForegroundNotification() {
        if (isForegroundActive) return
        try {
            val notification = BookDownloadManager.createForegroundNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        BookDownloadManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                    isForegroundActive = true
                } catch (e: Exception) {
                    // Fallback without DATA_SYNC flag if restricted by Android 14+
                    startForeground(BookDownloadManager.NOTIFICATION_ID, notification)
                    isForegroundActive = true
                }
            } else {
                startForeground(BookDownloadManager.NOTIFICATION_ID, notification)
                isForegroundActive = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSelfSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
        isForegroundActive = false
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelfSafely()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val isStillDownloading = BookDownloadManager.itemStates.value.values.any { 
                it.status == com.Nightjar.Th3GradeLibraryKMP.network.DownloadStatus.DOWNLOADING 
            }
            if (isStillDownloading) {
                // User swiped app away from recents, but a download is active: KEEP RUNNING in foreground!
                ensureForegroundNotification()
            } else {
                stopSelfSafely()
            }
        } catch (_: Exception) {}
    }
}
