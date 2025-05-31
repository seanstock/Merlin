package com.example.merlin.data.manager

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.ToolId
import java.util.LinkedList

/**
 * Manages a rolling window of conversation history for API calls.
 * Ensures the context size does not exceed a specified maximum, preserving a system prompt if present.
 */
class ConversationContextManager(private val maxSize: Int = 20) {

    private val history: LinkedList<ChatMessage> = LinkedList()

    /**
     * Initializes the conversation with a system prompt, clearing any existing history.
     */
    fun initializeWithSystemPrompt(systemPromptContent: String) {
        clearHistory()
        if (systemPromptContent.isNotBlank()) {
            val systemMessage = ChatMessage(
                role = ChatRole.System,
                messageContent = TextContent(systemPromptContent)
            )
            history.addFirst(systemMessage)
        }
    }

    /**
     * Adds a message to the history, maintaining the rolling window.
     * If a system prompt is present, it is preserved at the beginning, and the rolling window
     * applies to subsequent messages up to maxSize - 1.
     */
    fun addMessage(message: ChatMessage) {
        val systemPrompt = if (history.isNotEmpty() && history.first.role == ChatRole.System) history.first else null
        val effectiveMaxSize = if (systemPrompt != null) maxSize -1 else maxSize

        if (systemPrompt != null) {
            history.removeFirst() // Temporarily remove system prompt to manage the rolling window for other messages
        }

        history.add(message)

        // Maintain rolling window for non-system messages
        while (history.size > effectiveMaxSize && history.isNotEmpty()) {
            history.removeFirst()
        }

        if (systemPrompt != null) {
            history.addFirst(systemPrompt) // Re-add system prompt at the beginning
        }
    }

    /**
     * Convenience method to add a user message.
     */
    fun addUserMessage(content: String) {
        addMessage(ChatMessage(role = ChatRole.User, messageContent = TextContent(content)))
    }

    /**
     * Convenience method to add an assistant's response, potentially including tool calls.
     */
    fun addAssistantMessage(content: String?, toolCalls: List<ToolCall>? = null) {
        addMessage(ChatMessage(
            role = ChatRole.Assistant, 
            messageContent = content?.let { TextContent(it) },
            toolCalls = toolCalls
        ))
    }
    
    /**
     * Adds a message with the "tool" role, used for providing results of a function call.
     */
    fun addToolMessage(toolCallId: String, content: String) {
        addMessage(ChatMessage(
            role = ChatRole.Tool, 
            toolCallId = ToolId(toolCallId), 
            messageContent = TextContent(content)
        ))
    }

    /**
     * Returns a copy of the current conversation history.
     */
    fun getFormattedHistory(): List<ChatMessage> {
        return LinkedList(history) // Return a defensive copy
    }

    /**
     * Clears all messages from the history.
     */
    fun clearHistory() {
        history.clear()
    }
} 