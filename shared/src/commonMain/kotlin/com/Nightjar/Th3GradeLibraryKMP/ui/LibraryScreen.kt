@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                    val dynamicBottomPadding = bottomPadding
                    LazyVerticalGrid(
                        state = lazyGridState,
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
                            bottom = dynamicBottomPadding
                        )
                    ) {
                        itemsIndexed(items = filteredList, key = { _, item -> item.id }, contentType = { _, _ -> "library_item" }) { index, item ->
                            ContentCard(
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
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(if (hasAnimated) 1f else 0f) }
    val yOffsetAnim = remember { androidx.compose.animation.core.Animatable(if (hasAnimated) 0f else 50f) }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            val delayMs = index * 100L
            kotlinx.coroutines.delay(delayMs)
            launch {
                alphaAnim.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
            }
            launch {
                yOffsetAnim.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
            }
            hasAnimated = true
        }
    }

    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    val infiniteTransition = rememberInfiniteTransition(label = "cardOrbs")
    val orbScale1 by if (!isLowEnd) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(animation = tween(3000), repeatMode = RepeatMode.Reverse),
            label = "orb1"
        )
    } else remember { mutableStateOf(1f) }

    val orbScale2 by if (!isLowEnd) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(animation = tween(4000, delayMillis = 500), repeatMode = RepeatMode.Reverse),
            label = "orb2"
        )
    } else remember { mutableStateOf(1f) }

    val floatY by if (!isLowEnd) {
        infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "floatIcon"
        )
    } else remember { mutableStateOf(0f) }

    val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
    val isWide = screenWidthDp >= 600.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isWide) Modifier.aspectRatio(1.6f) else Modifier.height(190.dp))
            .graphicsLayer {
                alpha = alphaAnim.value
                translationY = yOffsetAnim.value
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
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))), 
                    shape = RoundedCornerShape(32.dp)
                )
                .bounceClick { onClick() }
        ) {
            if (!isLowEnd) {
                // Liquid Orb 1 (Top Left, White)
                Box(
                    modifier = Modifier
                        .offset(x = (-48).dp, y = (-48).dp)
                        .size(192.dp)
                        .graphicsLayer { 
                            scaleX = orbScale1
                            scaleY = orbScale1 
                            translationX = orbScale1 * 20f
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                // Liquid Orb 2 (Bottom Right, Dark)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 32.dp, y = 32.dp)
                        .size(160.dp)
                        .graphicsLayer { 
                            scaleX = orbScale2
                            scaleY = orbScale2 
                            translationX = -orbScale2 * 20f
                            translationY = orbScale2 * 10f
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Floating background icon
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp)
                    .graphicsLayer {
                        translationY = floatY
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
    item: BookItem,
    isGridView: Boolean,
    isDark: Boolean,
    index: Int = 0,
    onClick: () -> Unit
) {

    val imageModifier = Modifier.fillMaxSize()

    // Animate list/grid fraction smoothly with physical spring dynamics
    val fraction by animateFloatAsState(
        targetValue = if (isGridView) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "grid_list_fraction"
    )

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Layout(
        content = {
            // Child 0: Cover Image with Radial Orbs background
            Box(modifier = imageModifier) {
                // Animated Radial Orbs (Replaced expensive blur with Brush.radialGradient)
                val infiniteTransition = rememberInfiniteTransition()
                val orbScale1 by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(animation = tween(2500), repeatMode = RepeatMode.Reverse)
                )
                val orbScale2 by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(animation = tween(3500, delayMillis = 500), repeatMode = RepeatMode.Reverse)
                )

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
                            .scale(orbScale1)
                            .background(Brush.radialGradient(listOf(parseHexColor(item.colorStart).copy(alpha=0.4f), Color.Transparent)), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .size(100.dp)
                            .scale(orbScale2)
                            .background(Brush.radialGradient(listOf(parseHexColor(item.colorEnd).copy(alpha=0.3f), Color.Transparent)), CircleShape)
                    )
                }

                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                        .data(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                        .crossfade(true)
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
                Text(
                    text = item.author,
                    fontSize = (12 + (11 - 12) * fraction).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            // Child 2: Indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
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
            // Child 3: Arrow icon (fades out in Grid view)
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f * (1f - fraction))
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "card_${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            renderInOverlayDuringTransition = false,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.85f, stiffness = 320f) }
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
        // In grid mode, the image width is containerWidth - padding
        val gridWidthPx = containerWidth.toFloat()
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

        // Measure dot
        val dotPlaceable = measurables[2].measure(Constraints())

        // Measure arrow
        val arrowPlaceable = measurables[3].measure(Constraints())

        // Calculate layout height
        val listLayoutHeight = 96.dp.toPx().toInt() // Fixed image height in list mode
        val gridLayoutHeight = imgHeight + 12.dp.toPx().toInt() + textPlaceable.height + 8.dp.toPx().toInt() + dotPlaceable.height
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
            val gridTextY = imgHeight + 12.dp.toPx().toInt()

            val textX = (listTextX + fraction * (gridTextX - listTextX)).toInt()
            val textY = (listTextY + fraction * (gridTextY - listTextY)).toInt()
            textPlaceable.placeRelative(textX, textY)

            // Place indicator dot
            // In list mode: to the right of author, below title or right after it.
            // Let's place it aligned with the text column
            val listDotX = listTextX
            val listDotY = listTextY + textPlaceable.height + 8.dp.toPx().toInt()

            // In grid mode: below author
            val gridDotX = 0
            val gridDotY = gridTextY + textPlaceable.height + 8.dp.toPx().toInt()

            val dotX = (listDotX + fraction * (gridDotX - listDotX)).toInt()
            val dotY = (listDotY + fraction * (gridDotY - listDotY)).toInt()
            dotPlaceable.placeRelative(dotX, dotY)

            // Place arrow icon (only visible in list mode, at the far right)
            val arrowX = containerWidth - arrowPlaceable.width
            val arrowY = (layoutHeight - arrowPlaceable.height) / 2
            arrowPlaceable.placeRelative(arrowX, arrowY)
        }
    }
}

