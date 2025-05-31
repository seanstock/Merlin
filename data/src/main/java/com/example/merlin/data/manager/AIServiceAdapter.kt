package com.example.merlin.data.manager

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.FunctionTool
import com.example.merlin.data.model.openaidl.MerlinAIResponse
import com.example.merlin.data.remote.OpenAIClientWrapper

/**
 * Simple interface for AI operations used internally by AIInteractionManager.
 * This provides a clean abstraction without breaking existing functionality.
 */
interface AIServiceInterface {
    fun isInitialized(): Boolean
    suspend fun getChatCompletionWithFunctions(
        messages: List<ChatMessage>,
        functionTools: List<FunctionTool>?
    ): MerlinAIResponse?
    
    suspend fun getChatCompletionWithMemoryContext(
        messages: List<ChatMessage>,
        functionTools: List<FunctionTool>?,
        memoryContext: String?
    ): MerlinAIResponse?
}

/**
 * Adapter that wraps OpenAIClientWrapper to implement the service interface.
 * This is the first step in creating a clean service boundary.
 */
class OpenAIServiceAdapter(
    private val openAIClient: OpenAIClientWrapper
) : AIServiceInterface {
    
    override fun isInitialized(): Boolean {
        return openAIClient.isInitialized()
    }
    
    override suspend fun getChatCompletionWithFunctions(
        messages: List<ChatMessage>,
        functionTools: List<FunctionTool>?
    ): MerlinAIResponse? {
        return openAIClient.getChatCompletionWithFunctions(messages, functionTools)
    }
    
    override suspend fun getChatCompletionWithMemoryContext(
        messages: List<ChatMessage>,
        functionTools: List<FunctionTool>?,
        memoryContext: String?
    ): MerlinAIResponse? {
        return openAIClient.getChatCompletionWithMemoryContext(messages, functionTools, memoryContext)
    }
} 