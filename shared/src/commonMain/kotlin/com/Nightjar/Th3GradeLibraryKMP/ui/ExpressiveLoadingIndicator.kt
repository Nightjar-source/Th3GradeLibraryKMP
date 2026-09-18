package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Loading Indicator – Floating blob morph.
 *
 * How it works:
 *  - Draw a closed smooth path using many sample points around a circle.
 *  - Each point's radius is: baseR + amplitude * sin(numWaves * angle + wavePhase)
 *    This makes the circle "bulge" organically.
 *  - `amplitude`  animates 0 → maxAmp → 0  → giving smooth↔bumpy transitions.
 *  - 
umWaves`   cycles through 3→8 to produce pentagon / oval / spiky blob shapes.
 *  - `wavePhase`  increments continuously for the rotation-within-morph feeling.
 *  - The whole Canvas also rotates with `rotation`.
 *
 * Result: the exact organic morphing blob seen in the M3 Expressive reference.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = Color.Unspecified, // ignored – no background
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blob_loading")

    // Overall slow rotation of the whole blob
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Wave amplitude: 0 = perfect circle, 1 = maximum bumpiness
    val amplitude by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude"
    )

    // Number of waves around the blob – drives the shape:
    //   ~3 → triangle-blob / pentagon
    //   ~5 → oval / egg
    //   ~8 → spiky bumpy circle
    val numWavesRaw by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "numWaves"
    )

    // Internal wave phase – makes bumps appear to flow/travel around the shape
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val baseR = this.size.width * 0.38f   // base radius (leaves a small margin)
        val maxBump = baseR * 0.28f           // maximum radial deviation

        val steps = if (isLowEnd) 32 else 120   // Adaptive sample points for low-end device smoothness

        val path = Path()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val angle = t * 2 * PI
            // Organic radius: base + amplitude-scaled sinusoid
            val r = baseR + amplitude * maxBump * sin(numWavesRaw * angle + wavePhase).toFloat()
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        rotate(rotation, Offset(cx, cy)) {
            drawPath(path = path, color = color)
        }
    }
}
