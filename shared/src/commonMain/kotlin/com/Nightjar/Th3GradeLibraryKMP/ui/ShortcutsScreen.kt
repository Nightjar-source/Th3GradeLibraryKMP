@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import com.Nightjar.Th3GradeLibraryKMP.LocalSharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.LocalAnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.Nightjar.Th3GradeLibraryKMP.theme.popInOnInitialLoad
import com.Nightjar.Th3GradeLibraryKMP.theme.screenEntranceCascade
import com.Nightjar.Th3GradeLibraryKMP.theme.adaptiveItemEntrance
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(
    platformActionHandler: PlatformActionHandler,
    isDark: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    onScrollableStateChanged: (Boolean) -> Unit = {},
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    val dynamicBooks by com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.dynamicBooks.collectAsState()
    val dynamicNotes by com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.dynamicNotes.collectAsState()
    val manifest by com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.manifest.collectAsState()

    val allItemsList = remember(dynamicBooks, dynamicNotes, manifest) {
        AllItems.books + AllItems.notes
    }

    val canScroll by remember {
        derivedStateOf {
            lazyGridState.canScrollForward || lazyGridState.canScrollBackward
        }
    }
    LaunchedEffect(canScroll) {
        onScrollableStateChanged(canScroll)
    }

    val adaptiveConfig = LocalAppAdaptiveConfig.current
    val isWide = adaptiveConfig.isWide

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
            val dynamicTopPadding = if (isWide) statusBarHeight + 52.dp else statusBarHeight + 68.dp

            val isNavigatingBack = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack.current
            LaunchedEffect(isNavigatingBack) {
                if (isNavigatingBack) {
                    lazyGridState.stopScroll()
                }
            }

            val dynamicShortcutColumns = remember(adaptiveConfig) {
                adaptiveConfig.shortcutsGridCells()
            }

            LazyVerticalGrid(
                state = lazyGridState,
                userScrollEnabled = !isNavigatingBack,
                columns = dynamicShortcutColumns,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .elasticOverscroll(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = bottomPadding)
            ) {
                item(key = "top_spacer", span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(dynamicTopPadding))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "أضف اختصارات للشاشة الرئيسية",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp).screenEntranceCascade(0)
                    )
                }
                itemsIndexed(
                    items = allItemsList,
                    key = { _, item -> item.id }
                ) { index, item ->
                    ShortcutItemCard(
                        item = item,
                        isDark = isDark,
                        index = index + 1,
                        onClick = {
                            platformActionHandler.addHomeScreenShortcut(item)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, org.jetbrains.compose.resources.ExperimentalResourceApi::class)
@Composable
fun ShortcutItemCard(
    item: BookItem,
    isDark: Boolean,
    index: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val imageModifier = Modifier.fillMaxSize()

    val cloudManifest by com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.manifest.collectAsState()
    val cloudCoverUrl = remember(item, cloudManifest) {
        val m = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.getMeta(item)
        m?.cover_url?.takeIf { it.isNotBlank() }
    }
    val hasCloudCover = !cloudCoverUrl.isNullOrBlank()
    val hasBundledCover = item.coverResName.isNotBlank() && item.coverResName != "app_icon" && item.id != "b_abood"

    val coverData: Any? = remember(item, cloudCoverUrl) {
        if (hasCloudCover) {
            cloudCoverUrl
        } else if (item.coverResName.startsWith("http://") || item.coverResName.startsWith("https://")) {
            item.coverResName
        } else if (hasBundledCover) {
            try {
                com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.getUri(
                    "drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"
                )
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .adaptiveItemEntrance(index, itemId = item.id)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverData != null) {
                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                        .data(coverData)
                        .memoryCacheKey("shortcut_cover_${item.id}_${coverData.hashCode()}")
                        .diskCacheKey("shortcut_cover_${item.id}_${coverData.hashCode()}")
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )
            } else {
                val pdfThumbnail = rememberPdfCoverThumbnail(item)
                if (pdfThumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = pdfThumbnail,
                        contentDescription = null,
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val itemGradStart = remember(item) {
                        try { Color(item.colorStart.removePrefix("#").toLong(16) or 0x00000000FF000000) } catch (_: Exception) { Color(0xFF2563EB) }
                    }
                    val itemGradEnd = remember(item) {
                        try { Color(item.colorEnd.removePrefix("#").toLong(16) or 0x00000000FF000000) } catch (_: Exception) { Color(0xFF1D4ED8) }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(itemGradStart, itemGradEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Pin to home screen",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Justify
        )
    }
}

