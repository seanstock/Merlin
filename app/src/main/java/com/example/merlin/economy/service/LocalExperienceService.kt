package com.example.merlin.economy.service

import com.example.merlin.economy.model.*
import java.time.Instant
import java.util.UUID

class LocalExperienceService : ExperienceService {

    private var experienceData = mutableMapOf<String, ExperienceDto>()
    private var transactions = mutableMapOf<String, MutableList<XpTransactionDto>>()

    init {
        // Seed with some initial data for demo purposes
        val demoChildId = "demo_child"
        getDemoExperience(demoChildId)
        transactions.getOrPut(demoChildId) { mutableListOf() }.add(
            XpTransactionDto(
                id = UUID.randomUUID().toString(),
                childId = demoChildId,
                amount = 50,
                source = XpSource.TASK_COMPLETION,
                description = "Completed a math quiz",
                timestamp = Instant.now().minusSeconds(3600).toString(),
                metadata = mapOf("subject" to "math")
            )
        )
    }

    private fun getDemoExperience(childId: String): ExperienceDto {
        return experienceData.getOrPut(childId) {
            ExperienceDto(
                childId = childId,
                level = 5,
                currentXp = 320,
                nextLevelXp = LevelProgression.getXpForNextLevel(5),
                totalXpEarned = 1820
            )
        }
    }

    override suspend fun awardXp(childId: String, amount: Int, source: String, description: String, metadata: Map<String, String>): Result<ExperienceDto> {
        val current = getDemoExperience(childId)
        val newTotalXp = current.totalXpEarned + amount
        val newLevel = LevelProgression.getLevelFromTotalXp(newTotalXp)
        val xpForCurrentLevel = LevelProgression.getXpRequiredForLevel(newLevel)
        val xpForNextLevel = LevelProgression.getXpForNextLevel(newLevel)

        val updated = current.copy(
            totalXpEarned = newTotalXp,
            level = newLevel,
            currentXp = newTotalXp - xpForCurrentLevel,
            nextLevelXp = xpForNextLevel
        )
        experienceData[childId] = updated

        val transaction = XpTransactionDto(
            id = UUID.randomUUID().toString(),
            childId = childId,
            amount = amount,
            source = source,
            description = description,
            timestamp = Instant.now().toString(),
            metadata = metadata
        )
        transactions.getOrPut(childId) { mutableListOf() }.add(transaction)

        return Result.success(updated)
    }

    override suspend fun getExperience(childId: String): Result<ExperienceDto> {
        return Result.success(getDemoExperience(childId))
    }

    override suspend fun calculateXpForActivity(activityType: String, difficulty: Int, performance: Float, bonusMultipliers: Map<String, Float>): Result<Int> {
        val base = XpSource.BASE_XP_AMOUNTS[activityType] ?: 10
        var total = (base * difficulty * performance).toInt()
        bonusMultipliers.forEach { (_, multiplier) ->
            total = (total * multiplier).toInt()
        }
        return Result.success(total)
    }

    override suspend fun getXpHistory(childId: String, limit: Int, offset: Int): Result<List<XpTransactionDto>> {
        val history = transactions[childId] ?: emptyList()
        return Result.success(history.sortedByDescending { it.timestamp }.drop(offset).take(limit))
    }

    override suspend fun checkAndProcessLevelUp(childId: String): Result<LevelUpDto?> {
        // This should be called after awarding XP, but for demo we can simplify
        return Result.success(null)
    }

    override suspend fun getLevelRequirements(level: Int): Result<Int> {
        return Result.success(LevelProgression.getXpRequiredForLevel(level))
    }

    override suspend fun calculateLevelFromXp(totalXp: Int): Result<Int> {
        return Result.success(LevelProgression.getLevelFromTotalXp(totalXp))
    }

    override suspend fun getLevelRewards(level: Int): Result<List<String>> {
        return Result.success(listOf("New avatar background", "50 Merlin Coins"))
    }

    override suspend fun hasUnlockedFeature(childId: String, featureId: String): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun getXpStats(childId: String): Result<XpStatsDto> {
        val exp = getDemoExperience(childId)
        val childTransactions = transactions[childId] ?: emptyList()
        return Result.success(XpStatsDto(
            childId = childId,
            totalXpEarned = exp.totalXpEarned,
            currentLevel = exp.level,
            xpThisWeek = childTransactions.map { it.amount }.sum(), // Simplified for demo
            xpThisMonth = childTransactions.map { it.amount }.sum(), // Simplified for demo
            averageXpPerDay = if (childTransactions.isNotEmpty()) childTransactions.map { it.amount }.sum().toFloat() / 7f else 0f,
            xpBySource = childTransactions.groupingBy { it.source }.eachCount().mapValues { it.value * 10 }, // Approximation
            levelUpsThisMonth = 0, // Simplified
            lastUpdated = Instant.now().toString()
        ))
    }

    override suspend fun getXpBreakdownBySource(childId: String, days: Int): Result<Map<String, Int>> {
        val childTransactions = transactions[childId] ?: emptyList()
        return Result.success(childTransactions.groupingBy { it.source }.fold(0) { acc, transaction -> acc + transaction.amount })
    }

    override suspend fun getLevelProgressionHistory(childId: String): Result<List<LevelUpDto>> {
        return Result.success(emptyList()) // Not implemented for demo
    }

    override suspend fun calculateXpEarningRate(childId: String, days: Int): Result<Float> {
        return Result.success(25.5f)
    }

    override suspend fun getAgeGroupRank(childId: String, ageGroup: String): Result<LeaderboardPositionDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAgeGroupLeaderboard(ageGroup: String, limit: Int): Result<List<LeaderboardEntryDto>> {
        TODO("Not yet implemented")
    }

    override suspend fun getPercentileRanking(childId: String): Result<Float> {
        TODO("Not yet implemented")
    }

    override suspend fun projectNextLevelDate(childId: String): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun recommendXpActivities(childId: String): Result<List<XpActivityRecommendationDto>> {
        TODO("Not yet implemented")
    }

    override suspend fun calculateXpStrategy(childId: String, targetLevel: Int, targetDate: String): Result<XpStrategyDto> {
        TODO("Not yet implemented")
    }

    override suspend fun awardXpBatch(awards: List<XpAwardDto>): Result<List<ExperienceDto>> {
        TODO("Not yet implemented")
    }

    override suspend fun processXpTransactions(transactions: List<XpTransactionDto>): Result<Unit> {
        TODO("Not yet implemented")
    }
} 