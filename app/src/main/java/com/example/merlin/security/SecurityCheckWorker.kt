package com.example.merlin.security

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for performing periodic security checks in the background.
 * This provides a backup mechanism for security monitoring when the app is not active.
 */
class SecurityCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SecurityCheckWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting periodic security check via WorkManager")
            
            val securityManager = SecurityManager(applicationContext)
            val threat = securityManager.enforceSecurityMeasures()
            
            if (threat != null) {
                Log.w(TAG, "Security threat detected in background check: $threat")
                
                // Trigger security response
                val securityResponseManager = SecurityResponseManager(applicationContext)
                securityResponseManager.respondToThreat(threat)
                
                // Log the detection
                Log.w(TAG, "Background security check detected threat: $threat")
            } else {
                Log.d(TAG, "Background security check completed - no threats detected")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during background security check", e)
            Result.retry()
        }
    }
} 