package com.example.merlin.economy.model

/**
 * Data transfer object for economy transactions.
 * Pure Kotlin data class with no Android dependencies, fully serializable for API transport.
 */
data class TransactionDto(
    val id: String = "",  // UUID string, not auto-generated Long
    val childId: String,
    val amount: Int,
    val category: String,
    val description: String,
    val timestamp: String,  // ISO 8601 string
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Transaction categories for spending/earning classification
 */
object TransactionCategory {
    const val EARNING_TASK_COMPLETION = "earning_task_completion"
    const val EARNING_FIRST_TRY_BONUS = "earning_first_try_bonus"
    const val EARNING_PERFECT_BONUS = "earning_perfect_bonus"
    const val EARNING_NEW_CONCEPT_BONUS = "earning_new_concept_bonus"
    const val EARNING_TEACHING_MODE_BONUS = "earning_teaching_mode_bonus"
    const val EARNING_DAILY_LOGIN = "earning_daily_login"
    const val EARNING_STREAK_BONUS = "earning_streak_bonus"
    
    const val SPENDING_ENTERTAINMENT = "spending_entertainment"
    const val SPENDING_EDUCATIONAL_GAMES = "spending_educational_games"
    const val SPENDING_CREATIVE_APPS = "spending_creative_apps"
    const val SPENDING_PHYSICAL_ACTIVITY = "spending_physical_activity"
    const val SPENDING_CUSTOMIZATION = "spending_customization"
    const val SPENDING_LEARNING_ENHANCEMENT = "spending_learning_enhancement"
    const val SPENDING_APP_ACCESS = "spending_app_access"  // NEW: For purchasing app access
    
    val EARNING_CATEGORIES = setOf(
        EARNING_TASK_COMPLETION,
        EARNING_FIRST_TRY_BONUS,
        EARNING_PERFECT_BONUS,
        EARNING_NEW_CONCEPT_BONUS,
        EARNING_TEACHING_MODE_BONUS,
        EARNING_DAILY_LOGIN,
        EARNING_STREAK_BONUS
    )
    
    val SPENDING_CATEGORIES = setOf(
        SPENDING_ENTERTAINMENT,
        SPENDING_EDUCATIONAL_GAMES,
        SPENDING_CREATIVE_APPS,
        SPENDING_PHYSICAL_ACTIVITY,
        SPENDING_CUSTOMIZATION,
        SPENDING_LEARNING_ENHANCEMENT,
        SPENDING_APP_ACCESS  // Add to spending categories
    )
    
    /**
     * Category-specific discount rates for spending
     * 1.0 = no discount, 0.5 = 50% discount (1 MC = 2 seconds)
     */
    val CATEGORY_DISCOUNTS = mapOf(
        SPENDING_ENTERTAINMENT to 1.0f,      // 1:1 ratio
        SPENDING_EDUCATIONAL_GAMES to 0.8f,  // 0.8:1 discount
        SPENDING_CREATIVE_APPS to 0.7f,      // 0.7:1 discount
        SPENDING_PHYSICAL_ACTIVITY to 0.5f,  // 0.5:1 discount
        SPENDING_CUSTOMIZATION to 1.0f,      // 1:1 ratio
        SPENDING_LEARNING_ENHANCEMENT to 1.0f, // 1:1 ratio, unlimited
        SPENDING_APP_ACCESS to 0.9f          // 0.9:1 slight discount for app access
    )
}

/**
 * Summary of transactions for reporting
 */
data class TransactionSummaryDto(
    val childId: String,
    val totalEarned: Int,
    val totalSpent: Int,
    val currentBalance: Int,
    val todayEarned: Int,
    val todaySpent: Int,
    val earningsBreakdown: Map<String, Int>,  // category -> amount
    val spendingBreakdown: Map<String, Int>,  // category -> amount
    val period: String  // ISO 8601 date range
) 