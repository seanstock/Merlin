package com.example.merlin.ui.theme

import com.example.merlin.data.repository.ChildProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for managing app themes based on child preferences
 */
class ThemeService(
    private val childProfileRepository: ChildProfileRepository
) {
    
    /**
     * Get the current theme for a specific child
     */
    suspend fun getThemeForChild(childId: String): AppTheme {
        return withContext(Dispatchers.IO) {
            val profile = childProfileRepository.getById(childId)
            val themeId = profile?.selectedTheme ?: AppThemes.getDefaultTheme().id
            AppThemes.getThemeById(themeId) ?: AppThemes.getDefaultTheme()
        }
    }
    
    /**
     * Get a flow of the current theme for a specific child
     */
    fun getThemeFlowForChild(childId: String): Flow<AppTheme> {
        return childProfileRepository.getByIdFlow(childId).map { profile ->
            val themeId = profile?.selectedTheme ?: AppThemes.getDefaultTheme().id
            AppThemes.getThemeById(themeId) ?: AppThemes.getDefaultTheme()
        }
    }
    
    /**
     * Update the theme for a specific child
     */
    suspend fun updateThemeForChild(childId: String, themeId: String) {
        withContext(Dispatchers.IO) {
            val profile = childProfileRepository.getById(childId)
            if (profile != null) {
                val updatedProfile = profile.copy(selectedTheme = themeId)
                childProfileRepository.update(updatedProfile)
            }
        }
    }
    
    /**
     * Get all available themes
     */
    fun getAllThemes(): List<AppTheme> {
        return AppThemes.ALL_THEMES
    }
} 