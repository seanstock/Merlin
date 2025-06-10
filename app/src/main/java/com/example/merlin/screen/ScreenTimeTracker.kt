package com.example.merlin.screen

import android.content.Context
import com.example.merlin.data.repository.DailyUsageLogRepository
import com.example.merlin.data.database.entities.DailyUsageLog
import com.example.merlin.data.database.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Simple screen time tracking service using existing DailyUsageLog entity.
 * Tracks daily usage and provides current session time queries.
 */
class ScreenTimeTracker(context: Context) {
    
    private val database = DatabaseProvider.getInstance(context)
    private val dailyUsageRepository = DailyUsageLogRepository(database.dailyUsageLogDao())
    
    // Session tracking
    private var sessionStartTime: Long = 0
    private var currentSessionTime: Int = 0 // seconds
    private var isSessionActive: Boolean = false
    
    companion object {
        private const val TAG = "ScreenTimeTracker"
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
    
    /**
     * Start tracking screen time session
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        currentSessionTime = 0
        isSessionActive = true
    }
    
    /**
     * Stop tracking and save to database
     */
    suspend fun stopSession(childId: String) = withContext(Dispatchers.IO) {
        if (!isSessionActive) return@withContext
        
        val sessionDuration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        currentSessionTime += sessionDuration
        
        // Save to daily usage log
        val today = LocalDate.now().format(dateFormatter)
        val existingLog = dailyUsageRepository.getByChildAndDate(childId, today)
        
        if (existingLog != null) {
            val updatedLog = existingLog.copy(
                secondsUsed = (existingLog.secondsUsed ?: 0) + sessionDuration
            )
            dailyUsageRepository.update(updatedLog)
        } else {
            val newLog = DailyUsageLog(
                childId = childId,
                date = today,
                secondsUsed = sessionDuration
            )
            dailyUsageRepository.insert(newLog)
        }
        
        isSessionActive = false
    }
    
    /**
     * Update current session time (call periodically)
     */
    fun updateSessionTime() {
        if (isSessionActive) {
            currentSessionTime = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        }
    }
    
    /**
     * Get current session screen time in seconds
     */
    fun getCurrentSessionTime(): Int {
        updateSessionTime()
        return currentSessionTime
    }
    
    /**
     * Get today's total screen time in seconds
     */
    suspend fun getTodayTotalTime(childId: String): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val todayLog = dailyUsageRepository.getByChildAndDate(childId, today)
        val savedTime = todayLog?.secondsUsed ?: 0
        
        // Add current session time if active
        val sessionTime = if (isSessionActive) getCurrentSessionTime() else 0
        
        savedTime + sessionTime
    }
    
    /**
     * Get screen time for the last N days
     */
    suspend fun getScreenTimeHistory(childId: String, days: Int = 7): List<Pair<String, Int>> = 
        withContext(Dispatchers.IO) {
            val allLogs = dailyUsageRepository.getForChild(childId)
            
            allLogs.takeLast(days).map { log ->
                log.date to (log.secondsUsed ?: 0)
            }
        }
    
    /**
     * Format seconds to human readable time
     */
    fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${remainingSeconds}s"
        }
    }
} 