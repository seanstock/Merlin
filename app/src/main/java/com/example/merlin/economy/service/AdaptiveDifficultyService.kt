package com.example.merlin.economy.service

import com.example.merlin.economy.model.*

/**
 * Adaptive difficulty service interface for maintaining optimal challenge levels.
 * Targets ~80% success rate to keep children engaged but not frustrated.
 * Pure business logic contracts with no Android dependencies - ready for local or remote implementation.
 */
interface AdaptiveDifficultyService {
    
    // ============= DIFFICULTY CALCULATION =============
    
    /**
     * Calculate appropriate task difficulty for a child in a specific subject
     */
    suspend fun calculateTaskDifficulty(
        childId: String,
        subject: String
    ): Result<Int>
    
    /**
     * Get difficulty recommendation with detailed analysis
     */
    suspend fun getDifficultyRecommendation(
        childId: String,
        subject: String
    ): Result<DifficultyRecommendationDto>
    
    /**
     * Predict success probability for a given difficulty level
     */
    suspend fun predictSuccessProbability(
        childId: String,
        subject: String,
        difficulty: Int
    ): Result<Float>
    
    // ============= PERFORMANCE TRACKING =============
    
    /**
     * Record the result of a completed task
     */
    suspend fun recordTaskResult(
        childId: String,
        subject: String,
        difficulty: Int,
        success: Boolean,
        timeSpent: Int = 0,
        hintsUsed: Int = 0,
        attemptsToSolve: Int = 1,
        metadata: Map<String, String> = emptyMap()
    ): Result<Unit>
    
    /**
     * Record multiple task results in batch
     */
    suspend fun recordTaskResults(
        results: List<TaskResultDto>
    ): Result<Unit>
    
    /**
     * Get performance statistics for a child in a subject
     */
    suspend fun getPerformanceStats(
        childId: String,
        subject: String
    ): Result<PerformanceStatsDto>
    
    /**
     * Get recent task results for analysis
     */
    suspend fun getRecentTaskResults(
        childId: String,
        subject: String,
        limit: Int = 10
    ): Result<List<TaskResultDto>>
    
    // ============= LEARNING ANALYSIS =============
    
    /**
     * Analyze learning patterns for a child
     */
    suspend fun analyzeLearningPatterns(
        childId: String,
        subject: String
    ): Result<LearningPatternDto>
    
    /**
     * Get performance trends over time
     */
    suspend fun getPerformanceTrends(
        childId: String,
        subject: String,
        weeks: Int = 4
    ): Result<List<PerformanceTrendDto>>
    
    /**
     * Calculate subject mastery level
     */
    suspend fun calculateSubjectMastery(
        childId: String,
        subject: String
    ): Result<SubjectMasteryDto>
    
    /**
     * Get all subjects with mastery information
     */
    suspend fun getAllSubjectMastery(
        childId: String
    ): Result<List<SubjectMasteryDto>>
    
    // ============= RECOMMENDATIONS =============
    
    /**
     * Get personalized learning recommendations
     */
    suspend fun getLearningRecommendations(
        childId: String,
        subject: String
    ): Result<List<LearningRecommendationDto>>
    
    /**
     * Recommend optimal session length based on performance patterns
     */
    suspend fun recommendSessionLength(
        childId: String,
        subject: String
    ): Result<Int>  // Recommended session length in seconds
    
    /**
     * Recommend best time of day for learning based on performance
     */
    suspend fun recommendOptimalLearningTime(
        childId: String
    ): Result<OptimalLearningTimeDto>
    
    // ============= ANALYTICS & REPORTING =============
    
    /**
     * Generate performance report for parents/educators
     */
    suspend fun generatePerformanceReport(
        childId: String,
        startDate: String,  // ISO 8601
        endDate: String     // ISO 8601
    ): Result<PerformanceReportDto>
    
    /**
     * Get difficulty adjustment history
     */
    suspend fun getDifficultyAdjustmentHistory(
        childId: String,
        subject: String,
        limit: Int = 20
    ): Result<List<DifficultyAdjustmentDto>>
    
    /**
     * Calculate overall learning velocity across all subjects
     */
    suspend fun calculateLearningVelocity(
        childId: String,
        days: Int = 30
    ): Result<Float>
}

/**
 * Learning recommendation with action items
 */
data class LearningRecommendationDto(
    val id: String,
    val childId: String,
    val subject: String,
    val type: String,           // focus_area, difficulty_adjustment, session_timing, etc.
    val title: String,
    val description: String,
    val actionItems: List<String>,
    val estimatedImpact: String,  // high, medium, low
    val confidence: Float,        // 0.0 to 1.0
    val timestamp: String         // ISO 8601 string
)

/**
 * Optimal learning time recommendation
 */
data class OptimalLearningTimeDto(
    val childId: String,
    val recommendedHour: Int,     // 0-23 hour of day
    val confidenceScore: Float,   // 0.0 to 1.0
    val averagePerformanceAtTime: Float,
    val sampleSize: Int,          // Number of sessions analyzed
    val alternativeHours: List<Int>, // Other good times
    val timestamp: String         // ISO 8601 string
)

/**
 * Comprehensive performance report
 */
data class PerformanceReportDto(
    val childId: String,
    val reportPeriod: String,     // ISO 8601 date range
    val overallStats: PerformanceStatsDto,
    val subjectBreakdown: List<SubjectMasteryDto>,
    val improvementAreas: List<String>,
    val strengthAreas: List<String>,
    val keyAchievements: List<String>,
    val recommendations: List<LearningRecommendationDto>,
    val difficultyProgression: List<DifficultyAdjustmentDto>,
    val learningVelocity: Float,
    val generatedAt: String       // ISO 8601 string
)

/**
 * Difficulty adjustment tracking
 */
data class DifficultyAdjustmentDto(
    val id: String,
    val childId: String,
    val subject: String,
    val previousDifficulty: Int,
    val newDifficulty: Int,
    val reason: String,
    val successRateBefore: Float,
    val tasksAnalyzed: Int,
    val confidence: Float,
    val timestamp: String         // ISO 8601 string
) 