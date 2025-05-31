package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_history",
    indices = [Index(value = ["child_id", "ts"], unique = false)]
    // Note: Room doesn't directly support DESC in @Index for schema generation.
    // Order by ts DESC will be handled in DAO queries.
)
data class ChatHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "child_id")
    val childId: String?,

    val role: String?, // 'user' or 'assistant'

    val content: String?,

    val ts: Long?
) 