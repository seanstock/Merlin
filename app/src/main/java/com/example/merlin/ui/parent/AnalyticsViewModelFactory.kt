package com.example.merlin.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.merlin.economy.service.AdaptiveDifficultyService
import com.example.merlin.economy.service.BadgeService
import com.example.merlin.economy.service.EconomyService
import com.example.merlin.economy.service.ExperienceService

class AnalyticsViewModelFactory(
    private val adaptiveDifficultyService: AdaptiveDifficultyService,
    private val economyService: EconomyService,
    private val badgeService: BadgeService,
    private val experienceService: ExperienceService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(
                adaptiveDifficultyService,
                economyService,
                badgeService,
                experienceService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 