package com.example.merlin.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.*
import kotlinx.coroutines.*
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Runtime security monitoring system that performs periodic security checks,
 * monitors critical app events, and provides tamper-resistant logging.
 */
class RuntimeSecurityMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "RuntimeSecurityMonitor"
        private const val SECURITY_LOG_PREFS = "merlin_security_logs"
        private const val KEY_LOG_ENTRIES = "log_entries"
        private const val KEY_LOG_COUNT = "log_count"
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_CHECK_INTERVAL = "check_interval"
        
        // Security check intervals (with randomization to prevent timing attacks)
        private const val BASE_CHECK_INTERVAL = 30 * 1000L // 30 seconds
        private const val INTERVAL_VARIANCE = 15 * 1000L // ±15 seconds
        private const val CRITICAL_EVENT_DELAY = 2 * 1000L // 2 seconds after critical events
        
        // Maximum log entries to prevent storage bloat
        private const val MAX_LOG_ENTRIES = 1000
        
        // Work manager tags
        private const val PERIODIC_SECURITY_WORK = "periodic_security_check"
        private const val CRITICAL_EVENT_WORK = "critical_event_security_check"
    }
    
    private val securityManager = SecurityManager(context)
    private val securityResponseManager = SecurityResponseManager(context)
    private val secureRandom = SecureRandom()
    
    private val securityLogPrefs: SharedPreferences by lazy {
        createEncryptedLogPreferences()
    }
    
    private val eventCallbacks = ConcurrentHashMap<String, SecurityEventCallback>()
    private val handler = Handler(Looper.getMainLooper())
    
    private var isMonitoringActive = false
    private var monitoringJob: Job? = null
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Interface for security event callbacks.
     */
    interface SecurityEventCallback {
        fun onSecurityCheckCompleted(result: SecurityCheckResult)
        fun onCriticalEventDetected(event: CriticalSecurityEvent)
        fun onTamperAttemptDetected(attempt: TamperAttempt)
    }
    
    /**
     * Data class representing a security check result.
     */
    data class SecurityCheckResult(
        val timestamp: Long,
        val checkType: SecurityCheckType,
        val threat: SecurityThreat?,
        val checkDuration: Long,
        val additionalInfo: Map<String, Any> = emptyMap()
    )
    
    /**
     * Data class representing a critical security event.
     */
    data class CriticalSecurityEvent(
        val timestamp: Long,
        val eventType: CriticalEventType,
        val context: String,
        val securityLevel: SecurityLevel,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    /**
     * Data class representing a tamper attempt.
     */
    data class TamperAttempt(
        val timestamp: Long,
        val attemptType: TamperType,
        val severity: TamperSeverity,
        val details: String,
        val evidence: Map<String, Any> = emptyMap()
    )
    
    /**
     * Enum for different types of security checks.
     */
    enum class SecurityCheckType {
        PERIODIC_BACKGROUND,
        CRITICAL_EVENT_TRIGGERED,
        USER_INITIATED,
        SYSTEM_STATE_CHANGE,
        TAMPER_DETECTION
    }
    
    /**
     * Enum for critical events that trigger security checks.
     */
    enum class CriticalEventType {
        APP_LAUNCH,
        USER_LOGIN,
        PAYMENT_INITIATED,
        SENSITIVE_DATA_ACCESS,
        SETTINGS_CHANGE,
        PERMISSION_GRANTED,
        EXTERNAL_STORAGE_ACCESS,
        NETWORK_STATE_CHANGE,
        SCREEN_UNLOCK,
        APP_INSTALL_DETECTED
    }
    
    /**
     * Enum for security levels.
     */
    enum class SecurityLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Enum for tamper attempt types.
     */
    enum class TamperType {
        TIMING_MANIPULATION,
        MEMORY_INJECTION,
        DEBUGGER_ATTACHMENT,
        HOOK_DETECTION,
        EMULATOR_DETECTION,
        SIGNATURE_MISMATCH,
        RUNTIME_MODIFICATION
    }
    
    /**
     * Enum for tamper severity levels.
     */
    enum class TamperSeverity {
        SUSPICIOUS,
        MODERATE,
        HIGH,
        CRITICAL
    }
    
    /**
     * Start runtime security monitoring.
     */
    fun startMonitoring() {
        if (isMonitoringActive) {
            Log.w(TAG, "Security monitoring is already active")
            return
        }
        
        Log.i(TAG, "Starting runtime security monitoring")
        isMonitoringActive = true
        
        // Start periodic background checks
        startPeriodicSecurityChecks()
        
        // Start continuous monitoring coroutine
        startContinuousMonitoring()
        
        // Schedule WorkManager periodic checks as backup
        schedulePeriodicWorkManagerChecks()
        
        logSecurityEvent("MONITORING_STARTED", "Runtime security monitoring activated", SecurityLevel.MEDIUM)
    }
    
    /**
     * Stop runtime security monitoring.
     */
    fun stopMonitoring() {
        if (!isMonitoringActive) {
            Log.w(TAG, "Security monitoring is not active")
            return
        }
        
        Log.i(TAG, "Stopping runtime security monitoring")
        isMonitoringActive = false
        
        // Cancel all monitoring activities
        monitoringJob?.cancel()
        cancelPeriodicWorkManagerChecks()
        
        logSecurityEvent("MONITORING_STOPPED", "Runtime security monitoring deactivated", SecurityLevel.MEDIUM)
    }
    
    /**
     * Trigger security check for critical events.
     */
    fun triggerCriticalEventCheck(eventType: CriticalEventType, context: String = "", metadata: Map<String, Any> = emptyMap()) {
        Log.d(TAG, "Triggering critical event security check: $eventType")
        
        val event = CriticalSecurityEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            context = context,
            securityLevel = determineSecurityLevel(eventType),
            metadata = metadata
        )
        
        // Notify callbacks
        eventCallbacks.values.forEach { callback ->
            try {
                callback.onCriticalEventDetected(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error in critical event callback", e)
            }
        }
        
        // Schedule delayed security check to avoid timing attacks
        scheduleCriticalEventSecurityCheck(event)
        
        logSecurityEvent("CRITICAL_EVENT", "Critical event detected: $eventType", event.securityLevel, metadata)
    }
    
    /**
     * Perform immediate security check.
     */
    suspend fun performSecurityCheck(checkType: SecurityCheckType): SecurityCheckResult {
        val startTime = System.currentTimeMillis()
        
        // Add random delay to prevent timing attacks
        val randomDelay = secureRandom.nextInt(500) + 100 // 100-600ms
        delay(randomDelay.toLong())
        
        Log.d(TAG, "Performing security check: $checkType")
        
        val threat = securityManager.enforceSecurityMeasures()
        val checkDuration = System.currentTimeMillis() - startTime
        
        val result = SecurityCheckResult(
            timestamp = startTime,
            checkType = checkType,
            threat = threat,
            checkDuration = checkDuration,
            additionalInfo = gatherAdditionalSecurityInfo()
        )
        
        // Handle detected threats
        if (threat != null) {
            Log.w(TAG, "Security threat detected during runtime check: $threat")
            handleDetectedThreat(threat, result)
        }
        
        // Notify callbacks
        eventCallbacks.values.forEach { callback ->
            try {
                callback.onSecurityCheckCompleted(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in security check callback", e)
            }
        }
        
        // Log the security check
        logSecurityCheck(result)
        
        return result
    }
    
    /**
     * Register a security event callback.
     */
    fun registerCallback(id: String, callback: SecurityEventCallback) {
        eventCallbacks[id] = callback
        Log.d(TAG, "Registered security event callback: $id")
    }
    
    /**
     * Unregister a security event callback.
     */
    fun unregisterCallback(id: String) {
        eventCallbacks.remove(id)
        Log.d(TAG, "Unregistered security event callback: $id")
    }
    
    /**
     * Get security monitoring statistics.
     */
    fun getMonitoringStats(): SecurityMonitoringStats {
        val logCount = securityLogPrefs.getInt(KEY_LOG_COUNT, 0)
        val lastCheck = securityLogPrefs.getLong(KEY_LAST_CHECK, 0)
        val checkInterval = securityLogPrefs.getLong(KEY_CHECK_INTERVAL, BASE_CHECK_INTERVAL)
        
        return SecurityMonitoringStats(
            isActive = isMonitoringActive,
            totalLogEntries = logCount,
            lastSecurityCheck = lastCheck,
            currentCheckInterval = checkInterval,
            registeredCallbacks = eventCallbacks.size,
            uptime = if (isMonitoringActive) System.currentTimeMillis() - lastCheck else 0
        )
    }
    
    /**
     * Get recent security logs.
     */
    fun getRecentSecurityLogs(limit: Int = 50): List<SecurityLogEntry> {
        val logsJson = securityLogPrefs.getString(KEY_LOG_ENTRIES, "[]") ?: "[]"
        // In a real implementation, you'd parse JSON and return SecurityLogEntry objects
        // For now, return empty list as this would require JSON parsing library
        return emptyList()
    }
    
    // Private helper methods
    
    private fun startPeriodicSecurityChecks() {
        monitoringJob = monitoringScope.launch {
            performPeriodicSecurityCheck()
        }
    }
    
    private suspend fun performPeriodicSecurityCheck() {
        while (isMonitoringActive) {
            try {
                // Perform the security check
                performSecurityCheck(SecurityCheckType.PERIODIC_BACKGROUND)
                
                // Calculate next check time with variance
                val nextCheckInterval = BASE_CHECK_INTERVAL + Random.nextLong(-INTERVAL_VARIANCE, INTERVAL_VARIANCE)
                
                // Wait for the next check
                delay(nextCheckInterval)

            } catch (e: CancellationException) {
                // This is expected when monitoring is stopped.
                // Log it for debugging but at a lower level, and then exit the loop.
                Log.d(TAG, "Periodic security check coroutine was cancelled.", e)
                break // Exit the loop as the job is cancelled
            } catch (e: Exception) {
                // Log other exceptions as errors
                Log.e(TAG, "Error during periodic security check", e)
                // Wait a bit before retrying to avoid tight loop on error
                delay(BASE_CHECK_INTERVAL)
            }
        }
    }
    
    private fun startContinuousMonitoring() {
        // This would include additional monitoring like:
        // - Memory integrity checks
        // - Anti-debugging measures
        // - Hook detection
        // - Emulator detection
        // For now, we'll implement basic tamper detection
        
        monitoringScope.launch {
            while (isMonitoringActive) {
                try {
                    detectTamperAttempts()
                    delay(5000) // Check every 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error during continuous monitoring", e)
                    delay(10000) // Longer delay on error
                }
            }
        }
    }
    
    private fun schedulePeriodicWorkManagerChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val periodicWork = PeriodicWorkRequestBuilder<UnifiedSecurityWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        )
            .setInputData(UnifiedSecurityWorker.createPeriodicCheckData())
            .setConstraints(constraints)
            .addTag(PERIODIC_SECURITY_WORK)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_SECURITY_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork
            )
    }
    
    private fun cancelPeriodicWorkManagerChecks() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(PERIODIC_SECURITY_WORK)
    }
    
    private fun scheduleCriticalEventSecurityCheck(event: CriticalSecurityEvent) {
        val workData = UnifiedSecurityWorker.createCriticalEventCheckData(
            eventType = event.eventType.name,
            eventContext = event.context,
            eventTimestamp = event.timestamp
        )
        
        val criticalEventWork = OneTimeWorkRequestBuilder<UnifiedSecurityWorker>()
            .setInputData(workData)
            .setInitialDelay(CRITICAL_EVENT_DELAY, TimeUnit.MILLISECONDS)
            .addTag(CRITICAL_EVENT_WORK)
            .build()
        
        WorkManager.getInstance(context)
            .enqueue(criticalEventWork)
    }
    
    private fun detectTamperAttempts() {
        // Implement various tamper detection mechanisms
        
        // Check for debugger attachment
        if (android.os.Debug.isDebuggerConnected()) {
            val tamperAttempt = TamperAttempt(
                timestamp = System.currentTimeMillis(),
                attemptType = TamperType.DEBUGGER_ATTACHMENT,
                severity = TamperSeverity.HIGH,
                details = "Debugger attachment detected during runtime",
                evidence = mapOf("debug_status" to true)
            )
            handleTamperAttempt(tamperAttempt)
        }
        
        // Check for timing manipulation (basic implementation)
        val startTime = System.nanoTime()
        Thread.sleep(1) // Minimal sleep
        val endTime = System.nanoTime()
        val actualDuration = (endTime - startTime) / 1_000_000 // Convert to milliseconds
        
        if (actualDuration > 50) { // If 1ms sleep took more than 50ms, suspicious
            val tamperAttempt = TamperAttempt(
                timestamp = System.currentTimeMillis(),
                attemptType = TamperType.TIMING_MANIPULATION,
                severity = TamperSeverity.MODERATE,
                details = "Unusual timing behavior detected",
                evidence = mapOf("expected_duration" to 1, "actual_duration" to actualDuration)
            )
            handleTamperAttempt(tamperAttempt)
        }
    }
    
    private fun handleTamperAttempt(attempt: TamperAttempt) {
        Log.w(TAG, "Tamper attempt detected: ${attempt.attemptType} - ${attempt.details}")
        
        // Notify callbacks
        eventCallbacks.values.forEach { callback ->
            try {
                callback.onTamperAttemptDetected(attempt)
            } catch (e: Exception) {
                Log.e(TAG, "Error in tamper attempt callback", e)
            }
        }
        
        // Log the tamper attempt
        logSecurityEvent(
            "TAMPER_ATTEMPT",
            "Tamper attempt: ${attempt.attemptType}",
            when (attempt.severity) {
                TamperSeverity.SUSPICIOUS -> SecurityLevel.LOW
                TamperSeverity.MODERATE -> SecurityLevel.MEDIUM
                TamperSeverity.HIGH -> SecurityLevel.HIGH
                TamperSeverity.CRITICAL -> SecurityLevel.CRITICAL
            },
            attempt.evidence
        )
        
        // Take action based on severity
        when (attempt.severity) {
            TamperSeverity.HIGH, TamperSeverity.CRITICAL -> {
                // Trigger immediate security response
                securityResponseManager.respondToThreat(SecurityThreat.TAMPERED_APP)
            }
            else -> {
                // Log and monitor
                Log.w(TAG, "Tamper attempt logged for monitoring: ${attempt.attemptType}")
            }
        }
    }
    
    private fun handleDetectedThreat(threat: SecurityThreat, checkResult: SecurityCheckResult) {
        Log.w(TAG, "Handling detected threat: $threat")
        
        // Trigger security response
        securityResponseManager.respondToThreat(threat)
        
        // Log the threat detection
        logSecurityEvent(
            "THREAT_DETECTED",
            "Security threat detected: $threat",
            SecurityLevel.CRITICAL,
            mapOf(
                "check_type" to checkResult.checkType.name,
                "check_duration" to checkResult.checkDuration
            )
        )
    }
    
    private fun gatherAdditionalSecurityInfo(): Map<String, Any> {
        return mapOf(
            "system_time" to System.currentTimeMillis(),
            "debug_enabled" to android.os.Debug.isDebuggerConnected(),
            "monitoring_active" to isMonitoringActive,
            "callback_count" to eventCallbacks.size
        )
    }
    
    private fun determineSecurityLevel(eventType: CriticalEventType): SecurityLevel {
        return when (eventType) {
            CriticalEventType.PAYMENT_INITIATED,
            CriticalEventType.SENSITIVE_DATA_ACCESS -> SecurityLevel.CRITICAL
            
            CriticalEventType.USER_LOGIN,
            CriticalEventType.PERMISSION_GRANTED,
            CriticalEventType.APP_INSTALL_DETECTED -> SecurityLevel.HIGH
            
            CriticalEventType.SETTINGS_CHANGE,
            CriticalEventType.EXTERNAL_STORAGE_ACCESS,
            CriticalEventType.NETWORK_STATE_CHANGE -> SecurityLevel.MEDIUM
            
            else -> SecurityLevel.LOW
        }
    }
    
    private fun logSecurityEvent(
        eventType: String,
        message: String,
        level: SecurityLevel,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val timestamp = System.currentTimeMillis()
        val logEntry = SecurityLogEntry(
            timestamp = timestamp,
            eventType = eventType,
            message = message,
            level = level,
            metadata = metadata
        )
        
        // Store in encrypted preferences (simplified implementation)
        val currentCount = securityLogPrefs.getInt(KEY_LOG_COUNT, 0)
        securityLogPrefs.edit()
            .putInt(KEY_LOG_COUNT, currentCount + 1)
            .putLong("log_${currentCount}_timestamp", timestamp)
            .putString("log_${currentCount}_type", eventType)
            .putString("log_${currentCount}_message", message)
            .putString("log_${currentCount}_level", level.name)
            .apply()
        
        // Clean up old logs if necessary
        if (currentCount > MAX_LOG_ENTRIES) {
            cleanupOldLogs()
        }
        
        Log.d(TAG, "Security event logged: $eventType - $message")
    }
    
    private fun logSecurityCheck(result: SecurityCheckResult) {
        logSecurityEvent(
            "SECURITY_CHECK",
            "Security check completed: ${result.checkType}",
            if (result.threat != null) SecurityLevel.HIGH else SecurityLevel.LOW,
            mapOf(
                "check_type" to result.checkType.name,
                "threat" to (result.threat?.name ?: "none"),
                "duration" to result.checkDuration
            )
        )
    }
    
    private fun cleanupOldLogs() {
        // Remove oldest logs to maintain MAX_LOG_ENTRIES limit
        // This is a simplified implementation
        val editor = securityLogPrefs.edit()
        val currentCount = securityLogPrefs.getInt(KEY_LOG_COUNT, 0)
        val logsToRemove = currentCount - MAX_LOG_ENTRIES
        
        for (i in 0 until logsToRemove) {
            editor.remove("log_${i}_timestamp")
            editor.remove("log_${i}_type")
            editor.remove("log_${i}_message")
            editor.remove("log_${i}_level")
        }
        
        editor.putInt(KEY_LOG_COUNT, MAX_LOG_ENTRIES)
        editor.apply()
    }
    
    private fun createEncryptedLogPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                SECURITY_LOG_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted log preferences, falling back to regular preferences", e)
            context.getSharedPreferences(SECURITY_LOG_PREFS, Context.MODE_PRIVATE)
        }
    }
}

/**
 * Data class for security log entries.
 */
data class SecurityLogEntry(
    val timestamp: Long,
    val eventType: String,
    val message: String,
    val level: RuntimeSecurityMonitor.SecurityLevel,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Data class for security monitoring statistics.
 */
data class SecurityMonitoringStats(
    val isActive: Boolean,
    val totalLogEntries: Int,
    val lastSecurityCheck: Long,
    val currentCheckInterval: Long,
    val registeredCallbacks: Int,
    val uptime: Long
) 