package com.example.merlin.curriculum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.merlin.curriculum.domain.CurriculumManager

class CurriculumViewModelFactory(
    private val curriculumManager: CurriculumManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurriculumViewModel::class.java)) {
            return CurriculumViewModel(curriculumManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 