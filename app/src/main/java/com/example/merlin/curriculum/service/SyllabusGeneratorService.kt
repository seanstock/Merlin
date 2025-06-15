package com.example.merlin.curriculum.service

import com.example.merlin.curriculum.model.CurriculumDto
import com.example.merlin.curriculum.model.SyllabusDto
import com.example.merlin.curriculum.model.GenerationProgressDto

/**
 * LaaS-compliant service interface for automated syllabus to curriculum generation.
 * Designed for eventual externalization to remote AI services.
 */
interface SyllabusGeneratorService {
    /**
     * Parse a syllabus document and extract structured information
     */
    suspend fun parseSyllabus(syllabusText: String, metadata: Map<String, String> = emptyMap()): Result<SyllabusDto>
    
    /**
     * Generate a complete curriculum from a parsed syllabus
     */
    suspend fun generateCurriculum(
        syllabus: SyllabusDto,
        targetWeeks: Int = 16,
        sessionsPerWeek: Int = 3,
        sessionDuration: Int = 35
    ): Result<CurriculumDto>
    
    /**
     * Generate curriculum directly from syllabus text (combines parse + generate)
     */
    suspend fun generateFromText(
        syllabusText: String,
        title: String,
        category: String,
        targetWeeks: Int = 16,
        sessionsPerWeek: Int = 3,
        sessionDuration: Int = 35,
        progressCallback: ((GenerationProgressDto) -> Unit)? = null
    ): Result<CurriculumDto>
    
    /**
     * Get available curriculum templates for generation
     */
    suspend fun getAvailableTemplates(): Result<Map<String, List<String>>>
    
    /**
     * Generate curriculum from a predefined template
     */
    suspend fun generateFromTemplate(
        templateName: String,
        category: String,
        customizations: Map<String, Any> = emptyMap()
    ): Result<CurriculumDto>
    
    /**
     * Validate syllabus content before generation
     */
    suspend fun validateSyllabus(syllabusText: String): Result<List<String>>
} 