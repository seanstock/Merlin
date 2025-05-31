package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType

/**
 * Data Access Object for Memory entities.
 * Provides methods for storing, retrieving, and managing child memories.
 */
@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: Memory): Long

    @Update
    suspend fun update(memory: Memory)

    @Delete
    suspend fun delete(memory: Memory)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): Memory?

    @Query("SELECT * FROM memories WHERE child_id = :childId ORDER BY ts DESC")
    suspend fun getForChild(childId: String): List<Memory>

    @Query("SELECT * FROM memories ORDER BY ts DESC")
    suspend fun getAll(): List<Memory>

    /**
     * Get recent memories for a child, limited by count.
     */
    @Query("SELECT * FROM memories WHERE child_id = :childId ORDER BY ts DESC LIMIT :limit")
    suspend fun getRecentMemories(childId: String, limit: Int = 10): List<Memory>

    /**
     * Get memories for a child filtered by type.
     */
    @Query("SELECT * FROM memories WHERE child_id = :childId AND type = :type ORDER BY ts DESC")
    suspend fun getMemoriesByType(childId: String, type: MemoryType): List<Memory>

    /**
     * Get memories for a child with importance above a threshold.
     */
    @Query("SELECT * FROM memories WHERE child_id = :childId AND importance >= :minImportance ORDER BY importance DESC, ts DESC")
    suspend fun getImportantMemories(childId: String, minImportance: Int = 4): List<Memory>

    /**
     * Get memories for a child within a time range.
     */
    @Query("SELECT * FROM memories WHERE child_id = :childId AND ts BETWEEN :startTime AND :endTime ORDER BY ts DESC")
    suspend fun getMemoriesInTimeRange(childId: String, startTime: Long, endTime: Long): List<Memory>

    /**
     * Delete old memories for a child older than the specified timestamp.
     * Used for memory cleanup to prevent database bloat.
     */
    @Query("DELETE FROM memories WHERE child_id = :childId AND ts < :timestamp")
    suspend fun deleteOldMemories(childId: String, timestamp: Long): Int

    /**
     * Delete memories with low importance to free up space.
     */
    @Query("DELETE FROM memories WHERE child_id = :childId AND importance <= :maxImportance")
    suspend fun deleteLowImportanceMemories(childId: String, maxImportance: Int = 2): Int

    /**
     * Get count of memories for a child.
     */
    @Query("SELECT COUNT(*) FROM memories WHERE child_id = :childId")
    suspend fun getMemoryCount(childId: String): Int

    /**
     * Get the most recent memory timestamp for a child.
     */
    @Query("SELECT MAX(ts) FROM memories WHERE child_id = :childId")
    suspend fun getLastMemoryTimestamp(childId: String): Long?

    /**
     * Clear all memories for a specific child.
     */
    @Query("DELETE FROM memories WHERE child_id = :childId")
    suspend fun clearMemoriesForChild(childId: String): Int

    /**
     * Search memories by text content.
     */
    @Query("SELECT * FROM memories WHERE child_id = :childId AND text LIKE '%' || :searchText || '%' ORDER BY ts DESC")
    suspend fun searchMemories(childId: String, searchText: String): List<Memory>
} 