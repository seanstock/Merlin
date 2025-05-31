package com.example.merlin.data.remote

import android.util.Log
import androidx.collection.LruCache
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionTool
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.ToolChoice
import com.aallam.openai.api.chat.ToolType
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.exception.OpenAIHttpException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.merlin.data.BuildConfig
import com.example.merlin.data.model.openaidl.FunctionParameterProperty
import com.example.merlin.data.model.openaidl.MerlinAIResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import java.io.IOException

// Test import resolution
// import com.aallam.openai.api.chat.Tool // This was one of the unresolved imports

class OpenAIClientWrapper {

    // private val testTool: com.aallam.openai.api.chat.Tool? = null // Test declaration

    private val apiKey: String = BuildConfig.OPENAI_API_KEY
    private var openAI: OpenAI? = null
    private val responseCache: LruCache<String, MerlinAIResponse>

    companion object { // Moved constants and helpers to companion object
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_DELAY_MS = 1000L
        private const val MAX_BACKOFF_DELAY_MS = 16000L // e.g., 16 seconds
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val RESPONSE_CACHE_SIZE = 10 // Cache size

        fun buildParametersJson(properties: Map<String, FunctionParameterProperty>, required: List<String>? = null): Parameters {
            val jsonObject = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") { 
                    properties.forEach { (key, prop) ->
                        putJsonObject(key) { 
                            put("type", prop.type)
                            put("description", prop.description)
                            prop.enum?.let {
                                putJsonArray("enum") {
                                    it.forEach { enumValue -> add(enumValue) }
                                }
                            }
                        }
                    }
                }
                required?.let {
                    if (it.isNotEmpty()) {
                        putJsonArray("required") {
                            it.forEach { reqValue -> add(reqValue) }
                        }
                    }
                }
            }
            return Parameters(jsonObject)
        }

        fun createLaunchGameFunctionTool(): FunctionTool {
            val paramsMap = mapOf(
                "game_id" to FunctionParameterProperty(type = "string", description = "The unique identifier for the game to launch."),
                "level" to FunctionParameterProperty(type = "integer", description = "The suggested starting difficulty level for the game, e.g., 1-5.")
            )
            val requiredParams = listOf("game_id")
            val parametersJson = buildParametersJson(paramsMap, requiredParams)
            return FunctionTool(
                name = "launch_game",
                description = "Launches an educational game for the child based on the provided game ID and optional level.",
                parameters = parametersJson
            )
        }
        
        private fun isRetryableHttpError(statusCode: Int?): Boolean {
            return when (statusCode) {
                408, // Request Timeout
                429, // Too Many Requests (Rate Limiting)
                500, // Internal Server Error
                502, // Bad Gateway
                503, // Service Unavailable
                504  // Gateway Timeout
                -> true
                else -> false
            }
        }
    }

    init {
        responseCache = LruCache<String, MerlinAIResponse>(RESPONSE_CACHE_SIZE)
        if (apiKey == "KEY_NOT_FOUND_IN_LOCAL_PROPERTIES" || apiKey.isBlank() || !apiKey.startsWith("sk-")) {
            Log.e(
                "OpenAIClientWrapper",
                "OpenAI API key is invalid or not found. Please set a valid OPENAI_API_KEY in local.properties. Current value: '$apiKey'"
            )
            // Depending on the app's error handling strategy, this could throw an exception,
            // or leave openAI as null, and subsequent calls would need to handle that.
        } else {
            try {
                openAI = OpenAI(token = apiKey)
                Log.d("OpenAIClientWrapper", "OpenAI client initialized successfully.")
            } catch (e: Exception) {
                Log.e("OpenAIClientWrapper", "Failed to initialize OpenAI client: ${e.message}", e)
                // openAI will remain null
            }
        }
    }

    fun isInitialized(): Boolean = openAI != null

    private fun generateCacheKey(chatMessages: List<ChatMessage>, functionTools: List<FunctionTool>?, memoryContext: String?): String {
        val messagesKeyPart = chatMessages.joinToString("|") { msg ->
            val role = msg.role.toString()
            val content = msg.messageContent?.toString() ?: ""
            "${role}:${content.take(50)}"
        }
        val functionsKeyPart = functionTools?.joinToString("|") { it.name } ?: "NO_FUNCS"
        val memoryKeyPart = memoryContext?.take(100)?.hashCode()?.toString() ?: "NO_MEM"
        return "MSG:$messagesKeyPart##FUNC:$functionsKeyPart##MEM:$memoryKeyPart"
    }

    suspend fun getChatCompletionWithFunctions(
        chatMessages: List<ChatMessage>,
        functionTools: List<FunctionTool>?
    ): MerlinAIResponse? {
        return getChatCompletionWithMemoryContext(chatMessages, functionTools, null)
    }

    suspend fun getChatCompletionWithMemoryContext(
        chatMessages: List<ChatMessage>,
        functionTools: List<FunctionTool>?,
        memoryContext: String?
    ): MerlinAIResponse? {
        if (!isInitialized()) {
            Log.e("OpenAIClientWrapper", "OpenAI client not initialized.")
            return MerlinAIResponse(null, null, "Error: OpenAI client not initialized.")
        }

        // Enhance messages with memory context if provided
        val enhancedMessages = if (memoryContext != null && chatMessages.isNotEmpty()) {
            val systemMessage = chatMessages.firstOrNull { it.role == ChatRole.System }
            if (systemMessage != null) {
                // Append memory context to existing system message
                val enhancedSystemContent = "${systemMessage.messageContent}\n\n$memoryContext"
                val enhancedSystemMessage = ChatMessage(
                    role = ChatRole.System,
                    messageContent = com.aallam.openai.api.chat.TextContent(enhancedSystemContent)
                )
                listOf(enhancedSystemMessage) + chatMessages.drop(1)
            } else {
                // Add memory context as a new system message at the beginning
                val memorySystemMessage = ChatMessage(
                    role = ChatRole.System,
                    messageContent = com.aallam.openai.api.chat.TextContent(memoryContext)
                )
                listOf(memorySystemMessage) + chatMessages
            }
        } else {
            chatMessages
        }

        val cacheKey = generateCacheKey(enhancedMessages, functionTools, memoryContext)
        val cachedResponse = responseCache.get(cacheKey)
        if (cachedResponse != null) {
            Log.d("OpenAIClientWrapper", "Cache HIT for key: $cacheKey")
            return cachedResponse
        }
        Log.d("OpenAIClientWrapper", "Cache MISS for key: $cacheKey. Fetching from API.")

        var attempts = 0
        var currentDelayMs = INITIAL_BACKOFF_DELAY_MS

        while (attempts < MAX_RETRIES) {
            try {
                val tools = functionTools?.map { functionTool ->
                    Tool(
                        type = ToolType("function"),
                        function = functionTool
                    )
                }
                
                val request = ChatCompletionRequest(
                    model = ModelId("gpt-4o"),
                    messages = enhancedMessages,
                    tools = tools,
                    toolChoice = if (tools?.isNotEmpty() == true) ToolChoice.Auto else null
                )
                
                Log.d("OpenAIClientWrapper", "Attempt ${attempts + 1} for ChatCompletionRequest")
                val completion: ChatCompletion = openAI!!.chatCompletion(request)
                Log.d("OpenAIClientWrapper", "Received ChatCompletion after attempt ${attempts + 1}")

                val choice = completion.choices.firstOrNull()
                val message = choice?.message

                val toolCall = message?.toolCalls?.firstOrNull()

                val successfulMerlinResponse = if (toolCall != null) {
                    Log.d("OpenAIClientWrapper", "Tool call received: ${(toolCall as? ToolCall.Function)?.function?.name}")
                    MerlinAIResponse(
                        (message?.messageContent as? com.aallam.openai.api.chat.TextContent)?.content ?: message?.messageContent?.toString(), 
                        (toolCall as? ToolCall.Function)?.function?.name, 
                        (toolCall as? ToolCall.Function)?.function?.argumentsOrNull
                    )
                } else {
                    MerlinAIResponse(
                        (message?.messageContent as? com.aallam.openai.api.chat.TextContent)?.content ?: message?.messageContent?.toString(), 
                        null, 
                        null
                    )
                }
                
                if (successfulMerlinResponse.content != null || successfulMerlinResponse.functionCallName != null) {
                    responseCache.put(cacheKey, successfulMerlinResponse)
                    Log.d("OpenAIClientWrapper", "Response cached for key: $cacheKey")
                }
                return successfulMerlinResponse

            } catch (e: OpenAIAPIException) {
                val statusCode = try {
                    val clazz = e::class.java
                    val statusField = clazz.getDeclaredField("statusCode")?.apply { isAccessible = true }
                        ?: clazz.getDeclaredField("status")?.apply { isAccessible = true }
                    statusField?.get(e) as? Int
                } catch (ex: Exception) {
                    null
                }
                Log.w("OpenAIClientWrapper", "OpenAI API Exception on attempt ${attempts + 1}: Code=${statusCode}, Message=${e.message}", e)
                if (isRetryableHttpError(statusCode) && attempts < MAX_RETRIES - 1) {
                    attempts++
                    Log.d("OpenAIClientWrapper", "Retrying in ${currentDelayMs}ms...")
                    delay(currentDelayMs)
                    currentDelayMs = (currentDelayMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_BACKOFF_DELAY_MS)
                } else {
                    Log.e("OpenAIClientWrapper", "Non-retryable API error or max retries reached after ${attempts + 1} attempts.", e)
                    return MerlinAIResponse(null, null, "API Error: ${e.message} (Code: ${statusCode})")
                }
            } catch (e: OpenAIHttpException) {
                val statusCode = try {
                    val clazz = e::class.java
                    val statusField = clazz.getDeclaredField("statusCode")?.apply { isAccessible = true }
                        ?: clazz.getDeclaredField("status")?.apply { isAccessible = true }
                    statusField?.get(e) as? Int
                } catch (ex: Exception) {
                    null
                }
                Log.w("OpenAIClientWrapper", "OpenAI HTTP Exception on attempt ${attempts + 1}: Code=${statusCode}, Message=${e.message}", e)
                if (isRetryableHttpError(statusCode) && attempts < MAX_RETRIES - 1) {
                    attempts++
                    Log.d("OpenAIClientWrapper", "Retrying in ${currentDelayMs}ms...")
                    delay(currentDelayMs)
                    currentDelayMs = (currentDelayMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_BACKOFF_DELAY_MS)
                } else {
                    Log.e("OpenAIClientWrapper", "Non-retryable HTTP error or max retries reached after ${attempts + 1} attempts.", e)
                    return MerlinAIResponse(null, null, "Network Error: ${e.message} (Code: ${statusCode})")
                }
            } catch (e: IOException) { 
                Log.w("OpenAIClientWrapper", "Network IOException on attempt ${attempts + 1}: ${e.message}", e)
                if (attempts < MAX_RETRIES - 1) {
                    attempts++
                    Log.d("OpenAIClientWrapper", "Retrying in ${currentDelayMs}ms...")
                    delay(currentDelayMs)
                    currentDelayMs = (currentDelayMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_BACKOFF_DELAY_MS)
                } else {
                    Log.e("OpenAIClientWrapper", "Max retries reached for IOException after ${attempts + 1} attempts.", e)
                    return MerlinAIResponse(null, null, "Network Error: ${e.message}")
                }
            } catch (e: Exception) { 
                Log.e("OpenAIClientWrapper", "Unexpected error on attempt ${attempts + 1}: ${e.message}", e)
                return MerlinAIResponse(null, null, "Unexpected Error: ${e.message}") 
            }
        }
        Log.e("OpenAIClientWrapper", "Max retries reached. Failing operation after $MAX_RETRIES attempts.")
        return MerlinAIResponse(null, null, "Error: Max retries reached after $MAX_RETRIES attempts.")
    }
} 