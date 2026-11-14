package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * UI-free state holder for the "clear search text" animation (UDF / hoisting):
 * all logic & state live here; the Composable layer only renders and forwards events.
 */
class ClearSearchAnimator {
    /** True while the clear animation is running (drives visibility of X / ghost text). */
    var isClearingAnimation by mutableStateOf(false)
        private set

    /** Snapshot of the cleared text shown while it slides/fades out. */
    var displayClearText by mutableStateOf("")
        private set

    /** Horizontal slide (px) of the ghost text. Read inside graphicsLayer by the UI. */
    val clearTextSlideOffset = Animatable(0f)

    /** Opacity/scale (0..1) of the ghost text. Read inside graphicsLayer by the UI. */
    val clearTextSlideAlpha = Animatable(1f)

    /**
     * Starts the clear animation: snaps to origin, slides the text out and fades it,
     * then resets. No-op while a clear is already in flight or the text is empty.
     */
    fun startClear(scope: CoroutineScope, text: String) {
        if (text.isEmpty() || isClearingAnimation) return
        displayClearText = text
        isClearingAnimation = true
        scope.launch {
            clearTextSlideOffset.snapTo(0f)
            clearTextSlideAlpha.snapTo(1f)
            launch {
                clearTextSlideOffset.animateTo(
                    targetValue = 90f,
                    animationSpec = tween(280, easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f))
                )
            }
            launch {
                clearTextSlideAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(220, easing = LinearEasing)
                )
            }
            delay(280)
            isClearingAnimation = false
            displayClearText = ""
            clearTextSlideOffset.snapTo(0f)
            clearTextSlideAlpha.snapTo(1f)
        }
    }
}
