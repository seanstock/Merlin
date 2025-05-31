package com.example.merlin.security

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for performing security checks triggered by critical events.
 * This ensures security checks happen even if the main monitoring system is compromised.
 */
class CriticalEventSecurityWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "CriticalEventSecurityWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            val eventType = inputData.getString("event_type") ?: "UNKNOWN"
            val eventContext = inputData.getString("event_context") ?: ""
            val eventTimestamp = inputData.getLong("event_timestamp", System.currentTimeMillis())
            
            Log.d(TAG, "Starting critical event security check for: $eventType")
            
            val securityManager = SecurityManager(applicationContext)
            val threat = securityManager.enforceSecurityMeasures()
            
            if (threat != null) {
                Log.w(TAG, "Security threat detected during critical event check: $threat (Event: $eventType)")
                
                // Trigger security response
                val securityResponseManager = SecurityResponseManager(applicationContext)
                securityResponseManager.respondToThreat(threat)
                
                // Log the detection with event context
                Log.w(TAG, "Critical event security check detected threat: $threat for event: $eventType")
            } else {
                Log.d(TAG, "Critical event security check completed - no threats detected for event: $eventType")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during critical event security check", e)
            Result.retry()
        }
    }
} 