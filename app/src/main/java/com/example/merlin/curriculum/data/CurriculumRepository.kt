package com.example.merlin.curriculum.data

import com.example.merlin.curriculum.model.*
import com.example.merlin.data.database.dao.CurriculumDao
import com.example.merlin.data.database.entities.*

/**
 * Repository for curriculum data access using Room database.
 * Local implementation detail - not exposed to service layer.
 */
class CurriculumRepository(
    private val curriculumDao: CurriculumDao
) {
    
    suspend fun getAllCurricula(): List<CurriculumEntity> {
        return curriculumDao.getAllCurricula()
    }
    
    suspend fun getCurriculumById(id: String): CurriculumEntity {
        return curriculumDao.getCurriculumById(id) 
            ?: throw NoSuchElementException("Curriculum not found: $id")
    }
    
    suspend fun getChildProgress(curriculumId: String, childId: String): CurriculumProgressEntity {
        return curriculumDao.getCurriculumProgress(curriculumId, childId)
            ?: throw NoSuchElementException("Progress not found for curriculum: $curriculumId, child: $childId")
    }
    
    suspend fun saveLessonProgress(progress: LessonProgressEntity) {
        curriculumDao.insertLessonProgress(progress)
    }
    
    suspend fun saveTaskProgress(progress: TaskProgressEntity) {
        curriculumDao.insertTaskProgress(progress)
    }
    
    suspend fun getNextLesson(curriculumId: String, childId: String): LessonEntity? {
        // Simplified implementation - returns null for now
        // TODO: Implement proper next lesson logic based on curriculum structure
        return null
    }
    
    suspend fun getTaskProgress(lessonId: String, childId: String): List<TaskProgressEntity> {
        return curriculumDao.getTaskProgressForLesson(lessonId, childId)
    }
    
    suspend fun getLessonProgress(lessonId: String, childId: String): LessonProgressEntity? {
        return curriculumDao.getLessonProgress(lessonId, childId)
    }
    
    // Helper methods for curriculum initialization
    suspend fun insertCurriculum(curriculum: CurriculumEntity) {
        curriculumDao.insertCurriculum(curriculum)
    }
    
    suspend fun insertCurricula(curricula: List<CurriculumEntity>) {
        curriculumDao.insertCurricula(curricula)
    }
    
    suspend fun initializeCurriculumProgress(curriculumId: String, childId: String) {
        val progress = CurriculumProgressEntity(
            id = "${curriculumId}_${childId}",
            curriculumId = curriculumId,
            childId = childId,
            currentLessonId = null,
            completedLessonsJson = "[]",
            overallProgress = 0
        )
        curriculumDao.insertCurriculumProgress(progress)
    }
    
    suspend fun saveCurriculum(curriculum: CurriculumEntity): Long {
        return curriculumDao.insertCurriculum(curriculum)
    }
}

// Stub entities for compatibility - these will be replaced by proper Room entities
data class LessonEntity(val id: String) {
    fun toDto(): LessonDto = LessonDto("", "", "", emptyList(), emptyList(), 0)
}

// DTO to Entity conversion methods
fun CurriculumEntity.toDto(): CurriculumDto {
    val lessons = try {
        val jsonArray = org.json.JSONArray(lessonsJson)
        val lessonsList = mutableListOf<LessonDto>()
        
        for (i in 0 until jsonArray.length()) {
            val lessonObj = jsonArray.getJSONObject(i)
            val objectivesArray = lessonObj.getJSONArray("objectives")
            val activitiesArray = lessonObj.getJSONArray("activities")
            
            val objectives = mutableListOf<String>()
            for (j in 0 until objectivesArray.length()) {
                objectives.add(objectivesArray.getString(j))
            }
            
            val activities = mutableListOf<ActivityDto>()
            for (j in 0 until activitiesArray.length()) {
                val activityObj = activitiesArray.getJSONObject(j)
                activities.add(
                    ActivityDto(
                        id = activityObj.getString("id"),
                        title = activityObj.getString("title"),
                        type = activityObj.getString("type"),
                        content = activityObj.getString("content"),
                        estimatedMinutes = activityObj.getInt("estimatedMinutes")
                    )
                )
            }
            
            lessonsList.add(
                LessonDto(
                    id = lessonObj.getString("id"),
                    title = lessonObj.getString("title"),
                    description = lessonObj.getString("description"),
                    objectives = objectives,
                    activities = activities,
                    order = lessonObj.getInt("order")
                )
            )
        }
        lessonsList
    } catch (e: Exception) {
        emptyList()
    }
    
    return CurriculumDto(id, title, description, gradeLevel, subject, lessons)
}

fun CurriculumProgressEntity.toDto(): CurriculumProgressDto {
    // TODO: Parse completedLessonsJson to List<String>
    return CurriculumProgressDto(curriculumId, childId, currentLessonId, emptyList(), overallProgress)
}

fun LessonProgressEntity.toDto(): LessonProgressDto {
    return LessonProgressDto(lessonId, childId, status, percentComplete, grade, tutorNotes)
}

fun TaskProgressEntity.toDto(): TaskProgressDto {
    return TaskProgressDto(taskId, lessonId, childId, status, percentComplete, grade, tutorNotes)
}

// Entity to DTO conversion methods
fun CurriculumProgressDto.toEntity(): CurriculumProgressEntity {
    return CurriculumProgressEntity(
        id = "${curriculumId}_${childId}",
        curriculumId = curriculumId,
        childId = childId,
        currentLessonId = currentLessonId,
        completedLessonsJson = "", // TODO: Serialize completedLessons
        overallProgress = overallProgress
    )
}

fun CurriculumDto.toEntity(): CurriculumEntity {
    // Convert lessons to JSON string
    val lessonsJson = try {
        val jsonArray = org.json.JSONArray()
        lessons.forEach { lesson ->
            val lessonObj = org.json.JSONObject()
            lessonObj.put("id", lesson.id)
            lessonObj.put("title", lesson.title)
            lessonObj.put("description", lesson.description)
            lessonObj.put("order", lesson.order)
            
            val objectivesArray = org.json.JSONArray()
            lesson.objectives.forEach { objective ->
                objectivesArray.put(objective)
            }
            lessonObj.put("objectives", objectivesArray)
            
            val activitiesArray = org.json.JSONArray()
            lesson.activities.forEach { activity ->
                val activityObj = org.json.JSONObject()
                activityObj.put("id", activity.id)
                activityObj.put("title", activity.title)
                activityObj.put("type", activity.type)
                activityObj.put("content", activity.content)
                activityObj.put("estimatedMinutes", activity.estimatedMinutes)
                activitiesArray.put(activityObj)
            }
            lessonObj.put("activities", activitiesArray)
            
            jsonArray.put(lessonObj)
        }
        jsonArray.toString()
    } catch (e: Exception) {
        "[]"
    }
    
    return CurriculumEntity(
        id = id,
        title = title,
        description = description,
        gradeLevel = gradeLevel,
        subject = subject,
        lessonsJson = lessonsJson
    )
} 