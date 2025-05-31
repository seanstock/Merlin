package com.example.merlin.ui.chat

import androidx.compose.runtime.Immutable

/**
 * Represents a chat message in the AI tutor conversation.
 */
@Immutable
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val functionCall: FunctionCall? = null
)

/**
 * Represents a function call from the AI (e.g., launching a game).
 */
@Immutable
data class FunctionCall(
    val name: String,
    val arguments: Map<String, Any>
)

/**
 * Represents a game launch event triggered by the AI.
 */
@Immutable
data class GameLaunchEvent(
    val gameId: String,
    val level: Int = 1
) 