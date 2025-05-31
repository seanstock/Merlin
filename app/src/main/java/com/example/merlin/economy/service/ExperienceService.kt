package com.example.merlin.economy.service

import com.example.merlin.economy.model.*

/**
 * Experience service interface for managing XP, levels, and progression systems.
 * Pure business logic contracts with no Android dependencies - ready for local or remote implementation.
 */
interface ExperienceService {
    
    // ============= XP MANAGEMENT =============
    
    /**
     * Award experience points to a child
     */
    suspend fun awardXp(
        childId: String,
        amount: Int,
        source: String,
        description: String = "",
        metadata: Map<String, String> = emptyMap()
    ): Result<ExperienceDto>
    
    /**
     * Get current experience information for a child
     */
    suspend fun getExperience(childId: String): Result<ExperienceDto>
    
    /**
     * Calculate XP for a specific activity
     */
    suspend fun calculateXpForActivity(
        activityType: String,
        difficulty: Int = 1,
        performance: Float = 1.0f,  // 0.0 to 1.0 performance multiplier
        bonusMultipliers: Map<String, Float> = emptyMap()
    ): Result<Int>
    
    /**
     * Get XP transaction history
     */
    suspend fun getXpHistory(
        childId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<XpTransactionDto>>
    
    // ============= LEVEL MANAGEMENT =============
    
    /**
     * Check if child should level up and process level up if needed
     */
    suspend fun checkAndProcessLevelUp(childId: String): Result<LevelUpDto?>
    
    /**
     * Get level requirements for a specific level
     */
    suspend fun getLevelRequirements(level: Int): Result<Int>
    
    /**
     * Calculate level from total XP
     */
    suspend fun calculateLevelFromXp(totalXp: Int): Result<Int>
    
    /**
     * Get rewards unlocked at a specific level
     */
    suspend fun getLevelRewards(level: Int): Result<List<String>>
    
    /**
     * Check if child has unlocked a specific level-based feature
     */
    suspend fun hasUnlockedFeature(childId: String, featureId: String): Result<Boolean>
    
    // ============= ANALYTICS & STATISTICS =============
    
    /**
     * Get XP statistics for a child
     */
    suspend fun getXpStats(childId: String): Result<XpStatsDto>
    
    /**
     * Get XP breakdown by source
     */
    suspend fun getXpBreakdownBySource(
        childId: String,
        days: Int = 30
    ): Result<Map<String, Int>>
    
    /**
     * Get level progression history
     */
    suspend fun getLevelProgressionHistory(childId: String): Result<List<LevelUpDto>>
    
    /**
     * Calculate XP earning rate (XP per day)
     */
    suspend fun calculateXpEarningRate(
        childId: String,
        days: Int = 7
    ): Result<Float>
    
    // ============= LEADERBOARDS & COMPARISON =============
    
    /**
     * Get child's rank among peers in their age group
     */
    suspend fun getAgeGroupRank(childId: String, ageGroup: String): Result<LeaderboardPositionDto>
    
    /**
     * Get top children in age group by level
     */
    suspend fun getAgeGroupLeaderboard(
        ageGroup: String,
        limit: Int = 10
    ): Result<List<LeaderboardEntryDto>>
    
    /**
     * Get child's percentile ranking
     */
    suspend fun getPercentileRanking(childId: String): Result<Float>  // 0.0 to 100.0
    
    // ============= PROJECTIONS & RECOMMENDATIONS =============
    
    /**
     * Project when child will reach next level
     */
    suspend fun projectNextLevelDate(childId: String): Result<String>  // ISO 8601 date
    
    /**
     * Recommend activities to earn XP efficiently
     */
    suspend fun recommendXpActivities(childId: String): Result<List<XpActivityRecommendationDto>>
    
    /**
     * Calculate optimal XP strategy for level goals
     */
    suspend fun calculateXpStrategy(
        childId: String,
        targetLevel: Int,
        targetDate: String  // ISO 8601 date
    ): Result<XpStrategyDto>
    
    // ============= BATCH OPERATIONS =============
    
    /**
     * Award XP to multiple children (for group activities)
     */
    suspend fun awardXpBatch(
        awards: List<XpAwardDto>
    ): Result<List<ExperienceDto>>
    
    /**
     * Process multiple XP transactions
     */
    suspend fun processXpTransactions(
        transactions: List<XpTransactionDto>
    ): Result<Unit>
}

/**
 * Leaderboard position information
 */
data class LeaderboardPositionDto(
    val childId: String,
    val rank: Int,
    val totalParticipants: Int,
    val level: Int,
    val totalXp: Int,
    val ageGroup: String,
    val percentile: Float,          // 0.0 to 100.0
    val xpToNextRank: Int,         // XP needed to move up one rank
    val lastUpdated: String        // ISO 8601 timestamp
)

/**
 * Leaderboard entry for displaying rankings
 */
data class LeaderboardEntryDto(
    val rank: Int,
    val childId: String,
    val displayName: String,       // Anonymized or child's chosen display name
    val level: Int,
    val totalXp: Int,
    val recentXpGained: Int,       // XP gained in last week
    val badgeCount: Int,
    val profileImageUrl: String = ""
)

/**
 * XP activity recommendation
 */
data class XpActivityRecommendationDto(
    val activityType: String,
    val title: String,
    val description: String,
    val estimatedXp: Int,
    val estimatedTimeMinutes: Int,
    val difficulty: String,        // easy, medium, hard
    val xpEfficiency: Float,       // XP per minute
    val reason: String,           // Why this is recommended
    val prerequisites: List<String> = emptyList()
)

/**
 * XP strategy for reaching goals
 */
data class XpStrategyDto(
    val childId: String,
    val currentLevel: Int,
    val targetLevel: Int,
    val targetDate: String,        // ISO 8601 date
    val xpNeeded: Int,
    val daysAvailable: Int,
    val dailyXpTarget: Int,
    val recommendedActivities: List<XpActivityRecommendationDto>,
    val achievabilityScore: Float, // 0.0 to 1.0 - how realistic the goal is
    val alternativeStrategies: List<AlternativeStrategyDto>
)

/**
 * Alternative strategy option
 */
data class AlternativeStrategyDto(
    val name: String,
    val description: String,
    val dailyXpRequired: Int,
    val estimatedCompletionDate: String, // ISO 8601 date
    val difficulty: String,              // easy, medium, hard
    val mainActivities: List<String>
)

/**
 * XP award for batch operations
 */
data class XpAwardDto(
    val childId: String,
    val amount: Int,
    val source: String,
    val description: String,
    val metadata: Map<String, String> = emptyMap()
) 