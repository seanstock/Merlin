package com.example.merlin.data.manager

import android.util.Log
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionTool
import com.aallam.openai.api.chat.TextContent
import com.example.merlin.data.manager.MemoryRetriever.ScoredMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages conversation context within AI model token limits.
 * Provides intelligent prioritization and dynamic adjustment of context window.
 */
class ContextWindowManager(
    private val maxTokens: Int = DEFAULT_MAX_TOKENS,
    private val reservedTokensForResponse: Int = DEFAULT_RESERVED_TOKENS,
    private val memoryRetriever: MemoryRetriever? = null
) {
    companion object {
        private const val TAG = "ContextWindowManager"
        private const val DEFAULT_MAX_TOKENS = 8000 // Conservative limit for GPT-4
        private const val DEFAULT_RESERVED_TOKENS = 1000 // Reserve tokens for AI response
        private const val AVERAGE_TOKENS_PER_CHAR = 0.25 // Rough estimate for English text
        private const val SYSTEM_MESSAGE_PRIORITY = 1000 // Highest priority
        private const val RECENT_MESSAGE_PRIORITY = 800 // High priority for recent messages
        private const val MEMORY_BASE_PRIORITY = 600 // Base priority for memories
        private const val FUNCTION_TOOL_TOKENS = 200 // Estimated tokens per function tool
    }

    /**
     * Configuration for context window management.
     */
    data class ContextConfig(
        val maxTokens: Int = DEFAULT_MAX_TOKENS,
        val reservedTokensForResponse: Int = DEFAULT_RESERVED_TOKENS,
        val prioritizeRecentMessages: Boolean = true,
        val includeMemories: Boolean = true,
        val maxMemories: Int = 5,
        val minMessagesRequired: Int = 3 // Always keep at least this many recent messages
    )

    private val config = ContextConfig(maxTokens = maxTokens, reservedTokensForResponse = reservedTokensForResponse)

    /**
     * Represents a prioritized context item (message or memory).
     */
    private data class PrioritizedItem(
        val content: String,
        val priority: Int,
        val tokenCount: Int,
        val type: ItemType,
        val chatMessage: ChatMessage? = null,
        val memory: ScoredMemory? = null
    )

    private enum class ItemType {
        SYSTEM_MESSAGE,
        USER_MESSAGE,
        ASSISTANT_MESSAGE,
        MEMORY_CONTEXT,
        FUNCTION_TOOLS
    }

    /**
     * Optimizes context window by selecting the most important messages and memories within token limits.
     */
    suspend fun optimizeContextWindow(
        chatMessages: List<ChatMessage>,
        relevantMemories: List<ScoredMemory> = emptyList(),
        functionTools: List<FunctionTool> = emptyList(),
        memoryContext: String? = null
    ): ContextWindowResult = withContext(Dispatchers.Default) {
        
        // Log.d(TAG, "Optimizing context window with ${chatMessages.size} messages, ${relevantMemories.size} memories")
        
        val availableTokens = config.maxTokens - config.reservedTokensForResponse
        val functionToolTokens = functionTools.size * FUNCTION_TOOL_TOKENS
        val remainingTokens = availableTokens - functionToolTokens
        
        if (remainingTokens <= 0) {
             // Log.w(TAG, "No tokens available after reserving for response and function tools")
             return@withContext ContextWindowResult(
                optimizedMessages = emptyList(),
                includedMemoryContext = null,
                totalTokens = functionToolTokens,
                droppedMessages = chatMessages.size,
                droppedMemories = relevantMemories.size
            )
        }
        
        // Create prioritized items
        val prioritizedItems = mutableListOf<PrioritizedItem>()
        
        // Add chat messages with priority
        chatMessages.forEachIndexed { index, message ->
            val priority = calculateMessagePriority(message, index, chatMessages.size)
            val actualContent = (message.messageContent as? TextContent)?.content ?: ""
            val tokenCount = estimateTokenCount(actualContent)
            val itemType = when (message.role) {
                ChatRole.System -> ItemType.SYSTEM_MESSAGE
                ChatRole.User -> ItemType.USER_MESSAGE
                ChatRole.Assistant -> ItemType.ASSISTANT_MESSAGE
                else -> ItemType.USER_MESSAGE
            }
            
            prioritizedItems.add(PrioritizedItem(
                content = actualContent,
                priority = priority,
                tokenCount = tokenCount,
                type = itemType,
                chatMessage = message
            ))
        }
        
        // Add memory context if provided
        if (memoryContext != null && config.includeMemories) {
            val memoryTokens = estimateTokenCount(memoryContext)
            prioritizedItems.add(PrioritizedItem(
                content = memoryContext,
                priority = MEMORY_BASE_PRIORITY,
                tokenCount = memoryTokens,
                type = ItemType.MEMORY_CONTEXT
            ))
        }
        
        // Select items within token limit
        val selectedItems = selectItemsWithinTokenLimit(prioritizedItems, remainingTokens)
        
        // Build optimized context
        val optimizedMessages = buildOptimizedMessages(selectedItems)
        val includedMemoryContext = selectedItems.find { it.type == ItemType.MEMORY_CONTEXT }?.content
        
        val totalTokens = selectedItems.sumOf { it.tokenCount } + functionToolTokens
        val droppedMessages = chatMessages.size - selectedItems.count { it.chatMessage != null }
        val droppedMemories = if (memoryContext != null && includedMemoryContext == null) 1 else 0
        
        // Log.d(TAG, "Context optimization complete: ${optimizedMessages.size} messages, $totalTokens tokens, $droppedMessages dropped messages")
        
        ContextWindowResult(
            optimizedMessages = optimizedMessages,
            includedMemoryContext = includedMemoryContext,
            totalTokens = totalTokens,
            droppedMessages = droppedMessages,
            droppedMemories = droppedMemories
        )
    }

    /**
     * Calculate priority for a chat message based on its role, position, and content.
     */
    private fun calculateMessagePriority(message: ChatMessage, index: Int, totalMessages: Int): Int {
        val basePriority = when (message.role) {
            ChatRole.System -> SYSTEM_MESSAGE_PRIORITY
            ChatRole.User -> RECENT_MESSAGE_PRIORITY
            ChatRole.Assistant -> RECENT_MESSAGE_PRIORITY - 50
            ChatRole.Tool -> RECENT_MESSAGE_PRIORITY - 100
            else -> 400
        }
        
        // Boost priority for recent messages
        val recencyBoost = if (config.prioritizeRecentMessages) {
            val recentnessRatio = (totalMessages - index).toFloat() / totalMessages
            (recentnessRatio * 200).toInt()
        } else {
            0
        }
        
        // Boost priority for longer, more substantial messages
        val actualContent = (message.messageContent as? TextContent)?.content ?: ""
        val contentBoost = when {
            actualContent.length > 200 -> 50
            actualContent.length > 100 -> 25
            else -> 0
        }
        
        return basePriority + recencyBoost + contentBoost
    }

    /**
     * Select items that fit within the token limit, prioritizing by importance.
     */
    private fun selectItemsWithinTokenLimit(
        items: List<PrioritizedItem>,
        tokenLimit: Int
    ): List<PrioritizedItem> {
        val sortedItems = items.sortedByDescending { it.priority }
        val selectedItems = mutableListOf<PrioritizedItem>()
        var usedTokens = 0
        
        // Always include system messages first
        val systemMessages = sortedItems.filter { it.type == ItemType.SYSTEM_MESSAGE }
        for (systemMessage in systemMessages) {
            if (usedTokens + systemMessage.tokenCount <= tokenLimit) {
                selectedItems.add(systemMessage)
                usedTokens += systemMessage.tokenCount
            }
        }
        
        // Ensure we keep minimum required recent messages
        val nonSystemItems = sortedItems.filter { it.type != ItemType.SYSTEM_MESSAGE }
        val recentMessages = nonSystemItems
            .filter { it.chatMessage != null }
            .sortedByDescending { it.priority }
            .take(config.minMessagesRequired)
        
        for (recentMessage in recentMessages) {
            if (!selectedItems.contains(recentMessage) && usedTokens + recentMessage.tokenCount <= tokenLimit) {
                selectedItems.add(recentMessage)
                usedTokens += recentMessage.tokenCount
            }
        }
        
        // Add remaining items by priority
        for (item in nonSystemItems) {
            if (!selectedItems.contains(item) && usedTokens + item.tokenCount <= tokenLimit) {
                selectedItems.add(item)
                usedTokens += item.tokenCount
            }
        }
        
        return selectedItems
    }

    /**
     * Build optimized chat messages from selected items.
     */
    private fun buildOptimizedMessages(selectedItems: List<PrioritizedItem>): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        
        // Add system messages first
        selectedItems
            .filter { it.type == ItemType.SYSTEM_MESSAGE && it.chatMessage != null }
            .sortedBy { it.priority } // System messages in original order
            .forEach { messages.add(it.chatMessage!!) }
        
        // Add memory context as system message if included
        val memoryContext = selectedItems.find { it.type == ItemType.MEMORY_CONTEXT }
        if (memoryContext != null) {
            // Check if we already have a system message to append to
            val existingSystemMessage = messages.find { it.role == ChatRole.System }
            if (existingSystemMessage != null) {
                // Append memory context to existing system message
                val enhancedContent = "${existingSystemMessage.messageContent}\n\n${memoryContext.content}"
                val enhancedMessage = ChatMessage(
                    role = ChatRole.System,
                    messageContent = com.aallam.openai.api.chat.TextContent(enhancedContent)
                )
                messages[messages.indexOf(existingSystemMessage)] = enhancedMessage
            } else {
                // Add memory context as new system message
                messages.add(ChatMessage(
                    role = ChatRole.System,
                    messageContent = com.aallam.openai.api.chat.TextContent(memoryContext.content)
                ))
            }
        }
        
        // Add conversation messages in chronological order
        selectedItems
            .filter { it.chatMessage != null && it.type != ItemType.SYSTEM_MESSAGE }
            .sortedBy { items -> 
                // Sort by original order in conversation
                selectedItems.indexOf(items)
            }
            .forEach { messages.add(it.chatMessage!!) }
        
        return messages
    }

    /**
     * Estimate token count for text content.
     * This is a rough approximation - for production, consider using a proper tokenizer.
     */
    private fun estimateTokenCount(text: String): Int {
        if (text.isEmpty()) return 0
        
        // Rough estimation: ~4 characters per token for English text
        // This is conservative to avoid exceeding limits
        val charCount = text.length
        val estimatedTokens = (charCount * AVERAGE_TOKENS_PER_CHAR).toInt()
        
        // Add some padding for safety
        return (estimatedTokens * 1.1).toInt()
    }

    /**
     * Get current token usage statistics.
     */
    fun getTokenUsageStats(
        chatMessages: List<ChatMessage>,
        memoryContext: String? = null,
        functionTools: List<FunctionTool> = emptyList()
    ): TokenUsageStats {
        val messageTokens = chatMessages.sumOf {
            val contentString = when (val mc = it.messageContent) {
                is TextContent -> mc.content
                else -> mc?.toString() ?: "" // Fallback for other content types if any, or empty string
            }
            estimateTokenCount(contentString)
        }
        val memoryTokens = memoryContext?.let { estimateTokenCount(it) } ?: 0
        val functionTokens = functionTools.size * FUNCTION_TOOL_TOKENS
        val totalTokens = messageTokens + memoryTokens + functionTokens
        
        return TokenUsageStats(
            messageTokens = messageTokens,
            memoryTokens = memoryTokens,
            functionTokens = functionTokens,
            totalTokens = totalTokens,
            availableTokens = config.maxTokens - totalTokens,
            utilizationPercentage = (totalTokens.toFloat() / config.maxTokens * 100).toInt()
        )
    }

    /**
     * Check if context needs optimization based on token usage.
     */
    fun needsOptimization(
        chatMessages: List<ChatMessage>,
        memoryContext: String? = null,
        functionTools: List<FunctionTool> = emptyList()
    ): Boolean {
        val stats = getTokenUsageStats(chatMessages, memoryContext, functionTools)
        return stats.totalTokens > (config.maxTokens - config.reservedTokensForResponse)
    }

    /**
     * Result of context window optimization.
     */
    data class ContextWindowResult(
        val optimizedMessages: List<ChatMessage>,
        val includedMemoryContext: String?,
        val totalTokens: Int,
        val droppedMessages: Int,
        val droppedMemories: Int
    )

    /**
     * Token usage statistics.
     */
    data class TokenUsageStats(
        val messageTokens: Int,
        val memoryTokens: Int,
        val functionTokens: Int,
        val totalTokens: Int,
        val availableTokens: Int,
        val utilizationPercentage: Int
    )
} 