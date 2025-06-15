package com.example.merlin.curriculum.service

import android.content.Context
import com.example.merlin.curriculum.model.*
import com.example.merlin.data.manager.AIServiceInterface
import com.example.merlin.data.remote.OpenAIClientWrapper
import com.example.merlin.data.manager.OpenAIServiceAdapter
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.TextContent
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Local implementation of SyllabusGeneratorService that uses OpenAI to convert
 * syllabi into structured curriculum lessons, adapting the prompt engineering from TaskMaster.
 * This service is LaaS-compliant for future externalization.
 */
class LocalSyllabusGeneratorService(
    private val context: Context
) : SyllabusGeneratorService {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val aiService: AIServiceInterface = OpenAIServiceAdapter(OpenAIClientWrapper())

    override suspend fun parseSyllabus(syllabusText: String, metadata: Map<String, String>): Result<SyllabusDto> {
        // This is a simplified local parser. A real implementation would use AI.
        return Result.success(SyllabusDto(
            title = metadata["title"] ?: "Parsed Syllabus",
            description = "Syllabus parsed from text.",
            category = metadata["category"] ?: "General",
            targetAgeRange = metadata["targetAgeRange"] ?: "8-10",
            difficulty = "intermediate",
            learningObjectives = listOf("Primary learning goals from syllabus text."),
            topics = listOf(SyllabusTopicDto(
                title = "Core Topic",
                description = syllabusText,
                learningObjectives = listOf("Master core concepts.")
            ))
        ))
    }

    override suspend fun generateCurriculum(
        syllabus: SyllabusDto,
        targetWeeks: Int,
        sessionsPerWeek: Int,
        sessionDuration: Int
    ): Result<CurriculumDto> = withContext(Dispatchers.IO) {
        try {
            val numLessons = targetWeeks * sessionsPerWeek
            val systemPrompt = buildSystemPromptForCurriculum(numLessons, syllabus)
            val userPrompt = buildUserPromptForCurriculum(syllabus, numLessons)

            val messages = listOf(
                ChatMessage.System(systemPrompt),
                ChatMessage.User(userPrompt) // Using the direct string constructor
            )

            // Force cache miss with unique memory context so each generation is fresh
            val aiResponse = aiService.getChatCompletionWithMemoryContext(
                messages,
                null,
                java.util.UUID.randomUUID().toString()
            )
            val responseContent = aiResponse?.content ?: throw Exception("AI service returned a null or empty response.")

            val curriculum = parseAIResponseToCurriculumDto(responseContent, syllabus)

            // Persist curriculum so UI list updates immediately
            val curService = com.example.merlin.config.ServiceLocator.getCurriculumService(context)
            curService.saveCurriculum(curriculum)

            // Pre-create lesson progress for active child
            val childId = com.example.merlin.utils.UserSessionRepository.getInstance(context).getActiveChildId()
            childId?.let { cid ->
                curriculum.lessons.forEach { lesson ->
                    curService.recordLessonProgress(
                        lessonId = lesson.id,
                        childId = cid,
                        progress = com.example.merlin.curriculum.model.LessonProgressDto(
                            lessonId = lesson.id,
                            childId = cid,
                            status = "not_started",
                            percentComplete = 0,
                            grade = 0,
                            tutorNotes = ""
                        )
                    )
                }
            }

            Result.success(curriculum)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPromptForCurriculum(numLessons: Int, syllabus: SyllabusDto): String {
        // Adapted from TaskMaster's proven prompting style
        return """
        You are an AI assistant specialized in analyzing educational syllabi and generating structured, pedagogically sound curriculum lessons in JSON format.

        Analyze the provided syllabus content and generate exactly $numLessons comprehensive lessons for a curriculum titled "${syllabus.title}".
        The target audience is in the ${syllabus.targetAgeRange} age range, for the subject of ${syllabus.category}.

        Respond ONLY with a valid JSON object matching the CurriculumDto structure. Do not include any explanation or markdown formatting.

        The JSON structure you must adhere to is:
        {
            "id": "unique_curriculum_id",
            "title": "curriculum_title",
            "description": "curriculum_description",
            "gradeLevel": "target_age_range_as_string",
            "subject": "curriculum_category",
            "lessons": [
                {
                    "id": "unique_lesson_id",
                    "title": "lesson_title",
                    "description": "lesson_description",
                    "objectives": ["objective1", "objective2"],
                    "activities": [
                        {
                            "id": "unique_activity_id",
                            "title": "activity_title",
                            "type": "exercise|quiz|reading|game",
                            "content": "activity_content_or_game_id",
                            "estimatedMinutes": 15
                        }
                    ],
                    "order": 1
                }
            ]
        }

        Guidelines:
        1. Create exactly $numLessons lessons, numbered sequentially in the 'order' field starting from 1.
        2. Ensure all IDs (curriculum, lesson, activity) are unique UUIDs.
        3. The 'gradeLevel' in the output JSON should match the syllabus's target age range.
        4. The 'subject' in the output JSON should match the syllabus's category.
        5. Base the curriculum's title and description on the provided syllabus.
        6. Order lessons logically, with foundational concepts first.
        7. Ensure activities are engaging and appropriate for the target age range.
        8. Adhere strictly to any specific topics or requirements mentioned in the syllabus content.
        """.trimIndent()
    }

    private fun buildUserPromptForCurriculum(syllabus: SyllabusDto, numLessons: Int): String {
        val syllabusContent = """
        Title: ${syllabus.title}
        Description: ${syllabus.description}
        Category: ${syllabus.category}
        Target Age Range: ${syllabus.targetAgeRange}
        Difficulty: ${syllabus.difficulty}
        Learning Objectives: ${syllabus.learningObjectives.joinToString()}
        Topics: ${syllabus.topics.joinToString { it.title + ": " + it.description }}
        """.trimIndent()

        return """
        Here is the educational syllabus to break down into exactly $numLessons lessons.

        $syllabusContent

        Requirements:
        - Generate exactly $numLessons comprehensive lessons.
        - Ensure content is appropriate for the ${syllabus.targetAgeRange} age range.
        - Populate all fields in the JSON structure as specified in the system prompt.
        - Generate unique UUIDs for all 'id' fields.
        """.trimIndent()
    }

    private fun parseAIResponseToCurriculumDto(raw: String, syllabus: SyllabusDto): CurriculumDto {
        // Clean possible markdown fencing or language tags
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```JSON")
                .removePrefix("```")
            if (cleaned.endsWith("```")) cleaned = cleaned.removeSuffix("```")
        }
        cleaned = cleaned.trim()

        return try {
            val tempDto = gson.fromJson(cleaned, CurriculumDto::class.java)
            tempDto.copy(id = UUID.randomUUID().toString())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse AI JSON. Cleaned response: $cleaned", e)
        }
    }

    // --- Other Interface Methods (Simplified Implementations) ---

    override suspend fun generateFromText(
        syllabusText: String,
        title: String,
        category: String,
        targetWeeks: Int,
        sessionsPerWeek: Int,
        sessionDuration: Int,
        progressCallback: ((GenerationProgressDto) -> Unit)?
    ): Result<CurriculumDto> = withContext(Dispatchers.IO) {
        progressCallback?.invoke(GenerationProgressDto(GenerationStage.PARSING_SYLLABUS, 10, "Parsing syllabus..."))
        val syllabusResult = parseSyllabus(syllabusText, mapOf("title" to title, "category" to category))

        if (syllabusResult.isSuccess) {
            progressCallback?.invoke(GenerationProgressDto(GenerationStage.GENERATING_STRUCTURE, 50, "Generating curriculum..."))
            val curriculumResult = generateCurriculum(syllabusResult.getOrThrow(), targetWeeks, sessionsPerWeek, sessionDuration)
            progressCallback?.invoke(GenerationProgressDto(GenerationStage.COMPLETE, 100, "Done."))
            curriculumResult
        } else {
            progressCallback?.invoke(GenerationProgressDto(GenerationStage.ERROR, 100, "Failed to parse syllabus."))
            Result.failure(syllabusResult.exceptionOrNull() ?: Exception("Unknown parsing error."))
        }
    }

    override suspend fun getAvailableTemplates(): Result<Map<String, List<String>>> {
        return Result.success(mapOf("Math" to listOf("Basic Algebra"), "Science" to listOf("Intro to Physics")))
    }

    override suspend fun generateFromTemplate(
        templateName: String,
        category: String,
        customizations: Map<String, Any>
    ): Result<CurriculumDto> {
        // Simplified: In a real app, this would load a template syllabus text
        val templateSyllabus = "Template for $templateName in $category"
        return generateFromText(templateSyllabus, templateName, category)
    }

    override suspend fun validateSyllabus(syllabusText: String): Result<List<String>> {
        val issues = mutableListOf<String>()
        if (syllabusText.length < 50) issues.add("Syllabus is too short.")
        return Result.success(issues)
    }
} 