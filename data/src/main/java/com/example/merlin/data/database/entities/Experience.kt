package com.example.merlin.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for storing child experience and level information.
 */
@Entity(
    tableName = "experiences",
    foreignKeys = [
        ForeignKey(
            entity = ChildProfile::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["childId"], unique = true),
        Index(value = ["level"]),
        Index(value = ["totalXpEarned"])
    ]
)
data class Experience(
    @PrimaryKey
    val childId: String,              // One record per child
    val level: Int,
    val currentXp: Int,               // XP within current level
    val totalXpEarned: Int,           // Total XP ever earned
    val lastUpdated: Long             // Timestamp
) 