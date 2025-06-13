package com.example.merlin.ui.theme

import androidx.compose.ui.graphics.Color

// 🍎 APPLE-INSPIRED COLOR SYSTEM 🍎
// Clean, minimal, sophisticated - just like Apple designs

// SYSTEM COLORS (Primary Apple Palette)
val AppleBlue = Color(0xFF007AFF)           // System Blue - primary actions
val AppleDarkBlue = Color(0xFF0056CC)       // Dark variant of system blue
val AppleLightBlue = Color(0xFF4DA3FF)      // Light variant for states

val AppleGray = Color(0xFF8E8E93)           // System Gray - secondary text
val AppleGray2 = Color(0xFFAEAEB2)          // System Gray 2 - subtle elements  
val AppleGray3 = Color(0xFFC7C7CC)          // System Gray 3 - separators
val AppleGray4 = Color(0xFFD1D1D6)          // System Gray 4 - fills
val AppleGray5 = Color(0xFFE5E5EA)          // System Gray 5 - grouped backgrounds
val AppleGray6 = Color(0xFFF2F2F7)          // System Gray 6 - grouped content

// BACKGROUND COLORS
val AppleSystemBackground = Color(0xFFFFFFFF)      // Pure white background
val AppleSecondarySystemBackground = Color(0xFFF2F2F7)  // Light gray background
val AppleTertiarySystemBackground = Color(0xFFFFFFFF)   // White for cards

// LABEL COLORS  
val ApplePrimaryLabel = Color(0xFF000000)           // Primary text - pure black
val AppleSecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)  // Secondary text
val AppleTertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)   // Tertiary text

// SEMANTIC COLORS
val AppleRed = Color(0xFFFF453A)             // System Red - destructive actions
val AppleOrange = Color(0xFFFF9500)          // System Orange - warnings
val AppleYellow = Color(0xFFFFCC02)          // System Yellow - caution
val AppleGreen = Color(0xFF30D158)           // System Green - success
val AppleMint = Color(0xFF63E6E2)            // System Mint - fresh accent
val AppleTeal = Color(0xFF40CBB8)            // System Teal - cool accent
val AppleCyan = Color(0xFF64D2FF)            // System Cyan - info
val AppleIndigo = Color(0xFF5856D6)          // System Indigo - deep accent
val ApplePurple = Color(0xFFAF52DE)          // System Purple - creative

// DARK MODE VARIANTS (for future dark mode support)
val AppleSystemBackgroundDark = Color(0xFF000000)
val AppleSecondarySystemBackgroundDark = Color(0xFF1C1C1E)
val AppleTertiarySystemBackgroundDark = Color(0xFF2C2C2E)
val ApplePrimaryLabelDark = Color(0xFFFFFFFF)
val AppleSecondaryLabelDark = Color(0xFFEBEBF5).copy(alpha = 0.6f)

// CARD AND SURFACE COLORS
val AppleCardBackground = Color(0xFFFFFFFF)         // Card backgrounds
val AppleModalBackground = Color(0xFFF2F2F7)        // Modal/sheet backgrounds
val AppleSeparator = Color(0xFF3C3C43).copy(alpha = 0.29f)  // Separator lines

// INTERACTIVE COLORS
val AppleBluePressed = Color(0xFF0051D5)            // Pressed blue state
val AppleGrayPressed = Color(0xFFD1D1D6)            // Pressed gray state

// ALIAS COLORS FOR COMPATIBILITY (gradually phase out)
val MagicalBlue = AppleBlue
val MagicalPurple = ApplePurple
val EnchantedIndigo = AppleIndigo
val SunshineYellow = AppleYellow
val DragonGreen = AppleGreen
val PhoenixOrange = AppleOrange
val StardustPink = ApplePurple.copy(alpha = 0.3f)
val UnicornMint = AppleMint
val CloudWhite = AppleSystemBackground
val MidnightMagic = Color(0xFF1C1C1E)
val SparkleGold = AppleYellow
val CrystalBlue = AppleBlue
val RainbowRed = AppleRed

// SURFACE ELEVATION COLORS (subtle shadows like Apple)
val AppleShadowLight = Color(0xFF000000).copy(alpha = 0.05f)
val AppleShadowMedium = Color(0xFF000000).copy(alpha = 0.1f)
val AppleShadowStrong = Color(0xFF000000).copy(alpha = 0.15f)

// LEGACY MATERIAL COLORS (keep for compatibility)
val Purple200 = ApplePurple.copy(alpha = 0.5f)
val Purple500 = ApplePurple
val Purple700 = AppleIndigo
val Teal200 = AppleTeal.copy(alpha = 0.5f)
val Teal700 = AppleTeal
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

// ADDITIONAL LEGACY ALIASES
val Purple80 = ApplePurple.copy(alpha = 0.8f)
val PurpleGrey80 = AppleGray2
val Pink80 = ApplePurple.copy(alpha = 0.3f)

val Purple40 = AppleIndigo
val PurpleGrey40 = AppleGray
val Pink40 = ApplePurple.copy(alpha = 0.4f)

// SEMANTIC ALIASES
val MerlinPurple = ApplePurple
val SkyBlue = AppleCyan  
val EmeraldGreen = AppleGreen
val EnchantedGreen = AppleGreen

// SOPHISTICATED COOL PALETTE ALIASES (for backward compatibility)
val DeepOcean = AppleIndigo
val WisdomBlue = AppleBlue
val ForestGreen = AppleGreen
val SageGreen = AppleMint
val RoyalPurple = ApplePurple
val LavenderMist = ApplePurple.copy(alpha = 0.2f)

val MoonlightSilver = AppleGray
val IvoryWhite = AppleSystemBackground
val StormyGray = AppleGray2
val MidnightNavy = Color(0xFF1C1C1E)

val AmberGlow = AppleYellow.copy(alpha = 0.8f)
val WarmTerracotta = AppleOrange.copy(alpha = 0.7f)
val CloudySky = AppleGray5

val MistyBlue = AppleGray6
val SeafoamMist = AppleMint.copy(alpha = 0.1f)
val IceBlue = AppleBlue.copy(alpha = 0.05f)