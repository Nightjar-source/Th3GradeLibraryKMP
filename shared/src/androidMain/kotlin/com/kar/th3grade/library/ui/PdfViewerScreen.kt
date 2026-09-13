@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.Nightjar.gradeiraqi3library.ui

import com.Nightjar.gradeiraqi3library.theme.bounceClick

import android.os.Build
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.core.view.WindowCompat
import com.Nightjar.gradeiraqi3library.data.AllItems
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.gradeiraqi3library.data.BookItem
import com.Nightjar.gradeiraqi3library.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import java.io.File
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.Nightjar.gradeiraqi3library.LocalSharedTransitionScope
import com.Nightjar.gradeiraqi3library.LocalAnimatedVisibilityScope
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.positionChanged
import android.util.LruCache
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val pdfMutex = Mutex()

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
actual fun PdfViewerScreen(
    item: BookItem,
    isSaved: (Int) -> Boolean,
    onToggleSave: (Int) -> Unit,
    onClose: () -> Unit,
    initialPage: Int?,
    isDark: Boolean
) {
    val context = LocalContext.current
    val appSettings by com.Nightjar.gradeiraqi3library.network.SyncEngine.appSettings.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isAppDark = isDark || when (appSettings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    var tempFile by remember(item) { mutableStateOf<File?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var loadingError by remember(item) { mutableStateOf<String?>(null) }
    var orientation by remember(appSettings.pdfScrollDirection) { mutableStateOf(appSettings.pdfScrollDirection) }
    var isZoomed by remember(item) { mutableStateOf(false) }

    // Find and cast Activity context to toggle system status bars
    val activity = remember(context) {
        var currentContext = context
        var act: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                act = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        act
    }

    var isCurrentPageTopLight by remember { mutableStateOf(!isAppDark) }
    var isCurrentPageBottomLight by remember { mutableStateOf(!isAppDark) }
    val pageLuminanceMap = remember { mutableStateMapOf<Int, Pair<Boolean, Boolean>>() }

    // Guaranteed cleanup on exit: restore system bars to match active app theme contrast
    DisposableEffect(activity, isAppDark) {
        onDispose {
            activity?.let { act ->
                val window = act.window
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = !isAppDark
                controller.isAppearanceLightNavigationBars = !isAppDark
            }
        }
    }

    LaunchedEffect(isZoomed, isCurrentPageTopLight, isCurrentPageBottomLight, activity) {
        activity?.let { act ->
            val window = act.window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            // Dynamic contrast: adapt status bar and 3-button navigation bar to real-time content
            controller.isAppearanceLightStatusBars = isCurrentPageTopLight
            controller.isAppearanceLightNavigationBars = isCurrentPageBottomLight

            if (isZoomed) {
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        onClose()
    }

    val scope = rememberCoroutineScope()
    // Load last read page index directly from unified SyncEngine database
    val savedPage = remember(item) { com.Nightjar.gradeiraqi3library.network.SyncEngine.lastReadPages.value[item.id] ?: 0 }

    // Load file and instantiate PdfRenderer
    LaunchedEffect(item) {
        try {
            val file = File(context.cacheDir, "pdf_${item.id}.pdf")
            if (!file.exists()) {
                withContext(Dispatchers.IO) {
                    val assetManager = context.assets
                    // نبحث عن مسار الملف الدقيق داخل الأصول
                    val possiblePaths = listOf(
                        "composeResources/com.Nightjar.gradeiraqi3library.generated.resources/${item.pdfPath}",
                        "composeResources/shared.generated.resources/${item.pdfPath}",
                        "composeResources/com.kar.th3grade.library.generated.resources/${item.pdfPath}",
                        item.pdfPath
                    )
                    
                    var inputStream: java.io.InputStream? = null
                    for (path in possiblePaths) {
                        try {
                            inputStream = assetManager.open(path)
                            if (inputStream != null) break
                        } catch (e: Exception) { }
                    }

                    if (inputStream != null) {
                        // النسخ المتدفق (Streaming): هذا هو الحل السحري! 
                        // نقرأ الملف كأجزاء صغيرة (32 كيلوبايت) ونكتبها في الذاكرة المؤقتة.
                        // هذا يمنع امتلاء الرام (OOM) مهما كان حجم الملف، حتى لو كان 1 غيغابايت!
                        inputStream.use { input ->
                            java.io.FileOutputStream(file).use { output ->
                                input.copyTo(output, 32 * 1024)
                            }
                        }
                    } else {
                        // كحل احتياطي أخير في حال عدم العثور على المسار المباشر
                        val bytes = Res.readBytes(item.pdfPath)
                        file.writeBytes(bytes)
                    }
                }
            }
            tempFile = file

            var pfd: ParcelFileDescriptor? = null
            try {
                if (!isActive) return@LaunchedEffect
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                if (!isActive) {
                    pfd?.close()
                    return@LaunchedEffect
                }
                pdfRenderer = PdfRenderer(pfd)
            } catch (initEx: Exception) {
                pfd?.close()
                throw initEx
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadingError = "فشل في تحميل الملف: ${e.message}"
        }
    }

    DisposableEffect(item, activity) {
        onDispose {
            try {
                val rendererToClose = pdfRenderer
                pdfRenderer = null
                val fileToDelete = tempFile
                
                // Asynchronously close PdfRenderer inside pdfMutex on Dispatchers.IO
                // This guarantees any active page.close() finishes first, preventing SIGSEGV in libpdfium.so
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        pdfMutex.withLock {
                            try {
                                rendererToClose?.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            try {
                                fileToDelete?.delete()
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                PdfBitmapCache.cache.evictAll()
                activity?.let { act ->
                    val window = act.window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        window.isStatusBarContrastEnforced = false
                    }
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.isAppearanceLightStatusBars = !isAppDark
                    controller.isAppearanceLightNavigationBars = !isAppDark
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    var isFirstPageReady by remember { mutableStateOf(false) }

    val cornerRadius by if (animatedVisibilityScope != null) {
        androidx.compose.animation.core.animateDpAsState(
            targetValue = if (animatedVisibilityScope.transition.targetState == androidx.compose.animation.EnterExitState.Visible) 0.dp else 28.dp,
            label = "corner"
        )
    } else remember { mutableStateOf(0.dp) }

    val rootModifier = Modifier
        .fillMaxSize()
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
        .then(
            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "card_${item.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.LinearOutSlowInEasing)),
                        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)),
                        renderInOverlayDuringTransition = false,
                        boundsTransform = { initialBounds, targetBounds ->
                            val isExpanding = targetBounds.width > initialBounds.width
                            if (isExpanding) {
                                // Open physics: fast, responsive, fluid expansion
                                spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 380f
                                )
                            } else {
                                // Close physics: soft, cushioned, elegant contraction
                                spring(
                                    dampingRatio = 0.88f,
                                    stiffness = 320f
                                )
                            }
                        }
                    )
                }
            } else Modifier
        )
        .background(MaterialTheme.colorScheme.background)

    Box(modifier = rootModifier) {

        if (loadingError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(loadingError!!, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onClose) {
                    Text("رجوع")
                }
            }
        } else {
            if (pdfRenderer != null) {
                androidx.compose.runtime.key(item.id) {
                    val pageCount = pdfRenderer!!.pageCount
                    val targetInitialPage = initialPage ?: if (pageCount > 0) savedPage.coerceAtMost(pageCount - 1) else 0
                    val pagerState = rememberPagerState(initialPage = targetInitialPage.coerceAtMost(maxOf(0, pageCount - 1))) { pageCount }

                    LaunchedEffect(pagerState.currentPage) {
                        if (pageCount > 0) {
                            com.Nightjar.gradeiraqi3library.network.SyncEngine.saveLastReadPage(item.id, pagerState.currentPage)
                        }
                    }

                    val currentPage = pagerState.currentPage
                    val currentLum = pageLuminanceMap[currentPage]

                    // Intelligently sync bars to current page contrast or reader background
                    LaunchedEffect(currentPage, currentLum, isAppDark) {
                        if (currentLum != null) {
                            isCurrentPageTopLight = currentLum.first
                            isCurrentPageBottomLight = currentLum.second
                        } else {
                            isCurrentPageTopLight = !isAppDark
                            isCurrentPageBottomLight = !isAppDark
                        }
                    }

                    var showJumpDialog by remember { mutableStateOf(false) }
                    var jumpPageInput by remember { mutableStateOf("") }

                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        // Render page content
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (orientation == "vertical") {
                                VerticalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(0.dp),
                                    pageSpacing = 0.dp
                                ) { pageIndex ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val currentRenderer = pdfRenderer
                                        if (currentRenderer != null) {
                                            AndroidPdfPage(
                                                renderer = currentRenderer,
                                                pageIndex = pageIndex,
                                                scrollDirection = orientation,
                                                isAppDark = isAppDark,
                                                onZoomChanged = { isZoomed = it },
                                                onPageReady = {
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isFirstPageReady = true
                                                    }
                                                },
                                                onLuminanceCalculated = { topLight, bottomLight ->
                                                    pageLuminanceMap[pageIndex] = (topLight to bottomLight)
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isCurrentPageTopLight = topLight
                                                        isCurrentPageBottomLight = bottomLight
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(0.dp),
                                    pageSpacing = 0.dp
                                ) { pageIndex ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val currentRenderer = pdfRenderer
                                        if (currentRenderer != null) {
                                            AndroidPdfPage(
                                                renderer = currentRenderer,
                                                pageIndex = pageIndex,
                                                scrollDirection = orientation,
                                                isAppDark = isAppDark,
                                                onZoomChanged = { isZoomed = it },
                                                onPageReady = {
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isFirstPageReady = true
                                                    }
                                                },
                                                onLuminanceCalculated = { topLight, bottomLight ->
                                                    pageLuminanceMap[pageIndex] = (topLight to bottomLight)
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isCurrentPageTopLight = topLight
                                                        isCurrentPageBottomLight = bottomLight
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    // Completely Floating Top control bar
                    AnimatedVisibility(
                        visible = !isZoomed,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = item.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )

                            // Toolbar actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isPageSaved = isSaved(pagerState.currentPage)
                                IconButton(
                                    onClick = { onToggleSave(pagerState.currentPage) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isPageSaved) Color(0x33F59E0B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                        .bounceClick { onToggleSave(pagerState.currentPage) }
                                ) {
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = isPageSaved,
                                        transitionSpec = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.5f) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 0.5f) }
                                    ) { saved ->
                                        Icon(
                                            imageVector = if (saved) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (saved) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val newOrientation = if (orientation == "vertical") "horizontal" else "vertical"
                                        orientation = newOrientation
                                        com.Nightjar.gradeiraqi3library.network.SyncEngine.saveSettings(appSettings.copy(pdfScrollDirection = newOrientation))
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                        .bounceClick {
                                            val newOrientation = if (orientation == "vertical") "horizontal" else "vertical"
                                            orientation = newOrientation
                                            com.Nightjar.gradeiraqi3library.network.SyncEngine.saveSettings(appSettings.copy(pdfScrollDirection = newOrientation))
                                        }
                                ) {
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = orientation,
                                        transitionSpec = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.5f) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 0.5f) }
                                    ) { currentOrientation ->
                                        Icon(
                                            imageVector = if (currentOrientation == "vertical") Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                                            contentDescription = "Change Scroll Direction",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                
                    // Floating Page Indicator capsule at bottom center
                    AnimatedVisibility(
                        visible = !isZoomed,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Card(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                                .padding(bottom = 20.dp)
                                .shadow(12.dp, shape = CircleShape),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val currentPage = pagerState.currentPage + 1

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (pagerState.currentPage > 0) {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Text(
                                    text = "$currentPage / $pageCount",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { showJumpDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (pagerState.currentPage < pageCount - 1) {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Animated Overlay for Jump Dialog
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showJumpDialog,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        var jumpErrorText by remember { mutableStateOf<String?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { 
                                    showJumpDialog = false
                                    jumpPageInput = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clickable(enabled = false) {}
                                    .shadow(24.dp, RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                                    .animateEnterExit(
                                        enter = androidx.compose.animation.scaleIn(
                                            initialScale = 0.8f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                                        ) + androidx.compose.animation.fadeIn(),
                                        exit = androidx.compose.animation.scaleOut(
                                            targetScale = 0.9f,
                                            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
                                        ) + androidx.compose.animation.fadeOut()
                                    )
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "إلى أي صفحة تريد الانتقال؟",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = jumpPageInput,
                                        onValueChange = { 
                                            jumpPageInput = it 
                                            jumpErrorText = null // Clear error on edit
                                        },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        singleLine = true,
                                        placeholder = { Text("مثال: 15", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (jumpErrorText != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = jumpErrorText!!,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        TextButton(onClick = { 
                                            showJumpDialog = false 
                                            jumpPageInput = ""
                                        }) {
                                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }
                                        Button(
                                            onClick = {
                                                val p = jumpPageInput.toIntOrNull()
                                                if (p == null) {
                                                    jumpErrorText = "الرجاء إدخال رقم صحيح"
                                                } else if (p !in 1..pageCount) {
                                                    jumpErrorText = "رقم الصفحة غير موجود في هذا الملف (1 - $pageCount)"
                                                } else {
                                                    scope.launch { pagerState.animateScrollToPage(p - 1) }
                                                    showJumpDialog = false
                                                    jumpPageInput = ""
                                                    jumpErrorText = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("انتقال سريع", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }

            // 2. Loading overlay (rendered ONLY while PDF is initializing from disk)
            var showLoading by remember { mutableStateOf(false) }
            LaunchedEffect(pdfRenderer) {
                if (pdfRenderer == null) {
                    kotlinx.coroutines.delay(250)
                    showLoading = true
                } else {
                    showLoading = false
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showLoading,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        com.Nightjar.gradeiraqi3library.ui.ExpressiveLoadingIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "جاري معالجة وفتح الملف...",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
        }
    }
}

// Singleton Cache for PDF Pages to prevent lag during scrolling
object PdfBitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory
    val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
}

fun clearPdfCache(cacheDir: File) {
    try {
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("pdf_") && file.name.endsWith(".pdf")) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectZoomPanGestures(
    onGesture: (pan: Offset, zoom: Float) -> Boolean
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val consume = onGesture(panChange, zoomChange)
                    if (consume) {
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

fun sampleBitmapLuminance(bitmap: Bitmap): Pair<Boolean, Boolean> {
    return try {
        if (bitmap.width <= 0 || bitmap.height <= 0) return true to true
        val w = bitmap.width
        val h = bitmap.height

        val topY = 24.coerceAtMost(h - 1)
        val topP1 = bitmap.getPixel((w * 0.15f).toInt().coerceIn(0, w - 1), topY)
        val topP2 = bitmap.getPixel(w / 2, topY)
        val topP3 = bitmap.getPixel((w * 0.85f).toInt().coerceIn(0, w - 1), topY)
        val avgTopLum = (androidx.core.graphics.ColorUtils.calculateLuminance(topP1) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(topP2) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(topP3)) / 3.0

        val botY = (h - 24).coerceIn(0, h - 1)
        val botP1 = bitmap.getPixel((w * 0.15f).toInt().coerceIn(0, w - 1), botY)
        val botP2 = bitmap.getPixel(w / 2, botY)
        val botP3 = bitmap.getPixel((w * 0.85f).toInt().coerceIn(0, w - 1), botY)
        val avgBotLum = (androidx.core.graphics.ColorUtils.calculateLuminance(botP1) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(botP2) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(botP3)) / 3.0

        (avgTopLum > 0.5) to (avgBotLum > 0.5)
    } catch (_: Exception) {
        true to true
    }
}

@Composable
fun AndroidPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    scrollDirection: String = "vertical",
    isAppDark: Boolean = false,
    onZoomChanged: (Boolean) -> Unit = {},
    onPageReady: () -> Unit = {},
    onLuminanceCalculated: (isTopLight: Boolean, isBottomLight: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val cacheKey = "pdf_${renderer.hashCode()}_page_$pageIndex"
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(PdfBitmapCache.cache.get(cacheKey)) }
    
    // Zoom and pan states - reset when page changes
    var scale by remember(pageIndex) { mutableStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    LaunchedEffect(bitmap) {
        if (bitmap != null) {
            onPageReady()
        }
    }

    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    pdfMutex.withLock {
                        currentCoroutineContext().ensureActive()
                        val page = renderer.openPage(pageIndex)
                        try {
                            // Dynamic RAM-based scaling to prevent OutOfMemoryError
                            val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
                            val scaleFactor = when {
                                maxMemoryMb >= 512 -> 1.5 // High-end devices with large heap
                                maxMemoryMb >= 256 -> 1.2 // Mid-range
                                else -> 1.0 // Low-end or tight memory
                            }
                            
                            val width = (page.width * scaleFactor).toInt()
                            val height = (page.height * scaleFactor).toInt()
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            
                            val canvas = android.graphics.Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)

                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            PdfBitmapCache.cache.put(cacheKey, bmp)
                            bitmap = bmp
                        } finally {
                            try {
                                page.close()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            onPageReady()
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()
            
            val bitmapWidth = currentBitmap.width.toFloat()
            val bitmapHeight = currentBitmap.height.toFloat()
            val imageRatio = bitmapWidth / bitmapHeight
            val containerRatio = containerWidth / containerHeight
            
            // Fitted dimensions to ensure the whole page is visible without cropping
            val w: Float
            val h: Float
            if (imageRatio > containerRatio) {
                w = containerWidth
                h = containerWidth / imageRatio
            } else {
                h = containerHeight
                w = containerHeight * imageRatio
            }

            LaunchedEffect(currentBitmap, scale, scrollDirection, isAppDark, h, containerHeight) {
                try {
                    val (bmpTopLight, bmpBotLight) = sampleBitmapLuminance(currentBitmap)
                    val touchesEdges = (scrollDirection == "vertical") || (scale > 1.05f) || (h >= containerHeight - 80f)
                    val effectiveTopLight = if (touchesEdges) bmpTopLight else !isAppDark
                    val effectiveBottomLight = if (touchesEdges) bmpBotLight else !isAppDark
                    onLuminanceCalculated(effectiveTopLight, effectiveBottomLight)
                } catch (_: Exception) {}
            }

            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "الصفحة ${pageIndex + 1}",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(
                        width = with(androidx.compose.ui.platform.LocalDensity.current) { w.toDp() },
                        height = with(androidx.compose.ui.platform.LocalDensity.current) { h.toDp() }
                    )
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(pageIndex) { // Key on pageIndex to reset input detector
                        detectTapGestures(
                            onDoubleTap = {
                                val targetScale = if (scale > 1.05f) 1f else 2.5f
                                val targetOffsetX = 0f
                                val targetOffsetY = 0f
                                
                                val startScale = scale
                                val startOffset = offset
                                
                                scope.launch {
                                    animate(0f, 1f) { fraction, _ ->
                                        scale = startScale + fraction * (targetScale - startScale)
                                        offset = Offset(
                                            startOffset.x + fraction * (targetOffsetX - startOffset.x),
                                            startOffset.y + fraction * (targetOffsetY - startOffset.y)
                                        )
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(pageIndex) { // Key on pageIndex to reset input detector
                        detectZoomPanGestures { pan, zoom ->
                            scale = (scale * zoom).coerceIn(1f, 15f)
                            
                            val maxOffsetX = maxOf(0f, (w * scale - containerWidth) / 2f)
                            val maxOffsetY = maxOf(0f, (h * scale - containerHeight) / 2f)
                            
                            val newOffsetX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newOffsetY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            
                            offset = Offset(newOffsetX, newOffsetY)
                            
                            // Determine if we should consume the gesture
                            val atHorizontalEdge = (newOffsetX == -maxOffsetX && pan.x < 0) || (newOffsetX == maxOffsetX && pan.x > 0)
                            val atVerticalEdge = (newOffsetY == -maxOffsetY && pan.y < 0) || (newOffsetY == maxOffsetY && pan.y > 0)
                            
                            if (scrollDirection == "horizontal" && atHorizontalEdge && abs(pan.x) > abs(pan.y)) {
                                false
                            } else if (scrollDirection == "vertical" && atVerticalEdge && abs(pan.y) > abs(pan.x)) {
                                false
                            } else {
                                scale > 1f
                            }
                        }
                    }
            )
        }
    } else {
        Box(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            com.Nightjar.gradeiraqi3library.ui.ExpressiveLoadingIndicator(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
