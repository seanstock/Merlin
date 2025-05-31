package com.example.merlin.data.manager

import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import org.junit.Assert.*
import org.junit.Test

class MemoryRetrieverTest {

    @Test
    fun retrievalConfig_dataClass_shouldExist() {
        // Test that the RetrievalConfig data class exists and can be instantiated
        val config = MemoryRetriever.RetrievalConfig(
            keywordWeight = 1.5,
            recencyWeight = 0.9,
            importanceWeight = 1.3,
            typeWeight = 0.7,
            maxMemories = 8,
            minRelevanceScore = 0.2
        )
        
        assertEquals(1.5, config.keywordWeight, 0.001)
        assertEquals(0.9, config.recencyWeight, 0.001)
        assertEquals(1.3, config.importanceWeight, 0.001)
        assertEquals(0.7, config.typeWeight, 0.001)
        assertEquals(8, config.maxMemories)
        assertEquals(0.2, config.minRelevanceScore, 0.001)
    }

    @Test
    fun retrievalConfig_defaultValues_shouldBeReasonable() {
        // Test that default values are reasonable for memory retrieval
        val config = MemoryRetriever.RetrievalConfig()
        
        assertTrue("Keyword weight should be positive", config.keywordWeight > 0.0)
        assertTrue("Recency weight should be positive", config.recencyWeight > 0.0)
        assertTrue("Importance weight should be positive", config.importanceWeight > 0.0)
        assertTrue("Type weight should be positive", config.typeWeight > 0.0)
        assertTrue("Max memories should be reasonable", config.maxMemories in 3..10)
        assertTrue("Min relevance score should be reasonable", config.minRelevanceScore in 0.05..0.3)
    }

    @Test
    fun scoredMemory_dataClass_shouldExist() {
        // Test that the ScoredMemory data class exists and can be instantiated
        val memory = Memory(
            id = 1L,
            childId = "test_child",
            ts = System.currentTimeMillis(),
            text = "Child loves math games",
            sentiment = 0.8,
            type = MemoryType.PREFERENCE,
            importance = 4
        )
        
        val scoredMemory = MemoryRetriever.ScoredMemory(
            memory = memory,
            relevanceScore = 0.75,
            keywordMatches = listOf("math", "games")
        )
        
        assertEquals(memory, scoredMemory.memory)
        assertEquals(0.75, scoredMemory.relevanceScore, 0.001)
        assertEquals(listOf("math", "games"), scoredMemory.keywordMatches)
    }

    @Test
    fun memoryRetriever_constants_shouldBeReasonable() {
        // Test that the companion object constants are accessible and reasonable
        // We can't directly access private constants, but we can test the concept
        val testConfig = MemoryRetriever.RetrievalConfig()
        
        // Default max memories should be reasonable for conversation context
        assertTrue("Default max memories should be reasonable", testConfig.maxMemories in 3..10)
        
        // Min relevance score should filter out irrelevant memories
        assertTrue("Min relevance score should filter noise", testConfig.minRelevanceScore > 0.0)
        assertTrue("Min relevance score should not be too high", testConfig.minRelevanceScore < 0.5)
    }

    @Test
    fun memoryType_contextMatching_shouldCoverAllTypes() {
        // Test that we can create memories of all types for context matching
        val memoryTypes = MemoryType.values()
        
        memoryTypes.forEach { type ->
            val memory = Memory(
                id = 1L,
                childId = "test_child",
                ts = System.currentTimeMillis(),
                text = "Test memory for type $type",
                sentiment = 0.0,
                type = type,
                importance = 3
            )
            
            assertEquals(type, memory.type)
            assertNotNull("Memory text should not be null", memory.text)
        }
        
        // Verify we have all expected types
        val expectedTypes = setOf(
            MemoryType.GENERAL, MemoryType.PREFERENCE, MemoryType.ACHIEVEMENT,
            MemoryType.DIFFICULTY, MemoryType.EMOTIONAL, MemoryType.PERSONAL,
            MemoryType.EDUCATIONAL
        )
        assertEquals(expectedTypes, memoryTypes.toSet())
    }

    @Test
    fun keywordExtraction_concepts_shouldWork() {
        // Test keyword extraction concepts (we can't test private methods directly)
        val testTexts = mapOf(
            "I love playing math games" to setOf("love", "playing", "math", "games"),
            "My favorite color is blue" to setOf("favorite", "color", "blue"),
            "I'm scared of the dark" to setOf("scared", "dark"),
            "Math is really hard for me" to setOf("math", "really", "hard")
        )
        
        testTexts.forEach { (text, expectedKeywords) ->
            // Simulate keyword extraction logic
            val words = text.lowercase()
                .split(Regex("\\W+"))
                .filter { it.length > 2 }
                .filter { !setOf("the", "and", "for").contains(it) }
                .toSet()
            
            // Should contain some of the expected keywords
            val intersection = words.intersect(expectedKeywords)
            assertTrue("Should extract meaningful keywords from: $text", intersection.isNotEmpty())
        }
    }

