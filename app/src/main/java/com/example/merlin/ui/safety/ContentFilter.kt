package com.example.merlin.ui.safety

import com.example.merlin.ui.chat.ChatScreenPerformance

/**
 * Lightweight content filtering system for child safety.
 * Provides basic protection against inappropriate content while maintaining educational focus.
 * Optimized with caching for improved performance.
 */
class ContentFilter {
    
    companion object {
        private const val TAG = "ContentFilter"
        
        // Lightweight logging function that works in both Android and unit test environments
        private fun logWarning(message: String) {
            try {
                // Use Android Log in production, fallback to println in tests
                android.util.Log.w(TAG, message)
            } catch (e: RuntimeException) {
                // In unit tests where Log is not mocked, use println
                println("ContentFilter WARNING: $message")
            }
        }
        
        // Inappropriate content patterns (basic MVP implementation)
        private val INAPPROPRIATE_KEYWORDS = setOf(
            // Violence and weapons
            "violence", "violent", "fight", "hit", "punch", "kick", "hurt", "weapon", "gun", "knife",
            "bomb", "war", "kill", "death", "die", "blood", "attack", "murder",
            
            // Adult content
            "sex", "sexual", "nude", "naked", "adult", "mature", "romance", "dating",
            
            // Scary content
            "scary", "horror", "ghost", "monster", "nightmare", "frightening", "terrifying",
            "creepy", "evil", "demon", "devil",
            
            // Inappropriate behavior
            "lie", "steal", "cheat", "bully", "mean", "hate", "stupid", "dumb", "idiot",
            "shut up", "go away",
            
            // Substances
            "drug", "alcohol", "beer", "wine", "cigarette", "smoke", "drunk",
            
            // Personal information requests
            "address", "phone number", "password", "credit card", "social security",
            "full name", "where do you live", "what school"
        )
        
        // Educational encouragement keywords
        private val EDUCATIONAL_KEYWORDS = setOf(
            "learn", "study", "practice", "explore", "discover", "understand", "think",
            "create", "imagine", "build", "solve", "question", "curious", "wonder",
            "math", "science", "reading", "writing", "art", "music", "nature",
            "animals", "plants", "space", "ocean", "friendship", "kindness", "sharing"
        )
        
        // Positive reinforcement phrases for redirection
        private val REDIRECTION_RESPONSES = listOf(
            "Let's explore something fun and educational instead! What would you like to learn about today?",
            "I love helping with learning! How about we try a fun educational game or talk about something interesting?",
            "That's not something I can help with, but I have lots of fun learning activities! What subjects do you enjoy?",
            "Let's focus on something positive and educational! Would you like to learn about animals, space, math, or something else?",
            "I'm here to help you learn and grow! What's something new you'd like to discover today?",
            "How about we try something educational and fun? I can help with homework, games, or interesting facts!"
        )
    }
    
    /**
     * Filters user input for inappropriate content with performance optimization.
     * @param input The user's message
     * @return FilterResult indicating if content is appropriate and any necessary actions
     */
    fun filterUserInput(input: String): FilterResult {
        val cleanInput = input.lowercase().trim()
        
        // ⚡ PERFORMANCE OPTIMIZATION: Check cache first
        val cachedResult = ChatScreenPerformance.FilteringOptimization.getCachedFilterResult(cleanInput)
        if (cachedResult != null) {
            return if (cachedResult) {
                FilterResult(
                    isAppropriate = true,
                    reason = "Content approved (cached)",
                    suggestedResponse = null,
                    requiresRedirection = false
                )
            } else {
                FilterResult(
                    isAppropriate = false,
                    reason = "Inappropriate content detected (cached)",
                    suggestedResponse = REDIRECTION_RESPONSES.random(),
                    requiresRedirection = true
                )
            }
        }
        
        // Check for inappropriate keywords
        val foundInappropriate = INAPPROPRIATE_KEYWORDS.any { keyword ->
            cleanInput.contains(keyword)
        }
        
        if (foundInappropriate) {
            logWarning("Inappropriate content detected in user input")
            val result = FilterResult(
                isAppropriate = false,
                reason = "Inappropriate content detected",
                suggestedResponse = REDIRECTION_RESPONSES.random(),
                requiresRedirection = true
            )
            
            // Cache the result for future performance
            ChatScreenPerformance.FilteringOptimization.cacheFilterResult(cleanInput, false)
            return result
        }
        
        // Check for personal information requests
        if (containsPersonalInfoRequest(cleanInput)) {
            logWarning("Personal information request detected")
            val result = FilterResult(
                isAppropriate = false,
                reason = "Personal information request",
                suggestedResponse = "I can't ask for or share personal information. Let's talk about fun learning topics instead!",
                requiresRedirection = true
            )
            
            // Cache the result for future performance
            ChatScreenPerformance.FilteringOptimization.cacheFilterResult(cleanInput, false)
            return result
        }
        
        val result = FilterResult(
            isAppropriate = true,
            reason = "Content approved",
            suggestedResponse = null,
            requiresRedirection = false
        )
        
        // Cache the positive result
        ChatScreenPerformance.FilteringOptimization.cacheFilterResult(cleanInput, true)
        return result
    }
    
