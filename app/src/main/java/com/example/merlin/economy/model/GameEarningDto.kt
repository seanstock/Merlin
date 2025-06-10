package com.example.merlin.economy.model

/**
 * DTO for game-based coin earning operations with daily limit tracking
 */
data class GameEarningDto(
    val childId: String,
    val gameId: String,
    val coinsAwarded: Int,
    val actualAmount: Int,
    val wasLimited: Boolean,
    val dailyEarned: Int,
    val dailyLimit: Int,
    val remainingToday: Int,
    val source: String,
    val timestamp: String
) 