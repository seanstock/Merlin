package com.example.merlin.data.manager

import android.util.Log
import com.example.merlin.data.database.entities.ChildProfile
import com.example.merlin.data.database.entities.ChatHistory
import com.example.merlin.data.database.entities.Memory
import com.example.merlin.data.database.entities.MemoryType
import com.example.merlin.data.model.openaidl.MerlinAIResponse
import com.example.merlin.data.repository.MemoryRepository
import com.example.merlin.database.MerlinDatabase
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.FunctionTool
import com.aallam.openai.api.core.Parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

/**
 * Manages AI interactions with memory storage and fallback capabilities.
 * Orchestrates the flow between AI service calls, conversation context, and memory storage.
 */
class AIInteractionManager(
    private val aiService: AIServiceInterface,
    private val database: MerlinDatabase,
    private val memoryRepository: MemoryRepository,
    private val fallbackTaskProvider: FallbackTaskProvider? = null
) {
    companion object {
        private const val TAG = "AIInteractionManager"
        private const val MAX_CHAT_HISTORY = 20
        private const val SYSTEM_PROMPT_TEMPLATE = """
        
            You are Merlin, a friendly AI tutor for a %d-year-old %s child named %s, located in %s, speaking %s. 

            Your role is to:
            - Offer engaging educational tasks or launch games
            - Grant Merlin Coins for good behavior, effort, creativity, and kindness (1-10 coins per call)
            - Check coin balances and answer questions about the economy system
            - Track and report screen time usage
            - Provide supportive feedback and encouragement
            - Adjust task difficulty dynamically to achieve ~80%% success rate
            - Remember personal details and preferences from previous conversations
            - Be patient, kind, and age-appropriate in all interactions

            Additional Instructions
            - For young children under 6, try to keep responses very short (< 25 words)
            - When a child asks to play a game, launch it immediately with level 1 unless they specify a different level
            - Don't ask for confirmation to launch games - just launch them when requested 

            Available functions:
            - launch_game: Start educational games for the child
            - grant_coins: Award 1-10 Merlin Coins for positive behavior at your discretion
            - check_coins: Check current coin balance and daily earning progress
            - check_screen_time: Check today's screen time and current session duration
            - award_bonus_coins: Award bonus coins for exceptional performance

            Always be encouraging and make learning fun!
        """
    }

    // Thread-safe cache for conversation contexts per child
    private val conversationContexts = ConcurrentHashMap<String, ConversationContextManager>()
    
    // Memory retriever for personalization
    private val memoryRetriever = MemoryRetriever(memoryRepository)
    
    // Context window manager for token limit management
    private val contextWindowManager = ContextWindowManager(
        maxTokens = 8000, // Conservative limit for GPT-4
        reservedTokensForResponse = 1000,
        memoryRetriever = memoryRetriever
    )
    
    // Memory summarizer for long-term memory management  
    private val memorySummarizer = MemorySummarizer(
        aiService = aiService,
        memoryRepository = memoryRepository
    )

    /**
     * Configuration for significance detection algorithm.
     */
    data class SignificanceConfig(
        val minMessageLength: Int = 10,
        val minResponseLength: Int = 20,
        val significanceThreshold: Double = 0.3,
        val emotionalWeight: Double = 1.5,
        val personalWeight: Double = 1.3,
        val educationalWeight: Double = 1.1,
        val questionWeight: Double = 1.2
    )

    private val significanceConfig = SignificanceConfig()

    /**
     * Process a user interaction and return an AI response.
     * Handles the complete flow: context retrieval, AI call, memory storage, and fallback.
     */
    suspend fun processInteraction(childId: String, userMessage: String): MerlinAIResponse {
        Log.d(TAG, "Processing interaction for child: $childId")
        
        return try {
            // Get or create conversation context for this child
            val contextManager = getOrCreateContextManager(childId)
            
            // Retrieve child profile for personalization
            val childProfile = withContext(Dispatchers.IO) {
                database.childProfileDao().getById(childId)
            }
            
            if (childProfile == null) {
                Log.w(TAG, "Child profile not found for ID: $childId")
                return MerlinAIResponse(
                    content = "I'm sorry, I couldn't find your profile. Please make sure you're logged in correctly.",
                    functionCallName = null,
                    functionCallArguments = null
                )
            }

            // Initialize system prompt if this is a new conversation
            if (contextManager.getFormattedHistory().isEmpty()) {
                val systemPrompt = createSystemPrompt(childProfile)
                contextManager.initializeWithSystemPrompt(systemPrompt)
                Log.d(TAG, "Initialized system prompt for child: $childId")
            }

            // Add user message to context
            contextManager.addUserMessage(userMessage)

            // Store user message in database
            withContext(Dispatchers.IO) {
                database.chatHistoryDao().insert(ChatHistory(
                    childId = childId,
                    role = "user",
                    content = userMessage,
                    ts = System.currentTimeMillis()
                ))
            }

            // Get AI response with memory context
            val aiResponse = getAIResponse(childId, userMessage, contextManager)
            
            if (aiResponse != null && (aiResponse.content != null || aiResponse.functionCallName != null)) {
                // Add assistant response to context
                contextManager.addAssistantMessage(aiResponse.content)
                
                // Store assistant response in database
                withContext(Dispatchers.IO) {
                    database.chatHistoryDao().insert(ChatHistory(
                        childId = childId,
                        role = "assistant",
                        content = aiResponse.content ?: "Function call: ${aiResponse.functionCallName}",
                        ts = System.currentTimeMillis()
                    ))
                }

                // Check if this interaction should be stored as a memory
                if (isSignificantInteraction(userMessage, aiResponse.content)) {
                    storeMemory(childId, userMessage, aiResponse.content)
                    
                    // Check if memory summarization is needed (background operation)
                    checkAndPerformMemorySummarization(childId)
                }

                Log.d(TAG, "Successfully processed interaction for child: $childId")
                return aiResponse
            } else {
                Log.w(TAG, "AI response was null or empty, falling back")
                return getFallbackResponse(childProfile)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing interaction for child: $childId", e)
            return getFallbackResponse(
                database.childProfileDao().getById(childId) ?: ChildProfile(
                    id = childId,
                    name = "there",
                    birthdate = null,
                    age = 8,
                    gender = "child",
                    preferredLanguage = "en",
                    location = "unknown"
                )
            )
        }
    }

    /**
     * Get or create a conversation context manager for a specific child.
     */
    private fun getOrCreateContextManager(childId: String): ConversationContextManager {
        return conversationContexts.computeIfAbsent(childId) {
            ConversationContextManager(maxSize = MAX_CHAT_HISTORY)
        }
    }

    /**
     * Create a personalized system prompt based on the child's profile.
     */
    private fun createSystemPrompt(childProfile: ChildProfile): String {
        return SYSTEM_PROMPT_TEMPLATE.format(
            childProfile.age ?: 8,
            childProfile.gender ?: "child",
            childProfile.name ?: "there",
            childProfile.location ?: "your area",
            childProfile.preferredLanguage ?: "English"
        )
    }

    /**
     * Get AI response using the OpenAI client with function calling capabilities and memory context.
     * Uses context window management to stay within token limits.
     */
    private suspend fun getAIResponse(
        childId: String,
        currentMessage: String,
        contextManager: ConversationContextManager
    ): MerlinAIResponse? {
        return try {
            val chatHistory = contextManager.getFormattedHistory()
            
            // Create all function tools that Merlin needs
            val functionTools = getAllAvailableFunctionTools()
            
            // Retrieve relevant memories for personalization
            val conversationHistory = chatHistory.mapNotNull { 
                when (val content = it.messageContent) {
                    is com.aallam.openai.api.chat.TextContent -> content.content
                    else -> content?.toString()
                }
            }
            val relevantMemories = memoryRetriever.getRelevantMemories(
                childId = childId,
                currentMessage = currentMessage,
                conversationHistory = conversationHistory,
                maxMemories = 5
            )
            
            // Format memories for AI prompt
            val memoryContext = if (relevantMemories.isNotEmpty()) {
                memoryRetriever.formatMemoriesForPrompt(relevantMemories)
            } else {
                null
            }
            
            // Check if context window optimization is needed
            val needsOptimization = contextWindowManager.needsOptimization(
                chatMessages = chatHistory,
                memoryContext = memoryContext,
                functionTools = functionTools
            )
            
            val finalMessages: List<ChatMessage>
            val finalMemoryContext: String?
            
            if (needsOptimization) {
                Log.d(TAG, "Context window optimization required - optimizing context")
                val optimizationResult = contextWindowManager.optimizeContextWindow(
                    chatMessages = chatHistory,
                    relevantMemories = relevantMemories,
                    functionTools = functionTools,
                    memoryContext = memoryContext
                )
                
                finalMessages = optimizationResult.optimizedMessages
                finalMemoryContext = optimizationResult.includedMemoryContext
                
                Log.d(TAG, "Context optimized: ${optimizationResult.droppedMessages} messages dropped, " +
                        "${optimizationResult.droppedMemories} memories dropped, " +
                        "${optimizationResult.totalTokens} total tokens")
            } else {
                finalMessages = chatHistory
                finalMemoryContext = memoryContext
                
                val stats = contextWindowManager.getTokenUsageStats(chatHistory, memoryContext, functionTools)
                Log.d(TAG, "No optimization needed: ${stats.totalTokens} tokens (${stats.utilizationPercentage}% utilization)")
            }
            
            Log.d(TAG, "Making AI request with ${finalMessages.size} messages, ${functionTools.size} functions, and ${relevantMemories.size} memories")
            
            // Use the enhanced method with optimized context
            aiService.getChatCompletionWithMemoryContext(finalMessages, functionTools, finalMemoryContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting AI response", e)
            null
        }
    }

    /**
     * Determine if an interaction is significant enough to store as a memory.
     * Enhanced algorithm with multiple factors and configurable thresholds.
     */
    private fun isSignificantInteraction(userMessage: String, aiResponse: String?): Boolean {
        if (aiResponse == null) return false
        
        val userLower = userMessage.lowercase()
        val responseLower = aiResponse.lowercase()
        val combinedText = "$userLower $responseLower"
        
        var significanceScore = 0.0
        
        // 1. Content-based significance
        significanceScore += analyzeContentSignificance(combinedText)
        
        // 2. Emotional significance
        significanceScore += analyzeEmotionalSignificance(combinedText) * significanceConfig.emotionalWeight
        
        // 3. Personal information significance
        significanceScore += analyzePersonalSignificance(combinedText) * significanceConfig.personalWeight
        
        // 4. Educational significance
        significanceScore += analyzeEducationalSignificance(combinedText) * significanceConfig.educationalWeight
        
        // 5. Question-answer significance
        significanceScore += analyzeQuestionAnswerSignificance(userMessage, aiResponse) * significanceConfig.questionWeight
        
        // 6. Interaction length factor
        significanceScore += analyzeInteractionLength(userMessage, aiResponse)
        
        // 7. Sentiment analysis factor
        significanceScore += analyzeSentiment(combinedText)
        
        val isSignificant = significanceScore >= significanceConfig.significanceThreshold
        
        Log.d(TAG, "Significance analysis - Score: $significanceScore, Threshold: ${significanceConfig.significanceThreshold}, Significant: $isSignificant")
        
        return isSignificant
    }

    /**
     * Analyze content for general significance keywords.
     */
    private fun analyzeContentSignificance(text: String): Double {
        val significantKeywords = listOf(
            "like", "love", "hate", "favorite", "prefer", "want", "need",
            "birthday", "age", "hobby", "interest", "game", "sport", "color", "food",
            "remember", "important", "special", "always", "never"
        )
        
        val matchCount = significantKeywords.count { keyword ->
            text.contains(Regex("\\b$keyword\\b"))
        }
        
        return (matchCount * 0.1).coerceAtMost(0.5)
    }

    /**
     * Analyze emotional content significance.
     */
    private fun analyzeEmotionalSignificance(text: String): Double {
        val emotionalKeywords = mapOf(
            // High emotional significance
            "scared" to 0.3, "terrified" to 0.4, "worried" to 0.25, "anxious" to 0.25,
            "sad" to 0.3, "crying" to 0.4, "upset" to 0.25, "angry" to 0.3,
            "excited" to 0.2, "happy" to 0.15, "proud" to 0.25, "amazed" to 0.2,
            
            // Medium emotional significance
            "frustrated" to 0.2, "confused" to 0.15, "surprised" to 0.1,
            "curious" to 0.1, "interested" to 0.1, "bored" to 0.15
        )
        
        var emotionalScore = 0.0
        emotionalKeywords.forEach { (keyword, weight) ->
            if (text.contains(Regex("\\b$keyword\\b"))) {
                emotionalScore += weight
            }
        }
        
        return emotionalScore.coerceAtMost(0.6)
    }

    /**
     * Analyze personal information significance.
     */
    private fun analyzePersonalSignificance(text: String): Double {
        val personalKeywords = mapOf(
            // Family and relationships
            "family" to 0.3, "mom" to 0.25, "dad" to 0.25, "mother" to 0.25, "father" to 0.25,
            "sister" to 0.2, "brother" to 0.2, "grandma" to 0.2, "grandpa" to 0.2,
            "friend" to 0.2, "best friend" to 0.3, "pet" to 0.2,
            
            // Personal details
            "birthday" to 0.3, "age" to 0.2, "school" to 0.2, "teacher" to 0.2,
            "home" to 0.15, "house" to 0.15, "room" to 0.1
        )
        
        var personalScore = 0.0
        personalKeywords.forEach { (keyword, weight) ->
            if (text.contains(Regex("\\b$keyword\\b"))) {
                personalScore += weight
            }
        }
        
        return personalScore.coerceAtMost(0.5)
    }

    /**
     * Analyze educational content significance.
     */
    private fun analyzeEducationalSignificance(text: String): Double {
        val educationalKeywords = mapOf(
            // Subjects
            "math" to 0.2, "reading" to 0.2, "science" to 0.2, "history" to 0.2,
            "english" to 0.2, "art" to 0.15, "music" to 0.15, "sports" to 0.15,
            
            // Learning activities
            "learn" to 0.15, "study" to 0.15, "practice" to 0.1, "homework" to 0.2,
            "test" to 0.2, "quiz" to 0.15, "lesson" to 0.15,
            
            // Achievement indicators
            "grade" to 0.2, "score" to 0.15, "correct" to 0.1, "wrong" to 0.15,
            "understand" to 0.15, "learned" to 0.15
        )
        
        var educationalScore = 0.0
        educationalKeywords.forEach { (keyword, weight) ->
            if (text.contains(Regex("\\b$keyword\\b"))) {
                educationalScore += weight
            }
        }
        
        return educationalScore.coerceAtMost(0.4)
    }

    /**
     * Analyze question-answer patterns for significance.
     */
    private fun analyzeQuestionAnswerSignificance(userMessage: String, aiResponse: String): Double {
        var qaScore = 0.0
        
        // User asks questions
        if (userMessage.contains("?")) {
            qaScore += 0.15
        }
        
        // AI asks questions (gathering information)
        if (aiResponse.contains("?")) {
            qaScore += 0.1
        }
        
        // Preference-revealing questions
        val preferencePatterns = listOf(
            "what.*favorite", "do you like", "which.*prefer", "what.*want",
            "how.*feel", "what.*think"
        )
        
        val combinedText = "$userMessage $aiResponse".lowercase()
        preferencePatterns.forEach { pattern ->
            if (combinedText.contains(Regex(pattern))) {
                qaScore += 0.2
            }
        }
        
        return qaScore.coerceAtMost(0.3)
    }

    /**
     * Analyze interaction length for significance.
     */
    private fun analyzeInteractionLength(userMessage: String, aiResponse: String): Double {
        val userLength = userMessage.length
        val responseLength = aiResponse.length
        
        var lengthScore = 0.0
        
        // Substantial user input
        when {
            userLength > 100 -> lengthScore += 0.2
            userLength > 50 -> lengthScore += 0.15
            userLength > 20 -> lengthScore += 0.1
            userLength < significanceConfig.minMessageLength -> lengthScore -= 0.1
        }
        
        // Substantial AI response
        when {
            responseLength > 200 -> lengthScore += 0.15
            responseLength > 100 -> lengthScore += 0.1
            responseLength > 50 -> lengthScore += 0.05
            responseLength < significanceConfig.minResponseLength -> lengthScore -= 0.05
        }
        
        return lengthScore.coerceIn(-0.2, 0.3)
    }

    /**
     * Basic sentiment analysis for significance.
     * Returns higher scores for emotionally charged content.
     */
    private fun analyzeSentiment(text: String): Double {
        val positiveWords = listOf(
            "great", "awesome", "amazing", "wonderful", "fantastic", "excellent",
            "love", "like", "enjoy", "fun", "happy", "excited", "proud"
        )
        
        val negativeWords = listOf(
            "hate", "dislike", "terrible", "awful", "horrible", "bad",
            "sad", "angry", "frustrated", "scared", "worried", "upset"
        )
        
        val positiveCount = positiveWords.count { word ->
            text.contains(Regex("\\b$word\\b"))
        }
        
        val negativeCount = negativeWords.count { word ->
            text.contains(Regex("\\b$word\\b"))
        }
        
        val sentimentIntensity = (positiveCount + negativeCount) * 0.05
        return sentimentIntensity.coerceAtMost(0.2)
    }

    /**
     * Store a significant interaction as a memory in the database.
     */
    private suspend fun storeMemory(childId: String, userMessage: String, aiResponse: String?) {
        try {
            withContext(Dispatchers.IO) {
                val memoryContent = "Child: $userMessage\nMerlin: ${aiResponse ?: "No response"}"
                val memoryType = determineMemoryType(userMessage, aiResponse)
                val importance = calculateMemoryImportance(userMessage, aiResponse)
                
                val memory = Memory(
                    childId = childId,
                    ts = System.currentTimeMillis(),
                    text = memoryContent,
                    sentiment = 0.0, // Basic sentiment, can be enhanced later
                    type = memoryType,
                    importance = importance
                )
                
                database.memoryDao().insert(memory)
                Log.d(TAG, "Stored memory for child: $childId (type: $memoryType, importance: $importance)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing memory for child: $childId", e)
        }
    }

    /**
     * Determine the type of memory based on the interaction content.
     */
    private fun determineMemoryType(userMessage: String, aiResponse: String?): MemoryType {
        val combinedText = "$userMessage ${aiResponse ?: ""}".lowercase()
        
        return when {
            combinedText.contains(Regex("\\b(like|love|hate|favorite|prefer)\\b")) -> MemoryType.PREFERENCE
            combinedText.contains(Regex("\\b(scared|happy|sad|excited|worried|proud|angry|frustrated)\\b")) -> MemoryType.EMOTIONAL
            combinedText.contains(Regex("\\b(family|mom|dad|sister|brother|friend|pet)\\b")) -> MemoryType.PERSONAL
            combinedText.contains(Regex("\\b(good job|well done|correct|excellent|achievement|accomplished)\\b")) -> MemoryType.ACHIEVEMENT
            combinedText.contains(Regex("\\b(difficult|hard|struggle|don't understand|confused|help)\\b")) -> MemoryType.DIFFICULTY
            combinedText.contains(Regex("\\b(learn|study|school|math|reading|science|subject)\\b")) -> MemoryType.EDUCATIONAL
            else -> MemoryType.GENERAL
        }
    }

    /**
     * Calculate the importance of a memory based on various factors.
     */
    private fun calculateMemoryImportance(userMessage: String, aiResponse: String?): Int {
        val combinedText = "$userMessage ${aiResponse ?: ""}".lowercase()
        var importance = 3 // Default importance
        
        // Increase importance for emotional content
        if (combinedText.contains(Regex("\\b(scared|worried|sad|frustrated|angry)\\b"))) {
            importance += 1
        }
        
        // Increase importance for preferences and personal information
        if (combinedText.contains(Regex("\\b(favorite|love|hate|family|friend)\\b"))) {
            importance += 1
        }
        
        // Increase importance for achievements
        if (combinedText.contains(Regex("\\b(proud|accomplished|good job|excellent)\\b"))) {
            importance += 1
        }
        
        // Increase importance for difficulties that need attention
        if (combinedText.contains(Regex("\\b(difficult|struggle|don't understand|confused)\\b"))) {
            importance += 1
        }
        
        // Decrease importance for very short interactions
        if (userMessage.length < 10 && (aiResponse?.length ?: 0) < 20) {
            importance -= 1
        }
        
        // Ensure importance stays within valid range (1-5)
        return importance.coerceIn(1, 5)
    }

    /**
     * Provide a fallback response when AI is unavailable or fails.
     */
    private fun getFallbackResponse(childProfile: ChildProfile): MerlinAIResponse {
        val fallbackMessages = listOf(
            "Hi ${childProfile.name ?: "there"}! I'm having trouble connecting right now, but I'm still here to help you learn!",
            "Let's try a fun learning activity! What subject would you like to explore today?",
            "I'm experiencing some technical difficulties, but don't worry - we can still have fun learning together!",
            "While I get back online, why don't you tell me about something interesting you learned recently?",
            "I'm having connection issues, but I'd love to hear about your day! What's been the best part so far?"
        )
        
        val randomMessage = fallbackMessages.random()
        
        Log.d(TAG, "Providing fallback response for child: ${childProfile.id}")
        
        return MerlinAIResponse(
            content = randomMessage,
            functionCallName = null,
            functionCallArguments = null
        )
    }

    /**
     * Clear conversation context for a specific child.
     * Useful for starting fresh conversations or memory management.
     */
    fun clearConversationContext(childId: String) {
        conversationContexts[childId]?.clearHistory()
        memoryRetriever.clearCache(childId)
        Log.d(TAG, "Cleared conversation context and memory cache for child: $childId")
    }

    /**
     * Clear all conversation contexts.
     * Useful for memory management or app restart scenarios.
     */
    fun clearAllConversationContexts() {
        conversationContexts.clear()
        memoryRetriever.clearCache()
        memorySummarizer.clearCache()
        Log.d(TAG, "Cleared all conversation contexts and memory cache")
    }

    /**
     * Check if memory summarization is needed and perform it in the background.
     * This helps maintain long-term memory efficiency.
     */
    private suspend fun checkAndPerformMemorySummarization(childId: String) {
        try {
            // Check if summarization is needed (non-blocking check)
            val needsSummarization = memorySummarizer.needsSummarization(childId)
            
            if (needsSummarization) {
                Log.d(TAG, "Memory summarization needed for child: $childId")
                
                // Perform summarization in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = memorySummarizer.summarizeOldMemories(childId)
                        if (result != null) {
                            Log.d(TAG, "Successfully summarized ${result.memoryCount} memories for child: $childId")
                            
                            // Clear memory retriever cache to ensure fresh data
                            memoryRetriever.clearCache(childId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Background memory summarization failed for child: $childId", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking memory summarization needs for child: $childId", e)
        }
    }

    /**
     * Get memory summarization statistics for monitoring.
     */
    suspend fun getMemorySummarizationStats(childId: String): MemorySummarizer.SummarizationStats {
        return memorySummarizer.getSummarizationStats(childId)
    }

    /**
     * Manually trigger memory summarization for a child.
     */
    suspend fun triggerMemorySummarization(childId: String): MemorySummarizer.SummarizationResult? {
        return memorySummarizer.summarizeOldMemories(childId)
    }

    /**
     * Get all available function tools for Merlin.
     */
    private fun getAllAvailableFunctionTools(): List<FunctionTool> {
        return listOf(
            createStartGameTool(),
            createCheckCoinsTool(),
            createGrantCoinsTool(),
            createCheckScreenTimeTool()
        )
    }
    
    /**
     * Create the start_game function tool
     */
    private fun createStartGameTool(): FunctionTool {
        val parametersJson = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("game_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The ID of the game to launch")
                })
                put("level", buildJsonObject {
                    put("type", "integer")
                    put("description", "The difficulty level (1-5)")
                    put("minimum", 1)
                    put("maximum", 5)
                })
                put("reason", buildJsonObject {
                    put("type", "string")
                    put("description", "Encouraging reason for playing this game")
                })
            })
            put("required", kotlinx.serialization.json.buildJsonArray {
                add(JsonPrimitive("game_id"))
            })
        }
        return FunctionTool(
            name = "start_game",
            description = "Launch a specific educational game for the child",
            parameters = Parameters(parametersJson)
        )
    }
    
    /**
     * Create the check_coins function tool
     */
    private fun createCheckCoinsTool(): FunctionTool {
        val parametersJson = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("include_details", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether to include detailed breakdown of earnings and limits")
                    put("default", false)
                })
            })
            put("required", kotlinx.serialization.json.buildJsonArray {})
        }
        return FunctionTool(
            name = "check_coins",
            description = "Check the child's current Merlin Coin balance and earning status",
            parameters = Parameters(parametersJson)
        )
    }
    
    /**
     * Create the grant_coins function tool
     */
    private fun createGrantCoinsTool(): FunctionTool {
        val parametersJson = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("amount", buildJsonObject {
                    put("type", "integer")
                    put("description", "Number of coins to grant (1-10)")
                    put("minimum", 1)
                    put("maximum", 10)
                })
                put("reason", buildJsonObject {
                    put("type", "string")
                    put("description", "Specific reason for granting coins (be encouraging and specific)")
                })
            })
            put("required", kotlinx.serialization.json.buildJsonArray {
                add(JsonPrimitive("amount"))
                add(JsonPrimitive("reason"))
            })
        }
        return FunctionTool(
            name = "grant_coins", 
            description = "Grant bonus Merlin Coins to the child for good behavior, effort, or achievements (1-10 coins). Use this at your discretion to reward positive interactions, creativity, kindness, effort, or learning milestones.",
            parameters = Parameters(parametersJson)
        )
    }
    
    /**
     * Create the check_screen_time function tool
     */
    private fun createCheckScreenTimeTool(): FunctionTool {
        val parametersJson = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", kotlinx.serialization.json.buildJsonArray {})
        }
        return FunctionTool(
            name = "check_screen_time",
            description = "Check the child's screen time usage for today and current session",
            parameters = Parameters(parametersJson)
        )
    }
}

/**
 * Interface for providing fallback tasks when AI is unavailable.
 * This can be implemented to provide local educational content.
 */
interface FallbackTaskProvider {
    fun getTaskForChild(childProfile: ChildProfile): String
} 