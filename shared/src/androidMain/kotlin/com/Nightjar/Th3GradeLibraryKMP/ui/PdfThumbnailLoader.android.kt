package com.Nightjar.Th3GradeLibraryKMP.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberPdfPageThumbnail(
    item: BookItem,
    targetPage: Int
): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = "pdf_${item.id}_thumb_page_$targetPage"

    // Check memory cache first
    var cachedBitmap by remember(cacheKey) {
        mutableStateOf(PdfBitmapCache.cache.get(cacheKey)?.asImageBitmap())
    }

    if (cachedBitmap != null) {
        return cachedBitmap
    }

    LaunchedEffect(item.id, item.pdfPath, targetPage) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                // Check cache again inside IO
                val existing = PdfBitmapCache.cache.get(cacheKey)
                if (existing != null && !existing.isRecycled) {
                    return@withContext existing
                }

                val cacheDir = context.cacheDir
                val pdfFile = File(cacheDir, "pdf_${item.id}.pdf")

                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    val tempTarget = File(cacheDir, "pdf_${item.id}_thumb_${System.currentTimeMillis()}.tmp")
                    val assetManager = context.assets
                    val possiblePaths = listOf(
                        "composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources/${item.pdfPath}",
                        "composeResources/shared.generated.resources/${item.pdfPath}",
                        item.pdfPath
                    )

                    var inputStream: java.io.InputStream? = null
                    for (path in possiblePaths) {
                        try {
                            inputStream = assetManager.open(path)
                            if (inputStream != null) break
                        } catch (_: Exception) {}
                    }

                    if (inputStream != null) {
                        inputStream.use { input ->
                            FileOutputStream(tempTarget).use { output ->
                                input.copyTo(output, 16 * 1024)
                            }
                        }
                    } else {
                        try {
                            val bytes = Res.readBytes(item.pdfPath)
                            tempTarget.writeBytes(bytes)
                        } catch (_: Exception) {
                            return@withContext null
                        }
                    }

                    if (tempTarget.exists() && tempTarget.length() > 0L) {
                        tempTarget.renameTo(pdfFile)
                    }
                }

                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    return@withContext null
                }

                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val pageCount = renderer.pageCount
                        if (pageCount <= 0) return@withContext null
                        val safePage = targetPage.coerceIn(0, pageCount - 1)
                        renderer.openPage(safePage).use { page ->
                            // Render at thumbnail resolution (approx 360x480) for lightweight memory footprint
                            val targetW = 360
                            val targetH = ((targetW.toFloat() / page.width) * page.height).toInt().coerceAtLeast(1)
                            val rendered = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            PdfBitmapCache.cache.put(cacheKey, rendered)
                            rendered
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        if (bitmap != null && !bitmap.isRecycled) {
            cachedBitmap = bitmap.asImageBitmap()
        }
    }

    return cachedBitmap
}
