package com.example.merlin.curriculum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.curriculum.model.CurriculumDto
import com.example.merlin.curriculum.domain.CurriculumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LessonListUiState(
    val curriculum: CurriculumDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class LessonListViewModel(
    private val curriculumId: String,
    private val curriculumManager: CurriculumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonListUiState())
    val uiState: StateFlow<LessonListUiState> = _uiState.asStateFlow()

    init {
        loadCurriculum()
    }

    private fun loadCurriculum() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            curriculumManager.getCurriculumById(curriculumId)
                .onSuccess { curriculum ->
                    _uiState.value = LessonListUiState(curriculum = curriculum, isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = LessonListUiState(error = "Failed to load curriculum: ${error.message}", isLoading = false)
                }
        }
    }
} 