@file:OptIn(
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class
)
package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.Nightjar.Th3GradeLibraryKMP.LocalSharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.LocalAnimatedVisibilityScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import coil3.request.crossfade
import coil3.request.CachePolicy
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import com.Nightjar.Th3GradeLibraryKMP.theme.bounceClick
import com.Nightjar.Th3GradeLibraryKMP.theme.parseHexColor
import com.Nightjar.Th3GradeLibraryKMP.theme.popInOnInitialLoad
import com.Nightjar.Th3GradeLibraryKMP.theme.screenEntranceStagger
import com.Nightjar.Th3GradeLibraryKMP.theme.screenEntranceCascade
import com.Nightjar.Th3GradeLibraryKMP.theme.adaptiveItemEntrance
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.ui.composed
import androidx.compose.ui.unit.lerp
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice
import androidx.compose.ui.draw.drawBehind
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalScreenEntranceKey
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsReturningFromPdf

@Composable
fun LibraryScreen(
    category: String?, // "books", "notes" or null for categories choice
    isGridView: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCategory: (String?) -> Unit,
    onNavigateToPdf: (BookItem) -> Unit,
    isDark: Boolean,
    onToggleViewMode: () -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onVoiceSearchTrigger: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    onScrollableStateChanged: (Boolean) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState(),
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val widthClass = windowSizeClass.windowWidthSizeClass
    val heightClass = windowSizeClass.windowHeightSizeClass

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val responsiveInfo = com.Nightjar.Th3GradeLibraryKMP.theme.LocalResponsiveLayoutInfo.current
        val isWide = widthClass != WindowWidthSizeClass.COMPACT
        val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
        Column(modifier = Modifier.fillMaxSize()) {
            if (category == null) {
                // Home/Categories view
                // Uses hoisted scrollState
                val canScroll by remember {
                    derivedStateOf {
                        scrollState.maxValue > 0 && scrollState.maxValue < Int.MAX_VALUE
                    }
                }
                LaunchedEffect(canScroll) {
                    onScrollableStateChanged(canScroll)
                }

                val dynamicTopPadding = when { responsiveInfo.isPhoneLandscape -> statusBarHeight + 44.dp; isWide -> statusBarHeight + 72.dp; else -> statusBarHeight + 58.dp }
                    
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 800.dp)
                            .padding(horizontal = 16.dp)
                            .elasticOverscroll()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(dynamicTopPadding))
                        Text(
                            text = "دراستك أسهل\nفي مكان واحد.",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 40.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CategoryMenuCard(
                                    modifier = Modifier.popInOnInitialLoad(0),
                                    title = "الملازم الدراسية",
                                    desc = "أفضل الملخصات لأساتذة العراق",
                                    brush = Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF059669))),
                                    icon = Icons.Default.Description,
                                    index = 0,
                                    onClick = { onNavigateToCategory("notes") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CategoryMenuCard(
                                    modifier = Modifier.popInOnInitialLoad(1),
                                    title = "الكتب الرسمية",
                                    desc = "المنهج الوزاري المعتمد 2027",
                                    brush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF1D4ED8))),
                                    icon = Icons.Default.Book,
                                    index = 1,
                                    onClick = { onNavigateToCategory("books") }
                                )
                            }
                        }
                    } else {
                        CategoryMenuCard(
                            modifier = Modifier.popInOnInitialLoad(0),
                            title = "الملازم الدراسية",
                            desc = "أفضل الملخصات لأساتذة العراق",
                            brush = Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF059669))),
                            icon = Icons.Default.Description,
                            index = 0,
                            onClick = { onNavigateToCategory("notes") }
                        )

                        CategoryMenuCard(
                            modifier = Modifier.popInOnInitialLoad(1),
                            title = "الكتب الرسمية",
                            desc = "المنهج الوزاري المعتمد 2027",
                            brush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF1D4ED8))),
                            icon = Icons.Default.Book,
                            index = 1,
                            onClick = { onNavigateToCategory("books") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(bottomPadding)) // Allow scrolling bottom content to middle
                }
                }
            } else {
                // Category contents list/grid
                val allList = if (category == "books") AllItems.books else AllItems.notes
                val filteredList = remember(searchQuery, category) {
                    if (searchQuery.isBlank()) {
                        allList
                    } else {
                        allList.filter {
                            it.title.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

                // Uses hoisted lazyGridState
                val canScroll by remember {
                    derivedStateOf {
                        lazyGridState.canScrollForward || lazyGridState.canScrollBackward
                    }
                }
                LaunchedEffect(canScroll) {
                    onScrollableStateChanged(canScroll)
                }

                LaunchedEffect(lazyGridState) {
                    androidx.compose.runtime.snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
                        TransitionTargetTracker.visibleItemIds = visibleItems.mapNotNull { it.key as? String }.toSet()
                    }
                }



                val gridListFraction by animateFloatAsState(
                    targetValue = if (isGridView) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.78f, // Fluid moist curve
                        stiffness = 320f
                    ),
                    label = "global_grid_list_fraction"
                )

                if (filteredList.isEmpty()) {
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
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("لم يتم العثور على نتائج", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
                    val dynamicTopPadding = when { responsiveInfo.isPhoneLandscape -> statusBarHeight + 44.dp; isWide -> statusBarHeight + 72.dp; else -> statusBarHeight + 58.dp }
                    val dynamicGridColumns = remember(widthClass, heightClass, screenWidthDp, isGridView) {
                        if (isGridView) {
                            when (widthClass) {
                                WindowWidthSizeClass.COMPACT -> {
                                    // Official Material 3 Compact: Micro window (<340dp) -> 1 column; Standard phone -> 2 columns
                                    if (screenWidthDp < 340.dp) {
                                        GridCells.Fixed(1)
                                    } else {
                                        GridCells.Fixed(2)
                                    }
                                }
                                WindowWidthSizeClass.MEDIUM -> {
                                    // Official Material 3 Medium (600dp - 840dp):
                                    // Compact height indicates mobile phone in landscape orientation -> 4 columns!
                                    // Medium/Expanded height indicates tablet in portrait -> 3 columns!
                                    if (heightClass == WindowHeightSizeClass.COMPACT) {
                                        GridCells.Fixed(4)
                                    } else {
                                        GridCells.Fixed(3)
                                    }
                                }
                                WindowWidthSizeClass.EXPANDED -> {
                                    // Official Material 3 Expanded (>= 840dp):
                                    // Large screens, Samsung Galaxy Tab 12 Pro, Googlebook OS laptops, DeX
                                    when {
                                        screenWidthDp >= 1800.dp -> GridCells.Fixed(8) // Ultra-wide 4K / Dual monitor / Samsung DeX
                                        screenWidthDp >= 1440.dp -> GridCells.Fixed(6) // Googlebook 15-16" / High-end desktop
                                        screenWidthDp >= 1080.dp -> GridCells.Fixed(5) // Samsung Galaxy Tab 12 Pro / Googlebook 13-14"
                                        else                     -> GridCells.Fixed(4) // Standard Tablet landscape (840dp - 1080dp)
                                    }
                                }
                                else -> GridCells.Fixed(2)
                            }
                        } else {
                            when (widthClass) {
                                WindowWidthSizeClass.COMPACT -> GridCells.Fixed(1)
                                WindowWidthSizeClass.MEDIUM -> GridCells.Fixed(2)
                                WindowWidthSizeClass.EXPANDED -> if (screenWidthDp >= 1400.dp) GridCells.Fixed(3) else GridCells.Fixed(2)
                                else -> GridCells.Fixed(1)
                            }
                        }
                    }

                LazyVerticalGrid(
                    state = lazyGridState,
                    userScrollEnabled = true,
                    columns = dynamicGridColumns,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .elasticOverscroll(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(if (isGridView) 16.dp else 12.dp),
                    contentPadding = PaddingValues(
                        top = dynamicTopPadding, 
                        bottom = bottomPadding
                    )
                ) {
                    itemsIndexed(items = filteredList, key = { _, item -> item.id }, contentType = { _, _ -> "library_item" }) { index, item ->
                        val isReturning = LocalIsReturningFromPdf.current
                        ContentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .adaptiveItemEntrance(index, item.id)
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = if (isReturning) null else spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 300f
                                    )
                                ),
                            item = item,
                            isGridView = isGridView,
                            isDark = isDark,
                            index = index,
                            onClick = { onNavigateToPdf(item) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun CategoryMenuCard(
    title: String,
    desc: String,
    brush: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    index: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
    val isWide = screenWidthDp >= 600.dp
    val isLowEnd = LocalIsLowEndDevice.current
    val isReturningFromPdf = LocalIsReturningFromPdf.current
    val screenKey = LocalScreenEntranceKey.current

    val orb1Brush = remember { Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)) }
    val orb2Brush = remember { Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent)) }
    val borderBrush = remember { Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val p48 = remember(density) { with(density) { 48.dp.toPx() } }
    val p96 = remember(density) { with(density) { 96.dp.toPx() } }
    val p80 = remember(density) { with(density) { 80.dp.toPx() } }

    val animOrb1: State<Float>?
    val animOrb2: State<Float>?
    if (!isLowEnd) {
        val infiniteTransition = rememberInfiniteTransition(label = "LiquidAnimation_$index")
        animOrb1 = infiniteTransition.animateFloat(
            initialValue = -25f,
            targetValue = 25f,
            animationSpec = infiniteRepeatable(
                animation = tween(4500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LiquidOrb1Anim"
        )
        animOrb2 = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LiquidOrb2Anim"
        )
    } else {
        animOrb1 = null
        animOrb2 = null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isWide) Modifier.aspectRatio(1.6f) else if (screenWidthDp < 360.dp) Modifier.height(150.dp) else Modifier.height(190.dp))
            .graphicsLayer {
                ambientShadowColor = Color.Black.copy(alpha = 0.5f)
                spotShadowColor = Color.Black.copy(alpha = 0.5f)
            }
            .clip(RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .background(brush)
                .border(
                    width = 1.5.dp, 
                    brush = borderBrush, 
                    shape = RoundedCornerShape(32.dp)
                )
                .drawBehind {
                    if (!isLowEnd && animOrb1 != null && animOrb2 != null) {
                        val offset1 = animOrb1.value
                        val offset2 = animOrb2.value
                        drawCircle(
                            brush = orb1Brush,
                            radius = p96,
                            center = Offset(offset1 + p48, offset1 + p48)
                        )
                        drawCircle(
                            brush = orb2Brush,
                            radius = p80,
                            center = Offset(size.width - p48 + offset2, size.height - p48 + offset2)
                        )
                    }
                }
                .bounceClick { onClick() }
        ) {
            // Floating background icon
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp)
                    .graphicsLayer {
                        alpha = 0.15f
                        scaleX = 3f
                        scaleY = 3f
                    }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            val cardPadding = if (screenWidthDp < 360.dp) 16.dp else 28.dp
            Column(
                modifier = Modifier.fillMaxSize().padding(cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (screenWidthDp < 360.dp) 20.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.2f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // mb-2
                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.2f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyGridItemScope.ContentCard(
    modifier: Modifier = Modifier,
    item: BookItem,
    isGridView: Boolean,
    isDark: Boolean,
    index: Int = 0,
    onClick: () -> Unit
) {
    val cardCornerRadius by animateDpAsState(
        targetValue = if (isGridView) 20.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 300f),
        label = "cardCornerRadius"
    )
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val isReturningFromPdf = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsReturningFromPdf.current
    val isThisCardTarget = (TransitionTargetTracker.activeTargetId == "card_${item.id}" || TransitionTargetTracker.activeTargetId == item.id)

    val sharedBoundsModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card_${item.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 150,
                        delayMillis = 75,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 130,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                renderInOverlayDuringTransition = true,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(cardCornerRadius)),
                boundsTransform = { _, _ -> spring(dampingRatio = 0.82f, stiffness = 280f) },
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .zIndex(if (isThisCardTarget) 15f else if (com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack.current) 1f else 0f)
            .fillMaxWidth()
            .then(sharedBoundsModifier)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = 340f
                )
            )
            .liquidGlass(isDark, borderRadius = cardCornerRadius, alpha = 0.5f)
            .clip(RoundedCornerShape(cardCornerRadius))
            .clickable {
                TransitionTargetTracker.activeTargetId = "card_${item.id}"
                TransitionTargetTracker.isTransitionActive = true
                onClick()
            }
            .padding(12.dp)
    ) {
        if (isGridView) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        coil3.compose.AsyncImage(
                            model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                                .data(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                                .crossfade(!isReturningFromPdf)
                                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            parseHexColor(item.colorStart),
                                            parseHexColor(item.colorEnd)
                                        )
                                    )
                                )
                        )
                        Text(
                            text = item.author,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        coil3.compose.AsyncImage(
                            model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                                .data(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                                .crossfade(!isReturningFromPdf)
                                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            parseHexColor(item.colorStart),
                                            parseHexColor(item.colorEnd)
                                        )
                                    )
                                )
                            )
                            Text(
                                text = item.author,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }


