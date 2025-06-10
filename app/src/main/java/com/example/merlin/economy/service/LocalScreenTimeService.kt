package com.example.merlin.economy.service

import android.content.Context
import com.example.merlin.data.repository.DailyUsageLogRepository
import com.example.merlin.data.database.entities.DailyUsageLog
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.ui.parent.DailyUsage
import com.example.merlin.ui.parent.TimeRestrictions
import com.example.merlin.ui.parent.TimeSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalScreenTimeService(context: Context) : ScreenTimeService {

    private val database = DatabaseProvider.getInstance(context)
    private val dailyUsageRepository = DailyUsageLogRepository(database.dailyUsageLogDao())
    
    // Session tracking (moved from ScreenTimeTracker)
    private var sessionStartTime: Long = 0
    private var currentSessionTime: Int = 0 // seconds
    private var isSessionActive: Boolean = false
    
    // Demo data for features not yet persisted
    private val demoRestrictions = mutableMapOf<String, TimeRestrictions>()
    private val demoSchedules = mutableMapOf<String, MutableMap<DayOfWeek, TimeSlot>>()
    private val demoSubjectDistribution = mutableMapOf<String, Map<String, Float>>()
    
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    // === SESSION MANAGEMENT (from ScreenTimeTracker) ===
    
    /**
     * Start tracking screen time session
     */
    override fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        currentSessionTime = 0
        isSessionActive = true
    }
    
    /**
     * Stop tracking and save to database
     */
    override suspend fun stopSession(childId: String) = withContext(Dispatchers.IO) {
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
    override fun getCurrentSessionTime(): Int {
        updateSessionTime()
        return currentSessionTime
    }
    
    /**
     * Get today's total screen time in seconds
     */
    override suspend fun getTodayTotalTime(childId: String): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val todayLog = dailyUsageRepository.getByChildAndDate(childId, today)
        val savedTime = todayLog?.secondsUsed ?: 0
        
        // Add current session time if active
        val sessionTime = if (isSessionActive) getCurrentSessionTime() else 0
        
        savedTime + sessionTime
    }
    
    /**
     * Format seconds to human readable time
     */
    override fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${remainingSeconds}s"
        }
    }

    // === SERVICE INTERFACE IMPLEMENTATION ===

    override suspend fun getDailyUsage(childId: String, days: Int): Result<List<DailyUsage>> = 
        withContext(Dispatchers.IO) {
            try {
                val allLogs = dailyUsageRepository.getForChild(childId)
                val recentLogs = allLogs.takeLast(days)
                
                // Convert to DailyUsage format (expecting day names)
                val dailyUsage = recentLogs.mapIndexed { index, log ->
                    val dayName = when (index % 7) {
                        0 -> "Mon"
                        1 -> "Tue" 
                        2 -> "Wed"
                        3 -> "Thu"
                        4 -> "Fri"
                        5 -> "Sat"
                        6 -> "Sun"
                        else -> "Day"
                    }
                    DailyUsage(dayName, (log.secondsUsed ?: 0) / 60) // Convert to minutes
                }
                
                // If no real data, provide some demo data
                if (dailyUsage.isEmpty()) {
                    val demoUsage = listOf(
                        DailyUsage("Mon", 65),
                        DailyUsage("Tue", 80),
                        DailyUsage("Wed", 75),
                        DailyUsage("Thu", 90),
                        DailyUsage("Fri", 110),
                        DailyUsage("Sat", 130),
                        DailyUsage("Sun", 120)
                    )
                    Result.success(demoUsage)
                } else {
                    Result.success(dailyUsage)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTimeRestrictions(childId: String): Result<TimeRestrictions> {
        val restrictions = demoRestrictions.getOrPut(childId) {
            TimeRestrictions(
                dailyLimitMinutes = 120,
                blockedPeriods = listOf(TimeSlot(19, 0, 21, 0))
            )
        }
        return Result.success(restrictions)
    }

    override suspend fun setDailyLimit(childId: String, minutes: Int): Result<Unit> {
        val current = demoRestrictions.getOrPut(childId) { TimeRestrictions() }
        demoRestrictions[childId] = current.copy(dailyLimitMinutes = minutes)
        return Result.success(Unit)
    }

    override suspend fun addBlockedPeriod(childId: String, slot: TimeSlot): Result<Unit> {
        val current = demoRestrictions.getOrPut(childId) { TimeRestrictions() }
        val updatedSlots = current.blockedPeriods.toMutableList().apply { add(slot) }
        demoRestrictions[childId] = current.copy(blockedPeriods = updatedSlots)
        return Result.success(Unit)
    }

    override suspend fun removeBlockedPeriod(childId: String, slot: TimeSlot): Result<Unit> {
        val current = demoRestrictions.getOrPut(childId) { TimeRestrictions() }
        val updatedSlots = current.blockedPeriods.toMutableList().apply { remove(slot) }
        demoRestrictions[childId] = current.copy(blockedPeriods = updatedSlots)
        return Result.success(Unit)
    }

    override suspend fun getSubjectDistribution(childId: String, days: Int): Result<Map<String, Float>> {
        // For now, return demo data - future implementation could track subject-specific usage
        val distribution = demoSubjectDistribution.getOrPut(childId) {
            mapOf(
                "Math" to 0.4f,
                "Reading" to 0.3f,
                "Science" to 0.2f,
                "Creative" to 0.1f
            )
        }
        return Result.success(distribution)
    }

    override suspend fun getWeeklySchedule(childId: String): Result<Map<DayOfWeek, TimeSlot>> {
        val schedule = demoSchedules.getOrPut(childId) {
            mutableMapOf(
                DayOfWeek.MONDAY to TimeSlot(16, 0, 18, 0),
                DayOfWeek.TUESDAY to TimeSlot(16, 0, 18, 0),
                DayOfWeek.WEDNESDAY to TimeSlot(16, 0, 18, 0),
                DayOfWeek.THURSDAY to TimeSlot(16, 0, 18, 0),
                DayOfWeek.FRIDAY to TimeSlot(16, 0, 19, 0)
            )
        }
        return Result.success(schedule)
    }

    override suspend fun updateScheduleForDay(childId: String, day: DayOfWeek, slot: TimeSlot): Result<Unit> {
        val schedule = demoSchedules.getOrPut(childId) { mutableMapOf() }
        schedule[day] = slot
        return Result.success(Unit)
    }
} 