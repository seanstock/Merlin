package com.example.merlin.ui.wallet

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.economy.service.LocalEconomyService
import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.database.DatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing wallet state and economy service interactions.
 */
class WalletViewModel(
    application: Application,
    private val childId: String
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WalletViewModel"
    }

    // Dependencies
    private val database = DatabaseProvider.getInstance(application)
    private val economyStateRepository = EconomyStateRepository(database.economyStateDao())
    private val childProfileRepository = ChildProfileRepository(database.childProfileDao())
    private val economyService = LocalEconomyService(economyStateRepository, childProfileRepository)

    // Wallet state
    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Screen time state
    private val _screenTimeRemaining = MutableStateFlow(0)
    val screenTimeRemaining: StateFlow<Int> = _screenTimeRemaining.asStateFlow()

    // Notification events
    private val _notificationEvent = MutableStateFlow<WalletNotificationEvent?>(null)
    val notificationEvent: StateFlow<WalletNotificationEvent?> = _notificationEvent.asStateFlow()

    init {
        Log.d(TAG, "WalletViewModel initialized for childId: $childId")
        loadBalance()
    }

    /**
     * Load current wallet balance from the economy service
     */
    fun loadBalance() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val balanceResult = economyService.getBalance(childId)
                balanceResult
                    .onSuccess { balanceDto ->
                        _balance.value = balanceDto.balance
                        Log.d(TAG, "Balance loaded: ${balanceDto.balance} MC")
                    }
                    .onError { exception ->
                        Log.e(TAG, "Failed to load balance", exception)
                        _errorMessage.value = "Failed to load wallet balance"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading balance", e)
                _errorMessage.value = "Unexpected error loading wallet"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Spend coins on screen time
     */
    fun spendCoins(timeInSeconds: Int, category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Calculate cost based on category
                val costResult = economyService.calculateScreenTimeCost(timeInSeconds, category)
                costResult
                    .onSuccess { cost ->
                        // Check if user can afford it
                        val validationResult = economyService.validateSpendingRequest(childId, cost, category)
                        validationResult
                            .onSuccess { validation ->
                                if (validation.isValid) {
                                    // Spend the coins
                                    val spendResult = economyService.spendCoins(
                                        childId = childId,
                                        amount = cost,
                                        category = category,
                                        description = "Screen time unlock: ${timeInSeconds / 60}m ${timeInSeconds % 60}s",
                                        metadata = mapOf(
                                            "category" to category,
                                            "timeInSeconds" to timeInSeconds.toString()
                                        )
                                    )
                                    spendResult
                                        .onSuccess { balanceChange ->
                                            _balance.value = balanceChange.newBalance
                                            _screenTimeRemaining.value = timeInSeconds
                                            
                                            // Trigger success notification
                                            _notificationEvent.value = WalletNotificationEvent.SpendingSuccess(
                                                amount = cost,
                                                timeUnlocked = timeInSeconds,
                                                category = category
                                            )
                                            
                                            Log.d(TAG, "Successfully spent $cost MC for $timeInSeconds seconds of $category time")
                                        }
                                        .onError { exception ->
                                            Log.e(TAG, "Failed to spend coins", exception)
                                            _errorMessage.value = "Failed to spend coins"
                                        }
                                } else {
                                    _errorMessage.value = validation.errorMessage
                                    Log.w(TAG, "Spending validation failed: ${validation.errorMessage}")
                                }
                            }
                            .onError { exception ->
                                Log.e(TAG, "Failed to validate spending", exception)
                                _errorMessage.value = "Failed to validate spending request"
                            }
                    }
                    .onError { exception ->
                        Log.e(TAG, "Failed to calculate cost", exception)
                        _errorMessage.value = "Failed to calculate cost"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error spending coins", e)
                _errorMessage.value = "Unexpected error spending coins"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Award coins for completing tasks/games
     */
    fun awardCoins(
        amount: Int,
        category: String,
        description: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                val awardResult = economyService.awardCoins(
                    childId = childId,
                    amount = amount,
                    category = category,
                    description = description,
                    metadata = metadata
                )
                awardResult
                    .onSuccess { balanceChange ->
                        _balance.value = balanceChange.newBalance
                        
                        // Trigger earning notification
                        _notificationEvent.value = WalletNotificationEvent.EarningSuccess(
                            amount = amount,
                            newBalance = balanceChange.newBalance,
                            reason = description
                        )
                        
                        Log.d(TAG, "Successfully awarded $amount MC. New balance: ${balanceChange.newBalance}")
                    }
                    .onError { exception ->
                        Log.e(TAG, "Failed to award coins", exception)
                        _errorMessage.value = "Failed to award coins"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error awarding coins", e)
                _errorMessage.value = "Unexpected error awarding coins"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear notification event
     */
    fun clearNotificationEvent() {
        _notificationEvent.value = null
    }

    /**
     * Update screen time remaining (called by timer)
     */
    fun updateScreenTimeRemaining(seconds: Int) {
        _screenTimeRemaining.value = seconds
        if (seconds <= 0) {
            // Trigger screen time expired notification
            _notificationEvent.value = WalletNotificationEvent.ScreenTimeExpired
        }
    }

    /**
     * Check if child can afford a specific amount
     */
    suspend fun canAfford(amount: Int): Boolean {
        return try {
            val result = economyService.hasSufficientCoins(childId, amount)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking affordability", e)
            false
        }
    }

    /**
     * Get balance warnings (when running low)
     */
    fun checkBalanceWarnings() {
        val currentBalance = _balance.value
        when {
            currentBalance == 0 -> {
                _notificationEvent.value = WalletNotificationEvent.BalanceWarning(
                    "No coins left! Complete learning tasks to earn more."
                )
            }
            currentBalance <= 30 -> { // Less than 30 seconds
                _notificationEvent.value = WalletNotificationEvent.BalanceWarning(
                    "Only $currentBalance MC left - earn more by completing tasks!"
                )
            }
        }
    }
}

/**
 * Events for wallet notifications and feedback
 */
sealed class WalletNotificationEvent {
    data class EarningSuccess(
        val amount: Int,
        val newBalance: Int,
        val reason: String
    ) : WalletNotificationEvent()
    
    data class SpendingSuccess(
        val amount: Int,
        val timeUnlocked: Int,
        val category: String
    ) : WalletNotificationEvent()
    
    data class BalanceWarning(
        val message: String
    ) : WalletNotificationEvent()
    
    object ScreenTimeExpired : WalletNotificationEvent()
} 