package com.example.merlin.ui.camera

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.merlin.economy.service.AppLaunchService

/**
 * Factory for creating CameraAccessViewModel instances.
 * Follows LaaS architecture by injecting service interfaces, not implementations.
 */
class CameraAccessViewModelFactory(
    private val application: Application,
    private val childId: String,
    private val appLaunchService: AppLaunchService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraAccessViewModel::class.java)) {
            return CameraAccessViewModel(application, childId, appLaunchService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
} 