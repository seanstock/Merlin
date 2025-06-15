package com.example.merlin.curriculum.data

import android.content.Context
import com.example.merlin.curriculum.model.*
import com.example.merlin.data.database.entities.CurriculumEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads curriculum data from assets into the database.
 * Handles the conversion from JSON format to our DTO/Entity structure.
 */
class CurriculumDataLoader(
    private val context: Context,
    private val repository: CurriculumRepository
) {
    
    suspend fun loadCurriculaFromAssets() = withContext(Dispatchers.IO) {
        try {
            // Check if curricula are already loaded
            val existingCurricula = repository.getAllCurricula()
            if (existingCurricula.isNotEmpty()) {
                return@withContext // Already loaded
            }
            
            val curriculaToLoad = mutableListOf<CurriculumEntity>()
            
            // Load each curriculum file
            val curriculumFiles = listOf(
                "sample_math.json" to "Mathematics",
                "sample_science.json" to "Science", 
                "sample_language.json" to "Language Arts"
            )
            
            curriculumFiles.forEach { (filename, subject) ->
                try {
                    val jsonString = context.assets.open("curricula/$filename").bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val curriculum = convertToCurriculumEntity(item, subject)
                        curriculaToLoad.add(curriculum)
                    }
                } catch (e: Exception) {
                    println("Error loading curriculum file $filename: ${e.message}")
                }
            }
            
            // Insert all curricula
            if (curriculaToLoad.isNotEmpty()) {
                repository.insertCurricula(curriculaToLoad)
                println("Loaded ${curriculaToLoad.size} curricula into database")
            }
            
        } catch (e: Exception) {
            println("Error loading curricula from assets: ${e.message}")
        }
    }
    
    private fun convertToCurriculumEntity(item: JSONObject, subject: String): CurriculumEntity {
        val id = item.getInt("id")
        val title = item.getString("title")
        val description = item.getString("description")
        val tasksArray = item.getJSONArray("tasks")
        
        // Convert tasks to lessons (simplified mapping)
        val lessons = mutableListOf<LessonDto>()
        for (i in 0 until tasksArray.length()) {
            val task = tasksArray.getJSONObject(i)
            val activitiesArray = task.getJSONArray("activities")
            val objectivesArray = task.getJSONArray("learningObjectives")
            
            val activities = mutableListOf<ActivityDto>()
            for (j in 0 until activitiesArray.length()) {
                val activity = activitiesArray.getJSONObject(j)
                activities.add(
                    ActivityDto(
                        id = "${task.getString("id")}_activity_$j",
                        title = activity.getString("type").replaceFirstChar { it.uppercase() },
                        type = activity.getString("type"),
                        content = activity.getString("description"),
                        estimatedMinutes = activity.getInt("duration")
                    )
                )
            }
            
            val objectives = mutableListOf<String>()
            for (j in 0 until objectivesArray.length()) {
                objectives.add(objectivesArray.getString(j))
            }
            
            lessons.add(
                LessonDto(
                    id = task.getString("id"),
                    title = task.getString("title"),
                    description = task.getString("description"),
                    objectives = objectives,
                    activities = activities,
                    order = i + 1
                )
            )
        }
        
        return CurriculumEntity(
            id = "curriculum_$id",
            title = title,
            description = description,
            gradeLevel = extractGradeLevel(title).toString(),
            subject = subject,
            lessonsJson = serializeLessons(lessons)
        )
    }
    
    private fun extractGradeLevel(title: String): Int {
        // Extract grade level from title like "Basic Addition (Grade 1)"
        val gradeRegex = "Grade (\\d+)".toRegex()
        val match = gradeRegex.find(title)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }
    
    private fun serializeLessons(lessons: List<LessonDto>): String {
        return try {
            // Simple JSON serialization for lessons
            val jsonArray = JSONArray()
            lessons.forEach { lesson ->
                val lessonObj = JSONObject().apply {
                    put("id", lesson.id)
                    put("title", lesson.title)
                    put("description", lesson.description)
                    put("order", lesson.order)
                    put("objectives", JSONArray(lesson.objectives))
                    
                    val activitiesArray = JSONArray()
                    lesson.activities.forEach { activity ->
                        val activityObj = JSONObject().apply {
                            put("id", activity.id)
                            put("title", activity.title)
                            put("type", activity.type)
                            put("content", activity.content)
                            put("estimatedMinutes", activity.estimatedMinutes)
                        }
                        activitiesArray.put(activityObj)
                    }
                    put("activities", activitiesArray)
                }
                jsonArray.put(lessonObj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }
} 