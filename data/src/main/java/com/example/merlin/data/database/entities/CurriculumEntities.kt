package com.example.merlin.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entities for curriculum data storage.
 * Local implementation detail - not exposed to service layer.
 */

@Entity(tableName = "curricula")
data class CurriculumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val gradeLevel: String,
    val subject: String,
    val lessonsJson: String // JSON serialized lessons for simplicity
)

@Entity(tableName = "curriculum_progress")
data class CurriculumProgressEntity(
    @PrimaryKey val id: String, // "${curriculumId}_${childId}"
    val curriculumId: String,
    val childId: String,
    val currentLessonId: String?,
    val completedLessonsJson: String, // JSON serialized list
    val overallProgress: Int
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val id: String, // "${lessonId}_${childId}"
    val lessonId: String,
    val childId: String,
    val status: String,
    val percentComplete: Int,
    val grade: Int,
    val tutorNotes: String,
    val completedAt: Long? = null
)

@Entity(
    tableName = "task_progress",
    foreignKeys = [
        ForeignKey(
            entity = LessonProgressEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonProgressId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lessonProgressId"])]
)
data class TaskProgressEntity(
    @PrimaryKey val id: String, // "${taskId}_${childId}"
    val taskId: String,
    val lessonId: String,
    val childId: String,
    val lessonProgressId: String,
    val status: String,
    val percentComplete: Int,
    val grade: Int,
    val tutorNotes: String,
    val completedAt: Long? = null
) 