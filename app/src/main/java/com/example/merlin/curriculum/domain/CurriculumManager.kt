package com.example.merlin.curriculum.domain

import com.example.merlin.curriculum.service.CurriculumService
import com.example.merlin.curriculum.model.*
import com.example.merlin.economy.service.EconomyService

/**
 * Service-agnostic curriculum manager.
 * Depends on CurriculumService interface for LaaS compliance.
 */
class CurriculumManager(
    private val curriculumService: CurriculumService,  // 🎯 Interface dependency
    private val economyService: EconomyService
) {
    
    suspend fun getCurrentLessonContext(curriculumId: String, childId: String): CurriculumContext? {
        // Platform-agnostic business logic
        val progress = curriculumService.getChildProgress(curriculumId, childId).getOrNull()
        return progress?.let { buildContext(it) }
    }
    
    suspend fun markLessonComplete(lessonId: String, childId: String, grade: Int, notes: String): Result<Unit> {
        val progress = LessonProgressDto(
            lessonId = lessonId,
            childId = childId,
            status = "completed",
            percentComplete = 100,
            grade = grade,
            tutorNotes = notes
        )
        
        // Award coins based on grade
        val coinReward = when {
            grade >= 90 -> 20
            grade >= 80 -> 15
            grade >= 70 -> 10
            grade >= 60 -> 5
            else -> 2
        }
        economyService.awardCoins(childId, coinReward, "lesson_completed", "Completed lesson: $lessonId")
        
        return curriculumService.recordLessonProgress(lessonId, childId, progress)
    }
    
    suspend fun updateTaskProgress(taskId: String, childId: String, status: String, percentComplete: Int, grade: Int, notes: String): Result<Unit> {
        val progress = TaskProgressDto(
            taskId = taskId,
            lessonId = "", // Get from context
            childId = childId,
            status = status,
            percentComplete = percentComplete,
            grade = grade,
            tutorNotes = notes
        )
        return curriculumService.recordTaskProgress(taskId, childId, progress)
    }
    
    suspend fun getTaskProgress(lessonId: String, childId: String): Result<List<TaskProgressDto>> {
        return curriculumService.getTaskProgress(lessonId, childId)
    }
    
    suspend fun getAvailableCurricula(): Result<List<CurriculumDto>> {
        return curriculumService.getAvailableCurricula()
    }
    
    suspend fun getCurriculumById(id: String): Result<CurriculumDto> {
        return curriculumService.getCurriculumById(id)
    }
    
    suspend fun getNextLesson(curriculumId: String, childId: String): Result<LessonDto?> {
        return curriculumService.getNextLesson(curriculumId, childId)
    }
    
    private fun buildContext(progress: CurriculumProgressDto): CurriculumContext {
        return CurriculumContext(
            curriculumId = progress.curriculumId,
            currentLessonId = progress.currentLessonId,
            overallProgress = progress.overallProgress
        )
    }
}

// Simple context data class
data class CurriculumContext(
    val curriculumId: String,
    val currentLessonId: String?,
    val overallProgress: Int
) 