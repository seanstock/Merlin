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
import com.example.merlin.ai.MerlinTools
import com.example.merlin.ai.MerlinToolExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for managing chat state and AI interactions with tool calling support.
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

    // Tool executor for handling Merlin's function calls
    private val toolExecutor = MerlinToolExecutor(
        context = application,
        childId = childId,
        onNavigateToScreen = { screen, message ->
            _navigationEvent.value = NavigationEvent(screen, message)
        },
        onShowMessage = { message ->
            _toolMessage.value = message
        },
        onLaunchGame = { gameId, level, reason ->
            _gameLaunchEvent.value = GameLaunchEvent(gameId, level, reason)
        }
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

    // Navigation events from tools
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    // Tool messages
    private val _toolMessage = MutableStateFlow<String?>(null)
    val toolMessage: StateFlow<String?> = _toolMessage.asStateFlow()

    init {
        initializeTextToSpeech()
        addWelcomeMessage()
        Log.d(TAG, "ChatViewModel initialized for childId: $childId with tool support")
    }

    /**
     * Initialize Text-to-Speech engine.
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                Log.d(TAG, "TTS initialized: $isTtsInitialized")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    /**
     * Add welcome message from Merlin.
     */
    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = "Hi there! I'm Merlin, your AI learning companion! 🧙‍♂️✨ I can help you learn, start games, check your coin balance, and much more. What would you like to do today?",
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = listOf(welcomeMessage)
        
        // Speak welcome message
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

        // Process AI response with tool support
        viewModelScope.launch {
            try {
                // Use AI interaction manager to get response
                val aiResponse = aiInteractionManager.processInteraction(
                    childId = this@ChatViewModel.childId,
                    userMessage = message
                )

                // Check if AI made a function call
                val functionCallName = aiResponse.functionCallName
                if (functionCallName != null) {
                    Log.d(TAG, "AI made function call: $functionCallName")
                    
                    // Parse function arguments
                    val arguments = parseJsonArguments(aiResponse.functionCallArguments)
                    
                    // Execute the tool
                    val toolResult = toolExecutor.executeTool(functionCallName, arguments)
                    
                    // Create response message with tool result
                    val responseContent = if (toolResult.success) {
                        toolResult.message
                    } else {
                        "I had trouble with that request: ${toolResult.message}"
                    }
                    
                    // 🛡️ CONTENT FILTERING - Filter tool result
                    val toolFilterResult = contentFilter.filterAIResponse(responseContent)
                    
                    val finalContent = if (toolFilterResult.isAppropriate) {
                        contentFilter.enhanceEducationalContent(responseContent)
                    } else {
                        Log.w(TAG, "Tool result filtered: ${toolFilterResult.reason}")
                        toolFilterResult.suggestedResponse ?: "I need to think of a better way to help you with that."
                    }
                    
                    // Add tool result message
                    val assistantMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = finalContent,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + assistantMessage
                    
                    // Speak the result
                    if (_isTtsEnabled.value) {
                        speakMessage(finalContent)
                    }
                    
                } else {
                    // Regular text response
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

                    // Regular response
                    val assistantMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = finalContent,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + assistantMessage
                    
                    // Speak AI response
                    if (_isTtsEnabled.value) {
                        speakMessage(finalContent)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in AI interaction", e)
                _errorMessage.value = "I'm having trouble connecting right now. Please try again!"
                
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "Sorry, I'm having trouble connecting right now. Please try again in a moment! 🔄",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + errorMessage
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

    // Clear events
    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    fun clearToolMessage() {
        _toolMessage.value = null
    }



    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        aiInteractionManager.clearAllConversationContexts()
    }
} 