package com.Nightjar.gradeiraqi3library

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toPath
import java.io.File

class MainApplication : Application(), SingletonImageLoader.Factory {
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
        
        // Clean up old temporary PDF files from cacheDir on startup to keep app size minimal
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("pdf_") && file.name.endsWith(".pdf")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
