package com.example.merlin.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.economy.service.AppLaunchService
import com.example.merlin.economy.service.PurchasableAppDto
import com.example.merlin.economy.service.AppAccessPurchaseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for camera access functionality.
 * Follows LaaS architecture by depending on AppLaunchService interface, not implementation.
 */
class CameraAccessViewModel(
    application: Application,
    private val childId: String,
    private val appLaunchService: AppLaunchService
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableApps = MutableStateFlow<List<PurchasableAppDto>>(emptyList())
    val availableApps: StateFlow<List<PurchasableAppDto>> = _availableApps.asStateFlow()

    private val _lastPurchaseResult = MutableStateFlow<AppAccessPurchaseDto?>(null)
    val lastPurchaseResult: StateFlow<AppAccessPurchaseDto?> = _lastPurchaseResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAvailableApps()
    }

    /**
     * Load list of available apps for purchase
     */
    private fun loadAvailableApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = appLaunchService.getAvailableApps()
                if (result.isSuccess) {
                    _availableApps.value = result.getOrThrow()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to load available apps: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading apps: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Purchase access to an app with coins
     */
    fun purchaseAppAccess(appPackage: String, durationMinutes: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = appLaunchService.purchaseAppAccess(childId, appPackage, durationMinutes)
                if (result.isSuccess) {
                    _lastPurchaseResult.value = result.getOrThrow()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Purchase failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error during purchase: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate cost for app access
     */
    suspend fun calculateCost(appPackage: String, durationMinutes: Int): Result<Int> {
        return appLaunchService.calculateAppAccessCost(appPackage, durationMinutes)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear last purchase result
     */
    fun clearLastPurchase() {
        _lastPurchaseResult.value = null
    }
} 