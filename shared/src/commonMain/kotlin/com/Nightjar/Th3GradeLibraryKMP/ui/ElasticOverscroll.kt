package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** True when the platform draws its own native stretch overscroll (Android 12+ / API 31+). */
val LocalSystemStretchOverscroll = compositionLocalOf { false }

/**
 * Elastic bounce/overscroll effect with LIVE coordinates.
 *
 * Why `Modifier.offset {}` instead of `graphicsLayer { translationY }`:
 * graphicsLayer is a draw-phase transform — shared-element transitions and `boundsInRoot()`
 * still see the ORIGINAL layout position, so a pulled-down cover animates from its old top
 * position then jumps. `offset {}` runs in the placement phase, so the live scroll position is
 * reflected in layout coordinates immediately during and after the reader transition (no cache).
 *
 * Sync: the float state is written directly from the nested-scroll callbacks and read during
 * placement — 1:1 with the finger, no coroutine/recomposition per frame (no first-touch flash).
 * Animatable is only used for the release rebound, writing into the SAME float state each frame.
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.32f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    val systemStretch = LocalSystemStretchOverscroll.current
    var componentHeight by remember { mutableStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    // Plain holder — no Compose state write during drag (avoids per-frame recomposition/flash).
    val reboundJob = remember { arrayOfNulls<Job>(1) }

    fun settleTo(target: Float, spec: AnimationSpec<Float>) {
        reboundJob[0]?.cancel()
        reboundJob[0] = scope.launch {
            Animatable(offsetY.floatValue).animateTo(target, spec) {
                offsetY.floatValue = value
                TransitionTargetTracker.currentOverscrollY = value
            }
        }
    }

    val connection = remember(systemStretch, topEnabled, bottomEnabled, maxStretchRatio) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = offsetY.floatValue
                if (current != 0f && source == NestedScrollSource.UserInput) {
                    if (reboundJob[0]?.isActive == true) reboundJob[0]?.cancel()
                    val delta = available.y
                    if (current > 0 && delta < 0) {
                        val next = (current + delta).coerceAtLeast(0f)
                        offsetY.floatValue = next
                        TransitionTargetTracker.currentOverscrollY = next
                        return Offset(0f, delta)
                    }
                    if (current < 0 && delta > 0) {
                        val next = (current + delta).coerceAtMost(0f)
                        offsetY.floatValue = next
                        TransitionTargetTracker.currentOverscrollY = next
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
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                val pullingDown = available.y > 0
                val pullingUp = available.y < 0
                if ((pullingDown && !topEnabled) || (pullingUp && !bottomEnabled)) return Offset.Zero
                // Android 12+: let the system draw its native, smooth bottom stretch —
                // don't consume it (our old custom one fought the system and produced a flash).
                if (pullingUp && systemStretch) return Offset.Zero

                if (reboundJob[0]?.isActive == true) reboundJob[0]?.cancel()
                val maxOffset = if (componentHeight > 0) (componentHeight * maxStretchRatio).coerceIn(200f, 320f) else 240f
                val current = offsetY.floatValue
                val next = if (pullingDown) {
                    (current + available.y * 0.65f).coerceIn(0f, maxOffset)
                } else {
                    val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                    val resistance = 0.55f * (1f - progress * 0.75f)
                    (current + available.y * resistance).coerceIn(-maxOffset, 0f)
                }
                offsetY.floatValue = next
                TransitionTargetTracker.currentOverscrollY = next
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = offsetY.floatValue
                if (current == 0f) return Velocity.Zero
                if (TransitionTargetTracker.isTransitionActive) {
                    settleTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                    return Velocity.Zero
                }
                if (current < 0f) {
                    settleTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                    return Velocity.Zero
                }
                val maxReachability = 220f
                if (current > maxReachability) {
                    settleTo(maxReachability, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                    return Velocity.Zero
                }
                return Velocity.Zero
            }
        }
    }

    DisposableEffect(Unit) { onDispose { reboundJob[0]?.cancel() } }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(connection)
        // Placement-phase offset → shared-element transitions read the LIVE scroll position.
        .offset { IntOffset(0, offsetY.floatValue.roundToInt()) }
}
