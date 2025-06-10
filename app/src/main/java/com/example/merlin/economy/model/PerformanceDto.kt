package com.example.merlin.economy.model

/**
 * Data transfer object for performance statistics tracking.
 * Pure Kotlin data class with no Android dependencies, fully serializable for API transport.
 */
data class PerformanceStatsDto(
    val childId: String,
    val subject: String,
    val successRate: Float,         // 0.0 to 1.0
    val averageDifficulty: Float,   // Average difficulty level attempted
    val tasksCompleted: Int,
    val tasksSucceeded: Int,
    val tasksFailed: Int,
    val currentStreak: Int,         // Current success streak
    val bestStreak: Int,           // Best success streak ever
    val lastUpdated: String        // ISO 8601 string
) {
    val failureRate: Float get() = 1.0f - successRate
    val successPercentage: Float get() = successRate * 100f
}

/**
 * Individual task result for tracking performance
 */
data class TaskResultDto(
    val id: String,
    val childId: String,
    val subject: String,
    val difficulty: Int,           // 1-5 difficulty level
    val success: Boolean,
    val timeSpent: Int,           // Time in seconds
    val hintsUsed: Int,
    val attemptsToSolve: Int,
    val timestamp: String,        // ISO 8601 string
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Difficulty recommendation based on performance analysis
 */
data class DifficultyRecommendationDto(
    val childId: String,
    val subject: String,
    val currentDifficulty: Int,
    val recommendedDifficulty: Int,
    val reason: String,                    // Explanation for the recommendation
    val confidence: Float,                 // 0.0 to 1.0 confidence in recommendation
    val analysisWindow: Int,              // Number of recent tasks analyzed
    val successRateInWindow: Float,       // Success rate in analysis window
    val timestamp: String                 // ISO 8601 string
)

/**
 * Learning pattern analysis
 */
data class LearningPatternDto(
    val childId: String,
    val subject: String,
    val preferredDifficulty: Int,         // Difficulty level where child performs best
    val optimalSessionLength: Int,        // Optimal session length in seconds
    val bestTimeOfDay: String,           // Time when child performs best
    val commonMistakePatterns: List<String>,
    val strengthAreas: List<String>,
    val improvementAreas: List<String>,
    val learningVelocity: Float,         // Rate of improvement over time
    val lastAnalyzed: String             // ISO 8601 string
)

/**
 * Adaptive difficulty configuration
 */
object AdaptiveDifficulty {
    const val MIN_DIFFICULTY = 1
    const val MAX_DIFFICULTY = 5
    const val TARGET_SUCCESS_RATE = 0.8f     // 80% success rate target
    const val SUCCESS_RATE_TOLERANCE = 0.1f  // ±10% tolerance
    
    const val MIN_TASKS_FOR_ANALYSIS = 5     // Minimum tasks before adjusting difficulty
    const val ANALYSIS_WINDOW_SIZE = 10      // Number of recent tasks to analyze
    
    /**
     * Determine if difficulty should be adjusted based on success rate
     */
    fun shouldIncreaseDifficulty(successRate: Float): Boolean {
        return successRate > (TARGET_SUCCESS_RATE + SUCCESS_RATE_TOLERANCE)
    }
    
    fun shouldDecreaseDifficulty(successRate: Float): Boolean {
        return successRate < (TARGET_SUCCESS_RATE - SUCCESS_RATE_TOLERANCE)
    }
    
    /**
     * Calculate confidence in difficulty recommendation based on sample size
     */
    fun calculateConfidence(sampleSize: Int): Float {
        return when {
            sampleSize < MIN_TASKS_FOR_ANALYSIS -> 0.2f
            sampleSize < ANALYSIS_WINDOW_SIZE -> 0.6f
            sampleSize < 20 -> 0.8f
            else -> 1.0f
        }
    }
}

/**
 * Performance trend analysis over time
 */
data class PerformanceTrendDto(
    val childId: String,
    val subject: String,
    val timePerformanceWeek: String,      // ISO 8601 week
    val weeklySuccessRate: Float,
    val weeklyTasksCompleted: Int,
    val weeklyAverageDifficulty: Float,
    val improvementFromPreviousWeek: Float, // Positive = improvement, negative = decline
    val longestStreakThisWeek: Int,
    val totalTimeSpentMinutes: Int,
    val timestamp: String                  // ISO 8601 string
)

/**
 * DTO for subject-specific mastery level
 */
data class SubjectMasteryDto(
    val childId: String,
    val subject: String,
    val gradeLevel: Int,
    val masteryScore: Float,    // Progress within the current grade level (0.0 to 1.0)
    val masteryLevel: String,   // "Beginner", "Intermediate", "Advanced" - descriptive label
    val topicsCompleted: List<String>,
    val topicsInProgress: List<String>,
    val topicsNotStarted: List<String>,
    val estimatedTimeToNextLevel: Int, // In hours
    val lastUpdated: String
)

/**
 * Mastery levels
 */
object MasteryLevel {
    const val BEGINNER = "beginner"       // 0.0 - 0.25
    const val INTERMEDIATE = "intermediate" // 0.25 - 0.60
    const val ADVANCED = "advanced"       // 0.60 - 0.85
    const val EXPERT = "expert"          // 0.85 - 1.0
    
    fun getMasteryLevel(score: Float): String = when {
        score < 0.25f -> BEGINNER
        score < 0.60f -> INTERMEDIATE
        score < 0.85f -> ADVANCED
        else -> EXPERT
    }
    
    fun getMasteryColor(level: String): String = when (level) {
        BEGINNER -> "#FF9800"      // Orange
        INTERMEDIATE -> "#2196F3"  // Blue
        ADVANCED -> "#4CAF50"      // Green
        EXPERT -> "#9C27B0"        // Purple
        else -> "#9E9E9E"          // Gray
    }
} 