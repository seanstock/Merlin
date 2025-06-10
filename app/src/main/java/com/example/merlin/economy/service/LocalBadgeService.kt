package com.example.merlin.economy.service

import com.example.merlin.economy.model.*
import java.time.Instant
import java.util.UUID

class LocalBadgeService : BadgeService {

    private val demoBadgeDefinitions: List<BadgeDefinitionDto>
    private val demoBadges: MutableList<BadgeDto>

    init {
        demoBadgeDefinitions = createDemoBadgeDefinitions()
        demoBadges = createDemoEarnedBadges()
    }

    private fun createDemoBadgeDefinitions(): List<BadgeDefinitionDto> {
        return listOf(
            BadgeDefinitionDto(
                id = "math_mastery_level_1",
                name = "Math Whiz",
                description = "Complete 10 math challenges.",
                imageUrl = "https://example.com/badge_math.png",
                category = BadgeCategory.SUBJECT_MASTERY,
                rarity = BadgeRarity.COMMON,
                requirements = mapOf("subject" to "math", "count" to "10"),
                rewards = mapOf("xp" to "100", "coins" to "50")
            ),
            BadgeDefinitionDto(
                id = "reading_adventure_level_1",
                name = "Bookworm",
                description = "Read 5 stories.",
                imageUrl = "https://example.com/badge_reading.png",
                category = BadgeCategory.EXPLORATION,
                rarity = BadgeRarity.COMMON,
                requirements = mapOf("activity" to "reading", "count" to "5"),
                rewards = mapOf("xp" to "100", "coins" to "50")
            ),
            BadgeDefinitionDto(
                id = "streak_starter",
                name = "Streak Starter",
                description = "Complete 3 tasks in a row.",
                imageUrl = "https://example.com/badge_streak.png",
                category = BadgeCategory.STREAK,
                rarity = BadgeRarity.UNCOMMON,
                requirements = mapOf("streak" to "3"),
                rewards = mapOf("xp" to "150")
            )
        )
    }

    private fun createDemoEarnedBadges(): MutableList<BadgeDto> {
        return mutableListOf(
            BadgeDto(
                id = "earned-math-master-1",
                childId = "demo_child",
                name = "Math Whiz",
                description = "Complete 10 math challenges.",
                earnedAt = "2023-10-26T10:00:00Z",
                category = BadgeCategory.SUBJECT_MASTERY,
                imageUrl = "https://example.com/badge_math.png",
                rarity = BadgeRarity.COMMON
            )
        )
    }

    override suspend fun awardBadge(badge: BadgeDto): Result<BadgeDto> {
        demoBadges.add(badge)
        return Result.success(badge)
    }

    override suspend fun awardBadgeById(childId: String, badgeDefinitionId: String, earnedAt: String): Result<BadgeDto> {
        val definition = demoBadgeDefinitions.find { it.id == badgeDefinitionId }
        return if (definition != null) {
            val newBadge = BadgeDto(
                id = "earned-${UUID.randomUUID()}",
                childId = childId,
                name = definition.name,
                description = definition.description,
                earnedAt = Instant.now().toString(),
                category = definition.category,
                imageUrl = definition.imageUrl,
                rarity = definition.rarity,
                benefits = definition.rewards
            )
            demoBadges.add(newBadge)
            Result.success(newBadge)
        } else {
            Result.error("Badge definition not found")
        }
    }

    override suspend fun getBadges(childId: String): Result<List<BadgeDto>> {
        return Result.success(demoBadges.filter { it.childId == childId })
    }

    override suspend fun getBadgesByCategory(childId: String, category: String): Result<List<BadgeDto>> {
        return Result.success(demoBadges.filter { it.childId == childId && it.category == category })
    }

    override suspend fun hasBadge(childId: String, badgeId: String): Result<Boolean> {
        return Result.success(demoBadges.any { it.childId == childId && it.id == badgeId })
    }

    override suspend fun getBadgeStats(childId: String): Result<BadgeStatsDto> {
        val earnedBadges = demoBadges.filter { it.childId == childId }
        val earnedCount = earnedBadges.size
        return Result.success(BadgeStatsDto(
            childId = childId,
            totalBadgesEarned = earnedCount,
            badgesByCategory = earnedBadges.groupingBy { it.category }.eachCount(),
            badgesByRarity = earnedBadges.groupingBy { it.rarity }.eachCount(),
            mostRecentBadge = earnedBadges.maxByOrNull { it.earnedAt },
            rareestBadge = earnedBadges.minByOrNull { BadgeRarity.getRarityWeight(it.rarity) },
            lastUpdated = Instant.now().toString()
        ))
    }

    override suspend fun getAllBadgeDefinitions(): Result<List<BadgeDefinitionDto>> {
        return Result.success(demoBadgeDefinitions)
    }

    override suspend fun getBadgeDefinitionsByCategory(category: String): Result<List<BadgeDefinitionDto>> {
        return Result.success(demoBadgeDefinitions.filter { it.category == category })
    }

    override suspend fun getBadgeDefinition(badgeId: String): Result<BadgeDefinitionDto> {
        val definition = demoBadgeDefinitions.find { it.id == badgeId }
        return if (definition != null) Result.success(definition) else Result.error("Not found")
    }

    override suspend fun checkBadgeRequirements(childId: String, badgeDefinitionId: String): Result<BadgeRequirementCheckDto> {
        return Result.success(
            BadgeRequirementCheckDto(
                badgeDefinitionId = badgeDefinitionId,
                childId = childId,
                isEligible = true,
                requirementsMet = mapOf("requirement1" to true),
                missingRequirements = emptyList(),
                progressPercentage = 1.0f,
                estimatedTimeToEarn = "0 days",
                checkedAt = Instant.now().toString()
            )
        )
    }

    override suspend fun getBadgeProgress(childId: String, badgeDefinitionId: String): Result<BadgeProgressDto> {
        return Result.success(
            BadgeProgressDto(
                badgeId = badgeDefinitionId,
                childId = childId,
                currentProgress = 5,
                targetProgress = 10,
                lastUpdated = Instant.now().toString()
            )
        )
    }

    override suspend fun updateBadgeProgress(childId: String, badgeDefinitionId: String, progressIncrement: Int): Result<BadgeProgressDto> {
        return getBadgeProgress(childId, badgeDefinitionId)
    }

    override suspend fun getAllBadgeProgress(childId: String): Result<List<BadgeProgressDto>> {
        return Result.success(emptyList())
    }

    override suspend fun checkForNewBadges(childId: String): Result<List<BadgeDto>> {
       return Result.success(emptyList())
    }

    override suspend fun checkBadgeAfterActivity(childId: String, activityType: String, activityData: Map<String, String>): Result<List<BadgeDto>> {
        return Result.success(emptyList())
    }

    override suspend fun evaluateAllBadges(childId: String): Result<BadgeEvaluationResultDto> {
        return Result.success(
            BadgeEvaluationResultDto(
                childId = childId,
                newlyEarnedBadges = emptyList(),
                progressUpdates = emptyList(),
                nearCompletionBadges = emptyList(),
                evaluatedAt = Instant.now().toString()
            )
        )
    }

    override suspend fun getBadgeEarningTrends(childId: String, days: Int): Result<List<BadgeEarningTrendDto>> {
        return Result.success(emptyList())
    }

    override suspend fun getPopularBadges(limit: Int): Result<List<PopularBadgeDto>> {
        return Result.success(emptyList())
    }

    override suspend fun getBadgeRecommendations(childId: String): Result<List<BadgeRecommendationDto>> {
        return Result.success(emptyList())
    }
} 