package com.Nightjar.Th3GradeLibraryKMP

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AppCleanupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cleanAllTemporaryPdfFiles(applicationContext)
        stopSelf()
    }

    companion object {
        fun cleanAllTemporaryPdfFiles(context: Context) {
            try {
                val targets = mutableListOf<java.io.File>()
                targets.add(context.cacheDir)
                targets.add(context.filesDir)
                targets.add(context.codeCacheDir)
                targets.add(context.noBackupFilesDir)
                context.externalCacheDir?.let { targets.add(it) }
                try {
                    context.externalCacheDirs?.filterNotNull()?.forEach { if (!targets.contains(it)) targets.add(it) }
                } catch (e: Exception) { }
                targets.forEach { dir ->
                    try {
                        dir.listFiles()?.forEach { file ->
                            if (file.name.startsWith("pdf_") && (file.name.endsWith(".pdf") || file.name.endsWith(".tmp"))) {
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
