package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "subject_mastery",
    primaryKeys = ["child_id", "subject"]
)
data class SubjectMastery(
    @ColumnInfo(name = "child_id")
    val childId: String,

    val subject: String,

    @ColumnInfo(name = "grade_estimate")
    val gradeEstimate: Double?,

    @ColumnInfo(name = "adaptive_difficulty")
    val adaptiveDifficulty: Double?,

    @ColumnInfo(name = "updated_ts")
    val updatedTs: Long?
) 