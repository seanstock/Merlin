package com.example.merlin.ai

import android.content.Context

/**
 * Provides AI service instances based on configuration.
 * This allows switching between local and remote AI implementations
 * as part of the Learning-as-a-Service architecture.
 */
object AIServiceProvider {
    
    private var _aiService: AIService? = null
    
    /**
     * Get the current AI service instance.
     * Returns null if no service has been set - the actual implementation
     * should be provided by the data module.
     */
    fun getAIService(context: Context): AIService? {
        return _aiService
    }
    
    /**
     * Set a custom AI service implementation.
     * Used for dependency injection and providing the actual implementation.
     */
    fun setAIService(aiService: AIService?) {
        _aiService = aiService
    }
    
    /**
     * Clear the cached service instance
     */
    fun clearCache() {
        _aiService = null
    }
} 