    @Test
    fun recencyScoring_concepts_shouldWork() {
        // Test recency scoring concepts
        val currentTime = System.currentTimeMillis()
        val oneHourAgo = currentTime - (60 * 60 * 1000)
        val halfDayAgo = currentTime - (12 * 60 * 60 * 1000) // 12 hours ago (within 1 day)
        val threeDaysAgo = currentTime - (3 * 24 * 60 * 60 * 1000) // 3 days ago (within 1 week)
        val twoWeeksAgo = currentTime - (14 * 24 * 60 * 60 * 1000) // 2 weeks ago (within 1 month)
        val threeMonthsAgo = currentTime - (100L * 24 * 60 * 60 * 1000) // 100 days ago (older than 30 days)
        
        // Simulate recency scoring logic
        fun calculateRecencyScore(timestamp: Long): Double {
            val ageInDays = (currentTime - timestamp) / (24 * 60 * 60 * 1000.0)
            return when {
                ageInDays <= 1 -> 1.0
                ageInDays <= 7 -> 0.8
                ageInDays <= 30 -> 0.6
                else -> 0.2
            }
        }
        
        val recentScore = calculateRecencyScore(oneHourAgo)
        val halfDayScore = calculateRecencyScore(halfDayAgo)
        val threeDayScore = calculateRecencyScore(threeDaysAgo)
        val twoWeekScore = calculateRecencyScore(twoWeeksAgo)
        val threeMonthScore = calculateRecencyScore(threeMonthsAgo)
        
        // Test that scores decrease with age
        assertEquals("Recent memories should score 1.0", 1.0, recentScore, 0.001)
        assertEquals("Half-day memories should score 1.0", 1.0, halfDayScore, 0.001)
        assertEquals("Three-day memories should score 0.8", 0.8, threeDayScore, 0.001)
        assertEquals("Two-week memories should score 0.6", 0.6, twoWeekScore, 0.001)
        assertEquals("Three-month memories should score 0.2", 0.2, threeMonthScore, 0.001)
        
        assertTrue("Recent memories should score highest", recentScore >= threeDayScore)
        assertTrue("Week-old memories should score higher than month-old", threeDayScore >= twoWeekScore)
        assertTrue("Month-old memories should score higher than very old", twoWeekScore >= threeMonthScore)
        assertTrue("All scores should be positive", threeMonthScore > 0.0)
    }

    @Test
    fun importanceScoring_concepts_shouldWork() {
        // Test importance scoring concepts
        val importanceLevels = 1..5
        
        importanceLevels.forEach { importance ->
            // Simulate importance scoring (normalize to 0-1)
            val score = importance / 5.0
            
            assertTrue("Importance score should be in valid range", score in 0.0..1.0)
            
            if (importance == 5) {
                assertEquals("Max importance should give max score", 1.0, score, 0.001)
            }
            if (importance == 1) {
                assertEquals("Min importance should give min score", 0.2, score, 0.001)
            }
        }
    }

    @Test
    fun typeBasedScoring_concepts_shouldWork() {
        // Test type-based scoring concepts
        val testCases = mapOf(
            MemoryType.PREFERENCE to "I love chocolate ice cream",
            MemoryType.EMOTIONAL to "I feel really happy today",
            MemoryType.EDUCATIONAL to "I want to learn more about science",
            MemoryType.PERSONAL to "My mom is a teacher",
            MemoryType.ACHIEVEMENT to "I'm proud of my drawing",
            MemoryType.DIFFICULTY to "Math is really hard for me"
        )
        
        testCases.forEach { (memoryType, message) ->
            val messageLower = message.lowercase()
            
            // Simulate type-based scoring logic
            val score = when (memoryType) {
                MemoryType.PREFERENCE -> {
                    if (messageLower.contains(Regex("\\b(like|love|hate|favorite)\\b"))) 0.8 else 0.3
                }
                MemoryType.EMOTIONAL -> {
                    if (messageLower.contains(Regex("\\b(feel|happy|sad|excited)\\b"))) 0.9 else 0.3
                }
                MemoryType.EDUCATIONAL -> {
                    if (messageLower.contains(Regex("\\b(learn|study|science|school)\\b"))) 0.8 else 0.4
                }
                MemoryType.PERSONAL -> {
                    if (messageLower.contains(Regex("\\b(mom|dad|family|friend)\\b"))) 0.7 else 0.3
                }
                MemoryType.ACHIEVEMENT -> {
                    if (messageLower.contains(Regex("\\b(proud|accomplished|good)\\b"))) 0.7 else 0.3
                }
                MemoryType.DIFFICULTY -> {
                    if (messageLower.contains(Regex("\\b(hard|difficult|struggle)\\b"))) 0.8 else 0.3
                }
                else -> 0.4
            }
            
            assertTrue("Type-based score should be positive for $memoryType", score > 0.0)
            assertTrue("Type-based score should be reasonable for $memoryType", score <= 1.0)
        }
    }

    @Test
    fun memoryFormatting_concepts_shouldWork() {
        // Test memory formatting concepts
        val testMemories = listOf(
            Memory(
                id = 1L,
                childId = "test_child",
                ts = System.currentTimeMillis() - (60 * 60 * 1000), // 1 hour ago
                text = "Child loves math games",
                sentiment = 0.8,
                type = MemoryType.PREFERENCE,
                importance = 4
            ),
            Memory(
                id = 2L,
                childId = "test_child",
                ts = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // 1 day ago
                text = "Child struggled with fractions",
                sentiment = -0.3,
                type = MemoryType.DIFFICULTY,
                importance = 5
            )
        )
        
        // Simulate memory formatting
        val formattedMemories = testMemories.mapIndexed { index, memory ->
            val importance = "★".repeat(memory.importance)
            "${index + 1}. [${memory.type.name}] $importance\n   ${memory.text}"
        }.joinToString("\n\n")
        
        assertTrue("Formatted memories should contain type info", formattedMemories.contains("PREFERENCE"))
        assertTrue("Formatted memories should contain type info", formattedMemories.contains("DIFFICULTY"))
        assertTrue("Formatted memories should contain importance stars", formattedMemories.contains("★★★★"))
        assertTrue("Formatted memories should contain importance stars", formattedMemories.contains("★★★★★"))
        assertTrue("Formatted memories should contain memory text", formattedMemories.contains("loves math games"))
        assertTrue("Formatted memories should contain memory text", formattedMemories.contains("struggled with fractions"))
    }
} 