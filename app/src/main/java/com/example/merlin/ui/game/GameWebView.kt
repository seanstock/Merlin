package com.example.merlin.ui.game

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

/**
 * Secure WebView component for hosting React-based HTML games.
 * Implements comprehensive security measures including CSP, origin whitelisting,
 * and JavaScript interface restrictions.
 */
@Composable
fun GameWebView(
    gameId: String,
    level: Int = 1,
    onGameComplete: (Boolean, Long, Int) -> Unit,
    onGameProgress: (Int) -> Unit = {},
    onGameError: (String) -> Unit = {},
    onCoinEarned: ((Int, String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    configureSecureWebView(
                        gameId = gameId,
                        level = level,
                        onGameComplete = onGameComplete,
                        onGameProgress = onGameProgress,
                        onGameError = onGameError,
                        onCoinEarned = onCoinEarned,
                        onLoadingChanged = { loading -> isLoading = loading },
                        onError = { error -> loadError = error }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading game...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Error display
        loadError?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Game Load Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Configures WebView with comprehensive security settings and game integration.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSecureWebView(
    gameId: String,
    level: Int,
    onGameComplete: (Boolean, Long, Int) -> Unit,
    onGameProgress: (Int) -> Unit,
    onGameError: (String) -> Unit,
    onCoinEarned: ((Int, String, String) -> Unit)?,
    onLoadingChanged: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val gameBridge = GameBridge(
        onGameComplete = onGameComplete,
        onGameProgress = onGameProgress,
        onGameError = onGameError,
        onCoinEarned = onCoinEarned
    )

    // Configure WebView settings with security focus
    settings.apply {
        // Enable JavaScript (required for React games)
        javaScriptEnabled = true
        
        // Enable DOM storage for game state
        domStorageEnabled = true
        
        // Disable potentially dangerous features
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        
        // Disable geolocation and other sensitive APIs
        setGeolocationEnabled(false)
        
        // Configure caching for performance
        cacheMode = WebSettings.LOAD_DEFAULT
        
        // Disable zoom controls (games should handle their own scaling)
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
        
        // Configure text settings
        textZoom = 100
        
        // Disable safe browsing for local content
        safeBrowsingEnabled = false
        
        // Configure media settings
        mediaPlaybackRequiresUserGesture = false
    }

    // Add JavaScript interface with restricted name
    addJavascriptInterface(gameBridge, GameBridge.BRIDGE_NAME)

    // Configure WebViewClient for security and navigation control
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return true
            
            // Only allow navigation within game assets
            return if (isAllowedGameUrl(url, gameId)) {
                false // Allow navigation
            } else {
                Log.w("GameWebView", "Blocked navigation to unauthorized URL: $url")
                true // Block navigation
            }
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null
            
            // Implement Content Security Policy for additional security
            if (url.contains("index.html")) {
                return injectCSP(url)
            }
            
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onLoadingChanged(false)
            
            // Inject additional security measures
            view?.evaluateJavascript(getSecurityScript(), null)
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            onError("Failed to load game: $description")
            onLoadingChanged(false)
        }
    }

    // Configure WebChromeClient for console logging and debugging
    webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            consoleMessage?.let { msg ->
                val logLevel = when (msg.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                    ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                    else -> Log.DEBUG
                }
                Log.println(logLevel, "GameWebView", "Game Console: ${msg.message()}")
            }
            return true
        }
    }

    // Load the game
    val gameUrl = "file:///android_asset/games/$gameId/index.html?level=$level"
    loadUrl(gameUrl)
    onLoadingChanged(true)
}

/**
 * Checks if a URL is allowed for game navigation.
 */
private fun isAllowedGameUrl(url: String, gameId: String): Boolean {
    return url.startsWith("file:///android_asset/games/$gameId/") ||
           url.startsWith("data:") ||
           url == "about:blank"
}

/**
 * Injects Content Security Policy into HTML responses.
 */
private fun injectCSP(url: String): WebResourceResponse? {
    return try {
        // This is a simplified CSP injection - in a real implementation,
        // you would read the actual HTML file and inject the CSP header
        val cspHeader = "default-src 'self' 'unsafe-inline' 'unsafe-eval' data:; " +
                       "connect-src 'none'; " +
                       "frame-src 'none'; " +
                       "object-src 'none';"
        
        val headers = mapOf(
            "Content-Security-Policy" to cspHeader,
            "X-Frame-Options" to "DENY",
            "X-Content-Type-Options" to "nosniff"
        )
        
        WebResourceResponse("text/html", "UTF-8", 200, "OK", headers, ByteArrayInputStream(byteArrayOf()))
    } catch (e: Exception) {
        Log.e("GameWebView", "Error injecting CSP", e)
        null
    }
}

/**
 * Returns JavaScript code for additional security measures.
 */
private fun getSecurityScript(): String {
    return """
        (function() {
            // Disable right-click context menu
            document.addEventListener('contextmenu', function(e) {
                e.preventDefault();
            });
            
            // Disable text selection
            document.addEventListener('selectstart', function(e) {
                e.preventDefault();
            });
            
            // Disable drag and drop
            document.addEventListener('dragstart', function(e) {
                e.preventDefault();
            });
            
            // Override console methods in production
            if (typeof ${GameBridge.BRIDGE_NAME} !== 'undefined') {
                const originalLog = console.log;
                console.log = function(...args) {
                    ${GameBridge.BRIDGE_NAME}.logDebug(args.join(' '));
                    originalLog.apply(console, args);
                };
            }
            
            // Prevent navigation away from game
            window.addEventListener('beforeunload', function(e) {
                e.preventDefault();
                e.returnValue = '';
            });
        })();
    """.trimIndent()
} 