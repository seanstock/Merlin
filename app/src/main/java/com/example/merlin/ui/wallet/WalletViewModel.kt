package com.example.merlin.ui.wallet

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.economy.service.LocalEconomyService
import com.example.merlin.economy.service.AppLaunchService
import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.timer.ScreenTimeManager
import com.example.merlin.config.ServiceLocator
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
    private val appLaunchService = ServiceLocator.getAppLaunchService(application)
    private val appSessionManager = ServiceLocator.getAppSessionManager()

    // Wallet state
    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Screen time state is now managed globally by ScreenTimeManager

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
                    .onFailure { exception ->
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
     * Spend coins on screen time or special purchases
     */
    fun spendCoins(timeInSeconds: Int, category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                when (category) {
                    "call_daddy" -> {
                        // Special case: Fixed cost for calling daddy
                        val cost = timeInSeconds // In this case, timeInSeconds is actually the cost
                        val validationResult = economyService.validateSpendingRequest(childId, cost, category)
                        validationResult
                            .onSuccess { validation ->
                                if (validation.isValid) {
                                    val spendResult = economyService.spendCoins(
                                        childId = childId,
                                        amount = cost,
                                        category = "SPENDING_CALL",
                                        description = "Call to daddy",
                                        metadata = mapOf(
                                            "category" to category,
                                            "cost" to cost.toString()
                                        )
                                    )
                                    spendResult
                                        .onSuccess { balanceChange ->
                                            _balance.value = balanceChange.newBalance
                                            
                                            // Trigger success notification
                                            _notificationEvent.value = WalletNotificationEvent.SpendingSuccess(
                                                amount = cost,
                                                timeUnlocked = 0, // No screen time unlocked
                                                category = category
                                            )
                                            
                                            Log.d(TAG, "Successfully spent $cost MC for call to daddy")
                                        }
                                        .onFailure { exception ->
                                            Log.e(TAG, "Failed to spend coins for call", exception)
                                            _errorMessage.value = "Failed to spend coins for call"
                                        }
                                } else {
                                    _errorMessage.value = validation.errorMessage
                                    Log.w(TAG, "Call spending validation failed: ${validation.errorMessage}")
                                }
                            }
                            .onFailure { exception ->
                                Log.e(TAG, "Failed to validate call spending", exception)
                                _errorMessage.value = "Failed to validate spending request"
                            }
                    }
                    
                    else -> {
                        // Regular screen time purchase
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
                                                    ScreenTimeManager.addTime(timeInSeconds)
                                                    
                                                    // Trigger success notification
                                                    _notificationEvent.value = WalletNotificationEvent.SpendingSuccess(
                                                        amount = cost,
                                                        timeUnlocked = timeInSeconds,
                                                        category = category
                                                    )
                                                    
                                                    Log.d(TAG, "Successfully spent $cost MC for $timeInSeconds seconds of $category time")
                                                }
                                                .onFailure { exception ->
                                                    Log.e(TAG, "Failed to spend coins", exception)
                                                    _errorMessage.value = "Failed to spend coins"
                                                }
                                        } else {
                                            _errorMessage.value = validation.errorMessage
                                            Log.w(TAG, "Spending validation failed: ${validation.errorMessage}")
                                        }
                                    }
                                    .onFailure { exception ->
                                        Log.e(TAG, "Failed to validate spending", exception)
                                        _errorMessage.value = "Failed to validate spending request"
                                    }
                            }
                            .onFailure { exception ->
                                Log.e(TAG, "Failed to calculate cost", exception)
                                _errorMessage.value = "Failed to calculate cost"
                            }
                    }
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
                    .onFailure { exception ->
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
     * Refresh balance from database - call this when returning to screen
     */
    fun refreshBalance() {
        loadBalance()
    }

    /**
     * Clear notification event
     */
    fun clearNotificationEvent() {
        _notificationEvent.value = null
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

    /**
     * Purchase app access - separate from screen time spending
     */
    fun purchaseAppAccess(appPackage: String, durationMinutes: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Whitelist the external app for lock-task mode
                ServiceLocator.getKioskManager(getApplication()).addAllowedPackage(appPackage)

                val purchaseResult = appLaunchService.purchaseAppAccess(
                    childId = childId,
                    appPackage = appPackage,
                    durationMinutes = durationMinutes
                )
                
                purchaseResult
                    .onSuccess { purchase ->
                        if (purchase.success) {
                            // Now spend the coins through the economy service
                            val spendResult = economyService.spendCoins(
                                childId = childId,
                                amount = purchase.totalCost,
                                category = "SPENDING_APP_ACCESS",
                                description = "App access purchase: $appPackage for $durationMinutes minutes",
                                metadata = mapOf(
                                    "app_package" to appPackage,
                                    "duration_minutes" to durationMinutes.toString()
                                )
                            )
                            
                            spendResult
                                .onSuccess { balanceChange ->
                                    _balance.value = balanceChange.newBalance
                                    
                                    // Get app icon and add session
                                    val iconResult = (appLaunchService as? com.example.merlin.economy.service.LocalAppLaunchService)?.getAppIcon(appPackage)
                                    val appIcon = iconResult?.getOrNull()
                                    val appName = appPackage.substringAfterLast(".")
                                    appSessionManager.addSession(purchase, appName, appIcon)
                                    
                                    // Trigger success notification
                                    _notificationEvent.value = WalletNotificationEvent.SpendingSuccess(
                                        amount = purchase.totalCost,
                                        timeUnlocked = 0, // No screen time unlocked
                                        category = "app_access"
                                    )
                                    
                                    Log.d(TAG, "Successfully purchased app access: $appPackage for $durationMinutes minutes")
                                }
                                .onFailure { exception ->
                                    Log.e(TAG, "Failed to spend coins for app access", exception)
                                    _errorMessage.value = "Failed to spend coins for app access"
                                }
                        } else {
                            _errorMessage.value = purchase.errorMessage
                            Log.e(TAG, "App access purchase failed: ${purchase.errorMessage}")
                        }
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to purchase app access", exception)
                        _errorMessage.value = "Failed to purchase app access"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error purchasing app access", e)
                _errorMessage.value = "Unexpected error purchasing app access"
            } finally {
                _isLoading.value = false
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