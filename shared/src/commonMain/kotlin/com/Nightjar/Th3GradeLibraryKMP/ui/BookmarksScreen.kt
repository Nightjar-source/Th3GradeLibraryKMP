@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.Nightjar.Th3GradeLibraryKMP.LocalAnimatedVisibilityScope
import com.Nightjar.Th3GradeLibraryKMP.LocalSharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsReturningFromPdf
import com.Nightjar.Th3GradeLibraryKMP.theme.expressiveButtonMorph
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BookmarksScreen(
    savedItems: List<BookItem>,
    savedPages: List<String>,
    isDark: Boolean,
    onNavigateToPdf: (BookItem, Int) -> Unit,
    bottomPadding: Dp = 130.dp,
    onScrollableStateChanged: (Boolean) -> Unit = {},
    lazyGridState: LazyGridState = rememberLazyGridState()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val activeTarget = TransitionTargetTracker.activeTargetId
            val isTransitionActive = TransitionTargetTracker.isTransitionActive
            val isReturningFromPdf = LocalIsReturningFromPdf.current
            val groupedPages = remember(savedPages, activeTarget, isTransitionActive, isReturningFromPdf) {
                val allPages = savedPages.toMutableSet()
                if ((isTransitionActive || isReturningFromPdf) && activeTarget != null && activeTarget.startsWith("bookmark_")) {
                    val sub = activeTarget.removePrefix("bookmark_")
                    val lastUnderscore = sub.lastIndexOf('_')
                    if (lastUnderscore > 0 && lastUnderscore < sub.length - 1) {
                        val bookId = sub.substring(0, lastUnderscore)
                        val pageNum = sub.substring(lastUnderscore + 1)
                        allPages.add("$bookId:$pageNum")
                    }
                }
                allPages.mapNotNull {
                    val lastColon = it.lastIndexOf(':')
                    if (lastColon > 0 && lastColon < it.length - 1) {
                        val bId = it.substring(0, lastColon)
                        val pIdx = it.substring(lastColon + 1).toIntOrNull()
                        if (pIdx != null) bId to pIdx else null
                    } else null
                }.groupBy({ it.first }, { it.second })
            }

            val sortedBookIds = remember(groupedPages, savedItems) {
                groupedPages.keys.sortedBy { bookId ->
                    savedItems.indexOfFirst { it.id == bookId }.takeIf { it >= 0 } ?: Int.MAX_VALUE
                }
            }

            if (sortedBookIds.isEmpty() && savedPages.isEmpty()) {
                LaunchedEffect(Unit) {
                    onScrollableStateChanged(false)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "لا توجد محفوظات",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "المحفوظات قسم خاص للصفحات التي تحفظها من قارئ الكتب والملازم اضغط على أيقونة النجمة للحفظ او الازالة او لاظهار الصفحات اضغط على اسم الملزمة او الكتاب",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                val canScroll by remember {
                    derivedStateOf {
                        lazyGridState.canScrollForward || lazyGridState.canScrollBackward
                    }
                }
                LaunchedEffect(canScroll) {
                    onScrollableStateChanged(canScroll)
                }

                val isNavigatingBack = LocalIsNavigatingBack.current
                LaunchedEffect(isNavigatingBack) {
                    if (isNavigatingBack) {
                        lazyGridState.stopScroll()
                    }
                }

                val adaptiveConfig = LocalAppAdaptiveConfig.current
                val isMedium = adaptiveConfig.isWide
                val screenVisibilityScope = LocalAnimatedVisibilityScope.current
                val expandedSet by SyncEngine.expandedBookmarks.collectAsState()

                val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
                val dynamicTopPadding = if (isMedium) statusBarHeight + 52.dp else statusBarHeight + 68.dp

                val dynamicBookmarkColumns = remember(adaptiveConfig) {
                    adaptiveConfig.bookmarksGridCells()
                }

                LazyVerticalGrid(
                    state = lazyGridState,
                    userScrollEnabled = true,
                    columns = dynamicBookmarkColumns,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .elasticOverscroll(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = dynamicTopPadding, bottom = bottomPadding)
                ) {
                    items(sortedBookIds, key = { it }) { bookId ->
                        val book = remember(bookId, savedItems) { savedItems.find { it.id == bookId } }
                        if (book != null) {
                            val pages = groupedPages[bookId] ?: emptyList()
                            BookAccordionItem(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = spring(
                                        dampingRatio = 0.86f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                                item = book,
                                pages = pages,
                                isPageSaved = { pageIdx -> savedPages.contains("${book.id}:$pageIdx") },
                                isDark = isDark,
                                isExpanded = expandedSet.contains(book.id),
                                onToggleExpand = { SyncEngine.toggleExpandedBookmark(book.id) },
                                onNavigateToPdf = { pageIdx -> onNavigateToPdf(book, pageIdx) },
                                screenVisibilityScope = screenVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookAccordionItem(
    item: BookItem,
    pages: List<Int>,
    isPageSaved: (Int) -> Boolean,
    isDark: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToPdf: (Int) -> Unit,
    screenVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier
) {
    val isReturningFromPdf = LocalIsReturningFromPdf.current
    val isCardActiveTarget = (TransitionTargetTracker.isTransitionActive || isReturningFromPdf) &&
            (TransitionTargetTracker.activeTargetId?.startsWith("bookmark_${item.id}_") == true)
    val isExpandedEffective = isExpanded || isCardActiveTarget

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(isDark, borderRadius = 22.dp, alpha = 0.96f)
            .clip(RoundedCornerShape(22.dp))
    ) {
        val headerInteractionSource = remember { MutableInteractionSource() }

        // Accordion Header (Material 3 Expressive 2026 with Fluid Scale & Morph)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .expressiveButtonMorph(
                    restRadius = 22.dp,
                    pressedRadius = 16.dp,
                    pressedScale = 0.985f,
                    interactionSource = headerInteractionSource
                )
                .clickable(
                    interactionSource = headerInteractionSource,
                    indication = null
                ) { onToggleExpand() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val cloudManifest by com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.manifest.collectAsState()
                val cloudCoverUrl = remember(item, cloudManifest) {
                    val m = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.getMeta(item)
                    m?.cover_url?.takeIf { it.isNotBlank() }
                }
                val hasCloudCover = !cloudCoverUrl.isNullOrBlank()
                val hasBundledCover = item.coverResName.isNotBlank() && item.coverResName != "app_icon" && item.id != "b_abood"

                if (hasCloudCover || hasBundledCover) {
                    val remoteUrl = if (hasCloudCover) cloudCoverUrl!! else if (item.coverResName.startsWith("http://") || item.coverResName.startsWith("https://")) item.coverResName else null
                    val persistentCover = if (remoteUrl != null) rememberPersistentCloudCover(item, remoteUrl) else null
                    val coverData = persistentCover ?: remember(item) {
                        try {
                            Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png")
                        } catch (_: Throwable) {
                            null
                        }
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(coverData)
                            .memoryCacheKey("book_cover_${item.id}_${coverData.hashCode()}")
                            .diskCacheKey("book_cover_${item.id}_${coverData.hashCode()}")
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val pdfThumbnail = rememberPdfCoverThumbnail(item)
                    if (pdfThumbnail != null) {
                        androidx.compose.foundation.Image(
                            bitmap = pdfThumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            Color(0xFF2563EB).copy(alpha = 0.25f),
                                            Color(0xFF1D4ED8).copy(alpha = 0.40f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val pagesCount = pages.size
                val pagesCountText = when {
                    pagesCount == 1 -> "صفحة واحدة محفوظة"
                    pagesCount == 2 -> "صفحتان محفوظتان"
                    pagesCount in 3..10 -> "$pagesCount صفحات محفوظة"
                    else -> "$pagesCount صفحة محفوظة"
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = pagesCountText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            val arrowRotation by animateFloatAsState(
                targetValue = if (isExpandedEffective) 180f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "arrow_rotation"
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpandedEffective) "إغلاق" else "توسيع",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = arrowRotation
                    }
                )
            }
        }

        // Instant synchronized divider line directly under Header
        val dividerAlpha by animateFloatAsState(
            targetValue = if (isExpandedEffective) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "divider_alpha"
        )
        if (dividerAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .graphicsLayer { alpha = dividerAlpha }
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
                        CircleShape
                    )
            )
        }

        val sharedTransitionScope = LocalSharedTransitionScope.current
        val effectiveVisibilityScope = screenVisibilityScope ?: LocalAnimatedVisibilityScope.current

        // Smooth Accordion Expansion / Collapse with Material 3 Expressive Spring Motion
        AnimatedVisibility(
            visible = isExpandedEffective,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                expandFrom = Alignment.Top
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 180,
                    easing = LinearOutSlowInEasing
                )
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                shrinkTowards = Alignment.Top
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(horizontal = 14.dp)
                    .padding(top = 10.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortedPages = remember(pages) { pages.sorted() }
                sortedPages.forEach { pageIndex ->
                    key(item.id, pageIndex) {
                        BookmarkPageRow(
                            item = item,
                            pageIndex = pageIndex,
                            isDark = isDark,
                            isPageSaved = isPageSaved(pageIndex),
                            onNavigateToPdf = { onNavigateToPdf(pageIndex) },
                            sharedTransitionScope = sharedTransitionScope,
                            effectiveVisibilityScope = effectiveVisibilityScope
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BookmarkPageRow(
    item: BookItem,
    pageIndex: Int,
    isDark: Boolean,
    isPageSaved: Boolean,
    onNavigateToPdf: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    effectiveVisibilityScope: AnimatedVisibilityScope?
) {
    var isItemDeletedLocally by remember { mutableStateOf(false) }
    val rowInteractionSource = remember { MutableInteractionSource() }

    val rowTargetKey = "bookmark_${item.id}_$pageIndex"
    val isReturning = LocalIsReturningFromPdf.current
    val isTransitionActive = TransitionTargetTracker.isTransitionActive

    // Deferred removal: wait until shrink animation back from PDF Reader completes before collapsing unstarred row
    LaunchedEffect(isPageSaved, isTransitionActive, isReturning) {
        if (!isPageSaved && !isTransitionActive && !isReturning) {
            isItemDeletedLocally = true
        } else if (isPageSaved) {
            isItemDeletedLocally = false
        }
    }

    AnimatedVisibility(
        visible = !isItemDeletedLocally,
        enter = expandVertically(
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMediumLow
            ),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(160, easing = FastOutLinearInEasing)
        )
    ) {
        // Always attach sharedBounds (matching LibraryScreen) so source bounds are known before click!
        val rowSharedBoundsModifier = if (sharedTransitionScope != null && effectiveVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = rowTargetKey),
                    animatedVisibilityScope = effectiveVisibilityScope,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = tween(
                            durationMillis = 150,
                            delayMillis = 75,
                            easing = LinearEasing
                        )
                    ),
                    exit = androidx.compose.animation.fadeOut(
                        animationSpec = tween(
                            durationMillis = 130,
                            easing = LinearEasing
                        )
                    ),
                    renderInOverlayDuringTransition = true,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(14.dp)),
                    boundsTransform = { _, _ ->
                        spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
                    },
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(androidx.compose.ui.layout.ContentScale.Crop, Alignment.Center)
                )
            }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            // Foreground Interactive Bookmark Row (removal handled via Star toggle inside PDF Reader)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rowSharedBoundsModifier)
                    .expressiveButtonMorph(
                        restRadius = 14.dp,
                        pressedRadius = 8.dp,
                        pressedScale = 0.96f,
                        interactionSource = rowInteractionSource,
                        backgroundColor = if (isDark) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        borderWidth = 1.dp,
                        borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                    )
                    .clickable(
                        interactionSource = rowInteractionSource,
                        indication = null
                    ) {
                        TransitionTargetTracker.activeTargetId = rowTargetKey
                        TransitionTargetTracker.isTransitionActive = true
                        onNavigateToPdf()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Page",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "الصفحة رقم ${pageIndex + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
