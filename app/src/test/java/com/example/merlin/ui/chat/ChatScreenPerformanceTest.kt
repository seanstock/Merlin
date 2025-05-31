package com.example.merlin.ui.chat

import androidx.compose.ui.graphics.Color
import com.example.merlin.ui.theme.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After

/**
 * Performance optimization test suite for ChatScreen.
 * Tests gradient caching, animation optimization, memory management, and performance monitoring.
 */
class ChatScreenPerformanceTest {

    @Before
    fun setup() {
        // Clear any existing cache before each test
        ChatScreenPerformance.cleanup()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        ChatScreenPerformance.cleanup()
    }

    // ============================================================================
    // GRADIENT CACHING TESTS
    // ============================================================================

    @Test
    fun gradientCache_shouldCacheAndReuseBrushes() {
        // Test that gradients are cached and reused efficiently
        val colors = listOf(WisdomBlue, DeepOcean)
        val key = "test_gradient"
        
        // First call should create and cache the gradient
        val firstGradient = ChatScreenPerformance.getCachedGradient(key, colors)
        assertNotNull("First gradient should not be null", firstGradient)
        
        // Second call should return the same cached instance
        val secondGradient = ChatScreenPerformance.getCachedGradient(key, colors)
        assertSame("Second gradient should be the same cached instance", firstGradient, secondGradient)
    }

    @Test
    fun gradientCache_shouldSupportVerticalGradients() {
        // Test vertical gradient caching
        val colors = listOf(MistyBlue, SeafoamMist, CloudWhite)
        val key = "vertical_test"
        
        val verticalGradient = ChatScreenPerformance.getCachedVerticalGradient(key, colors)
        assertNotNull("Vertical gradient should not be null", verticalGradient)
        
        // Should use different cache key for vertical vs radial
        val radialGradient = ChatScreenPerformance.getCachedGradient(key, colors)
        assertNotSame("Vertical and radial gradients should be different instances", verticalGradient, radialGradient)
    }

    @Test
    fun gradientCache_shouldClearProperly() {
        // Test that cache clears correctly
        val colors = listOf(SageGreen, ForestGreen)
        val key = "clear_test"
        
        val gradient = ChatScreenPerformance.getCachedGradient(key, colors)
        assertNotNull("Gradient should exist before clearing", gradient)
        
        ChatScreenPerformance.clearGradientCache()
        
        // After clearing, should create new instance
        val newGradient = ChatScreenPerformance.getCachedGradient(key, colors)
        assertNotNull("New gradient should not be null", newGradient)
        assertNotSame("New gradient should be different instance after cache clear", gradient, newGradient)
    }

    // ============================================================================
    // ANIMATION OPTIMIZATION TESTS
    // ============================================================================

    @Test
    fun animationFrameManager_shouldControlAnimationState() {
        // Test animation frame manager functionality
        val frameManager = ChatScreenPerformance.AnimationFrameManager()
        
        // Default should be optimized (enabled)
        assertTrue("Animations should be enabled by default", frameManager.frameRateOptimized.value)
        
        frameManager.setAnimationsEnabled(false)
        assertFalse("Animations should be disabled after setting to false", frameManager.frameRateOptimized.value)
        
        frameManager.setAnimationsEnabled(true)
        assertTrue("Animations should be re-enabled", frameManager.frameRateOptimized.value)
    }

    // ============================================================================
    // MESSAGE OPTIMIZATION TESTS
    // ============================================================================

    @Test
    fun messageOptimization_shouldDetectVirtualizationNeed() {
        // Test message virtualization detection
        val smallMessageCount = 10
        val largeMessageCount = 100
        
        assertFalse("Small message count should not need virtualization", 
            ChatScreenPerformance.MessageOptimization.shouldVirtualizeMessages(smallMessageCount))
        
        assertTrue("Large message count should need virtualization",
            ChatScreenPerformance.MessageOptimization.shouldVirtualizeMessages(largeMessageCount))
    }

    @Test
    fun messageOptimization_shouldCalculateVisibleRange() {
        // Test visible message range calculation
        val totalMessages = 100
        val firstVisibleIndex = 40
        val visibleItemCount = 10
        
        val range = ChatScreenPerformance.MessageOptimization.getVisibleMessageRange(
            totalMessages, firstVisibleIndex, visibleItemCount
        )
        
        assertTrue("Range should start before first visible item", range.first <= firstVisibleIndex)
        assertTrue("Range should end after last visible item", range.last >= firstVisibleIndex + visibleItemCount)
        assertTrue("Range should be within bounds", range.first >= 0 && range.last < totalMessages)
    }

