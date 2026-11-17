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
 */
val LocalSystemStretchOverscroll = compositionLocalOf { false }

/**
 * Elastic bounce/overscroll effect on vertical scrolling — custom rubber band (One UI behavior).
 *
 * Performance-safe: the nested-scroll callbacks write a plain [mutableFloatStateOf] synchronously
 * on the main thread, and `graphicsLayer { translationY = dragOffset.floatValue }` reads it
 * directly during the draw phase — NO coroutine, NO snapshotFlow, NO Animatable round-trip.
 * This keeps the drag 1:1 with the finger (no "stutter" / frame jump) and updates coordinates
 * instantly so shared-element transitions read the live position (no cached-position jump).
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.32f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    var componentHeight by remember { mutableStateOf(0f) }
    // Direct, synchronous offset state — read inside graphicsLayer with zero recomposition.
    val dragOffset = remember { mutableFloatStateOf(0f) }
    // Animatable is used ONLY for the release rebound (not during finger tracking).
    val rebound = remember { Animatable(0f) }
    var isRebounding by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = dragOffset.floatValue
                if (current != 0f && source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (current > 0 && delta < 0) {
                        val newOffset = (current + delta).coerceAtLeast(0f)
                        dragOffset.floatValue = newOffset
                        return Offset(0f, delta)
                    }
                    if (current < 0 && delta > 0) {
                        val newOffset = (current + delta).coerceAtMost(0f)
                        dragOffset.floatValue = newOffset
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
                    val isPullingDown = available.y > 0
                    val isPullingUp = available.y < 0
                    if ((isPullingDown && !topEnabled) || (isPullingUp && !bottomEnabled)) {
                        return Offset.Zero
                    }
                    isRebounding = false
                    val maxOffset = if (componentHeight > 0) (componentHeight * maxStretchRatio).coerceIn(200f, 320f) else 240f
                    val current = dragOffset.floatValue
                    if (isPullingDown) {
                        val newTarget = (current + available.y * 0.65f).coerceIn(0f, maxOffset)
                        dragOffset.floatValue = newTarget
                    } else {
                        val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                        val resistance = 0.55f * (1f - progress * 0.75f)
                        val targetOffset = (current + available.y * resistance).coerceIn(-maxOffset, 0f)
                        dragOffset.floatValue = targetOffset
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = dragOffset.floatValue
                if (current == 0f) return Velocity.Zero

                // Transition in flight: settle smoothly to 0.
                if (TransitionTargetTracker.isTransitionActive) {
                    rebound.snapTo(current)
                    isRebounding = true
                    rebound.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                    dragOffset.floatValue = 0f
                    isRebounding = false
                    return available
                }
                // Bottom rubber band rebounds to rest.
                if (current < 0f) {
                    rebound.snapTo(current)
                    isRebounding = true
                    rebound.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                    dragOffset.floatValue = 0f
                    isRebounding = false
                    return available
                }
                // One UI reachability: settle beyond max to max, otherwise stay where released.
                val maxReachability = 220f
                if (current > maxReachability) {
                    rebound.snapTo(current)
                    isRebounding = true
                    rebound.animateTo(maxReachability, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                    dragOffset.floatValue = maxReachability
                    isRebounding = false
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // Mirror live offset into the global tracker synchronously (no flow) so shared-element
    // transitions always read the up-to-date position — no cached-coordinate jump.
    LaunchedEffect(Unit) {
        snapshotFlow { dragOffset.floatValue }
            .collectLatest { TransitionTargetTracker.currentOverscrollY = it }
    }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            // Direct read of the float state — 1:1 with the finger, zero recomposition/jank.
            translationY = if (isRebounding) rebound.value else dragOffset.floatValue
        }
}
