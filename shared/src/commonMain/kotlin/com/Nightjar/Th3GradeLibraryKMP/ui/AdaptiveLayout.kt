package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Official Material 3 Adaptive Navigation Types:
 * - BottomNavigationBar: Compact phone portrait, foldable outer screen (< 600dp)
 * - NavigationRail: Medium tablet, unfolded foldable, desktop (600dp - 1199dp)
 * - PermanentNavigationDrawer: Large tablet, multi-monitor, Samsung DeX (>= 1200dp)
 */
enum class AppNavigationType {
    BottomNavigationBar,
    NavigationRail,
    PermanentNavigationDrawer
}

/**
 * Official Material 3 Content Strategy:
 * Separates layout logic from individual UI components.
 */
enum class AppContentLayoutStrategy {
    SinglePane,
    DualPane,
    BoundedCenter
}

/**
 * Clean architectural model holding all adaptive decisions.
 * Implements Android & Material 3 Adaptive guidelines (2026/2027 standards).
 */
@Immutable
data class AppAdaptiveConfig(
    val windowSizeClass: WindowSizeClass,
    val widthClass: WindowWidthSizeClass,
    val heightClass: WindowHeightSizeClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val isLarge: Boolean,
    val isExtraLarge: Boolean,
    val isWide: Boolean,
    val navigationType: AppNavigationType,
    val contentStrategy: AppContentLayoutStrategy,
    val maxReadingWidth: Dp,
    val maxContentWidth: Dp,
    val sideRailPaddingEnd: Dp,
    val topBarHeight: Dp
) {
    /**
     * Computes grid columns for the Library Screen
     */
    fun libraryGridCells(isGridView: Boolean): GridCells {
        return if (isGridView) {
            // Adaptive Grid with Minimum Adaptive Cell Width to prevent shrink/distortion on landscape & split-screen
            // Never shrink below comfortable touch target (155.dp min width matching standard phone portrait)
            GridCells.Adaptive(minSize = 155.dp)
        } else {
            when {
                screenWidthDp >= 1600.dp -> GridCells.Fixed(4)
                isExtraLarge || isLarge -> GridCells.Fixed(3)
                isExpanded || isMedium -> GridCells.Fixed(2)
                else -> GridCells.Fixed(1)
            }
        }
    }

    /**
     * Computes grid columns for the News Screen
     */
    fun newsGridCells(): GridCells {
        return when {
            screenWidthDp >= 1800.dp -> GridCells.Fixed(5)
            screenWidthDp >= 1400.dp -> GridCells.Fixed(4)
            isExtraLarge || isLarge -> GridCells.Fixed(3)
            isExpanded -> GridCells.Fixed(2)
            isMedium -> GridCells.Fixed(2)
            else -> GridCells.Fixed(1)
        }
    }

    /**
     * Computes grid columns for the Bookmarks Screen
     */
    fun bookmarksGridCells(): GridCells {
        return when {
            screenWidthDp >= 1800.dp -> GridCells.Fixed(5)
            screenWidthDp >= 1400.dp -> GridCells.Fixed(4)
            isExtraLarge || isLarge -> GridCells.Fixed(3)
            isExpanded || isMedium -> GridCells.Fixed(2)
            else -> GridCells.Fixed(1)
        }
    }

    /**
     * Computes grid columns for the Shortcuts Screen
     */
    fun shortcutsGridCells(): GridCells {
        return when {
            screenWidthDp >= 1800.dp -> GridCells.Fixed(6)
            screenWidthDp >= 1400.dp -> GridCells.Fixed(5)
            isExtraLarge || isLarge -> GridCells.Fixed(4)
            isExpanded -> GridCells.Fixed(3)
            isMedium -> GridCells.Fixed(2)
            else -> GridCells.Fixed(2) // Always 2 columns on phones and foldable cover display
        }
    }

    companion object {
        fun default(): AppAdaptiveConfig {
            return AppAdaptiveConfig(
                windowSizeClass = WindowSizeClass(360, 640),
                widthClass = WindowWidthSizeClass.COMPACT,
                heightClass = WindowHeightSizeClass.MEDIUM,
                screenWidthDp = 360.dp,
                screenHeightDp = 640.dp,
                isCompact = true,
                isMedium = false,
                isExpanded = false,
                isLarge = false,
                isExtraLarge = false,
                isWide = false,
                navigationType = AppNavigationType.BottomNavigationBar,
                contentStrategy = AppContentLayoutStrategy.SinglePane,
                maxReadingWidth = 840.dp,
                maxContentWidth = 1200.dp,
                sideRailPaddingEnd = 16.dp,
                topBarHeight = 48.dp
            )
        }
    }
}

val LocalAppAdaptiveConfig = staticCompositionLocalOf<AppAdaptiveConfig> {
    AppAdaptiveConfig.default()
}

/**
 * Top-level Composable that observes official WindowAdaptiveInfo,
 * calculates the AppAdaptiveConfig according to official Material 3 breakpoints,
 * and decouples layout logic from child UI components.
 */
@Composable
fun rememberAppAdaptiveConfig(): AppAdaptiveConfig {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val widthClass = windowSizeClass.windowWidthSizeClass
    val heightClass = windowSizeClass.windowHeightSizeClass

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }

    return remember(windowSizeClass, widthClass, heightClass, screenWidthDp, screenHeightDp) {
        val isAtLeastMedium = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) || screenWidthDp >= 600.dp
        val isAtLeastExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) || screenWidthDp >= 840.dp
        val isLarge = screenWidthDp in 1200.dp..1599.dp
        val isExtraLarge = screenWidthDp >= 1600.dp

        val isCompact = !isAtLeastMedium
        val isMedium = isAtLeastMedium && !isAtLeastExpanded
        val isExpanded = isAtLeastExpanded && !isLarge && !isExtraLarge

        val navType = when {
            isExtraLarge -> AppNavigationType.PermanentNavigationDrawer
            isAtLeastMedium -> AppNavigationType.NavigationRail
            else -> AppNavigationType.BottomNavigationBar
        }

        val contentStrategy = when {
            isExtraLarge || isLarge -> AppContentLayoutStrategy.BoundedCenter
            isExpanded || isMedium -> AppContentLayoutStrategy.DualPane
            else -> AppContentLayoutStrategy.SinglePane
        }

        val topBarH = if (isAtLeastMedium) 40.dp else 48.dp
        val sidePadding = if (isAtLeastMedium) 96.dp else 16.dp

        AppAdaptiveConfig(
            windowSizeClass = windowSizeClass,
            widthClass = widthClass,
            heightClass = heightClass,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isCompact = isCompact,
            isMedium = isMedium,
            isExpanded = isExpanded,
            isLarge = isLarge,
            isExtraLarge = isExtraLarge,
            isWide = isAtLeastMedium,
            navigationType = navType,
            contentStrategy = contentStrategy,
            maxReadingWidth = 840.dp,
            maxContentWidth = 1200.dp,
            sideRailPaddingEnd = sidePadding,
            topBarHeight = topBarH
        )
    }
}
