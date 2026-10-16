package com.Nightjar.Th3GradeLibraryKMP

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AppCleanupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cleanAllTemporaryPdfFiles(this)
        stopSelf()
    }

    companion object {
        fun cleanAllTemporaryPdfFiles(context: Context) {
            try {
                com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(context.cacheDir)
                context.filesDir?.let { com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(it) }
                com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
                try {
                    coil3.SingletonImageLoader.get(context).memoryCache?.clear()
                } catch (_: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
