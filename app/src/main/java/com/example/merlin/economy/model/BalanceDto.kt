package com.example.merlin.economy.model

/**
 * Data transfer object for child wallet balance information.
 * Pure Kotlin data class with no Android dependencies, fully serializable for API transport.
 */
data class BalanceDto(
    val childId: String,
    val balance: Int,           // Current Merlin Coins
    val dailyCap: Int,          // Maximum MC that can be earned today
    val todayEarned: Int,       // MC earned today
    val todaySpent: Int,        // MC spent today
    val ageGroup: String = "",  // For context (3-5, 6-8, 9-12)
    val lastUpdated: String     // ISO 8601 timestamp
)

/**
 * Daily caps by age group (in Merlin Coins = seconds)
 */
object DailyCaps {
    const val AGE_3_TO_5 = 1800    // 30 minutes
    const val AGE_6_TO_8 = 2700    // 45 minutes  
    const val AGE_9_TO_12 = 3600   // 60 minutes
    
    /**
     * Get daily cap based on age
     */
    fun getCapForAge(age: Int): Int = when {
        age <= 5 -> AGE_3_TO_5
        age <= 8 -> AGE_6_TO_8
        else -> AGE_9_TO_12
    }
    
    /**
     * Get age group string for display
     */
    fun getAgeGroupString(age: Int): String = when {
        age <= 5 -> "3-5"
        age <= 8 -> "6-8"
        else -> "9-12"
    }
    
    /**
     * Convert MC to time display string
     */
    fun formatMCAsTime(mc: Int): String {
        val minutes = mc / 60
        val seconds = mc % 60
        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }
    
    /**
     * Convert MC to simplified time display (e.g., "15m")
     */
    fun formatMCAsSimpleTime(mc: Int): String {
        val minutes = mc / 60
        return if (minutes > 0) {
            "${minutes}m"
        } else {
            "${mc}s"
        }
    }
}

/**
 * Balance change information for notifications
 */
data class BalanceChangeDto(
    val childId: String,
    val previousBalance: Int,
    val newBalance: Int,
    val changeAmount: Int,      // Positive for earnings, negative for spending
    val reason: String,         // Description of the change
    val category: String,       // Transaction category
    val timestamp: String       // ISO 8601 timestamp
) {
    val isEarning: Boolean get() = changeAmount > 0
    val isSpending: Boolean get() = changeAmount < 0
} 