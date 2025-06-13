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
import androidx.annotation.DrawableRes
import com.example.merlin.R

// 🍎 APPLE-INSPIRED DARK THEME 🍎
private val AppleDarkColorScheme = darkColorScheme(
    primary = AppleBlue,                          // System Blue
    onPrimary = AppleSystemBackground,            // White text on blue
    primaryContainer = AppleDarkBlue,             // Darker blue for containers
    onPrimaryContainer = AppleSystemBackground,   // White text
    
    secondary = AppleGray,                        // System Gray
    onSecondary = AppleSystemBackground,          // White text
    secondaryContainer = AppleGray2,              // Light gray containers
    onSecondaryContainer = ApplePrimaryLabelDark, // White text
    
    tertiary = AppleTeal,                         // System Teal accent
    onTertiary = AppleSystemBackground,           // White text
    tertiaryContainer = AppleMint,                // Light teal
    onTertiaryContainer = ApplePrimaryLabel,      // Black text
    
    background = AppleSystemBackgroundDark,       // Pure black
    onBackground = ApplePrimaryLabelDark,         // White text
    surface = AppleSecondarySystemBackgroundDark, // Dark gray surface
    onSurface = ApplePrimaryLabelDark,            // White text
    
    surfaceVariant = AppleTertiarySystemBackgroundDark, // Medium gray
    onSurfaceVariant = AppleSecondaryLabelDark,   // Light gray text
    
    outline = AppleSeparator,                     // Separator lines
    outlineVariant = AppleGray3,                  // Subtle outlines
    
    error = AppleRed,                             // System Red
    onError = AppleSystemBackground,              // White text
    errorContainer = AppleRed.copy(alpha = 0.12f), // Light red container
    onErrorContainer = AppleRed                   // Red text
)

// 🍎 APPLE-INSPIRED LIGHT THEME 🍎  
private val AppleLightColorScheme = lightColorScheme(
    primary = AppleBlue,                          // System Blue
    onPrimary = AppleSystemBackground,            // White text on blue
    primaryContainer = AppleLightBlue,            // Light blue containers
    onPrimaryContainer = AppleDarkBlue,           // Dark blue text
    
    secondary = AppleGray,                        // System Gray
    onSecondary = AppleSystemBackground,          // White text
    secondaryContainer = AppleGray5,              // Light gray containers
    onSecondaryContainer = ApplePrimaryLabel,     // Black text
    
    tertiary = AppleTeal,                         // System Teal accent
    onTertiary = AppleSystemBackground,           // White text  
    tertiaryContainer = AppleMint.copy(alpha = 0.12f), // Very light teal
    onTertiaryContainer = AppleTeal,              // Teal text
    
    background = AppleSystemBackground,           // Pure white
    onBackground = ApplePrimaryLabel,             // Black text
    surface = AppleSystemBackground,              // Pure white surface
    onSurface = ApplePrimaryLabel,                // Black text
    
    surfaceVariant = AppleGray6,                  // Very light gray
    onSurfaceVariant = AppleSecondaryLabel,       // Gray text
    
    outline = AppleSeparator,                     // Separator lines
    outlineVariant = AppleGray4,                  // Subtle outlines
    
    error = AppleRed,                             // System Red
    onError = AppleSystemBackground,              // White text
    errorContainer = AppleRed.copy(alpha = 0.12f), // Light red container
    onErrorContainer = AppleRed,                  // Red text
    
    // Additional semantic colors
    inverseSurface = ApplePrimaryLabel,           // Black inverse surface
    inverseOnSurface = AppleSystemBackground,     // White inverse text
    inversePrimary = AppleLightBlue,              // Light blue inverse
    
    surfaceTint = AppleBlue,                      // Blue tint
    scrim = Color.Black.copy(alpha = 0.32f)       // Modal scrim
)

@Composable
fun MerlinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to maintain Apple's consistent design language
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AppleDarkColorScheme
        else -> AppleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Theme system for personalizing the child's experience
 */
data class AppTheme(
    val id: String,
    val name: String,
    val tutorName: String,
    val tutorDescription: String,
    @DrawableRes val backgroundImage: Int,
    @DrawableRes val tutorAvatar: Int
)

object AppThemes {
    val UNDER_THE_SEA = AppTheme(
        id = "under_the_sea",
        name = "Under the Sea",
        tutorName = "Olivia",
        tutorDescription = "A friendly octopus who loves exploring the ocean depths",
        backgroundImage = R.drawable.underthesea,
        tutorAvatar = R.drawable.underthesea
    )
    
    val OUTER_SPACE = AppTheme(
        id = "outer_space",
        name = "Outer Space",
        tutorName = "Spoko",
        tutorDescription = "A curious alien who knows all about the stars and planets",
        backgroundImage = R.drawable.outerspace_bg,
        tutorAvatar = R.drawable.underthesea
    )
    
    val LOST_WORLD = AppTheme(
        id = "lost_world",
        name = "Lost World",
        tutorName = "Wizbit",
        tutorDescription = "A wise owl who guides adventures through ancient lands",
        backgroundImage = R.drawable.underthesea,
        tutorAvatar = R.drawable.underthesea
    )
    
    val FANTASY = AppTheme(
        id = "fantasy",
        name = "Fantasy",
        tutorName = "Prince",
        tutorDescription = "A brave mouse prince from a magical kingdom",
        backgroundImage = R.drawable.underthesea,
        tutorAvatar = R.drawable.underthesea
    )
    
    val ALL_THEMES = listOf(UNDER_THE_SEA, OUTER_SPACE, LOST_WORLD, FANTASY)
    
    fun getThemeById(id: String): AppTheme? {
        return ALL_THEMES.find { it.id == id }
    }
    
    fun getDefaultTheme(): AppTheme = UNDER_THE_SEA
}