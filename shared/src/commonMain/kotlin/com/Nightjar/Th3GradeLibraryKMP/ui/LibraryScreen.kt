@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.animation.*
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearOutSlowInEasing
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
        val isWide = screenWidthDp >= 600.dp
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

                    val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
                    val dynamicTopPadding = if (isWide) statusBarHeight + 64.dp else statusBarHeight + 76.dp
                    
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
                            title = "الملازم الدراسية",
                            desc = "أفضل الملخصات لأساتذة العراق",
                            brush = Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF059669))),
                            icon = Icons.Default.Description,
                            index = 0,
                            onClick = { onNavigateToCategory("notes") }
                        )

                        CategoryMenuCard(
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
                    val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
                    val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
                    val isMedium = screenWidthDp >= 600.dp
                    val dynamicTopPadding = if (isMedium) {
                        statusBarHeight + 64.dp
                    } else {
                        statusBarHeight + 76.dp
                    }
                LazyVerticalGrid(
                    state = lazyGridState,
                    userScrollEnabled = true,
                    columns = if (isGridView) {
                        GridCells.Adaptive(minSize = 140.dp)
                    } else {
                        GridCells.Adaptive(minSize = 340.dp)
                    },
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
                        ContentCard(
                            modifier = Modifier
                                .animateItem(
                                    placementSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 300f
                                    )
                                )
                                .adaptiveItemEntrance(index, itemId = item.id),
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

    var isCardVisible by remember(screenKey) { mutableStateOf(isReturningFromPdf) }
    LaunchedEffect(screenKey) {
        isCardVisible = true
    }
    val cardProgress by animateFloatAsState(
        targetValue = if (isCardVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = if (isReturningFromPdf) 0 else index * 90,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "categoryCardProgress_$index"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val slideOffsetPx = remember(density) { with(density) { 28.dp.toPx() } }
    val orb1Brush = remember { Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)) }
    val orb2Brush = remember { Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent)) }
    val borderBrush = remember { Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))) }

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
            .then(if (isWide) Modifier.aspectRatio(1.6f) else Modifier.height(190.dp))
            .graphicsLayer {
                translationY = (1f - cardProgress) * slideOffsetPx
                alpha = if (isReturningFromPdf) 1f else (0.85f + 0.15f * cardProgress)
                val s = 0.98f + 0.02f * cardProgress
                scaleX = s
                scaleY = s
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

            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
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
                        fontSize = 28.sp,
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
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 100,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                renderInOverlayDuringTransition = true,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(cardCornerRadius)),
                boundsTransform = { _, _ -> spring(dampingRatio = 0.80f, stiffness = 300f) },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(androidx.compose.ui.layout.ContentScale.Crop)
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


