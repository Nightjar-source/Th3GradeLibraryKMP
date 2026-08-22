package com.Nightjar.gradeiraqi3library.ui

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
import com.Nightjar.gradeiraqi3library.generated.resources.Res
import com.Nightjar.gradeiraqi3library.generated.resources.app_icon
import com.Nightjar.gradeiraqi3library.theme.KufiReemFontFamily
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 350)
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000) // Show splash for 2.0 seconds
        onAnimationFinished()
    }

    val shapes = listOf("✏️", "✨", "🇮🇶", "📝", "✨", "📚")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B), // Very dark purple
                        Color(0xFF312E81), // Indigo
                        Color(0xFF0F172A)  // Slate
                    )
                )
            ), // Beautiful gradient
        contentAlignment = Alignment.Center
    ) {
        // Swimming Shapes background (BEHIND)
        shapes.forEachIndexed { index, shape ->
            SwimmingShape(index = index, text = shape)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
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
            Text(
                text = "مكتبة الثالث متوسط",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KufiReemFontFamily
            )
        }
    }
}

@Composable
fun SwimmingShape(index: Int, text: String) {
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
                this.alpha = 0.4f // Increased visibility
            }
    )
}
