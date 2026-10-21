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
import com.example.domain.model.ThemeColorway

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
    val accent: Color,
    val accentGlow: Color = accent,
    val accentDim: Color = accent.copy(alpha = 0.4f)
)

val LocalNeuColors = staticCompositionLocalOf {
    NeuColors(
        background = NeuDarkBackground,
        lightShadow = NeuDarkShadowLight,
        darkShadow = NeuDarkShadowDark,
        textPrimary = NeuDarkTextPrimary,
        textSecondary = NeuDarkTextSecondary,
        accent = NeuDarkAccent,
        accentGlow = NeuDarkAccent,
        accentDim = NeuDarkAccent.copy(alpha = 0.4f)
    )
}

@Composable
fun PulseMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    colorway: ThemeColorway = ThemeColorway.AMBER,
    content: @Composable () -> Unit
) {
    val (accentColor, glowColor, dimColor) = when (colorway) {
        ThemeColorway.CYAN -> Triple(Color(0xFF00D9FF), Color(0xFF38E1FF), Color(0x6600D9FF))
        ThemeColorway.GREEN -> Triple(Color(0xFF7CFF5B), Color(0xFF9EFF84), Color(0x667CFF5B))
        ThemeColorway.AMBER -> Triple(Color(0xFFFFC107), Color(0xFFFFB300), Color(0x66FFC107))
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            background = NeuDarkBackground,
            surface = NeuDarkBackground,
            onPrimary = Color.Black,
            onBackground = NeuDarkTextPrimary,
            onSurface = NeuDarkTextPrimary
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            background = NeuLightBackground,
            surface = NeuLightBackground,
            onPrimary = Color.White,
            onBackground = NeuLightTextPrimary,
            onSurface = NeuLightTextPrimary
        )
    }

    val neuColors = if (darkTheme) {
        NeuColors(
            background = NeuDarkBackground,
            lightShadow = NeuDarkShadowLight,
            darkShadow = NeuDarkShadowDark,
            textPrimary = NeuDarkTextPrimary,
            textSecondary = NeuDarkTextSecondary,
            accent = accentColor,
            accentGlow = glowColor,
            accentDim = dimColor
        )
    } else {
        NeuColors(
            background = NeuLightBackground,
            lightShadow = NeuLightShadowLight,
            darkShadow = NeuLightShadowDark,
            textPrimary = NeuLightTextPrimary,
            textSecondary = NeuLightTextSecondary,
            accent = accentColor,
            accentGlow = glowColor,
            accentDim = dimColor
        )
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
