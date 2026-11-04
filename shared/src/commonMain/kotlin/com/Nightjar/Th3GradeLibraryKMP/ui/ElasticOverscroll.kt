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
import kotlinx.coroutines.launch

/**
 * Samsung One UI Reachability pull-down effect (One-Handed Operation).
 * Pulling down at the top smoothly translates content down towards the middle of the screen
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

    val nestedScrollConnection = remember(topEnabled, bottomEnabled) {
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
                // Smoothly pull down content from top into the middle of the screen
                if (source == NestedScrollSource.UserInput && available.y > 0f && topEnabled) {
                    val current = overscrollOffset.value
                    val progress = (current / maxReachDistancePx).coerceIn(0f, 1f)
                    // Natural fluid dampening: easy at start, gently resists near max reach
                    val resistance = 0.55f * (1f - progress * 0.50f)
                    val targetOffset = (current + available.y * resistance).coerceIn(0f, maxReachDistancePx)
                    scope.launch { overscrollOffset.snapTo(targetOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = overscrollOffset.value
                // Samsung One UI Reachability: if pulled past 35% towards center, stay at reach position
                if (current > maxReachDistancePx * 0.35f && available.y >= -100f) {
                    overscrollOffset.animateTo(
                        maxReachDistancePx,
                        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    )
                    return available
                } else if (current > 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
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
            // Pure downward translation to center area for one-handed reachability
            // Zero scale distortion (scaleX = 1f, scaleY = 1f) preserving crisp original elements
            this.translationY = offsetVal
        }
}
