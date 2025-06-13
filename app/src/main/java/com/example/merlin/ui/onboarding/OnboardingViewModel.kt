package com.example.merlin.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.merlin.data.database.entities.ChildProfile
import com.example.merlin.data.database.entities.ParentSettings
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.repository.ParentSettingsRepository
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.merlin.utils.UserSessionRepository

/**
 * ViewModel for managing the onboarding flow state and data collection.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "OnboardingViewModel"
        private const val ONBOARDING_PREFS = "merlin_onboarding_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_CURRENT_CHILD_ID = "current_child_id"
    }
    
    private val context = application.applicationContext
    private val userSessionRepository = UserSessionRepository.getInstance(application)
    
    // TODO: These will be injected via DI in the future
    // For now, we'll need to initialize them when the database is available
    private var childProfileRepository: ChildProfileRepository? = null
    private var parentSettingsRepository: ParentSettingsRepository? = null
    
    private val onboardingPrefs: SharedPreferences by lazy {
        createEncryptedPreferences()
    }
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    /**
     * Data class representing the current state of the onboarding flow.
     */
    data class OnboardingUiState(
        val currentStep: OnboardingStep = OnboardingStep.WELCOME,
        val isLoading: Boolean = false,
        val error: String? = null,
        val childProfile: ChildProfileData = ChildProfileData(),
        val parentPin: String = "",
        val parentPinConfirmation: String = "",
        val permissionsGranted: Map<String, Boolean> = emptyMap(),
        val tutorialCompleted: Boolean = false
    )
    
    /**
     * Enum representing the different steps in the onboarding flow.
     */
    enum class OnboardingStep {
        WELCOME,
        PERMISSIONS,
        CHILD_INFO,
        THEME_SELECTION,
        PARENT_PIN,
        TUTORIAL,
        AI_INTRODUCTION,
        COMPLETED
    }
    
    /**
     * Data class for collecting child profile information.
     */
    data class ChildProfileData(
        val name: String = "",
        val age: Int? = null,
        val gender: String = "",
        val interests: List<String> = emptyList(),
        val preferredLanguage: String = "en",
        val selectedTheme: String? = null
    )
    
    /**
     * Check if onboarding has been completed.
     */
    fun isOnboardingCompleted(): Boolean {
        return userSessionRepository.isOnboardingCompleted()
    }
    
    /**
     * Get the current child ID if onboarding is completed.
     */
    fun getCurrentChildId(): String? {
        return if (isOnboardingCompleted()) {
            onboardingPrefs.getString(KEY_CURRENT_CHILD_ID, null)
        } else null
    }
    
    /**
     * Navigate to the next step in the onboarding flow.
     */
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val nextStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.CHILD_INFO
            OnboardingStep.CHILD_INFO -> OnboardingStep.THEME_SELECTION
            OnboardingStep.THEME_SELECTION -> OnboardingStep.PARENT_PIN
            OnboardingStep.PARENT_PIN -> OnboardingStep.TUTORIAL
            OnboardingStep.TUTORIAL -> OnboardingStep.AI_INTRODUCTION
            OnboardingStep.AI_INTRODUCTION -> OnboardingStep.COMPLETED
            OnboardingStep.COMPLETED -> OnboardingStep.COMPLETED
        }
        
        _uiState.value = _uiState.value.copy(currentStep = nextStep)
        
        if (nextStep == OnboardingStep.COMPLETED) {
            completeOnboarding()
        }
    }
    
    /**
     * Navigate to the previous step in the onboarding flow.
     */
    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        val previousStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.PERMISSIONS -> OnboardingStep.WELCOME
            OnboardingStep.CHILD_INFO -> OnboardingStep.PERMISSIONS
            OnboardingStep.THEME_SELECTION -> OnboardingStep.CHILD_INFO
            OnboardingStep.PARENT_PIN -> OnboardingStep.THEME_SELECTION
            OnboardingStep.TUTORIAL -> OnboardingStep.PARENT_PIN
            OnboardingStep.AI_INTRODUCTION -> OnboardingStep.TUTORIAL
            OnboardingStep.COMPLETED -> OnboardingStep.AI_INTRODUCTION
        }
        
        _uiState.value = _uiState.value.copy(currentStep = previousStep)
    }
    
    /**
     * Update child profile information.
     */
    fun updateChildProfile(
        name: String? = null,
        age: Int? = null,
        gender: String? = null,
        interests: List<String>? = null,
        preferredLanguage: String? = null,
        selectedTheme: String? = null
    ) {
        val currentProfile = _uiState.value.childProfile
        _uiState.value = _uiState.value.copy(
            childProfile = currentProfile.copy(
                name = name ?: currentProfile.name,
                age = age ?: currentProfile.age,
                gender = gender ?: currentProfile.gender,
                interests = interests ?: currentProfile.interests,
                preferredLanguage = preferredLanguage ?: currentProfile.preferredLanguage,
                selectedTheme = selectedTheme ?: currentProfile.selectedTheme
            )
        )
    }
    
    /**
     * Update parent PIN.
     */
    fun updateParentPin(pin: String) {
        _uiState.value = _uiState.value.copy(parentPin = pin)
    }
    
    /**
     * Update parent PIN confirmation.
     */
    fun updateParentPinConfirmation(pinConfirmation: String) {
        _uiState.value = _uiState.value.copy(parentPinConfirmation = pinConfirmation)
    }
    
    /**
     * Update permission status.
     */
    fun updatePermissionStatus(permission: String, granted: Boolean) {
        val currentPermissions = _uiState.value.permissionsGranted.toMutableMap()
        currentPermissions[permission] = granted
        _uiState.value = _uiState.value.copy(permissionsGranted = currentPermissions)
    }
    
    /**
     * Mark tutorial as completed.
     */
    fun markTutorialCompleted() {
        _uiState.value = _uiState.value.copy(tutorialCompleted = true)
    }
    
    /**
     * Validate the current step and return true if it's valid to proceed.
     */
    fun validateCurrentStep(): Boolean {
        return when (_uiState.value.currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.PERMISSIONS -> validatePermissions()
            OnboardingStep.CHILD_INFO -> validateChildInfo()
            OnboardingStep.THEME_SELECTION -> true
            OnboardingStep.PARENT_PIN -> validateParentPin()
            OnboardingStep.TUTORIAL -> _uiState.value.tutorialCompleted
            OnboardingStep.AI_INTRODUCTION -> true
            OnboardingStep.COMPLETED -> true
        }
    }
    
    /**
     * Validate that required permissions are granted.
     */
    private fun validatePermissions(): Boolean {
        val permissions = _uiState.value.permissionsGranted
        // For now, we'll require accessibility and overlay permissions
        return permissions["accessibility"] == true && permissions["overlay"] == true
    }
    
    /**
     * Validate child information.
     */
    private fun validateChildInfo(): Boolean {
        val profile = _uiState.value.childProfile
        return profile.name.isNotBlank() && profile.age != null && profile.age!! > 0
    }
    
    /**
     * Validate parent PIN.
     */
    private fun validateParentPin(): Boolean {
        val pin = _uiState.value.parentPin
        val confirmation = _uiState.value.parentPinConfirmation
        return pin.length >= 4 && pin == confirmation
    }
    
    /**
     * Complete the onboarding process by saving all data.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val childId = UUID.randomUUID().toString()
                val childProfile = createChildProfile(childId)
                val parentSettings = createParentSettings(childId)
                
                // Save to database (assuming repositories are injected/set)
                withContext(Dispatchers.IO) { // Perform database operations on IO dispatcher
                    childProfileRepository?.insert(childProfile) // Assuming this is suspend or handled by repo
                    parentSettingsRepository?.insert(parentSettings) // Assuming this is suspend or handled by repo
                }
                
                // Mark onboarding as completed in onboarding-specific prefs
                onboardingPrefs.edit()
                    .putBoolean(KEY_ONBOARDING_COMPLETED, true)
                    .putString(KEY_CURRENT_CHILD_ID, childId) // This stores it for this ViewModel's own check
                    .apply()

                // Save active child ID and name to general UserSessionRepository
                userSessionRepository.setActiveChildId(childId)
                userSessionRepository.saveChildName(childProfile.name!!)
                userSessionRepository.setOnboardingCompleted(true)
                
                Log.d(TAG, "Onboarding completed successfully for child ID: $childId, Name: ${childProfile.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error completing onboarding", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to complete setup: ${e.message}",
                    isLoading = false
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * Create a ChildProfile entity from the collected data.
     */
    private fun createChildProfile(childId: String): ChildProfile {
        val profileData = _uiState.value.childProfile
        return ChildProfile(
            id = childId,
            name = profileData.name,
            birthdate = null, // We could calculate this from age if needed
            age = profileData.age,
            gender = profileData.gender.takeIf { it.isNotBlank() },
            preferredLanguage = profileData.preferredLanguage,
            location = null, // We're not collecting location in this version
            selectedTheme = profileData.selectedTheme ?: "under_the_sea" // Default theme
        )
    }
    
    /**
     * Create a ParentSettings entity with the PIN and default settings.
     */
    private fun createParentSettings(childId: String): ParentSettings {
        val pin = _uiState.value.parentPin
        val (hashedPin, salt) = hashPin(pin)
        
        // Create a basic config JSON with PIN and default settings
        val configJson = """
            {
                "pin_hash": "$hashedPin",
                "salt": "$salt",
                "screen_time_limits": {
                    "daily_minutes": 120,
                    "session_minutes": 30
                },
                "subject_weights": {
                    "math": 1.0,
                    "reading": 1.0,
                    "science": 1.0
                },
                "content_filtering": {
                    "enabled": true,
                    "level": "strict"
                }
            }
        """.trimIndent()
        
        return ParentSettings(
            childId = childId,
            configJson = configJson
        )
    }
    
    /**
     * Hash the PIN with a salt for secure storage.
     */
    private fun hashPin(pin: String): Pair<String, String> {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltString = salt.joinToString("") { "%02x".format(it) }
        
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashedPin = digest.digest(pin.toByteArray())
        val hashedPinString = hashedPin.joinToString("") { "%02x".format(it) }
        
        return Pair(hashedPinString, saltString)
    }
    
    /**
     * Create encrypted shared preferences for onboarding data.
     */
    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                ONBOARDING_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted preferences, falling back to regular preferences", e)
            context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Set repository dependencies (to be called when DI is available).
     */
    fun setRepositories(
        childProfileRepository: ChildProfileRepository,
        parentSettingsRepository: ParentSettingsRepository
    ) {
        this.childProfileRepository = childProfileRepository
        this.parentSettingsRepository = parentSettingsRepository
    }
    
    /**
     * Clear onboarding data (for testing purposes).
     */
    fun clearOnboardingData() {
        onboardingPrefs.edit().clear().apply()
        _uiState.value = OnboardingUiState()
        userSessionRepository.clearActiveUserSession()
    }
    
    /**
     * Update selected theme.
     */
    fun updateSelectedTheme(themeId: String) {
        updateChildProfile(selectedTheme = themeId)
    }
} 