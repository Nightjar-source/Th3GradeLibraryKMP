package com.Nightjar.Th3GradeLibraryKMP.data

import android.content.Context
import java.io.File

actual object BookStorageEngine {
    private const val STORAGE_DIR_NAME = "books_storage"
    private val bundledAssetCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, STORAGE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getBooksDir(context: Context): File {
        val dir = File(context.filesDir, "files/books")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getNotesDir(context: Context): File {
        val dir = File(context.filesDir, "files/notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCoversDir(context: Context): File {
        val dir = File(context.filesDir, "files/covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPermanentFile(context: Context, item: BookItem): File {
        val targetDir = if (item.isNote) getNotesDir(context) else getBooksDir(context)
        val structuredFile = File(targetDir, "pdf_${item.id}.pdf")
        if (structuredFile.exists()) return structuredFile

        // Fallback for items previously downloaded into books_storage/
        val legacyFile = File(getStorageDir(context), "pdf_${item.id}.pdf")
        if (legacyFile.exists()) return legacyFile

        return structuredFile
    }

    fun getPermanentCoverFile(context: Context, item: BookItem): File {
        return File(getCoversDir(context), "cover_${item.id}_v2.jpg")
    }

    fun getPermanentUrlCoverFile(context: Context, item: BookItem, coverUrl: String): File {
        return File(getCoversDir(context), "cover_${item.id}_url_${coverUrl.hashCode()}.jpg")
    }

    actual fun isDownloaded(context: Any, item: BookItem): Boolean {
        val ctx = context as Context
        val file = getPermanentFile(ctx, item)
        if (file.exists() && file.length() > 1024L) {
            return true
        }
        if (hasBundledAsset(ctx, item)) {
            return true
        }
        return false
    }

    actual fun isUpdateAvailable(context: Any, item: BookItem): Boolean {
        return try {
            val ctx = context as Context
            val file = getPermanentFile(ctx, item)
            if (!file.exists() || file.length() <= 1024L) return false
            val meta = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.getMeta(item)
            val metaSize = meta?.file_size ?: -1L
            val pdfChanged = metaSize > 0L && kotlin.math.abs(file.length() - metaSize) > 4096L
            val coverUrl = meta?.cover_url?.takeIf { it.isNotBlank() }
            val coverChanged = coverUrl != null && !getPermanentUrlCoverFile(ctx, item, coverUrl).let { it.exists() && it.length() > 512L }
            pdfChanged || coverChanged
        } catch (_: Exception) {
            false
        }
    }

    actual fun hasBundledAsset(context: Any, item: BookItem): Boolean {
        return bundledAssetCache.getOrPut(item.id) {
            try {
                val ctx = context as Context
                val possiblePaths = listOf(
                    "composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources/${item.pdfPath}",
                    "composeResources/shared.generated.resources/${item.pdfPath}",
                    item.pdfPath
                )
                val assetManager = ctx.assets
                for (path in possiblePaths) {
                    try {
                        assetManager.open(path).use { return@getOrPut true }
                    } catch (_: Exception) {}
                }
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    actual fun deleteBook(context: Any, item: BookItem): Boolean {
        val ctx = context as Context
        val file = getPermanentFile(ctx, item)
        val coverFile = getPermanentCoverFile(ctx, item)
        try { if (coverFile.exists()) coverFile.delete() } catch (_: Exception) {}
        return if (file.exists()) file.delete() else false
    }
}
