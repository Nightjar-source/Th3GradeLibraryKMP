@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.Nightjar.gradeiraqi3library.ui

import com.Nightjar.gradeiraqi3library.LocalSharedTransitionScope
import com.Nightjar.gradeiraqi3library.LocalAnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.gradeiraqi3library.data.BookItem
import coil3.request.crossfade

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.Nightjar.gradeiraqi3library.data.AllItems
import org.jetbrains.compose.resources.painterResource
import com.Nightjar.gradeiraqi3library.theme.liquidGlass
import com.Nightjar.gradeiraqi3library.theme.parseHexColor

@Composable
fun BookmarksScreen(
    savedItems: List<BookItem>,
    savedPages: List<String>,
    isDark: Boolean,
    onNavigateToPdf: (BookItem, Int) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    onScrollableStateChanged: (Boolean) -> Unit = {},
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dynamicBottomPadding = (maxHeight * 0.25f).coerceAtLeast(bottomPadding)
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
                    Text("لا توجد محفوظات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "المحفوظات قسم خاص للصفحات التي تحفظها من قارئ الكتب والملازم اضغط على أيقونة النجمة للحفظ او الازالة او لاظهار الصفحات اضفظ على اسم الملزمة او الكتاب",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

            val groupedPages = remember(savedPages) {
                savedPages.mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        parts[0] to parts[1].toIntOrNull()
                    } else null
                }.filter { it.second != null }
                 .groupBy({ it.first }, { it.second!! })
            }

            val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
            val isMedium = screenWidthDp >= 600.dp
            LazyVerticalGrid(
                state = lazyGridState,
                columns = if (isMedium) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = bottomPadding)
            ) {
                item(key = "top_spacer", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val dynamicTopPadding = (this@BoxWithConstraints.maxHeight * 0.2f).coerceAtLeast(statusBarHeight + 76.dp)
                    Spacer(modifier = Modifier.height(dynamicTopPadding))
                }
                
                itemsIndexed(groupedPages.keys.toList(), key = { _, id -> id }) { index, bookId ->
                    val book = remember(bookId) { savedItems.find { it.id == bookId } }
                    if (book != null) {
                        val pages = groupedPages[bookId] ?: emptyList()
                        BookAccordionItem(
                            item = book,
                            pages = pages,
                            isDark = isDark,
                            index = index,
                            onNavigateToPdf = { pageIdx -> onNavigateToPdf(book, pageIdx) }
                        )
                    }
                }
                
                item(key = "bottom_spacer", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(dynamicBottomPadding))
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
    index: Int,
    onNavigateToPdf: (Int) -> Unit
) {
    val expandedSet by com.Nightjar.gradeiraqi3library.network.SyncEngine.expandedBookmarks.collectAsState()
    val isExpanded = expandedSet.contains(item.id)

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    var imageModifier = Modifier.fillMaxSize()
    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            imageModifier = imageModifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "cover-${item.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }

    var columnModifier = Modifier
        .fillMaxWidth()
        .liquidGlass(isDark, borderRadius = 20.dp, alpha = 0.5f)
        .clip(RoundedCornerShape(20.dp))

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            columnModifier = columnModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "book-${item.id}"),
                animatedVisibilityScope = animatedVisibilityScope, renderInOverlayDuringTransition = false
            )
        }
    }

    Column(
        modifier = columnModifier
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { com.Nightjar.gradeiraqi3library.network.SyncEngine.toggleExpandedBookmark(item.id) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                        .data(com.Nightjar.gradeiraqi3library.generated.resources.Res.getUri("drawable/${item.coverResName}." + if (item.coverResName in listOf("ayajaaa", "english", "englishactivity", "kss1")) "jpg" else "png"))
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
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

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Expanded content
        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))
                pages.sorted().forEach { pageIndex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onNavigateToPdf(pageIndex) }
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
