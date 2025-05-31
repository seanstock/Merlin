package com.example.merlin.ui.chat

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.data.manager.AIInteractionManager
import com.example.merlin.data.manager.FallbackTaskProviderImpl
import com.example.merlin.data.manager.OpenAIServiceAdapter
import com.example.merlin.data.remote.OpenAIClientWrapper
import com.example.merlin.data.repository.MemoryRepository
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.ui.safety.ContentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for managing chat state and AI interactions.
 */
class ChatViewModel(application: Application, private val childId: String) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }

    // Dependencies
    private val database = DatabaseProvider.getInstance(application)
    private val memoryRepository = MemoryRepository(database.memoryDao())
    private val openAIClient = OpenAIClientWrapper()
    private val aiService = OpenAIServiceAdapter(openAIClient)
    private val fallbackTaskProvider = FallbackTaskProviderImpl()
    private val contentFilter = ContentFilter() // Child safety content filtering
    private val aiInteractionManager = AIInteractionManager(
        aiService = aiService,
        database = database,
        memoryRepository = memoryRepository,
        fallbackTaskProvider = fallbackTaskProvider
    )

    // Text-to-Speech
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // Chat state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Game launch callback
    private val _gameLaunchEvent = MutableStateFlow<GameLaunchEvent?>(null)
    val gameLaunchEvent: StateFlow<GameLaunchEvent?> = _gameLaunchEvent.asStateFlow()

    init {
        initializeTextToSpeech()
        addWelcomeMessage()
        Log.d(TAG, "ChatViewModel initialized for childId: $childId")
    }

    /**
     * Initialize Text-to-Speech engine.
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                isTtsInitialized = true
                Log.d(TAG, "Text-to-Speech initialized successfully")
            } else {
                Log.e(TAG, "Text-to-Speech initialization failed")
            }
        }
    }

    /**
     * Add welcome message from Merlin.
     */
    private fun addWelcomeMessage() {
        val welcomeMessageContent = "Hi there! I'm Merlin, your friendly AI tutor! I'm here to help you learn and have fun. What would you like to explore today?"
        val welcomeMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = welcomeMessageContent,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = listOf(welcomeMessage)
        
        if (_isTtsEnabled.value) {
            speakMessage(welcomeMessage.content)
        }
    }

    /**
     * Send a message to the AI tutor.
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // 🛡️ CONTENT FILTERING - Filter user input first
        val userFilterResult = contentFilter.filterUserInput(message.trim())
        
        if (!userFilterResult.isAppropriate) {
            Log.w(TAG, "User input filtered: ${userFilterResult.reason}")
            
            // Add user message (to show what they said)
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message.trim(),
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage
            
            // Add filtered response with redirection
            val filteredResponse = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = userFilterResult.suggestedResponse ?: "Let's talk about something educational and fun instead!",
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + filteredResponse
            _currentInput.value = ""
            
            if (_isTtsEnabled.value) {
                speakMessage(filteredResponse.content)
            }
            return
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = message.trim(),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        // Add user message to chat
        _messages.value = _messages.value + userMessage
        _currentInput.value = ""
        _isLoading.value = true
        _errorMessage.value = null

        // Process AI response
        viewModelScope.launch {
            try {
                val aiResponse = aiInteractionManager.processInteraction(
                    childId = this@ChatViewModel.childId,
                    userMessage = message
                )

                val responseContent = aiResponse.content ?: "I'm having trouble responding right now. Let's try something else!"
                
                // 🛡️ CONTENT FILTERING - Filter AI response
                val aiFilterResult = contentFilter.filterAIResponse(responseContent)
                
                val finalContent = if (aiFilterResult.isAppropriate) {
                    // Enhance educational content if appropriate
                    contentFilter.enhanceEducationalContent(responseContent)
                } else {
                    Log.w(TAG, "AI response filtered: ${aiFilterResult.reason}")
                    aiFilterResult.suggestedResponse ?: "I need to think of a better way to help you with that. Let's try something educational and fun instead!"
                }

                val functionCallName = if (aiFilterResult.isAppropriate) aiResponse.functionCallName else null
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = finalContent,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    functionCall = if (functionCallName != null) {
                        FunctionCall(
                            name = functionCallName,
                            arguments = parseJsonArguments(aiResponse.functionCallArguments)
                        )
                    } else null
                )

                _messages.value = _messages.value + assistantMessage
                
                // Speak AI response
                if (_isTtsEnabled.value && assistantMessage.content.isNotBlank()) {
                    speakMessage(assistantMessage.content)
                }

                // Handle function calls (only if content passed filtering)
                if (aiFilterResult.isAppropriate) {
                    assistantMessage.functionCall?.let { functionCall ->
                        handleFunctionCall(functionCall)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing AI interaction", e)
                
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "I'm having some technical difficulties right now. Let's try again in a moment!",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    hasError = true
                )
                
                _messages.value = _messages.value + errorMessage
                _errorMessage.value = "Failed to get AI response. Please try again."
                
                if (_isTtsEnabled.value) {
                    speakMessage(errorMessage.content)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update the current input text.
     */
    fun updateInput(input: String) {
        _currentInput.value = input
    }

    /**
     * Toggle Text-to-Speech on/off.
     */
    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
        if (!_isTtsEnabled.value) {
            textToSpeech?.stop()
        }
    }

    /**
     * Speak a message using Text-to-Speech.
     */
    private fun speakMessage(message: String) {
        if (isTtsInitialized && _isTtsEnabled.value) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "merlin_message")
        }
    }

    /**
     * Handle function calls from the AI (e.g., launching games).
     */
    private fun handleFunctionCall(functionCall: FunctionCall) {
        when (functionCall.name) {
            "launch_game" -> {
                val gameId = functionCall.arguments["game_id"] as? String
                val level = (functionCall.arguments["level"] as? Number)?.toInt() ?: 1
                
                Log.d(TAG, "AI requested to launch game: $gameId at level $level")
                
                if (gameId != null) {
                    // Emit game launch event for the UI to handle
                    _gameLaunchEvent.value = GameLaunchEvent(gameId, level)
                } else {
                    Log.w(TAG, "Game launch requested but no game_id provided")
                }
            }
            else -> {
                Log.w(TAG, "Unknown function call: ${functionCall.name}")
            }
        }
    }

    /**
     * Parse JSON arguments from function call response.
     */
    private fun parseJsonArguments(jsonString: String?): Map<String, Any> {
        if (jsonString.isNullOrBlank()) return emptyMap()
        
        return try {
            // Simple JSON parsing for function arguments
            // In a production app, you'd use a proper JSON library
            val cleanJson = jsonString.trim().removeSurrounding("{", "}")
            val pairs = cleanJson.split(",")
            val result = mutableMapOf<String, Any>()
            
            for (pair in pairs) {
                val keyValue = pair.split(":")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().removeSurrounding("\"")
                    val value = keyValue[1].trim().removeSurrounding("\"")
                    
                    // Try to parse as number, otherwise keep as string
                    result[key] = value.toIntOrNull() ?: value
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing function arguments: $jsonString", e)
            emptyMap()
        }
    }

    /**
     * Clear the game launch event after it's been handled.
     */
    fun clearGameLaunchEvent() {
        _gameLaunchEvent.value = null
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear chat history.
     */
    fun clearChat() {
        _messages.value = emptyList()
        aiInteractionManager.clearConversationContext(this.childId)
        addWelcomeMessage()
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        aiInteractionManager.clearAllConversationContexts()
    }
} 