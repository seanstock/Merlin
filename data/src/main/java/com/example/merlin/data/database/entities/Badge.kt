package com.example.merlin.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for storing earned badges.
 * Represents badges that have been awarded to children.
 */
@Entity(
    tableName = "badges",
    foreignKeys = [
        ForeignKey(
            entity = ChildProfile::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["childId"]),
        Index(value = ["category"]),
        Index(value = ["earnedAt"]),
        Index(value = ["rarity"])
    ]
)
data class Badge(
    @PrimaryKey
    val id: String,                    // UUID string
    val childId: String,
    val name: String,
    val description: String,
    val earnedAt: Long,               // Timestamp
    val category: String,             // task_completion, streak, etc.
    val imageUrl: String,
    val rarity: String,               // common, uncommon, rare, epic, legendary
    val benefitsJson: String? = null  // JSON string of benefits map
) 