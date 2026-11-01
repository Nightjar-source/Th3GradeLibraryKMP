package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/**
 * Samsung One UI tactile reachability overscroll effect.
 * Pulling down at top gently stretches and squashes items into reach.
 * Upward bounce at the bottom is strictly disabled (bottomEnabled = false) to prevent reverse rebounds.
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.12f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = false
): Modifier = composed {
    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    if (isLowEnd) return@composed this

    var componentHeight by remember { mutableStateOf(0f) }
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
                // Strictly allow top pull-down only; never bounce upwards at the bottom!
                if (source == NestedScrollSource.UserInput && available.y > 0f && topEnabled) {
                    val maxOffset = if (componentHeight > 0) (componentHeight * maxStretchRatio).coerceAtMost(160f) else 140f
                    val current = overscrollOffset.value
                    val progress = (current / maxOffset).coerceIn(0f, 1f)
                    val resistance = 0.40f * (1f - progress * 0.75f)
                    val targetOffset = (current + available.y * resistance).coerceIn(0f, maxOffset)
                    scope.launch { overscrollOffset.snapTo(targetOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value > 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(
                            dampingRatio = 0.78f,
                            stiffness = 300f
                        )
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
    val maxOffset = if (componentHeight > 0) (componentHeight * maxStretchRatio).coerceAtMost(160f) else 140f
    val progress = (offsetVal / maxOffset).coerceIn(0f, 1f)

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            this.translationY = offsetVal
            // Tactile Samsung One UI compression/squash effect on top pull
            this.scaleX = 1f + (progress * 0.02f)
            this.scaleY = 1f - (progress * 0.012f)
            this.transformOrigin = TransformOrigin(0.5f, 0f)
        }
}
