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
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    // Limit RAM cache to 25% of available application memory
                    .maxSizePercent(context, 0.25)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").path.toPath())
                    // Limit disk cache to 50MB to keep app size minimal
                    .maxSizeBytes(50 * 1024 * 1024L)
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
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // التطبيق في الخلفية (Cached State)، نفرغ الرام فوراً
            SingletonImageLoader.get(this).memoryCache?.clear()
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
        }
    }
}
