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
import kotlinx.coroutines.flow.collectLatest

/**
 * True when the platform draws its own stretch overscroll (Android 12+ / API 31+).
 * Currently unused — kept so call sites can opt back into system stretch if desired.
 */
val LocalSystemStretchOverscroll = compositionLocalOf { false }

/**
 * Elastic bounce/overscroll effect on vertical scrolling — custom rubber band (One UI behavior).
 * Top edge AND bottom edge both use the custom stretch so the effect is guaranteed on every device
 * (including Android 12+ where platform stretch may be suppressed by nested-scroll connections).
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.32f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    var componentHeight by remember { mutableStateOf(0f) }
    val overscrollOffset = remember { Animatable(0f) }
    // Written synchronously from nestedScroll callbacks (main thread) — zero coroutine flood.
    val dragTarget = remember { mutableFloatStateOf(0f) }

    // Single collector: drag follows the finger 1:1 without spawning a coroutine per scroll event.
    LaunchedEffect(Unit) {
        snapshotFlow { dragTarget.floatValue }
            .collectLatest { target -> overscrollOffset.snapTo(target) }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = overscrollOffset.value
                if (current != 0f && source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    // Pulling up while stretched down into the reachability zone: scroll it back up first.
                    if (current > 0 && delta < 0) {
                        val newOffset = (current + delta).coerceAtLeast(0f)
                        dragTarget.floatValue = newOffset
                        return Offset(0f, delta)
                    }
                    // Pulling down while stretched upward at the bottom edge.
                    if (current < 0 && delta > 0) {
                        val newOffset = (current + delta).coerceAtMost(0f)
                        dragTarget.floatValue = newOffset
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
                    val isPullingUp = available.y < 0   // Bottom edge stretch

                    if ((isPullingDown && !topEnabled) || (isPullingUp && !bottomEnabled)) {
                        return Offset.Zero
                    }

                    val maxReachability = 220f // Brings top content comfortably to mid-screen
                    val maxOffset = if (componentHeight > 0) (componentHeight * maxStretchRatio).coerceIn(200f, 320f) else 240f
                    val current = overscrollOffset.value

                    if (isPullingDown) {
                        // Smoothly pull down toward mid-screen and keep it there without bouncing back.
                        val newTarget = (current + available.y * 0.65f).coerceIn(0f, maxOffset)
                        dragTarget.floatValue = newTarget
                        return Offset(0f, available.y)
                    } else {
                        // Bottom edge rubber-band stretch
                        val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                        val resistance = 0.55f * (1f - progress * 0.75f)
                        val targetOffset = (current + available.y * resistance).coerceIn(-maxOffset, 0f)
                        dragTarget.floatValue = targetOffset
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = overscrollOffset.value
                // When transitioning to or from PDF, reset overscroll cleanly to 0 with fluid spring.
                if (TransitionTargetTracker.isTransitionActive) {
                    dragTarget.floatValue = 0f
                    overscrollOffset.animateTo(
                        0f,
                        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    )
                    return available
                }
                // Bottom edge rubber-band rebound to 0.
                if (current < 0f) {
                    dragTarget.floatValue = 0f
                    overscrollOffset.animateTo(
                        0f,
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    return available
                }
                // If pulled beyond max reachability at the top, gently settle at maxReachability without jumping back to 0.
                val maxReachability = 220f
                if (current > maxReachability) {
                    dragTarget.floatValue = maxReachability
                    overscrollOffset.animateTo(
                        maxReachability,
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    return available
                }
                // One UI Reachability: remains pulled down at the user's release position.
                return Velocity.Zero
            }
        }
    }

    // Mirror drag position into the global tracker via a flow — not a frame-keyed LaunchedEffect.
    LaunchedEffect(Unit) {
        snapshotFlow { overscrollOffset.value }
            .collectLatest { TransitionTargetTracker.currentOverscrollY = it }
    }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            translationY = overscrollOffset.value
        }
}
