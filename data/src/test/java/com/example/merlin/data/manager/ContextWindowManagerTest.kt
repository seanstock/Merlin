package com.example.merlin.data.manager

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionTool
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolType
import com.aallam.openai.api.core.Parameters
import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import com.example.merlin.data.manager.MemoryRetriever.ScoredMemory
import com.example.merlin.data.remote.OpenAIClientWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ContextWindowManagerTest {

    private lateinit var contextWindowManager: ContextWindowManager
    private val maxTokens = 1000
    private val reservedTokens = 200
    private val defaultSystemPrompt = "You are a helpful AI assistant."

    @Before
    fun setUp() {
        contextWindowManager = ContextWindowManager(
            maxTokens = maxTokens,
            reservedTokensForResponse = reservedTokens
        )
    }

    // Helper function to create a ChatMessage with TextContent
    private fun createTextMessage(role: ChatRole, content: String): ChatMessage {
        return ChatMessage(role = role, messageContent = TextContent(content))
    }

    // Helper function to create a sample FunctionTool
    private fun createTestFunctionTool(name: String = "test_function"): FunctionTool {
        val parametersJson = buildJsonObject { put("type", "object") }
        return FunctionTool(
            name = name,
            description = "A test function",
            parameters = Parameters(parametersJson)
        )
    }

    @Test
    fun contextConfig_dataClass_shouldExist() {
        // Test that ContextConfig data class exists and can be instantiated
        val config = ContextWindowManager.ContextConfig(
            maxTokens = 2000,
            reservedTokensForResponse = 300,
            prioritizeRecentMessages = false,
            includeMemories = false,
            maxMemories = 3,
            minMessagesRequired = 2
        )
        
        assertEquals(2000, config.maxTokens)
        assertEquals(300, config.reservedTokensForResponse)
        assertFalse(config.prioritizeRecentMessages)
        assertFalse(config.includeMemories)
        assertEquals(3, config.maxMemories)
        assertEquals(2, config.minMessagesRequired)
    }

    @Test
    fun contextConfig_defaultValues_shouldBeReasonable() {
        // Test that default values are reasonable
        val config = ContextWindowManager.ContextConfig()
        
        assertTrue("Max tokens should be positive", config.maxTokens > 0)
        assertTrue("Reserved tokens should be positive", config.reservedTokensForResponse > 0)
        assertTrue("Reserved tokens should be less than max", config.reservedTokensForResponse < config.maxTokens)
        assertTrue("Should prioritize recent messages by default", config.prioritizeRecentMessages)
        assertTrue("Should include memories by default", config.includeMemories)
        assertTrue("Max memories should be reasonable", config.maxMemories in 3..10)
        assertTrue("Min messages should be reasonable", config.minMessagesRequired in 1..5)
    }

    @Test
    fun contextWindowResult_dataClass_shouldExist() {
        // Test that ContextWindowResult data class exists
        val result = ContextWindowManager.ContextWindowResult(
            optimizedMessages = emptyList(),
            includedMemoryContext = "test context",
            totalTokens = 500,
            droppedMessages = 2,
            droppedMemories = 1
        )
        
        assertEquals(emptyList<ChatMessage>(), result.optimizedMessages)
        assertEquals("test context", result.includedMemoryContext)
        assertEquals(500, result.totalTokens)
        assertEquals(2, result.droppedMessages)
        assertEquals(1, result.droppedMemories)
    }

    @Test
    fun tokenUsageStats_dataClass_shouldExist() {
        // Test that TokenUsageStats data class exists
        val stats = ContextWindowManager.TokenUsageStats(
            messageTokens = 300,
            memoryTokens = 100,
            functionTokens = 200,
            totalTokens = 600,
            availableTokens = 400,
            utilizationPercentage = 60
        )
        
        assertEquals(300, stats.messageTokens)
        assertEquals(100, stats.memoryTokens)
        assertEquals(200, stats.functionTokens)
        assertEquals(600, stats.totalTokens)
        assertEquals(400, stats.availableTokens)
        assertEquals(60, stats.utilizationPercentage)
    }

    @Test
    fun getTokenUsageStats_withEmptyInput_shouldReturnZeroStats() {
        // Test token usage calculation with empty inputs
        val stats = contextWindowManager.getTokenUsageStats(
            chatMessages = emptyList(),
            memoryContext = null,
            functionTools = emptyList()
        )
        
        assertEquals(0, stats.messageTokens)
        assertEquals(0, stats.memoryTokens)
        assertEquals(0, stats.functionTokens)
        assertEquals(0, stats.totalTokens)
        assertEquals(maxTokens, stats.availableTokens)
        assertEquals(0, stats.utilizationPercentage)
    }

    @Test
    fun getTokenUsageStats_withMessages_shouldCalculateCorrectly() {
        // Test token usage calculation with messages
        val messages = listOf(
            createTextMessage(ChatRole.System, "You are a helpful assistant."),
            createTextMessage(ChatRole.User, "Hello!"),
            createTextMessage(ChatRole.Assistant, "Hi there! How can I help you today?")
        )
        
        val stats = contextWindowManager.getTokenUsageStats(
            chatMessages = messages,
            memoryContext = null,
            functionTools = emptyList()
        )
        
        assertTrue("Message tokens should be positive", stats.messageTokens > 0)
        assertEquals(0, stats.memoryTokens)
        assertEquals(0, stats.functionTokens)
        assertEquals(stats.messageTokens, stats.totalTokens)
        assertEquals(maxTokens - stats.totalTokens, stats.availableTokens)
        assertTrue("Utilization should be reasonable", stats.utilizationPercentage in 0..100)
    }

    @Test
    fun getTokenUsageStats_withMemoryContext_shouldIncludeMemoryTokens() {
        // Test token usage calculation with memory context
        val memoryContext = "Previous conversation: Child loves math games and struggles with fractions."
        
        val stats = contextWindowManager.getTokenUsageStats(
            chatMessages = emptyList(),
            memoryContext = memoryContext,
            functionTools = emptyList()
        )
        
        assertEquals(0, stats.messageTokens)
        assertTrue("Memory tokens should be positive", stats.memoryTokens > 0)
        assertEquals(0, stats.functionTokens)
        assertEquals(stats.memoryTokens, stats.totalTokens)
    }

    @Test
    fun getTokenUsageStats_withFunctionTools_shouldIncludeFunctionTokens() {
        // Test token usage calculation with function tools
        val functionTool = createTestFunctionTool()
        val functionTools = listOf(functionTool)
        
        val stats = contextWindowManager.getTokenUsageStats(
            chatMessages = emptyList(),
            memoryContext = null,
            functionTools = functionTools
        )
        
        assertEquals(0, stats.messageTokens)
        assertEquals(0, stats.memoryTokens)
        assertTrue("Function tokens should be positive", stats.functionTokens > 0)
        assertEquals(stats.functionTokens, stats.totalTokens)
    }

    @Test
    fun needsOptimization_withSmallContext_shouldReturnFalse() {
        // Test that small contexts don't need optimization
        val messages = listOf(
            createTextMessage(ChatRole.User, "Hi"),
            createTextMessage(ChatRole.Assistant, "Hello!")
        )
        
        val needsOptimization = contextWindowManager.needsOptimization(
            chatMessages = messages,
            memoryContext = null,
            functionTools = emptyList()
        )
        
        assertFalse("Small context should not need optimization", needsOptimization)
    }

    @Test
    fun needsOptimization_withLargeContext_shouldReturnTrue() {
        // Test that large contexts need optimization
        val largeContent = "This is a very long message that contains a lot of text. ".repeat(50)
        val messages = (1..20).map { index ->
            createTextMessage(if (index % 2 == 0) ChatRole.User else ChatRole.Assistant, largeContent)
        }
        
        val needsOptimization = contextWindowManager.needsOptimization(
            chatMessages = messages,
            memoryContext = null,
            functionTools = emptyList()
        )
        
        assertTrue("Large context should need optimization", needsOptimization)
    }

    @Test
    fun optimizeContextWindow_emptyInput_shouldReturnEmptyResult() = runBlocking {
        val result = contextWindowManager.optimizeContextWindow(
            chatMessages = emptyList(),
            relevantMemories = emptyList(),
            functionTools = emptyList(),
            memoryContext = null
        )
        
        assertTrue(result.optimizedMessages.isEmpty())
        assertNull(result.includedMemoryContext)
        assertEquals(0, result.totalTokens)
        assertEquals(0, result.droppedMessages)
        assertEquals(0, result.droppedMemories)
    }

    @Test
    fun optimizeContextWindow_withSystemMessage_shouldPreserveSystemMessage() = runBlocking {
        val systemMessage = createTextMessage(ChatRole.System, defaultSystemPrompt)
        val userMessage = createTextMessage(ChatRole.User, "Tell me a joke.")
        
        val result = contextWindowManager.optimizeContextWindow(
            chatMessages = listOf(systemMessage, userMessage),
            memoryContext = null
        )
        
        assertEquals(2, result.optimizedMessages.size)
        assertEquals(ChatRole.System, result.optimizedMessages.first().role)
        assertEquals(defaultSystemPrompt, (result.optimizedMessages.first().messageContent as? TextContent)?.content)
    }

    @Test
    fun optimizeContextWindow_withTokenLimit_shouldRespectLimits() = runBlocking {
        val longMessageContent = "This is a very, very long message. ".repeat(100) // Creates content > maxTokens
        val messages = listOf(
            createTextMessage(ChatRole.System, defaultSystemPrompt),
            createTextMessage(ChatRole.User, longMessageContent),
            createTextMessage(ChatRole.Assistant, "Okay, here is a very long answer... ".repeat(50))
        )
        
        val result = contextWindowManager.optimizeContextWindow(chatMessages = messages)
        
        assertTrue("Total tokens should not exceed max configured tokens", result.totalTokens <= maxTokens - reservedTokens)
        assertTrue("Should have dropped some messages", result.droppedMessages > 0 || messages.size < 2) // Allow if only system message kept
        // Ensure system prompt is kept if possible
        if (result.optimizedMessages.isNotEmpty()) {
            assertEquals(ChatRole.System, result.optimizedMessages.first().role)
        }
    }

    @Test
    fun optimizeContextWindow_withMemories_shouldIncludeMemoryContext() = runBlocking {
        val memoryText = "Child likes red balloons."
        val memory = Memory(
            id = 0L,
            childId = "child1",
            ts = System.currentTimeMillis(),
            text = memoryText,
            type = MemoryType.PREFERENCE,
            sentiment = 0.8,
            importance = 4
        )
        val scoredMemory = ScoredMemory(memory, 0.9)
        
        val configWithMemories = ContextWindowManager.ContextConfig(includeMemories = true, maxMemories = 1)
        val windowManagerWithMemories = ContextWindowManager(
            maxTokens = maxTokens, 
            reservedTokensForResponse = reservedTokens, 
            // memoryRetriever = mockMemoryRetriever // You would mock this
        )

        // Simulate memoryRetriever behavior if not fully mocking
        val messages = listOf(createTextMessage(ChatRole.User, "What toy should I get?"))
        
        val result = windowManagerWithMemories.optimizeContextWindow(
            chatMessages = messages,
            // relevantMemories = listOf(scoredMemory), // This would come from a mocked retriever
            memoryContext = memoryText // Directly provide memoryContext for this test
        )
        
        assertEquals(memoryText, result.includedMemoryContext)
        assertTrue("Total tokens should account for memory", result.totalTokens > 0)
    }

    @Test
    fun optimizeContextWindow_recentMessagesPrioritized() = runBlocking {
        val messages = mutableListOf<ChatMessage>()
        messages.add(createTextMessage(ChatRole.System, defaultSystemPrompt))
        for (i in 1..10) {
            messages.add(createTextMessage(ChatRole.User, "User message $i"))
            messages.add(createTextMessage(ChatRole.Assistant, "Assistant response $i"))
        }
        // Make the last few messages very long to force dropping older ones
        messages.add(createTextMessage(ChatRole.User, "Very important recent question?".repeat(20)))
        messages.add(createTextMessage(ChatRole.Assistant, "Crucial recent answer!".repeat(20)))

        val result = contextWindowManager.optimizeContextWindow(chatMessages = messages)

        assertTrue("Should have some messages", result.optimizedMessages.isNotEmpty())
        // Check if the last (non-system) messages are present
        val lastUserMsgContent = (messages[messages.size - 2].messageContent as TextContent).content
        val lastAsstMsgContent = (messages.last().messageContent as TextContent).content

        val optimizedContents = result.optimizedMessages.mapNotNull { (it.messageContent as? TextContent)?.content }

        assertTrue("Optimized messages should contain the last user message content", optimizedContents.contains(lastUserMsgContent))
        assertTrue("Optimized messages should contain the last assistant message content", optimizedContents.contains(lastAsstMsgContent))
        if (result.optimizedMessages.any { it.role == ChatRole.System }) {
             assertEquals(defaultSystemPrompt, (result.optimizedMessages.first{it.role == ChatRole.System}.messageContent as? TextContent)?.content)
        }
    }

    @Test
    fun optimizeContextWindow_withFunctionTools_shouldAccountForTokens() = runBlocking {
        val funcTool = createTestFunctionTool()
        val messages = listOf(createTextMessage(ChatRole.User, "Call the test function."))

        val result = contextWindowManager.optimizeContextWindow(
            chatMessages = messages,
            functionTools = listOf(funcTool)
        )

        val stats = contextWindowManager.getTokenUsageStats(messages, null, listOf(funcTool))
        assertEquals(stats.totalTokens, result.totalTokens)
        assertTrue("Resulting messages should not be empty if space allows", result.optimizedMessages.isNotEmpty() || stats.functionTokens >= maxTokens - reservedTokens)
    }
} 