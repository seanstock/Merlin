package com.example.merlin.ai

/**
 * Defines all the tools/functions that Merlin can use to interact with the app.
 * This transforms Merlin from a simple chat bot into an AI agent that can actually DO things.
 * Uses domain-level types to maintain independence from specific AI providers.
 */
object MerlinTools {

    /**
     * Get all available tools for Merlin to use
     */
    fun getAllTools(): List<AIFunctionTool> = listOf(
        createStartGameTool(),
        createCheckProgressTool(),
        createCheckCoinsBalance(),
        createAwardCoins(),
        createNavigateToScreen(),
        createSetReminder(),
        createShowLearningContent()
    )

    /**
     * Tool to start/launch a specific game
     */
    private fun createStartGameTool(): AIFunctionTool = AIFunctionTool(
        name = "start_game",
        description = "Launch a specific game for the child to play. Use this when the child asks to play a game or when you want to suggest practice through gameplay.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "game_id" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the game to start",
                    "enum" to listOf("color-match", "math-quest", "word-builder", "pattern-puzzle")
                ),
                "level" to mapOf(
                    "type" to "integer",
                    "description" to "Starting level (1-10). Default is 1.",
                    "minimum" to 1,
                    "maximum" to 10
                ),
                "reason" to mapOf(
                    "type" to "string",
                    "description" to "Brief explanation of why this game was chosen (for context)"
                )
            ),
            "required" to listOf("game_id")
        )
    )

    /**
     * Tool to check child's learning progress
     */
    private fun createCheckProgressTool(): AIFunctionTool = AIFunctionTool(
        name = "check_progress",
        description = "Check the child's learning progress, recent scores, and achievements. Use this to provide personalized feedback.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "category" to mapOf(
                    "type" to "string",
                    "description" to "Progress category to check",
                    "enum" to listOf("games", "overall", "coins", "achievements", "recent_activity")
                ),
                "days" to mapOf(
                    "type" to "integer",
                    "description" to "Number of days to look back (default: 7)",
                    "minimum" to 1,
                    "maximum" to 30
                )
            ),
            "required" to listOf("category")
        )
    )

    /**
     * Tool to check coin balance and earning potential
     */
    private fun createCheckCoinsBalance(): AIFunctionTool = AIFunctionTool(
        name = "check_coins",
        description = "Check the child's Merlin Coin balance, daily earnings, and spending power. Use this when discussing rewards or screen time.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "include_details" to mapOf(
                    "type" to "boolean",
                    "description" to "Include detailed breakdown of earnings and spending",
                    "default" to false
                )
            )
        )
    )

    /**
     * Tool to award bonus coins for exceptional learning
     */
    private fun createAwardCoins(): AIFunctionTool = AIFunctionTool(
        name = "award_bonus_coins",
        description = "Award bonus Merlin Coins for exceptional learning, helping others, or special achievements. Use sparingly for meaningful moments.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "amount" to mapOf(
                    "type" to "integer",
                    "description" to "Number of bonus coins to award (1-10)",
                    "minimum" to 1,
                    "maximum" to 10
                ),
                "reason" to mapOf(
                    "type" to "string",
                    "description" to "Clear explanation of why the bonus was earned"
                )
            ),
            "required" to listOf("amount", "reason")
        )
    )

    /**
     * Tool to navigate to different app screens
     */
    private fun createNavigateToScreen(): AIFunctionTool = AIFunctionTool(
        name = "navigate_to_screen",
        description = "Navigate the child to a specific screen in the app. Use this to guide them to relevant features.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "screen" to mapOf(
                    "type" to "string",
                    "description" to "The screen to navigate to",
                    "enum" to listOf("games", "wallet", "achievements", "progress", "settings", "chat")
                ),
                "message" to mapOf(
                    "type" to "string",
                    "description" to "Optional message to show when navigating"
                )
            ),
            "required" to listOf("screen")
        )
    )

    /**
     * Tool to set learning reminders
     */
    private fun createSetReminder(): AIFunctionTool = AIFunctionTool(
        name = "set_reminder",
        description = "Set a reminder for the child about learning goals, practice sessions, or important tasks.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "message" to mapOf(
                    "type" to "string",
                    "description" to "The reminder message"
                ),
                "when" to mapOf(
                    "type" to "string",
                    "description" to "When to remind",
                    "enum" to listOf("in_30_minutes", "in_1_hour", "tomorrow", "next_session")
                ),
                "type" to mapOf(
                    "type" to "string",
                    "description" to "Type of reminder",
                    "enum" to listOf("practice", "goal", "celebration", "break")
                )
            ),
            "required" to listOf("message", "when")
        )
    )

    /**
     * Tool to show specific learning content
     */
    private fun createShowLearningContent(): AIFunctionTool = AIFunctionTool(
        name = "show_learning_content",
        description = "Display specific learning content, explanations, or visual aids to help with understanding.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "content_type" to mapOf(
                    "type" to "string",
                    "description" to "Type of content to show",
                    "enum" to listOf("explanation", "example", "visual_aid", "practice_problem", "hint")
                ),
                "topic" to mapOf(
                    "type" to "string",
                    "description" to "The learning topic or subject"
                ),
                "difficulty" to mapOf(
                    "type" to "string",
                    "description" to "Difficulty level for the content",
                    "enum" to listOf("beginner", "intermediate", "advanced")
                )
            ),
            "required" to listOf("content_type", "topic")
        )
    )
}

// Tool execution result classes moved to ToolExecutionResult.kt

/**
 * Utility functions for parsing tool calls
 */
object ToolCallParser {
    
    fun parseStringParam(params: Map<String, Any>, key: String): String? {
        return params[key] as? String
    }
    
    fun parseIntParam(params: Map<String, Any>, key: String): Int? {
        return when (val value = params[key]) {
            is Int -> value
            is String -> value.toIntOrNull()
            is Double -> value.toInt()
            else -> null
        }
    }
    
    fun parseBooleanParam(params: Map<String, Any>, key: String): Boolean? {
        return when (val value = params[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
    }
} 