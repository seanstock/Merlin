package com.example.merlin.curriculum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.merlin.curriculum.service.SyllabusGeneratorService
import com.example.merlin.curriculum.service.CurriculumService

class CurriculumGeneratorViewModelFactory(
    private val syllabusGeneratorService: SyllabusGeneratorService,
    private val curriculumService: CurriculumService
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurriculumGeneratorViewModel::class.java)) {
            return CurriculumGeneratorViewModel(
                syllabusGeneratorService = syllabusGeneratorService,
                curriculumService = curriculumService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 