@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import com.Nightjar.Th3GradeLibraryKMP.LocalSharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.LocalAnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.zIndex
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.Nightjar.Th3GradeLibraryKMP.theme.popInOnInitialLoad
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Brush
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import kotlin.math.abs
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewsScreen(
    onOpenSettings: () -> Unit,
    onClick: (String, String) -> Unit,
    isDark: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    onScrollableStateChanged: (Boolean) -> Unit = {},
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    val newsList by SyncEngine.newsList.collectAsState()
    val readNewsIds by SyncEngine.readNewsIds.collectAsState()
    val syncing by SyncEngine.syncing.collectAsState()
    val lastFetchCount by SyncEngine.lastFetchedCount.collectAsState()
    
    val seenIds = remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    // Show fetch result banner
    var showFetchBanner by remember { mutableStateOf(false) }
    var fetchBannerText by remember { mutableStateOf("") }

    val isOnline by SyncEngine.isOnline.collectAsState()
    var networkBannerState by remember { mutableStateOf<NetworkBannerState?>(null) }
    var firstCheckDone by remember { mutableStateOf(false) }

    var isInitialLoad by remember { mutableStateOf(true) }
    LaunchedEffect(syncing) {
        if (!syncing && isInitialLoad) {
            isInitialLoad = false
        }
    }

    LaunchedEffect(isOnline) {
        if (!firstCheckDone) {
            firstCheckDone = true
        } else {
            networkBannerState = if (isOnline) NetworkBannerState.ONLINE else NetworkBannerState.OFFLINE
        }
    }

    LaunchedEffect(networkBannerState) {
        if (networkBannerState != null) {
            kotlinx.coroutines.delay(3000)
            networkBannerState = null
        }
    }

    var pendingManualToast by remember { mutableStateOf(false) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(lastFetchCount) {
        if (lastFetchCount != -1) {
            isManualRefreshing = false // Fix stuck spinner
            if (pendingManualToast) {
                when (lastFetchCount) {
                    0 -> ToastManager.showToast("لا توجد أخبار جديدة حالياً")
                    in 1..Int.MAX_VALUE -> ToastManager.showToast("تم جلب $lastFetchCount خبر جديد")
                    -2 -> ToastManager.showToast("فشل الاتصال، يرجى المحاولة لاحقاً")
                }
                pendingManualToast = false
            }
            SyncEngine.clearLastFetchedCount()
        }
    }
    val notificationsEnabled by SyncEngine.notificationsEnabled.collectAsState()
    val appSettings by SyncEngine.appSettings.collectAsState()
    
    // Automatic syncing on opening the screen is removed as per requirements.
    // Sync is now strictly manual via Pull-to-Refresh.
    var isNotificationWarningDismissed by remember { mutableStateOf(false) }

    // Undo deletion states
    var recentlyDeletedItem by remember { mutableStateOf<NewsItem?>(null) }
    var recentlyDeletedIndex by remember { mutableStateOf(-1) }
    var showUndoBanner by remember { mutableStateOf(false) }
    
    var bannerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(showUndoBanner) {
        if (showUndoBanner) {
            kotlinx.coroutines.delay(5000)
            showUndoBanner = false
        }
    }

    val isNavigatingBack = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack.current
    LaunchedEffect(isNavigatingBack) {
        if (isNavigatingBack) {
            lazyGridState.stopScroll()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pullRefreshState = rememberPullToRefreshState()
        // Uses hoisted lazyListState
        val canScroll by remember {
            derivedStateOf {
                lazyGridState.canScrollForward || lazyGridState.canScrollBackward
            }
        }
                LaunchedEffect(canScroll) {
                    onScrollableStateChanged(canScroll)
                }
                LaunchedEffect(newsList.isEmpty(), syncing) {
                    if (newsList.isEmpty() && !syncing) {
                        onScrollableStateChanged(false)
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isManualRefreshing,
                    onRefresh = {
                        if (!isManualRefreshing && !showFetchBanner) {
                            if (!isOnline) {
                                ToastManager.showToast("تعذر المزامنة: تحقق من اتصال الإنترنت")
                            } else {
                                isManualRefreshing = true
                                pendingManualToast = true
                                scope.launch { 
                                    SyncEngine.fetchAndSync(forced = true)
                                }
                            }
                        }
                    },
                    state = pullRefreshState,
                    indicator = {
                        val isPulling = pullRefreshState.distanceFraction > 0f
                        if (isPulling || isManualRefreshing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 100.dp) // Start safely below the Top App Bar
                                    .offset {
                                        // Move down as the user pulls
                                        androidx.compose.ui.unit.IntOffset(0, (pullRefreshState.distanceFraction * 120f).roundToInt())
                                    }
                                    .scale(if (isManualRefreshing) 1f else pullRefreshState.distanceFraction.coerceIn(0.5f, 1f))
                                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape)
                                    .padding(8.dp)
                            ) {
                                ExpressiveLoadingIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
                    val isMedium = screenWidthDp >= 600.dp
                    LazyVerticalGrid(
                        columns = if (isMedium) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 340.dp),
                        state = lazyGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                            .elasticOverscroll(topEnabled = false),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 0.dp, bottom = bottomPadding)
                    ) {
                        item(key = "top_spacer", contentType = "spacer", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                            val dynamicTopPadding = if (isMedium) statusBarHeight + 64.dp else statusBarHeight + 76.dp
                            Spacer(modifier = Modifier.height(dynamicTopPadding))
                        }
                        
                        if (newsList.isEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp).padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (syncing) {
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .liquidGlass(isDark, borderRadius = 24.dp, alpha = 0.8f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ExpressiveLoadingIndicator(
                                                modifier = Modifier.size(36.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                                .padding(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Newspaper,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("آخر الأخبار لك", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "أحدث الأخبار العاجلة والتبليغات الوزارية الرسمية تصلك أولاً بأول، اسحب للأسفل للتحديث.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(newsList, key = { _, it -> it.id }, contentType = { _, _ -> "news_item" }) { index, newsItem ->
                                NewsCardSwipeable(
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = tween(500),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            fadeOutSpec = tween(200)
                                        )
                                        .popInOnInitialLoad(index),
                                    news = newsItem,
                                    isDark = isDark,
                                    isRead = readNewsIds.contains(newsItem.id) || (newsItem.link.isNotEmpty() && readNewsIds.contains(newsItem.link)),
                                    onClick = {
                                        SyncEngine.markNewsAsRead(newsItem.id)
                                        if (newsItem.link.isNotEmpty()) {
                                            SyncEngine.markNewsAsRead(newsItem.link)
                                        }
                                        onClick(newsItem.link, newsItem.title)
                                    },
                                    onDelete = {
                                        val delIndex = newsList.indexOf(newsItem)
                                        if (delIndex >= 0) {
                                            recentlyDeletedItem = newsItem
                                            recentlyDeletedIndex = delIndex
                                            SyncEngine.deleteNewsItem(newsItem.id)
                                            showUndoBanner = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

        androidx.compose.animation.AnimatedVisibility(
            visible = !notificationsEnabled && !isNotificationWarningDismissed,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { -it }
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { -it }
            ) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 64.dp) // Starts below the compact Top App Bar
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .zIndex(10f)
        ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { isNotificationWarningDismissed = true }
                        ) { change, _ -> change.consume() }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bell icon with badge
                        Box {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "الإشعارات معطلة",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp
                            )
                            Text(
                                "فعّل لتصلك أخبار عاجلة فور نشرها",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = {
                                SyncEngine.platformActionHandler?.openNotificationSettings()
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                "تفعيل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

        // Fetch result banner (bottom)
        AnimatedVisibility(
            visible = showFetchBanner,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = when (lastFetchCount) {
                            0 -> MaterialTheme.colorScheme.surfaceVariant
                            -2 -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = fetchBannerText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (lastFetchCount) {
                        0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        -2 -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }

        // Undo Deletion Banner (bottom)
        androidx.compose.animation.AnimatedVisibility(
            visible = showUndoBanner,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 8.dp) // Float exactly above bottom capsule
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { showUndoBanner = false }
                    ) { change, _ -> change.consume() }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تم حذف الخبر بنجاح",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = {
                            if (recentlyDeletedItem != null && recentlyDeletedIndex >= 0) {
                                SyncEngine.restoreNewsItem(recentlyDeletedItem!!, recentlyDeletedIndex)
                            }
                            showUndoBanner = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                    ) {
                        Text("تراجع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Network status banner overlay
        NetworkStatusBanner(
            state = networkBannerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = bottomPadding + 24.dp)
                .zIndex(15f)
        )
    }
}

/**
 * Card with swipe-to-delete
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NewsCardSwipeable(
    news: NewsItem,
    isDark: Boolean,
    isRead: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX, 
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val deleteThreshold = with(LocalDensity.current) { 120.dp.toPx() }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    var cardModifier = Modifier
        .graphicsLayer { translationX = animatedOffset }
        .clip(RoundedCornerShape(24.dp))

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            cardModifier = cardModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "news-${news.link}"),
                animatedVisibilityScope = animatedVisibilityScope, renderInOverlayDuringTransition = false
            )
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Delete background
        val progress = (abs(offsetX) / deleteThreshold).coerceIn(0f, 1f)
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = progress)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart // Start is RIGHT in RTL. Card moves left, revealing right side.
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = progress),
                    modifier = Modifier.size(26.dp).scale(0.5f + (progress * 0.5f))
                )
            }
        }

        NewsCard(
            news = news,
            isDark = isDark,
            isRead = isRead,
            modifier = cardModifier
                .clickable(onClick = onClick)
                .pointerInput(news.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            if (abs(offsetX) > deleteThreshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelete()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Swipe left = negative dragAmount.x (moves card left)
                            offsetX = (offsetX + dragAmount.x).coerceIn(-deleteThreshold * 1.5f, 0f)
                        }
                    )
                }
        )
    }
}

@Composable
fun NewsCard(
    news: NewsItem,
    isDark: Boolean,
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    val cleanDate = news.formattedDate.ifEmpty { formatNewsDate(news.pubDate) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.65f else 0.85f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(8.dp)
    ) {
        // Image container with floating Date/Read badges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            // 1. Persistent Vector Placeholder in background (visible while loading, offline, or if article has no image)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = "صورة الخبر",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "مكتبة الثالث متوسط",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 2. Real Image rendered with smooth Fluid crossfade animation over the placeholder
            if (!news.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(news.imageUrl)
                        .crossfade(true)
                        .crossfade(450)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top gradient scrim to guarantee crisp badge readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )

            // Date badge and Read status row floating on top of the image
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Date Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cleanDate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Read Status Badge
                if (isRead) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "مقروء ✓",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // News title text placed below the image
        Text(
            text = news.title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatNewsDate(pubDate: String): String = try {
    val dParts = pubDate.split(" ")
    if (dParts.size >= 5) {
        val day = dParts[1]
        val month = when (dParts[2].lowercase()) {
            "jan" -> "1"; "feb" -> "2"; "mar" -> "3"; "apr" -> "4"
            "may" -> "5"; "jun" -> "6"; "jul" -> "7"; "aug" -> "8"
            "sep" -> "9"; "oct" -> "10"; "nov" -> "11"; "dec" -> "12"
            else -> "1"
        }
        val year = dParts[3]
        val timeParts = dParts[4].split(":")
        var hour = timeParts[0].toIntOrNull() ?: 12
        val min = timeParts[1]
        
        // تعديل التوقيت ليكون بتوقيت العراق (+3)
        hour += 3
        if (hour >= 24) hour -= 24
        
        val amPm = if (hour >= 12) "م" else "ص"
        if (hour > 12) hour -= 12
        if (hour == 0) hour = 12
        "$day-$month-$year  $hour:$min $amPm"
    } else pubDate
} catch (e: Exception) { pubDate }

enum class NetworkBannerState {
    ONLINE, OFFLINE
}

@Composable
fun NetworkStatusBanner(
    state: NetworkBannerState?,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = state != null,
        enter = (androidx.compose.animation.slideInVertically(
            initialOffsetY = { (it * 0.7f).toInt() },
            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
        ) + androidx.compose.animation.scaleIn(
            initialScale = 0.88f,
            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
        ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))),
        exit = (androidx.compose.animation.slideOutVertically(
            targetOffsetY = { (it * 1.3f).toInt() },
            animationSpec = androidx.compose.animation.core.tween(170, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        ) + androidx.compose.animation.scaleOut(
            targetScale = 0.80f,
            animationSpec = androidx.compose.animation.core.tween(170, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(130))),
        modifier = modifier
    ) {
        val safeState = state ?: NetworkBannerState.OFFLINE
        val backgroundColor = if (safeState == NetworkBannerState.ONLINE) androidx.compose.ui.graphics.Color(0xFF10B981) else androidx.compose.ui.graphics.Color(0xFFEF4444)
        val text = if (safeState == NetworkBannerState.ONLINE) "تمت استعادة الاتصال" else "أنت غير متصل بالإنترنت"
        val icon = if (safeState == NetworkBannerState.ONLINE) androidx.compose.material.icons.Icons.Default.Done else androidx.compose.material.icons.Icons.Default.Warning
        
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 16.dp)
                .animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f), alignment = Alignment.Center)
                .background(backgroundColor, RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