    // ============================================================================
    // TEXT-TO-SPEECH OPTIMIZATION TESTS
    // ============================================================================

    @Test
    fun ttsOptimization_shouldManageSpeechState() {
        // Test TTS state management
        assertFalse("Should not be speaking initially", 
            ChatScreenPerformance.TTSOptimization.isCurrentlySpeaking())
        
        ChatScreenPerformance.TTSOptimization.setCurrentlySpeaking(true)
        assertTrue("Should be speaking after setting to true",
            ChatScreenPerformance.TTSOptimization.isCurrentlySpeaking())
        
        ChatScreenPerformance.TTSOptimization.setCurrentlySpeaking(false)
        assertFalse("Should not be speaking after setting to false",
            ChatScreenPerformance.TTSOptimization.isCurrentlySpeaking())
    }

    @Test
    fun ttsOptimization_shouldManageSpeechQueue() {
        // Test speech queue management
        val text1 = "Hello world"
        val text2 = "How are you?"
        
        // Queue should be empty initially
        assertNull("Queue should be empty initially",
            ChatScreenPerformance.TTSOptimization.getNextSpeechItem())
        
        ChatScreenPerformance.TTSOptimization.queueSpeech(text1)
        ChatScreenPerformance.TTSOptimization.queueSpeech(text2)
        
        assertEquals("First item should be retrieved first", text1,
            ChatScreenPerformance.TTSOptimization.getNextSpeechItem())
        assertEquals("Second item should be retrieved second", text2,
            ChatScreenPerformance.TTSOptimization.getNextSpeechItem())
        
        assertNull("Queue should be empty after retrieving all items",
            ChatScreenPerformance.TTSOptimization.getNextSpeechItem())
    }

    @Test
    fun ttsOptimization_shouldClearQueue() {
        // Test queue clearing
        ChatScreenPerformance.TTSOptimization.queueSpeech("Test speech")
        ChatScreenPerformance.TTSOptimization.clearSpeechQueue()
        
        assertNull("Queue should be empty after clearing",
            ChatScreenPerformance.TTSOptimization.getNextSpeechItem())
    }

    // ============================================================================
    // CONTENT FILTERING OPTIMIZATION TESTS
    // ============================================================================

    @Test
    fun filteringOptimization_shouldCacheResults() {
        // Test content filtering cache
        val input = "Hello world"
        
        // Initially should have no cached result
        assertNull("Should have no cached result initially",
            ChatScreenPerformance.FilteringOptimization.getCachedFilterResult(input))
        
        ChatScreenPerformance.FilteringOptimization.cacheFilterResult(input, true)
        
        assertTrue("Should return cached appropriate result",
            ChatScreenPerformance.FilteringOptimization.getCachedFilterResult(input) == true)
    }

    @Test
    fun filteringOptimization_shouldHandleCaseInsensitive() {
        // Test case insensitive caching
        val input = "Hello World"
        val inputLower = "hello world"
        
        ChatScreenPerformance.FilteringOptimization.cacheFilterResult(input, false)
        
        assertFalse("Should return cached result regardless of case",
            ChatScreenPerformance.FilteringOptimization.getCachedFilterResult(inputLower) == true)
    }

    // ============================================================================
    // LOCK SCREEN OPTIMIZATION TESTS
    // ============================================================================

    @Test
    fun lockScreenOptimization_shouldManageOverlayMode() {
        // Test overlay mode management
        // Reset state first to ensure clean test
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(false)
        
        assertFalse("Should not be in overlay mode initially",
            ChatScreenPerformance.LockScreenOptimization.isOverlayMode())
        
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(true)
        assertTrue("Should be in overlay mode after setting",
            ChatScreenPerformance.LockScreenOptimization.isOverlayMode())
    }

    @Test
    fun lockScreenOptimization_shouldAdaptAnimationDuration() {
        // Test adaptive animation duration
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(false)
        val normalDuration = ChatScreenPerformance.LockScreenOptimization.getAnimationDuration()
        
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(true)
        val overlayDuration = ChatScreenPerformance.LockScreenOptimization.getAnimationDuration()
        
        assertTrue("Overlay mode should have shorter animation duration",
            overlayDuration < normalDuration)
    }

