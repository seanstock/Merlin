package com.example.merlin.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.merlin.economy.service.LocalEconomyService
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.screen.ScreenTimeTracker

/**
 * Executor for handling Merlin's AI function calls.
 * Bridges AI function calls to actual service implementations.
 */
class MerlinToolExecutor(
    private val context: Context,
    private val childId: String,
    private val onNavigateToScreen: (String, String?) -> Unit,
    private val onShowMessage: (String) -> Unit,
    private val onLaunchGame: (String, Int, String) -> Unit
) {
    companion object {
        private const val TAG = "MerlinToolExecutor"
    }

    // Lazy initialization of services
    private val database by lazy { DatabaseProvider.getInstance(context) }
    private val economyStateRepository by lazy { EconomyStateRepository(database.economyStateDao()) }
    private val childProfileRepository by lazy { ChildProfileRepository(database.childProfileDao()) }
    private val economyService by lazy { LocalEconomyService(economyStateRepository, childProfileRepository) }
    private val screenTimeTracker by lazy { ScreenTimeTracker(context) }

    /**
     * Execute a tool function called by the AI
     */
    suspend fun executeTool(functionName: String, arguments: Map<String, Any>): ToolExecutionResult {
        Log.d(TAG, "Executing tool: $functionName with arguments: $arguments")
        
        return try {
            when (functionName) {
                "start_game", "launch_game" -> executeStartGame(arguments)
                "grant_coins" -> executeGrantCoins(arguments)
                "check_coins" -> executeCheckCoins(arguments)
                "check_screen_time" -> executeCheckScreenTime(arguments)
                "award_bonus_coins" -> executeAwardBonusCoins(arguments)
                "navigate_to_screen" -> executeNavigateToScreen(arguments)
                "check_progress" -> executeCheckProgress(arguments)
                "set_reminder" -> executeSetReminder(arguments)
                "show_learning_content" -> executeShowLearningContent(arguments)
                else -> {
                    Log.w(TAG, "Unknown function: $functionName")
                    ToolExecutionResult(
                        success = false,
                        message = "I don't know how to do that yet. Let me learn more!",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing tool $functionName", e)
            ToolExecutionResult(
                success = false,
                message = "Oops! I had trouble with that. Let's try something else!",
                data = null
            )
        }
    }

    /**
     * Execute start_game/launch_game function
     */
    private fun executeStartGame(arguments: Map<String, Any>): ToolExecutionResult {
        val gameId = arguments["game_id"] as? String
        val level = (arguments["level"] as? Number)?.toInt() ?: 1
        val reason = arguments["reason"] as? String ?: "Let's have some fun learning!"

        return if (gameId != null) {
            onLaunchGame(gameId, level, reason)
            ToolExecutionResult(
                success = true,
                message = "$reason Ready to play $gameId? Let's go! 🎮",
                data = GameLaunchResult(gameId, level, true, reason)
            )
        } else {
            ToolExecutionResult(
                success = false,
                message = "I need to know which game you'd like to play!",
                data = null
            )
        }
    }

    /**
     * Execute grant_coins function - Award AI coins for positive behavior
     */
    private suspend fun executeGrantCoins(arguments: Map<String, Any>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val amount = (arguments["amount"] as? Number)?.toInt()
        val reason = arguments["reason"] as? String

        if (amount == null || amount < 1 || amount > 10) {
            return@withContext ToolExecutionResult(
                success = false,
                message = "I can only grant between 1 and 10 coins at a time!",
                data = null
            )
        }

        if (reason.isNullOrBlank()) {
            return@withContext ToolExecutionResult(
                success = false,
                message = "I need a good reason to award coins!",
                data = null
            )
        }

        try {
            // Use the childId passed in constructor
            val result = economyService.awardCoins(
                childId = childId,
                amount = amount,
                category = "ai_reward",
                description = "AI granted: $reason",
                metadata = mapOf(
                    "source" to "ai_interaction",
                    "reason" to reason,
                    "amount" to amount.toString()
                )
            )

            result.fold(
                onSuccess = { balanceChange ->
                    val message = "🎉 Awesome! You earned $amount Merlin Coins for $reason! " +
                            "You now have ${balanceChange.newBalance} MC total! ✨"
                    ToolExecutionResult(
                        success = true,
                        message = message,
                        data = balanceChange
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to grant coins", error)
                    ToolExecutionResult(
                        success = false,
                        message = "I had trouble awarding those coins. Let's try again later!",
                        data = null
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error granting coins", e)
            ToolExecutionResult(
                success = false,
                message = "Something went wrong while awarding coins. Let's keep learning though!",
                data = null
            )
        }
    }

    /**
     * Execute check_coins function - Check current coin balance
     */
    private suspend fun executeCheckCoins(arguments: Map<String, Any>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val includeDetails = arguments["include_details"] as? Boolean ?: false

        try {
            // Use the childId passed in constructor
            val balanceResult = economyService.getBalance(childId)
            
            balanceResult.fold(
                onSuccess = { balance ->
                    val minutes = balance.balance / 60
                    val seconds = balance.balance % 60
                    
                    val basicMessage = "💰 You have ${balance.balance} Merlin Coins! " +
                            "That's ${minutes}m ${seconds}s of screen time! ⏰"
                    
                    val detailedMessage = if (includeDetails) {
                        val realWorldValue = balance.balance / 25.0f // 25 MC = 1 cent
                        basicMessage + "\n" +
                                "📊 Today: Earned ${balance.todayEarned} MC, Spent ${balance.todaySpent} MC\n" +
                                "🎯 Daily cap: ${balance.dailyCap} MC\n" +
                                "💵 Real-world value: ${String.format("%.2f", realWorldValue)}¢"
                    } else {
                        basicMessage
                    }
                    
                    ToolExecutionResult(
                        success = true,
                        message = detailedMessage,
                        data = balance
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to get balance", error)
                    ToolExecutionResult(
                        success = false,
                        message = "I'm having trouble checking your coin balance right now. Let's try again!",
                        data = null
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking coins", e)
            ToolExecutionResult(
                success = false,
                message = "Oops! I couldn't check your coins right now.",
                data = null
            )
        }
    }

    /**
     * Execute check_screen_time function - Check current screen time usage
     */
    private suspend fun executeCheckScreenTime(arguments: Map<String, Any>): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            // Get actual screen time data using existing ScreenTimeTracker
            val todayUsageSeconds = screenTimeTracker.getTodayTotalTime(childId)
            val sessionUsageSeconds = screenTimeTracker.getCurrentSessionTime()
            
            val todayFormatted = screenTimeTracker.formatTime(todayUsageSeconds)
            val sessionFormatted = screenTimeTracker.formatTime(sessionUsageSeconds)
            
            val message = "📱 Screen Time Today: $todayFormatted\n" +
                    "⏱️ Current Session: $sessionFormatted\n" +
                    "Keep up the great learning! 🌟"
            
            ToolExecutionResult(
                success = true,
                message = message,
                data = mapOf(
                    "todayUsageSeconds" to todayUsageSeconds,
                    "sessionUsageSeconds" to sessionUsageSeconds,
                    "todayFormatted" to todayFormatted,
                    "sessionFormatted" to sessionFormatted
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking screen time", e)
            ToolExecutionResult(
                success = false,
                message = "I couldn't check your screen time right now.",
                data = null
            )
        }
    }

    /**
     * Execute award_bonus_coins function (legacy - delegates to grant_coins)
     */
    private suspend fun executeAwardBonusCoins(arguments: Map<String, Any>): ToolExecutionResult {
        return executeGrantCoins(arguments)
    }

    /**
     * Execute navigate_to_screen function
     */
    private fun executeNavigateToScreen(arguments: Map<String, Any>): ToolExecutionResult {
        val screen = arguments["screen"] as? String
        val message = arguments["message"] as? String

        return if (screen != null) {
            onNavigateToScreen(screen, message)
            ToolExecutionResult(
                success = true,
                message = message ?: "Taking you to $screen! 🧭",
                data = mapOf("screen" to screen, "message" to message)
            )
        } else {
            ToolExecutionResult(
                success = false,
                message = "I need to know where you'd like to go!",
                data = null
            )
        }
    }

    /**
     * Execute check_progress function
     */
    private suspend fun executeCheckProgress(arguments: Map<String, Any>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val category = arguments["category"] as? String ?: "overall"
        val days = (arguments["days"] as? Number)?.toInt() ?: 7

        // TODO: Implement actual progress checking
        val message = when (category) {
            "games" -> "🎮 You've been doing great in games! Keep playing and learning!"
            "coins" -> "💰 Your coin earning is on track! You're building good habits!"
            "achievements" -> "🏆 You've unlocked some amazing achievements recently!"
            "recent_activity" -> "📈 Your recent learning activity shows awesome progress!"
            else -> "🌟 Overall, you're doing fantastic! Keep up the excellent work!"
        }

        ToolExecutionResult(
            success = true,
            message = message,
            data = mapOf("category" to category, "days" to days)
        )
    }

    /**
     * Execute set_reminder function
     */
    private fun executeSetReminder(arguments: Map<String, Any>): ToolExecutionResult {
        val reminderMessage = arguments["message"] as? String
        val whenRemind = arguments["when"] as? String
        val type = arguments["type"] as? String ?: "general"

        return if (reminderMessage != null && whenRemind != null) {
            // TODO: Implement actual reminder system
            val responseMessage = "⏰ I've set a reminder for you: \"$reminderMessage\" $whenRemind! " +
                    "I'll make sure to remind you! 📝"
            
            ToolExecutionResult(
                success = true,
                message = responseMessage,
                data = mapOf(
                    "message" to reminderMessage,
                    "when" to whenRemind,
                    "type" to type
                )
            )
        } else {
            ToolExecutionResult(
                success = false,
                message = "I need to know what to remind you about and when!",
                data = null
            )
        }
    }

    /**
     * Execute show_learning_content function
     */
    private fun executeShowLearningContent(arguments: Map<String, Any>): ToolExecutionResult {
        val contentType = arguments["content_type"] as? String
        val topic = arguments["topic"] as? String
        val difficulty = arguments["difficulty"] as? String ?: "beginner"

        return if (contentType != null && topic != null) {
            // TODO: Implement actual content display
            val message = when (contentType) {
                "explanation" -> "📚 Let me explain $topic in a simple way that's perfect for you!"
                "example" -> "💡 Here's a great example of $topic to help you understand!"
                "visual_aid" -> "🎨 I'd love to show you a visual representation of $topic!"
                "practice_problem" -> "🧩 Ready for a $topic practice problem? This will be fun!"
                "hint" -> "💭 Here's a helpful hint about $topic to guide you!"
                else -> "🌟 Let me share some great content about $topic!"
            }

            ToolExecutionResult(
                success = true,
                message = message,
                data = mapOf(
                    "contentType" to contentType,
                    "topic" to topic,
                    "difficulty" to difficulty
                )
            )
        } else {
            ToolExecutionResult(
                success = false,
                message = "I need to know what topic and content type you'd like to see!",
                data = null
            )
        }
    }
} 