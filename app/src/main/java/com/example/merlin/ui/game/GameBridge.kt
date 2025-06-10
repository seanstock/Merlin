package com.example.merlin.ui.game

import android.webkit.JavascriptInterface
import android.util.Log

/**
 * JavaScript bridge for communication between WebView games and native Android app.
 * This class provides a secure interface for games to report completion status,
 * scores, and other game events back to the native application.
 */
class GameBridge(
    private val onGameComplete: (Boolean, Long, Int) -> Unit,
    private val onGameProgress: (Int) -> Unit = {},
    private val onGameError: (String) -> Unit = {},
    private val onCoinEarned: ((Int, String, String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "GameBridge"
        const val BRIDGE_NAME = "MerlinGameBridge"
    }

    /**
     * Called by JavaScript when a game is completed.
     * @param success Whether the game was completed successfully
     * @param timeMs Time taken to complete the game in milliseconds
     * @param score Optional score achieved in the game (default: 0)
     */
    @JavascriptInterface
    fun gameCompleted(success: Boolean, timeMs: Long, score: Int = 0) {
        Log.d(TAG, "Game completed: success=$success, time=${timeMs}ms, score=$score")
        try {
            onGameComplete(success, timeMs, score)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling game completion", e)
        }
    }

    /**
     * Called by JavaScript to report game progress.
     * @param progressPercent Progress as a percentage (0-100)
     */
    @JavascriptInterface
    fun updateProgress(progressPercent: Int) {
        Log.d(TAG, "Game progress: $progressPercent%")
        try {
            val clampedProgress = progressPercent.coerceIn(0, 100)
            onGameProgress(clampedProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling progress update", e)
        }
    }

    /**
     * Called by JavaScript when a player earns coins during gameplay.
     * @param amount Number of coins to award
     * @param gameId ID of the game awarding coins (e.g., "color-match")
     * @param source Description of earning source (e.g., "correct color match")
     * @return JSON string with earning result including daily limit info
     */
    @JavascriptInterface
    fun earnCoins(amount: Int, gameId: String, source: String): String {
        Log.d(TAG, "Coins earned: amount=$amount, game=$gameId, source=$source")
        try {
            if (onCoinEarned != null) {
                onCoinEarned.invoke(amount, gameId, source)
                return """{"success": true, "message": "Coins awarded"}"""
            } else {
                Log.w(TAG, "No coin earning handler configured")
                return """{"success": false, "message": "Coin earning not available"}"""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling coin earning", e)
            return """{"success": false, "message": "Error awarding coins"}"""
        }
    }

    /**
     * Called by JavaScript when a game error occurs.
     * @param errorMessage Description of the error
     */
    @JavascriptInterface
    fun reportError(errorMessage: String) {
        Log.e(TAG, "Game error reported: $errorMessage")
        try {
            onGameError(errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling game error report", e)
        }
    }

    /**
     * Called by JavaScript to log debug information.
     * @param message Debug message from the game
     */
    @JavascriptInterface
    fun logDebug(message: String) {
        Log.d(TAG, "Game debug: $message")
    }

    /**
     * Called by JavaScript to request native app information.
     * @return JSON string with app information
     */
    @JavascriptInterface
    fun getAppInfo(): String {
        return """
            {
                "version": "1.0",
                "platform": "android",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
    }
} 