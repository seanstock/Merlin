package com.example.merlin.curriculum.data

import com.example.merlin.curriculum.model.LessonProgressDto
import com.example.merlin.curriculum.model.TaskProgressDto
import com.example.merlin.data.database.entities.LessonProgressEntity
import com.example.merlin.data.database.entities.TaskProgressEntity

// DTO -> Entity
fun LessonProgressDto.toEntity(): LessonProgressEntity = LessonProgressEntity(
    id = "${lessonId}_${childId}",
    lessonId = lessonId,
    childId = childId,
    status = status,
    percentComplete = percentComplete,
    grade = grade,
    tutorNotes = tutorNotes
)

fun TaskProgressDto.toEntity(): TaskProgressEntity = TaskProgressEntity(
    id = "${taskId}_${childId}",
    taskId = taskId,
    lessonId = lessonId,
    childId = childId,
    lessonProgressId = "${lessonId}_${childId}",
    status = status,
    percentComplete = percentComplete,
    grade = grade,
    tutorNotes = tutorNotes
) 