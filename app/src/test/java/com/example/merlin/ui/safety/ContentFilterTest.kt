package com.example.merlin.ui.safety

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ContentFilter to ensure child safety mechanisms work correctly.
 */
class ContentFilterTest {

    private val contentFilter = ContentFilter()

    @Test
    fun `filterUserInput should block inappropriate content`() {
        // Test inappropriate content
        val inappropriateInputs = listOf(
            "I want to fight someone",
            "Tell me about violence",
            "What is your address?",
            "You are stupid"
        )

        inappropriateInputs.forEach { input ->
            val result = contentFilter.filterUserInput(input)
            assertFalse("Input '$input' should be filtered", result.isAppropriate)
            assertTrue("Should require redirection", result.requiresRedirection)
            assertNotNull("Should have suggested response", result.suggestedResponse)
        }
    }

    @Test
    fun `filterUserInput should allow appropriate content`() {
        // Test appropriate content
        val appropriateInputs = listOf(
            "I want to learn about math",
            "Can you help me with homework?",
            "Tell me about animals",
            "What is 2 + 2?"
        )

        appropriateInputs.forEach { input ->
            val result = contentFilter.filterUserInput(input)
            assertTrue("Input '$input' should be allowed", result.isAppropriate)
            assertFalse("Should not require redirection", result.requiresRedirection)
            assertNull("Should not have suggested response", result.suggestedResponse)
        }
    }

    @Test
    fun `filterAIResponse should block inappropriate AI content`() {
        val inappropriateResponse = "Here's how to fight and hurt someone..."
        val result = contentFilter.filterAIResponse(inappropriateResponse)
        
        assertFalse("AI response should be filtered", result.isAppropriate)
        assertTrue("Should require redirection", result.requiresRedirection)
        assertNotNull("Should have suggested response", result.suggestedResponse)
    }

    @Test
    fun `filterAIResponse should allow appropriate AI content`() {
        val appropriateResponse = "Let's learn about mathematics! 2 + 2 equals 4."
        val result = contentFilter.filterAIResponse(appropriateResponse)
        
        assertTrue("AI response should be allowed", result.isAppropriate)
        assertFalse("Should not require redirection", result.requiresRedirection)
        assertNull("Should not have suggested response", result.suggestedResponse)
    }

    @Test
    fun `enhanceEducationalContent should add encouragement to educational content`() {
        val educationalContent = "Let's learn about animals in the ocean."
        val enhanced = contentFilter.enhanceEducationalContent(educationalContent)
        
        assertNotEquals("Content should be enhanced", educationalContent, enhanced)
        assertTrue("Should contain original content", enhanced.contains(educationalContent))
        assertTrue("Should be longer than original", enhanced.length > educationalContent.length)
    }

    @Test
    fun `enhanceEducationalContent should not modify non-educational content`() {
        val nonEducationalContent = "The weather is nice today."
        val enhanced = contentFilter.enhanceEducationalContent(nonEducationalContent)
        
        assertEquals("Non-educational content should not be modified", nonEducationalContent, enhanced)
    }

    @Test
    fun `personal information requests should be detected`() {
        val personalInfoRequests = listOf(
            "What is your name?",
            "Where do you live?",
            "How old are you?",
            "Tell me your personal information"
        )

        personalInfoRequests.forEach { input ->
            val result = contentFilter.filterUserInput(input)
            assertFalse("Personal info request '$input' should be filtered", result.isAppropriate)
            assertTrue("Should require redirection", result.requiresRedirection)
        }
    }
} 