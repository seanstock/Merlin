package com.example.merlin.ui.accessibility

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Accessibility constants for child-friendly interface design.
 * Following WCAG guidelines and child-specific accessibility requirements.
 */
object AccessibilityConstants {
    
    // Touch targets - minimum 48dp as per Material Design and WCAG guidelines
    val MIN_TOUCH_TARGET = 48.dp
    val RECOMMENDED_TOUCH_TARGET = 56.dp
    val LARGE_TOUCH_TARGET = 64.dp // For primary actions
    
    // Text sizes for enhanced readability
    val CHILD_BODY_TEXT = 18.sp      // Larger than standard 14sp
    val CHILD_TITLE_TEXT = 24.sp     // Larger than standard 20sp
    val CHILD_HEADLINE_TEXT = 32.sp  // Larger than standard 28sp
    val CHILD_DISPLAY_TEXT = 48.sp   // Very large for main titles
    
    // Content descriptions for screen readers
    object ContentDescriptions {
        const val MERLIN_AVATAR = "Merlin the wizard, your AI tutor"
        const val USER_AVATAR = "Your profile picture"
        const val VOICE_INPUT_START = "Tap to start voice message"
        const val VOICE_INPUT_STOP = "Tap to stop voice recording"
        const val SEND_MESSAGE = "Send your message to Merlin"
        const val CLEAR_CHAT = "Clear all chat messages"
        const val BACK_BUTTON = "Go back to previous screen"
        const val EXIT_TO_APP = "Exit chat and return to main menu"
        const val SETTINGS = "Open settings menu"
        const val CHAT_MESSAGE_FROM_USER = "Your message"
        const val CHAT_MESSAGE_FROM_MERLIN = "Message from Merlin"
        const val LOADING_INDICATOR = "Merlin is thinking of a response"
        const val ERROR_MESSAGE = "Error message, please try again"
    }
    
    // Semantic roles for better screen reader navigation
    object SemanticRoles {
        const val CHAT_AREA = "Chat conversation area"
        const val INPUT_AREA = "Message input area"
        const val NAVIGATION_AREA = "Navigation controls"
        const val AVATAR_AREA = "Profile pictures"
    }
} 