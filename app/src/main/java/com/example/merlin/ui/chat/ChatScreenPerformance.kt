package com.example.merlin.ui.chat

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Performance optimization utilities for ChatScreen.
 * Addresses memory leaks, animation performance, and rendering efficiency.
 */
object ChatScreenPerformance {

    /**
     * Optimized gradient cache to prevent unnecessary gradient object creation.
     * Each gradient is expensive to create, so we cache them for reuse.
     */
    private val gradientCache = mutableMapOf<String, Brush>()
    
    /**
     * Get cached gradient or create new one if not exists.
     */
    fun getCachedGradient(key: String, colors: List<Color>): Brush {
        return gradientCache.getOrPut(key) {
            Brush.radialGradient(colors)
        }
    }
    
    /**
     * Get cached vertical gradient.
     */
    fun getCachedVerticalGradient(key: String, colors: List<Color>): Brush {
        return gradientCache.getOrPut("vertical_$key") {
            Brush.verticalGradient(colors)
        }
    }
    
    /**
     * Clear gradient cache to prevent memory buildup.
     * Call this when the screen is disposed or during memory pressure.
     */
    fun clearGradientCache() {
        gradientCache.clear()
    }
    
    /**
     * Animation frame rate manager to prevent dropped frames.
     */
    class AnimationFrameManager {
        private val _frameRateOptimized = MutableStateFlow(true)
        val frameRateOptimized: StateFlow<Boolean> = _frameRateOptimized.asStateFlow()
        
        /**
         * Enable/disable animations based on device performance.
         */
        fun setAnimationsEnabled(enabled: Boolean) {
            _frameRateOptimized.value = enabled
        }
    }
    
    /**
     * Memory-efficient message bubble rendering strategy.
     * Uses virtualization for large message lists.
     */
    object MessageOptimization {
        
        /**
         * Maximum number of messages to keep in memory at once.
         * Older messages beyond this limit will be virtualized.
         */
        const val MAX_MESSAGES_IN_MEMORY = 50
        
        /**
         * Check if message list needs virtualization.
         */
        fun shouldVirtualizeMessages(messageCount: Int): Boolean {
            return messageCount > MAX_MESSAGES_IN_MEMORY
        }
        
        /**
         * Get visible message range for virtualization.
         */
        fun getVisibleMessageRange(
            totalMessages: Int,
            firstVisibleIndex: Int,
            visibleItemCount: Int
        ): IntRange {
            val start = maxOf(0, firstVisibleIndex - 10) // Buffer before
            val end = minOf(totalMessages - 1, firstVisibleIndex + visibleItemCount + 10) // Buffer after
            return start..end
        }
    }
    
    /**
     * Text-to-Speech optimization to prevent audio conflicts.
     */
    object TTSOptimization {
        private var currentlySpeaking = false
        
        fun setCurrentlySpeaking(speaking: Boolean) {
            currentlySpeaking = speaking
        }
        
        fun isCurrentlySpeaking(): Boolean = currentlySpeaking
        
        /**
         * Queue management for TTS to prevent overlapping speech.
         */
        private val speechQueue = mutableListOf<String>()
        
        fun queueSpeech(text: String) {
            speechQueue.add(text)
        }
        
        fun getNextSpeechItem(): String? {
            return if (speechQueue.isNotEmpty()) {
                speechQueue.removeAt(0)
            } else null
        }
        
        fun clearSpeechQueue() {
            speechQueue.clear()
        }
    }
    
    /**
     * Content filtering optimization for reduced latency.
     */
    object FilteringOptimization {
        
        /**
         * Cache for recently filtered content to avoid re-processing.
         */
        private val filterCache = mutableMapOf<String, Boolean>()
        private const val MAX_CACHE_SIZE = 100
        
        fun getCachedFilterResult(input: String): Boolean? {
            return filterCache[input.lowercase().trim()]
        }
        
