package com.example.merlin.curriculum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.curriculum.domain.CurriculumManager
import com.example.merlin.curriculum.model.CurriculumDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurriculumUiState(
    val curricula: List<CurriculumDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CurriculumViewModel(
    private val curriculumManager: CurriculumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurriculumUiState())
    val uiState: StateFlow<CurriculumUiState> = _uiState.asStateFlow()

    init {
        loadCurricula()
    }

    fun refreshCurricula() {
        loadCurricula()
    }

    private fun loadCurricula() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            curriculumManager.getAvailableCurricula()
                .onSuccess { curricula ->
                    _uiState.update {
                        it.copy(isLoading = false, curricula = curricula)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(isLoading = false, error = exception.message ?: "Failed to load curricula")
                    }
                }
        }
    }

    fun deleteCurriculum(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = curriculumManager.deleteCurriculum(id)
            if (result.isSuccess) {
                // Reloads the curricula, which will also set isLoading to false
                loadCurricula()
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to delete curriculum.")
                }
            }
        }
    }

    fun selectCurriculum(curriculumId: String) {
        // Placeholder for navigation or detail view
        // For now, just log the selection
        // In the future, this could navigate to lesson view or set active curriculum
        println("Selected curriculum: $curriculumId")
    }

    fun onNavigateToLessons(curriculumId: String) {
        // Handle navigation
    }
} 