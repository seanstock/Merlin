package com.example.merlin.curriculum.service

import com.example.merlin.curriculum.data.CurriculumRepository
import com.example.merlin.curriculum.data.toEntity
import com.example.merlin.curriculum.data.toDto
import com.example.merlin.curriculum.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local implementation of CurriculumService.
 * Handles local storage via Repository pattern.
 */
class LocalCurriculumService(
    private val repository: CurriculumRepository
) : CurriculumService {

    override suspend fun getAvailableCurricula(): Result<List<CurriculumDto>> {
        return try {
            val curricula = repository.getAllCurricula()
            if (curricula.isEmpty()) {
                // Preschool Curriculum (Ages 3-4) with task-based structure
                val preschoolCurriculum = createPreschoolCurriculum()
                
                // Save the default curriculum to database
                repository.saveCurriculum(preschoolCurriculum.toEntity())
                
                Result.success(listOf(preschoolCurriculum))
            } else {
                Result.success(curricula.map { it.toDto() })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createPreschoolCurriculum(): CurriculumDto {
        // Convert task-based structure to lessons using the same conversion logic as CurriculumDataLoader
        val tasksJsonArray = createPreschoolTasksJson()
        val lessons = convertTasksToLessons(tasksJsonArray)
        
        return CurriculumDto(
            id = "preschool_complete",
            title = "Preschool Complete Curriculum (Ages 3-4)",
            description = "Developmentally appropriate preschool curriculum focusing on social-emotional development, early literacy, basic math concepts, and creative expression",
            gradeLevel = "Preschool",
            subject = "Complete Curriculum",
            lessons = lessons
        )
    }

    private fun createPreschoolTasksJson(): JSONArray {
        val tasks = JSONArray()

        // Week 1: Getting to Know Each Other & Classroom Rules
        tasks.put(createTask(
            id = "ps_1_1",
            title = "Hello Friends - Names and Faces",
            description = "Learn classmates' names and practice greeting skills",
            week = 1,
            session = 1,
            objectives = listOf(
                "Say their own name clearly",
                "Recognize and say 2-3 classmates' names",
                "Practice appropriate greetings (hello, good morning)"
            ),
            activities = listOf(
                createActivity("circle_time", "Name Circle Game", 15),
                createActivity("interactive", "Photo Matching Activity", 10)
            ),
            assessment = createAssessment("observation", 3, "Observe name recognition and greeting behavior")
        ))

        tasks.put(createTask(
            id = "ps_1_2",
            title = "Our Classroom Rules",
            description = "Learn basic classroom expectations and safety rules",
            week = 1,
            session = 2,
            objectives = listOf(
                "Follow 2-3 simple classroom rules",
                "Show understanding of safe vs unsafe behaviors",
                "Practice raising hand before speaking"
            ),
            activities = listOf(
                createActivity("discussion", "Rule Making Circle", 15),
                createActivity("role_play", "Practice Following Rules", 15)
            ),
            assessment = createAssessment("observation", 3, "Observe rule-following behavior throughout the session"),
            prerequisites = listOf("ps_1_1")
        ))

        // Week 2: Colors and Shapes
        tasks.put(createTask(
            id = "ps_2_1", 
            title = "Primary Colors Exploration",
            description = "Identify and name red, blue, and yellow",
            week = 2,
            session = 1,
            objectives = listOf(
                "Identify red, blue, and yellow",
                "Sort objects by color",
                "Use color words in sentences"
            ),
            activities = listOf(
                createActivity("hands_on", "Color Sorting with Bears", 15),
                createActivity("creative", "Primary Color Painting", 20)
            ),
            assessment = createAssessment("interactive", 4, "Color identification game"),
            prerequisites = listOf("ps_1_2")
        ))

        tasks.put(createTask(
            id = "ps_2_2",
            title = "Circle, Square, Triangle Fun",
            description = "Learn basic shapes through play and exploration",
            week = 2,
            session = 2,
            objectives = listOf(
                "Identify circles, squares, and triangles",
                "Find shapes in the environment",
                "Create pictures using basic shapes"
            ),
            activities = listOf(
                createActivity("exploration", "Shape Hunt Around Classroom", 15),
                createActivity("creative", "Shape Collage Making", 20)
            ),
            assessment = createAssessment("hands_on", 4, "Shape identification with manipulatives"),
            prerequisites = listOf("ps_2_1")
        ))

        // Week 3: Numbers and Counting
        tasks.put(createTask(
            id = "ps_3_1",
            title = "Counting 1-5 with Objects",
            description = "Practice counting small sets of objects",
            week = 3,
            session = 1,
            objectives = listOf(
                "Count objects 1-5 accurately",
                "Match number symbols 1-5 to quantities",
                "Use one-to-one correspondence when counting"
            ),
            activities = listOf(
                createActivity("hands_on", "Counting Bears and Blocks", 15),
                createActivity("game", "Number and Quantity Matching", 15)
            ),
            assessment = createAssessment("interactive", 5, "Count and match quantities 1-5"),
            prerequisites = listOf("ps_2_2")
        ))

        tasks.put(createTask(
            id = "ps_3_2",
            title = "Big and Small Comparisons",
            description = "Compare sizes and understand relative concepts",
            week = 3,
            session = 2,
            objectives = listOf(
                "Identify big and small objects",
                "Compare two objects by size",
                "Use size vocabulary (big, small, bigger, smaller)"
            ),
            activities = listOf(
                createActivity("exploration", "Size Sorting Activity", 15),
                createActivity("creative", "Big and Small Art Project", 15)
            ),
            assessment = createAssessment("observation", 3, "Observe size comparison skills during play"),
            prerequisites = listOf("ps_3_1")
        ))

        // Week 4: Letters and Sounds
        tasks.put(createTask(
            id = "ps_4_1",
            title = "My Name Letters",
            description = "Recognize letters in their own name",
            week = 4,
            session = 1,
            objectives = listOf(
                "Recognize the first letter of their name",
                "Identify their name among other names",
                "Trace or copy letters from their name"
            ),
            activities = listOf(
                createActivity("writing", "Name Tracing Practice", 15),
                createActivity("game", "Find Your Name Game", 10)
            ),
            assessment = createAssessment("hands_on", 3, "Name letter recognition task"),
            prerequisites = listOf("ps_3_2")
        ))

        tasks.put(createTask(
            id = "ps_4_2",
            title = "Letter Sounds A-B-C",
            description = "Introduction to beginning letter sounds",
            week = 4,
            session = 2,
            objectives = listOf(
                "Recognize the sounds of A, B, and C",
                "Identify objects that start with these sounds",
                "Participate in rhyming games"
            ),
            activities = listOf(
                createActivity("interactive", "Letter Sound Games", 15),
                createActivity("exploration", "Sound Treasure Hunt", 15)
            ),
            assessment = createAssessment("interactive", 4, "Letter sound identification game"),
            prerequisites = listOf("ps_4_1")
        ))

        // Week 5: Social Skills and Emotions
        tasks.put(createTask(
            id = "ps_5_1",
            title = "Feelings and Faces",
            description = "Identify basic emotions and facial expressions",
            week = 5,
            session = 1,
            objectives = listOf(
                "Name happy, sad, mad, and scared feelings",
                "Recognize emotions in pictures and faces",
                "Express their own feelings with words"
            ),
            activities = listOf(
                createActivity("discussion", "Feelings Circle Time", 15),
                createActivity("creative", "Emotion Faces Art", 15)
            ),
            assessment = createAssessment("discussion", 4, "Emotion identification and expression"),
            prerequisites = listOf("ps_4_2")
        ))

        tasks.put(createTask(
            id = "ps_5_2",
            title = "Sharing and Taking Turns",
            description = "Practice important social skills",
            week = 5,
            session = 2,
            objectives = listOf(
                "Wait for their turn during activities",
                "Share toys and materials with friends",
                "Ask politely for a turn"
            ),
            activities = listOf(
                createActivity("game", "Turn-Taking Board Games", 20),
                createActivity("role_play", "Sharing Practice Scenarios", 15)
            ),
            assessment = createAssessment("observation", 4, "Observe sharing and turn-taking during free play"),
            prerequisites = listOf("ps_5_1")
        ))

        // Week 6: Patterns and Movement
        tasks.put(createTask(
            id = "ps_6_1",
            title = "Simple Patterns with Colors",
            description = "Create and continue simple AB patterns",
            week = 6,
            session = 1,
            objectives = listOf(
                "Recognize AB patterns (red-blue-red-blue)",
                "Continue a pattern that's already started",
                "Create their own simple pattern"
            ),
            activities = listOf(
                createActivity("hands_on", "Pattern Blocks Activity", 15),
                createActivity("creative", "Pattern Painting", 15)
            ),
            assessment = createAssessment("hands_on", 4, "Pattern completion and creation"),
            prerequisites = listOf("ps_5_2")
        ))

        tasks.put(createTask(
            id = "ps_6_2",
            title = "Moving Our Bodies",
            description = "Develop gross motor skills through movement",
            week = 6,
            session = 2,
            objectives = listOf(
                "Follow simple movement directions",
                "Balance on one foot for 3 seconds",
                "Jump, hop, and march with control"
            ),
            activities = listOf(
                createActivity("movement", "Follow the Leader Dance", 15),
                createActivity("game", "Movement Challenge Course", 20)
            ),
            assessment = createAssessment("observation", 4, "Observe gross motor skill development"),
            prerequisites = listOf("ps_6_1")
        ))

        return tasks
    }

    private fun createTask(
        id: String,
        title: String,
        description: String,
        week: Int,
        session: Int,
        objectives: List<String>,
        activities: List<JSONObject>,
        assessment: JSONObject,
        prerequisites: List<String> = emptyList()
    ): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("description", description)
            put("week", week)
            put("session", session)
            put("learningObjectives", JSONArray(objectives))
            put("prerequisites", JSONArray(prerequisites))
            put("estimatedDuration", 35) // 35 minutes per session for preschoolers
            put("activities", JSONArray(activities))
            put("assessment", assessment)
            put("status", "pending")
            put("progress", 0)
            put("completedAt", JSONObject.NULL)
        }
    }

    private fun createActivity(type: String, description: String, duration: Int): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("description", description)
            put("duration", duration)
        }
    }

    private fun createAssessment(type: String, questions: Int, description: String): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("questions", questions)
            put("description", description)
        }
    }

    private fun convertTasksToLessons(tasksArray: JSONArray): List<LessonDto> {
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
        
        return lessons
    }

    override suspend fun getCurriculumById(id: String): Result<CurriculumDto> {
        return try {
            val curriculum = repository.getCurriculumById(id)
            Result.success(curriculum.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCurriculum(curriculum: CurriculumDto): Result<Long> {
        return try {
            val curriculumId = repository.saveCurriculum(curriculum.toEntity())
            Result.success(curriculumId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCurriculum(id: String): Result<Unit> {
        return try {
            repository.deleteCurriculum(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChildProgress(curriculumId: String, childId: String): Result<CurriculumProgressDto> {
        return try {
            val progress = repository.getChildProgress(curriculumId, childId)
            Result.success(progress.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordLessonProgress(lessonId: String, childId: String, progress: LessonProgressDto): Result<Unit> {
        return try {
            repository.saveLessonProgress(progress.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordTaskProgress(taskId: String, childId: String, progress: TaskProgressDto): Result<Unit> {
        return try {
            repository.saveTaskProgress(progress.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNextLesson(curriculumId: String, childId: String): Result<LessonDto?> {
        return try {
            val nextLesson = repository.getNextLesson(curriculumId, childId)
            Result.success(nextLesson?.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaskProgress(lessonId: String, childId: String): Result<List<TaskProgressDto>> {
        return try {
            val taskProgress = repository.getTaskProgress(lessonId, childId)
            Result.success(taskProgress.map { it.toDto() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLessonProgress(lessonId: String, childId: String): Result<LessonProgressDto?> {
        return try {
            val lp = repository.getLessonProgress(lessonId, childId)
            Result.success(lp?.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 