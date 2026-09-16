package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val CurvedShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val SkeuoDarkColorScheme = darkColorScheme(
    primary = SkeuoAmberGlow,
    onPrimary = SkeuoDeckDark,
    primaryContainer = SkeuoCardSurface,
    onPrimaryContainer = SkeuoAmberGlow,
    secondary = SkeuoChromeMid,
    onSecondary = SkeuoDeckDark,
    secondaryContainer = SkeuoDeckElevated,
    onSecondaryContainer = SkeuoTextPrimary,
    tertiary = SkeuoLcdCyan,
    onTertiary = SkeuoDeckDark,
    background = SkeuoDeckDark,
    onBackground = SkeuoTextPrimary,
    surface = SkeuoDeckElevated,
    onSurface = SkeuoTextPrimary,
    surfaceVariant = SkeuoCardSurface,
    onSurfaceVariant = SkeuoTextSecondary,
    outline = SkeuoChromeDark
)

@Composable
fun PulseMusicTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SkeuoDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CurvedShapes,
        content = content
    )
}
