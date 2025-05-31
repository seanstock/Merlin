package com.example.merlin.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Manages Speech-to-Text functionality for voice input in the chat interface.
 */
class SpeechToTextManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechToTextManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // State flows for UI
    private val _isListening = MutableStateFlow(false)
    val isListeningFlow: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedTextFlow: StateFlow<String> = _recognizedText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _error.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    val isAvailableFlow: StateFlow<Boolean> = _isAvailable.asStateFlow()

    init {
        checkAvailability()
    }

    /**
     * Check if Speech-to-Text is available on this device.
     */
    private fun checkAvailability() {
        _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context)
        if (!_isAvailable.value) {
            Log.w(TAG, "Speech recognition is not available on this device")
        }
    }

    /**
     * Start listening for speech input.
     */
    fun startListening(onResult: (String) -> Unit) {
        if (!_isAvailable.value) {
            _error.value = "Speech recognition is not available"
            return
        }

        if (isListening) {
            Log.w(TAG, "Already listening for speech")
            return
        }

        try {
            // Create speech recognizer if needed
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(createRecognitionListener(onResult))
            }

            // Create recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Merlin...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            // Start listening
            speechRecognizer?.startListening(intent)
            isListening = true
            _isListening.value = true
            _error.value = null
            _recognizedText.value = ""
            
            Log.d(TAG, "Started listening for speech")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _error.value = "Failed to start speech recognition"
            _isListening.value = false
            isListening = false
        }
    }

    /**
     * Stop listening for speech input.
     */
    fun stopListening() {
        if (!isListening) return

        try {
            speechRecognizer?.stopListening()
            isListening = false
            _isListening.value = false
            Log.d(TAG, "Stopped listening for speech")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }

    /**
     * Cancel speech recognition.
     */
    fun cancel() {
        if (!isListening) return

        try {
            speechRecognizer?.cancel()
            isListening = false
            _isListening.value = false
            _recognizedText.value = ""
            Log.d(TAG, "Cancelled speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }

    /**
     * Create recognition listener for handling speech recognition events.
     */
    private fun createRecognitionListener(onResult: (String) -> Unit): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _error.value = null
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech input was detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                
                Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
                _error.value = errorMessage
                _isListening.value = false
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "Speech recognition result: $recognizedText")
                    _recognizedText.value = recognizedText
                    onResult(recognizedText)
                }
                _isListening.value = false
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partialText = matches[0]
                    _recognizedText.value = partialText
                    Log.d(TAG, "Partial speech recognition result: $partialText")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        }
    }

    /**
     * Clear any error messages.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            _isListening.value = false
            Log.d(TAG, "Speech recognition cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up speech recognition", e)
        }
    }
} 