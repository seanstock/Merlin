package com.example.merlin.economy.service

import com.example.merlin.economy.model.*

/**
 * Core economy service interface for managing Merlin Coins, transactions, and wallet operations.
 * Pure business logic contracts with no Android dependencies - ready for local or remote implementation.
 */
interface EconomyService {
    
    // ============= WALLET MANAGEMENT =============
    
    /**
     * Get current balance information for a child
     */
    suspend fun getBalance(childId: String): Result<BalanceDto>
    
    /**
     * Get daily earning cap based on child's age
     */
    suspend fun getDailyCapByAge(age: Int): Result<Int>
    
    /**
     * Check if child can earn more coins today (hasn't hit daily cap)
     */
    suspend fun canEarnMore(childId: String): Result<Boolean>
    
    /**
     * Get remaining coins that can be earned today
     */
    suspend fun getRemainingDailyEarnings(childId: String): Result<Int>
    
    // ============= TRANSACTION MANAGEMENT =============
    
    /**
     * Record a transaction (earning or spending)
     */
    suspend fun recordTransaction(transaction: TransactionDto): Result<TransactionDto>
    
    /**
     * Award Merlin Coins to a child's wallet
     */
    suspend fun awardCoins(
        childId: String,
        amount: Int,
        category: String,
        description: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<BalanceChangeDto>
    
    /**
     * Spend Merlin Coins from a child's wallet
     */
    suspend fun spendCoins(
        childId: String,
        amount: Int,
        category: String,
        description: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<BalanceChangeDto>
    
    /**
     * Get transaction history for a child
     */
    suspend fun getTransactionHistory(
        childId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<TransactionDto>>
    
    /**
     * Get transaction summary for a specific period
     */
    suspend fun getTransactionSummary(
        childId: String,
        startDate: String,  // ISO 8601
        endDate: String     // ISO 8601
    ): Result<TransactionSummaryDto>
    
    // ============= REWARD CALCULATION =============
    
    /**
     * Calculate reward amount based on task performance
     */
    suspend fun calculateReward(
        difficulty: Int,
        isFirstTry: Boolean,
        isPerfect: Boolean,
        isNewConcept: Boolean,
        isTeachingMode: Boolean
    ): Result<Int>
    
    /**
     * Calculate screen time cost with category discounts
     */
    suspend fun calculateScreenTimeCost(
        timeInSeconds: Int,
        category: String
    ): Result<Int>
    
    /**
     * Calculate real-world value conversion (MC to cents)
     */
    suspend fun convertToRealWorldValue(coins: Int): Result<Float>
    
    // ============= SPENDING VALIDATION =============
    
    /**
     * Check if child has sufficient coins for a purchase
     */
    suspend fun hasSufficientCoins(childId: String, amount: Int): Result<Boolean>
    
    /**
     * Validate spending request (sufficient funds, category limits, etc.)
     */
    suspend fun validateSpendingRequest(
        childId: String,
        amount: Int,
        category: String
    ): Result<SpendingValidationDto>
    
    // ============= ANALYTICS & REPORTING =============
    
    /**
     * Get earning patterns for analytics
     */
    suspend fun getEarningPatterns(
        childId: String,
        days: Int = 30
    ): Result<List<EarningPatternDto>>
    
    /**
     * Get spending patterns for analytics  
     */
    suspend fun getSpendingPatterns(
        childId: String,
        days: Int = 30
    ): Result<List<SpendingPatternDto>>
    
    /**
     * Get economy dashboard data for parents
     */
    suspend fun getEconomyDashboard(childId: String): Result<EconomyDashboardDto>
}

/**
 * Spending validation result
 */
data class SpendingValidationDto(
    val isValid: Boolean,
    val errorMessage: String = "",
    val currentBalance: Int,
    val requestedAmount: Int,
    val afterSpendingBalance: Int
)

/**
 * Earning patterns for analytics
 */
data class EarningPatternDto(
    val date: String,           // ISO 8601 date
    val totalEarned: Int,
    val earnedByCategory: Map<String, Int>,
    val taskCount: Int,
    val averageTaskReward: Float
)

/**
 * Spending patterns for analytics
 */
data class SpendingPatternDto(
    val date: String,           // ISO 8601 date
    val totalSpent: Int,
    val spentByCategory: Map<String, Int>,
    val averageTimePerSession: Float,
    val mostUsedCategory: String
)

/**
 * Economy dashboard data for parents
 */
data class EconomyDashboardDto(
    val balance: BalanceDto,
    val recentTransactions: List<TransactionDto>,
    val weeklyEarnings: Int,
    val weeklySpending: Int,
    val topEarningCategories: Map<String, Int>,
    val topSpendingCategories: Map<String, Int>,
    val streakDays: Int,
    val totalRealWorldValue: Float  // In cents
) 