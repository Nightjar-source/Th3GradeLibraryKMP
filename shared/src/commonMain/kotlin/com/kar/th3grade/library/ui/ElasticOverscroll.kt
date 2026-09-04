package com.Nightjar.gradeiraqi3library.ui

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
 * Elastic bounce/overscroll effect on vertical scrolling.
 * Custom built smooth rubber band effect.
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.10f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    val isLowEnd = com.Nightjar.gradeiraqi3library.theme.LocalIsLowEndDevice.current
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
                    if ((current > 0 && delta < 0) || (current < 0 && delta > 0)) {
                        val newOffset = current + delta
                        return if ((current > 0 && newOffset <= 0) || (current < 0 && newOffset >= 0)) {
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
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val isPullingDown = available.y > 0 // Top edge overscroll
                    val isPullingUp = available.y < 0   // Bottom edge overscroll
                    
                    if ((isPullingDown && !topEnabled && overscrollOffset.value <= 0f) || 
                        (isPullingUp && !bottomEnabled && overscrollOffset.value >= 0f)) {
                        return Offset.Zero
                    }
                    
                    val maxOffset = if (componentHeight > 0) componentHeight * maxStretchRatio else 140f
                    val current = overscrollOffset.value
                    val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                    val resistance = 0.35f * (1f - progress * progress)
                    val targetOffset = (current + available.y * resistance).coerceIn(-maxOffset, maxOffset)
                    scope.launch { overscrollOffset.snapTo(targetOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            translationY = overscrollOffset.value
        }
}
