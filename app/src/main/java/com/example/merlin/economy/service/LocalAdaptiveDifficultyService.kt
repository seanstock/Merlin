package com.example.merlin.economy.service

import android.util.Log
import com.example.merlin.economy.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.*

/**
 * Local implementation of adaptive difficulty service.
 * Maintains ~80% success rate through intelligent difficulty adjustment.
 */
class LocalAdaptiveDifficultyService : AdaptiveDifficultyService {
    
    companion object {
        private const val TAG = "LocalAdaptiveDifficultyService"
    }
    
    // In-memory storage for demo (replace with repository/database in production)
    private val taskResults = mutableMapOf<String, MutableList<TaskResultDto>>()
    private val performanceStats = mutableMapOf<String, PerformanceStatsDto>()
    private val difficultyAdjustments = mutableMapOf<String, MutableList<DifficultyAdjustmentDto>>()
    
    // ============= SAMPLE DATA GENERATION =============

    suspend fun generateSampleData(childId: String, subject: String) {
        val key = "$childId-$subject"
        if (taskResults[key].isNullOrEmpty()) {
            repeat(15) {
                val success = Math.random() < 0.8 // Maintain ~80% success rate
                recordTaskResult(
                    childId = childId,
                    subject = subject,
                    difficulty = (1..5).random(),
                    success = success,
                    timeSpent = (30..300).random(),
                    hintsUsed = (0..2).random(),
                    attemptsToSolve = if (success) 1 else (1..3).random(),
                    metadata = mapOf("sample" to "true")
                ).getOrThrow()
            }
        }
    }
    
    // ============= DIFFICULTY CALCULATION =============
    
    override suspend fun calculateTaskDifficulty(
        childId: String,
        subject: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Calculating task difficulty for child: $childId, subject: $subject")
            
            val stats = getPerformanceStats(childId, subject).getOrNull()
            
            // If no history, start with medium difficulty
            if (stats == null || stats.tasksCompleted < AdaptiveDifficulty.MIN_TASKS_FOR_ANALYSIS) {
                val defaultDifficulty = 3 // Medium difficulty
                Log.d(TAG, "No sufficient history, using default difficulty: $defaultDifficulty")
                return@withContext Result.success(defaultDifficulty)
            }
            
            val currentDifficulty = stats.averageDifficulty.toInt()
            val successRate = stats.successRate
            
            val newDifficulty = when {
                AdaptiveDifficulty.shouldIncreaseDifficulty(successRate) -> {
                    (currentDifficulty + 1).coerceAtMost(AdaptiveDifficulty.MAX_DIFFICULTY)
                }
                AdaptiveDifficulty.shouldDecreaseDifficulty(successRate) -> {
                    (currentDifficulty - 1).coerceAtLeast(AdaptiveDifficulty.MIN_DIFFICULTY)
                }
                else -> currentDifficulty
            }
            
            // Record difficulty adjustment if changed
            if (newDifficulty != currentDifficulty) {
                recordDifficultyAdjustment(childId, subject, currentDifficulty, newDifficulty, successRate, stats.tasksCompleted)
            }
            
            Log.d(TAG, "Calculated difficulty: $newDifficulty (was: $currentDifficulty, success rate: $successRate)")
            Result.success(newDifficulty)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating task difficulty", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getDifficultyRecommendation(
        childId: String,
        subject: String
    ): Result<DifficultyRecommendationDto> = withContext(Dispatchers.IO) {
        try {
            val difficulty = calculateTaskDifficulty(childId, subject).getOrThrow()
            val stats = getPerformanceStats(childId, subject).getOrNull()
            val confidence = AdaptiveDifficulty.calculateConfidence(stats?.tasksCompleted ?: 0)
            
            val recommendation = DifficultyRecommendationDto(
                childId = childId,
                subject = subject,
                currentDifficulty = stats?.averageDifficulty?.toInt() ?: 3,
                recommendedDifficulty = difficulty,
                reason = generateReasoningText(stats?.successRate ?: 0f, difficulty),
                confidence = confidence,
                analysisWindow = AdaptiveDifficulty.ANALYSIS_WINDOW_SIZE,
                successRateInWindow = stats?.successRate ?: 0f,
                timestamp = Instant.now().toString()
            )
            
            Result.success(recommendation)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting difficulty recommendation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun predictSuccessProbability(
        childId: String,
        subject: String,
        difficulty: Int
    ): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val stats = getPerformanceStats(childId, subject).getOrNull()
            
            // If no history, use baseline estimates
            if (stats == null || stats.tasksCompleted < AdaptiveDifficulty.MIN_TASKS_FOR_ANALYSIS) {
                val baseline = when (difficulty) {
                    1 -> 0.95f
                    2 -> 0.90f
                    3 -> 0.80f
                    4 -> 0.65f
                    5 -> 0.50f
                    else -> 0.80f
                }
                return@withContext Result.success(baseline)
            }
            
            // Calculate prediction based on current performance and difficulty delta
            val currentDifficulty = stats.averageDifficulty
            val difficultyDelta = difficulty - currentDifficulty
            
            // Adjust success rate based on difficulty change
            val adjustedSuccessRate = stats.successRate * (1f - (difficultyDelta * 0.15f))
            val clampedProbability = adjustedSuccessRate.coerceIn(0.1f, 0.95f)
            
            Result.success(clampedProbability)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting success probability", e)
            Result.failure(e)
        }
    }
    
