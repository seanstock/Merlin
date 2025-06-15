package com.example.merlin.curriculum.model

/**
 * Data Transfer Objects for curriculum service boundaries.
 * Platform-agnostic and JSON-serializable for LaaS compatibility.
 */

data class CurriculumDto(
    val id: String,
    val title: String,
    val description: String,
    val gradeLevel: String,
    val subject: String,
    val lessons: List<LessonDto>
)

data class LessonDto(
    val id: String,
    val title: String,
    val description: String,
    val objectives: List<String>,
    val activities: List<ActivityDto>,
    val order: Int
)

data class ActivityDto(
    val id: String,
    val title: String,
    val type: String, // "exercise", "quiz", "reading", etc.
    val content: String,
    val estimatedMinutes: Int
)

data class CurriculumProgressDto(
    val curriculumId: String,
    val childId: String,
    val currentLessonId: String?,
    val completedLessons: List<String>,
    val overallProgress: Int // 0-100
)

// Simple Task Tracking DTOs
data class TaskProgressDto(
    val taskId: String,
    val lessonId: String,
    val childId: String,
    val status: String,           // "not_started", "in_progress", "completed"
    val percentComplete: Int,     // 0-100
    val grade: Int,              // 0-100
    val tutorNotes: String       // AI-generated or manual notes
)

data class LessonProgressDto(
    val lessonId: String,
    val childId: String,
    val status: String,           // "not_started", "in_progress", "completed"
    val percentComplete: Int,     // 0-100
    val grade: Int,              // 0-100  
    val tutorNotes: String       // Overall lesson notes
) 