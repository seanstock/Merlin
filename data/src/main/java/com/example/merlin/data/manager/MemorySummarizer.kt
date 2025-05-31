package com.example.merlin.data.manager

import android.util.Log
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import com.example.merlin.data.remote.OpenAIClientWrapper
import com.example.merlin.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages memory summarization to maintain long-term context while reducing token usage.
 * Groups related memories and creates AI-generated summaries for efficient storage.
 */
class MemorySummarizer(
    private val aiService: AIServiceInterface,
    private val memoryRepository: MemoryRepository
) {
    companion object {
        private const val TAG = "MemorySummarizer"
        private const val DEFAULT_BATCH_SIZE = 10 // Memories to summarize at once
        private const val DEFAULT_AGE_THRESHOLD_DAYS = 30 // Summarize memories older than this
        private const val DEFAULT_MIN_MEMORIES_FOR_SUMMARY = 5 // Minimum memories needed for summarization
        private const val SUMMARY_PROMPT_TEMPLATE = """You are helping to summarize a child's learning interactions for an AI tutor named Merlin.

Please create a concise summary of the following memories that captures:
1. Key preferences and interests
2. Learning strengths and difficulties  
3. Emotional patterns and responses
4. Important personal information
5. Educational progress and achievements

Keep the summary focused, factual, and useful for personalizing future tutoring sessions.

Memories to summarize:
%s

Please provide a clear, organized summary in 2-3 paragraphs."""
    }

    /**
     * Configuration for memory summarization.
     */
    data class SummarizationConfig(
        val batchSize: Int = DEFAULT_BATCH_SIZE,
        val ageThresholdDays: Int = DEFAULT_AGE_THRESHOLD_DAYS,
        val minMemoriesForSummary: Int = DEFAULT_MIN_MEMORIES_FOR_SUMMARY,
        val enableAutoSummarization: Boolean = true,
        val preserveHighImportanceMemories: Boolean = true,
        val maxSummaryLength: Int = 500 // Maximum characters for summary
    )

    private val config = SummarizationConfig()
    
    // Cache for summarization results
    private val summarizationCache = ConcurrentHashMap<String, SummarizationResult>()

    /**
     * Result of memory summarization operation.
     */
    data class SummarizationResult(
        val summary: String,
        val originalMemoryIds: List<Long>,
        val memoryCount: Int,
        val timeRange: Pair<Long, Long>, // Start and end timestamps
        val dominantTypes: List<MemoryType>,
        val averageImportance: Double,
        val createdAt: Long = System.currentTimeMillis()
    )

    /**
     * Summary statistics for monitoring.
     */
    data class SummarizationStats(
        val totalMemoriesSummarized: Int,
        val summariesCreated: Int,
        val tokensSaved: Int,
        val oldestSummaryAge: Long,
        val newestSummaryAge: Long
    )

    /**
     * Automatically summarize old memories for a specific child.
     */
    suspend fun summarizeOldMemories(childId: String): SummarizationResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting automatic summarization for child: $childId")
            
            // Get old memories that haven't been summarized
            val cutoffTime = System.currentTimeMillis() - (config.ageThresholdDays * 24 * 60 * 60 * 1000L)
            val oldMemories = memoryRepository.getMemoriesInTimeRange(
                childId = childId,
                startTime = 0,
                endTime = cutoffTime
            ).filter { !it.text.isNullOrEmpty() }
            
            if (oldMemories.size < config.minMemoriesForSummary) {
                Log.d(TAG, "Not enough old memories for summarization: ${oldMemories.size}")
                return@withContext null
            }
            
            // Group memories by type and time for better summarization
            val groupedMemories = groupMemoriesForSummarization(oldMemories)
            
            if (groupedMemories.isEmpty()) {
                Log.d(TAG, "No suitable memory groups found for summarization")
                return@withContext null
            }
            
            // Summarize the largest group first
            val largestGroup = groupedMemories.maxByOrNull { it.size } ?: return@withContext null
            
            val result = summarizeMemoryGroup(childId, largestGroup)
            
            if (result != null) {
                // Store the summary as a new memory
                storeSummaryAsMemory(childId, result)
                
                // Optionally remove original memories (keeping high importance ones)
                if (config.preserveHighImportanceMemories) {
                    removeOriginalMemories(largestGroup.filter { it.importance < 4 })
                } else {
                    removeOriginalMemories(largestGroup)
                }
                
                Log.d(TAG, "Successfully summarized ${largestGroup.size} memories for child: $childId")
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during automatic summarization for child: $childId", e)
            null
        }
    }

    /**
     * Summarize a specific group of memories.
     */
    suspend fun summarizeMemoryGroup(
        childId: String,
        memories: List<Memory>
    ): SummarizationResult? = withContext(Dispatchers.IO) {
        
        if (memories.isEmpty()) {
            Log.w(TAG, "Cannot summarize empty memory group")
            return@withContext null
        }
        
        try {
            // Create cache key for this group
            val cacheKey = generateCacheKey(childId, memories)
            summarizationCache[cacheKey]?.let { cached ->
                Log.d(TAG, "Returning cached summarization result")
                return@withContext cached
            }
            
            // Format memories for AI summarization
            val formattedMemories = formatMemoriesForSummarization(memories)
            val prompt = SUMMARY_PROMPT_TEMPLATE.format(formattedMemories)
            
            // Create chat messages for AI request
            val chatMessages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    messageContent = com.aallam.openai.api.chat.TextContent("You are an expert at creating concise, useful summaries of educational interactions.")
                ),
                ChatMessage(
                    role = ChatRole.User,
                    messageContent = com.aallam.openai.api.chat.TextContent(prompt)
                )
            )
            
            Log.d(TAG, "Requesting AI summarization for ${memories.size} memories")
            
            // Get AI summary
            val aiResponse = aiService.getChatCompletionWithFunctions(chatMessages, null)
            
            if (aiResponse?.content != null) {
                val summary = aiResponse.content.take(config.maxSummaryLength)
                
                // Calculate statistics
                val timeRange = Pair(
                    memories.minOfOrNull { it.ts ?: 0L } ?: 0L,
                    memories.maxOfOrNull { it.ts ?: 0L } ?: 0L
                )
                
                val dominantTypes = memories.groupBy { it.type }
                    .toList()
                    .sortedByDescending { it.second.size }
                    .take(3)
                    .map { it.first }
                
                val averageImportance = memories.map { it.importance }.average()
                
                val result = SummarizationResult(
                    summary = summary,
                    originalMemoryIds = memories.mapNotNull { it.id },
                    memoryCount = memories.size,
                    timeRange = timeRange,
                    dominantTypes = dominantTypes,
                    averageImportance = averageImportance
                )
                
                // Cache the result
                summarizationCache[cacheKey] = result
                
                Log.d(TAG, "Successfully created summary for ${memories.size} memories")
                return@withContext result
                
            } else {
                Log.w(TAG, "AI summarization returned null or empty content")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory group summarization", e)
            return@withContext null
        }
    }

    /**
     * Group memories for optimal summarization.
     */
    private fun groupMemoriesForSummarization(memories: List<Memory>): List<List<Memory>> {
        val groups = mutableListOf<List<Memory>>()
        
        // Group by memory type first
        val typeGroups = memories.groupBy { it.type }
        
        typeGroups.forEach { (type, typeMemories) ->
            // Further group by time periods (weekly chunks)
            val weeklyGroups = typeMemories.groupBy { memory ->
                val timestamp = memory.ts ?: 0L
                timestamp / (7 * 24 * 60 * 60 * 1000L) // Week number
            }
            
            weeklyGroups.values.forEach { weekMemories ->
                if (weekMemories.size >= config.minMemoriesForSummary) {
                    // Split large groups into smaller batches
                    weekMemories.chunked(config.batchSize).forEach { batch ->
                        if (batch.size >= config.minMemoriesForSummary) {
                            groups.add(batch)
                        }
                    }
                }
            }
        }
        
        // If no type-based groups are large enough, try mixed grouping by time only
        if (groups.isEmpty()) {
            val timeGroups = memories.groupBy { memory ->
                val timestamp = memory.ts ?: 0L
                timestamp / (7 * 24 * 60 * 60 * 1000L) // Week number
            }
            
            timeGroups.values.forEach { weekMemories ->
                if (weekMemories.size >= config.minMemoriesForSummary) {
                    weekMemories.chunked(config.batchSize).forEach { batch ->
                        if (batch.size >= config.minMemoriesForSummary) {
                            groups.add(batch)
                        }
                    }
                }
            }
        }
        
        return groups.sortedByDescending { it.size }
    }

    /**
     * Format memories for AI summarization prompt.
     */
    private fun formatMemoriesForSummarization(memories: List<Memory>): String {
        return memories.mapIndexed { index, memory ->
            val timeAgo = formatTimeAgo(memory.ts ?: 0L)
            val importance = "★".repeat(memory.importance)
            val type = memory.type.name.lowercase().replaceFirstChar { it.uppercase() }
            
            "${index + 1}. [$type] $importance ($timeAgo)\n   ${memory.text?.trim()}"
        }.joinToString("\n\n")
    }

    /**
     * Store summarization result as a new memory.
     */
    private suspend fun storeSummaryAsMemory(childId: String, result: SummarizationResult) {
        try {
            val summaryMemory = Memory(
                childId = childId,
                ts = System.currentTimeMillis(),
                text = "SUMMARY (${result.memoryCount} memories): ${result.summary}",
                sentiment = 0.0, // Neutral sentiment for summaries
                type = result.dominantTypes.firstOrNull() ?: MemoryType.GENERAL,
                importance = maxOf(4, result.averageImportance.toInt()) // Summaries are important
            )
            
            memoryRepository.insertMemory(summaryMemory)
            Log.d(TAG, "Stored summary as new memory for child: $childId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error storing summary as memory", e)
        }
    }

    /**
     * Remove original memories after successful summarization.
     */
    private suspend fun removeOriginalMemories(memories: List<Memory>) {
        try {
            memories.forEach { memory ->
                memory.id?.let { id ->
                    memoryRepository.deleteMemory(id)
                }
            }
            Log.d(TAG, "Removed ${memories.size} original memories after summarization")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error removing original memories", e)
        }
    }

    /**
     * Get summarization statistics for monitoring.
     */
    suspend fun getSummarizationStats(childId: String): SummarizationStats = withContext(Dispatchers.IO) {
        try {
            val allMemories = memoryRepository.getMemoriesForChild(childId)
            val summaryMemories = allMemories.filter { 
                it.text?.startsWith("SUMMARY") == true 
            }
            
            val totalMemoriesSummarized = summaryMemories.sumOf { memory ->
                // Extract count from summary text
                val countMatch = Regex("SUMMARY \\((\\d+) memories\\)").find(memory.text ?: "")
                countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
            
            val oldestSummary = summaryMemories.minOfOrNull { it.ts ?: 0L } ?: 0L
            val newestSummary = summaryMemories.maxOfOrNull { it.ts ?: 0L } ?: 0L
            
            // Estimate tokens saved (rough calculation)
            val tokensSaved = totalMemoriesSummarized * 50 // Assume 50 tokens saved per summarized memory
            
            SummarizationStats(
                totalMemoriesSummarized = totalMemoriesSummarized,
                summariesCreated = summaryMemories.size,
                tokensSaved = tokensSaved,
                oldestSummaryAge = if (oldestSummary > 0) System.currentTimeMillis() - oldestSummary else 0L,
                newestSummaryAge = if (newestSummary > 0) System.currentTimeMillis() - newestSummary else 0L
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating summarization stats", e)
            SummarizationStats(0, 0, 0, 0L, 0L)
        }
    }

    /**
     * Check if a child's memories need summarization.
     */
    suspend fun needsSummarization(childId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (config.ageThresholdDays * 24 * 60 * 60 * 1000L)
            val oldMemories = memoryRepository.getMemoriesInTimeRange(
                childId = childId,
                startTime = 0,
                endTime = cutoffTime
            ).filter { it.text?.startsWith("SUMMARY") != true } // Exclude existing summaries
            
            oldMemories.size >= config.minMemoriesForSummary
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking summarization needs", e)
            false
        }
    }

    /**
     * Generate cache key for memory group.
     */
    private fun generateCacheKey(childId: String, memories: List<Memory>): String {
        val memoryIds = memories.mapNotNull { it.id }.sorted().joinToString(",")
        return "${childId}_${memoryIds.hashCode()}"
    }

    /**
     * Format timestamp as human-readable time ago.
     */
    private fun formatTimeAgo(timestamp: Long): String {
        if (timestamp <= 0) return "unknown time"
        
        val ageInDays = (System.currentTimeMillis() - timestamp) / (24 * 60 * 60 * 1000)
        
        return when {
            ageInDays < 1 -> "today"
            ageInDays < 7 -> "${ageInDays}d ago"
            ageInDays < 30 -> "${ageInDays / 7}w ago"
            ageInDays < 365 -> "${ageInDays / 30}mo ago"
            else -> "${ageInDays / 365}y ago"
        }
    }

    /**
     * Clear summarization cache.
     */
    fun clearCache() {
        summarizationCache.clear()
        Log.d(TAG, "Cleared summarization cache")
    }

    /**
     * Get cache statistics for monitoring.
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cacheSize" to summarizationCache.size,
            "cacheKeys" to summarizationCache.keys.toList()
        )
    }
} 