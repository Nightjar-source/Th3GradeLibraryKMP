package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import com.Nightjar.Th3GradeLibraryKMP.theme.parseHexColor

/**
 * Three-tier resilient cover component:
 * 1. Tier 1: Static cover image asset loaded via Coil 3.
 * 2. Tier 2: First page of PDF rendered in background (or user's saved reading page).
 * 3. Tier 3: Elegant fallback layout with broken-file indicator, dynamic theme gradient,
 *            and readable book title (prepared for future remote download links).
 */
@Composable
fun BookCoverImage(
    item: BookItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    targetPage: Int? = null
) {
    var isStaticImageFailed by remember(item.id, item.coverResName) {
        mutableStateOf(item.coverResName.isBlank())
    }

    // Determine target page (caller-specified, saved progress, or default 0)
    val savedPages by SyncEngine.lastReadPages.collectAsState()
    val resolvedPage = targetPage ?: (savedPages[item.id] ?: 0)

    val pdfThumbnail = if (isStaticImageFailed) {
        rememberPdfPageThumbnail(item = item, targetPage = resolvedPage)
    } else null

    val startColor = remember(item.colorStart) { parseHexColor(item.colorStart) }
    val endColor = remember(item.colorEnd) { parseHexColor(item.colorEnd) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (!isStaticImageFailed) {
            // Tier 1: Static Cover Image via Coil 3
            val modelUri = remember(item.coverResName) {
                Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png")
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(modelUri)
                    .crossfade(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = item.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) {
                        isStaticImageFailed = true
                    }
                }
            )
        } else if (pdfThumbnail != null) {
            // Tier 2: Rendered PDF First Page / Saved Progress Page
            Image(
                bitmap = pdfThumbnail,
                contentDescription = item.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Tier 3: Elegant Fallback (Broken Image / Download Pending)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                startColor.copy(alpha = 0.85f),
                                endColor.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = shape
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "ملف بانتظار التنزيل",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
