package com.Nightjar.gradeiraqi3library.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

fun Modifier.elasticOverscroll(maxStretchRatio: Float = 0.2f): Modifier = composed {
    var componentHeight by remember { mutableStateOf(0f) }
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollOffset.value != 0f) {
                    val sign = sign(overscrollOffset.value)
                    val delta = available.y
                    if (sign != sign(delta)) {
                        // Scrolling back towards 0
                        val newOffset = overscrollOffset.value + delta * 0.5f
                        if (sign(newOffset) != sign) {
                            scope.launch { overscrollOffset.snapTo(0f) }
                            return Offset(0f, available.y - (newOffset - 0f) * 2f)
                        } else {
                            scope.launch { overscrollOffset.snapTo(newOffset) }
                            return Offset(0f, available.y)
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
                if (available.y != 0f) {
                    val maxOffset = if (componentHeight > 0) componentHeight * maxStretchRatio else 300f
                    val resistance = 1f - (abs(overscrollOffset.value) / maxOffset).coerceIn(0f, 1f)
                    val delta = available.y * 0.3f * resistance
                    val targetOffset = (overscrollOffset.value + delta).coerceIn(-maxOffset, maxOffset)
                    scope.launch { overscrollOffset.snapTo(targetOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (overscrollOffset.value != 0f) {
                    scope.launch {
                        overscrollOffset.animateTo(
                            0f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                if (available.y != 0f) {
                    val maxOffset = if (componentHeight > 0) componentHeight * maxStretchRatio else 300f
                    val delta = available.y * 0.05f
                    val targetOffset = (overscrollOffset.value + delta).coerceIn(-maxOffset, maxOffset)
                    overscrollOffset.animateTo(
                        targetOffset,
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                    )
                    overscrollOffset.animateTo(
                        0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )
                } else if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )
                }
                return androidx.compose.ui.unit.Velocity.Zero
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
