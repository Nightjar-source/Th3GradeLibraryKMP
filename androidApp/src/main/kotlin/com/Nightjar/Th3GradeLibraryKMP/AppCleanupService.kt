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
        cleanAllTemporaryPdfFiles(applicationContext)
        stopSelf()
    }

    companion object {
        fun cleanAllTemporaryPdfFiles(context: Context) {
            try {
                // Cancel any pending timers so background tasks don't hold file locks
                com.Nightjar.Th3GradeLibraryKMP.ui.cancelAllPendingCacheCleanups()

                // 1. Clean app cache directory
                com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(context.cacheDir)
                context.cacheDir?.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".pdf") || file.name.endsWith(".tmp") || file.name.startsWith("pdf_")) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }

                // 2. Clean external cache directory (if mounted)
                context.externalCacheDir?.let { extCache ->
                    com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(extCache)
                    extCache.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".pdf") || file.name.endsWith(".tmp") || file.name.startsWith("pdf_")) {
                            try { file.delete() } catch (_: Exception) {}
                        }
                    }
                }

                // 3. Clean filesDir in case any temporary PDF was copied there
                context.filesDir?.let { filesDir ->
                    com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(filesDir)
                    filesDir.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".pdf") || file.name.endsWith(".tmp") || file.name.startsWith("pdf_")) {
                            try { file.delete() } catch (_: Exception) {}
                        }
                    }
                }

                // 4. Evict in-memory bitmap cache and coil memory cache
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
