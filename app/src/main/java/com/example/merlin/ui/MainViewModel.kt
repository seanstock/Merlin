package com.example.merlin.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.merlin.utils.UserSessionRepository
import com.example.merlin.config.ServiceLocator

/**
 * Sealed interface for UI variant state management
 */
sealed class UiVariantState {
    object Loading : UiVariantState()
    data class Error(val message: String) : UiVariantState()
    data class Success(val variant: UIVariant) : UiVariantState()
}

/**
 * ViewModel for determining the appropriate UI variant based on child's age
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val userSessionRepository = UserSessionRepository.getInstance(context)

    // Use the new state holder, starting in a Loading state
    private val _uiVariantState = MutableStateFlow<UiVariantState>(UiVariantState.Loading)
    val uiVariantState: StateFlow<UiVariantState> = _uiVariantState.asStateFlow()

    // Keep the old property for compatibility if needed, but drive it from the new state
    val uiVariant: StateFlow<UIVariant>
        get() = _uiVariantState.map {
            when (it) {
                is UiVariantState.Success -> it.variant
                else -> UIVariant.ADVANCED // Fallback for error/loading
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UIVariant.ADVANCED)

    companion object {
        private const val TAG = "MainViewModel"
    }

    init {
        // Observe active child ID changes and update UI variant reactively
        observeActiveChildChanges()
    }

    /**
     * Observe changes to the active child ID and update UI variant accordingly
     */
    private fun observeActiveChildChanges() {
        viewModelScope.launch {
            userSessionRepository.activeChildIdFlow.collect { activeChildId ->
                Log.d(TAG, "Active child ID changed: $activeChildId")
                if (activeChildId != null) {
                    try {
                        val uiConfigService = ServiceLocator.getUIConfigurationService(context)
                        val variant = uiConfigService.getUIVariant(activeChildId)
                        Log.d(TAG, "UI variant for child $activeChildId: $variant")
                        _uiVariantState.value = UiVariantState.Success(variant)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing UI variant", e)
                        _uiVariantState.value = UiVariantState.Error("Failed to load UI variant")
                    }
                } else {
                    Log.d(TAG, "No active child ID found, waiting for update...")
                    _uiVariantState.value = UiVariantState.Loading
                }
            }
        }
    }
} 