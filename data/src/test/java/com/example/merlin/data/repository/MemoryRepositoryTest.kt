package com.example.merlin.data.repository

import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import org.junit.Assert.*
import org.junit.Test

class MemoryRepositoryTest {

    @Test
    fun memoryStatistics_dataClass_shouldExist() {
        // Test that the MemoryStatistics data class exists and can be instantiated
        val stats = MemoryStatistics(
            totalCount = 10,
            lastMemoryTimestamp = System.currentTimeMillis(),
            typeDistribution = mapOf(MemoryType.GENERAL to 5, MemoryType.PREFERENCE to 3),
            importanceDistribution = mapOf(1 to 2, 2 to 3, 3 to 4, 4 to 1, 5 to 0)
        )
        
        assertEquals(10, stats.totalCount)
        assertNotNull(stats.lastMemoryTimestamp)
        assertEquals(5, stats.typeDistribution[MemoryType.GENERAL])
        assertEquals(3, stats.typeDistribution[MemoryType.PREFERENCE])
        assertEquals(4, stats.importanceDistribution[3])
    }

    @Test
    fun memoryType_enum_shouldHaveAllExpectedValues() {
        // Test that all expected memory types exist
        val expectedTypes = setOf(
            MemoryType.GENERAL,
            MemoryType.PREFERENCE,
            MemoryType.ACHIEVEMENT,
            MemoryType.DIFFICULTY,
            MemoryType.EMOTIONAL,
            MemoryType.PERSONAL,
            MemoryType.EDUCATIONAL
        )
        
        val actualTypes = MemoryType.values().toSet()
        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun memory_entity_shouldSupportNewFields() {
        // Test that the Memory entity supports the new type and importance fields
        val memory = Memory(
            id = 1L,
            childId = "test_child",
            ts = System.currentTimeMillis(),
            text = "Test memory content",
            sentiment = 0.5,
            type = MemoryType.PREFERENCE,
            importance = 4
        )
        
        assertEquals(1L, memory.id)
        assertEquals("test_child", memory.childId)
        assertEquals("Test memory content", memory.text)
        assertEquals(0.5, memory.sentiment!!, 0.001)
        assertEquals(MemoryType.PREFERENCE, memory.type)
        assertEquals(4, memory.importance)
    }
} 