package com.example.merlin.ai

/**
 * Result of executing an AI tool function
 */
data class ToolExecutionResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)

/**
 * Result data for game launch operations
 */
data class GameLaunchResult(
    val gameId: String,
    val level: Int,
    val launched: Boolean,
    val reason: String
) 