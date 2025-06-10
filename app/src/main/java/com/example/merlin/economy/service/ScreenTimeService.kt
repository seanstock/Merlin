package com.example.merlin.economy.service

import com.example.merlin.ui.parent.DailyUsage
import com.example.merlin.ui.parent.TimeRestrictions
import com.example.merlin.ui.parent.TimeSlot
import java.time.DayOfWeek

interface ScreenTimeService {

    /**
     * Get the daily screen time usage for the past week.
     */
    suspend fun getDailyUsage(childId: String, days: Int = 7): Result<List<DailyUsage>>

    /**
     * Get the current time restrictions for a child.
     */
    suspend fun getTimeRestrictions(childId: String): Result<TimeRestrictions>

    /**
     * Set the daily screen time limit.
     */
    suspend fun setDailyLimit(childId: String, minutes: Int): Result<Unit>

    /**
     * Add a blocked time period for a child.
     */
    suspend fun addBlockedPeriod(childId: String, slot: TimeSlot): Result<Unit>

    /**
     * Remove a blocked time period.
     */
    suspend fun removeBlockedPeriod(childId: String, slot: TimeSlot): Result<Unit>

    /**
     * Get the subject distribution of screen time.
     */
    suspend fun getSubjectDistribution(childId: String, days: Int = 7): Result<Map<String, Float>>

    /**
     * Get the weekly screen time schedule.
     */
    suspend fun getWeeklySchedule(childId: String): Result<Map<DayOfWeek, TimeSlot>>

    /**
     * Update the screen time schedule for a specific day.
     */
    suspend fun updateScheduleForDay(childId: String, day: DayOfWeek, slot: TimeSlot): Result<Unit>
} 