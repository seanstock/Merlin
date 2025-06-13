package com.example.merlin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 🍎 APPLE-INSPIRED TYPOGRAPHY SYSTEM 🍎
// Clean, readable, hierarchical - matching Apple's SF Pro design principles

val Typography = Typography(
    // LARGE DISPLAY STYLES - Apple's big, bold headlines
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, // System font (closest to SF Pro on Android)
        fontWeight = FontWeight.Bold,    // Apple uses semibold/bold for large text
        fontSize = 34.sp,               // Apple's Title 1 size
        lineHeight = 41.sp,             // Apple's 1.2 line height ratio
        letterSpacing = 0.37.sp         // Apple's subtle letter spacing
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,               // Apple's Title 2 size
        lineHeight = 34.sp,             // Proper line height
        letterSpacing = 0.36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,               // Apple's Title 3 size
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    ),
    
    // HEADLINE STYLES - Apple's section headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,               // Apple's Headline size
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,               // Apple's Body size when used as header
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp      // Apple's negative tracking for larger text
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,               // Apple's Callout size when used as header
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    ),
    
    // TITLE STYLES - Apple's card/section titles
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,               // Apple's Body semibold
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,               // Apple's Callout
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,               // Apple's Subheadline
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    
    // BODY TEXT STYLES - Apple's primary reading text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,               // Apple's Body - primary reading size
        lineHeight = 22.sp,             // Comfortable reading line height
        letterSpacing = (-0.41).sp      // Apple's body tracking
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,               // Apple's Callout
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,               // Apple's Subheadline
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    
    // LABEL STYLES - Apple's UI element labels and captions
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,               // Apple's Footnote
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,               // Apple's Caption 1
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,               // Apple's Caption 2
        lineHeight = 13.sp,
        letterSpacing = 0.07.sp
    )
)

// ADDITIONAL APPLE-INSPIRED TEXT STYLES FOR SPECIFIC USE CASES

// Large title for main screens (like iOS navigation titles)
val AppleLargeTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 41.sp,
    letterSpacing = 0.37.sp
)

// Navigation bar title style
val AppleNavigationTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.41).sp
)

// Button text styles
val AppleButtonTextLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.41).sp
)

val AppleButtonTextMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 21.sp,
    letterSpacing = (-0.32).sp
)

// Tab bar label style
val AppleTabBarLabel = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.12.sp
)

// Card subtitle style
val AppleCardSubtitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.24).sp
)

// Monospace style for numbers/codes (Apple uses SF Mono)
val AppleMonospaceBody = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 21.sp,
    letterSpacing = 0.sp
)