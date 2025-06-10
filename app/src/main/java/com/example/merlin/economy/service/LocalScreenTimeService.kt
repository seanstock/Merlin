package com.example.merlin.economy.service

import com.example.merlin.ui.parent.DailyUsage
import com.example.merlin.ui.parent.TimeRestrictions
import com.example.merlin.ui.parent.TimeSlot
import java.time.DayOfWeek
import kotlin.random.Random

class LocalScreenTimeService : ScreenTimeService {

    private val demoRestrictions = mutableMapOf<String, TimeRestrictions>()
    private val demoSchedules = mutableMapOf<String, MutableMap<DayOfWeek, TimeSlot>>()

    override suspend fun getDailyUsage(childId: String, days: Int): Result<List<DailyUsage>> {
        val usage = listOf(
            DailyUsage("Mon", 65),
            DailyUsage("Tue", 80),
            DailyUsage("Wed", 75),
            DailyUsage("Thu", 90),
            DailyUsage("Fri", 110),
            DailyUsage("Sat", 130),
            DailyUsage("Sun", 120)
        )
        return Result.success(usage)
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
        val distribution = mapOf(
            "Math" to 0.4f,
            "Reading" to 0.3f,
            "Science" to 0.2f,
            "Creative" to 0.1f
        )
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