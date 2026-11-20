package com.Nightjar.Th3GradeLibraryKMP

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager

class BookDownloadForegroundService : Service() {

    companion object {
        const val ACTION_STOP = "com.Nightjar.Th3GradeLibraryKMP.ACTION_STOP_DOWNLOAD_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        if (intent?.action == ACTION_STOP) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        try {
            val notification = BookDownloadManager.createForegroundNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    BookDownloadManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(BookDownloadManager.NOTIFICATION_ID, notification)
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
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelfSafely()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            if (BookDownloadManager.itemStates.value.values.none { it.status == com.Nightjar.Th3GradeLibraryKMP.network.DownloadStatus.DOWNLOADING }) {
                stopSelfSafely()
            }
        } catch (_: Exception) {}
    }
}
