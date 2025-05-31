package com.example.merlin.data

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.FunctionTool
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolType
import com.aallam.openai.api.core.Parameters
import org.junit.Test
import org.junit.Assert.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Simple test to verify OpenAI API imports and basic usage work correctly.
 */
class OpenAIApiTest {

    @Test
    fun testChatMessageCreation() {
        val message = ChatMessage(
            role = ChatRole.User,
            messageContent = TextContent("Hello, world!")
        )
        
        assertEquals(ChatRole.User, message.role)
        assertTrue("Content should be TextContent", message.messageContent is TextContent)
        assertEquals("Hello, world!", (message.messageContent as? TextContent)?.content)
    }

    @Test
    fun testFunctionToolCreation() {
        val parametersJson = buildJsonObject {
            put("type", "object")
        }
        val parameters = Parameters(parametersJson)
        
        val functionTool = FunctionTool(
            name = "test_function",
            description = "A test function",
            parameters = parameters
        )
        
        assertEquals("test_function", functionTool.name)
        assertEquals("A test function", functionTool.description)
    }

    @Test
    fun testToolCreation() {
        val parametersJson = buildJsonObject {
            put("type", "object")
        }
        val parameters = Parameters(parametersJson)
        
        val functionTool = FunctionTool(
            name = "test_function",
            description = "A test function",
            parameters = parameters
        )
        
        val tool = Tool(
            type = ToolType("function"),
            function = functionTool
        )
        
        assertEquals("function", tool.type.value)
        assertEquals("test_function", tool.function?.name)
    }
} 