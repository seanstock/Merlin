package com.example.merlin.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages security responses when threats are detected.
 * Handles PIN re-locking, secure lockout screens, and persistent state tracking.
 */
class SecurityResponseManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityResponseManager"
        private const val SECURITY_PREFS_NAME = "merlin_security_prefs"
        private const val KEY_LOCKOUT_STATE = "lockout_state"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"
        private const val KEY_LOCKOUT_REASON = "lockout_reason"
        private const val KEY_LOCKOUT_COUNT = "lockout_count"
        private const val KEY_LAST_SECURITY_CHECK = "last_security_check"
        
        // Lockout durations (in milliseconds)
        private const val LOCKOUT_DURATION_FIRST = 5 * 60 * 1000L // 5 minutes
        private const val LOCKOUT_DURATION_SECOND = 15 * 60 * 1000L // 15 minutes
        private const val LOCKOUT_DURATION_PERSISTENT = 60 * 60 * 1000L // 1 hour
        
        // Security check intervals
        private const val SECURITY_CHECK_INTERVAL = 30 * 1000L // 30 seconds
    }
    
    private val securityPrefs: SharedPreferences by lazy {
        createEncryptedPreferences()
    }
    
    private val lockoutCallbacks = ConcurrentHashMap<String, SecurityLockoutCallback>()
    private val handler = Handler(Looper.getMainLooper())
    private var securityCheckRunnable: Runnable? = null
    
    /**
     * Interface for security lockout callbacks.
     */
    interface SecurityLockoutCallback {
        fun onSecurityLockoutTriggered(threat: SecurityThreat, lockoutInfo: LockoutInfo)
        fun onSecurityLockoutCleared()
    }
    
    /**
     * Data class containing lockout information.
     */
    data class LockoutInfo(
        val threat: SecurityThreat,
        val timestamp: Long,
        val duration: Long,
        val count: Int,
        val reason: String,
        val isActive: Boolean
    )
    
    /**
     * Respond to a detected security threat.
     */
    fun respondToThreat(threat: SecurityThreat): LockoutInfo {
        Log.w(TAG, "Responding to security threat: $threat")
        
        val lockoutInfo = createLockoutInfo(threat)
        
        // Store lockout state persistently
        storeLockoutState(lockoutInfo)
        
        // Trigger immediate security response
        triggerSecurityLockout(lockoutInfo)
        
        // Schedule lockout clearance
        scheduleLockoutClearance(lockoutInfo)
        
        Log.w(TAG, "Security lockout activated: ${lockoutInfo.reason} for ${lockoutInfo.duration / 1000}s")
        
        return lockoutInfo
    }
    
    /**
     * Check if the device is currently in security lockout.
     */
    fun isInSecurityLockout(): Boolean {
        val lockoutState = securityPrefs.getBoolean(KEY_LOCKOUT_STATE, false)
        val lockoutTimestamp = securityPrefs.getLong(KEY_LOCKOUT_TIMESTAMP, 0)
        val lockoutDuration = getLockoutDuration()
        
        if (!lockoutState) return false
        
        val currentTime = System.currentTimeMillis()
        val lockoutEndTime = lockoutTimestamp + lockoutDuration
        
        if (currentTime >= lockoutEndTime) {
            // Lockout has expired, clear it
            clearLockoutState()
            return false
        }
        
        return true
    }
    
    /**
     * Get current lockout information if active.
     */
    fun getCurrentLockoutInfo(): LockoutInfo? {
        if (!isInSecurityLockout()) return null
        
        val threat = SecurityThreat.valueOf(
            securityPrefs.getString(KEY_LOCKOUT_REASON, SecurityThreat.ROOTED_DEVICE.name) 
                ?: SecurityThreat.ROOTED_DEVICE.name
        )
        val timestamp = securityPrefs.getLong(KEY_LOCKOUT_TIMESTAMP, 0)
        val count = securityPrefs.getInt(KEY_LOCKOUT_COUNT, 0)
        val duration = getLockoutDuration()
        
        return LockoutInfo(
            threat = threat,
            timestamp = timestamp,
            duration = duration,
            count = count,
            reason = generateLockoutReason(threat),
            isActive = true
        )
    }
    
    /**
     * Force clear security lockout (for testing or admin override).
     */
    fun clearSecurityLockout() {
        Log.i(TAG, "Manually clearing security lockout")
        clearLockoutState()
        notifyLockoutCleared()
    }
    
    /**
     * Register a callback for security lockout events.
     */
    fun registerLockoutCallback(id: String, callback: SecurityLockoutCallback) {
        lockoutCallbacks[id] = callback
        Log.d(TAG, "Registered lockout callback: $id")
    }
    
    /**
     * Unregister a security lockout callback.
     */
    fun unregisterLockoutCallback(id: String) {
        lockoutCallbacks.remove(id)
        Log.d(TAG, "Unregistered lockout callback: $id")
    }
    
    /**
     * Start periodic security monitoring.
     */
    fun startSecurityMonitoring(securityManager: SecurityManager) {
        Log.d(TAG, "Starting periodic security monitoring")
        
        securityCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    val threat = securityManager.enforceSecurityMeasures()
                    if (threat != null && !isInSecurityLockout()) {
                        Log.w(TAG, "Security threat detected during monitoring: $threat")
                        respondToThreat(threat)
                    }
                    
                    // Update last check timestamp
                    securityPrefs.edit()
                        .putLong(KEY_LAST_SECURITY_CHECK, System.currentTimeMillis())
                        .apply()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during security monitoring", e)
                }
                
                // Schedule next check
                handler.postDelayed(this, SECURITY_CHECK_INTERVAL)
            }
        }
        
        handler.post(securityCheckRunnable!!)
    }
    
    /**
     * Stop periodic security monitoring.
     */
    fun stopSecurityMonitoring() {
        Log.d(TAG, "Stopping periodic security monitoring")
        securityCheckRunnable?.let { handler.removeCallbacks(it) }
        securityCheckRunnable = null
    }
    
    /**
     * Launch secure lockout activity.
     */
    fun launchSecureLockoutActivity(lockoutInfo: LockoutInfo) {
        try {
            val intent = Intent(context, SecurityLockoutActivity::class.java).apply {
                putExtra("threat_type", lockoutInfo.threat.name)
                putExtra("lockout_reason", lockoutInfo.reason)
                putExtra("lockout_duration", lockoutInfo.duration)
                putExtra("lockout_count", lockoutInfo.count)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            context.startActivity(intent)
            Log.i(TAG, "Launched security lockout activity")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching security lockout activity", e)
        }
    }
    
    /**
     * Get security monitoring statistics.
     */
    fun getSecurityStats(): SecurityStats {
        val lastCheck = securityPrefs.getLong(KEY_LAST_SECURITY_CHECK, 0)
        val lockoutCount = securityPrefs.getInt(KEY_LOCKOUT_COUNT, 0)
        val isMonitoring = securityCheckRunnable != null
        val currentLockout = getCurrentLockoutInfo()
        
        return SecurityStats(
            lastSecurityCheck = lastCheck,
            totalLockouts = lockoutCount,
            isMonitoringActive = isMonitoring,
            currentLockout = currentLockout,
            securityCheckInterval = SECURITY_CHECK_INTERVAL
        )
    }
    
    // Private helper methods
    
    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                SECURITY_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted preferences, falling back to regular preferences", e)
            context.getSharedPreferences(SECURITY_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    private fun createLockoutInfo(threat: SecurityThreat): LockoutInfo {
        val currentCount = securityPrefs.getInt(KEY_LOCKOUT_COUNT, 0) + 1
        val duration = calculateLockoutDuration(currentCount)
        val timestamp = System.currentTimeMillis()
        val reason = generateLockoutReason(threat)
        
        return LockoutInfo(
            threat = threat,
            timestamp = timestamp,
            duration = duration,
            count = currentCount,
            reason = reason,
            isActive = true
        )
    }
    
    private fun calculateLockoutDuration(count: Int): Long {
        return when (count) {
            1 -> LOCKOUT_DURATION_FIRST
            2 -> LOCKOUT_DURATION_SECOND
            else -> LOCKOUT_DURATION_PERSISTENT
        }
    }
    
    private fun getLockoutDuration(): Long {
        val count = securityPrefs.getInt(KEY_LOCKOUT_COUNT, 0)
        return calculateLockoutDuration(count)
    }
    
    private fun generateLockoutReason(threat: SecurityThreat): String {
        return when (threat) {
            SecurityThreat.ROOTED_DEVICE -> "Device root access detected. This poses a security risk to the application."
            SecurityThreat.SAFE_MODE -> "Device is in Safe Mode. Normal security features may be disabled."
            SecurityThreat.ADB_ENABLED -> "USB debugging (ADB) is enabled. This allows unauthorized access to the device."
            SecurityThreat.DEVELOPER_OPTIONS -> "Developer options are enabled. This may compromise device security."
            SecurityThreat.TAMPERED_APP -> "Application tampering detected. The app integrity has been compromised."
            SecurityThreat.DEBUGGER_ATTACHED -> "Debugger attachment detected. This may indicate malicious activity."
        }
    }
    
    private fun storeLockoutState(lockoutInfo: LockoutInfo) {
        securityPrefs.edit()
            .putBoolean(KEY_LOCKOUT_STATE, true)
            .putLong(KEY_LOCKOUT_TIMESTAMP, lockoutInfo.timestamp)
            .putString(KEY_LOCKOUT_REASON, lockoutInfo.threat.name)
            .putInt(KEY_LOCKOUT_COUNT, lockoutInfo.count)
            .apply()
    }
    
    private fun clearLockoutState() {
        securityPrefs.edit()
            .putBoolean(KEY_LOCKOUT_STATE, false)
            .remove(KEY_LOCKOUT_TIMESTAMP)
            .remove(KEY_LOCKOUT_REASON)
            .apply()
    }
    
    private fun triggerSecurityLockout(lockoutInfo: LockoutInfo) {
        // Notify all registered callbacks
        lockoutCallbacks.values.forEach { callback ->
            try {
                callback.onSecurityLockoutTriggered(lockoutInfo.threat, lockoutInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Error in lockout callback", e)
            }
        }
        
        // Launch lockout activity
        launchSecureLockoutActivity(lockoutInfo)
    }
    
    private fun scheduleLockoutClearance(lockoutInfo: LockoutInfo) {
        handler.postDelayed({
            if (isInSecurityLockout()) {
                Log.i(TAG, "Security lockout duration expired, clearing lockout")
                clearLockoutState()
                notifyLockoutCleared()
            }
        }, lockoutInfo.duration)
    }
    
    private fun notifyLockoutCleared() {
        lockoutCallbacks.values.forEach { callback ->
            try {
                callback.onSecurityLockoutCleared()
            } catch (e: Exception) {
                Log.e(TAG, "Error in lockout cleared callback", e)
            }
        }
    }
}

/**
 * Data class containing security monitoring statistics.
 */
data class SecurityStats(
    val lastSecurityCheck: Long,
    val totalLockouts: Int,
    val isMonitoringActive: Boolean,
    val currentLockout: SecurityResponseManager.LockoutInfo?,
    val securityCheckInterval: Long
) 