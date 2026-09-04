package com.Nightjar.gradeiraqi3library

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import com.Nightjar.gradeiraqi3library.data.BookItem
import com.Nightjar.gradeiraqi3library.data.AllItems
import com.Nightjar.gradeiraqi3library.data.PlatformActionHandler
import com.Nightjar.gradeiraqi3library.network.SyncEngine
import com.Nightjar.gradeiraqi3library.theme.Th3GradeTheme
import com.Nightjar.gradeiraqi3library.theme.liquidGlass
import com.Nightjar.gradeiraqi3library.theme.parseHexColor
import com.Nightjar.gradeiraqi3library.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.Nightjar.gradeiraqi3library.generated.resources.Res
import com.Nightjar.gradeiraqi3library.generated.resources.*
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun App(
    platformActionHandler: PlatformActionHandler,
    systemAccentColor: String? = null,
    dynamicColorScheme: androidx.compose.material3.ColorScheme? = null,
    initialBookId: String? = null,
    initialIsNote: Boolean = false,
    initialPage: String? = null,
    initialSearchQuery: String? = null,
    onIntentConsumed: () -> Unit = {}
) {
    val appSettings by SyncEngine.appSettings.collectAsState()
    
    LaunchedEffect(platformActionHandler) {
        SyncEngine.platformActionHandler = platformActionHandler
    }

    val isDarkTheme = when (appSettings.theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val seedColorHex = if (appSettings.useMaterialYou && systemAccentColor != null) {
        systemAccentColor
    } else {
        appSettings.primaryColor
    }

    Th3GradeTheme(
        darkTheme = isDarkTheme,
        pureBlackMode = appSettings.pureBlackMode,
        pureWhiteMode = appSettings.pureWhiteMode,
        seedColorHex = seedColorHex,
        dynamicColorScheme = if (appSettings.useMaterialYou) dynamicColorScheme else null
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            com.Nightjar.gradeiraqi3library.theme.LocalIsLowEndDevice provides (platformActionHandler?.isLowEndDevice() ?: false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppContent(
                    platformActionHandler = platformActionHandler,
                    isDark = isDarkTheme,
                    initialBookId = initialBookId,
                    initialIsNote = initialIsNote,
                    initialPage = initialPage,
                    initialSearchQuery = initialSearchQuery,
                    onIntentConsumed = onIntentConsumed
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent(
    platformActionHandler: PlatformActionHandler,
    isDark: Boolean,
    initialBookId: String?,
    initialIsNote: Boolean,
    initialPage: String?,
    initialSearchQuery: String?,
    onIntentConsumed: () -> Unit
) {
    val appSettings by SyncEngine.appSettings.collectAsState()
    val toastMessage by ToastManager.toastMessage.collectAsState()
    val scope = rememberCoroutineScope()

    
    // Hoisted scroll/list states for preserving positions across navigation
    val newsLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val libraryHomeScrollState = androidx.compose.foundation.rememberScrollState()
    val libraryBooksLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val libraryNotesLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val bookmarksLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val shortcutsLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val aboutScrollState = androidx.compose.foundation.rememberScrollState()

    val pageHistory = androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf(
            if (initialPage != null && initialPage != "home") listOf("home", initialPage) 
            else listOf("home")
        ) 
    }
    var page by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialPage ?: "home") }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val bottomBarHeightPx = with(density) { 200.dp.toPx() } // Safety height to slide completely off screen under nav bars
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarOffsetHeightPx by animateFloatAsState(
        targetValue = if (isBottomBarVisible) 0f else bottomBarHeightPx,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "bottomBarOffset"
    )

    var canCurrentPageScroll by remember { mutableStateOf(false) }
    var accumulatedScrollY by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember(canCurrentPageScroll) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (!canCurrentPageScroll || page == "home") {
                    isBottomBarVisible = true
                    accumulatedScrollY = 0f
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                val delta = consumed.y
                if (delta == 0f) return androidx.compose.ui.geometry.Offset.Zero
                
                accumulatedScrollY += delta
                if (isBottomBarVisible) {
                    if (accumulatedScrollY < 0) {
                        if (accumulatedScrollY < -150f) {
                            isBottomBarVisible = false
                            accumulatedScrollY = 0f
                        }
                    } else {
                        accumulatedScrollY = 0f
                    }
                } else {
                    if (accumulatedScrollY > 0) {
                        if (accumulatedScrollY > 100f) {
                            isBottomBarVisible = true
                            accumulatedScrollY = 0f
                        }
                    } else {
                        accumulatedScrollY = 0f
                    }
                }
                
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    var isNavigatingBack by remember { mutableStateOf(false) }

    fun popPage(): Boolean {
        isNavigatingBack = true
        val current = pageHistory.value
        if (current.size > 1) {
            val updated = current.dropLast(1)
            pageHistory.value = updated
            page = updated.last()
            return true
        }
        return false
    }

    // Connect properties to SyncEngine (Unified Persistent Storage)
    val isGridView by SyncEngine.isGridView.collectAsState()
    val selectedCategory by SyncEngine.selectedCategory.collectAsState()

    var selectedItem by remember { mutableStateOf<BookItem?>(null) }

    LaunchedEffect(page, selectedCategory, selectedItem) {
        isBottomBarVisible = true
        accumulatedScrollY = 0f
        canCurrentPageScroll = false
    }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(initialSearchQuery ?: "") }

    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(!appSettings.isOnboardingCompleted) }

    // Modal states
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isAppSettingsOpen by remember { mutableStateOf(false) }
    var isNewsSettingsOpen by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    fun navigateTo(newPage: String) {
        isNavigatingBack = false
        isAppSettingsOpen = false
        isNewsSettingsOpen = false
        if (newPage == "home") {
            pageHistory.value = listOf("home")
            page = "home"
        } else {
            val current = pageHistory.value
            if (newPage != current.lastOrNull()) {
                pageHistory.value = current.filter { it != newPage } + newPage
                page = newPage
            }
        }
    }

    // Bookmarks list from SyncEngine (Persistent Cache)
    val savedItemIds by SyncEngine.savedBookmarks.collectAsState()
    val savedItems = remember(savedItemIds) {
        val savedBookIds = savedItemIds.map { it.split(":")[0] }.toSet()
        (AllItems.books + AllItems.notes).filter { it.id in savedBookIds }
    }
    var pdfInitialPage by remember { mutableStateOf<Int?>(null) }

    // Platform Back Handlers
    val isSettingsOpen = isAppSettingsOpen || isNewsSettingsOpen
    val backEnabled = showClearCacheDialog || isDrawerOpen || isSettingsOpen || page != "home" || selectedItem != null || pageHistory.value.size > 1
    PlatformBackHandler(enabled = backEnabled) {
        if (showClearCacheDialog) {
            showClearCacheDialog = false
        } else if (isDrawerOpen) {
            isDrawerOpen = false
        } else if (isNewsSettingsOpen) {
            isNewsSettingsOpen = false
        } else if (isAppSettingsOpen) {
            isAppSettingsOpen = false
        } else if (selectedItem != null) {
            popPage()
            scope.launch {
                kotlinx.coroutines.delay(350)
                selectedItem = null
                pdfInitialPage = null
            }
        } else {
            when (page) {
                "list" -> {
                    if (!popPage()) {
                        navigateTo("home")
                    }
                }
                else -> {
                    if (!popPage()) {
                        // Exit the app gracefully if stack is empty
                    }
                }
            }
        }
    }

    // Handle deep link / shortcut / notification parameters reactively
    LaunchedEffect(initialBookId, initialIsNote, initialPage, initialSearchQuery) {
        var consumed = false
        if (initialBookId != null) {
            val target = AllItems.books.firstOrNull { it.id == initialBookId }
                ?: AllItems.notes.firstOrNull { it.id == initialBookId }
            if (target != null) {
                SyncEngine.saveSelectedCategory(if (target.isNote) "notes" else "books")
                navigateTo("list") // Push parent section first
                
                // Auto-scroll to item so back animation works perfectly
                val list = if (target.isNote) AllItems.notes else AllItems.books
                val index = list.indexOf(target)
                if (index >= 0) {
                    val gridState = if (target.isNote) libraryNotesLazyGridState else libraryBooksLazyGridState
                    scope.launch { gridState.scrollToItem(index) }
                }

                pdfInitialPage = SyncEngine.lastReadPages.value[target.id]
                selectedItem = target
                navigateTo("pdf") // Push PDF on top
                consumed = true
            }
        }
        if (initialPage != null) {
            navigateTo(initialPage)
            consumed = true
        }
        if (initialSearchQuery != null) {
            searchQuery = initialSearchQuery
            isSearchOpen = true
            consumed = true
        }
        if (consumed) {
            onIntentConsumed()
        }
    }

    // Auto-fetch on start ONLY if the setting is "on_open" (sync on open)
    LaunchedEffect(appSettings.syncInterval) {
        if (appSettings.syncInterval == "on_open") {
            if (appSettings.lastSync > 0L) {
                scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    SyncEngine.fetchAndSync(forced = false)
                }
            }
        }
        // Removed the while(true) loop that was causing continuous UI updates.
        // Background sync is handled properly by WorkManager according to the user's interval.
    }

    // First time install auto-fetch & notification permission: trigger only after splash and onboarding finish
    LaunchedEffect(showSplash, showOnboarding) {
        if (!showSplash && !showOnboarding) {
            
            
            // Always sync when the app is fully opened by the user, regardless of background sync interval!
            scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                SyncEngine.fetchAndSync(forced = true)
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
        val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
        val isWide = screenWidthDp >= 600.dp
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listBottomPadding = if (isWide) 24.dp + navBarHeight else 120.dp + navBarHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (isWide && page in listOf("home", "list", "news", "bookmarks", "shortcuts", "about")) 90.dp else 0.dp) // reduced from 108.dp for smaller rail
        ) {
                // Background Orbs (Premium Liquid M3 Aesthetics)
                if (platformActionHandler?.isLowEndDevice() != true) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    val orbSize = if (isWide) 200.dp else 300.dp
                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .align(Alignment.TopEnd)
                            .offset(x = (orbSize / 3), y = (-orbSize / 6))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        parseHexColor(appSettings.primaryColor).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .align(Alignment.BottomStart)
                            .offset(x = (-orbSize / 3), y = (orbSize / 3))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        parseHexColor(appSettings.primaryColor).copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                }

                // Page Content (Always matches full content area)
                val horizontalInsets = if (page != "pdf") WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal) else WindowInsets(0,0,0,0)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(horizontalInsets)
                        .nestedScroll(nestedScrollConnection),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth() // removed widthIn to allow full edge-to-edge content on tablets
                    ) {
                        AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                val pageOrder = listOf("home", "list", "news", "bookmarks", "shortcuts", "about")
                                val initialIndex = pageOrder.indexOf(initialState).takeIf { it >= 0 } ?: 0
                                val targetIndex = pageOrder.indexOf(targetState).takeIf { it >= 0 } ?: 0
                                
                                if (targetState == "pdf") {
                                    fadeIn(tween(260, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))).togetherWith(
                                        fadeOut(tween(180))
                                    )
                                } else if (initialState == "pdf") {
                                    fadeIn(tween(200, easing = androidx.compose.animation.core.LinearOutSlowInEasing)).togetherWith(
                                        fadeOut(tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 1f, 1f)))
                                    )
                                } else {
                                    // RTL partial-slide transitions matching caliq5 exactly
                                    val fluidEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
                                    val slideSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
                                        durationMillis = 280,
                                        easing = fluidEasing
                                    )
                                    val fadeSpec = androidx.compose.animation.core.tween<Float>(
                                        durationMillis = 220,
                                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                                    )
                                    val direction = if (isNavigatingBack || targetIndex < initialIndex) 1 else -1 // RTL: Forward is -1 (from left), Backward is 1 (from right)

                                    (slideInHorizontally(
                                        initialOffsetX = { fullWidth -> (fullWidth * 0.38f * direction).toInt() },
                                        animationSpec = slideSpec
                                    ) + fadeIn(animationSpec = fadeSpec)).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> (-fullWidth * 0.22f * direction).toInt() },
                                            animationSpec = slideSpec
                                        ) + fadeOut(animationSpec = fadeSpec)
                                    )
                                }
                            }
                        ) { targetPage ->
                            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@AnimatedContent) {
                                when (targetPage) {
                                    "home" -> LibraryScreen(
                                        category = null,
                                        isGridView = isGridView,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onNavigateToCategory = {
                                            SyncEngine.saveSelectedCategory(it)
                                            navigateTo("list")
                                        },
                                        onNavigateToPdf = {
                                            selectedItem = it
                                            pdfInitialPage = SyncEngine.lastReadPages.value[it.id]
                                            navigateTo("pdf")
                                        },
                                        isDark = isDark,
                                        onToggleViewMode = { SyncEngine.saveGridView(!isGridView) },
                                        isSearchOpen = isSearchOpen,
                                        onToggleSearch = { isSearchOpen = !isSearchOpen },
                                        onVoiceSearchTrigger = {
                                            platformActionHandler.showToast("جاري الاستماع للبحث الصوتي...")
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        scrollState = libraryHomeScrollState
                                    )
                                    "list" -> LibraryScreen(
                                        category = selectedCategory ?: "books",
                                        isGridView = isGridView,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onNavigateToCategory = {
                                            SyncEngine.saveSelectedCategory(it)
                                            navigateTo("list")
                                        },
                                        onNavigateToPdf = {
                                            selectedItem = it
                                            pdfInitialPage = SyncEngine.lastReadPages.value[it.id]
                                            navigateTo("pdf")
                                        },
                                        isDark = isDark,
                                        onToggleViewMode = { SyncEngine.saveGridView(!isGridView) },
                                        isSearchOpen = isSearchOpen,
                                        onToggleSearch = { isSearchOpen = !isSearchOpen },
                                        onVoiceSearchTrigger = {
                                            platformActionHandler.showToast("جاري الاستماع للبحث الصوتي...")
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = if (selectedCategory == "books") libraryBooksLazyGridState else libraryNotesLazyGridState
                                    )
                                    "news" -> NewsScreen(
                                        onOpenSettings = { isNewsSettingsOpen = true },
                                        onClick = { url, _ ->
                                            platformActionHandler.openCustomTab(url, appSettings.primaryColor)
                                        },
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = newsLazyGridState
                                    )
                                    "bookmarks" -> BookmarksScreen(
                                        savedItems = savedItems,
                                        savedPages = savedItemIds.toList(),
                                        isDark = isDark,
                                        onNavigateToPdf = { item, pageIdx ->
                                            pdfInitialPage = pageIdx
                                            selectedItem = item
                                            navigateTo("pdf")
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = bookmarksLazyGridState
                                    )
                                    "shortcuts" -> ShortcutsScreen(
                                        platformActionHandler = platformActionHandler,
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = shortcutsLazyGridState
                                    )
                                    "about" -> AboutScreen(
                                        onOpenUrl = { url ->
                                            platformActionHandler.showToast("تحويل إلى تليغرام...")
                                            platformActionHandler.openUrl(url)
                                        },
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        scrollState = aboutScrollState
                                    )
                                    "pdf" -> {
                                        if (selectedItem != null) {
                                            PdfViewerScreen(
                                                item = selectedItem!!,
                                                isSaved = { pageIdx -> "${selectedItem!!.id}:$pageIdx" in savedItemIds },
                                                onToggleSave = { pageIdx ->
                                                    val key = "${selectedItem!!.id}:$pageIdx"
                                                    val isNowSaved = SyncEngine.toggleBookmark(key)
                                                    if (!isNowSaved) {
                                                        platformActionHandler.showToast("تم حذف الصفحة ${pageIdx + 1} من المحفوظات")
                                                    } else {
                                                        platformActionHandler.showToast("تم حفظ الصفحة ${pageIdx + 1} في المحفوظات")
                                                    }
                                                },
                                                onClose = {
                                                    popPage()
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(350) // Wait for transition to finish
                                                        selectedItem = null
                                                        pdfInitialPage = null
                                                    }
                                                },
                                                initialPage = pdfInitialPage
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // --- Floating App Bar (Always visible except PDF and WebView) ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = page != "pdf" && page != "web",
                    enter = slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(400)),
                    exit = slideOutVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(400)),
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(50f)
                ) {
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 608.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.95f)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                if (page != "home") {
                                    IconButton(
                                        onClick = {
                                            if (page == "list" && selectedCategory != null) {
                                                SyncEngine.saveSelectedCategory(null)
                                                navigateTo("home")
                                            } else {
                                                if (!popPage()) {
                                                    navigateTo("home")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        parseHexColor(appSettings.primaryColor).copy(alpha = 0.5f),
                                                        Color.Transparent
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(com.Nightjar.gradeiraqi3library.generated.resources.Res.drawable.app_icon),
                                            contentDescription = "App Icon",
                                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                var savedTitle by remember { mutableStateOf("مكتبة الثالث متوسط") }
                                val appBarTitleRaw = when (page) {
                                    "news" -> "آخر الأخبار"
                                    "bookmarks" -> "المحفوظات"
                                    "shortcuts" -> "إضافة اختصار"
                                    "about" -> "حول التطبيق"
                                    "list" -> if (selectedCategory == "books") "الكتب الرسمية" else "الملازم الدراسية"
                                    else -> "مكتبة الثالث متوسط"
                                }
                                if (page != "pdf" && page != "error") {
                                    savedTitle = appBarTitleRaw
                                }

                                AnimatedContent(
                                    targetState = savedTitle,
                                    transitionSpec = {
                                        (slideInVertically { height -> height / 2 } + fadeIn(tween(220)))
                                            .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(220)))
                                    },
                                    label = "titleTransition"
                                ) { targetTitle ->
                                    Text(
                                        text = targetTitle,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = if (targetTitle == "مكتبة الثالث متوسط") com.Nightjar.gradeiraqi3library.theme.KufiReemFontFamily else null,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = page == "news",
                                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                                ) {
                                    IconButton(onClick = { isNewsSettingsOpen = true }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                AnimatedVisibility(
                                    visible = page == "list",
                                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { SyncEngine.saveGridView(!isGridView) }) {
                                            AnimatedContent(
                                                targetState = isGridView,
                                                transitionSpec = { fadeIn(tween(300)) + scaleIn(initialScale = 0.8f) togetherWith fadeOut(tween(300)) + scaleOut(targetScale = 0.8f) }
                                            ) { gridView ->
                                                Icon(
                                                    imageVector = if (gridView) Icons.Default.ViewList else Icons.Default.GridView,
                                                    contentDescription = "Toggle Grid/List view",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                                            AnimatedContent(
                                                targetState = isSearchOpen,
                                                transitionSpec = { fadeIn(tween(300)) + scaleIn(initialScale = 0.8f) togetherWith fadeOut(tween(300)) + scaleOut(targetScale = 0.8f) }
                                            ) { searchOpen ->
                                                Icon(
                                                    imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                                                    contentDescription = "Toggle Search",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Floating Search Bar (Aligned inside the Main Content Area) ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = page == "list" && isSearchOpen,
                    enter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    val topPadding = 80.dp
                    var isListening by remember { mutableStateOf(false) }
                    val pulseAnim = animateFloatAsState(
                        targetValue = if (isListening) 1.2f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(top = topPadding, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 608.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.95f)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = if (isListening) "جاري الاستماع..." else searchQuery,
                                onValueChange = { if (!isListening) searchQuery = it },
                                placeholder = { Text("ابحث عن ملزمة أو كتاب...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = isListening
                            )
                            
                            IconButton(
                                onClick = {
                                    if (!isListening) {
                                        isListening = true
                                        platformActionHandler.startVoiceSearch(
                                            onResult = { text ->
                                                searchQuery = text
                                            },
                                            onEnd = {
                                                isListening = false
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .graphicsLayer {
                                        val s = if (isListening) pulseAnim.value else 1f
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .background(
                                        color = if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                    contentDescription = "Voice search",
                                    tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (searchQuery.isNotEmpty() && !isListening) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        }
                    }
                }



        // --- Mobile Bottom Navigation Capsule (Only visible on small screens and main tabs) ---
        val mainTabs = listOf("home", "list", "news", "bookmarks", "shortcuts", "about")
        androidx.compose.animation.AnimatedVisibility(
            visible = page in mainTabs && !isWide,
            enter = slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().zIndex(50f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .offset { androidx.compose.ui.unit.IntOffset(x = 0, y = bottomBarOffsetHeightPx.roundToInt()) }
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val navItems = listOf(
                    Triple("المكتبة", Icons.Default.Home) {
                        navigateTo("home")
                        SyncEngine.saveSelectedCategory(null)
                    },
                    Triple("الأخبار", Icons.Default.Newspaper) { navigateTo("news") },
                    Triple("المحفوظات", Icons.Default.Star) { navigateTo("bookmarks") },
                    Triple("القائمة", Icons.Default.Menu) { isDrawerOpen = true }
                )

                val isNavActive = listOf(page == "home" || page == "list", page == "news", page == "bookmarks", isDrawerOpen)
                var lastValidIndex by remember { mutableStateOf(0) }
                val currentIndex = if (isDrawerOpen) 3 else isNavActive.take(3).indexOfFirst { it }.takeIf { it >= 0 }
                if (currentIndex != null) {
                    lastValidIndex = currentIndex
                }
                val selectedIndex = lastValidIndex

                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                        .height(64.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .liquidGlass(isDark, borderRadius = 32.dp, alpha = 0.90f)
                        .padding(horizontal = 8.dp)
                ) {
                    val contentWidth = maxWidth
                    val itemWidth = contentWidth / navItems.size
                    var dragOffset by remember { mutableStateOf<Float?>(null) }
                    val itemWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemWidth.toPx() }
                    
                    val indicatorOffset by animateDpAsState(
                        targetValue = if (dragOffset != null) with(androidx.compose.ui.platform.LocalDensity.current) { (dragOffset!! - (itemWidthPx / 2)).toDp() } else itemWidth * selectedIndex,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(itemWidth)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(layoutDirection) {
                                val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()
                                        if (change != null) {
                                            if (change.pressed) {
                                                val rawX = change.position.x
                                                val directionalX = if (isRtl) size.width.toFloat() - rawX else rawX
                                                dragOffset = directionalX.coerceIn(0f, size.width.toFloat())
                                            } else {
                                                // When finger lifted, trigger click if we dragged
                                                if (dragOffset != null) {
                                                    val idx = (dragOffset!! / itemWidthPx).toInt().coerceIn(0, navItems.lastIndex)
                                                    navItems[idx].third()
                                                    dragOffset = null
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isActive = isNavActive[index]
                            val tint by animateColorAsState(
                                targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(250)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp)), // Removed clickable since we handle it in pointerInput
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = item.first,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.first,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // --- Tablet/Wide Floating Side Navigation Capsule ---
        androidx.compose.animation.AnimatedVisibility(
            visible = page in mainTabs && isWide,
            enter = slideInHorizontally(animationSpec = tween(400, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(400)),
            exit = slideOutHorizontally(animationSpec = tween(400, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().zIndex(50f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(end = 12.dp, top = 24.dp, bottom = 24.dp), // reduced end padding from 24.dp to 12.dp
                contentAlignment = Alignment.Center
            ) {
                val navItems = listOf(
                    Triple("المكتبة", Icons.Default.Home) {
                        navigateTo("home")
                        SyncEngine.saveSelectedCategory(null)
                    },
                    Triple("الأخبار", Icons.Default.Newspaper) { navigateTo("news") },
                    Triple("المحفوظات", Icons.Default.Star) { navigateTo("bookmarks") },
                    Triple("المزيد", Icons.Default.Menu) { isDrawerOpen = true }
                )

                val isNavActive = listOf(page == "home" || page == "list", page == "news", page == "bookmarks", isDrawerOpen)
                var lastValidIndex by remember { mutableStateOf(0) }
                val currentIndex = if (isDrawerOpen) 3 else isNavActive.take(3).indexOfFirst { it }.takeIf { it >= 0 }
                if (currentIndex != null) {
                    lastValidIndex = currentIndex
                }
                val selectedIndex = lastValidIndex

                BoxWithConstraints(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .fillMaxHeight()
                        .width(72.dp) // reduced from 84.dp
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .liquidGlass(isDark, borderRadius = 32.dp, alpha = 0.90f)
                        .padding(vertical = 12.dp)
                ) {
                    val contentHeight = maxHeight
                    val itemHeight = contentHeight / navItems.size
                    var dragOffsetY by remember { mutableStateOf<Float?>(null) }
                    val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemHeight.toPx() }

                    val indicatorOffset by animateDpAsState(
                        targetValue = if (dragOffsetY != null) with(androidx.compose.ui.platform.LocalDensity.current) { (dragOffsetY!! - (itemHeightPx / 2)).toDp() } else itemHeight * selectedIndex,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                    )

                    Box(
                        modifier = Modifier
                            .offset(y = indicatorOffset)
                            .height(itemHeight)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()
                                        if (change != null) {
                                            if (change.pressed) {
                                                val rawY = change.position.y
                                                dragOffsetY = rawY.coerceIn(0f, size.height.toFloat())
                                            } else {
                                                // When finger lifted, trigger tab action if dragged
                                                if (dragOffsetY != null) {
                                                    val idx = (dragOffsetY!! / itemHeightPx).toInt().coerceIn(0, navItems.lastIndex)
                                                    navItems[idx].third()
                                                    dragOffsetY = null
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isActive = isNavActive[index]
                            val tint by animateColorAsState(
                                targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(250)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = item.first,
                                    tint = tint,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Slide Drawer ---
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isDrawerOpen = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .clickable(enabled = false) {}
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp))
                        .liquidGlass(isDark, borderRadius = 0.dp, alpha = 0.98f)
                        .animateEnterExit(
                            enter = slideInHorizontally { it },
                            exit = slideOutHorizontally { it }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(top = 24.dp, bottom = 12.dp)
                    ) {
                        // Top Header (now scrollable)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(20.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    parseHexColor(appSettings.primaryColor).copy(alpha = 0.8f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.app_icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "مكتبة الثالث متوسط",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = com.Nightjar.gradeiraqi3library.theme.KufiReemFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Middle Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            DrawerItem("المكتبة", Icons.Default.Home, page == "home" || page == "list", {
                                navigateTo("home")
                                SyncEngine.saveSelectedCategory(null)
                                isDrawerOpen = false
                            })
                            DrawerItem("الأخبار", Icons.Default.Newspaper, page == "news", {
                                navigateTo("news")
                                isDrawerOpen = false
                            })
                            DrawerItem("المحفوظات", Icons.Default.Star, page == "bookmarks", {
                                navigateTo("bookmarks")
                                isDrawerOpen = false
                            })
                            DrawerItem("إضافة اختصار", Icons.Default.AddCard, page == "shortcuts", {
                                navigateTo("shortcuts")
                                isDrawerOpen = false
                            })
                            DrawerItem("حول التطبيق", Icons.Default.Info, page == "about", {
                                navigateTo("about")
                                isDrawerOpen = false
                            })
                        }

                        Spacer(modifier = Modifier.weight(1f, fill = false))

                        // Bottom Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            DrawerItem("الإعدادات المظهرية", Icons.Default.Palette, isAppSettingsOpen, {
                                isAppSettingsOpen = true
                                isDrawerOpen = false
                            })
                        }
                    }
                }
            }
        }

        // Modals
        com.Nightjar.gradeiraqi3library.ui.AppSettingsModal(
            isOpen = isAppSettingsOpen,
            onClose = { isAppSettingsOpen = false },
            settings = appSettings,
            onUpdateSettings = { newSettings ->
                SyncEngine.saveSettings(newSettings)
                SyncEngine.platformActionHandler?.rescheduleBackgroundSync()
            }
        )

        com.Nightjar.gradeiraqi3library.ui.NewsSettingsModal(
            isOpen = isNewsSettingsOpen,
            onClose = { isNewsSettingsOpen = false },
            settings = appSettings,
            onUpdateSettings = { newSettings ->
                SyncEngine.saveSettings(newSettings)
                SyncEngine.platformActionHandler?.rescheduleBackgroundSync()
            },
            onRequestClearCache = {
                isNewsSettingsOpen = false
                showClearCacheDialog = true
            }
        )

        // Clear Cache Dialog Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showClearCacheDialog,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(200f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showClearCacheDialog = false },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 400.dp)
                        .clickable(enabled = false) {}
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
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "تأكيد الحذف النهائي",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "هل أنت متأكد من مسح جميع الأخبار المحفوظة؟ سيتم إفراغ الشاشة بالكامل.",
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showClearCacheDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.Button(
                                onClick = {
                                    showClearCacheDialog = false
                                    SyncEngine.clearNewsCache()
                                    SyncEngine.platformActionHandler?.showToast("تم مسح كاش الأخبار بالكامل بنجاح ✓")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("حذف الآن", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }


        // --- Onboarding Screen ---
        androidx.compose.animation.AnimatedVisibility(
            visible = showOnboarding && !showSplash,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(90f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            com.Nightjar.gradeiraqi3library.ui.OnboardingScreen(
                platformActionHandler = platformActionHandler,
                onComplete = {
                    showOnboarding = false
                    SyncEngine.completeOnboarding()
                    // If they skip, ensure we still request notification permission (as requested by user)
                    platformActionHandler.requestNotificationPermission()
                }
            )
        }

        // --- Splash Screen Overlay ---
        androidx.compose.animation.AnimatedVisibility(
            visible = showSplash,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.scaleOut(
                targetScale = 1.06f,
                animationSpec = tween(480, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(420)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            com.Nightjar.gradeiraqi3library.ui.AnimatedSplashScreen(
                onAnimationFinished = { 
                    showSplash = false 
                }
            )
        }
        
        // Clear toast immediately on navigation to prevent lingering toasts from previous screens
        LaunchedEffect(page) {
            ToastManager.clearToast()
        }

        var lastNonNullToastMessage by remember { mutableStateOf<String?>(null) }
        if (toastMessage != null) {
            lastNonNullToastMessage = toastMessage
        }

        // Global Toast Message (Floating Dynamic Pill with animateBackOutDown)
        androidx.compose.animation.AnimatedVisibility(
            visible = toastMessage != null,
            enter = (androidx.compose.animation.slideInVertically(
                initialOffsetY = { (it * 0.7f).toInt() },
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            ) + androidx.compose.animation.fadeIn(tween(200))),
            exit = (androidx.compose.animation.slideOutVertically(
                targetOffsetY = { (it * 1.3f).toInt() },
                animationSpec = tween(170, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.80f,
                animationSpec = tween(170, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            ) + androidx.compose.animation.fadeOut(tween(130))),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(bottom = if (isWide) 24.dp else 110.dp) // above bottom nav
                .zIndex(250f)
        ) {
            lastNonNullToastMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { com.Nightjar.gradeiraqi3library.ui.ToastManager.clearToast() }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { com.Nightjar.gradeiraqi3library.ui.ToastManager.clearToast() }
                            ) { change, _ -> change.consume() }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inversePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
            }
        }

@Composable
fun RailNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}





