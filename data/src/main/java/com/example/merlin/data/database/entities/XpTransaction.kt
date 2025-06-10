package com.example.merlin.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for storing XP transaction history.
 * Tracks all XP gains for analytics and history.
 */
@Entity(
    tableName = "xp_transactions",
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
        Index(value = ["source"]),
        Index(value = ["timestamp"]),
        Index(value = ["childId", "timestamp"])
    ]
)
data class XpTransaction(
    @PrimaryKey
    val id: String,                   // UUID string
    val childId: String,
    val amount: Int,
    val source: String,               // task_completion, first_try_bonus, etc.
    val description: String,
    val timestamp: Long,              // Timestamp
    val metadataJson: String? = null  // JSON string of metadata map
) 