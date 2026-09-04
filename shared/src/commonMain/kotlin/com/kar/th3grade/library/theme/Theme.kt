package com.Nightjar.gradeiraqi3library.theme

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.drawWithCache
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.rememberDynamicColorScheme
import org.jetbrains.compose.resources.Font
import com.Nightjar.gradeiraqi3library.generated.resources.Res
import com.Nightjar.gradeiraqi3library.generated.resources.reem_kufi_bold
import com.Nightjar.gradeiraqi3library.generated.resources.reem_kufi_medium
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.material3.ColorScheme

fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val argb = if (cleanHex.length == 6) {
            "FF$cleanHex".toLong(16)
        } else {
            cleanHex.toLong(16)
        }
        Color(argb)
    } catch (e: Exception) {
        Color(0xFF6366F1) // Fallback indigo
    }
}

val LocalIsLowEndDevice = compositionLocalOf { false }

// Glassmorphism modifier optimized with drawWithCache
fun Modifier.liquidGlass(
    isDark: Boolean,
    borderRadius: Dp = 24.dp,
    alpha: Float = 0.95f // Increased to hide content underneath
): Modifier = composed {
    val isLowEnd = LocalIsLowEndDevice.current
    val bg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = if (isLowEnd) 1.0f else alpha) // Slate 800 (Solid for low end)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLowEnd) 1.0f else alpha)
    }
    
    if (isLowEnd) {
        return@composed this.background(bg, RoundedCornerShape(borderRadius))
    }
    val border = if (isDark) {
        Color(0xFFFFFFFF).copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    }
    this.drawWithCache {
        val roundedRect = RoundRect(
            rect = size.toRect(),
            cornerRadius = CornerRadius(borderRadius.toPx())
        )
        val path = Path().apply { addRoundRect(roundedRect) }
        
        onDrawBehind {
            drawPath(path, color = bg)
            drawPath(
                path, 
                color = border, 
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        )
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
}

/**
 * Staggered pop-in animation on initial screen load (from caliq5).
 */
fun Modifier.popInOnInitialLoad(index: Int = 0): Modifier = composed {
    var isLoaded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        isLoaded = true
    }
    val animProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 400,
            delayMillis = (index.coerceAtMost(8) * 40),
            easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
        ),
        label = "popIn_$index"
    )
    this.graphicsLayer {
        this.alpha = animProgress
        this.translationY = (1f - animProgress) * 30f // dp is not available directly without LocalDensity, but float is fine for translationY (pixels)
        this.scaleX = 0.95f + 0.05f * animProgress
        this.scaleY = 0.95f + 0.05f * animProgress
    }
}

val KufiReemFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.reem_kufi_medium, FontWeight.Medium),
        Font(Res.font.reem_kufi_bold, FontWeight.Bold)
    )

@Composable
fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 350, easing = LinearOutSlowInEasing)
    
    val primary = animateColorAsState(targetColorScheme.primary, animationSpec).value
    val onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec).value
    val primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec).value
    val onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec).value
    val inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec).value
    val secondary = animateColorAsState(targetColorScheme.secondary, animationSpec).value
    val onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec).value
    val secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec).value
    val onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec).value
    val tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec).value
    val onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec).value
    val tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec).value
    val onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec).value
    val background = animateColorAsState(targetColorScheme.background, animationSpec).value
    val onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec).value
    val surface = animateColorAsState(targetColorScheme.surface, animationSpec).value
    val onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec).value
    val surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec).value
    val onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec).value
    val surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec).value
    val inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec).value
    val inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec).value
    val error = animateColorAsState(targetColorScheme.error, animationSpec).value
    val onError = animateColorAsState(targetColorScheme.onError, animationSpec).value
    val errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec).value
    val onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec).value
    val outline = animateColorAsState(targetColorScheme.outline, animationSpec).value
    val outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec).value
    val scrim = animateColorAsState(targetColorScheme.scrim, animationSpec).value

    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim
    )
}

@Composable
fun Th3GradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlackMode: Boolean = false,
    pureWhiteMode: Boolean = false,
    seedColorHex: String = "#2563eb",
    dynamicColorScheme: androidx.compose.material3.ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val seedColor = parseHexColor(seedColorHex)
    val isMonochrome = seedColorHex.equals("#171717", ignoreCase = true) || seedColorHex.equals("#ffffff", ignoreCase = true)
    
    val baseColorScheme = dynamicColorScheme ?: rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme
    )

    val colorScheme = if (isMonochrome) {
        if (darkTheme) {
            ColorScheme(
                primary = Color.White,
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF262626),
                onPrimaryContainer = Color.White,
                inversePrimary = Color.Black,
                secondary = Color(0xFFE5E5E5),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF404040),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFFD4D4D4),
                onTertiary = Color.Black,
                tertiaryContainer = Color(0xFF262626),
                onTertiaryContainer = Color(0xFFD4D4D4),
                background = Color.Black,
                onBackground = Color.White,
                surface = Color(0xFF0A0A0A),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF171717),
                onSurfaceVariant = Color(0xFFD4D4D4),
                surfaceTint = Color.White,
                inverseSurface = Color.White,
                inverseOnSurface = Color.Black,
                error = Color(0xFFF87171),
                onError = Color.Black,
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFF87171),
                outline = Color(0xFF404040),
                outlineVariant = Color(0xFF262626),
                scrim = Color.Black
            )
        } else {
            ColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF5F5F5),
                onPrimaryContainer = Color.Black,
                inversePrimary = Color.White,
                secondary = Color(0xFF404040),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE5E5E5),
                onSecondaryContainer = Color.Black,
                tertiary = Color(0xFF737373),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFF5F5F5),
                onTertiaryContainer = Color(0xFF262626),
                background = Color.White,
                onBackground = Color.Black,
                surface = Color(0xFFFAFAFA),
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF5F5F5),
                onSurfaceVariant = Color(0xFF525252),
                surfaceTint = Color.Black,
                inverseSurface = Color.Black,
                inverseOnSurface = Color.White,
                error = Color(0xFFB91C1C),
                onError = Color.White,
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                outline = Color(0xFFD4D4D4),
                outlineVariant = Color(0xFFE5E5E5),
                scrim = Color.Black
            )
        }
    } else {
        if (darkTheme) {
            if (pureBlackMode) {
                baseColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212)
                )
            } else {
                baseColorScheme
            }
        } else {
            if (pureWhiteMode) {
                baseColorScheme.copy(
                    background = Color.White,
                    surface = Color.White,
                    surfaceVariant = Color(0xFFF1F5F9), // Light slate/gray background variant for items
                    onBackground = Color(0xFF0F172A), // Slate 900 for premium high contrast
                    onSurface = Color(0xFF0F172A),
                    onSurfaceVariant = Color(0xFF334155), // Slate 700 for subtext contrast
                    outline = Color(0xFFE2E8F0),
                    outlineVariant = Color(0xFFF1F5F9)
                )
            } else {
                baseColorScheme
            }
        }
    }

    val animatedColorScheme = animateColorScheme(colorScheme)

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography(),
        content = content
    )
}
