package com.Nightjar.Th3GradeLibraryKMP.data

/**
 * Android implementation of PlatformMemoryManager (February 2027 Standards).
 */
actual object PlatformMemoryManager {
    actual fun trimMemory() {
        try {
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
            System.gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun clearDeepCache() {
        try {
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
            System.gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
