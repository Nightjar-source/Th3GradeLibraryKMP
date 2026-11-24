package com.Nightjar.Th3GradeLibraryKMP.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.BookStorageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Strictly bounded LRU Cache in memory (max 60 thumbnails ~ 15MB max) with automatic memory eviction
private val thumbnailLruCache = object : LruCache<String, ImageBitmap>(60) {}

@Composable
actual fun rememberPdfCoverThumbnail(item: BookItem): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(item.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            try {
                val file = BookStorageEngine.getPermanentFile(context, item)
                if (file.exists() && file.length() > 1024L) {
                    val cacheKey = "${item.id}_${file.lastModified()}"
                    val cached = synchronized(thumbnailLruCache) { thumbnailLruCache.get(cacheKey) }
                    if (cached != null) {
                        withContext(Dispatchers.Main) {
                            bitmap = cached
                        }
                        return@withContext
                    }

                    // 1. Check permanent offline cover file in files/covers/
                    val permanentCover = BookStorageEngine.getPermanentCoverFile(context, item)
                    if (permanentCover.exists() && permanentCover.length() > 512L) {
                        val decoded = android.graphics.BitmapFactory.decodeFile(permanentCover.absolutePath)
                        if (decoded != null) {
                            val imgBmp = decoded.asImageBitmap()
                            synchronized(thumbnailLruCache) {
                                thumbnailLruCache.put(cacheKey, imgBmp)
                            }
                            withContext(Dispatchers.Main) {
                                bitmap = imgBmp
                            }
                            return@withContext
                        }
                    }

                    // 2. Render from PDF page 0 and persist permanently to files/covers/
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            if (renderer.pageCount > 0) {
                                renderer.openPage(0).use { page ->
                                    val width = 360
                                    val height = (360 * page.height / page.width).coerceAtLeast(480)
                                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    // Ensure solid pure white background so transparent PDF pages never turn black
                                    val canvas = android.graphics.Canvas(bmp)
                                    canvas.drawColor(android.graphics.Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    
                                    // Save permanently in user data directory (Offline forever)
                                    try {
                                        permanentCover.parentFile?.mkdirs()
                                        permanentCover.outputStream().use { out ->
                                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                        }
                                    } catch (_: Exception) {}

                                    val imgBmp = bmp.asImageBitmap()
                                    synchronized(thumbnailLruCache) {
                                        thumbnailLruCache.put(cacheKey, imgBmp)
                                    }
                                    withContext(Dispatchers.Main) {
                                        bitmap = imgBmp
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
    return bitmap
}