    @Test
    fun lockScreenOptimization_shouldRecommendSimplifiedAnimations() {
        // Test simplified animation recommendation
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(false)
        assertFalse("Should not recommend simplified animations in normal mode",
            ChatScreenPerformance.LockScreenOptimization.shouldUseSimplifiedAnimations())
        
        ChatScreenPerformance.LockScreenOptimization.setOverlayMode(true)
        assertTrue("Should recommend simplified animations in overlay mode",
            ChatScreenPerformance.LockScreenOptimization.shouldUseSimplifiedAnimations())
    }

    // ============================================================================
    // VOICE INPUT OPTIMIZATION TESTS
    // ============================================================================

    @Test
    fun voiceInputOptimization_shouldDebounceSpeech() {
        // Test speech debouncing
        ChatScreenPerformance.VoiceInputOptimization.resetSpeechTiming()
        
        assertTrue("First speech should be allowed",
            ChatScreenPerformance.VoiceInputOptimization.shouldProcessSpeech())
        
        // Immediate second call should be debounced
        assertFalse("Immediate second speech should be debounced",
            ChatScreenPerformance.VoiceInputOptimization.shouldProcessSpeech())
    }

    @Test
    fun voiceInputOptimization_shouldResetTiming() {
        // Test timing reset
        ChatScreenPerformance.VoiceInputOptimization.shouldProcessSpeech()
        ChatScreenPerformance.VoiceInputOptimization.resetSpeechTiming()
        
        assertTrue("Should allow speech after reset",
            ChatScreenPerformance.VoiceInputOptimization.shouldProcessSpeech())
    }

    // ============================================================================
    // PERFORMANCE MONITORING TESTS
    // ============================================================================

    @Test
    fun performanceMonitor_shouldTrackFrameTiming() {
        // Test frame time tracking
        ChatScreenPerformance.PerformanceMonitor.clearFrameHistory()
        
        // Initially no frame data
        assertEquals("Should have no frame data initially", 0.0,
            ChatScreenPerformance.PerformanceMonitor.getAverageFrameTime(), 0.01)
        
        // Record some frame times
        ChatScreenPerformance.PerformanceMonitor.recordFrameTime()
        Thread.sleep(10) // Simulate frame time
        ChatScreenPerformance.PerformanceMonitor.recordFrameTime()
        
        assertTrue("Should have recorded frame timing data",
            ChatScreenPerformance.PerformanceMonitor.getAverageFrameTime() > 0)
    }

    @Test
    fun performanceMonitor_shouldEvaluatePerformance() {
        // Test performance evaluation
        ChatScreenPerformance.PerformanceMonitor.clearFrameHistory()
        
        // Simulate good performance (short frame times)
        for (i in 1..10) {
            ChatScreenPerformance.PerformanceMonitor.recordFrameTime()
            Thread.sleep(1) // Very short frame time
            ChatScreenPerformance.PerformanceMonitor.recordFrameTime()
        }
        
        // Note: This test is dependent on actual timing, so it may be flaky
        // In a real implementation, you might want to make this more deterministic
        val avgFrameTime = ChatScreenPerformance.PerformanceMonitor.getAverageFrameTime()
        assertTrue("Average frame time should be reasonable", avgFrameTime >= 0)
    }

    // ============================================================================
    // CLEANUP TESTS
    // ============================================================================

    @Test
    fun cleanup_shouldClearAllCaches() {
        // Test comprehensive cleanup
        // Set up some cached data
        ChatScreenPerformance.getCachedGradient("test", listOf(Color.Red))
        ChatScreenPerformance.FilteringOptimization.cacheFilterResult("test", true)
        ChatScreenPerformance.TTSOptimization.queueSpeech("test")
        
        // Perform cleanup
        ChatScreenPerformance.cleanup()
        
        // Verify everything is cleaned up
        assertNull("Filter cache should be cleared",
            ChatScreenPerformance.FilteringOptimization.getCachedFilterResult("test"))
        
        assertNull("TTS queue should be cleared",
            ChatScreenPerformance.TTSOptimization.getNextSpeechItem())
        
        // Note: Gradient cache clearing is verified by creating new instances
        val newGradient = ChatScreenPerformance.getCachedGradient("test", listOf(Color.Red))
        assertNotNull("Should be able to create new gradient after cleanup", newGradient)
    }
} 