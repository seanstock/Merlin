package com.example.merlin.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.economy.model.*
import com.example.merlin.economy.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ViewModel for the Analytics screen with real service integration
 */
class AnalyticsViewModel(
    private val adaptiveDifficultyService: AdaptiveDifficultyService,
    private val economyService: EconomyService,
    private val badgeService: BadgeService,
    private val experienceService: ExperienceService
) : ViewModel() {
    
    companion object {
        private const val TAG = "AnalyticsViewModel"
        private const val DEFAULT_CHILD_ID = "demo_child" // Demo data for now
    }
    
    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()
    
    fun loadAnalytics(childId: String = DEFAULT_CHILD_ID) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Generate sample data for demo purposes if the service is the local implementation
                if (adaptiveDifficultyService is LocalAdaptiveDifficultyService) {
                    adaptiveDifficultyService.generateSampleData(childId, "math")
                    adaptiveDifficultyService.generateSampleData(childId, "reading")
                    adaptiveDifficultyService.generateSampleData(childId, "science")
                }

                // Load performance data from adaptive difficulty service
                val overallPerformance = loadOverallPerformance(childId)
                val subjectMastery = loadSubjectMastery(childId)
                val performanceTrends = loadPerformanceTrends(childId)
                val recommendations = loadRecommendations(childId)
                
                // Load economy data
                val experience = loadExperience(childId)
                val balance = loadBalance(childId)
                val recentTransactions = loadRecentTransactions(childId)
                
                // Load badge data
                val earnedBadges = loadEarnedBadges(childId)
                val availableBadges = loadAvailableBadges()
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    overallPerformance = overallPerformance,
                    subjectMastery = subjectMastery,
                    performanceTrends = performanceTrends,
                    earnedBadges = earnedBadges,
                    availableBadges = availableBadges,
                    experience = experience,
                    recommendations = recommendations,
                    balance = balance,
                    recentTransactions = recentTransactions
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading analytics", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadOverallPerformance(childId: String): PerformanceStatsDto? {
        return try {
            val result = adaptiveDifficultyService.getPerformanceStats(childId, "overall")
            if (result.isSuccess) {
                result.getOrNull()
            } else {
                // Generate sample task results and create stats
                adaptiveDifficultyService.getPerformanceStats(childId, "overall").getOrNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load performance stats, returning null", e)
            null
        }
    }
    
    private suspend fun loadSubjectMastery(childId: String): List<SubjectMasteryDto> {
        return try {
            val result = adaptiveDifficultyService.getAllSubjectMastery(childId)
            if (result.isSuccess) {
                val mastery = result.getOrNull()
                if (mastery.isNullOrEmpty()) {
                    // Generate sample data for all subjects
                    adaptiveDifficultyService.getAllSubjectMastery(childId).getOrNull() ?: emptyList()
                } else {
                    mastery
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load subject mastery, returning empty list", e)
            emptyList()
        }
    }
    
    private suspend fun loadPerformanceTrends(childId: String): List<PerformanceTrendDto> {
        return try {
            val result = adaptiveDifficultyService.getPerformanceTrends(childId, "overall", 7)
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load performance trends, returning empty list", e)
            emptyList()
        }
    }
    
    private suspend fun loadRecommendations(childId: String): List<LearningRecommendationDto> {
        return try {
            val result = adaptiveDifficultyService.getLearningRecommendations(childId, "overall")
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load recommendations, returning empty list", e)
            emptyList()
        }
    }
    
    private suspend fun loadExperience(childId: String): ExperienceDto? {
        return try {
            experienceService.getExperience(childId).getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load experience, returning null", e)
            null
        }
    }
    
    private suspend fun loadBalance(childId: String): BalanceDto? {
        return try {
            economyService.getBalance(childId).getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load balance, returning null", e)
            null
        }
    }
    
    private suspend fun loadRecentTransactions(childId: String): List<TransactionDto> {
        return try {
            economyService.getTransactionHistory(childId).getOrNull()?.take(5) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load transactions, returning empty list", e)
            emptyList()
        }
    }
    
    private suspend fun loadEarnedBadges(childId: String): List<BadgeDto> {
        return try {
            badgeService.getBadges(childId).getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load earned badges, returning empty list", e)
            emptyList()
        }
    }
    
    private suspend fun loadAvailableBadges(): List<BadgeDefinitionDto> {
        return try {
            badgeService.getAllBadgeDefinitions().getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load available badges, returning empty list", e)
            emptyList()
        }
    }
}

/**
 * State data class for Analytics screen
 */
data class AnalyticsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val overallPerformance: PerformanceStatsDto? = null,
    val subjectMastery: List<SubjectMasteryDto> = emptyList(),
    val performanceTrends: List<PerformanceTrendDto> = emptyList(),
    val earnedBadges: List<BadgeDto> = emptyList(),
    val availableBadges: List<BadgeDefinitionDto> = emptyList(),
    val experience: ExperienceDto? = null,
    val recommendations: List<LearningRecommendationDto> = emptyList(),
    val balance: BalanceDto? = null,
    val recentTransactions: List<TransactionDto> = emptyList()
) 