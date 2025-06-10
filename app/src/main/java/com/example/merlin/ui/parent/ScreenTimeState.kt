package com.example.merlin.ui.parent

import com.example.merlin.economy.model.BalanceDto
import java.time.DayOfWeek

data class ScreenTimeState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val dailyUsage: List<DailyUsage> = emptyList(),
    val timeRestrictions: TimeRestrictions = TimeRestrictions(),
    val subjectDistribution: Map<String, Float> = emptyMap(),
    val weeklySchedule: Map<DayOfWeek, TimeSlot> = emptyMap(),
    val balance: BalanceDto? = null // For displaying available screen time currency
)

data class DailyUsage(
    val day: String, // e.g., "Mon", "Tue"
    val usageMinutes: Int
)

data class TimeRestrictions(
    val dailyLimitMinutes: Int = 120, // Overall daily limit
    val blockedPeriods: List<TimeSlot> = emptyList()
)

data class TimeSlot(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    override fun toString(): String {
        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }
} 