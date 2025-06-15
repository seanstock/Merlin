package com.example.merlin.curriculum.model

/**
 * Data transfer object for parsed syllabus information
 */
data class SyllabusDto(
    val title: String,
    val description: String,
    val category: String,
    val targetAgeRange: String,
    val difficulty: String,
    val learningObjectives: List<String>,
    val topics: List<SyllabusTopicDto>,
    val estimatedDuration: String? = null,
    val prerequisites: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Individual topic within a syllabus
 */
data class SyllabusTopicDto(
    val title: String,
    val description: String,
    val subtopics: List<String> = emptyList(),
    val estimatedWeeks: Int? = null,
    val learningObjectives: List<String> = emptyList()
)

/**
 * Progress tracking for curriculum generation
 */
data class GenerationProgressDto(
    val stage: GenerationStage,
    val progress: Int, // 0-100
    val message: String,
    val currentTask: String? = null,
    val estimatedTimeRemaining: Long? = null // milliseconds
)

/**
 * Stages of curriculum generation process
 */
enum class GenerationStage {
    PARSING_SYLLABUS,
    EXTRACTING_TOPICS,
    GENERATING_STRUCTURE,
    CREATING_LESSONS,
    GENERATING_ACTIVITIES,
    CREATING_ASSESSMENTS,
    FINALIZING,
    COMPLETE,
    ERROR
} 