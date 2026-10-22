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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val allItemsList = AllItems.books + AllItems.notes

    val canScroll by remember {
        derivedStateOf {
            lazyGridState.canScrollForward || lazyGridState.canScrollBackward
        }
    }
    LaunchedEffect(canScroll) {
        onScrollableStateChanged(canScroll)
    }

    val responsiveInfo = com.Nightjar.Th3GradeLibraryKMP.theme.LocalResponsiveLayoutInfo.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = responsiveInfo.isWide
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
            val dynamicTopPadding = when { responsiveInfo.isPhoneLandscape -> statusBarHeight + 44.dp; isWide -> statusBarHeight + 72.dp; else -> statusBarHeight + 58.dp }

            val isNavigatingBack = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack.current
            LaunchedEffect(isNavigatingBack) {
                if (isNavigatingBack) {
                    lazyGridState.stopScroll()
                }
            }

            val dynamicShortcutColumns = remember(responsiveInfo) {
                if (!responsiveInfo.isWide) {
                    if (responsiveInfo.width < 340.dp) {
                        GridCells.Fixed(1)
                    } else {
                        GridCells.Fixed(2)
                    }
                } else if (responsiveInfo.height < 480.dp) {
                    GridCells.Fixed(4)
                } else if (responsiveInfo.width >= 1800.dp) {
                    GridCells.Fixed(8)
                } else if (responsiveInfo.width >= 1440.dp) {
                    GridCells.Fixed(6)
                } else if (responsiveInfo.width >= 1080.dp) {
                    GridCells.Fixed(5)
                } else {
                    GridCells.Fixed(4)
                }
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

    Column(
        modifier = modifier
            .fillMaxWidth()
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
            coil3.compose.AsyncImage(
                model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                    .data(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
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

