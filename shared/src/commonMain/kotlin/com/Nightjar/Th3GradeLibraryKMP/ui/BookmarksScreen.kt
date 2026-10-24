@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.Nightjar.Th3GradeLibraryKMP.theme.adaptiveItemEntrance
import com.Nightjar.Th3GradeLibraryKMP.theme.expressiveButtonMorph
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass

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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (savedPages.isEmpty()) {
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
                        val parts = it.split(":")
                        if (parts.size == 2) {
                            parts[0] to parts[1].toIntOrNull()
                        } else null
                    }.filter { it.second != null }
                    .groupBy({ it.first }, { it.second!! })
                }

                val isNavigatingBack = LocalIsNavigatingBack.current
                LaunchedEffect(isNavigatingBack) {
                    if (isNavigatingBack) {
                        lazyGridState.stopScroll()
                    }
                }

                val responsiveInfo = com.Nightjar.Th3GradeLibraryKMP.theme.LocalResponsiveLayoutInfo.current
                val isMedium = responsiveInfo.isWide
                val screenVisibilityScope = LocalAnimatedVisibilityScope.current
                val expandedSet by SyncEngine.expandedBookmarks.collectAsState()

                val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
                val dynamicTopPadding = when { responsiveInfo.isPhoneLandscape -> statusBarHeight + 44.dp; isMedium -> statusBarHeight + 72.dp; else -> statusBarHeight + 58.dp }

                var persistentBookIds by remember { mutableStateOf(groupedPages.keys.sortedBy { bookId ->
                    savedItems.indexOfFirst { it.id == bookId }.takeIf { it >= 0 } ?: Int.MAX_VALUE
                }) }

                LaunchedEffect(groupedPages) {
                    val newIds = groupedPages.keys.sortedBy { bookId ->
                        savedItems.indexOfFirst { it.id == bookId }.takeIf { it >= 0 } ?: Int.MAX_VALUE
                    }
                    if (newIds.size < persistentBookIds.size) {
                        // Delay removal so the deleted bookmark row smoothly shrinks out first
                        kotlinx.coroutines.delay(450)
                    }
                    persistentBookIds = newIds
                }

                val sortedBookIds = if (persistentBookIds.isNotEmpty()) persistentBookIds else groupedPages.keys.sortedBy { bookId ->
                    savedItems.indexOfFirst { it.id == bookId }.takeIf { it >= 0 } ?: Int.MAX_VALUE
                }

                // Ensure target bookmark is expanded and in viewport when returning from PDF
                LaunchedEffect(isReturningFromPdf, activeTarget) {
                    if (isReturningFromPdf && activeTarget != null && activeTarget.startsWith("bookmark_")) {
                        val sub = activeTarget.removePrefix("bookmark_")
                        val lastUnderscore = sub.lastIndexOf('_')
                        if (lastUnderscore > 0) {
                            val bookId = sub.substring(0, lastUnderscore)
                            SyncEngine.setBookmarkExpanded(bookId, true)
                            val idx = sortedBookIds.indexOf(bookId)
                            if (idx >= 0) {
                                val visibleIndices = lazyGridState.layoutInfo.visibleItemsInfo.map { it.index }
                                if (idx !in visibleIndices) {
                                    lazyGridState.scrollToItem(idx)
                                }
                            }
                        }
                    }
                }

                // Explicit Scroll State Preservation across tab navigation and background recreation
                var savedFirstVisibleItemIndex by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(0) }
                var savedFirstVisibleItemScrollOffset by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(0) }

                LaunchedEffect(lazyGridState.firstVisibleItemIndex, lazyGridState.firstVisibleItemScrollOffset) {
                    if (lazyGridState.firstVisibleItemIndex > 0 || lazyGridState.firstVisibleItemScrollOffset > 0) {
                        savedFirstVisibleItemIndex = lazyGridState.firstVisibleItemIndex
                        savedFirstVisibleItemScrollOffset = lazyGridState.firstVisibleItemScrollOffset
                    }
                }

                LaunchedEffect(sortedBookIds.isNotEmpty()) {
                    if (sortedBookIds.isNotEmpty() && (savedFirstVisibleItemIndex > 0 || savedFirstVisibleItemScrollOffset > 0)) {
                        if (lazyGridState.firstVisibleItemIndex == 0 && lazyGridState.firstVisibleItemScrollOffset == 0) {
                            lazyGridState.scrollToItem(savedFirstVisibleItemIndex, savedFirstVisibleItemScrollOffset)
                        }
                    }
                }

                val dynamicBookmarkColumns = remember(responsiveInfo) {
                    if (responsiveInfo.isWide) {
                        if (responsiveInfo.width >= 1800.dp) GridCells.Fixed(5)
                        else if (responsiveInfo.width >= 1440.dp) GridCells.Fixed(4)
                        else if (responsiveInfo.width >= 1080.dp) GridCells.Fixed(3)
                        else GridCells.Fixed(2)
                    } else {
                        GridCells.Fixed(1)
                    }
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
                    itemsIndexed(
                        items = sortedBookIds,
                        key = { _, it -> it },
                        contentType = { _, _ -> "book_accordion" }
                    ) { index, bookId ->
                        val book = remember(bookId, savedItems) { savedItems.find { it.id == bookId } }
                        if (book != null) {
                            val pages = groupedPages[bookId] ?: emptyList()
                            BookAccordionItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .com.Nightjar.Th3GradeLibraryKMP.theme.screenEntranceCascade(index),
                                item = book,
                                pages = pages,
                                isPageSaved = { pageIdx -> savedPages.contains("${book.id}:$pageIdx") },
                                isDark = isDark,
                                isExpanded = expandedSet.contains(book.id),
                                onToggleExpand = {
                                    TransitionTargetTracker.reset()
                                    SyncEngine.toggleExpandedBookmark(book.id)
                                },
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
    val sortedPages = remember(pages) { pages.distinct().sorted() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(isDark, borderRadius = 20.dp, alpha = 0.96f)
            .clip(RoundedCornerShape(20.dp))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                )
            )
    ) {
        // Accordion Header
        val responsiveInfo = com.Nightjar.Th3GradeLibraryKMP.theme.LocalResponsiveLayoutInfo.current
        val isMicro = responsiveInfo.width < 360.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isMicro) 16.dp else 20.dp))
                .clickable { onToggleExpand() }
                .padding(if (isMicro) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isMicro) 48.dp else 60.dp)
                    .clip(RoundedCornerShape(if (isMicro) 10.dp else 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(if (isMicro) 10.dp else 16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isMicro) 14.sp else 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val pagesCount = sortedPages.size
                val pagesCountText = when {
                    pagesCount == 1 -> "صفحة واحدة محفوظة"
                    pagesCount == 2 -> "صفحتان محفوظتان"
                    pagesCount in 3..10 -> "$pagesCount صفحات محفوظة"
                    else -> "$pagesCount صفحة محفوظة"
                }
                Text(
                    text = pagesCountText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val arrowRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                ),
                label = "arrow_rotation"
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "إغلاق" else "توسيع",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .graphicsLayer {
                        rotationZ = arrowRotation
                    }
            )
        }

        // Instant synchronized divider line directly under Header (t=0, smoothly blends with expand)
        val dividerAlpha by animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
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
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f),
                        CircleShape
                    )
            )
        }

        // Expanded Bookmark Rows with Ultra-Moist Fluid Organic Spring (سائل رطب كالحرير)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                ),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                ),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sharedTransitionScope = LocalSharedTransitionScope.current

                var renderedPages by remember { mutableStateOf(sortedPages) }
                LaunchedEffect(sortedPages) {
                    if ((sortedPages - renderedPages.toSet()).isNotEmpty()) {
                        renderedPages = (renderedPages + sortedPages).distinct().sorted()
                    }
                }

                renderedPages.forEach { pageIndex ->
                    key(pageIndex) {
                    val isPageStillSaved = isPageSaved(pageIndex)
                    val rowInteractionSource = remember { MutableInteractionSource() }
                    val rowTargetKey = "bookmark_${item.id}_$pageIndex"
                    val isReturningFromPdf = LocalIsReturningFromPdf.current
                    val isThisRowTarget = (TransitionTargetTracker.activeTargetId == rowTargetKey)
                    val isThisRowActive = isThisRowTarget && (TransitionTargetTracker.isTransitionActive || isReturningFromPdf)
                    val isRowVisible = isPageStillSaved || isThisRowActive

                    AnimatedVisibility(
                        visible = isRowVisible,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
                            expandFrom = Alignment.Top
                        ) + fadeIn(tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                    ) {
                        val rowSharedModifier = if (sharedTransitionScope != null && screenVisibilityScope != null && isThisRowActive) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = rowTargetKey),
                                    animatedVisibilityScope = screenVisibilityScope,
                                    enter = androidx.compose.animation.fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 130,
                                            easing = androidx.compose.animation.core.LinearEasing
                                        )
                                    ),
                                    exit = androidx.compose.animation.fadeOut(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 150,
                                            delayMillis = 75,
                                            easing = androidx.compose.animation.core.FastOutLinearInEasing
                                        )
                                    ),
                                    renderInOverlayDuringTransition = isThisRowActive,
                                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(14.dp)),
                                    boundsTransform = { _, _ ->
                                        spring(dampingRatio = 0.82f, stiffness = 280f)
                                    },
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                                )
                            }
                        } else Modifier

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isThisRowActive) Modifier.zIndex(15f) else Modifier)
                                .then(rowSharedModifier)
                                .liquidGlass(
                                    isDark = isDark,
                                    borderRadius = 14.dp,
                                    alpha = if (isDark) 0.60f else 0.45f
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(interactionSource = rowInteractionSource, indication = null) {
                                    TransitionTargetTracker.activeTargetId = rowTargetKey
                                    TransitionTargetTracker.isTransitionActive = true
                                    onNavigateToPdf(pageIndex)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Page",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "الصفحة رقم ${pageIndex + 1}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}
