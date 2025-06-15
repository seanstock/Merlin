package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.*

/**
 * Room DAO for curriculum data access.
 * Local implementation detail - not exposed to service layer.
 */
@Dao
interface CurriculumDao {
    
    // Curriculum operations
    @Query("SELECT * FROM curricula")
    suspend fun getAllCurricula(): List<CurriculumEntity>
    
    @Query("SELECT * FROM curricula WHERE id = :id")
    suspend fun getCurriculumById(id: String): CurriculumEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculum(curriculum: CurriculumEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurricula(curricula: List<CurriculumEntity>)
    
    @Query("DELETE FROM curricula WHERE id = :id")
    suspend fun deleteCurriculumById(id: String)
    
    // Curriculum progress operations
    @Query("SELECT * FROM curriculum_progress WHERE curriculumId = :curriculumId AND childId = :childId")
    suspend fun getCurriculumProgress(curriculumId: String, childId: String): CurriculumProgressEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculumProgress(progress: CurriculumProgressEntity)
    
    @Update
    suspend fun updateCurriculumProgress(progress: CurriculumProgressEntity)
    
    // Lesson progress operations
    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId AND childId = :childId")
    suspend fun getLessonProgress(lessonId: String, childId: String): LessonProgressEntity?
    
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId")
    suspend fun getAllLessonProgress(childId: String): List<LessonProgressEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonProgress(progress: LessonProgressEntity)
    
    @Update
    suspend fun updateLessonProgress(progress: LessonProgressEntity)
    
    // Task progress operations
    @Query("SELECT * FROM task_progress WHERE taskId = :taskId AND childId = :childId")
    suspend fun getTaskProgress(taskId: String, childId: String): TaskProgressEntity?
    
    @Query("SELECT * FROM task_progress WHERE lessonId = :lessonId AND childId = :childId")
    suspend fun getTaskProgressForLesson(lessonId: String, childId: String): List<TaskProgressEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskProgress(progress: TaskProgressEntity)
    
    @Update
    suspend fun updateTaskProgress(progress: TaskProgressEntity)
    
    // Helper queries for next lesson logic
    @Query("""
        SELECT * FROM curricula c 
        WHERE c.id = :curriculumId 
        AND NOT EXISTS (
            SELECT 1 FROM lesson_progress lp 
            WHERE lp.childId = :childId 
            AND lp.status = 'completed'
        )
        LIMIT 1
    """)
    suspend fun getNextLessonForCurriculum(curriculumId: String, childId: String): CurriculumEntity?
} 