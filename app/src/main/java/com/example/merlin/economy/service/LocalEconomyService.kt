package com.example.merlin.economy.service

import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.database.entities.EconomyState
import com.example.merlin.economy.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.lang.Math

/**
 * SIMPLE Local implementation of EconomyService.
 * KISS approach - just get the core wallet working with existing EconomyState.
 */
class LocalEconomyService(
    private val economyStateRepository: EconomyStateRepository,
    private val childProfileRepository: ChildProfileRepository
) : EconomyService {

    companion object {
        // Daily caps by age group (in seconds/MC)
        private const val DAILY_CAP_3_5_YEARS = 1800   // 30 minutes
        private const val DAILY_CAP_6_8_YEARS = 2700   // 45 minutes  
        private const val DAILY_CAP_9_12_YEARS = 3600  // 60 minutes
        
        // Category discount rates (spending multipliers)
        private const val ENTERTAINMENT_RATE = 1.0f     // 1:1
        private const val EDUCATIONAL_GAMES_RATE = 0.6f // 20% discount
        private const val CREATIVE_APPS_RATE = 0.9f     // 30% discount
        private const val PHYSICAL_ACTIVITY_RATE = 0.75f // 50% discount
        
        // Reward multipliers
        private const val PERFECT_COMPLETION_MULTIPLIER = 1.5f
        private const val FIRST_TRY_BONUS = 15
        private const val NEW_CONCEPT_MULTIPLIER = 2.0f
        private const val TEACHING_MODE_BONUS = 30
        
        // Real-world conversion rate
        private const val MC_TO_CENTS_RATIO = 25f       // 25 MC = 1 cent
    }

    // ============= WALLET MANAGEMENT =============

    override suspend fun getBalance(childId: String): Result<BalanceDto> = withContext(Dispatchers.IO) {
        try {
            val economyState = economyStateRepository.getByChildId(childId)
            val childProfile = childProfileRepository.getById(childId)
            
            val age = childProfile?.age ?: 8 // Default to middle age group
            val dailyCap = getDailyCapByAge(age).getOrThrow()
            
            val balance = economyState?.walletSeconds ?: 0
            
            Result.success(BalanceDto(
                childId = childId,
                balance = balance,
                dailyCap = dailyCap,
                todayEarned = 0, // TODO: implement daily tracking later
                todaySpent = 0,  // TODO: implement daily tracking later
                ageGroup = getAgeGroup(age),
                lastUpdated = Instant.now().toString()
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getDailyCapByAge(age: Int): Result<Int> {
        val cap = when {
            age <= 5 -> DAILY_CAP_3_5_YEARS
            age <= 8 -> DAILY_CAP_6_8_YEARS
            else -> DAILY_CAP_9_12_YEARS
        }
        return Result.success(cap)
    }

    override suspend fun canEarnMore(childId: String): Result<Boolean> {
        // TODO: Implement daily tracking later
        return Result.success(true)
    }

    override suspend fun getRemainingDailyEarnings(childId: String): Result<Int> {
        // TODO: Implement daily tracking later  
        val dailyCap = getDailyCapByAge(8).getOrThrow() // Default
        return Result.success(dailyCap)
    }

    // ============= TRANSACTION MANAGEMENT =============

    override suspend fun recordTransaction(transaction: TransactionDto): Result<TransactionDto> {
        // Simple implementation - just use existing awardCoins/spendCoins
        return if (transaction.amount > 0) {
            awardCoins(transaction.childId, transaction.amount, transaction.category, transaction.description, transaction.metadata)
            Result.success(transaction)
        } else {
            spendCoins(transaction.childId, -transaction.amount, transaction.category, transaction.description, transaction.metadata)
            Result.success(transaction)
        }
    }

    override suspend fun awardCoins(
        childId: String,
        amount: Int,
        category: String,
        description: String,
        metadata: Map<String, String>
    ): Result<BalanceChangeDto> = withContext(Dispatchers.IO) {
        try {
            // Get current state
            val existingState = economyStateRepository.getByChildId(childId)
            val currentBalance = existingState?.walletSeconds ?: 0
            val newBalance = currentBalance + amount

            // Update or create economy state
            if (existingState != null) {
                val updatedState = existingState.copy(
                    walletSeconds = newBalance,
                    lastEarnedTs = System.currentTimeMillis()
                )
                economyStateRepository.update(updatedState)
            } else {
                val newState = EconomyState(
                    childId = childId,
                    streak = null,
                    fatigueScore = null,
                    walletSeconds = newBalance,
                    badgesJson = null,
                    xpLevel = null,
                    lastEarnedTs = System.currentTimeMillis()
                )
                economyStateRepository.insert(newState)
            }

            Result.success(BalanceChangeDto(
                childId = childId,
                previousBalance = currentBalance,
                newBalance = newBalance,
                changeAmount = amount,
                reason = description,
                category = category,
                timestamp = Instant.now().toString()
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun spendCoins(
        childId: String,
        amount: Int,
        category: String,
        description: String,
        metadata: Map<String, String>
    ): Result<BalanceChangeDto> = withContext(Dispatchers.IO) {
        try {
            val existingState = economyStateRepository.getByChildId(childId)
            val currentBalance = existingState?.walletSeconds ?: 0
            
            if (currentBalance < amount) {
                return@withContext Result.error("Insufficient funds")
            }
            
            val newBalance = currentBalance - amount

            val updatedState = existingState!!.copy(
                walletSeconds = newBalance
            )
            economyStateRepository.update(updatedState)

            Result.success(BalanceChangeDto(
                childId = childId,
                previousBalance = currentBalance,
                newBalance = newBalance,
                changeAmount = -amount,
                reason = description,
                category = category,
                timestamp = Instant.now().toString()
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getTransactionHistory(childId: String, limit: Int, offset: Int): Result<List<TransactionDto>> {
        // TODO: Implement when we add Transaction entity later
        return Result.success(emptyList())
    }

    override suspend fun getTransactionSummary(childId: String, startDate: String, endDate: String): Result<TransactionSummaryDto> {
        // Simple empty summary for now
        return Result.success(TransactionSummaryDto(
            childId = childId,
            totalEarned = 0,
            totalSpent = 0,
            currentBalance = 0,
            todayEarned = 0,
            todaySpent = 0,
            earningsBreakdown = emptyMap(),
            spendingBreakdown = emptyMap(),
            period = "$startDate/$endDate"
        ))
    }

    // ============= REWARD CALCULATION =============

    override suspend fun calculateReward(
        difficulty: Int,
        isFirstTry: Boolean,
        isPerfect: Boolean,
        isNewConcept: Boolean,
        isTeachingMode: Boolean
    ): Result<Int> {
        try {
            var baseReward = when(difficulty) {
                1 -> 30
                2 -> 40
                3 -> 50
                else -> 60
            }
            
            // Apply multipliers
            if (isPerfect) baseReward = (baseReward * PERFECT_COMPLETION_MULTIPLIER).toInt()
            if (isNewConcept) baseReward = (baseReward * NEW_CONCEPT_MULTIPLIER).toInt()
            
            // Apply bonuses
            var totalReward = baseReward
            if (isFirstTry) totalReward += FIRST_TRY_BONUS
            if (isTeachingMode) totalReward += TEACHING_MODE_BONUS
            
            return Result.success(totalReward)
        } catch (e: Exception) {
            return Result.error(e)
        }
    }

    override suspend fun calculateScreenTimeCost(timeInSeconds: Int, category: String): Result<Int> {
        val rate = when(category.lowercase()) {
            "entertainment" -> ENTERTAINMENT_RATE
            "educational" -> EDUCATIONAL_GAMES_RATE
            "creative" -> CREATIVE_APPS_RATE
            "physical" -> PHYSICAL_ACTIVITY_RATE
            else -> ENTERTAINMENT_RATE
        }
        val cost = (timeInSeconds * rate).toInt()
        return Result.success(cost)
    }

    override suspend fun convertToRealWorldValue(coins: Int): Result<Float> {
        return Result.success(coins / MC_TO_CENTS_RATIO)
    }

    // ============= SPENDING VALIDATION =============

    override suspend fun hasSufficientCoins(childId: String, amount: Int): Result<Boolean> {
        return try {
            val balance = getBalance(childId).getOrThrow()
            Result.success(balance.balance >= amount)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun validateSpendingRequest(childId: String, amount: Int, category: String): Result<SpendingValidationDto> {
        return try {
            val balance = getBalance(childId).getOrThrow()
            val isValid = balance.balance >= amount
            
            Result.success(SpendingValidationDto(
                isValid = isValid,
                errorMessage = if (!isValid) "Insufficient funds" else "",
                currentBalance = balance.balance,
                requestedAmount = amount,
                afterSpendingBalance = if (isValid) balance.balance - amount else balance.balance
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= ANALYTICS & REPORTING =============

    override suspend fun getEarningPatterns(childId: String, days: Int): Result<List<EarningPatternDto>> {
        // TODO: Implement analytics
        return Result.success(emptyList())
    }

    override suspend fun getSpendingPatterns(childId: String, days: Int): Result<List<SpendingPatternDto>> {
        // TODO: Implement analytics
        return Result.success(emptyList())
    }

    override suspend fun getEconomyDashboard(childId: String): Result<EconomyDashboardDto> {
        // Simple dashboard
        return try {
            val balance = getBalance(childId).getOrThrow()
            Result.success(EconomyDashboardDto(
                balance = balance,
                recentTransactions = emptyList(),
                weeklyEarnings = 0,
                weeklySpending = 0,
                topEarningCategories = emptyMap(),
                topSpendingCategories = emptyMap(),
                streakDays = 0,
                totalRealWorldValue = convertToRealWorldValue(balance.balance).getOrThrow()
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= DAILY TRACKING METHODS =============

    /**
     * Get today's game earnings for a child
     */
    suspend fun getTodayGameEarnings(childId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // For now, return 0 until we implement proper daily tracking
            // TODO: Implement by tracking daily coin awards in a separate table
            Result.success(0)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    /**
     * Get remaining coins that can be earned from games today
     */
    suspend fun getRemainingGameEarnings(childId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Simple implementation - return daily cap minus today's earnings
            val todayEarned = getTodayGameEarnings(childId).getOrNull() ?: 0
            val remainingCoins = Math.max(0, 60 - todayEarned) // 60 coin daily limit
            Result.success(remainingCoins)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    /**
     * Award game coins with daily limit enforcement
     */
    suspend fun awardGameCoins(
        childId: String,
        amount: Int,
        gameId: String,
        source: String
    ): Result<GameEarningDto> = withContext(Dispatchers.IO) {
        try {
            // For now, just award coins without daily limit until we implement proper tracking
            // TODO: Implement actual daily tracking with database storage
            
            val awardResult = awardCoins(
                childId = childId,
                amount = amount,
                category = "game_reward",
                description = "$source - $gameId",
                metadata = mapOf("gameId" to gameId, "source" to source)
            )
            
            awardResult.fold(
                onSuccess = { balanceChange ->
                    Result.success(GameEarningDto(
                        childId = childId,
                        gameId = gameId,
                        coinsAwarded = amount,
                        actualAmount = amount,
                        wasLimited = false,
                        dailyEarned = amount, // Simple tracking for now
                        dailyLimit = 60,
                        remainingToday = 60 - amount,
                        source = source,
                        timestamp = java.time.Instant.now().toString()
                    ))
                },
                onFailure = { error ->
                    Result.error(error)
                }
            )
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= HELPER METHODS =============

    private fun getAgeGroup(age: Int): String = when {
        age <= 5 -> "3-5 years"
        age <= 8 -> "6-8 years"
        else -> "9-12 years"
    }
} 