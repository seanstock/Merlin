package com.example.merlin.curriculum.service

import com.example.merlin.curriculum.model.CurriculumDto
import com.example.merlin.curriculum.model.CurriculumProgressDto
import com.example.merlin.curriculum.model.LessonDto
import com.example.merlin.curriculum.model.LessonProgressDto
import com.example.merlin.curriculum.model.TaskProgressDto

/**
 * LaaS-compliant service interface for curriculum operations.
 * Designed for eventual externalization to remote services.
 */
interface CurriculumService {
    suspend fun getAvailableCurricula(): Result<List<CurriculumDto>>
    suspend fun getCurriculumById(id: String): Result<CurriculumDto>
    suspend fun saveCurriculum(curriculum: CurriculumDto): Result<Long>
    suspend fun deleteCurriculum(id: String): Result<Unit>
    suspend fun getChildProgress(curriculumId: String, childId: String): Result<CurriculumProgressDto>
    suspend fun recordLessonProgress(lessonId: String, childId: String, progress: LessonProgressDto): Result<Unit>
    suspend fun recordTaskProgress(taskId: String, childId: String, progress: TaskProgressDto): Result<Unit>
    suspend fun getLessonProgress(lessonId: String, childId: String): Result<LessonProgressDto?>
    suspend fun getNextLesson(curriculumId: String, childId: String): Result<LessonDto?>
    suspend fun getTaskProgress(lessonId: String, childId: String): Result<List<TaskProgressDto>>
} 