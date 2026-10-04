@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.animation.*
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
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
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import com.Nightjar.Th3GradeLibraryKMP.theme.bounceClick
import com.Nightjar.Th3GradeLibraryKMP.theme.parseHexColor
import com.Nightjar.Th3GradeLibraryKMP.theme.popInOnInitialLoad
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.composed
import androidx.compose.ui.unit.lerp
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice

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

                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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

                // Hoist fraction so it survives ContentCard recreation during GridCells swaps
                val gridListFraction by animateFloatAsState(
                    targetValue = if (isGridView) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = 240f
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
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
                    val isMedium = screenWidthDp >= 600.dp
                    val dynamicTopPadding = if (isMedium) {
                        statusBarHeight + 64.dp
                    } else {
                        statusBarHeight + 76.dp
                    }
                    val isNavigatingBack = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack.current
                    LaunchedEffect(isNavigatingBack) {
                        if (isNavigatingBack) {
                            lazyGridState.stopScroll()
                        }
                    }
                    LazyVerticalGrid(
                        state = lazyGridState,
                        userScrollEnabled = !isNavigatingBack,
                        columns = if (isGridView) {
                            GridCells.Adaptive(minSize = 140.dp) // Shows at least 2 on phones, expands dynamically on tablets/foldables
                        } else {
                            if (isMedium) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 350.dp)
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
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = spring(dampingRatio = 0.72f, stiffness = 200f)
                                ),
                                item = item,
                                fraction = gridListFraction,
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
    var isCardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isCardVisible = true
    }
    val cardProgress by animateFloatAsState(
        targetValue = if (isCardVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = index * 90,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "categoryCardProgress_$index"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val slideOffsetPx = remember(density) { with(density) { 28.dp.toPx() } }
    val orb1Brush = remember { Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)) }
    val orb2Brush = remember { Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent)) }
    val borderBrush = remember { Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))) }

    // Background animation calculated efficiently and disabled on low-end devices
    val anim1: Float
    val anim2: Float
    if (!isLowEnd) {
        val infiniteTransition = rememberInfiniteTransition(label = "LiquidAnimation_$index")
        val a1 by infiniteTransition.animateFloat(
            initialValue = -25f,
            targetValue = 25f,
            animationSpec = infiniteRepeatable(
                animation = tween(4500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LiquidOrb1Anim"
        )
        val a2 by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LiquidOrb2Anim"
        )
        anim1 = a1
        anim2 = a2
    } else {
        anim1 = 0f
        anim2 = 0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isWide) Modifier.aspectRatio(1.6f) else Modifier.height(190.dp))
            .graphicsLayer {
                translationY = (1f - cardProgress) * slideOffsetPx
                alpha = cardProgress
                val s = 0.96f + 0.04f * cardProgress
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
                    if (!isLowEnd) {
                        val o1Radius = 96.dp.toPx()
                        drawCircle(
                            brush = orb1Brush,
                            radius = o1Radius,
                            center = Offset(anim1 + 48.dp.toPx(), anim1 + 48.dp.toPx())
                        )
                        val o2Radius = 80.dp.toPx()
                        drawCircle(
                            brush = orb2Brush,
                            radius = o2Radius,
                            center = Offset(this.size.width - 48.dp.toPx() + anim2, this.size.height - 48.dp.toPx() + anim2)
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
    fraction: Float,
    isDark: Boolean,
    index: Int = 0,
    onClick: () -> Unit
) {

    val imageModifier = Modifier.fillMaxSize()

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Layout(
        content = {
            // Child 0: Cover Image with Radial Orbs background
            Box(modifier = imageModifier) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(lerp(16.dp, 20.dp, fraction)))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-10).dp, y = (-20).dp)
                            .size(80.dp)
                            .background(Brush.radialGradient(listOf(parseHexColor(item.colorStart).copy(alpha=0.35f), Color.Transparent)), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .size(100.dp)
                            .background(Brush.radialGradient(listOf(parseHexColor(item.colorEnd).copy(alpha=0.25f), Color.Transparent)), CircleShape)
                    )
                }

                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                        .data(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                        .crossfade(true)
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent),
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(lerp(16.dp, 20.dp, fraction)))
                )
            }
            // Child 1: Title & Details Column
            Column {
                Text(
                    text = item.title,
                    fontSize = (16 + (14 - 16) * fraction).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
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
                        fontSize = (12 + (11 - 12) * fraction).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Child 2: Arrow icon (fades out in Grid view)
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f * (1f - fraction))
            )
        },
        modifier = modifier
            .zIndex(if (com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack.current) 1f else 0f)
            .fillMaxWidth()
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "card_${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            renderInOverlayDuringTransition = false,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.76f, stiffness = 220f) },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(androidx.compose.ui.layout.ContentScale.Crop)
                        )
                    }
                } else Modifier
            )
            .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.5f)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) { measurables, constraints ->
        val containerWidth = constraints.maxWidth
        val listWidthPx = 76.dp.toPx()
        // In grid mode, the image width is containerWidth. In list mode, clamp expected grid width so it never explodes across full screen
        val expectedGridWidthPx = (containerWidth.toFloat() / 2f - 8.dp.toPx()).coerceIn(130.dp.toPx(), 200.dp.toPx())
        val gridWidthPx = if (containerWidth < 280.dp.toPx()) containerWidth.toFloat() else expectedGridWidthPx
        val imgWidth = (listWidthPx + fraction * (gridWidthPx - listWidthPx)).toInt()

        val listHeightPx = 96.dp.toPx()
        // In grid mode, the aspect ratio is 0.8 (height = width * 1.25)
        val gridHeightPx = gridWidthPx * 1.25f
        val imgHeight = (listHeightPx + fraction * (gridHeightPx - listHeightPx)).toInt()

        // Measure image
        val imagePlaceable = measurables[0].measure(
            Constraints.fixed(imgWidth, imgHeight)
        )

        // Measure text column
        // In list mode: width is containerWidth - imageWidth - spacing - arrowWidth - padding
        val listTextWidth = (containerWidth - listWidthPx - 40.dp.toPx()).toInt()
        val gridTextWidth = containerWidth
        val textWidth = (listTextWidth + fraction * (gridTextWidth - listTextWidth)).toInt()
        val textPlaceable = measurables[1].measure(
            Constraints(
                minWidth = 0,
                maxWidth = textWidth.coerceAtLeast(0),
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
        )

        // Measure arrow
        val arrowPlaceable = measurables[2].measure(Constraints())

        // Calculate layout height
        val listLayoutHeight = 96.dp.toPx().toInt() // Fixed image height in list mode
        val gridLayoutHeight = imgHeight + 10.dp.toPx().toInt() + textPlaceable.height
        val layoutHeight = (listLayoutHeight + fraction * (gridLayoutHeight - listLayoutHeight)).toInt()

        layout(containerWidth, layoutHeight) {
            // Place cover image at (0, 0)
            imagePlaceable.placeRelative(0, 0)

            // Place text column
            // In list mode: to the right of image, vertically centered
            val listTextX = (76.dp.toPx() + 16.dp.toPx()).toInt()
            val listTextY = (layoutHeight - textPlaceable.height) / 2
            
            // In grid mode: below image
            val gridTextX = 0
            val gridTextY = imgHeight + 10.dp.toPx().toInt()

            val textX = (listTextX + fraction * (gridTextX - listTextX)).toInt()
            val textY = (listTextY + fraction * (gridTextY - listTextY)).toInt()
            textPlaceable.placeRelative(textX, textY)

            // Place arrow icon (only visible in list mode, at the far right)
            val arrowX = containerWidth - arrowPlaceable.width
            val arrowY = (layoutHeight - arrowPlaceable.height) / 2
            arrowPlaceable.placeRelative(arrowX, arrowY)
        }
    }
}

