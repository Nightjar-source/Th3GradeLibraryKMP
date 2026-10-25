package com.Nightjar.Th3GradeLibraryKMP

import com.Nightjar.Th3GradeLibraryKMP.theme.expressiveButtonMorph
import com.Nightjar.Th3GradeLibraryKMP.theme.bounceClick
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
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
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import com.Nightjar.Th3GradeLibraryKMP.theme.Th3GradeTheme
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import com.Nightjar.Th3GradeLibraryKMP.theme.parseHexColor
import com.Nightjar.Th3GradeLibraryKMP.ui.*
import com.Nightjar.Th3GradeLibraryKMP.navigation.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Nightjar.Th3GradeLibraryKMP.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.*
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun App(
    platformActionHandler: PlatformActionHandler,
    systemAccentColor: String? = null,
    dynamicLightColorScheme: androidx.compose.material3.ColorScheme? = null,
    dynamicDarkColorScheme: androidx.compose.material3.ColorScheme? = null,
    initialBookId: String? = null,
    initialIsNote: Boolean = false,
    initialPage: String? = null,
    initialSearchQuery: String? = null,
    isProcessDeath: Boolean = false,
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
        dynamicColorScheme = if (appSettings.useMaterialYou) {
            if (isDarkTheme) dynamicDarkColorScheme else dynamicLightColorScheme
        } else null
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice provides (platformActionHandler?.isLowEndDevice() ?: false)
        ) {
            val isDirectShortcutLaunch = initialBookId != null || (initialPage != null && initialPage != "home")
            var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(!isDirectShortcutLaunch) }

            androidx.compose.animation.AnimatedContent(
                targetState = showSplash,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                     androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.spring(0.8f, 100f))) togetherWith
                    (androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) +
                     androidx.compose.animation.scaleOut(targetScale = 1.02f, animationSpec = androidx.compose.animation.core.tween(250)))
                },
                label = "SplashTransition"
            ) { isSplash ->
                if (isSplash) {
                    com.Nightjar.Th3GradeLibraryKMP.ui.AnimatedSplashScreen(
                        onAnimationFinished = { 
                            showSplash = false 
                        }
                    )
                } else {
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
                            isProcessDeath = isProcessDeath,
                            onIntentConsumed = onIntentConsumed
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

val LocalStableStatusBarHeight = staticCompositionLocalOf { 24.dp }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent(
    platformActionHandler: PlatformActionHandler,
    isDark: Boolean,
    initialBookId: String?,
    initialIsNote: Boolean,
    initialPage: String?,
    initialSearchQuery: String?,
    isProcessDeath: Boolean = false,
    onIntentConsumed: () -> Unit
) {
    val appSettings by SyncEngine.appSettings.collectAsState()
    val toastMessage by ToastManager.toastMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    
    // Hoisted scroll/list states for preserving positions across navigation
    val newsLazyGridState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) {
        androidx.compose.foundation.lazy.grid.LazyGridState()
    }
    val libraryHomeScrollState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) {
        androidx.compose.foundation.ScrollState(0)
    }
    val initialBookIndex = remember(initialBookId, initialIsNote) {
        if (initialBookId != null && !initialIsNote) {
            AllItems.books.indexOfFirst { it.id == initialBookId }.takeIf { it >= 0 } ?: 0
        } else 0
    }
    val initialNoteIndex = remember(initialBookId, initialIsNote) {
        if (initialBookId != null && initialIsNote) {
            AllItems.notes.indexOfFirst { it.id == initialBookId }.takeIf { it >= 0 } ?: 0
        } else 0
    }
    val libraryBooksLazyGridState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) {
        androidx.compose.foundation.lazy.grid.LazyGridState(firstVisibleItemIndex = initialBookIndex)
    }
    val libraryNotesLazyGridState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) {
        androidx.compose.foundation.lazy.grid.LazyGridState(firstVisibleItemIndex = initialNoteIndex)
    }
    val bookmarksLazyGridState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) {
        androidx.compose.foundation.lazy.grid.LazyGridState()
    }
    val shortcutsLazyGridState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) {
        androidx.compose.foundation.lazy.grid.LazyGridState()
    }
    val aboutScrollState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) {
        androidx.compose.foundation.ScrollState(0)
    }

    val viewModel: AppViewModel = viewModel()

    LaunchedEffect(isProcessDeath, initialPage, initialSearchQuery, initialBookId, initialIsNote) {
        viewModel.initLaunchState(
            isProcessDeath = isProcessDeath,
            initialPage = initialPage,
            initialSearchQuery = initialSearchQuery,
            initialBookId = initialBookId,
            initialIsNote = initialIsNote
        )
        onIntentConsumed()
    }

    val pageHistory = viewModel.pageHistory
    var page by viewModel::page
    val screenNavCounter by viewModel::screenNavCounter
    var isReturningFromPdf by viewModel::isReturningFromPdf

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
    var isNavigatingBack by viewModel::isNavigatingBack

    fun popPage(): Boolean = viewModel.popPage()

    // Connect properties to SyncEngine (Unified Persistent Storage)
    val isGridView by SyncEngine.isGridView.collectAsState()
    val selectedCategory by SyncEngine.selectedCategory.collectAsState()

    var selectedItem by viewModel::selectedItem

    LaunchedEffect(page, selectedCategory, selectedItem) {
        isBottomBarVisible = true
        accumulatedScrollY = 0f
        canCurrentPageScroll = false
    }

    LaunchedEffect(isDark, page) {
        if (page != "pdf") {
            platformActionHandler.updateSystemBars(
                isLightStatusBars = !isDark,
                isLightNavigationBars = !isDark
            )
        }
    }
    var isSearchOpen by viewModel::isSearchOpen
    var searchQuery by viewModel::searchQuery

    var showOnboarding by remember { mutableStateOf(!appSettings.isOnboardingCompleted) }

    // Modal states hoisted in AppViewModel
    var isDrawerOpen by viewModel::isDrawerOpen
    var isAppSettingsOpen by viewModel::isAppSettingsOpen
    var isNewsSettingsOpen by viewModel::isNewsSettingsOpen
    var showClearCacheDialog by viewModel::showClearCacheDialog

    fun navigateTo(newPage: String) = viewModel.navigateTo(newPage)

    // Bookmarks list from SyncEngine (Persistent Cache)
    val savedItemIds by SyncEngine.savedBookmarks.collectAsState()
    val isTransitionActive = com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive
    val activeTargetId = com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId

    val savedItems = remember(savedItemIds, isTransitionActive, isReturningFromPdf, activeTargetId) {
        val savedBookIds = savedItemIds.map { it.split(":")[0] }.toMutableSet()
        if ((isTransitionActive || isReturningFromPdf) && activeTargetId != null && activeTargetId.startsWith("bookmark_")) {
            val activeBookId = activeTargetId.substringAfter("bookmark_").substringBeforeLast("_")
            if (activeBookId.isNotEmpty()) savedBookIds.add(activeBookId)
        }
        (AllItems.books + AllItems.notes).filter { it.id in savedBookIds }
    }
    var pdfInitialPage by viewModel::pdfInitialPage

    // Centralized Unified Back Navigation Handler for ALL Platforms (Android, Desktop, Web, iOS)
    fun handleTopBarBack() {
        if (showClearCacheDialog) {
            showClearCacheDialog = false
        } else if (isDrawerOpen) {
            isDrawerOpen = false
        } else if (isNewsSettingsOpen) {
            isNewsSettingsOpen = false
        } else if (isAppSettingsOpen) {
            isAppSettingsOpen = false
        } else if (selectedItem != null) {
            isNavigatingBack = true
            viewModel.clearSelectedItem()
        } else if (page == "list") {
            isNavigatingBack = true
            navigateTo("home")
        } else {
            isNavigatingBack = true
            if (!popPage()) {
                navigateTo("home")
            }
        }
    }

    // Platform Back Handlers
    val isSettingsOpen = isAppSettingsOpen || isNewsSettingsOpen
    val backEnabled = showClearCacheDialog || isDrawerOpen || isSettingsOpen || page != "home" || selectedItem != null || pageHistory.value.size > 1
    PlatformBackHandler(enabled = backEnabled) {
        handleTopBarBack()
    }

    // Align LazyGrid position for initial book or shortcut without racing AppViewModel
    LaunchedEffect(initialBookId) {
        if (initialBookId != null) {
            val target = AllItems.books.firstOrNull { it.id == initialBookId }
                ?: AllItems.notes.firstOrNull { it.id == initialBookId }
            if (target != null) {
                val list = if (target.isNote) AllItems.notes else AllItems.books
                val targetIdx = list.indexOfFirst { it.id == target.id }
                if (targetIdx >= 0) {
                    val state = if (target.isNote) libraryNotesLazyGridState else libraryBooksLazyGridState
                    try {
                        state.scrollToItem(targetIdx)
                    } catch (_: Exception) {}
                }
            }
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

    // First time install auto-fetch & notification permission: trigger only after onboarding finish
    LaunchedEffect(showOnboarding) {
        if (!showOnboarding) {
            // Always sync when the app is fully opened by the user, regardless of background sync interval!
            scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                SyncEngine.fetchAndSync(forced = true)
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        val widthClass = when {
            screenWidth < 600.dp -> androidx.window.core.layout.WindowWidthSizeClass.COMPACT
            screenWidth < 840.dp -> androidx.window.core.layout.WindowWidthSizeClass.MEDIUM
            else -> androidx.window.core.layout.WindowWidthSizeClass.EXPANDED
        }

        val heightClass = when {
            screenHeight < 480.dp -> androidx.window.core.layout.WindowHeightSizeClass.COMPACT
            screenHeight < 900.dp -> androidx.window.core.layout.WindowHeightSizeClass.MEDIUM
            else -> androidx.window.core.layout.WindowHeightSizeClass.EXPANDED
        }

        // Real-time Material 3 Adaptive Layout:
        // A device is considered wide (side navigation rail) ONLY if:
        // 1. Phone held in landscape: (screenWidth >= 600.dp && isLandscape)
        // 2. Large tablet / foldable inner screen / desktop: (screenWidth >= 720.dp)
        // A mobile phone held in portrait is NEVER wide!
        val isWide = (screenWidth >= 600.dp) && (isLandscape || screenWidth >= 720.dp)

        val railWidth = when {
            screenHeight < 480.dp -> 52.dp // Compact navigation rail for phone landscape (M3 Adaptive Standard)
            widthClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED -> 72.dp // Official M3 expanded rail
            else -> 60.dp
        }

        // Micro/narrow window flags for pop-up, split-screen, and DeX window resizing
        val isMicroWindow = screenWidth < 360.dp
        val isNarrowWindow = screenWidth < 480.dp

        // Adaptive Grid Columns for Books & Study Notes (Strict Compose Adaptive standards):
        val libraryColumns = when {
            screenWidth < 340.dp -> 1 // Micro pop-up / 1/3 split-screen: exactly 1 column
            !isWide -> 2              // Phone portrait / compact window: exactly 2 columns!
            screenHeight < 480.dp -> 4 // Phone landscape: exactly 4 columns
            screenWidth >= 1440.dp -> 6 // Large desktop / 4K / wide monitor: 6 columns
            screenWidth >= 1080.dp -> 5 // Tablet landscape / Samsung DeX expanded (1200px+): 5 columns
            screenWidth >= 840.dp -> 4  // Tablet portrait / medium DeX: 4 columns
            else -> 3                  // Foldable inner screen / small tablet: 3 columns
        }

        val isPhoneLandscape = isLandscape && screenHeight < 480.dp
        val isExpandedScreen = widthClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED && screenHeight >= 480.dp
        val responsiveInfo = com.Nightjar.Th3GradeLibraryKMP.theme.ResponsiveLayoutInfo(
            width = screenWidth,
            height = screenHeight,
            isLandscape = isLandscape,
            isWide = isWide,
            isPhoneLandscape = isPhoneLandscape,
            widthClass = widthClass,
            heightClass = heightClass,
            gridColumns = libraryColumns,
            railWidth = railWidth
        )
        
        val currentNavBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val systemBarsInsets = WindowInsets.statusBars.union(WindowInsets.captionBar).union(WindowInsets.displayCutout)
        val currentStatusBar = systemBarsInsets.asPaddingValues().calculateTopPadding()
        
        // Dynamic bottom padding reserving space for system navigation bars, bottom bar, and standard AdMob banner height (50dp ~ 60dp) to prevent any UI overlapping
        // Dynamic bottom padding reserving space for system navigation bars, bottom bar, and official AdMob banner heights (50~60dp on phones, 90dp on tablets)
        val listBottomPadding = if (isWide) (110.dp + currentNavBar) else (160.dp + currentNavBar)

        CompositionLocalProvider(
            LocalStableStatusBarHeight provides currentStatusBar,
            com.Nightjar.Th3GradeLibraryKMP.theme.LocalResponsiveLayoutInfo provides responsiveInfo
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
                // Background Orbs (Premium Liquid M3 Aesthetics) - Layer 0
                if (platformActionHandler?.isLowEndDevice() != true) {
                    Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
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

                // Page Content (Always matches full content area edge-to-edge)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(10f)
                    ) {
                        val screenEntranceSeenMap = remember { mutableMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>>() }
                        val isAppRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                        AnimatedContent(
                            targetState = page,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val isToPdf = targetState == "pdf"
                                val isFromPdf = initialState == "pdf"

                                if (isToPdf) {
                                    (fadeIn(animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.pdfEnterFade)).togetherWith(
                                        fadeOut(animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.pdfEnterFade)
                                    ).apply {
                                        targetContentZIndex = 50f
                                    }
                                } else if (isFromPdf) {
                                    (fadeIn(animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.pdfExitFade)).togetherWith(
                                        fadeOut(animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.pdfExitFade)
                                    ).apply {
                                        targetContentZIndex = -1f
                                    }
                                } else {
                                    val direction = if (isAppRtl) -viewModel.navigationDirection else viewModel.navigationDirection

                                    (slideInHorizontally(
                                        initialOffsetX = { fullWidth -> fullWidth * direction },
                                        animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.screenSlideSpec
                                    ) + fadeIn(animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.screenFadeSpec)).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth * direction },
                                            animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.screenSlideSpec
                                        ) + fadeOut(animationSpec = com.Nightjar.Th3GradeLibraryKMP.animation.MotionSpecs.screenFadeSpec)
                                    )
                                }
                            }
                        ) { targetPage ->
                            val currentTransition = this@AnimatedContent.transition
                            LaunchedEffect(currentTransition.currentState, currentTransition.targetState, targetPage) {
                                if (currentTransition.currentState == currentTransition.targetState &&
                                    page != "pdf"
                                ) {
                                    com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.reset()
                                    viewModel.onTransitionFinished()
                                }
                            }
                            val pageContentPadding = PaddingValues(
                                end = if (isWide && targetPage in listOf("home", "list", "news", "bookmarks", "shortcuts", "about")) (railWidth + 24.dp) else 0.dp
                            )
                            val innerHorizontalInsets = if (targetPage != "pdf") WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal) else WindowInsets(0, 0, 0, 0)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(innerHorizontalInsets)
                                    .padding(pageContentPadding),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val seenKeysForScreen = remember(targetPage, screenNavCounter) {
                                    screenEntranceSeenMap.getOrPut("${targetPage}_$screenNavCounter") { androidx.compose.runtime.mutableStateMapOf() }
                                }
                                CompositionLocalProvider(
                                    LocalAnimatedVisibilityScope provides this@AnimatedContent,
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack provides isNavigatingBack,
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalSeenEntranceItems provides seenKeysForScreen,
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalScreenEntranceKey provides "${targetPage}_$screenNavCounter",
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsReturningFromPdf provides isReturningFromPdf
                                ) { saveableStateHolder.SaveableStateProvider(targetPage) {
                                when (targetPage) {
                                    "home" -> RenderHomeDestination(
                                        isGridView = isGridView,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onNavigateToCategory = {
                                            SyncEngine.saveSelectedCategory(it)
                                            navigateTo("list")
                                        },
                                        onNavigateToPdf = {
                                            viewModel.selectItem(it, SyncEngine.lastReadPages.value[it.id])
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
                                    "list" -> RenderListDestination(
                                        category = selectedCategory ?: "books",
                                        isGridView = isGridView,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onNavigateToCategory = {
                                            SyncEngine.saveSelectedCategory(it)
                                            navigateTo("list")
                                        },
                                        onNavigateToPdf = {
                                            viewModel.selectItem(it, SyncEngine.lastReadPages.value[it.id])
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
                                    "news" -> RenderNewsDestination(
                                        onOpenSettings = { isNewsSettingsOpen = true },
                                        onItemClick = { url, _ ->
                                            platformActionHandler.openCustomTab(url, appSettings.primaryColor)
                                        },
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = newsLazyGridState
                                    )
                                    "bookmarks" -> RenderBookmarksDestination(
                                        savedItems = savedItems,
                                        savedItemIds = savedItemIds,
                                        isTransitionActive = isTransitionActive,
                                        isReturningFromPdf = isReturningFromPdf,
                                        activeTargetId = activeTargetId,
                                        isDark = isDark,
                                        onNavigateToPdf = { item, pageIdx ->
                                            viewModel.selectItem(item, pageIdx, fromBookmarks = true)
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = bookmarksLazyGridState
                                    )
                                    "shortcuts" -> RenderShortcutsDestination(
                                        platformActionHandler = platformActionHandler,
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = shortcutsLazyGridState
                                    )
                                    "about" -> RenderAboutDestination(
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
                                        val currentItem = selectedItem
                                        if (currentItem != null) {
                                            RenderPdfDestination(
                                                item = currentItem,
                                                savedItemIds = savedItemIds,
                                                onToggleBookmark = { pageIdx ->
                                                    val key = "${currentItem.id}:$pageIdx"
                                                    val isNowSaved = SyncEngine.toggleBookmark(key)
                                                    if (!isNowSaved) {
                                                        platformActionHandler.showToast("تم حذف الصفحة ${pageIdx + 1} من المحفوظات")
                                                    } else {
                                                        platformActionHandler.showToast("تم حفظ الصفحة ${pageIdx + 1} في المحفوظات")
                                                    }
                                                },
                                                onClose = {
                                                    handleTopBarBack()
                                                },
                                                initialPage = pdfInitialPage,
                                                isDark = isDark,
                                                isFromBookmarks = viewModel.isFromBookmarks,
                                                sourcePage = viewModel.pdfSourcePage,
                                                isFromShortcut = viewModel.isFromShortcut
                                            )
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

                // --- Floating Top Header (App Bar + Search Bar) in Unified Non-Overlapping Column ---
                val topHeaderSharedScope = LocalSharedTransitionScope.current
                var isVoiceListening by remember { mutableStateOf(false) }

                val stopVoiceAndClearSearch = {
                    if (isVoiceListening) {
                        try {
                            platformActionHandler.stopVoiceSearch()
                        } catch (_: Exception) {}
                        isVoiceListening = false
                    }
                    searchQuery = ""
                }

                LaunchedEffect(page) {
                    if (page != "list") {
                        stopVoiceAndClearSearch()
                        isSearchOpen = false
                    }
                }

                DisposableEffect(isVoiceListening) {
                    onDispose {
                        if (isVoiceListening) {
                            try {
                                platformActionHandler.stopVoiceSearch()
                            } catch (_: Exception) {}
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .zIndex(200f)
                        .then(
                            if (topHeaderSharedScope != null) {
                                with(topHeaderSharedScope) {
                                    Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 50f)
                                }
                            } else Modifier
                        )
                ) {
                    // Floating App Bar
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !showOnboarding && page != "pdf" && page != "web",
                        enter = slideInVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeIn(tween(260)),
                        exit = slideOutVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeOut(tween(220)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    val isPhoneLandscape = isLandscape && screenHeight < 480.dp
                    val isExpandedScreen = widthClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED && screenHeight >= 480.dp
                    val animatedEndPadding by animateDpAsState(
                        targetValue = if (isWide) (railWidth + 16.dp) else if (isMicroWindow) 8.dp else 16.dp,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                        label = "topBarEndPadding"
                    )
                    val animatedStartPadding by animateDpAsState(
                        targetValue = if (isMicroWindow) 8.dp else 16.dp,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                        label = "topBarStartPadding"
                    )
                    val animatedTopPadding by animateDpAsState(
                        targetValue = when {
                            isPhoneLandscape -> 2.dp      // Phone landscape: total 38dp (< 46dp)
                            isMicroWindow -> 3.dp         // Micro window: total 42dp (< 46dp)
                            isExpandedScreen -> 8.dp      // Tablets / DeX: official M3 64dp
                            else -> 4.dp                  // Normal portrait phone: total 50dp (< 56dp)
                        },
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                        label = "topBarTopPadding"
                    )
                    val animatedElementSize by animateDpAsState(
                        targetValue = when {
                            isPhoneLandscape -> 34.dp     // Phone landscape: under 46dp
                            isMicroWindow -> 36.dp
                            isExpandedScreen -> 48.dp     // Tablets / DeX: standard
                            else -> 42.dp                 // Normal portrait phone: under 56dp (42dp + 8dp padding = 50dp)
                        },
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                        label = "topBarElementSize"
                    )

                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.captionBar).union(WindowInsets.displayCutout))
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(
                                start = animatedStartPadding,
                                end = animatedEndPadding,
                                top = animatedTopPadding,
                                bottom = animatedTopPadding
                            )
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Separate Floating Circular Back Button at Start Edge (caliq5 style)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = (page != "home"),
                            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f)),
                            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.75f, animationSpec = tween(180)),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(animatedElementSize)
                                    .liquidGlass(isDark, borderRadius = 22.dp, alpha = 0.92f)
                                    .clip(CircleShape)
                                    .bounceClick { handleTopBarBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(if (isPhoneLandscape) 18.dp else if (isMicroWindow) 20.dp else if (isWide) 22.dp else 26.dp)
                                )
                            }
                        }

                        // App Icon on Home Screen at Start Edge
                        androidx.compose.animation.AnimatedVisibility(
                            visible = (page == "home"),
                            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = tween(220)),
                            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.75f, animationSpec = tween(200)),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(animatedElementSize)
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
                                    painter = painterResource(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.drawable.app_icon),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(if (isMicroWindow) 24.dp else 28.dp).clip(CircleShape)
                                )
                            }
                        }

                        // 2. Separate Floating Oval Title Capsule in the Center with Dynamic Safe Margins
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

                        val titleStartMargin = if (isWide) 110.dp else (animatedElementSize + (if (isMicroWindow) 6.dp else 12.dp))
                        val titleEndMargin = if (isWide) {
                            110.dp
                        } else {
                            when (page) {
                                "list" -> animatedElementSize * 2 + (if (isMicroWindow) 12.dp else 20.dp)
                                "news" -> animatedElementSize + (if (isMicroWindow) 6.dp else 12.dp)
                                else -> animatedElementSize + (if (isMicroWindow) 6.dp else 12.dp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(start = titleStartMargin, end = titleEndMargin)
                                .height(animatedElementSize)
                                .wrapContentWidth()
                                .widthIn(max = 420.dp)
                                .liquidGlass(isDark, borderRadius = 24.dp, alpha = 0.92f)
                                .clip(CircleShape)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Touch shield */ }
                                .animateContentSize(spring(dampingRatio = 0.76f, stiffness = 320f))
                                .padding(horizontal = if (isMicroWindow) 8.dp else if (isWide) 16.dp else 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    fontSize = if (isPhoneLandscape) 12.sp else if (isMicroWindow) 12.sp else if (isWide) 15.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (targetTitle == "مكتبة الثالث متوسط") com.Nightjar.Th3GradeLibraryKMP.theme.KufiReemFontFamily else null,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 3. Floating Action Controls at End Edge (End of Header)
                        AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f)) + fadeIn(tween(220)))
                                    .togetherWith(scaleOut(targetScale = 0.8f, animationSpec = tween(180)) + fadeOut(tween(180)))
                                    .using(SizeTransform(clip = false))
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                            label = "topBarActions"
                        ) { targetPage ->
                            if (targetPage == "list") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Separate Floating Circle 1: Grid / List View Toggle with fluid liquid rotation
                                    val gridRotation by animateFloatAsState(
                                        targetValue = if (isGridView) 180f else 0f,
                                        animationSpec = spring(dampingRatio = 0.74f, stiffness = 320f),
                                        label = "gridRotation"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(animatedElementSize)
                                            .liquidGlass(isDark, borderRadius = 22.dp, alpha = 0.92f)
                                            .clip(CircleShape)
                                            .bounceClick { SyncEngine.saveGridView(!isGridView) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.animation.AnimatedContent(
                                            targetState = isGridView,
                                            transitionSpec = {
                                                (fadeIn(tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(0.74f, 320f)))
                                                    .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160)))
                                            },
                                            label = "gridToggle"
                                        ) { isGrid ->
                                            Icon(
                                                imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                                contentDescription = "Toggle Grid/List view",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .size(if (isWide) 20.dp else 22.dp)
                                                    .graphicsLayer { rotationZ = gridRotation }
                                            )
                                        }
                                    }

                                    // Separate Floating Circle 2: Search Toggle with fluid liquid morph
                                    val searchRotation by animateFloatAsState(
                                        targetValue = if (isSearchOpen) 90f else 0f,
                                        animationSpec = spring(dampingRatio = 0.74f, stiffness = 320f),
                                        label = "searchRotation"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(animatedElementSize)
                                            .liquidGlass(isDark, borderRadius = 22.dp, alpha = 0.92f)
                                            .clip(CircleShape)
                                            .bounceClick {
                                                if (isSearchOpen) {
                                                    stopVoiceAndClearSearch()
                                                    isSearchOpen = false
                                                } else {
                                                    stopVoiceAndClearSearch()
                                                    isSearchOpen = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.animation.AnimatedContent(
                                            targetState = isSearchOpen,
                                            transitionSpec = {
                                                (fadeIn(tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(0.74f, 320f)))
                                                    .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160)))
                                            },
                                            label = "searchToggle"
                                        ) { isOpen ->
                                            Icon(
                                                imageVector = if (isOpen) Icons.Default.Close else Icons.Default.Search,
                                                contentDescription = "Toggle Search",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .size(if (isWide) 20.dp else 22.dp)
                                                    .graphicsLayer { rotationZ = searchRotation }
                                            )
                                        }
                                    }
                                }
                            } else if (targetPage == "news") {
                                Box(
                                    modifier = Modifier
                                        .size(animatedElementSize)
                                        .liquidGlass(isDark, borderRadius = 22.dp, alpha = 0.92f)
                                        .clip(CircleShape)
                                        .bounceClick { isNewsSettingsOpen = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                    // Floating Search Bar (Naturally nested below App Bar in Column)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !showOnboarding && page == "list" && isSearchOpen,
                        enter = slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val animatedSearchBarHeight by animateDpAsState(
                            targetValue = when {
                                isPhoneLandscape -> 36.dp // Phone landscape: compact under 46dp
                                isMicroWindow -> 40.dp
                                isExpandedScreen -> 54.dp // Tablets / DeX: official M3 SearchBar
                                else -> 46.dp             // Normal portrait phone: sleek under 56dp
                            },
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                            label = "searchBarHeight"
                        )
                        val animatedSearchBarEndPadding by animateDpAsState(
                            targetValue = if (isWide) (railWidth + 16.dp) else if (isMicroWindow) 8.dp else 16.dp,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                            label = "searchBarEndPadding"
                        )
                        val animatedSearchBarStartPadding by animateDpAsState(
                            targetValue = if (isMicroWindow) 8.dp else 16.dp,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                            label = "searchBarStartPadding"
                        )
                        val searchFontSize = if (isPhoneLandscape || isMicroWindow) 12.sp else if (isWide) 14.sp else 15.sp

                        var isClearingAnimation by remember { mutableStateOf(false) }
                        var displayClearText by remember { mutableStateOf("") }
                        val clearTextSlideOffset = remember { androidx.compose.animation.core.Animatable(0f) }
                        val clearTextSlideAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
                        
                        val pulseAnim = animateFloatAsState(
                            targetValue = if (isVoiceListening) 1.2f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                                .padding(
                                    top = 4.dp,
                                    bottom = 8.dp,
                                    start = animatedSearchBarStartPadding,
                                    end = animatedSearchBarEndPadding
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 608.dp)
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = animatedSearchBarHeight)
                                .wrapContentHeight()
                                .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.95f)
                                .clip(RoundedCornerShape(28.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Touch shield */ }
                                .animateContentSize(spring(dampingRatio = 0.65f, stiffness = 180f))
                                .padding(horizontal = 16.dp, vertical = if (isWide) 4.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(if (isWide) 20.dp else 24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = if (isVoiceListening) "جاري الاستماع..." else searchQuery,
                                    onValueChange = { if (!isVoiceListening && !isClearingAnimation) searchQuery = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = searchFontSize
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    readOnly = isVoiceListening || isClearingAnimation,
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = searchQuery.isEmpty() && !isVoiceListening && !isClearingAnimation,
                                                enter = androidx.compose.animation.fadeIn(animationSpec = tween(220, delayMillis = 150)),
                                                exit = androidx.compose.animation.fadeOut(animationSpec = tween(120))
                                            ) {
                                                Text(
                                                    text = "ابحث عن ملزمة أو كتاب...",
                                                    style = LocalTextStyle.current.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        fontSize = searchFontSize
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (!isClearingAnimation) {
                                                innerTextField()
                                            }
                                        }
                                    }
                                )
                                if (isClearingAnimation && displayClearText.isNotEmpty()) {
                                    Text(
                                        text = displayClearText,
                                        style = LocalTextStyle.current.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = searchFontSize
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.graphicsLayer {
                                            translationX = clearTextSlideOffset.value
                                            alpha = clearTextSlideAlpha.value
                                            scaleX = clearTextSlideAlpha.value
                                            scaleY = clearTextSlideAlpha.value
                                        }
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    if (!isVoiceListening) {
                                        isVoiceListening = true
                                        platformActionHandler.startVoiceSearch(
                                            onResult = { text ->
                                                searchQuery = text
                                            },
                                            onEnd = {
                                                isVoiceListening = false
                                            }
                                        )
                                    } else {
                                        platformActionHandler.stopVoiceSearch()
                                        isVoiceListening = false
                                    }
                                },
                                modifier = Modifier
                                    .size(if (isWide) 36.dp else 40.dp)
                                    .graphicsLayer {
                                        val s = if (isVoiceListening) pulseAnim.value else 1f
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .background(
                                        color = if (isVoiceListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                androidx.compose.animation.AnimatedContent(
                                    targetState = isVoiceListening,
                                    transitionSpec = {
                                        (androidx.compose.animation.scaleIn(initialScale = 0.7f) + androidx.compose.animation.fadeIn()) togetherWith
                                        (androidx.compose.animation.scaleOut(targetScale = 1.3f) + androidx.compose.animation.fadeOut())
                                    }
                                ) { listening ->
                                    Icon(
                                        imageVector = if (listening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                        contentDescription = "Voice search",
                                        tint = if (listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(if (isWide) 20.dp else 24.dp)
                                    )
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = (searchQuery.isNotEmpty() || isClearingAnimation) && !isVoiceListening,
                                enter = androidx.compose.animation.fadeIn(animationSpec = tween(220)) + 
                                        androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)) +
                                        androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f)),
                                exit = androidx.compose.animation.fadeOut(animationSpec = tween(200)) + 
                                       androidx.compose.animation.scaleOut(targetScale = 0.5f, animationSpec = tween(200)) + 
                                       androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)) +
                                       androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(200))
                            ) {
                                val clearSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                IconButton(
                                    onClick = {
                                        val textToClear = searchQuery
                                        if (textToClear.isNotEmpty() && !isClearingAnimation) {
                                            displayClearText = textToClear
                                            isClearingAnimation = true
                                            searchQuery = ""
                                            scope.launch {
                                                clearTextSlideOffset.snapTo(0f)
                                                clearTextSlideAlpha.snapTo(1f)
                                                launch {
                                                    clearTextSlideOffset.animateTo(
                                                        targetValue = 90f,
                                                        animationSpec = tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0.2f, 1f))
                                                    )
                                                }
                                                launch {
                                                    clearTextSlideAlpha.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = tween(220, easing = androidx.compose.animation.core.LinearEasing)
                                                    )
                                                }
                                                kotlinx.coroutines.delay(280)
                                                isClearingAnimation = false
                                                displayClearText = ""
                                                clearTextSlideOffset.snapTo(0f)
                                                clearTextSlideAlpha.snapTo(1f)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(if (isWide) 36.dp else 40.dp)
                                        .expressiveButtonMorph(
                                            restRadius = if (isWide) 18.dp else 20.dp,
                                            pressedRadius = 8.dp,
                                            interactionSource = clearSource
                                        ),
                                    interactionSource = clearSource
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(if (isWide) 18.dp else 22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                }



        // --- Mobile Bottom Navigation Capsule (Only visible on small screens and main tabs) ---
        val mainTabs = listOf("home", "list", "news", "bookmarks", "shortcuts", "about")
        androidx.compose.animation.AnimatedVisibility(
            visible = !showOnboarding && page in mainTabs && !isWide,
            enter = slideInVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { it } + fadeIn(tween(260)),
            exit = slideOutVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().zIndex(200f).then(
                if (LocalSharedTransitionScope.current != null) {
                    with(LocalSharedTransitionScope.current!!) {
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 50f)
                    }
                } else Modifier
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .offset { androidx.compose.ui.unit.IntOffset(x = 0, y = bottomBarOffsetHeightPx.roundToInt()) }
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val mainNavItems = listOf(
                    Triple("المكتبة", Icons.Default.Home) {
                        navigateTo("home")
                        SyncEngine.saveSelectedCategory(null)
                    },
                    Triple("الأخبار", Icons.Default.Newspaper) { navigateTo("news") },
                    Triple("المحفوظات", Icons.Default.Star) { navigateTo("bookmarks") }
                )

                val isMainActive = listOf(page == "home" || page == "list", page == "news", page == "bookmarks")
                var lastValidIndex by remember { mutableStateOf(0) }
                val currentIndex = isMainActive.indexOfFirst { it }.takeIf { it >= 0 }
                if (currentIndex != null) {
                    lastValidIndex = currentIndex
                }
                val selectedIndex = lastValidIndex
                val isMenuButtonActive = isDrawerOpen || page == "about" || page == "shortcuts"

                val bottomBarHeight = if (screenHeight < 560.dp || isMicroWindow) 56.dp else 64.dp
                Row(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .height(bottomBarHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 1. Elongated Main Pill with 3 Tabs & Sliding Indicator
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 32.dp, alpha = 0.90f)
                            .padding(horizontal = 6.dp)
                    ) {
                        val contentWidth = maxWidth
                        val itemWidth = contentWidth / mainNavItems.size
                        var dragOffset by remember { mutableStateOf<Float?>(null) }
                        val itemWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemWidth.toPx() }

                        val indicatorOffset by animateDpAsState(
                            targetValue = if (dragOffset != null) with(androidx.compose.ui.platform.LocalDensity.current) { (dragOffset!! - (itemWidthPx / 2)).toDp() } else itemWidth * selectedIndex,
                            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
                        )

                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(itemWidth)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp, horizontal = 6.dp)
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
                                                    if (dragOffset != null) {
                                                        val idx = (dragOffset!! / itemWidthPx).toInt().coerceIn(0, mainNavItems.lastIndex)
                                                        mainNavItems[idx].third()
                                                        dragOffset = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            mainNavItems.forEachIndexed { index, item ->
                                val isActive = selectedIndex == index
                                val tint by animateColorAsState(
                                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(250)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(20.dp)),
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

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. Separate Squircle Menu Button ("القائمة") matching bar item proportions
                    val menuTint by animateColorAsState(
                        targetValue = if (isMenuButtonActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(250)
                    )
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .width(56.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.90f)
                            .clip(RoundedCornerShape(28.dp))
                            .bounceClick {
                                isDrawerOpen = !isDrawerOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMenuButtonActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
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
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = menuTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "القائمة",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = menuTint
                            )
                        }
                    }
                }
            }
        }
        
        // --- Tablet/Wide Floating Side Navigation Capsule ---
        androidx.compose.animation.AnimatedVisibility(
            visible = !showOnboarding && page in mainTabs && isWide,
            enter = slideInHorizontally(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeIn(tween(260)),
            exit = slideOutHorizontally(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().zIndex(250f).then(
                if (LocalSharedTransitionScope.current != null) {
                    with(LocalSharedTransitionScope.current!!) {
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 60f)
                    }
                } else Modifier
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(end = 12.dp, top = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val mainNavItems = listOf(
                    Triple("المكتبة", Icons.Default.Home) {
                        navigateTo("home")
                        SyncEngine.saveSelectedCategory(null)
                    },
                    Triple("الأخبار", Icons.Default.Newspaper) { navigateTo("news") },
                    Triple("المحفوظات", Icons.Default.Star) { navigateTo("bookmarks") }
                )

                val isMainActive = listOf(page == "home" || page == "list", page == "news", page == "bookmarks")
                var lastValidIndex by remember { mutableStateOf(0) }
                val currentIndex = isMainActive.indexOfFirst { it }.takeIf { it >= 0 }
                if (currentIndex != null) {
                    lastValidIndex = currentIndex
                }
                val selectedIndex = lastValidIndex
                val isMenuButtonActive = isDrawerOpen || page == "about" || page == "shortcuts"

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 1. Vertical 3-tab pill
                    BoxWithConstraints(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .weight(1f, fill = false)
                            .width(railWidth)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.90f)
                            .padding(vertical = 8.dp)
                    ) {
                        val contentHeight = maxHeight
                        val itemHeight = contentHeight / mainNavItems.size
                        var dragOffsetY by remember { mutableStateOf<Float?>(null) }
                        val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemHeight.toPx() }

                        val indicatorOffset by animateDpAsState(
                            targetValue = if (dragOffsetY != null) with(androidx.compose.ui.platform.LocalDensity.current) { (dragOffsetY!! - (itemHeightPx / 2)).toDp() } else itemHeight * selectedIndex,
                            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
                        )

                        Box(
                            modifier = Modifier
                                .offset(y = indicatorOffset)
                                .height(itemHeight)
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp)
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
                                                    if (dragOffsetY != null) {
                                                        val idx = (dragOffsetY!! / itemHeightPx).toInt().coerceIn(0, mainNavItems.lastIndex)
                                                        mainNavItems[idx].third()
                                                        dragOffsetY = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            mainNavItems.forEachIndexed { index, item ->
                                val isActive = selectedIndex == index
                                val tint by animateColorAsState(
                                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Separate Squircle Menu Button for Tablet ("القائمة") matching bottom bar sleek design & railWidth
                    val sideMenuTint by animateColorAsState(
                        targetValue = if (isMenuButtonActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(250)
                    )
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .width(railWidth)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.90f)
                            .clip(RoundedCornerShape(28.dp))
                            .bounceClick {
                                isDrawerOpen = !isDrawerOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMenuButtonActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
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
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = sideMenuTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "القائمة",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = sideMenuTint
                            )
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
            modifier = Modifier.zIndex(500f).then(
                if (LocalSharedTransitionScope.current != null) {
                    with(LocalSharedTransitionScope.current!!) {
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 100f)
                    }
                } else Modifier
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isDrawerOpen = false }
            ) {
                val drawerWidth = if (responsiveInfo.isPhoneLandscape) 330.dp else 300.dp
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
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
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(24.dp)
                                )
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
                                    fontFamily = com.Nightjar.Th3GradeLibraryKMP.theme.KufiReemFontFamily
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
        com.Nightjar.Th3GradeLibraryKMP.ui.AppSettingsModal(
            isOpen = isAppSettingsOpen,
            onClose = { isAppSettingsOpen = false },
            settings = appSettings,
            onUpdateSettings = { newSettings ->
                SyncEngine.saveSettings(newSettings)
                SyncEngine.platformActionHandler?.rescheduleBackgroundSync()
            }
        )

        com.Nightjar.Th3GradeLibraryKMP.ui.NewsSettingsModal(
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

        // Clear Cache Dialog Overlay (Material 3 Expressive System Dialog)
        androidx.compose.animation.AnimatedVisibility(
            visible = showClearCacheDialog,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(600f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.captionBar).union(WindowInsets.displayCutout))
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showClearCacheDialog = false },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
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
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
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
                            tint = Color(0xFFDC2626), // Material 3 Expressive Vivid Red
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "تأكيد الحذف النهائي",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            fontSize = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "هل أنت متأكد من مسح جميع الأخبار المحفوظة؟ سيتم إفراغ الشاشة بالكامل.",
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                onClick = { showClearCacheDialog = false },
                                modifier = Modifier.weight(1f),
                                restRadius = 24.dp,
                                pressedRadius = 8.dp,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "إلغاء",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                )
                            }
                            com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                onClick = {
                                    showClearCacheDialog = false
                                    SyncEngine.clearNewsCache()
                                    platformActionHandler.clearDiskCache()
                                    platformActionHandler.showToast("تم مسح كاش التطبيق والملفات المؤقتة بنجاح ✓")
                                },
                                modifier = Modifier.weight(1f),
                                restRadius = 24.dp,
                                pressedRadius = 8.dp,
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "حذف الآن",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                )
                            }
                        }
                    }
                }
            }
        }


        // --- Onboarding Screen ---
        androidx.compose.animation.AnimatedVisibility(
            visible = showOnboarding,
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
                .zIndex(200f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            com.Nightjar.Th3GradeLibraryKMP.ui.OnboardingScreen(
                platformActionHandler = platformActionHandler,
                onComplete = {
                    showOnboarding = false
                    SyncEngine.completeOnboarding()
                    // If they skip, ensure we still request notification permission (as requested by user)
                    platformActionHandler.requestNotificationPermission()
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
                initialOffsetY = { (it * 0.5f).toInt() },
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.expandHorizontally(
                expandFrom = Alignment.CenterHorizontally,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.fadeIn(tween(200))),
            exit = (androidx.compose.animation.slideOutVertically(
                targetOffsetY = { (it * 0.8f).toInt() },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.4f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.shrinkHorizontally(
                shrinkTowards = Alignment.CenterHorizontally,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.fadeOut(tween(150))),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(bottom = if (page == "pdf") 96.dp else if (page == "web") 24.dp else if (isWide) 24.dp else 100.dp)
                .zIndex(250f)
        ) {
            lastNonNullToastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 16.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 320f
                            ),
                            alignment = Alignment.Center
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.clearToast() }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.clearToast() }
                            ) { change, _ -> change.consume() }
                        }
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = msg,
                        transitionSpec = {
                            (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 250f)) { it / 2 } + fadeIn(tween(160)))
                                .togetherWith(slideOutVertically(tween(140)) { -it / 2 } + fadeOut(tween(140)))
                        },
                        contentAlignment = Alignment.Center,
                        label = "toastTextContent"
                    ) { targetMsg ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 14.dp),
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
                                text = targetMsg,
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
    val responsiveInfo = com.Nightjar.Th3GradeLibraryKMP.theme.LocalResponsiveLayoutInfo.current
    val isPhoneLandscape = responsiveInfo.isPhoneLandscape
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = if (isPhoneLandscape) 12.dp else 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(if (isPhoneLandscape) 10.dp else 14.dp))
        Text(
            text = label,
            fontSize = if (isPhoneLandscape) 14.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}





