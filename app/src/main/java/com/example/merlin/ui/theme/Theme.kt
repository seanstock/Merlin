package com.example.merlin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 🌙 MAGICAL NIGHT THEME 🌙
private val MagicalDarkColorScheme = darkColorScheme(
    primary = MagicalBlue,
    secondary = StardustPink,
    tertiary = SunshineYellow,
    background = MidnightMagic,
    surface = EnchantedIndigo,
    onPrimary = CloudWhite,
    onSecondary = CloudWhite,
    onTertiary = MidnightMagic,
    onBackground = CloudWhite,
    onSurface = CloudWhite,
    primaryContainer = EnchantedIndigo,
    secondaryContainer = MagicalPurple,
    tertiaryContainer = PhoenixOrange,
    onPrimaryContainer = CloudWhite,
    onSecondaryContainer = CloudWhite,
    onTertiaryContainer = CloudWhite
)

// ☀️ MAGICAL RAINBOW THEME ☀️
private val MagicalLightColorScheme = lightColorScheme(
    primary = MagicalPurple,
    secondary = StardustPink, 
    tertiary = DragonGreen,
    background = CloudWhite,
    surface = Color(0xFFF8F8FF), // Magical snow white
    onPrimary = CloudWhite,
    onSecondary = CloudWhite,
    onTertiary = CloudWhite,
    onBackground = MidnightMagic,
    onSurface = MidnightMagic,
    primaryContainer = MagicalBlue,
    secondaryContainer = UnicornMint,
    tertiaryContainer = SunshineYellow,
    onPrimaryContainer = CloudWhite,
    onSecondaryContainer = MidnightMagic,
    onTertiaryContainer = MidnightMagic,
    error = RainbowRed,
    onError = CloudWhite,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = RainbowRed,
    surfaceVariant = Color(0xFFE8F5E8), // Light mint
    onSurfaceVariant = MidnightMagic,
    outline = MagicalPurple
)

@Composable
fun MerlinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled for consistent magical experience
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MagicalDarkColorScheme
        else -> MagicalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}