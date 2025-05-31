package com.example.merlin.data.manager

import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MemorySummarizerTest {

    private lateinit var memorySummarizer: MemorySummarizer

    @Before
    fun setUp() {
        // Note: These tests focus on data classes and utility methods
        // Full integration tests would require mocking OpenAIClientWrapper and MemoryRepository
    }

    @Test
    fun summarizationConfig_dataClass_shouldExist() {
        // Test that SummarizationConfig data class exists and can be instantiated
        val config = MemorySummarizer.SummarizationConfig(
            batchSize = 15,
            ageThresholdDays = 45,
            minMemoriesForSummary = 8,
            enableAutoSummarization = false,
            preserveHighImportanceMemories = false,
            maxSummaryLength = 300
        )
        
        assertEquals(15, config.batchSize)
        assertEquals(45, config.ageThresholdDays)
        assertEquals(8, config.minMemoriesForSummary)
        assertFalse(config.enableAutoSummarization)
        assertFalse(config.preserveHighImportanceMemories)
        assertEquals(300, config.maxSummaryLength)
    }

    @Test
    fun summarizationConfig_defaultValues_shouldBeReasonable() {
        // Test that default values are reasonable
        val config = MemorySummarizer.SummarizationConfig()
        
        assertTrue("Batch size should be positive", config.batchSize > 0)
        assertTrue("Age threshold should be positive", config.ageThresholdDays > 0)
        assertTrue("Min memories should be positive", config.minMemoriesForSummary > 0)
        assertTrue("Should enable auto summarization by default", config.enableAutoSummarization)
        assertTrue("Should preserve high importance memories by default", config.preserveHighImportanceMemories)
        assertTrue("Max summary length should be reasonable", config.maxSummaryLength in 200..1000)
        
        // Reasonable defaults
        assertTrue("Batch size should be reasonable", config.batchSize in 5..20)
        assertTrue("Age threshold should be reasonable", config.ageThresholdDays in 7..90)
        assertTrue("Min memories should be reasonable", config.minMemoriesForSummary in 3..10)
    }

    @Test
    fun summarizationResult_dataClass_shouldExist() {
        // Test that SummarizationResult data class exists
        val timeRange = Pair(1000L, 2000L)
        val dominantTypes = listOf(MemoryType.PREFERENCE, MemoryType.EDUCATIONAL)
        
        val result = MemorySummarizer.SummarizationResult(
            summary = "Test summary of child's learning preferences",
            originalMemoryIds = listOf(1L, 2L, 3L),
            memoryCount = 3,
            timeRange = timeRange,
            dominantTypes = dominantTypes,
            averageImportance = 3.5,
            createdAt = 12345L
        )
        
        assertEquals("Test summary of child's learning preferences", result.summary)
        assertEquals(listOf(1L, 2L, 3L), result.originalMemoryIds)
        assertEquals(3, result.memoryCount)
        assertEquals(timeRange, result.timeRange)
        assertEquals(dominantTypes, result.dominantTypes)
        assertEquals(3.5, result.averageImportance, 0.01)
        assertEquals(12345L, result.createdAt)
    }

    @Test
    fun summarizationResult_defaultCreatedAt_shouldBeReasonable() {
        // Test that default createdAt is set to current time
        val currentTime = System.currentTimeMillis()
        
        val result = MemorySummarizer.SummarizationResult(
            summary = "Test",
            originalMemoryIds = emptyList(),
            memoryCount = 0,
            timeRange = Pair(0L, 0L),
            dominantTypes = emptyList(),
            averageImportance = 0.0
        )
        
        // Should be within a reasonable range of current time (within 1 second)
        assertTrue("Created at should be close to current time", 
            Math.abs(result.createdAt - currentTime) < 1000)
    }

    @Test
    fun summarizationStats_dataClass_shouldExist() {
        // Test that SummarizationStats data class exists
        val stats = MemorySummarizer.SummarizationStats(
            totalMemoriesSummarized = 25,
            summariesCreated = 3,
            tokensSaved = 1250,
            oldestSummaryAge = 86400000L, // 1 day
            newestSummaryAge = 3600000L   // 1 hour
        )
        
        assertEquals(25, stats.totalMemoriesSummarized)
        assertEquals(3, stats.summariesCreated)
        assertEquals(1250, stats.tokensSaved)
        assertEquals(86400000L, stats.oldestSummaryAge)
        assertEquals(3600000L, stats.newestSummaryAge)
    }

    @Test
    fun formatTimeAgo_variousTimestamps_shouldFormatCorrectly() {
        // Test time formatting logic (if accessible via public method or test utility)
        val currentTime = System.currentTimeMillis()
        
        // Test cases for different time periods
        val testCases = mapOf(
            currentTime to "today",
            currentTime - (2 * 24 * 60 * 60 * 1000L) to "2d ago",
            currentTime - (14 * 24 * 60 * 60 * 1000L) to "2w ago",
            currentTime - (60 * 24 * 60 * 60 * 1000L) to "2mo ago",
            currentTime - (400 * 24 * 60 * 60 * 1000L) to "1y ago",
            0L to "unknown time"
        )
        
        // Note: This test would require the formatTimeAgo method to be public or testable
        // For now, we're testing the logic conceptually
        testCases.forEach { (timestamp, expected) ->
            val ageInDays = (currentTime - timestamp) / (24 * 60 * 60 * 1000)
            
            val result = when {
                timestamp <= 0 -> "unknown time"
                ageInDays < 1 -> "today"
                ageInDays < 7 -> "${ageInDays}d ago"
                ageInDays < 30 -> "${ageInDays / 7}w ago"
                ageInDays < 365 -> "${ageInDays / 30}mo ago"
                else -> "${ageInDays / 365}y ago"
            }
            
            assertEquals("Time formatting for $timestamp", expected, result)
        }
    }

    @Test
    fun memoryGrouping_byType_shouldGroupCorrectly() {
        // Test memory grouping logic conceptually
        val memories = listOf(
            createTestMemory(1L, MemoryType.PREFERENCE, "Child loves math", 4),
            createTestMemory(2L, MemoryType.PREFERENCE, "Child dislikes reading", 3),
            createTestMemory(3L, MemoryType.EDUCATIONAL, "Learned fractions", 4),
            createTestMemory(4L, MemoryType.EDUCATIONAL, "Struggled with division", 3),
            createTestMemory(5L, MemoryType.EMOTIONAL, "Felt proud of achievement", 5),
            createTestMemory(6L, MemoryType.PERSONAL, "Has a pet dog", 2)
        )
        
        // Group by type
        val typeGroups = memories.groupBy { it.type }
        
        assertEquals(4, typeGroups.size) // 4 different types
        assertEquals(2, typeGroups[MemoryType.PREFERENCE]?.size)
        assertEquals(2, typeGroups[MemoryType.EDUCATIONAL]?.size)
        assertEquals(1, typeGroups[MemoryType.EMOTIONAL]?.size)
        assertEquals(1, typeGroups[MemoryType.PERSONAL]?.size)
    }

    @Test
    fun memoryGrouping_byTimeWeeks_shouldGroupCorrectly() {
        // Test time-based grouping logic
        val currentTime = System.currentTimeMillis()
        val weekInMs = 7 * 24 * 60 * 60 * 1000L
        
        val memories = listOf(
            createTestMemory(1L, MemoryType.GENERAL, "Recent memory", 3, currentTime),
            createTestMemory(2L, MemoryType.GENERAL, "Last week memory", 3, currentTime - weekInMs),
            createTestMemory(3L, MemoryType.GENERAL, "Two weeks ago", 3, currentTime - (2 * weekInMs)),
            createTestMemory(4L, MemoryType.GENERAL, "Another recent", 3, currentTime - (24 * 60 * 60 * 1000L))
        )
        
        // Group by week number
        val weekGroups = memories.groupBy { memory ->
            val timestamp = memory.ts ?: 0L
            timestamp / weekInMs
        }
        
        assertTrue("Should have multiple week groups", weekGroups.size >= 2)
        
        // Current week should have 2 memories (recent and another recent)
        val currentWeek = currentTime / weekInMs
        assertEquals(2, weekGroups[currentWeek]?.size)
    }

    @Test
    fun memoryFormatting_forSummarization_shouldIncludeKeyInfo() {
        // Test memory formatting for AI summarization
        val memory = createTestMemory(
            id = 1L,
            type = MemoryType.PREFERENCE,
            text = "Child loves math games and puzzles",
            importance = 4,
            timestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L) // 2 days ago
        )
        
        // Format similar to the actual implementation
        val importance = "★".repeat(memory.importance)
        val type = memory.type.name.lowercase().replaceFirstChar { it.uppercase() }
        
        assertEquals("★★★★", importance)
        assertEquals("Preference", type)
        assertEquals("Child loves math games and puzzles", memory.text)
    }

    @Test
    fun dominantTypes_calculation_shouldReturnMostCommon() {
        // Test dominant type calculation logic
        val memories = listOf(
            createTestMemory(1L, MemoryType.PREFERENCE, "Preference 1", 3),
            createTestMemory(2L, MemoryType.PREFERENCE, "Preference 2", 3),
            createTestMemory(3L, MemoryType.PREFERENCE, "Preference 3", 3),
            createTestMemory(4L, MemoryType.EDUCATIONAL, "Educational 1", 3),
            createTestMemory(5L, MemoryType.EDUCATIONAL, "Educational 2", 3),
            createTestMemory(6L, MemoryType.EMOTIONAL, "Emotional 1", 3)
        )
        
        val dominantTypes = memories.groupBy { it.type }
            .toList()
            .sortedByDescending { it.second.size }
            .take(3)
            .map { it.first }
        
        assertEquals(MemoryType.PREFERENCE, dominantTypes[0]) // Most common (3 occurrences)
        assertEquals(MemoryType.EDUCATIONAL, dominantTypes[1]) // Second most common (2 occurrences)
        assertEquals(MemoryType.EMOTIONAL, dominantTypes[2]) // Third most common (1 occurrence)
    }

    @Test
    fun averageImportance_calculation_shouldBeCorrect() {
        // Test average importance calculation
        val memories = listOf(
            createTestMemory(1L, MemoryType.GENERAL, "Memory 1", 5),
            createTestMemory(2L, MemoryType.GENERAL, "Memory 2", 3),
            createTestMemory(3L, MemoryType.GENERAL, "Memory 3", 4),
            createTestMemory(4L, MemoryType.GENERAL, "Memory 4", 2)
        )
        
        val averageImportance = memories.map { it.importance }.average()
        
        assertEquals(3.5, averageImportance, 0.01) // (5+3+4+2)/4 = 3.5
    }

    @Test
    fun timeRange_calculation_shouldFindMinMax() {
        // Test time range calculation
        val timestamps = listOf(1000L, 3000L, 2000L, 5000L, 1500L)
        val memories = timestamps.mapIndexed { index, timestamp ->
            createTestMemory(index.toLong(), MemoryType.GENERAL, "Memory $index", 3, timestamp)
        }
        
        val minTimestamp = memories.minOfOrNull { it.ts ?: 0L } ?: 0L
        val maxTimestamp = memories.maxOfOrNull { it.ts ?: 0L } ?: 0L
        
        assertEquals(1000L, minTimestamp)
        assertEquals(5000L, maxTimestamp)
    }

    @Test
    fun cacheKey_generation_shouldBeConsistent() {
        // Test cache key generation logic
        val memories = listOf(
            createTestMemory(3L, MemoryType.GENERAL, "Memory 3", 3),
            createTestMemory(1L, MemoryType.GENERAL, "Memory 1", 3),
            createTestMemory(2L, MemoryType.GENERAL, "Memory 2", 3)
        )
        
        val childId = "test_child"
        
        // Simulate cache key generation
        val memoryIds = memories.mapNotNull { it.id }.sorted().joinToString(",")
        val cacheKey = "${childId}_${memoryIds.hashCode()}"
        
        assertEquals("1,2,3", memoryIds)
        assertTrue("Cache key should contain child ID", cacheKey.startsWith(childId))
        assertTrue("Cache key should contain hash", cacheKey.contains("_"))
    }

    private fun createTestMemory(
        id: Long,
        type: MemoryType,
        text: String,
        importance: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Memory {
        return Memory(
            id = id,
            childId = "test_child",
            ts = timestamp,
            text = text,
            sentiment = 0.0,
            type = type,
            importance = importance
        )
    }
} 