    /**
     * Filters AI responses for child appropriateness with performance optimization.
     * @param response The AI's response
     * @return FilterResult indicating if response is appropriate
     */
    fun filterAIResponse(response: String): FilterResult {
        val cleanResponse = response.lowercase().trim()
        
        // ⚡ PERFORMANCE OPTIMIZATION: Check cache first
        val cachedResult = ChatScreenPerformance.FilteringOptimization.getCachedFilterResult(cleanResponse)
        if (cachedResult != null) {
            return if (cachedResult) {
                FilterResult(
                    isAppropriate = true,
                    reason = "Response approved (cached)",
                    suggestedResponse = null,
                    requiresRedirection = false
                )
            } else {
                FilterResult(
                    isAppropriate = false,
                    reason = "AI response contains inappropriate content (cached)",
                    suggestedResponse = "I apologize, but I need to think of a better way to help you with that. Let's try something educational and fun instead!",
                    requiresRedirection = true
                )
            }
        }
        
        // Check for inappropriate content in AI response
        val foundInappropriate = INAPPROPRIATE_KEYWORDS.any { keyword ->
            cleanResponse.contains(keyword)
        }
        
        if (foundInappropriate) {
            logWarning("Inappropriate content detected in AI response")
            val result = FilterResult(
                isAppropriate = false,
                reason = "AI response contains inappropriate content",
                suggestedResponse = "I apologize, but I need to think of a better way to help you with that. Let's try something educational and fun instead!",
                requiresRedirection = true
            )
            
            // Cache the result for future performance
            ChatScreenPerformance.FilteringOptimization.cacheFilterResult(cleanResponse, false)
            return result
        }
        
        val result = FilterResult(
            isAppropriate = true,
            reason = "Response approved",
            suggestedResponse = null,
            requiresRedirection = false
        )
        
        // Cache the positive result
        ChatScreenPerformance.FilteringOptimization.cacheFilterResult(cleanResponse, true)
        return result
    }
    
    /**
     * Enhances educational content by adding encouraging elements.
     * @param response The original response
     * @return Enhanced response with educational encouragement
     */
    fun enhanceEducationalContent(response: String): String {
        val hasEducationalKeywords = EDUCATIONAL_KEYWORDS.any { keyword ->
            response.lowercase().contains(keyword)
        }
        
        return if (hasEducationalKeywords) {
            // Add encouraging educational enhancement
            val encouragements = listOf(
                "Great question! ",
                "I love that you're curious about this! ",
                "What an interesting topic to explore! ",
                "You're such a good learner! "
            )
            encouragements.random() + response
        } else {
            response
        }
    }
    
    /**
     * Checks if the input contains requests for personal information.
     */
    private fun containsPersonalInfoRequest(input: String): Boolean {
        val personalInfoPatterns = listOf(
            "what.*your.*name",
            "where.*you.*live",
            "how.*old.*are.*you",
            "what.*your.*address",
            "give.*me.*your",
            "tell.*me.*your.*personal",
            "what.*school.*do.*you",
            "where.*do.*you.*go.*to.*school"
        )
        
        return personalInfoPatterns.any { pattern ->
            input.contains(Regex(pattern))
        }
    }
    
    /**
     * Result of content filtering operation.
     */
    data class FilterResult(
        val isAppropriate: Boolean,
        val reason: String,
        val suggestedResponse: String?,
        val requiresRedirection: Boolean
    )
} 