    // ============= PERFORMANCE TRACKING =============
    
    override suspend fun recordTaskResult(
        childId: String,
        subject: String,
        difficulty: Int,
        success: Boolean,
        timeSpent: Int,
        hintsUsed: Int,
        attemptsToSolve: Int,
        metadata: Map<String, String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val key = "$childId-$subject"
            val result = TaskResultDto(
                id = UUID.randomUUID().toString(),
                childId = childId,
                subject = subject,
                difficulty = difficulty,
                success = success,
                timeSpent = timeSpent,
                hintsUsed = hintsUsed,
                attemptsToSolve = attemptsToSolve,
                metadata = metadata,
                timestamp = Instant.now().toString()
            )
            
            taskResults.getOrPut(key) { mutableListOf() }.add(result)
            
            // Update performance stats
            updatePerformanceStats(childId, subject)
            
            Log.d(TAG, "Recorded task result: $result")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recording task result", e)
            Result.failure(e)
        }
    }
    
    override suspend fun recordTaskResults(
        results: List<TaskResultDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            results.forEach { result ->
                recordTaskResult(
                    result.childId,
                    result.subject,
                    result.difficulty,
                    result.success,
                    result.timeSpent,
                    result.hintsUsed,
                    result.attemptsToSolve,
                    result.metadata
                ).getOrThrow()
            }
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recording task results batch", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPerformanceStats(
        childId: String,
        subject: String
    ): Result<PerformanceStatsDto> = withContext(Dispatchers.IO) {
        try {
            val key = "$childId-$subject"
            val cachedStats = performanceStats[key]
            
            if (cachedStats != null) {
                val updatedCached = cachedStats.copy(lastUpdated = Instant.now().toString())
                Result.success(updatedCached)
            } else {
                Result.failure(Exception("No performance data available"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting performance stats", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRecentTaskResults(
        childId: String,
        subject: String,
        limit: Int
    ): Result<List<TaskResultDto>> = withContext(Dispatchers.IO) {
        try {
            val key = "$childId-$subject"
            val results = taskResults[key]?.takeLast(limit) ?: emptyList()
            Result.success(results)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent task results", e)
            Result.failure(e)
        }
    }
    
    // ============= LEARNING ANALYSIS =============
    
    override suspend fun analyzeLearningPatterns(
        childId: String,
        subject: String
    ): Result<LearningPatternDto> = withContext(Dispatchers.IO) {
        try {
            val results = getRecentTaskResults(childId, subject, 50).getOrNull() ?: emptyList()
            
            if (results.isEmpty()) {
                return@withContext Result.failure(Exception("Insufficient data for pattern analysis"))
            }
            
            val pattern = LearningPatternDto(
                childId = childId,
                subject = subject,
                preferredDifficulty = results.groupBy { it.difficulty }
                    .maxByOrNull { (_, tasks) -> tasks.count { it.success } }?.key ?: 3,
                optimalSessionLength = results.map { it.timeSpent }.average().toInt(),
                bestTimeOfDay = "morning", // Placeholder - would analyze by timestamp
                commonMistakePatterns = extractCommonMistakes(results),
                strengthAreas = extractStrengthAreas(results),
                improvementAreas = extractImprovementAreas(results),
                learningVelocity = calculateLearningVelocity(results),
                lastAnalyzed = Instant.now().toString()
            )
            
            Result.success(pattern)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing learning patterns", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPerformanceTrends(
        childId: String,
        subject: String,
        weeks: Int
    ): Result<List<PerformanceTrendDto>> = withContext(Dispatchers.IO) {
        try {
            val results = getRecentTaskResults(childId, subject, weeks * 20).getOrNull() ?: emptyList()
            
            // Group results by week and calculate trends
            val trends = results.chunked(20).mapIndexed { weekIndex, weekResults ->
                val successCount = weekResults.count { it.success }
                PerformanceTrendDto(
                    childId = childId,
                    subject = subject,
                    timePerformanceWeek = "2024-W${weekIndex + 1}",
                    weeklySuccessRate = if (weekResults.isNotEmpty()) successCount.toFloat() / weekResults.size else 0f,
                    weeklyTasksCompleted = weekResults.size,
                    weeklyAverageDifficulty = if (weekResults.isNotEmpty()) weekResults.map { it.difficulty }.average().toFloat() else 0f,
                    improvementFromPreviousWeek = 0f, // Calculate based on previous week
                    longestStreakThisWeek = calculateBestStreak(weekResults),
                    totalTimeSpentMinutes = weekResults.sumOf { it.timeSpent } / 60,
                    timestamp = Instant.now().toString()
                )
            }
            
            Result.success(trends)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting performance trends", e)
            Result.failure(e)
        }
    }
    
    override suspend fun calculateSubjectMastery(
        childId: String,
        subject: String
    ): Result<SubjectMasteryDto> = withContext(Dispatchers.IO) {
        try {
            val subjectResults = taskResults["$childId-$subject"] ?: emptyList()
            if (subjectResults.isEmpty()) {
                return@withContext Result.failure(Exception("No data for subject $subject"))
            }

            val gradeLevel = (subjectResults.count { it.success } / 5) + 1
            val tasksInCurrentGrade = subjectResults.count { it.success } % 5
            val masteryScore = tasksInCurrentGrade / 5f

            val masteryDto = SubjectMasteryDto(
                childId = childId,
                subject = subject,
                gradeLevel = gradeLevel,
                masteryLevel = when {
                    masteryScore >= 0.8f -> "Advanced"
                    masteryScore >= 0.5f -> "Intermediate"
                    else -> "Beginner"
                },
                masteryScore = masteryScore,
                topicsCompleted = subjectResults.filter { it.success }.mapNotNull { it.metadata["topic"] }.distinct(),
                topicsInProgress = listOf("Next Topic"), // Placeholder
                topicsNotStarted = listOf("Future Topic 1", "Future Topic 2"), // Placeholder
                estimatedTimeToNextLevel = (5 - tasksInCurrentGrade) * 2, // 2 hours per task
                lastUpdated = Instant.now().toString()
            )
            Result.success(masteryDto)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating subject mastery for $subject", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getAllSubjectMastery(
        childId: String
    ): Result<List<SubjectMasteryDto>> = withContext(Dispatchers.IO) {
        try {
            val subjects = taskResults.keys.map { it.substringAfter("-") }.distinct()
            val masteryList = subjects.map { subject ->
                val subjectResults = taskResults["$childId-$subject"] ?: emptyList()
                // Simple logic: grade level increases for every 5 tasks successfully completed
                val gradeLevel = (subjectResults.count { it.success } / 5) + 1
                val tasksInCurrentGrade = subjectResults.count { it.success } % 5
                val masteryScore = tasksInCurrentGrade / 5f

                SubjectMasteryDto(
                    childId = childId,
                    subject = subject,
                    gradeLevel = gradeLevel,
                    masteryScore = masteryScore,
                    masteryLevel = when {
                        masteryScore > 0.75f -> "Advanced"
                        masteryScore > 0.4f -> "Intermediate"
                        else -> "Beginner"
                    },
                    topicsCompleted = subjectResults.filter { it.success }.map { it.metadata["topic"] ?: "Unknown Topic" }.distinct(),
                    topicsInProgress = listOf("Next Topic"),
                    topicsNotStarted = listOf("Future Topic 1", "Future Topic 2"),
                    estimatedTimeToNextLevel = (5 - tasksInCurrentGrade) * 2, // 2 hours per task
                    lastUpdated = Instant.now().toString()
                )
            }
            Result.success(masteryList)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all subject mastery", e)
            Result.failure(e)
        }
    }
    
    // ============= RECOMMENDATIONS =============
    
    override suspend fun getLearningRecommendations(
        childId: String,
        subject: String
    ): Result<List<LearningRecommendationDto>> = withContext(Dispatchers.IO) {
        try {
            val stats = getPerformanceStats(childId, subject).getOrNull()
            val patterns = analyzeLearningPatterns(childId, subject).getOrNull()
            
            val recommendations = mutableListOf<LearningRecommendationDto>()
            
            // Difficulty adjustment recommendations
            if (stats != null && stats.tasksCompleted >= AdaptiveDifficulty.MIN_TASKS_FOR_ANALYSIS) {
                when {
                    AdaptiveDifficulty.shouldIncreaseDifficulty(stats.successRate) -> {
                        recommendations.add(createRecommendation(childId, subject, "difficulty_increase", "Increase Challenge", "Success rate is high - ready for harder tasks"))
                    }
                    AdaptiveDifficulty.shouldDecreaseDifficulty(stats.successRate) -> {
                        recommendations.add(createRecommendation(childId, subject, "difficulty_decrease", "Reduce Challenge", "Success rate is low - need easier tasks"))
                    }
                }
            }
            
            // Session length recommendations
            if (patterns != null && patterns.optimalSessionLength > 0) {
                recommendations.add(createRecommendation(childId, subject, "session_timing", "Optimize Session Length", "Recommended session: ${patterns.optimalSessionLength / 60} minutes"))
            }
            
            Result.success(recommendations)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting learning recommendations", e)
            Result.failure(e)
        }
    }
    
    override suspend fun recommendSessionLength(
        childId: String,
        subject: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val patterns = analyzeLearningPatterns(childId, subject).getOrNull()
            val recommendedLength = patterns?.optimalSessionLength ?: (15 * 60) // Default 15 minutes
            Result.success(recommendedLength)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recommending session length", e)
            Result.failure(e)
        }
    }
    
    override suspend fun recommendOptimalLearningTime(
        childId: String
    ): Result<OptimalLearningTimeDto> = withContext(Dispatchers.IO) {
        try {
            // Placeholder implementation - would analyze historical performance by time
            val recommendation = OptimalLearningTimeDto(
                childId = childId,
                recommendedHour = 10, // 10 AM
                confidenceScore = 0.7f,
                averagePerformanceAtTime = 0.85f,
                sampleSize = 20,
                alternativeHours = listOf(14, 16), // 2 PM, 4 PM
                timestamp = Instant.now().toString()
            )
            
            Result.success(recommendation)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recommending optimal learning time", e)
            Result.failure(e)
        }
    }
    
    // ============= ANALYTICS & REPORTING =============
    
    override suspend fun generatePerformanceReport(
        childId: String,
        startDate: String,
        endDate: String
    ): Result<PerformanceReportDto> = withContext(Dispatchers.IO) {
        try {
            val overallStats = PerformanceStatsDto(
                childId = childId,
                subject = "overall",
                successRate = 0.8f,
                averageDifficulty = 3.0f,
                tasksCompleted = 50,
                tasksSucceeded = 40,
                tasksFailed = 10,
                currentStreak = 5,
                bestStreak = 8,
                lastUpdated = Instant.now().toString()
            )
            
            val report = PerformanceReportDto(
                childId = childId,
                reportPeriod = "$startDate to $endDate",
                overallStats = overallStats,
                subjectBreakdown = getAllSubjectMastery(childId).getOrNull() ?: emptyList(),
                improvementAreas = listOf("Focus on accuracy", "Increase problem-solving speed"),
                strengthAreas = listOf("Strong logical thinking", "Good pattern recognition"),
                keyAchievements = listOf("Mastered basic arithmetic", "Improved reading comprehension"),
                recommendations = emptyList(), // Would aggregate from all subjects
                difficultyProgression = emptyList(), // Would aggregate difficulty changes
                learningVelocity = 0.75f,
                generatedAt = Instant.now().toString()
            )
            
            Result.success(report)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating performance report", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getDifficultyAdjustmentHistory(
        childId: String,
        subject: String,
        limit: Int
    ): Result<List<DifficultyAdjustmentDto>> = withContext(Dispatchers.IO) {
        try {
            val key = "$childId-$subject"
            val adjustments = difficultyAdjustments[key]?.takeLast(limit) ?: emptyList()
            Result.success(adjustments)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting difficulty adjustment history", e)
            Result.failure(e)
        }
    }
    
    override suspend fun calculateLearningVelocity(
        childId: String,
        days: Int
    ): Result<Float> = withContext(Dispatchers.IO) {
        try {
            // Placeholder implementation - would analyze improvement rate over time
            val velocity = 0.75f // Moderate improvement rate
            Result.success(velocity)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating learning velocity", e)
            Result.failure(e)
        }
    }
    
    // ============= PRIVATE HELPER METHODS =============
    
    private suspend fun updatePerformanceStats(childId: String, subject: String) {
        val key = "$childId-$subject"
        val results = getRecentTaskResults(childId, subject, AdaptiveDifficulty.ANALYSIS_WINDOW_SIZE).getOrNull() ?: emptyList()

        if (results.isEmpty()) return

        val successCount = results.count { it.success }
        val failureCount = results.size - successCount
        val currentStreak = calculateCurrentStreak(results)
        val bestStreak = calculateBestStreak(results)

        performanceStats[key] = PerformanceStatsDto(
            childId = childId,
            subject = subject,
            successRate = successCount.toFloat() / results.size,
            averageDifficulty = results.map { it.difficulty }.average().toFloat(),
            tasksCompleted = results.size,
            tasksSucceeded = successCount,
            tasksFailed = failureCount,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            lastUpdated = Instant.now().toString()
        )
    }
    
    private fun recordDifficultyAdjustment(
        childId: String,
        subject: String,
        oldDifficulty: Int,
        newDifficulty: Int,
        successRate: Float,
        tasksAnalyzed: Int
    ) {
        val key = "$childId-$subject"
        val adjustment = DifficultyAdjustmentDto(
            id = UUID.randomUUID().toString(),
            childId = childId,
            subject = subject,
            previousDifficulty = oldDifficulty,
            newDifficulty = newDifficulty,
            reason = generateAdjustmentReason(successRate),
            successRateBefore = successRate,
            tasksAnalyzed = tasksAnalyzed,
            confidence = AdaptiveDifficulty.calculateConfidence(tasksAnalyzed),
            timestamp = Instant.now().toString()
        )
        
        difficultyAdjustments.getOrPut(key) { mutableListOf() }.add(adjustment)
    }
    
    private fun generateReasoningText(successRate: Float, difficulty: Int): String {
        return when {
            successRate > 0.9f -> "Excellent performance - ready for increased challenge"
            successRate > 0.8f -> "Good performance - maintaining current difficulty"
            successRate > 0.7f -> "Adequate performance - minor adjustments needed"
            else -> "Performance below target - reducing difficulty to build confidence"
        }
    }
    
    private fun generateAdjustmentReason(successRate: Float): String {
        return when {
            successRate > AdaptiveDifficulty.TARGET_SUCCESS_RATE + AdaptiveDifficulty.SUCCESS_RATE_TOLERANCE -> 
                "Success rate too high - increasing difficulty"
            successRate < AdaptiveDifficulty.TARGET_SUCCESS_RATE - AdaptiveDifficulty.SUCCESS_RATE_TOLERANCE -> 
                "Success rate too low - decreasing difficulty"
            else -> "Maintaining optimal difficulty"
        }
    }
    
    private fun extractCommonMistakes(results: List<TaskResultDto>): List<String> {
        // Placeholder - would analyze metadata for common error patterns
        return listOf("Calculation errors", "Reading comprehension gaps")
    }
    
    private fun extractStrengthAreas(results: List<TaskResultDto>): List<String> {
        // Placeholder - would analyze successful task patterns
        return listOf("Pattern recognition", "Logical reasoning")
    }
    
    private fun extractImprovementAreas(results: List<TaskResultDto>): List<String> {
        // Placeholder - would analyze failed task patterns
        return listOf("Speed improvement", "Accuracy enhancement")
    }
    
    private fun calculateLearningVelocity(results: List<TaskResultDto>): Float {
        if (results.size < 10) return 0f
        
        val early = results.take(results.size / 2)
        val recent = results.takeLast(results.size / 2)
        
        val earlySuccess = early.count { it.success }.toFloat() / early.size
        val recentSuccess = recent.count { it.success }.toFloat() / recent.size
        
        return (recentSuccess - earlySuccess).coerceIn(-1f, 1f)
    }
    
    private fun calculateMasteryScore(successRate: Float, avgDifficulty: Float, tasksCompleted: Int): Float {
        val baseScore = successRate * 0.6f // 60% from success rate
        val difficultyBonus = (avgDifficulty / AdaptiveDifficulty.MAX_DIFFICULTY) * 0.3f // 30% from difficulty
        val experienceBonus = (tasksCompleted.coerceAtMost(100) / 100f) * 0.1f // 10% from experience
        
        return (baseScore + difficultyBonus + experienceBonus).coerceIn(0f, 1f)
    }
    
    private fun estimateTimeToNextLevel(currentScore: Float): Int {
        val nextThreshold = when {
            currentScore < 0.25f -> 0.25f
            currentScore < 0.60f -> 0.60f
            currentScore < 0.85f -> 0.85f
            else -> 1.0f
        }
        
        val scoreGap = nextThreshold - currentScore
        return ((scoreGap * 100).toInt()).coerceIn(5, 50) // 5-50 hours estimate
    }
    
    private fun calculateCurrentStreak(results: List<TaskResultDto>): Int {
        var streak = 0
        for (result in results.reversed()) {
            if (result.success) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
    
    private fun calculateBestStreak(results: List<TaskResultDto>): Int {
        var bestStreak = 0
        var currentStreak = 0
        
        for (result in results) {
            if (result.success) {
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }
        
        return bestStreak
    }
    
    private fun createRecommendation(
        childId: String,
        subject: String,
        type: String,
        title: String,
        description: String
    ): LearningRecommendationDto {
        return LearningRecommendationDto(
            id = UUID.randomUUID().toString(),
            childId = childId,
            subject = subject,
            type = type,
            title = title,
            description = description,
            actionItems = listOf("Review current performance", "Adjust task difficulty", "Monitor progress"),
            estimatedImpact = "medium",
            confidence = 0.8f,
            timestamp = Instant.now().toString()
        )
    }
}