        fun cacheFilterResult(input: String, isAppropriate: Boolean) {
            val key = input.lowercase().trim()
            if (filterCache.size >= MAX_CACHE_SIZE) {
                // Remove oldest entries (simple LRU-like behavior)
                val keysToRemove = filterCache.keys.take(10)
                keysToRemove.forEach { filterCache.remove(it) }
            }
            filterCache[key] = isAppropriate
        }
        
        fun clearFilterCache() {
            filterCache.clear()
        }
    }
    
    /**
     * Lock screen overlay performance optimization.
     */
    object LockScreenOptimization {
        
        /**
         * Reduce animation complexity when running as overlay.
         */
        private var isOverlayMode = false
        
        fun setOverlayMode(overlay: Boolean) {
            isOverlayMode = overlay
        }
        
        fun isOverlayMode(): Boolean = isOverlayMode
        
        /**
         * Get animation duration based on mode.
         * Shorter animations for overlay mode to reduce resource usage.
         */
        fun getAnimationDuration(): Int {
            return if (isOverlayMode) 150 else 300
        }
        
        /**
         * Should use simplified animations in overlay mode.
         */
        fun shouldUseSimplifiedAnimations(): Boolean = isOverlayMode
    }
    
    /**
     * Voice input performance optimization.
     */
    object VoiceInputOptimization {
        
        /**
         * Debounce speech recognition to prevent excessive API calls.
         */
        private var lastSpeechTime = 0L
        private const val SPEECH_DEBOUNCE_MS = 500L
        
        fun shouldProcessSpeech(): Boolean {
            val currentTime = System.currentTimeMillis()
            return if (currentTime - lastSpeechTime > SPEECH_DEBOUNCE_MS) {
                lastSpeechTime = currentTime
                true
            } else {
                false
            }
        }
        
        /**
         * Reset speech timing for new sessions.
         */
        fun resetSpeechTiming() {
            lastSpeechTime = 0L
        }
    }
    
    /**
     * Cleanup function to call when screen is disposed.
     */
    fun cleanup() {
        clearGradientCache()
        FilteringOptimization.clearFilterCache()
        TTSOptimization.clearSpeechQueue()
        VoiceInputOptimization.resetSpeechTiming()
        LockScreenOptimization.setOverlayMode(false) // Reset overlay mode
        PerformanceMonitor.clearFrameHistory()
    }
    
    /**
     * Performance monitoring and logging.
     */
    object PerformanceMonitor {
        private var lastFrameTime = 0L
        private val frameTimings = mutableListOf<Long>()
        private const val MAX_FRAME_HISTORY = 60 // Track last 60 frames
        
        fun recordFrameTime() {
            val currentTime = System.currentTimeMillis()
            if (lastFrameTime > 0) {
                val frameDuration = currentTime - lastFrameTime
                frameTimings.add(frameDuration)
                
                if (frameTimings.size > MAX_FRAME_HISTORY) {
                    frameTimings.removeAt(0)
                }
            }
            lastFrameTime = currentTime
        }
        
        fun getAverageFrameTime(): Double {
            return if (frameTimings.isNotEmpty()) {
                frameTimings.average()
            } else 0.0
        }
        
        fun isPerformanceGood(): Boolean {
            val avgFrameTime = getAverageFrameTime()
            return avgFrameTime < 16.67 // 60 FPS target
        }
        
        fun clearFrameHistory() {
            frameTimings.clear()
            lastFrameTime = 0L
        }
    }
}

/**
 * Composable helper for optimized gradients.
 */
@Composable
fun rememberOptimizedGradient(
    key: String,
    colors: List<Color>,
    isVertical: Boolean = false
): Brush {
    return remember(key, colors) {
        if (isVertical) {
            ChatScreenPerformance.getCachedVerticalGradient(key, colors)
        } else {
            ChatScreenPerformance.getCachedGradient(key, colors)
        }
    }
}

/**
 * Performance-aware DisposableEffect for cleanup.
 */
@Composable
fun PerformanceAwareDisposableEffect() {
    DisposableEffect(Unit) {
        onDispose {
            ChatScreenPerformance.cleanup()
        }
    }
} 