package com.example.merlin.ui.game

import android.content.Context
import android.util.Log
import android.webkit.WebView
import com.example.merlin.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages game loading, caching, and performance optimization.
 * Handles preloading of games, WebView pooling, and resource management.
 */
class GameManager private constructor(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "GameManager"
        private const val MAX_PRELOADED_GAMES = 3
        private const val WEBVIEW_POOL_SIZE = 2
        
        @Volatile
        private var INSTANCE: GameManager? = null
        
        fun getInstance(context: Context, coroutineScope: CoroutineScope): GameManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameManager(context.applicationContext, coroutineScope).also { INSTANCE = it }
            }
        }
    }

    // Game metadata cache
    private val gameMetadata = ConcurrentHashMap<String, GameMetadata>()
    
    // StateFlow for reactive games list
    private val _availableGames = MutableStateFlow<List<GameMetadata>>(emptyList())
    val availableGamesFlow: StateFlow<List<GameMetadata>> = _availableGames.asStateFlow()
    
    // Preloaded game cache
    private val preloadedGames = ConcurrentHashMap<String, PreloadedGame>()
    
    // WebView pool for performance
    private val webViewPool = mutableListOf<WebView>()
    private val poolLock = Any()

    init {
        initializeGameManager()
    }

    /**
     * Initialize the game manager using static game registry.
     * This is now instant since we use pre-defined games instead of asset discovery.
     */
    private fun initializeGameManager() {
        coroutineScope.launch {
            try {
                loadGamesFromRegistry()
                initializeWebViewPoolLazy() // Defer WebView creation
                Log.d(TAG, "GameManager initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GameManager", e)
            }
        }
    }

    /**
     * Load games from static registry - instant loading, no asset discovery needed.
     */
    private fun loadGamesFromRegistry() {
        try {
            // Load all games from static registry
            val games = GameRegistry.getAllGames()
            
            // Populate metadata cache
            games.forEach { game ->
                gameMetadata[game.id] = game
            }
            
            // Emit games list immediately
            _availableGames.value = games
            
            Log.d(TAG, "Loaded ${games.size} games from static registry")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading games from registry", e)
        }
    }

    /**
     * Create metadata for a discovered game (DEPRECATED - now using static registry).
     * Kept for backward compatibility but should not be used.
     */
    @Deprecated("Use GameRegistry instead", ReplaceWith("GameRegistry.getGameById(gameId)"))
    private fun createGameMetadata(gameId: String): GameMetadata {
        return GameMetadata(
            id = gameId,
            name = gameId.replace("-", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
            description = "Interactive game: $gameId",
            maxLevel = 10, // Default, could be read from game config
            estimatedLoadTime = 2000L, // Default 2 seconds
            requiresNetwork = false,
            supportedFeatures = listOf("touch", "timer", "scoring")
        )
    }

    /**
     * Initialize WebView pool lazily - don't block startup.
     * WebViews are created when first needed instead of at initialization.
     */
    private fun initializeWebViewPoolLazy() {
        // Mark pool as ready for lazy creation - actual WebViews created on demand
        Log.d(TAG, "WebView pool configured for lazy initialization")
    }
    
    /**
     * Initialize WebView pool for better performance (DEPRECATED - using lazy loading).
     * Kept for backward compatibility.
     */
    @Deprecated("Use lazy WebView creation instead")
    private suspend fun initializeWebViewPool() = withContext(Dispatchers.Main) {
        try {
            synchronized(poolLock) {
                repeat(WEBVIEW_POOL_SIZE) {
                    val webView = WebView(context).apply {
                        // Pre-configure WebView settings
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                        }
                    }
                    webViewPool.add(webView)
                }
            }
            Log.d(TAG, "WebView pool initialized with $WEBVIEW_POOL_SIZE instances")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebView pool", e)
        }
    }

    /**
     * Get a WebView from the pool or create a new one.
     * Now creates WebViews lazily on demand for better startup performance.
     */
    fun getWebView(): WebView {
        synchronized(poolLock) {
            return if (webViewPool.isNotEmpty()) {
                webViewPool.removeAt(0).also {
                    Log.d(TAG, "WebView retrieved from pool")
                }
            } else {
                Log.d(TAG, "Creating new WebView on demand")
                WebView(context).apply {
                    // Pre-configure WebView settings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                    }
                }
            }
        }
    }

    /**
     * Return a WebView to the pool for reuse.
     */
    fun returnWebView(webView: WebView) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                // Clean up the WebView
                webView.apply {
                    stopLoading()
                    clearHistory()
                    clearCache(true)
                    loadUrl("about:blank")
                    removeJavascriptInterface(GameBridge.BRIDGE_NAME)
                }
                
                synchronized(poolLock) {
                    if (webViewPool.size < WEBVIEW_POOL_SIZE) {
                        webViewPool.add(webView)
                        Log.d(TAG, "WebView returned to pool")
                    } else {
                        webView.destroy()
                        Log.d(TAG, "WebView destroyed (pool full)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error returning WebView to pool", e)
                try {
                    webView.destroy()
                } catch (destroyError: Exception) {
                    Log.e(TAG, "Error destroying WebView", destroyError)
                }
            }
        }
    }

    /**
     * Preload a game for faster loading.
     */
    fun preloadGame(gameId: String, level: Int = 1) {
        coroutineScope.launch {
            try {
                if (preloadedGames.containsKey(gameId)) {
                    Log.d(TAG, "Game $gameId already preloaded")
                    return@launch
                }
                
                if (preloadedGames.size >= MAX_PRELOADED_GAMES) {
                    // Remove oldest preloaded game
                    val oldestGame = preloadedGames.values.minByOrNull { it.loadTime }
                    oldestGame?.let { 
                        preloadedGames.remove(it.gameId)
                        Log.d(TAG, "Removed oldest preloaded game: ${it.gameId}")
                    }
                }
                
                val startTime = System.currentTimeMillis()
                val gameUrl = "file:///android_asset/games/$gameId/index.html?level=$level"
                
                // Create preloaded game entry
                val preloadedGame = PreloadedGame(
                    gameId = gameId,
                    level = level,
                    gameUrl = gameUrl,
                    loadTime = startTime,
                    isReady = true
                )
                
                preloadedGames[gameId] = preloadedGame
                
                val loadTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Game $gameId preloaded in ${loadTime}ms")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload game: $gameId", e)
            }
        }
    }

    /**
     * Get available games list.
     */
    fun getAvailableGames(): List<GameMetadata> {
        return gameMetadata.values.toList()
    }

    /**
     * Get metadata for a specific game.
     */
    fun getGameMetadata(gameId: String): GameMetadata? {
        return gameMetadata[gameId]
    }

    /**
     * Check if a game is preloaded.
     */
    fun isGamePreloaded(gameId: String): Boolean {
        return preloadedGames.containsKey(gameId) && preloadedGames[gameId]?.isReady == true
    }

    /**
     * Get game URL with optimizations.
     */
    fun getGameUrl(gameId: String, level: Int): String {
        val baseUrl = "file:///android_asset/games/$gameId/index.html"
        val params = mutableListOf<String>()
        
        params.add("level=$level")
        
        // Add cache busting for development
        if (BuildConfig.DEBUG) {
            params.add("t=${System.currentTimeMillis()}")
        }
        
        return if (params.isNotEmpty()) {
            "$baseUrl?${params.joinToString("&")}"
        } else {
            baseUrl
        }
    }

    /**
     * Preload games based on usage patterns or predictions.
     */
    fun preloadRecommendedGames() {
        coroutineScope.launch {
            try {
                // Preload the sample game by default
                preloadGame("sample-game", 1)
                
                // Could implement more sophisticated preloading logic here
                // based on user preferences, recent games, etc.
                
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading recommended games", e)
            }
        }
    }

    /**
     * Clear all preloaded games to free memory.
     */
    fun clearPreloadedGames() {
        preloadedGames.clear()
        Log.d(TAG, "Cleared all preloaded games")
    }

    /**
     * Clean up resources when the manager is no longer needed.
     */
    fun cleanup() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                synchronized(poolLock) {
                    webViewPool.forEach { webView ->
                        try {
                            webView.destroy()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error destroying WebView during cleanup", e)
                        }
                    }
                    webViewPool.clear()
                }
                
                clearPreloadedGames()
                gameMetadata.clear()
                
                Log.d(TAG, "GameManager cleanup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during GameManager cleanup", e)
            }
        }
    }
}

/**
 * Metadata about a game.
 */
data class GameMetadata(
    val id: String,
    val name: String,
    val description: String,
    val maxLevel: Int,
    val estimatedLoadTime: Long,
    val requiresNetwork: Boolean,
    val supportedFeatures: List<String>
)

/**
 * Represents a preloaded game.
 */
data class PreloadedGame(
    val gameId: String,
    val level: Int,
    val gameUrl: String,
    val loadTime: Long,
    val isReady: Boolean
) 