package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.app_icon
import com.Nightjar.Th3GradeLibraryKMP.theme.KufiReemFontFamily
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // --- 1. Logo Icon: animateJackInTheBox (Elastic pop-up with rotational wobble) & animateRotateOut ---
    val iconScale by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f
            startAnimation -> 1f
            else -> 0.15f
        },
        animationSpec = if (isExiting) tween(320, easing = FastOutLinearInEasing)
                        else spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)
    )

    val iconRotation by animateFloatAsState(
        targetValue = when {
            isExiting -> 180f
            startAnimation -> 0f
            else -> -25f
        },
        animationSpec = if (isExiting) tween(320, easing = FastOutLinearInEasing)
                        else spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
    )

    val iconAlpha by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f
            startAnimation -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = if (isExiting) 250 else 350)
    )

    // --- 2. Title Text: animatelightSpeedInRight + animateflipInX (3D Flip & Horizontal Slide) ---
    val textSlideX by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 160f,
        animationSpec = tween(durationMillis = 550, delayMillis = 150, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
    )

    val textFlipX by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 75f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1550)
        isExiting = true
        delay(250) // Allow exit animation to complete smoothly
        onAnimationFinished()
    }

    val shapes = listOf("✏️", "✨", "🇮🇶", "📝", "✨", "📚")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate dark (matching splash_background.xml)
                        Color(0xFF1E1B4B), // Very dark purple
                        Color(0xFF0F172A)  // Slate dark
                    )
                )
            ), // Seamless transition with splash_background.xml
        contentAlignment = Alignment.Center
    ) {
        // Swimming Shapes background (BEHIND)
        shapes.forEachIndexed { index, shape ->
            SwimmingShape(
                index = index, 
                text = shape,
                startAnimation = startAnimation,
                isExiting = isExiting
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo Icon with JackInTheBox & RotateOut
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        rotationZ = iconRotation
                        alpha = iconAlpha
                        cameraDistance = 12f * density
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Text with LightSpeedInRight & FlipInX
            Text(
                text = "مكتبة الثالث متوسط",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KufiReemFontFamily,
                modifier = Modifier.graphicsLayer {
                    translationX = textSlideX
                    rotationX = textFlipX
                    alpha = textAlpha
                    cameraDistance = 14f * density
                }
            )
        }
    }
}

@Composable
fun SwimmingShape(
    index: Int, 
    text: String,
    startAnimation: Boolean,
    isExiting: Boolean
) {
    val scaleAnim by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f
            startAnimation -> 1f
            else -> 0f
        },
        animationSpec = if (isExiting) tween(250) else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    )

    val infiniteTransition = rememberInfiniteTransition()

    // Start on-screen evenly distributed across X
    val startX = -500f + (index * 200f)
    // Move quickly across
    val direction = if (index % 2 == 0) 1f else -1f
    val endX = startX + (direction * 600f)

    val translationX by infiniteTransition.animateFloat(
        initialValue = startX,
        targetValue = endX,
        animationSpec = infiniteRepeatable(
            animation = tween(3000 + (index * 500), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Spread them across Y axis relative to center
    val baseOffsetY = -600f + (index * 250f)
    
    val translationY by infiniteTransition.animateFloat(
        initialValue = baseOffsetY,
        targetValue = baseOffsetY + (direction * 200f),
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (index * 400), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (index % 2 == 0) 360f else -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000 + (index * 1000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Text(
        text = text,
        fontSize = (40 + (index * 8)).sp, // varying large sizes
        modifier = Modifier
            .graphicsLayer {
                this.translationX = translationX
                this.translationY = translationY
                this.rotationZ = if (text == "🇮🇶") 0f else rotation
                this.scaleX = scaleAnim
                this.scaleY = scaleAnim
                this.alpha = 0.4f * scaleAnim
            }
    )
}
