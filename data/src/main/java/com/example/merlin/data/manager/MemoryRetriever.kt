package com.example.merlin.data.manager

import android.util.Log
import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import com.example.merlin.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Retrieves and ranks relevant memories for personalizing AI interactions.
 * Uses keyword matching, recency, importance, and conversation context to select memories.
 */
class MemoryRetriever(
    private val memoryRepository: MemoryRepository
) {
    companion object {
        private const val TAG = "MemoryRetriever"
        private const val DEFAULT_MAX_MEMORIES = 5
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
        private const val MIN_RELEVANCE_SCORE = 0.1
    }

    // Cache for memory retrieval results
    private val memoryCache = ConcurrentHashMap<String, CachedMemoryResult>()

    /**
     * Configuration for memory retrieval scoring.
     */
    data class RetrievalConfig(
        val keywordWeight: Double = 1.0,
        val recencyWeight: Double = 0.8,
        val importanceWeight: Double = 1.2,
        val typeWeight: Double = 0.6,
        val maxMemories: Int = DEFAULT_MAX_MEMORIES,
        val minRelevanceScore: Double = MIN_RELEVANCE_SCORE
    )

    private val config = RetrievalConfig()

    /**
     * Cached memory result with expiry.
     */
    private data class CachedMemoryResult(
        val memories: List<ScoredMemory>,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS
    }

    /**
     * Memory with relevance score for ranking.
     */
    data class ScoredMemory(
        val memory: Memory,
        val relevanceScore: Double,
        val keywordMatches: List<String> = emptyList()
    )

    /**
     * Retrieve relevant memories for the current conversation context.
     */
    suspend fun getRelevantMemories(
        childId: String,
        currentMessage: String,
        conversationHistory: List<String> = emptyList(),
        maxMemories: Int = config.maxMemories
    ): List<ScoredMemory> = withContext(Dispatchers.IO) {
        
        val cacheKey = generateCacheKey(childId, currentMessage, conversationHistory)
        
        // Check cache first
        memoryCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) {
                Log.d(TAG, "Returning cached memories for child: $childId")
                return@withContext cached.memories.take(maxMemories)
            } else {
                memoryCache.remove(cacheKey)
            }
        }

        try {
            // Get all memories for the child
            val allMemories = memoryRepository.getMemoriesForChild(childId)
            
            if (allMemories.isEmpty()) {
                Log.d(TAG, "No memories found for child: $childId")
                return@withContext emptyList()
            }

            // Score and rank memories
            val scoredMemories = scoreMemories(
                memories = allMemories,
                currentMessage = currentMessage,
                conversationHistory = conversationHistory
            )

            // Filter by minimum relevance and take top results
            val relevantMemories = scoredMemories
                .filter { it.relevanceScore >= config.minRelevanceScore }
                .sortedByDescending { it.relevanceScore }
                .take(maxMemories)

            // Cache the result
            memoryCache[cacheKey] = CachedMemoryResult(
                memories = relevantMemories,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Retrieved ${relevantMemories.size} relevant memories for child: $childId")
            relevantMemories

        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving memories for child: $childId", e)
            emptyList()
        }
    }

    /**
     * Score memories based on relevance to current conversation.
     */
    private fun scoreMemories(
        memories: List<Memory>,
        currentMessage: String,
        conversationHistory: List<String>
    ): List<ScoredMemory> {
        val currentTime = System.currentTimeMillis()
        val combinedContext = (conversationHistory + currentMessage).joinToString(" ").lowercase()
        val contextKeywords = extractKeywords(combinedContext)

        return memories.mapNotNull { memory ->
            memory.text?.let { memoryText ->
                val keywordScore = calculateKeywordScore(memoryText, contextKeywords)
                val recencyScore = calculateRecencyScore(memory.ts ?: 0, currentTime)
                val importanceScore = calculateImportanceScore(memory.importance)
                val typeScore = calculateTypeScore(memory.type, currentMessage)

                val totalScore = (keywordScore * config.keywordWeight) +
                        (recencyScore * config.recencyWeight) +
                        (importanceScore * config.importanceWeight) +
                        (typeScore * config.typeWeight)

                val matchedKeywords = findMatchingKeywords(memoryText, contextKeywords)

                ScoredMemory(
                    memory = memory,
                    relevanceScore = totalScore,
                    keywordMatches = matchedKeywords
                )
            }
        }
    }

    /**
     * Calculate keyword matching score between memory and current context.
     */
    private fun calculateKeywordScore(memoryText: String, contextKeywords: Set<String>): Double {
        val memoryKeywords = extractKeywords(memoryText.lowercase())
        val matchingKeywords = memoryKeywords.intersect(contextKeywords)
        
        if (contextKeywords.isEmpty()) return 0.0
        
        val exactMatches = matchingKeywords.size.toDouble()
        val partialMatches = findPartialMatches(memoryKeywords, contextKeywords)
        
        return (exactMatches + (partialMatches * 0.5)) / contextKeywords.size
    }

    /**
     * Calculate recency score (more recent memories score higher).
     */
    private fun calculateRecencyScore(memoryTimestamp: Long, currentTime: Long): Double {
        if (memoryTimestamp <= 0) return 0.0
        
        val ageInDays = (currentTime - memoryTimestamp) / (24 * 60 * 60 * 1000.0)
        
        return when {
            ageInDays <= 1 -> 1.0      // Last day
            ageInDays <= 7 -> 0.8      // Last week
            ageInDays <= 30 -> 0.6     // Last month
            ageInDays <= 90 -> 0.4     // Last 3 months
            else -> 0.2                // Older
        }
    }

    /**
     * Calculate importance score based on memory importance level.
     */
    private fun calculateImportanceScore(importance: Int): Double {
        return importance / 5.0 // Normalize to 0-1 scale
    }

    /**
     * Calculate type-based score based on conversation context.
     */
    private fun calculateTypeScore(memoryType: MemoryType, currentMessage: String): Double {
        val messageLower = currentMessage.lowercase()
        
        return when (memoryType) {
            MemoryType.PREFERENCE -> {
                if (messageLower.contains(Regex("\\b(like|love|hate|favorite|prefer)\\b"))) 0.8 else 0.3
            }
            MemoryType.EMOTIONAL -> {
                if (messageLower.contains(Regex("\\b(feel|scared|happy|sad|excited|worried)\\b"))) 0.9 else 0.3
            }
            MemoryType.EDUCATIONAL -> {
                if (messageLower.contains(Regex("\\b(learn|study|math|reading|science|school)\\b"))) 0.8 else 0.4
            }
            MemoryType.PERSONAL -> {
                if (messageLower.contains(Regex("\\b(family|friend|mom|dad|sister|brother)\\b"))) 0.7 else 0.3
            }
            MemoryType.ACHIEVEMENT -> {
                if (messageLower.contains(Regex("\\b(proud|accomplished|good|great|success)\\b"))) 0.7 else 0.3
            }
            MemoryType.DIFFICULTY -> {
                if (messageLower.contains(Regex("\\b(hard|difficult|struggle|help|confused)\\b"))) 0.8 else 0.3
            }
            MemoryType.GENERAL -> 0.4 // Default moderate relevance
        }
    }

    /**
     * Extract meaningful keywords from text.
     */
    private fun extractKeywords(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does",
            "did", "will", "would", "could", "should", "may", "might", "can", "i", "you", "he",
            "she", "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his",
            "her", "its", "our", "their", "this", "that", "these", "those"
        )
        
        return text.split(Regex("\\W+"))
            .map { it.lowercase().trim() }
            .filter { it.length > 2 && !stopWords.contains(it) }
            .toSet()
    }

    /**
     * Find keywords that match between memory and context.
     */
    private fun findMatchingKeywords(memoryText: String, contextKeywords: Set<String>): List<String> {
        val memoryKeywords = extractKeywords(memoryText.lowercase())
        return memoryKeywords.intersect(contextKeywords).toList()
    }

    /**
     * Find partial matches between keyword sets (e.g., "math" matches "mathematics").
     */
    private fun findPartialMatches(memoryKeywords: Set<String>, contextKeywords: Set<String>): Int {
        var partialMatches = 0
        
        for (memoryKeyword in memoryKeywords) {
            for (contextKeyword in contextKeywords) {
                if (memoryKeyword != contextKeyword && 
                    (memoryKeyword.contains(contextKeyword) || contextKeyword.contains(memoryKeyword))) {
                    partialMatches++
                    break
                }
            }
        }
        
        return partialMatches
    }

    /**
     * Generate cache key for memory retrieval.
     */
    private fun generateCacheKey(
        childId: String,
        currentMessage: String,
        conversationHistory: List<String>
    ): String {
        val contextHash = (conversationHistory + currentMessage)
            .joinToString("")
            .hashCode()
        return "${childId}_${contextHash}"
    }

    /**
     * Format memories for inclusion in AI prompt.
     */
    fun formatMemoriesForPrompt(scoredMemories: List<ScoredMemory>): String {
        if (scoredMemories.isEmpty()) {
            return ""
        }

        val formattedMemories = scoredMemories.mapIndexed { index, scoredMemory ->
            val memory = scoredMemory.memory
            val timeAgo = formatTimeAgo(memory.ts ?: 0)
            val importance = "★".repeat(memory.importance)
            
            "${index + 1}. [${memory.type.name}] $importance ($timeAgo)\n   ${memory.text}"
        }.joinToString("\n\n")

        return """
Previous relevant memories about this child:
$formattedMemories

Use these memories to personalize your response and show that you remember previous interactions.
""".trim()
    }

    /**
     * Format timestamp as human-readable time ago.
     */
    private fun formatTimeAgo(timestamp: Long): String {
        if (timestamp <= 0) return "unknown time"
        
        val ageInMinutes = (System.currentTimeMillis() - timestamp) / (60 * 1000)
        
        return when {
            ageInMinutes < 60 -> "${ageInMinutes}m ago"
            ageInMinutes < 24 * 60 -> "${ageInMinutes / 60}h ago"
            ageInMinutes < 7 * 24 * 60 -> "${ageInMinutes / (24 * 60)}d ago"
            ageInMinutes < 30 * 24 * 60 -> "${ageInMinutes / (7 * 24 * 60)}w ago"
            else -> "${ageInMinutes / (30 * 24 * 60)}mo ago"
        }
    }

    /**
     * Clear memory cache for a specific child or all children.
     */
    fun clearCache(childId: String? = null) {
        if (childId != null) {
            memoryCache.keys.removeAll { it.startsWith("${childId}_") }
            Log.d(TAG, "Cleared memory cache for child: $childId")
        } else {
            memoryCache.clear()
            Log.d(TAG, "Cleared all memory cache")
        }
    }

    /**
     * Get cache statistics for monitoring.
     */
    fun getCacheStats(): Map<String, Any> {
        val now = System.currentTimeMillis()
        val expiredEntries = memoryCache.values.count { it.isExpired() }
        
        return mapOf(
            "totalEntries" to memoryCache.size,
            "expiredEntries" to expiredEntries,
            "activeEntries" to (memoryCache.size - expiredEntries)
        )
    }
} 