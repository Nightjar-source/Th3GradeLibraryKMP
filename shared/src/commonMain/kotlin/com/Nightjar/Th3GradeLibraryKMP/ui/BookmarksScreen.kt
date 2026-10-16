package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
                val groupedPages = remember(savedPages, activeTarget, isTransitionActive) {
                    val allPages = savedPages.toMutableSet()
                    if (isTransitionActive && activeTarget != null && activeTarget.startsWith("bookmark_")) {
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

                val screenWidthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
                val isMedium = screenWidthDp >= 600.dp
                val screenVisibilityScope = LocalAnimatedVisibilityScope.current
                val expandedSet by SyncEngine.expandedBookmarks.collectAsState()

                val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
                val dynamicTopPadding = if (isMedium) statusBarHeight + 64.dp else statusBarHeight + 76.dp

                val sortedBookIds = remember(groupedPages, savedItems) {
                    groupedPages.keys.sortedBy { bookId ->
                        savedItems.indexOfFirst { it.id == bookId }.takeIf { it >= 0 } ?: Int.MAX_VALUE
                    }
                }

                LazyVerticalGrid(
                    state = lazyGridState,
                    userScrollEnabled = true,
                    columns = if (isMedium) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 340.dp),
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
                                item = book,
                                pages = pages,
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
    isDark: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToPdf: (Int) -> Unit,
    screenVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(isDark, borderRadius = 20.dp, alpha = 0.96f)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Accordion Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onToggleExpand() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
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

            Spacer(modifier = Modifier.width(16.dp))

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
                val pagesCountText = when {
                    pages.size == 1 -> "صفحة واحدة محفوظة"
                    pages.size == 2 -> "صفحتان محفوظتان"
                    pages.size in 3..10 -> "${pages.size} صفحات محفوظة"
                    else -> "${pages.size} صفحة محفوظة"
                }
                Text(
                    text = pagesCountText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val arrowRotation by animateFloatAsState(
                targetValue = if (isExpanded) 360f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.74f, // Moist fluid bounce
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "arrow_rotation"
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).rotate(arrowRotation)
            )
        }

        // Expanded Bookmark Rows with Moist Organic Spring
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.76f,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(220)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + fadeOut(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f), CircleShape)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sharedTransitionScope = LocalSharedTransitionScope.current

                    pages.sorted().forEach { pageIndex ->
                        val rowInteractionSource = remember { MutableInteractionSource() }
                        val rowTargetKey = "bookmark_${item.id}_$pageIndex"

                        val rowSharedModifier = if (sharedTransitionScope != null && screenVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = rowTargetKey),
                                    animatedVisibilityScope = screenVisibilityScope,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 90,
                                            delayMillis = 240,
                                            easing = LinearEasing
                                        )
                                    ),
                                    exit = fadeOut(tween(90, easing = LinearEasing)),
                                    renderInOverlayDuringTransition = false,
                                    boundsTransform = { _, _ ->
                                        spring(dampingRatio = 0.78f, stiffness = 340f)
                                    },
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop)
                                )
                            }
                        } else Modifier

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(rowSharedModifier)
                                .expressiveButtonMorph(
                                    restRadius = 14.dp,
                                    pressedRadius = 8.dp,
                                    pressedScale = 0.94f,
                                    interactionSource = rowInteractionSource,
                                    backgroundColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    borderWidth = 1.dp,
                                    borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
                                )
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
