package com.example.merlin.data.repository

import android.util.Log
import com.example.merlin.data.database.dao.MemoryDao
import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing Memory entities.
 * Provides business logic for memory storage, retrieval, and cleanup.
 */
class MemoryRepository(private val memoryDao: MemoryDao) {
    
    companion object {
        private const val TAG = "MemoryRepository"
        private const val MAX_MEMORIES_PER_CHILD = 500
        private const val CLEANUP_THRESHOLD = 400
        private const val OLD_MEMORY_DAYS = 90 // Days after which memories are considered old
    }

    /**
     * Insert a new memory and perform cleanup if necessary.
     */
    suspend fun insertMemory(memory: Memory): Long = withContext(Dispatchers.IO) {
        val memoryId = memoryDao.insert(memory)
        
        // Check if cleanup is needed
        memory.childId?.let { childId ->
            val memoryCount = memoryDao.getMemoryCount(childId)
            if (memoryCount > CLEANUP_THRESHOLD) {
                performMemoryCleanup(childId)
            }
        }
        
        Log.d(TAG, "Inserted memory with ID: $memoryId for child: ${memory.childId}")
        memoryId
    }

    /**
     * Update an existing memory.
     */
    suspend fun updateMemory(memory: Memory) = withContext(Dispatchers.IO) {
        memoryDao.update(memory)
        Log.d(TAG, "Updated memory with ID: ${memory.id}")
    }

    /**
     * Delete a memory.
     */
    suspend fun deleteMemory(memory: Memory) = withContext(Dispatchers.IO) {
        memoryDao.delete(memory)
        Log.d(TAG, "Deleted memory with ID: ${memory.id}")
    }

    /**
     * Delete a memory by ID.
     */
    suspend fun deleteMemory(memoryId: Long) = withContext(Dispatchers.IO) {
        memoryDao.deleteById(memoryId)
        Log.d(TAG, "Deleted memory with ID: $memoryId")
    }

    /**
     * Get a memory by ID.
     */
    suspend fun getMemoryById(id: Long): Memory? = withContext(Dispatchers.IO) {
        memoryDao.getById(id)
    }

    /**
     * Get all memories for a child.
     */
    suspend fun getMemoriesForChild(childId: String): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getForChild(childId)
    }

    /**
     * Get recent memories for a child.
     */
    suspend fun getRecentMemories(childId: String, limit: Int = 10): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getRecentMemories(childId, limit)
    }

    /**
     * Get memories by type for a child.
     */
    suspend fun getMemoriesByType(childId: String, type: MemoryType): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getMemoriesByType(childId, type)
    }

    /**
     * Get important memories for a child.
     */
    suspend fun getImportantMemories(childId: String, minImportance: Int = 4): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getImportantMemories(childId, minImportance)
    }

    /**
     * Get memories within a time range.
     */
    suspend fun getMemoriesInTimeRange(childId: String, startTime: Long, endTime: Long): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.getMemoriesInTimeRange(childId, startTime, endTime)
    }

    /**
     * Search memories by text content.
     */
    suspend fun searchMemories(childId: String, searchText: String): List<Memory> = withContext(Dispatchers.IO) {
        memoryDao.searchMemories(childId, searchText)
    }

    /**
     * Get memory count for a child.
     */
    suspend fun getMemoryCount(childId: String): Int = withContext(Dispatchers.IO) {
        memoryDao.getMemoryCount(childId)
    }

    /**
     * Get the most relevant memories for context.
     * Combines recent memories with important memories.
     */
    suspend fun getRelevantMemories(childId: String, maxCount: Int = 15): List<Memory> = withContext(Dispatchers.IO) {
        val recentMemories = memoryDao.getRecentMemories(childId, maxCount / 2)
        val importantMemories = memoryDao.getImportantMemories(childId, 4)
        
        // Combine and deduplicate
        val combinedMemories = (recentMemories + importantMemories)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Memory> { it.importance }.thenByDescending { it.ts })
            .take(maxCount)
        
        Log.d(TAG, "Retrieved ${combinedMemories.size} relevant memories for child: $childId")
        combinedMemories
    }

    /**
     * Perform memory cleanup to prevent database bloat.
     * Removes old, low-importance memories while preserving important ones.
     */
    suspend fun performMemoryCleanup(childId: String) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val oldMemoryThreshold = currentTime - (OLD_MEMORY_DAYS * 24 * 60 * 60 * 1000L)
            
            // Delete old, low-importance memories first
            val deletedOld = memoryDao.deleteOldMemories(childId, oldMemoryThreshold)
            Log.d(TAG, "Deleted $deletedOld old memories for child: $childId")
            
            // Check if we still need more cleanup
            val remainingCount = memoryDao.getMemoryCount(childId)
            if (remainingCount > MAX_MEMORIES_PER_CHILD) {
                val deletedLowImportance = memoryDao.deleteLowImportanceMemories(childId, 2)
                Log.d(TAG, "Deleted $deletedLowImportance low-importance memories for child: $childId")
            }
            
            val finalCount = memoryDao.getMemoryCount(childId)
            Log.d(TAG, "Memory cleanup completed for child: $childId. Final count: $finalCount")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory cleanup for child: $childId", e)
        }
    }

    /**
     * Clear all memories for a child.
     */
    suspend fun clearMemoriesForChild(childId: String): Int = withContext(Dispatchers.IO) {
        val deletedCount = memoryDao.clearMemoriesForChild(childId)
        Log.d(TAG, "Cleared $deletedCount memories for child: $childId")
        deletedCount
    }

    /**
     * Get memory statistics for a child.
     */
    suspend fun getMemoryStatistics(childId: String): MemoryStatistics = withContext(Dispatchers.IO) {
        val totalCount = memoryDao.getMemoryCount(childId)
        val lastTimestamp = memoryDao.getLastMemoryTimestamp(childId)
        
        val typeDistribution = MemoryType.values().associateWith { type ->
            memoryDao.getMemoriesByType(childId, type).size
        }
        
        val importanceDistribution = (1..5).associateWith { importance ->
            memoryDao.getImportantMemories(childId, importance).size
        }
        
        MemoryStatistics(
            totalCount = totalCount,
            lastMemoryTimestamp = lastTimestamp,
            typeDistribution = typeDistribution,
            importanceDistribution = importanceDistribution
        )
    }
}

/**
 * Data class representing memory statistics for a child.
 */
data class MemoryStatistics(
    val totalCount: Int,
    val lastMemoryTimestamp: Long?,
    val typeDistribution: Map<MemoryType, Int>,
    val importanceDistribution: Map<Int, Int>
) 