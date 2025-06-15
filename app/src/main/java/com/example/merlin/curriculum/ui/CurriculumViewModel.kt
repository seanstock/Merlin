package com.example.merlin.curriculum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.curriculum.domain.CurriculumManager
import com.example.merlin.curriculum.model.CurriculumDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CurriculumViewModel(
    private val curriculumManager: CurriculumManager
) : ViewModel() {

    private val _curricula = MutableStateFlow<List<CurriculumDto>>(emptyList())
    val curricula: StateFlow<List<CurriculumDto>> = _curricula.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurricula()
    }

    fun loadCurricula() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            curriculumManager.getAvailableCurricula()
                .onSuccess { curricula ->
                    _curricula.value = curricula
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load curricula"
                }
            
            _isLoading.value = false
        }
    }

    fun selectCurriculum(curriculumId: String) {
        // For now, just log the selection
        // In the future, this could navigate to lesson view or set active curriculum
        println("Selected curriculum: $curriculumId")
    }
} 