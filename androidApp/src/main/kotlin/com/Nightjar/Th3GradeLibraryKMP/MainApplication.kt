package com.Nightjar.Th3GradeLibraryKMP

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toPath
import java.io.File
import kotlinx.coroutines.launch

class MainApplication : LibraryApplication(), SingletonImageLoader.Factory {

    companion object {
        lateinit var instance: MainApplication
            private set
        val appContext: android.content.Context get() = instance.applicationContext
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        // Persistent storage scales with the device's available space (OS LRU caps effective use); RAM stays at Google/Android guidance.
        val usableBytes = context.cacheDir.parentFile?.usableSpace ?: (512L * 1024 * 1024)
        val diskCacheCapBytes = (usableBytes / 4).coerceIn(512L * 1024 * 1024, 4L * 1024 * 1024 * 1024)
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    // Google/Android guidance: RAM stays limited - keeps 25% of app memory ceiling while browsing.
                    .maxSizePercent(context, 0.25)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").path.toPath())
                    // Persistent storage: scales with device's available space, not a fixed tiny cap.
                    .maxSizeBytes(diskCacheCapBytes)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Clean up old temporary PDF and tmp files on startup
        AppCleanupService.cleanAllTemporaryPdfFiles(this)

        try {
            startService(android.content.Intent(this, AppCleanupService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Pre-warm Coil ImageLoader asynchronously on IO thread to eliminate first-screen image pipeline delay
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                coil3.SingletonImageLoader.get(this@MainApplication)
            } catch (_: Exception) {}
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Play Vitals memory guidance: reclaim as soon as the system reports pressure.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // التطبيق في الخلفية أو تحت الضغط: نفرغ الذاكرة فوراً
            SingletonImageLoader.get(this).memoryCache?.clear()
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            // أوضاع الخطر (اقتراب الـBackground kill): إفراغ كامل للذاكرة المؤقتة
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
        }
    }
}
