package com.example.merlin.ai

/**
 * Domain-level chat message that abstracts away provider-specific details.
 * Can be converted to/from provider-specific types like OpenAI ChatMessage.
 */
data class AIMessage(
    /**
     * The role of the message sender (system, user, assistant, function)
     */
    val role: AIRole,
    
    /**
     * The content of the message
     */
    val content: String
)

/**
 * Domain-level chat roles
 */
enum class AIRole {
    SYSTEM,
    USER, 
    ASSISTANT,
    FUNCTION
}

/**
 * Domain-level function tool definition
 */
data class AIFunctionTool(
    /**
     * Name of the function
     */
    val name: String,
    
    /**
     * Description of what the function does
     */
    val description: String,
    
    /**
     * JSON schema of the function parameters
     */
    val parameters: Map<String, Any>?
) 