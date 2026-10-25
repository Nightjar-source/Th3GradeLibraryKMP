package com.Nightjar.Th3GradeLibraryKMP.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsReturningFromPdf
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalScreenEntranceKey
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalSeenEntranceItems
import com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker
import kotlinx.coroutines.delay

/**
 * Isolated Scroll and Item Entrance Animation Extensions.
 * Eliminates all list flickering, provides fluid 120Hz sliding spring motion exactly matching caliq5,
 * and completely protects against re-triggering during lazy scrolling.
 */
fun Modifier.animateItemOnScroll(index: Int = 0, itemId: String? = null): Modifier = composed {
    val isLowEnd = LocalIsLowEndDevice.current
    if (isLowEnd || index > 12) return@composed this

    val isReturningFromPdf = LocalIsReturningFromPdf.current
    val isNavigatingBack = LocalIsNavigatingBack.current
    val itemKey = itemId ?: index.toString()
    val isActiveSharedTarget = TransitionTargetTracker.isTransitionActive &&
            (TransitionTargetTracker.activeTargetId == "card_$itemKey" ||
             TransitionTargetTracker.activeTargetId == itemKey ||
             TransitionTargetTracker.activeTargetId?.startsWith("bookmark_$itemKey") == true)

    val screenKey = LocalScreenEntranceKey.current
    val seenMap = LocalSeenEntranceItems.current

    // Keep completely stationary when navigating back or on active shared hero target
    if (isReturningFromPdf || isNavigatingBack || isActiveSharedTarget) {
        seenMap?.put(itemKey, true)
        return@composed this
    }

    val alreadySeen = seenMap?.get(itemKey) == true
    if (alreadySeen) return@composed this

    val animProgress = remember(screenKey, itemKey) { Animatable(0f) }
    val density = LocalDensity.current
    val offsetYPx = remember(density) { with(density) { 24.dp.toPx() } }

    LaunchedEffect(screenKey, itemKey) {
        val staggerDelay = MotionSpecs.getStaggerDelay(index)
        if (staggerDelay > 0) delay(staggerDelay)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = MotionSpecs.itemEntranceSpring
        )
        seenMap?.put(itemKey, true)
    }

    val p = animProgress.value
    this.graphicsLayer {
        this.alpha = 0.35f + 0.65f * p
        this.translationY = (1f - p) * offsetYPx
        this.scaleX = 0.96f + 0.04f * p
        this.scaleY = 0.96f + 0.04f * p
        this.clip = false
    }
}

/**
 * Clean alias for library, shortcuts, and news grids
 */
fun Modifier.screenCascadeEntrance(index: Int = 0, itemId: String? = null): Modifier = this.animateItemOnScroll(index, itemId)
