package com.example.merlin.curriculum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.curriculum.model.GenerationProgressDto
import com.example.merlin.curriculum.model.GenerationStage
import com.example.merlin.curriculum.service.SyllabusGeneratorService
import com.example.merlin.curriculum.service.CurriculumService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CurriculumGeneratorUiState(
    val isGenerating: Boolean = false,
    val generationProgress: GenerationProgressDto = GenerationProgressDto(
        stage = GenerationStage.PARSING_SYLLABUS,
        progress = 0,
        message = "Ready to generate"
    ),
    val generatedCurriculumId: String? = null,
    val error: String? = null
)

class CurriculumGeneratorViewModel(
    private val syllabusGeneratorService: SyllabusGeneratorService,
    private val curriculumService: CurriculumService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurriculumGeneratorUiState())
    val uiState: StateFlow<CurriculumGeneratorUiState> = _uiState.asStateFlow()

    private val _availableTemplates = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val availableTemplates: StateFlow<Map<String, List<String>>> = _availableTemplates.asStateFlow()

    fun loadAvailableTemplates() {
        viewModelScope.launch {
            syllabusGeneratorService.getAvailableTemplates()
                .onSuccess { templates ->
                    _availableTemplates.value = templates
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load templates: ${error.message}"
                    )
                }
        }
    }

    fun generateCurriculum(
        syllabusText: String,
        title: String,
        category: String,
        targetWeeks: Int = 16,
        sessionsPerWeek: Int = 3,
        sessionDuration: Int = 35
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generatedCurriculumId = null
            )

            syllabusGeneratorService.generateFromText(
                syllabusText = syllabusText,
                title = title,
                category = category,
                targetWeeks = targetWeeks,
                sessionsPerWeek = sessionsPerWeek,
                sessionDuration = sessionDuration,
                progressCallback = { progress ->
                    _uiState.value = _uiState.value.copy(
                        generationProgress = progress
                    )
                }
            ).onSuccess { curriculum ->
                // Save the generated curriculum to database
                curriculumService.saveCurriculum(curriculum)
                    .onSuccess { savedCurriculumId ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            generatedCurriculumId = savedCurriculumId.toString(),
                            generationProgress = GenerationProgressDto(
                                stage = GenerationStage.COMPLETE,
                                progress = 100,
                                message = "Curriculum generated and saved successfully!"
                            )
                        )
                    }
                    .onFailure { saveError ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            error = "Generation succeeded but save failed: ${saveError.message}",
                            generationProgress = GenerationProgressDto(
                                stage = GenerationStage.ERROR,
                                progress = 0,
                                message = "Save failed"
                            )
                        )
                    }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Generation failed: ${error.message}",
                    generationProgress = GenerationProgressDto(
                        stage = GenerationStage.ERROR,
                        progress = 0,
                        message = "Generation failed"
                    )
                )
            }
        }
    }

    fun generateFromTemplate(
        templateName: String,
        category: String,
        customizations: Map<String, Any> = emptyMap()
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generatedCurriculumId = null,
                generationProgress = GenerationProgressDto(
                    stage = GenerationStage.GENERATING_STRUCTURE,
                    progress = 20,
                    message = "Generating from template: $templateName"
                )
            )

            syllabusGeneratorService.generateFromTemplate(
                templateName = templateName,
                category = category,
                customizations = customizations
            ).onSuccess { curriculum ->
                // Save the generated curriculum to database
                curriculumService.saveCurriculum(curriculum)
                    .onSuccess { savedCurriculumId ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            generatedCurriculumId = savedCurriculumId.toString(),
                            generationProgress = GenerationProgressDto(
                                stage = GenerationStage.COMPLETE,
                                progress = 100,
                                message = "Template curriculum generated and saved successfully!"
                            )
                        )
                    }
                    .onFailure { saveError ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            error = "Template generation succeeded but save failed: ${saveError.message}",
                            generationProgress = GenerationProgressDto(
                                stage = GenerationStage.ERROR,
                                progress = 0,
                                message = "Save failed"
                            )
                        )
                    }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Template generation failed: ${error.message}",
                    generationProgress = GenerationProgressDto(
                        stage = GenerationStage.ERROR,
                        progress = 0,
                        message = "Template generation failed"
                    )
                )
            }
        }
    }

    fun validateSyllabusText(syllabusText: String): List<String> {
        val issues = mutableListOf<String>()
        
        if (syllabusText.length < 100) {
            issues.add("Syllabus text is too short (minimum 100 characters)")
        }
        
        if (!syllabusText.contains("objective", ignoreCase = true) && 
            !syllabusText.contains("goal", ignoreCase = true) &&
            !syllabusText.contains("learn", ignoreCase = true)) {
            issues.add("No clear learning objectives found")
        }
        
        if (syllabusText.split("\\s+".toRegex()).size < 50) {
            issues.add("Syllabus appears to lack sufficient detail")
        }
        
        return issues
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearGeneratedCurriculum() {
        _uiState.value = _uiState.value.copy(generatedCurriculumId = null)
    }
} 