package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.ChatHistory

@Dao
interface ChatHistoryDao {
    @Insert
    suspend fun insert(chatHistory: ChatHistory): Long

    // Get last N messages for a child, ordered by timestamp descending (most recent first)
    @Query("SELECT * FROM chat_history WHERE child_id = :childId ORDER BY ts DESC LIMIT :limit")
    fun getRecentForChild(childId: String, limit: Int = 20): List<ChatHistory>

    // Get all messages for a child (useful for exporting or full history view)
    @Query("SELECT * FROM chat_history WHERE child_id = :childId ORDER BY ts ASC")
    fun getAllForChild(childId: String): List<ChatHistory>

    // Delete messages older than a certain timestamp for a specific child (for pruning)
    @Query("DELETE FROM chat_history WHERE child_id = :childId AND ts < :timestamp")
    suspend fun deleteOlderThanForChild(childId: String, timestamp: Long)

    // Basic delete for a single entry if needed
    @Delete
    suspend fun delete(chatHistory: ChatHistory)

    // Clear all chat history for a specific child
    @Query("DELETE FROM chat_history WHERE child_id = :childId")
    suspend fun clearForChild(childId: String)
} 