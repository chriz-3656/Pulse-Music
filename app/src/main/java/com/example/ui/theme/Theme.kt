package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
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

data class NeuColors(
    val background: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color
)

val LocalNeuColors = staticCompositionLocalOf {
    NeuColors(
        background = NeuDarkBackground,
        lightShadow = NeuDarkShadowLight,
        darkShadow = NeuDarkShadowDark,
        textPrimary = NeuDarkTextPrimary,
        textSecondary = NeuDarkTextSecondary,
        accent = NeuDarkAccent
    )
}

private val LightColorScheme = lightColorScheme(
    primary = NeuLightAccent,
    background = NeuLightBackground,
    surface = NeuLightBackground,
    onPrimary = Color.White,
    onBackground = NeuLightTextPrimary,
    onSurface = NeuLightTextPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = NeuDarkAccent,
    background = NeuDarkBackground,
    surface = NeuDarkBackground,
    onPrimary = Color.Black,
    onBackground = NeuDarkTextPrimary,
    onSurface = NeuDarkTextPrimary
)

@Composable
fun PulseMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val neuColors = if (darkTheme) {
        NeuColors(NeuDarkBackground, NeuDarkShadowLight, NeuDarkShadowDark, NeuDarkTextPrimary, NeuDarkTextSecondary, NeuDarkAccent)
    } else {
        NeuColors(NeuLightBackground, NeuLightShadowLight, NeuLightShadowDark, NeuLightTextPrimary, NeuLightTextSecondary, NeuLightAccent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = neuColors.background.toArgb()
            window.navigationBarColor = neuColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNeuColors provides neuColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CurvedShapes,
            content = content
        )
    }
}
