package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import com.Nightjar.Th3GradeLibraryKMP.LocalSharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.LocalAnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NotificationDetailScreen(
    item: NewsItem,
    isDark: Boolean,
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
    onShare: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val sharedCardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "news-${item.link}"),
                animatedVisibilityScope = animatedVisibilityScope,
                renderInOverlayDuringTransition = true,
                boundsTransform = { _, _ -> spring(dampingRatio = 0.82f, stiffness = 280f) }
            )
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sharedCardModifier)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Card (if image exists)
                if (!item.imageUrl.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 380.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Date & Category Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = item.formattedDate.ifBlank { item.pubDate },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Share Icon Action
                    IconButton(
                        onClick = {
                            val shareText = "${item.title}\n\n${item.contentSnippet ?: ""}\n\n${item.link}"
                            onShare(shareText)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Title
                SelectionContainer {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 30.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )

                // Body Content (Formatted into styled paragraphs)
                val rawContent = item.contentSnippet ?: ""
                val embeddedUrls = remember(rawContent) { extractUrlsFromText(rawContent) }
                val allActionUrls = remember(item.link, embeddedUrls) {
                    val list = mutableListOf<String>()
                    if (item.link.isNotBlank() && (item.link.startsWith("http://") || item.link.startsWith("https://"))) {
                        list.add(item.link)
                    }
                    embeddedUrls.forEach { url ->
                        if (!list.contains(url)) list.add(url)
                    }
                    list
                }

                val paragraphs = remember(rawContent) {
                    rawContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                }

                if (paragraphs.isNotEmpty()) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            paragraphs.forEach { paragraph ->
                                val isBullet = paragraph.startsWith("-") || paragraph.startsWith("•") || paragraph.startsWith("*")
                                val cleanPara = if (isBullet) paragraph.substring(1).trim() else paragraph
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isBullet) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(if (isBullet) 10.dp else 2.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isBullet) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .size(6.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                        }
                                        Text(
                                            text = cleanPara,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                lineHeight = 26.sp,
                                                textAlign = TextAlign.Start
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Link Buttons (Distinct, beautifully styled buttons for every URL)
                if (allActionUrls.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allActionUrls.forEachIndexed { idx, url ->
                            val buttonTitle = if (allActionUrls.size == 1) "فتح الرابط المرفق" else "فتح الرابط المرفق #${idx + 1}"
                            Button(
                                onClick = { onOpenLink(url) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (idx == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (idx == 0) Icons.Default.OpenInBrowser else Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buttonTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Top Glass Bar (Strictly unified with App top bar design & M3 Adaptive)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val isWide = screenWidthDp >= 600.dp
        val topBarHeight = if (isWide) 40.dp else 48.dp

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = if (isWide) 4.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 608.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Island: Back Button
                Box(
                    modifier = Modifier
                        .size(topBarHeight)
                        .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Oval Island: Screen Title
                Box(
                    modifier = Modifier
                        .height(topBarHeight)
                        .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تفاصيل الإشعار",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Spacer balance
                Spacer(modifier = Modifier.size(topBarHeight))
            }
        }
    }
}

private fun extractUrlsFromText(text: String): List<String> {
    val regex = Regex("""(https?://[^\s\n\]\[\)\(]+)""")
    return regex.findAll(text).map { it.value.trimEnd('.', ',', ';') }.distinct().toList()
}
