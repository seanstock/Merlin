package com.example.merlin.ui.game

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Handles game completion results and integrates with the database.
 * Processes game outcomes, calculates scores, and stores results for tracking progress.
 */
class GameResultHandler(
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "GameResultHandler"
        
        // Score calculation constants
        private const val BASE_SCORE = 100
        private const val TIME_BONUS_THRESHOLD_MS = 30000L // 30 seconds
        private const val MAX_TIME_BONUS = 50
        private const val COMPLETION_BONUS = 25
    }

    /**
     * Processes a game completion result.
     * @param gameId Identifier of the completed game
     * @param level Level that was completed
     * @param success Whether the game was completed successfully
     * @param timeMs Time taken to complete the game in milliseconds
     * @param rawScore Raw score from the game (if any)
     * @param childProfileId ID of the child profile (if applicable)
     * @param onResult Callback with the final processed result
     */
    fun handleGameResult(
        gameId: String,
        level: Int,
        success: Boolean,
        timeMs: Long,
        rawScore: Int = 0,
        childProfileId: Long? = null,
        onResult: (GameResult) -> Unit = {}
    ) {
        coroutineScope.launch {
            try {
                val result = processGameResult(
                    gameId = gameId,
                    level = level,
                    success = success,
                    timeMs = timeMs,
                    rawScore = rawScore,
                    childProfileId = childProfileId
                )
                
                // Store result in database
                storeGameResult(result)
                
                // Notify callback
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
                
                Log.d(TAG, "Game result processed: $result")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing game result", e)
                withContext(Dispatchers.Main) {
                    onResult(
                        GameResult(
                            gameId = gameId,
                            level = level,
                            success = false,
                            timeMs = timeMs,
                            finalScore = 0,
                            timestamp = Date(),
                            error = "Failed to process result: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    /**
     * Processes the raw game data into a structured result.
     */
    private suspend fun processGameResult(
        gameId: String,
        level: Int,
        success: Boolean,
        timeMs: Long,
        rawScore: Int,
        childProfileId: Long?
    ): GameResult = withContext(Dispatchers.Default) {
        
        val finalScore = if (success) {
            calculateFinalScore(timeMs, rawScore, level)
        } else {
            0
        }
        
        val performance = calculatePerformance(success, timeMs, level)
        
        GameResult(
            gameId = gameId,
            level = level,
            success = success,
            timeMs = timeMs,
            rawScore = rawScore,
            finalScore = finalScore,
            performance = performance,
            timestamp = Date(),
            childProfileId = childProfileId
        )
    }

    /**
     * Calculates the final score based on completion time, raw score, and level.
     */
    private fun calculateFinalScore(timeMs: Long, rawScore: Int, level: Int): Int {
        var score = BASE_SCORE
        
        // Add raw score from game
        score += rawScore
        
        // Add completion bonus
        score += COMPLETION_BONUS
        
        // Add time bonus (faster completion = higher bonus)
        if (timeMs < TIME_BONUS_THRESHOLD_MS) {
            val timeBonus = ((TIME_BONUS_THRESHOLD_MS - timeMs) / 1000.0 * 2).toInt()
            score += timeBonus.coerceAtMost(MAX_TIME_BONUS)
        }
        
        // Level multiplier
        score = (score * (1.0 + (level - 1) * 0.1)).toInt()
        
        return score.coerceAtLeast(0)
    }

    /**
     * Calculates performance rating based on completion and time.
     */
    private fun calculatePerformance(success: Boolean, timeMs: Long, level: Int): GamePerformance {
        if (!success) return GamePerformance.FAILED
        
        // Adjust thresholds based on level difficulty
        val baseThreshold = 30000L // 30 seconds
        val levelAdjustment = (level - 1) * 10000L // +10 seconds per level
        val excellentThreshold = baseThreshold + levelAdjustment
        val goodThreshold = excellentThreshold * 1.5
        val averageThreshold = excellentThreshold * 2.0
        
        return when {
            timeMs <= excellentThreshold -> GamePerformance.EXCELLENT
            timeMs <= goodThreshold -> GamePerformance.GOOD
            timeMs <= averageThreshold -> GamePerformance.AVERAGE
            else -> GamePerformance.NEEDS_IMPROVEMENT
        }
    }

    /**
     * Stores the game result in the database.
     * Note: This is a placeholder implementation. In a real app, you would
     * inject the actual repository/DAO to store the results.
     */
    private suspend fun storeGameResult(result: GameResult) = withContext(Dispatchers.IO) {
        try {
            // TODO: Integrate with actual database repository
            // Example:
            // gameRepository.insertGameResult(result.toEntity())
            
            Log.d(TAG, "Game result stored: ${result.gameId} - Score: ${result.finalScore}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store game result", e)
            throw e
        }
    }

    /**
     * Retrieves game statistics for a specific game and level.
     */
    suspend fun getGameStats(gameId: String, level: Int): GameStats = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement actual database query
            // Example:
            // val results = gameRepository.getGameResults(gameId, level)
            // return GameStats.fromResults(results)
            
            // Placeholder implementation
            GameStats(
                gameId = gameId,
                level = level,
                totalAttempts = 0,
                successfulAttempts = 0,
                averageTime = 0L,
                bestScore = 0,
                averageScore = 0.0
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve game stats", e)
            throw e
        }
    }
}

/**
 * Represents a processed game result.
 */
data class GameResult(
    val gameId: String,
    val level: Int,
    val success: Boolean,
    val timeMs: Long,
    val rawScore: Int = 0,
    val finalScore: Int,
    val performance: GamePerformance = GamePerformance.AVERAGE,
    val timestamp: Date,
    val childProfileId: Long? = null,
    val error: String? = null
)

/**
 * Performance rating for game completion.
 */
enum class GamePerformance {
    EXCELLENT,
    GOOD,
    AVERAGE,
    NEEDS_IMPROVEMENT,
    FAILED
}

/**
 * Statistics for a specific game and level.
 */
data class GameStats(
    val gameId: String,
    val level: Int,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val averageTime: Long,
    val bestScore: Int,
    val averageScore: Double
) {
    val successRate: Double
        get() = if (totalAttempts > 0) successfulAttempts.toDouble() / totalAttempts else 0.0
} 