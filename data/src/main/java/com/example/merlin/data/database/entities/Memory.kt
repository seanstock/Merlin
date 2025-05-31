package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a significant interaction or piece of information about a child
 * that should be remembered for future personalization.
 */
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "child_id")
    val childId: String?,

    @ColumnInfo(name = "ts")
    val ts: Long?,

    @ColumnInfo(name = "text")
    val text: String?,

    @ColumnInfo(name = "sentiment", defaultValue = "0.0")
    val sentiment: Double? = 0.0,

    @ColumnInfo(name = "type", defaultValue = "'general'")
    val type: MemoryType = MemoryType.GENERAL,

    @ColumnInfo(name = "importance", defaultValue = "3")
    val importance: Int = 3 // Scale of 1-5, where 5 is most important
)

/**
 * Enum representing different types of memories for categorization.
 */
enum class MemoryType {
    GENERAL,        // General conversation or interaction
    PREFERENCE,     // Child's likes, dislikes, preferences
    ACHIEVEMENT,    // Accomplishments, successes, milestones
    DIFFICULTY,     // Areas where child struggles or needs help
    EMOTIONAL,      // Emotional states, feelings, reactions
    PERSONAL,       // Personal information, family, friends
    EDUCATIONAL     // Learning progress, subject mastery
} 