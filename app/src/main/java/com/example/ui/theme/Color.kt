package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Skeuomorphic Hi-Fi Analog Deck Palette
// ==========================================

// Hardware Surfaces & Brushed Metal
val SkeuoDeckDark = Color(0xFF141518)           // Main Hi-Fi chassis
val SkeuoDeckElevated = Color(0xFF1E2024)       // Raised beveled faceplate
val SkeuoRecessedTray = Color(0xFF0D0E10)       // Recessed audio tray / display bay
val SkeuoCardSurface = Color(0xFF23252A)        // Tactile component card
val SkeuoBevelHighlight = Color(0x38FFFFFF)      // Top-edge light reflection
val SkeuoBevelShadow = Color(0x80000000)         // Bottom-edge drop bevel

// Analog Knobs, Metallic Chrome & Aluminum
val SkeuoChromeLight = Color(0xFFE8ECEF)        // Chrome button highlight
val SkeuoChromeMid = Color(0xFF9EACB8)          // Brushed aluminum
val SkeuoChromeDark = Color(0xFF4B535C)         // Anodized steel
val SkeuoKnobGrip = Color(0xFF2E3137)           // Knurled grip texture

// Amber & Phosphor Illumination (LEDs & Displays)
val SkeuoAmberGlow = Color(0xFFFFB300)          // Warm vintage analog indicator
val SkeuoAmberDim = Color(0x66FFB300)           // Amber standby glow
val SkeuoPhosphorGreen = Color(0xFF4CAF50)      // VU meter nominal peak
val SkeuoPeakRed = Color(0xFFFF3D00)            // VU meter overdrive red
val SkeuoLcdCyan = Color(0xFF00E5FF)            // Digital VFD display font
val SkeuoLcdBg = Color(0xFF0A1216)              // Backlit LCD pane background

// High Contrast Text & Hardware Labels
val SkeuoTextPrimary = Color(0xFFF2F4F7)        // Stamped silver text
val SkeuoTextSecondary = Color(0xFFA2A8B5)      // Brushed metal label text
val SkeuoTextTertiary = Color(0xFF6B7280)       // Engraved subtle markings

// Aliases for compatibility across existing screens
val LilacPrimary = SkeuoAmberGlow
val PurpleContainer = SkeuoCardSurface
val OnPurpleContainer = SkeuoAmberGlow
val DeepPurple = SkeuoDeckDark
val DarkPurple = SkeuoRecessedTray

val DarkBackground = SkeuoDeckDark
val DarkSurface = SkeuoDeckElevated
val DarkSurfaceElevated = SkeuoCardSurface
val DarkSurfaceCard = SkeuoCardSurface
val DarkSurfaceVariant = Color(0xFF2C2F36)
val PurplePrimary = SkeuoAmberGlow

val TextPrimaryDark = SkeuoTextPrimary
val TextSecondaryDark = SkeuoTextSecondary
val TextTertiaryDark = SkeuoTextTertiary

val AccentLilac = SkeuoAmberGlow
val AccentPink = SkeuoPeakRed
val AccentCyan = SkeuoLcdCyan
val NeonCyan = SkeuoLcdCyan
val ElectricViolet = SkeuoAmberGlow
val CyberPink = SkeuoPeakRed
val DeepMidnight = SkeuoDeckDark

val CardBorder = Color(0x33A2A8B5)
val GlassBackground = Color(0xE61E2024)
val GradientStart = Color(0xFF23252A)
val GradientEnd = Color(0xFF141518)
