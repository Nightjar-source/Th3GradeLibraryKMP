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
 * One UI Reachability + native rubber-band overscroll, with LIVE coordinates.
 *
 * - Pulling DOWN at the top edge drags the whole content toward the middle of the screen
 *   (one-handed reachability) and stays there on release; drag back up to dismiss.
 * - Pulling UP at the bottom edge is left to Android 12+ native stretch (smooth, no flash);
 *   on older Android a custom rubber band is applied.
 * - Uses `Modifier.offset{}` (placement phase) instead of `graphicsLayer` so shared-element
 *   transitions and boundsInRoot() read the LIVE scroll position immediately — no cached jump.
 */
fun Modifier.elasticOverscroll(
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    val systemStretch = LocalSystemStretchOverscroll.current
    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    if (isLowEnd) return@composed this

    var componentHeight by remember { mutableStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
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

    val connection = remember(systemStretch, topEnabled, bottomEnabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = offsetY.floatValue
                if (current > 0f && source == NestedScrollSource.UserInput && available.y < 0f) {
                    // Finger moving up while pulled down → unwind the reachability offset first.
                    if (reboundJob[0]?.isActive == true) reboundJob[0]?.cancel()
                    val next = (current + available.y).coerceAtLeast(0f)
                    offsetY.floatValue = next
                    TransitionTargetTracker.currentOverscrollY = next
                    return Offset(0f, available.y)
                }
                if (current < 0f && source == NestedScrollSource.UserInput && available.y > 0f) {
                    if (reboundJob[0]?.isActive == true) reboundJob[0]?.cancel()
                    val next = (current + available.y).coerceAtMost(0f)
                    offsetY.floatValue = next
                    TransitionTargetTracker.currentOverscrollY = next
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                val pullingDown = available.y > 0f
                val pullingUp = available.y < 0f

                if (pullingUp) {
                    // Bottom edge: Android 12+ native stretch owns it — don't consume.
                    if (systemStretch) return Offset.Zero
                    if (!bottomEnabled) return Offset.Zero
                }
                if (pullingDown && !topEnabled) return Offset.Zero

                if (reboundJob[0]?.isActive == true) reboundJob[0]?.cancel()
                val current = offsetY.floatValue
                val next = if (pullingDown) {
                    // One UI Reachability: pull toward middle of the screen (up to ~42% height),
                    // with resistance that eases as it approaches the target.
                    val reachTarget = if (componentHeight > 0) componentHeight * 0.42f else 320f
                    val progress = (current / reachTarget).coerceIn(0f, 1f)
                    val resistance = 0.55f * (1f - progress * 0.7f)
                    (current + available.y * resistance).coerceIn(0f, reachTarget)
                } else {
                    // Custom bottom rubber band (only when system stretch is absent).
                    val maxOffset = if (componentHeight > 0) componentHeight * 0.12f else 120f
                    val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                    val resistance = 0.4f * (1f - progress * 0.75f)
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
                    // Bottom rubber band springs back to rest.
                    settleTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                    return Velocity.Zero
                }
                // One UI Reachability: a quick upward fling snaps content back to rest so it
                // never disappears behind the top bar; a slow release keeps it where dropped.
                if (available.y < -700f) {
                    settleTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    DisposableEffect(Unit) { onDispose { reboundJob[0]?.cancel() } }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(connection)
        // Placement-phase offset → live coordinates for reader/shared-element transitions.
        .offset { IntOffset(0, offsetY.floatValue.roundToInt()) }
}
