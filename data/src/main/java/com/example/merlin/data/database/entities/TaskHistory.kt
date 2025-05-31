package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_history")
data class TaskHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Default value needed for autoGenerate with Long

    @ColumnInfo(name = "child_id")
    val childId: String?,

    val ts: Long?,

    @ColumnInfo(name = "prompt_id")
    val promptId: String?,

    @ColumnInfo(name = "task_type")
    val taskType: String?,

    @ColumnInfo(name = "task_text")
    val taskText: String?,

    val correct: Int? // 0 for false, 1 for true
) 