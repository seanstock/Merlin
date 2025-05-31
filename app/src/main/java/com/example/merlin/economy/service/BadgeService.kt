package com.example.merlin.economy.service

import com.example.merlin.economy.model.*

/**
 * Badge service interface for managing achievements, rewards, and recognition system.
 * Pure business logic contracts with no Android dependencies - ready for local or remote implementation.
 */
interface BadgeService {
    
    // ============= BADGE MANAGEMENT =============
    
    /**
     * Award a badge to a child
     */
    suspend fun awardBadge(badge: BadgeDto): Result<BadgeDto>
    
    /**
     * Award a badge by definition ID
     */
    suspend fun awardBadgeById(
        childId: String,
        badgeDefinitionId: String,
        earnedAt: String = ""  // ISO 8601, current time if empty
    ): Result<BadgeDto>
    
    /**
     * Get all badges earned by a child
     */
    suspend fun getBadges(childId: String): Result<List<BadgeDto>>
    
    /**
     * Get badges by category for a child
     */
    suspend fun getBadgesByCategory(
        childId: String,
        category: String
    ): Result<List<BadgeDto>>
    
    /**
     * Check if a child has a specific badge
     */
    suspend fun hasBadge(childId: String, badgeId: String): Result<Boolean>
    
    /**
     * Get badge statistics for a child
     */
    suspend fun getBadgeStats(childId: String): Result<BadgeStatsDto>
    
    // ============= BADGE DEFINITIONS =============
    
    /**
     * Get all available badge definitions
     */
    suspend fun getAllBadgeDefinitions(): Result<List<BadgeDefinitionDto>>
    
    /**
     * Get badge definitions by category
     */
    suspend fun getBadgeDefinitionsByCategory(category: String): Result<List<BadgeDefinitionDto>>
    
    /**
     * Get a specific badge definition
     */
    suspend fun getBadgeDefinition(badgeId: String): Result<BadgeDefinitionDto>
    
    /**
     * Check if badge requirements are met
     */
    suspend fun checkBadgeRequirements(
        childId: String,
        badgeDefinitionId: String
    ): Result<BadgeRequirementCheckDto>
    
    // ============= BADGE PROGRESS =============
    
    /**
     * Get progress towards badges that have multiple steps
     */
    suspend fun getBadgeProgress(
        childId: String,
        badgeDefinitionId: String
    ): Result<BadgeProgressDto>
    
    /**
     * Update progress towards a badge
     */
    suspend fun updateBadgeProgress(
        childId: String,
        badgeDefinitionId: String,
        progressIncrement: Int
    ): Result<BadgeProgressDto>
    
    /**
     * Get all badges in progress for a child
     */
    suspend fun getAllBadgeProgress(childId: String): Result<List<BadgeProgressDto>>
    
    // ============= AUTOMATIC BADGE CHECKING =============
    
    /**
     * Check for newly earned badges based on recent activity
     */
    suspend fun checkForNewBadges(childId: String): Result<List<BadgeDto>>
    
    /**
     * Check specific badge type after an activity
     */
    suspend fun checkBadgeAfterActivity(
        childId: String,
        activityType: String,
        activityData: Map<String, String>
    ): Result<List<BadgeDto>>
    
    /**
     * Evaluate all badge requirements for a child
     */
    suspend fun evaluateAllBadges(childId: String): Result<BadgeEvaluationResultDto>
    
    // ============= ANALYTICS & REPORTING =============
    
    /**
     * Get badge earning trends over time
     */
    suspend fun getBadgeEarningTrends(
        childId: String,
        days: Int = 30
    ): Result<List<BadgeEarningTrendDto>>
    
    /**
     * Get most popular badges across all children
     */
    suspend fun getPopularBadges(limit: Int = 10): Result<List<PopularBadgeDto>>
    
    /**
     * Generate badge recommendation for a child
     */
    suspend fun getBadgeRecommendations(childId: String): Result<List<BadgeRecommendationDto>>
}

/**
 * Badge requirement check result
 */
data class BadgeRequirementCheckDto(
    val badgeDefinitionId: String,
    val childId: String,
    val isEligible: Boolean,
    val requirementsMet: Map<String, Boolean>,  // requirement -> met status
    val missingRequirements: List<String>,
    val progressPercentage: Float,
    val estimatedTimeToEarn: String,  // human readable estimate
    val checkedAt: String             // ISO 8601 timestamp
)

/**
 * Badge evaluation result for all badges
 */
data class BadgeEvaluationResultDto(
    val childId: String,
    val newlyEarnedBadges: List<BadgeDto>,
    val progressUpdates: List<BadgeProgressDto>,
    val nearCompletionBadges: List<String>,  // Badge IDs close to completion
    val evaluatedAt: String                  // ISO 8601 timestamp
)

/**
 * Badge earning trend data
 */
data class BadgeEarningTrendDto(
    val date: String,           // ISO 8601 date
    val badgesEarned: Int,
    val badgesByCategory: Map<String, Int>,
    val rareestBadgeEarned: String?,  // Badge ID of rarest badge earned that day
    val totalBadgesEarnedToDate: Int
)

/**
 * Popular badge statistics
 */
data class PopularBadgeDto(
    val badgeDefinition: BadgeDefinitionDto,
    val totalEarned: Int,
    val earningRate: Float,     // Percentage of children who have earned it
    val averageTimeToEarn: Float, // Average days to earn this badge
    val difficulty: String      // easy, medium, hard based on earning rate
)

/**
 * Badge recommendation for a child
 */
data class BadgeRecommendationDto(
    val badgeDefinition: BadgeDefinitionDto,
    val childId: String,
    val recommendationReason: String,
    val currentProgress: Float,  // 0.0 to 1.0
    val estimatedDaysToEarn: Int,
    val difficulty: String,      // easy, medium, hard for this specific child
    val priority: String,        // high, medium, low
    val actionSuggestions: List<String>
) 