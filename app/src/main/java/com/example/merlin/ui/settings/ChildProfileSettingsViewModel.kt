package com.example.merlin.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.data.database.entities.ChildProfile
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.config.ServiceLocator
import com.example.merlin.ui.theme.AppTheme
import com.example.merlin.ui.theme.ThemeService
import com.example.merlin.utils.UserSessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChildProfileSettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val childProfile: ChildProfile? = null,
    val availableThemes: List<AppTheme> = emptyList(),
    val isSavable: Boolean = false,
    val saveSuccess: Boolean = false
)

class ChildProfileSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val childProfileRepository: ChildProfileRepository
    private val userSessionRepository: UserSessionRepository
    private val themeService: ThemeService

    private val _uiState = MutableStateFlow(ChildProfileSettingsUiState())
    val uiState: StateFlow<ChildProfileSettingsUiState> = _uiState.asStateFlow()

    private var initialProfile: ChildProfile? = null

    init {
        val database = DatabaseProvider.getInstance(application)
        childProfileRepository = ChildProfileRepository(database.childProfileDao())
        userSessionRepository = UserSessionRepository.getInstance(application)
        themeService = ServiceLocator.getThemeService(application)

        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val childId = userSessionRepository.getActiveChildId()
            if (childId == null) {
                _uiState.value = ChildProfileSettingsUiState(isLoading = false, error = "No active child found.")
                return@launch
            }

            try {
                val profile = childProfileRepository.getById(childId)
                initialProfile = profile
                val themes = themeService.getAllThemes()
                _uiState.value = ChildProfileSettingsUiState(
                    isLoading = false,
                    childProfile = profile,
                    availableThemes = themes
                )
            } catch (e: Exception) {
                _uiState.value = ChildProfileSettingsUiState(isLoading = false, error = "Failed to load child profile.")
            }
        }
    }

    fun onNameChanged(name: String) {
        updateProfile { it.copy(name = name) }
    }

    fun onAgeChanged(ageString: String) {
        val age = ageString.toIntOrNull()
        if (age != null) {
            updateProfile { it.copy(age = age) }
        }
    }

    fun onGenderChanged(gender: String) {
        updateProfile { it.copy(gender = gender) }
    }

    fun onThemeChanged(themeId: String) {
        updateProfile { it.copy(selectedTheme = themeId) }
    }

    private fun updateProfile(updater: (ChildProfile) -> ChildProfile) {
        _uiState.update { currentState ->
            val updatedProfile = currentState.childProfile?.let(updater)
            currentState.copy(
                childProfile = updatedProfile,
                isSavable = updatedProfile != initialProfile // Check if changed from initial
            )
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val currentProfile = _uiState.value.childProfile
            if (currentProfile != null && _uiState.value.isSavable) {
                try {
                    childProfileRepository.update(currentProfile)
                    initialProfile = currentProfile // Update initial state after save
                    _uiState.update { it.copy(isSavable = false, saveSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to save changes.") }
                }
            }
        }
    }
    
    fun onSaveHandled() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
} 