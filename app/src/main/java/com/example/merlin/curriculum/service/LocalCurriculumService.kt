package com.example.merlin.curriculum.service

import com.example.merlin.curriculum.data.CurriculumRepository
import com.example.merlin.curriculum.data.toEntity
import com.example.merlin.curriculum.data.toDto
import com.example.merlin.curriculum.model.*

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
                // First Grade Curriculum based on provided content
                val firstGradeCurriculum = CurriculumDto(
                    id = "first_grade_complete",
                    title = "First Grade Complete Curriculum",
                    description = "Comprehensive first grade curriculum covering Language Arts, Mathematics, Social Studies, and Science",
                    gradeLevel = "1",
                    subject = "Complete Curriculum",
                    lessons = listOf(
                        // Language Arts Lessons
                        LessonDto(
                            id = "la_phonics",
                            title = "Phonics and Word Recognition",
                            description = "Apply knowledge of letter-sound correspondence and recognize consonants and vowels",
                            objectives = listOf(
                                "Apply knowledge of letter-sound correspondence (phonics) when reading",
                                "Recognize consonants and vowels including long and short vowel sounds",
                                "Blend sounds from words",
                                "Identify and change beginning, middle, and ending sounds"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "la_phonics_1",
                                    title = "Letter Sound Practice",
                                    type = "interactive",
                                    content = "Practice matching letters to their sounds using interactive games",
                                    estimatedMinutes = 20
                                ),
                                ActivityDto(
                                    id = "la_phonics_2", 
                                    title = "Vowel Recognition",
                                    type = "exercise",
                                    content = "Identify long and short vowel sounds in simple words",
                                    estimatedMinutes = 15
                                )
                            ),
                            order = 1
                        ),
                        LessonDto(
                            id = "la_reading",
                            title = "Reading Comprehension",
                            description = "Identify story elements and demonstrate understanding through retelling",
                            objectives = listOf(
                                "Identify and describe story elements of plot, setting, and characters",
                                "Recognize fiction and non-fiction",
                                "Retell stories showing beginning, middle, and end",
                                "Utilize illustrations to assist in story comprehension"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "la_reading_1",
                                    title = "Story Elements",
                                    type = "reading",
                                    content = "Read a short story and identify the main characters, setting, and plot",
                                    estimatedMinutes = 25
                                ),
                                ActivityDto(
                                    id = "la_reading_2",
                                    title = "Story Retelling",
                                    type = "interactive",
                                    content = "Practice retelling stories with beginning, middle, and end",
                                    estimatedMinutes = 20
                                )
                            ),
                            order = 2
                        ),
                        LessonDto(
                            id = "la_writing",
                            title = "Writing and Communication",
                            description = "Write simple sentences and organize ideas with proper punctuation",
                            objectives = listOf(
                                "Independently write a simple sentence",
                                "Stay focused on one topic",
                                "Use end of sentence punctuation",
                                "Capitalize the beginning of sentences and proper nouns"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "la_writing_1",
                                    title = "Sentence Building",
                                    type = "exercise",
                                    content = "Practice writing complete sentences with proper capitalization and punctuation",
                                    estimatedMinutes = 20
                                ),
                                ActivityDto(
                                    id = "la_writing_2",
                                    title = "Topic Writing",
                                    type = "creative",
                                    content = "Write 2-3 sentences about a chosen topic with descriptive words",
                                    estimatedMinutes = 25
                                )
                            ),
                            order = 3
                        ),
                        // Mathematics Lessons
                        LessonDto(
                            id = "math_numbers",
                            title = "Number Sense and Counting",
                            description = "Understand numbers 1-100 and base-ten number system",
                            objectives = listOf(
                                "Count, read, write, represent, and order numbers 1 to 100",
                                "Compare numbers using greater than, less than symbols",
                                "Count and group objects into tens and ones",
                                "Sequence numbers: before, after, between"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "math_numbers_1",
                                    title = "Counting to 100",
                                    type = "interactive",
                                    content = "Practice counting objects and writing numbers 1-100",
                                    estimatedMinutes = 20
                                ),
                                ActivityDto(
                                    id = "math_numbers_2",
                                    title = "Tens and Ones",
                                    type = "exercise",
                                    content = "Group objects into tens and ones using manipulatives",
                                    estimatedMinutes = 25
                                )
                            ),
                            order = 4
                        ),
                        LessonDto(
                            id = "math_operations",
                            title = "Addition and Subtraction",
                            description = "Demonstrate meaning of addition and subtraction to solve problems",
                            objectives = listOf(
                                "Demonstrate the meaning of addition and subtraction",
                                "Use addition and subtraction to solve problems to 10",
                                "Solve simple + and - number sentences using symbols",
                                "Solve simple + and - word problems"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "math_operations_1",
                                    title = "Addition Practice",
                                    type = "exercise",
                                    content = "Solve addition problems using concrete objects and number lines",
                                    estimatedMinutes = 20
                                ),
                                ActivityDto(
                                    id = "math_operations_2",
                                    title = "Word Problems",
                                    type = "problem_solving",
                                    content = "Solve simple addition and subtraction word problems",
                                    estimatedMinutes = 25
                                )
                            ),
                            order = 5
                        ),
                        LessonDto(
                            id = "math_geometry",
                            title = "Shapes and Measurement",
                            description = "Sort shapes and measure using non-standard units",
                            objectives = listOf(
                                "Sort and classify two and three dimensional shapes",
                                "Use positional words: in front of, behind, right, left, above, below",
                                "Use non-standard units to measure length",
                                "Tell time to the nearest half hour"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "math_geometry_1",
                                    title = "Shape Sorting",
                                    type = "interactive",
                                    content = "Sort and classify 2D and 3D shapes by attributes",
                                    estimatedMinutes = 20
                                ),
                                ActivityDto(
                                    id = "math_geometry_2",
                                    title = "Measurement Fun",
                                    type = "hands_on",
                                    content = "Measure classroom objects using paper clips and blocks",
                                    estimatedMinutes = 25
                                )
                            ),
                            order = 6
                        ),
                        // Science Lessons
                        LessonDto(
                            id = "science_life",
                            title = "Life Cycles and Human Body",
                            description = "Explore life cycles and learn about the human body",
                            objectives = listOf(
                                "Understand basic life cycles",
                                "Identify parts of the human body",
                                "Observe and describe living things"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "science_life_1",
                                    title = "Plant Life Cycle",
                                    type = "observation",
                                    content = "Observe and draw the stages of a plant's life cycle",
                                    estimatedMinutes = 30
                                ),
                                ActivityDto(
                                    id = "science_life_2",
                                    title = "My Body",
                                    type = "interactive",
                                    content = "Learn about body parts and their functions",
                                    estimatedMinutes = 20
                                )
                            ),
                            order = 7
                        ),
                        LessonDto(
                            id = "science_earth",
                            title = "Earth Features and Solar System",
                            description = "Introduction to Earth features and the solar system",
                            objectives = listOf(
                                "Identify basic Earth features",
                                "Learn about motion and movement",
                                "Introduction to the solar system"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "science_earth_1",
                                    title = "Earth Around Us",
                                    type = "exploration",
                                    content = "Explore different Earth features like mountains, rivers, and forests",
                                    estimatedMinutes = 25
                                ),
                                ActivityDto(
                                    id = "science_earth_2",
                                    title = "Sun, Moon, and Stars",
                                    type = "observation",
                                    content = "Learn about the sun, moon, and stars in our solar system",
                                    estimatedMinutes = 20
                                )
                            ),
                            order = 8
                        ),
                        // Social Studies Lesson
                        LessonDto(
                            id = "social_community",
                            title = "My Community",
                            description = "Learn about community helpers and important places",
                            objectives = listOf(
                                "Identify people and their jobs in the community",
                                "Recognize people who help us",
                                "Learn basic mapping skills and directions",
                                "Map the community"
                            ),
                            activities = listOf(
                                ActivityDto(
                                    id = "social_community_1",
                                    title = "Community Helpers",
                                    type = "interactive",
                                    content = "Learn about police officers, firefighters, teachers, and other helpers",
                                    estimatedMinutes = 25
                                ),
                                ActivityDto(
                                    id = "social_community_2",
                                    title = "Community Map",
                                    type = "creative",
                                    content = "Create a simple map of important places in our community",
                                    estimatedMinutes = 30
                                )
                            ),
                            order = 9
                        )
                    )
                )
                Result.success(listOf(firstGradeCurriculum))
            } else {
                Result.success(curricula.map { it.toDto() })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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