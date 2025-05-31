/*
// TEMPORARY: File commented out due to compilation errors
// TODO: Fix implementation issues and uncomment

package com.example.merlin.economy.service

import com.example.merlin.data.repository.TransactionRepository
import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.economy.model.*
import com.example.merlin.economy.mapper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Local implementation of EconomyService using Room database.
 * Handles all coin transactions, applies multipliers, manages daily caps, and tracks transaction history.
 * Implements the 1 Second = 1 Merlin Coin conversion and category-specific discount rates.
 */
class LocalEconomyService(
    private val transactionRepository: TransactionRepository,
    private val economyStateRepository: EconomyStateRepository,
    private val childProfileRepository: ChildProfileRepository
) : EconomyService {

    companion object {
        // Daily caps by age group (in seconds/MC)
        private const val DAILY_CAP_3_5_YEARS = 1800   // 30 minutes
        private const val DAILY_CAP_6_8_YEARS = 2700   // 45 minutes  
        private const val DAILY_CAP_9_12_YEARS = 3600  // 60 minutes
        
        // Reward multipliers
        private const val PERFECT_COMPLETION_MULTIPLIER = 1.5f
        private const val FIRST_TRY_BONUS = 15
        private const val NEW_CONCEPT_MULTIPLIER = 2.0f
        private const val TEACHING_MODE_BONUS = 30
        
        // Category discount rates (spending multipliers)
        private const val ENTERTAINMENT_RATE = 1.0f     // 1:1
        private const val EDUCATIONAL_GAMES_RATE = 0.8f // 20% discount
        private const val CREATIVE_APPS_RATE = 0.7f     // 30% discount
        private const val PHYSICAL_ACTIVITY_RATE = 0.5f // 50% discount
        
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
            
            val todayStart = getCurrentDayStartTimestamp()
            val todayEnd = getCurrentDayEndTimestamp()
            
            // Calculate today's earnings and spending
            val todayEarnings = transactionRepository.getSumByCategoriesInPeriod(
                childId, 
                TransactionCategory.EARNING_CATEGORIES.toList(),
                todayStart, 
                todayEnd
            )
            
            val todaySpending = transactionRepository.getSumByCategoriesInPeriod(
                childId,
                TransactionCategory.SPENDING_CATEGORIES.toList(),
                todayStart,
                todayEnd
            )
            
            val ageGroup = when {
                age <= 5 -> "3-5 years"
                age <= 8 -> "6-8 years"
                else -> "9-12 years"
            }
            
            val balance = if (economyState != null) {
                economyState.toBalanceDto(dailyCap, todayEarnings, Math.abs(todaySpending), ageGroup)
            } else {
                // Create default balance for new child
                BalanceDto(
                    childId = childId,
                    balance = 0,
                    dailyCap = dailyCap,
                    todayEarned = 0,
                    todaySpent = 0,
                    ageGroup = ageGroup,
                    lastUpdated = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                )
            }
            
            Result.success(balance)
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

    override suspend fun canEarnMore(childId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val balance = getBalance(childId).getOrThrow()
            Result.success(balance.todayEarned < balance.dailyCap)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getRemainingDailyEarnings(childId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val balance = getBalance(childId).getOrThrow()
            val remaining = maxOf(0, balance.dailyCap - balance.todayEarned)
            Result.success(remaining)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= TRANSACTION MANAGEMENT =============

    override suspend fun recordTransaction(transaction: TransactionDto): Result<TransactionDto> = withContext(Dispatchers.IO) {
        try {
            val entity = transaction.toEntity()
            transactionRepository.insert(entity)
            
            // Update economy state
            updateEconomyState(transaction.childId, transaction.amount)
            
            Result.success(transaction.copy(id = entity.id))
        } catch (e: Exception) {
            Result.error(e)
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
            // Check daily cap
            val canEarn = canEarnMore(childId).getOrThrow()
            if (!canEarn) {
                return@withContext Result.error("Daily earning cap reached")
            }
            
            val remaining = getRemainingDailyEarnings(childId).getOrThrow()
            val actualAmount = minOf(amount, remaining)
            
            val transaction = TransactionDto(
                id = UUID.randomUUID().toString(),
                childId = childId,
                amount = actualAmount,
                category = category,
                description = description,
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                metadata = metadata
            )
            
            recordTransaction(transaction).getOrThrow()
            
            val newBalance = getBalance(childId).getOrThrow()
            
            Result.success(BalanceChangeDto(
                childId = childId,
                previousBalance = newBalance.balance - actualAmount,
                newBalance = newBalance.balance,
                changeAmount = actualAmount,
                reason = description,
                category = category,
                timestamp = transaction.timestamp
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
            // Check sufficient funds
            val hasFunds = hasSufficientCoins(childId, amount).getOrThrow()
            if (!hasFunds) {
                return@withContext Result.error("Insufficient coins")
            }
            
            val transaction = TransactionDto(
                id = UUID.randomUUID().toString(),
                childId = childId,
                amount = -amount, // Negative for spending
                category = category,
                description = description,
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                metadata = metadata
            )
            
            recordTransaction(transaction).getOrThrow()
            
            val newBalance = getBalance(childId).getOrThrow()
            
            Result.success(BalanceChangeDto(
                childId = childId,
                previousBalance = newBalance.balance + amount,
                newBalance = newBalance.balance,
                changeAmount = -amount,
                reason = description,
                category = category,
                timestamp = transaction.timestamp
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getTransactionHistory(
        childId: String,
        limit: Int,
        offset: Int
    ): Result<List<TransactionDto>> = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionRepository.getTransactionsForChild(childId, limit, offset)
            Result.success(transactions.map { it.toDto() })
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getTransactionSummary(
        childId: String,
        startDate: String,
        endDate: String
    ): Result<TransactionSummaryDto> = withContext(Dispatchers.IO) {
        try {
            val startTimestamp = isoStringToTimestamp(startDate)
            val endTimestamp = isoStringToTimestamp(endDate)
            
            val transactions = transactionRepository.getTransactionsForChildInPeriod(
                childId, startTimestamp, endTimestamp
            )
            
            val totalEarned = transactions.filter { it.amount > 0 }.sumOf { it.amount }
            val totalSpent = transactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
            
            val earningsBreakdown = transactions.filter { it.amount > 0 }
                .groupBy { it.category }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                
            val spendingBreakdown = transactions.filter { it.amount < 0 }
                .groupBy { it.category }
                .mapValues { (_, txns) -> txns.sumOf { Math.abs(it.amount) } }
            
            val currentBalance = getBalance(childId).getOrThrow()
            
            Result.success(TransactionSummaryDto(
                childId = childId,
                totalEarned = totalEarned,
                totalSpent = totalSpent,
                currentBalance = currentBalance.balance,
                todayEarned = currentBalance.todayEarned,
                todaySpent = currentBalance.todaySpent,
                earningsBreakdown = earningsBreakdown,
                spendingBreakdown = spendingBreakdown,
                period = "$startDate to $endDate"
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= REWARD CALCULATION =============

    override suspend fun calculateReward(
        difficulty: Int,
        isFirstTry: Boolean,
        isPerfect: Boolean,
        isNewConcept: Boolean,
        isTeachingMode: Boolean
    ): Result<Int> {
        return try {
            // Base reward: 30-60 MC based on difficulty
            val baseReward = 30 + (difficulty * 6) // difficulty 1-5 gives 36-60 MC
            
            var finalReward = baseReward.toFloat()
            
            // Apply multipliers
            if (isPerfect) {
                finalReward *= PERFECT_COMPLETION_MULTIPLIER
            }
            
            if (isNewConcept) {
                finalReward *= NEW_CONCEPT_MULTIPLIER
            }
            
            // Add bonuses
            if (isFirstTry) {
                finalReward += FIRST_TRY_BONUS
            }
            
            if (isTeachingMode) {
                finalReward += TEACHING_MODE_BONUS
            }
            
            Result.success(finalReward.toInt())
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun calculateScreenTimeCost(
        timeInSeconds: Int,
        category: String
    ): Result<Int> {
        return try {
            val rate = when (category) {
                TransactionCategory.SPENDING_ENTERTAINMENT -> ENTERTAINMENT_RATE
                TransactionCategory.SPENDING_EDUCATIONAL_GAMES -> EDUCATIONAL_GAMES_RATE
                TransactionCategory.SPENDING_CREATIVE_APPS -> CREATIVE_APPS_RATE
                TransactionCategory.SPENDING_PHYSICAL_ACTIVITY -> PHYSICAL_ACTIVITY_RATE
                else -> ENTERTAINMENT_RATE // Default to 1:1
            }
            
            val cost = (timeInSeconds * rate).toInt()
            Result.success(cost)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun convertToRealWorldValue(coins: Int): Result<Float> {
        return try {
            val cents = coins / MC_TO_CENTS_RATIO
            Result.success(cents)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= SPENDING VALIDATION =============

    override suspend fun hasSufficientCoins(childId: String, amount: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val balance = getBalance(childId).getOrThrow()
            Result.success(balance.balance >= amount)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun validateSpendingRequest(
        childId: String,
        amount: Int,
        category: String
    ): Result<SpendingValidationDto> = withContext(Dispatchers.IO) {
        try {
            val balance = getBalance(childId).getOrThrow()
            val hasFunds = balance.balance >= amount
            
            val validation = SpendingValidationDto(
                isValid = hasFunds,
                errorMessage = if (hasFunds) "" else "Insufficient funds",
                currentBalance = balance.balance,
                requestedAmount = amount,
                afterSpendingBalance = if (hasFunds) balance.balance - amount else balance.balance
            )
            
            Result.success(validation)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= ANALYTICS & REPORTING =============

    override suspend fun getEarningPatterns(
        childId: String,
        days: Int
    ): Result<List<EarningPatternDto>> = withContext(Dispatchers.IO) {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (days * 24 * 60 * 60 * 1000L)
            
            val transactions = transactionRepository.getTransactionsForChildInPeriod(
                childId, startTime, endTime
            ).filter { it.amount > 0 } // Only earnings
            
            val patterns = transactions.groupBy { getStartOfDay(it.timestamp) }
                .map { (dayStart, dayTransactions) ->
                    val earnedByCategory = dayTransactions.groupBy { it.category }
                        .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                    
                    EarningPatternDto(
                        date = timestampToIsoString(dayStart),
                        totalEarned = dayTransactions.sumOf { it.amount },
                        earnedByCategory = earnedByCategory,
                        taskCount = dayTransactions.size,
                        averageTaskReward = if (dayTransactions.isNotEmpty()) {
                            dayTransactions.sumOf { it.amount }.toFloat() / dayTransactions.size
                        } else 0f
                    )
                }
                .sortedBy { it.date }
            
            Result.success(patterns)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getSpendingPatterns(
        childId: String,
        days: Int
    ): Result<List<SpendingPatternDto>> = withContext(Dispatchers.IO) {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (days * 24 * 60 * 60 * 1000L)
            
            val transactions = transactionRepository.getTransactionsForChildInPeriod(
                childId, startTime, endTime
            ).filter { it.amount < 0 } // Only spending
            
            val patterns = transactions.groupBy { getStartOfDay(it.timestamp) }
                .map { (dayStart, dayTransactions) ->
                    val spentByCategory = dayTransactions.groupBy { it.category }
                        .mapValues { (_, txns) -> txns.sumOf { Math.abs(it.amount) } }
                    
                    val mostUsedCategory = spentByCategory.maxByOrNull { it.value }?.key ?: ""
                    
                    SpendingPatternDto(
                        date = timestampToIsoString(dayStart),
                        totalSpent = dayTransactions.sumOf { Math.abs(it.amount) },
                        spentByCategory = spentByCategory,
                        averageTimePerSession = 0f, // Would need session tracking for this
                        mostUsedCategory = mostUsedCategory
                    )
                }
                .sortedBy { it.date }
            
            Result.success(patterns)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getEconomyDashboard(childId: String): Result<EconomyDashboardDto> = withContext(Dispatchers.IO) {
        try {
            val balance = getBalance(childId).getOrThrow()
            val recentTransactions = getTransactionHistory(childId, 10, 0).getOrThrow()
            
            // Calculate weekly stats
            val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val weekEnd = System.currentTimeMillis()
            
            val weeklyTransactions = transactionRepository.getTransactionsForChildInPeriod(
                childId, weekStart, weekEnd
            )
            
            val weeklyEarnings = weeklyTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val weeklySpending = weeklyTransactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
            
            val topEarningCategories = weeklyTransactions.filter { it.amount > 0 }
                .groupBy { it.category }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .toMap()
            
            val topSpendingCategories = weeklyTransactions.filter { it.amount < 0 }
                .groupBy { it.category }
                .mapValues { (_, txns) -> txns.sumOf { Math.abs(it.amount) } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .toMap()
            
            val realWorldValue = convertToRealWorldValue(balance.balance).getOrThrow()
            
            Result.success(EconomyDashboardDto(
                balance = balance,
                recentTransactions = recentTransactions,
                weeklyEarnings = weeklyEarnings,
                weeklySpending = weeklySpending,
                topEarningCategories = topEarningCategories,
                topSpendingCategories = topSpendingCategories,
                streakDays = 0, // Would need streak tracking for this
                totalRealWorldValue = realWorldValue
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    private suspend fun updateEconomyState(childId: String, amountChange: Int) {
        try {
            val existingState = economyStateRepository.getByChildId(childId)
            
            if (existingState != null) {
                val updatedState = existingState.copy(
                    walletSeconds = (existingState.walletSeconds ?: 0) + amountChange,
                    lastEarnedTs = System.currentTimeMillis()
                )
                economyStateRepository.update(updatedState)
            } else {
                val newState = com.example.merlin.data.database.entities.EconomyState(
                    childId = childId,
                    streak = null,
                    fatigueScore = null,
                    walletSeconds = maxOf(0, amountChange),
                    badgesJson = null,
                    xpLevel = null,
                    lastEarnedTs = System.currentTimeMillis()
                )
                economyStateRepository.insert(newState)
            }
        } catch (e: Exception) {
            // Log error but don't fail the transaction
            // In a production app, you'd want proper logging here
        }
    }
}
*/ 