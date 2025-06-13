package com.example.merlin.security

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified WorkManager worker for performing security checks in the background.
 * Handles both periodic security checks and critical event-triggered checks
 * based on input data to eliminate code duplication.
 */
class UnifiedSecurityWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "UnifiedSecurityWorker"
        
        // Input data keys
        const val KEY_CHECK_TYPE = "check_type"
        const val KEY_EVENT_TYPE = "event_type"
        const val KEY_EVENT_CONTEXT = "event_context"
        const val KEY_EVENT_TIMESTAMP = "event_timestamp"
        
        // Check types
        const val CHECK_TYPE_PERIODIC = "periodic"
        const val CHECK_TYPE_CRITICAL_EVENT = "critical_event"
        
        /**
         * Create input data for periodic security checks
         */
        fun createPeriodicCheckData() = workDataOf(
            KEY_CHECK_TYPE to CHECK_TYPE_PERIODIC
        )
        
        /**
         * Create input data for critical event security checks
         */
        fun createCriticalEventCheckData(
            eventType: String,
            eventContext: String = "",
            eventTimestamp: Long = System.currentTimeMillis()
        ) = workDataOf(
            KEY_CHECK_TYPE to CHECK_TYPE_CRITICAL_EVENT,
            KEY_EVENT_TYPE to eventType,
            KEY_EVENT_CONTEXT to eventContext,
            KEY_EVENT_TIMESTAMP to eventTimestamp
        )
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            val checkType = inputData.getString(KEY_CHECK_TYPE) ?: CHECK_TYPE_PERIODIC
            
            val logContext = when (checkType) {
                CHECK_TYPE_CRITICAL_EVENT -> {
                    val eventType = inputData.getString(KEY_EVENT_TYPE) ?: "UNKNOWN"
                    val eventContext = inputData.getString(KEY_EVENT_CONTEXT) ?: ""
                    val eventTimestamp = inputData.getLong(KEY_EVENT_TIMESTAMP, System.currentTimeMillis())
                    "critical event: $eventType (context: $eventContext, timestamp: $eventTimestamp)"
                }
                else -> "periodic check"
            }
            
            Log.d(TAG, "Starting security check via WorkManager - $logContext")
            
            val securityManager = SecurityManager(applicationContext)
            val threat = securityManager.enforceSecurityMeasures()
            
            if (threat != null) {
                Log.w(TAG, "Security threat detected during $logContext: $threat")
                
                // Trigger security response
                val securityResponseManager = SecurityResponseManager(applicationContext)
                securityResponseManager.respondToThreat(threat)
                
                // Log the detection with appropriate context
                Log.w(TAG, "Security check detected threat: $threat ($logContext)")
            } else {
                Log.d(TAG, "Security check completed - no threats detected ($logContext)")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during security check", e)
            Result.retry()
        }
    }
} 