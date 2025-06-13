package com.example.merlin

import android.app.Application
import android.util.Log
import com.example.merlin.ui.game.GameManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Custom Application class for Merlin that handles app-level initialization.
 * Initializes services that need to be available throughout the app lifetime.
 */
class MerlinApplication : Application() {
    
    companion object {
        private const val TAG = "MerlinApplication"
    }
    
    // Application-level coroutine scope for long-running operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Merlin application starting...")
        
        // Initialize GameManager at application startup for instant game list access
        initializeGameManager()
        
        Log.d(TAG, "Merlin application initialized successfully")
    }
    
    /**
     * Initialize GameManager singleton at application startup.
     * This ensures games are immediately available when user navigates to game screen.
     */
    private fun initializeGameManager() {
        try {
            // Initialize GameManager singleton with application context and scope
            GameManager.getInstance(this, applicationScope)
            Log.d(TAG, "GameManager initialized at application startup")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GameManager at startup", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Merlin application terminating...")
    }
} 