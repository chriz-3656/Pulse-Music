package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Mode Neumorphism Colors
val NeuLightBackground = Color(0xFFE0E5EC)
val NeuLightShadowLight = Color(0xFFFFFFFF)
val NeuLightShadowDark = Color(0xFFA3B1C6)
val NeuLightTextPrimary = Color(0xFF4A4E69)
val NeuLightTextSecondary = Color(0xFF9FA8DA)
val NeuLightAccent = Color(0xFFFFC107)

// Dark Mode Neumorphism Colors
val NeuDarkBackground = Color(0xFF202124) // Base gray
val NeuDarkShadowLight = Color(0xFF2A2B2F) // Top left
val NeuDarkShadowDark = Color(0xFF161719) // Bottom right
val NeuDarkTextPrimary = Color(0xFFE8EAED)
val NeuDarkTextSecondary = Color(0xFF9AA0A6)
val NeuDarkAccent = Color(0xFFFFCA28)

// Aliases mapping old names to new standard names, so app compiles!
val SkeuoDeckDark = NeuDarkBackground
val SkeuoDeckElevated = NeuDarkBackground
val SkeuoRecessedTray = NeuDarkBackground
val SkeuoCardSurface = NeuDarkBackground
val SkeuoBevelHighlight = NeuDarkShadowLight
val SkeuoBevelShadow = NeuDarkShadowDark

val SkeuoChromeLight = NeuLightBackground
val SkeuoChromeMid = NeuDarkTextSecondary
val SkeuoChromeDark = NeuDarkBackground
val SkeuoKnobGrip = NeuDarkBackground

val SkeuoAmberGlow = Color(0xFFFFC107)
val SkeuoAmberDim = Color(0x66FFC107)
val SkeuoPhosphorGreen = Color(0xFF81C995)
val SkeuoPeakRed = Color(0xFFF28B82)
val SkeuoLcdCyan = Color(0xFF00E5FF)
val SkeuoLcdBg = NeuDarkBackground

val SkeuoTextPrimary = NeuDarkTextPrimary
val SkeuoTextSecondary = NeuDarkTextSecondary
val SkeuoTextTertiary = Color(0xFF5F6368)

val LilacPrimary = NeuDarkAccent
val PurpleContainer = NeuDarkBackground
val OnPurpleContainer = NeuDarkAccent
val DeepPurple = NeuDarkBackground
val DarkPurple = NeuDarkBackground

val DarkBackground = NeuDarkBackground
val DarkSurface = NeuDarkBackground
val DarkSurfaceElevated = NeuDarkBackground
val DarkSurfaceCard = NeuDarkBackground
val DarkSurfaceVariant = NeuDarkBackground
val PurplePrimary = NeuDarkAccent

val TextPrimaryDark = NeuDarkTextPrimary
val TextSecondaryDark = NeuDarkTextSecondary
val TextTertiaryDark = SkeuoTextTertiary

val AccentLilac = NeuDarkAccent
val AccentPink = SkeuoPeakRed
val AccentCyan = NeuDarkAccent
val NeonCyan = NeuDarkAccent
val ElectricViolet = NeuDarkAccent
val CyberPink = SkeuoPeakRed
val DeepMidnight = NeuDarkBackground

val CardBorder = Color.Transparent
val GlassBackground = Color.Transparent
val GradientStart = NeuDarkBackground
val GradientEnd = NeuDarkBackground
