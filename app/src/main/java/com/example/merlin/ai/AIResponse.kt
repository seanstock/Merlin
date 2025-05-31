package com.example.merlin.ai

/**
 * Domain-level AI response that abstracts away provider-specific details.
 * This is equivalent to MerlinAIResponse but in the app module for clean architecture.
 * Can be used with any AI provider (OpenAI, Claude, Gemini, etc.).
 */
data class AIResponse(
    /**
     * The main textual content of the AI's response
     */
    val content: String?,
    
    /**
     * Name of the function the AI wants to call (if function calling is enabled)
     */
    val functionCallName: String?,
    
    /**
     * JSON string representation of arguments for the function call
     */
    val functionCallArguments: String?
) 