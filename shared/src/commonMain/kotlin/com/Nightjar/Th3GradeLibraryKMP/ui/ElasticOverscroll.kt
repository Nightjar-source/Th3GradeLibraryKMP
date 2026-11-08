package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Samsung One UI Reachability pull-down effect (One-Handed Operation).
 * Pulling down at the top smoothly translates content down towards the quarter of the screen
 * allowing comfortable one-handed interaction and tapping without any scale or size distortion.
 * Upward bounce at the bottom is strictly disabled.
 */
fun Modifier.elasticOverscroll(
    maxReachDistancePx: Float = 260f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = false
): Modifier = composed {
    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    if (isLowEnd) return@composed this

    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Samsung One UI Reachability: Smooth pull-down to exactly one quarter (25%) of screen height
    val quarterReachPx = remember(windowInfo.containerSize, density) {
        val screenHeightPx = windowInfo.containerSize.height.toFloat()
        with(density) {
            (screenHeightPx * 0.25f).coerceIn(150.dp.toPx(), 260.dp.toPx())
        }
    }
    val effectiveMaxReachPx = if (maxReachDistancePx > 0f && maxReachDistancePx != 260f) maxReachDistancePx else quarterReachPx

    val nestedScrollConnection = remember(topEnabled, bottomEnabled, effectiveMaxReachPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = overscrollOffset.value
                if (current > 0f && source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < 0f) {
                        val newOffset = current + delta
                        return if (newOffset <= 0f) {
                            scope.launch { overscrollOffset.snapTo(0f) }
                            Offset(0f, current)
                        } else {
                            scope.launch { overscrollOffset.snapTo(newOffset) }
                            Offset(0f, delta)
                        }
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Smoothly pull down content into exactly the quarter of the screen
                if (source == NestedScrollSource.UserInput && available.y > 0f && topEnabled) {
                    val current = overscrollOffset.value
                    val progress = (current / effectiveMaxReachPx).coerceIn(0f, 1f)
                    val resistance = 0.55f * (1f - progress * 0.50f)
                    val targetOffset = (current + available.y * resistance).coerceIn(0f, effectiveMaxReachPx)
                    scope.launch { overscrollOffset.snapTo(targetOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = overscrollOffset.value
                // Official Android Elastic Overscroll: Fluid organic spring return directly to 0 (Rubber-band physics)
                if (current > 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(dampingRatio = 0.78f, stiffness = 300f)
                    )
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(overscrollOffset.value) {
        TransitionTargetTracker.currentOverscrollY = overscrollOffset.value
    }

    val offsetVal = overscrollOffset.value

    this
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            // Pure downward translation to quarter area for one-handed reachability
            // Zero scale distortion (scaleX = 1f, scaleY = 1f) preserving crisp original elements
            this.translationY = offsetVal
        }
}
