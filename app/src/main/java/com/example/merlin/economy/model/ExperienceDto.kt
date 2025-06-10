package com.example.merlin.economy.model

/**
 * Data transfer object for experience/XP tracking.
 * Pure Kotlin data class with no Android dependencies, fully serializable for API transport.
 */
data class ExperienceDto(
    val childId: String,
    val level: Int,
    val currentXp: Int,           // XP in current level
    val nextLevelXp: Int,         // XP needed for next level
    val totalXpEarned: Int        // Total XP earned across all time
) {
    val progressToNextLevel: Float 
        get() = if (nextLevelXp > 0) currentXp.toFloat() / nextLevelXp.toFloat() else 1f
        
    val progressPercentage: Float 
        get() = progressToNextLevel * 100f
}

/**
 * XP sources for tracking where experience comes from
 */
object XpSource {
    const val TASK_COMPLETION = "task_completion"
    const val FIRST_TRY_BONUS = "first_try_bonus"
    const val PERFECT_COMPLETION = "perfect_completion"
    const val NEW_CONCEPT_MASTERY = "new_concept_mastery"
    const val TEACHING_MODE = "teaching_mode"
    const val DAILY_LOGIN = "daily_login"
    const val STREAK_BONUS = "streak_bonus"
    const val BADGE_EARNED = "badge_earned"
    const val LEVEL_UP_BONUS = "level_up_bonus"
    const val SPECIAL_EVENT = "special_event"
    
    val ALL_SOURCES = setOf(
        TASK_COMPLETION,
        FIRST_TRY_BONUS,
        PERFECT_COMPLETION,
        NEW_CONCEPT_MASTERY,
        TEACHING_MODE,
        DAILY_LOGIN,
        STREAK_BONUS,
        BADGE_EARNED,
        LEVEL_UP_BONUS,
        SPECIAL_EVENT
    )
    
    /**
     * Base XP amounts for different sources
     */
    val BASE_XP_AMOUNTS = mapOf(
        TASK_COMPLETION to 10,
        FIRST_TRY_BONUS to 5,
        PERFECT_COMPLETION to 15,
        NEW_CONCEPT_MASTERY to 25,
        TEACHING_MODE to 20,
        DAILY_LOGIN to 5,
        STREAK_BONUS to 10,
        BADGE_EARNED to 50,
        LEVEL_UP_BONUS to 100,
        SPECIAL_EVENT to 30
    )
}

/**
 * Level progression configuration
 */
object LevelProgression {
    /**
     * Calculate XP required for a specific level
     * Uses exponential curve: level^2 * 100 + level * 50
     */
    fun getXpRequiredForLevel(level: Int): Int {
        return if (level <= 1) {
            0
        } else {
            level * level * 100 + level * 50
        }
    }
    
    /**
     * Calculate current level based on total XP
     */
    fun getLevelFromTotalXp(totalXp: Int): Int {
        var level = 1
        while (getXpRequiredForLevel(level + 1) <= totalXp) {
            level++
        }
        return level
    }
    
    /**
     * Get XP needed for next level from current level
     */
    fun getXpForNextLevel(currentLevel: Int): Int {
        return getXpRequiredForLevel(currentLevel + 1) - getXpRequiredForLevel(currentLevel)
    }
    
    /**
     * Get maximum level (can be increased as needed)
     */
    fun getMaxLevel(): Int = 50
}

/**
 * XP transaction for tracking experience gains
 */
data class XpTransactionDto(
    val id: String,
    val childId: String,
    val amount: Int,
    val source: String,         // From XpSource
    val description: String,
    val timestamp: String,      // ISO 8601 string
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Level up event information
 */
data class LevelUpDto(
    val childId: String,
    val previousLevel: Int,
    val newLevel: Int,
    val bonusXp: Int,          // Bonus XP awarded for leveling up
    val rewards: List<String>, // Rewards unlocked at this level
    val timestamp: String      // ISO 8601 string
)

/**
 * XP statistics for reporting
 */
data class XpStatsDto(
    val childId: String,
    val totalXpEarned: Int,
    val currentLevel: Int,
    val xpThisWeek: Int,
    val xpThisMonth: Int,
    val averageXpPerDay: Float,
    val xpBySource: Map<String, Int>,  // source -> total XP
    val levelUpsThisMonth: Int,
    val lastUpdated: String            // ISO 8601 timestamp
) 