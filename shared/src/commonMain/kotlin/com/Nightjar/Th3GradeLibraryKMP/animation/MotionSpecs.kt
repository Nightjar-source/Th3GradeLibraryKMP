package com.Nightjar.Th3GradeLibraryKMP.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Clean Architecture Animation Model (120Hz Fluid Physics)
 * Advanced isolated specifications for screen transitions, shared bounds, and scroll motions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
object MotionSpecs {

    // 1. Hero / Shared Element Spring (NoBouncy prevents edge vibration and visual glitching)
    val sharedElementSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val sharedBoundsSpring: SpringSpec<Rect> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val cornerRadiusSpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val boundsTransform = BoundsTransform { _: androidx.compose.ui.unit.DpRect, _: androidx.compose.ui.unit.DpRect ->
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    // 2. Screen Navigation Spec (App.kt AnimatedContent)
    val fluidEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val screenSlideSpec: TweenSpec<IntOffset> = tween(
        durationMillis = 350,
        easing = fluidEasing
    )
    val screenFadeSpec: TweenSpec<Float> = tween(
        durationMillis = 260,
        easing = FastOutSlowInEasing
    )

    // Navigation to/from PDF Reader (Smooth Hero Fade)
    val pdfEnterFade: TweenSpec<Float> = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    val pdfExitFade: TweenSpec<Float> = tween(durationMillis = 300, easing = FastOutSlowInEasing)

    // 3. Staggered Item Entrance Spec (Lazy List Cascading from Visible Viewport)
    fun getStaggerDelay(index: Int): Long {
        return (index.coerceIn(0, 8) * 36L).coerceAtMost(240L)
    }

    val itemEntranceSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessLow
    )

    // 4. Accordion Expand/Collapse Spec (Bookmarks)
    val accordionSpring: SpringSpec<Int> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val accordionFloatSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
