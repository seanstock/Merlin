package com.example.merlin.economy.model

/**
 * Data transfer object for badge achievements.
 * Pure Kotlin data class with no Android dependencies, fully serializable for API transport.
 */
data class BadgeDto(
    val id: String,
    val childId: String,
    val name: String,
    val description: String,
    val earnedAt: String,       // ISO 8601 string
    val category: String,
    val imageUrl: String,
    val rarity: String,         // common, uncommon, rare, epic, legendary
    val benefits: Map<String, String> = emptyMap()  // Permanent benefits this badge provides
)

/**
 * Badge categories for organization
 */
object BadgeCategory {
    const val TASK_COMPLETION = "task_completion"
    const val STREAK = "streak"
    const val SUBJECT_MASTERY = "subject_mastery"
    const val TIME_MANAGEMENT = "time_management"
    const val EXPLORATION = "exploration"
    const val SOCIAL = "social"
    const val MILESTONE = "milestone"
    const val SPECIAL_EVENT = "special_event"
    
    val ALL_CATEGORIES = setOf(
        TASK_COMPLETION,
        STREAK,
        SUBJECT_MASTERY,
        TIME_MANAGEMENT,
        EXPLORATION,
        SOCIAL,
        MILESTONE,
        SPECIAL_EVENT
    )
}

/**
 * Badge rarity levels
 */
object BadgeRarity {
    const val COMMON = "common"
    const val UNCOMMON = "uncommon"
    const val RARE = "rare"
    const val EPIC = "epic"
    const val LEGENDARY = "legendary"
    
    val RARITY_ORDER = listOf(COMMON, UNCOMMON, RARE, EPIC, LEGENDARY)
    
    /**
     * Get rarity weight for sorting (higher = more rare)
     */
    fun getRarityWeight(rarity: String): Int = RARITY_ORDER.indexOf(rarity)
    
    /**
     * Get rarity color for UI display
     */
    fun getRarityColor(rarity: String): String = when (rarity) {
        COMMON -> "#9E9E9E"      // Gray
        UNCOMMON -> "#4CAF50"    // Green
        RARE -> "#2196F3"        // Blue
        EPIC -> "#9C27B0"        // Purple
        LEGENDARY -> "#FF9800"   // Orange
        else -> "#9E9E9E"
    }
}

/**
 * Badge progress tracking for badges that have multiple steps
 */
data class BadgeProgressDto(
    val badgeId: String,
    val childId: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val lastUpdated: String     // ISO 8601 timestamp
) {
    val isCompleted: Boolean get() = currentProgress >= targetProgress
    val progressPercentage: Float get() = (currentProgress.toFloat() / targetProgress) * 100f
}

/**
 * Comprehensive badge statistics for a child
 */
data class BadgeStatsDto(
    val childId: String,
    val totalBadgesEarned: Int,
    val badgesByCategory: Map<String, Int>,     // category -> count
    val badgesByRarity: Map<String, Int>,       // rarity -> count
    val mostRecentBadge: BadgeDto?,
    val rareestBadge: BadgeDto?,
    val lastUpdated: String                     // ISO 8601 timestamp
) 