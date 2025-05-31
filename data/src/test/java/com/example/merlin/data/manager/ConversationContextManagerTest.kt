package com.example.merlin.data.manager

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.ToolId
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConversationContextManagerTest {

    private lateinit var contextManager: ConversationContextManager
    private val maxSize = 5 // Using a smaller maxSize for easier testing

    @Before
    fun setup() {
        contextManager = ConversationContextManager(maxSize = maxSize)
    }

    @Test
    fun `initializeWithSystemPrompt adds system prompt correctly`() {
        val systemPrompt = "You are a helpful assistant."
        contextManager.initializeWithSystemPrompt(systemPrompt)
        val history = contextManager.getFormattedHistory()
        assertEquals(1, history.size)
        assertEquals(ChatRole.System, history.first().role)
        assertEquals(systemPrompt, (history.first().messageContent as? TextContent)?.content)
    }

    @Test
    fun `addMessage adds user message`() {
        contextManager.addUserMessage("Hello there!")
        val history = contextManager.getFormattedHistory()
        assertEquals(1, history.size)
        assertEquals(ChatRole.User, history.last().role)
        assertEquals("Hello there!", (history.last().messageContent as? TextContent)?.content)
    }

    @Test
    fun `addMessage adds assistant message`() {
        contextManager.addAssistantMessage("Hi! How can I help?")
        val history = contextManager.getFormattedHistory()
        assertEquals(1, history.size)
        assertEquals(ChatRole.Assistant, history.last().role)
        assertEquals("Hi! How can I help?", (history.last().messageContent as? TextContent)?.content)
    }

    @Test
    fun `rolling window works correctly without system prompt`() {
        for (i in 1..(maxSize + 2)) {
            contextManager.addUserMessage("Message $i")
        }
        val history = contextManager.getFormattedHistory()
        assertEquals(maxSize, history.size)
        assertEquals("Message 3", (history.first().messageContent as? TextContent)?.content)
        assertEquals("Message ${maxSize + 2}", (history.last().messageContent as? TextContent)?.content)
    }

    @Test
    fun `rolling window works correctly with system prompt`() {
        val systemPrompt = "System instructions."
        contextManager.initializeWithSystemPrompt(systemPrompt)

        for (i in 1..(maxSize + 1)) { // Add maxSize+1 user/assistant messages
            contextManager.addUserMessage("User message $i")
            contextManager.addAssistantMessage("Assistant response $i")
        }

        val history = contextManager.getFormattedHistory()
        assertEquals(maxSize, history.size)
        assertEquals(ChatRole.System, history.first().role)
        assertEquals(systemPrompt, (history.first().messageContent as? TextContent)?.content)

        val nonSystemMessages = history.drop(1)
        assertEquals(maxSize - 1, nonSystemMessages.size)
        assertEquals("Assistant response ${maxSize + 1}", (nonSystemMessages.last().messageContent as? TextContent)?.content)
    }

    @Test
    fun `clearHistory empties the context`() {
        contextManager.addUserMessage("Message 1")
        contextManager.addAssistantMessage("Response 1")
        contextManager.clearHistory()
        assertEquals(0, contextManager.getFormattedHistory().size)
    }

    @Test
    fun `initializeWithSystemPrompt clears previous history`() {
        contextManager.addUserMessage("Old message")
        contextManager.initializeWithSystemPrompt("New system prompt")
        val history = contextManager.getFormattedHistory()
        assertEquals(1, history.size)
        assertEquals("New system prompt", (history.first().messageContent as? TextContent)?.content)
    }
    
    @Test
    fun `addToolMessage adds tool message correctly`() {
        val toolCallIdString = "tool_abc123"
        val toolContent = "Result of the tool call."
        contextManager.addToolMessage(toolCallIdString, toolContent)
        val history = contextManager.getFormattedHistory()
        assertEquals(1, history.size)
        assertEquals(ChatRole.Tool, history.last().role)
        assertEquals(ToolId(toolCallIdString), history.last().toolCallId)
        assertEquals(toolContent, (history.last().messageContent as? TextContent)?.content)
    }
} 