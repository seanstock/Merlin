package com.example.merlin.economy.service

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Manages active app sessions for purchased apps
 * Scalable to handle any number of purchased apps
 */
class AppSessionManager {
    
    data class ActiveAppSession(
        val packageName: String,
        val displayName: String,
        val sessionId: String,
        val purchasedAt: Instant,
        val expiresAt: Instant,
        val durationMinutes: Int,
        val appIcon: Drawable? = null,
        val remainingMinutes: Int = 0
    ) {
        val isExpired: Boolean
            get() = Instant.now().isAfter(expiresAt)
            
        val remainingTime: Long
            get() = if (isExpired) 0 else ChronoUnit.MINUTES.between(Instant.now(), expiresAt)
    }
    
    private val _activeSessions = MutableStateFlow<Map<String, ActiveAppSession>>(emptyMap())
    val activeSessions: StateFlow<Map<String, ActiveAppSession>> = _activeSessions.asStateFlow()
    
    /**
     * Add a new app session after purchase
     */
    fun addSession(purchase: AppAccessPurchaseDto, appDisplayName: String, appIcon: Drawable? = null) {
        val currentSessions = _activeSessions.value.toMutableMap()
        
        val session = ActiveAppSession(
            packageName = purchase.appPackage,
            displayName = appDisplayName,
            sessionId = purchase.sessionId,
            purchasedAt = Instant.now(),
            expiresAt = if (purchase.expiresAt.isNotEmpty()) {
                Instant.parse(purchase.expiresAt)
            } else {
                Instant.now().plus(purchase.durationMinutes.toLong(), ChronoUnit.MINUTES)
            },
            durationMinutes = purchase.durationMinutes,
            appIcon = appIcon
        )
        
        currentSessions[purchase.appPackage] = session
        _activeSessions.value = currentSessions
    }
    
    /**
     * Remove expired sessions
     */
    fun cleanupExpiredSessions() {
        val currentSessions = _activeSessions.value
        val activeSessions = currentSessions.filterValues { !it.isExpired }
        
        if (activeSessions.size != currentSessions.size) {
            _activeSessions.value = activeSessions
        }
    }
    
    /**
     * Check if app has active session
     */
    fun hasActiveSession(packageName: String): Boolean {
        cleanupExpiredSessions()
        return _activeSessions.value.containsKey(packageName)
    }
    
    /**
     * Get active session for app
     */
    fun getActiveSession(packageName: String): ActiveAppSession? {
        cleanupExpiredSessions()
        return _activeSessions.value[packageName]
    }
    
    /**
     * Get all active app sessions for menu display
     */
    fun getActiveAppsForMenu(): List<ActiveAppSession> {
        cleanupExpiredSessions()
        return _activeSessions.value.values.toList()
    }
    
    /**
     * Remove specific session (manual cleanup)
     */
    fun removeSession(packageName: String) {
        val currentSessions = _activeSessions.value.toMutableMap()
        currentSessions.remove(packageName)
        _activeSessions.value = currentSessions
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AppSessionManager? = null
        
        fun getInstance(): AppSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSessionManager().also { INSTANCE = it }
            }
        }
    }
} 