package com.example.merlin.ai

/**
 * Abstract interface for AI services that can be implemented by different providers
 * (OpenAI, Claude, Gemini, etc.). This provides a clean boundary for Learning-as-a-Service
 * architecture where AI processing can be done locally or remotely.
 */
interface AIService {
    
    /**
     * Check if the AI service is properly initialized and ready to use
     * @return true if the service can handle requests, false otherwise
     */
    fun isInitialized(): Boolean
    
    /**
     * Generate a chat completion with optional function calling support
     * @param messages The conversation history
     * @param functionTools Optional function tools the AI can call
     * @return AI response or null if there was an error
     */
    suspend fun getChatCompletionWithFunctions(
        messages: List<AIMessage>,
        functionTools: List<AIFunctionTool>?
    ): AIResponse?
    
    /**
     * Generate a chat completion with additional memory context
     * @param messages The conversation history
     * @param functionTools Optional function tools the AI can call
     * @param memoryContext Additional context from previous conversations
     * @return AI response or null if there was an error
     */
    suspend fun getChatCompletionWithMemoryContext(
        messages: List<AIMessage>,
        functionTools: List<AIFunctionTool>?,
        memoryContext: String?
    ): AIResponse?
} 