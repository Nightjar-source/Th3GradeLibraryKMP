package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
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
 * Elastic bounce/overscroll effect on vertical scrolling.
 * Custom built smooth rubber band effect.
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.32f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    if (isLowEnd) return@composed this

    var componentHeight by remember { mutableStateOf(0f) }
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = overscrollOffset.value
                if (current != 0f && source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    // If pulled down into Reachability zone (current > 0) and user scrolls up (delta < 0),
                    // smoothly scroll the reachability zone back up to 0 first.
                    if (current > 0 && delta < 0) {
                        val newOffset = (current + delta).coerceAtLeast(0f)
                        scope.launch { overscrollOffset.snapTo(newOffset) }
                        return Offset(0f, delta)
                    }
                    // If stretched upward at bottom edge (current < 0) and user scrolls down (delta > 0)
                    if (current < 0 && delta > 0) {
                        val newOffset = (current + delta).coerceAtMost(0f)
                        scope.launch { overscrollOffset.snapTo(newOffset) }
                        return Offset(0f, delta)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val isPullingDown = available.y > 0 // Top edge pull (One UI Reachability)
                    val isPullingUp = available.y < 0   // Bottom edge overscroll stretch
                    
                    if ((isPullingDown && !topEnabled) || (isPullingUp && !bottomEnabled)) {
                        return Offset.Zero
                    }
                    
                    val maxReachability = 220f // Brings top content comfortably to mid-screen
                    val maxOffset = if (componentHeight > 0) (componentHeight * maxStretchRatio).coerceIn(200f, 320f) else 240f
                    val current = overscrollOffset.value

                    if (isPullingDown) {
                        // Smoothly pull down towards mid-screen and stay there without spring rebound
                        val newTarget = (current + available.y * 0.65f).coerceIn(0f, maxOffset)
                        scope.launch { overscrollOffset.snapTo(newTarget) }
                        return Offset(0f, available.y)
                    } else {
                        // Bottom edge rubber-band stretch
                        val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                        val resistance = 0.55f * (1f - progress * 0.75f)
                        val targetOffset = (current + available.y * resistance).coerceIn(-maxOffset, 0f)
                        scope.launch { overscrollOffset.snapTo(targetOffset) }
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = overscrollOffset.value
                // Bottom edge stretch rebounds back to 0.
                if (current < 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    return available
                }
                // If pulled beyond max reachability at the top, gently settle at maxReachability without jumping back to 0
                val maxReachability = 220f
                if (current > maxReachability) {
                    overscrollOffset.animateTo(
                        maxReachability,
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    return available
                }
                // One UI Reachability: remains pulled down at user's release position (ينزل ولا يرتد لأعلى)
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(overscrollOffset.value) {
        TransitionTargetTracker.currentOverscrollY = overscrollOffset.value
    }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            translationY = overscrollOffset.value
        }
}
