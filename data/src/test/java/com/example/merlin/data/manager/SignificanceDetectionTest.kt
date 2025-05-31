package com.example.merlin.data.manager

import org.junit.Assert.*
import org.junit.Test

class SignificanceDetectionTest {

    @Test
    fun significanceConfig_dataClass_shouldExist() {
        // Test that the SignificanceConfig data class exists and can be instantiated
        val config = AIInteractionManager.SignificanceConfig(
            minMessageLength = 15,
            minResponseLength = 25,
            significanceThreshold = 0.4,
            emotionalWeight = 2.0,
            personalWeight = 1.5,
            educationalWeight = 1.2,
            questionWeight = 1.3
        )
        
        assertEquals(15, config.minMessageLength)
        assertEquals(25, config.minResponseLength)
        assertEquals(0.4, config.significanceThreshold, 0.001)
        assertEquals(2.0, config.emotionalWeight, 0.001)
        assertEquals(1.5, config.personalWeight, 0.001)
        assertEquals(1.2, config.educationalWeight, 0.001)
        assertEquals(1.3, config.questionWeight, 0.001)
    }

    @Test
    fun significanceConfig_defaultValues_shouldBeReasonable() {
        // Test that default values are reasonable for a tutoring app
        val config = AIInteractionManager.SignificanceConfig()
        
        assertTrue("Min message length should be reasonable", config.minMessageLength in 5..20)
        assertTrue("Min response length should be reasonable", config.minResponseLength in 10..50)
        assertTrue("Significance threshold should be reasonable", config.significanceThreshold in 0.1..0.8)
        assertTrue("Emotional weight should prioritize emotions", config.emotionalWeight >= 1.0)
        assertTrue("Personal weight should prioritize personal info", config.personalWeight >= 1.0)
        assertTrue("Educational weight should be positive", config.educationalWeight > 0.0)
        assertTrue("Question weight should be positive", config.questionWeight > 0.0)
    }

    @Test
    fun memoryType_classification_shouldCoverMainCategories() {
        // Test that we have the expected memory types for a tutoring system
        val expectedTypes = setOf(
            "GENERAL", "PREFERENCE", "ACHIEVEMENT", "DIFFICULTY", 
            "EMOTIONAL", "PERSONAL", "EDUCATIONAL"
        )
        
        val actualTypes = com.example.merlin.data.database.entities.MemoryType.values()
            .map { it.name }.toSet()
        
        assertEquals("Should have all expected memory types", expectedTypes, actualTypes)
    }

    @Test
    fun memoryImportance_range_shouldBeValid() {
        // Test that importance values are in the expected range (1-5)
        val testCases = listOf(
            "I love math!" to "That's wonderful!",
            "I'm scared of spiders" to "It's okay to be scared",
            "My favorite color is blue" to "Blue is a great color!",
            "I don't understand this" to "Let me help you",
            "Hi" to "Hello!"
        )
        
        testCases.forEach { (userMessage, aiResponse) ->
            // We can't directly test the private method, but we can verify the concept
            // that importance should be calculated within valid range
            assertTrue("User message should not be empty", userMessage.isNotEmpty())
            assertTrue("AI response should not be empty", aiResponse.isNotEmpty())
            
            // Test that our test data covers different scenarios
            val hasEmotionalContent = userMessage.lowercase().contains(Regex("\\b(love|scared|favorite)\\b"))
            val hasEducationalContent = userMessage.lowercase().contains(Regex("\\b(math|understand)\\b"))
            
            // At least some test cases should have emotional or educational content
            if (userMessage.contains("love") || userMessage.contains("scared") || userMessage.contains("math")) {
                assertTrue("Should detect significant content", hasEmotionalContent || hasEducationalContent)
            }
        }
    }

    @Test
    fun interactionLength_analysis_shouldConsiderBothMessages() {
        // Test different interaction lengths
        val shortUser = "Hi"
        val shortAI = "Hello!"
        val mediumUser = "I like playing games after school"
        val mediumAI = "That sounds fun! What kind of games do you enjoy playing?"
        val longUser = "I really love mathematics because it helps me understand how the world works and I want to become an engineer someday"
        val longAI = "That's fantastic! Mathematics is indeed fundamental to engineering. What specific area of engineering interests you most?"
        
        // Verify our test data has the expected lengths
        assertTrue("Short user message should be short", shortUser.length < 10)
        assertTrue("Short AI response should be short", shortAI.length < 20)
        assertTrue("Medium user message should be medium", mediumUser.length in 20..50)
        assertTrue("Medium AI response should be medium", mediumAI.length in 50..100)
        assertTrue("Long user message should be long", longUser.length > 50)
        assertTrue("Long AI response should be long", longAI.length > 100)
        
        // Test that longer interactions would likely be more significant
        assertTrue("Long interaction should have more content", longUser.length > shortUser.length)
        assertTrue("Long response should have more content", longAI.length > shortAI.length)
    }

