package com.example.merlin.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.merlin.economy.service.ScreenTimeService

class ScreenTimeViewModelFactory(
    private val screenTimeService: ScreenTimeService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScreenTimeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScreenTimeViewModel(screenTimeService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 