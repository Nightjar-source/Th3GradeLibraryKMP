package com.Nightjar.Th3GradeLibraryKMP

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AppCleanupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            // Immediately clean extracted PDFs and memory/bitmap cache when swiped from Recents
            com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(cacheDir)
            filesDir?.let { com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(it) }
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
            try {
                coil3.SingletonImageLoader.get(this).memoryCache?.clear()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }
}
