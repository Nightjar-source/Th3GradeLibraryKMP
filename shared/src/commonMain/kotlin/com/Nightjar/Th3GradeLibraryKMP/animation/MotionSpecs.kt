package com.Nightjar.Th3GradeLibraryKMP.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset

/**
 * Clean Architecture Animation Model (120Hz Fluid Physics)
 * Contains isolated specifications for screen transitions, shared bounds, and scroll motions.
 */
object MotionSpecs {

    // 1. Organic Fluid Spring for Shared Bounds & Hero Expansions (كالجبنة السائلة)
    val sharedBoundsSpring: SpringSpec<androidx.compose.ui.geometry.Rect> = spring(
        dampingRatio = 0.82f,
        stiffness = 280f
    )

    val cornerRadiusSpring: SpringSpec<androidx.compose.ui.unit.Dp> = spring(
        dampingRatio = 0.78f,
        stiffness = 240f
    )

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

    // Navigation to/from PDF Reader
    val pdfEnterFade: TweenSpec<Float> = tween(durationMillis = 140, easing = LinearEasing)
    val pdfExitFade: TweenSpec<Float> = tween(durationMillis = 150, delayMillis = 60, easing = LinearEasing)

    // 3. Staggered Item Entrance Spec (Lazy List Cascading)
    fun getStaggerDelay(index: Int): Long {
        return (index.coerceIn(0, 8) * 22L)
    }

    val itemEntranceSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 320f
    )

    // 4. Accordion Expand/Collapse Spec (Bookmarks)
    val accordionSpring: SpringSpec<Int> = spring(
        dampingRatio = 0.82f,
        stiffness = 280f
    )
    val accordionFloatSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 320f
    )
}