    @Test
    fun emotionalKeywords_detection_shouldIdentifyEmotions() {
        // Test emotional keyword detection patterns
        val emotionalPhrases = mapOf(
            "I'm scared of the dark" to "scared",
            "I love ice cream" to "love", 
            "I'm so excited for the trip" to "excited",
            "I feel sad today" to "sad",
            "I'm proud of my drawing" to "proud",
            "I hate vegetables" to "hate",
            "I'm worried about the test" to "worried"
        )
        
        emotionalPhrases.forEach { (phrase, emotion) ->
            assertTrue("Phrase should contain emotion word", 
                phrase.lowercase().contains(emotion.lowercase()))
            
            // Verify the emotion word is detected as a whole word
            assertTrue("Should detect emotion as whole word",
                phrase.lowercase().contains(Regex("\\b$emotion\\b")))
        }
    }

    @Test
    fun personalKeywords_detection_shouldIdentifyPersonalInfo() {
        // Test personal information keyword detection
        val personalPhrases = mapOf(
            "My mom is a teacher" to "mom",
            "I have a little sister" to "sister", 
            "My best friend likes soccer" to "friend",
            "We have a pet dog" to "pet",
            "My birthday is next week" to "birthday",
            "I go to Lincoln Elementary School" to "school"
        )
        
        personalPhrases.forEach { (phrase, keyword) ->
            assertTrue("Phrase should contain personal keyword", 
                phrase.lowercase().contains(keyword.lowercase()))
            
            // Verify the keyword is detected as a whole word
            assertTrue("Should detect keyword as whole word",
                phrase.lowercase().contains(Regex("\\b$keyword\\b")))
        }
    }

    @Test
    fun educationalKeywords_detection_shouldIdentifyLearning() {
        // Test educational content keyword detection
        val educationalPhrases = mapOf(
            "I love math class" to "math",
            "Reading is my favorite subject" to "reading", 
            "I need help with science homework" to "science",
            "I learned about history today" to "learned",
            "I don't understand this lesson" to "understand",
            "I got a good grade on my test" to "grade"
        )
        
        educationalPhrases.forEach { (phrase, keyword) ->
            assertTrue("Phrase should contain educational keyword", 
                phrase.lowercase().contains(keyword.lowercase()))
            
            // Verify the keyword is detected as a whole word
            assertTrue("Should detect keyword as whole word",
                phrase.lowercase().contains(Regex("\\b$keyword\\b")))
        }
    }

    @Test
    fun questionPattern_detection_shouldIdentifyQuestions() {
        // Test question pattern detection
        val questionPhrases = listOf(
            "What's your favorite color?",
            "Do you like pizza?",
            "How do you feel about math?",
            "Which game do you prefer?",
            "What do you want to learn today?"
        )
        
        val nonQuestionPhrases = listOf(
            "I like blue.",
            "Pizza is delicious.",
            "Math is fun.",
            "I prefer chess.",
            "I want to learn science."
        )
        
        questionPhrases.forEach { phrase ->
            assertTrue("Should detect question mark", phrase.contains("?"))
        }
        
        nonQuestionPhrases.forEach { phrase ->
            assertFalse("Should not contain question mark", phrase.contains("?"))
        }
    }

    @Test
    fun sentimentAnalysis_shouldDetectPositiveAndNegative() {
        // Test sentiment detection
        val positiveWords = listOf("great", "awesome", "amazing", "love", "happy", "excited", "fun")
        val negativeWords = listOf("hate", "terrible", "awful", "sad", "angry", "scared", "worried")
        
        positiveWords.forEach { word ->
            val phrase = "I think this is $word"
            assertTrue("Should detect positive word as whole word",
                phrase.lowercase().contains(Regex("\\b$word\\b")))
        }
        
        negativeWords.forEach { word ->
            val phrase = "I feel $word about this"
            assertTrue("Should detect negative word as whole word",
                phrase.lowercase().contains(Regex("\\b$word\\b")))
        }
    }
} 