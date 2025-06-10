package com.example.merlin.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlin.economy.service.ScreenTimeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class ScreenTimeViewModel(
    private val screenTimeService: ScreenTimeService
) : ViewModel() {

    private val _state = MutableStateFlow(ScreenTimeState())
    val state: StateFlow<ScreenTimeState> = _state.asStateFlow()

    fun loadScreenTimeData(childId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val usage = screenTimeService.getDailyUsage(childId).getOrThrow()
                val restrictions = screenTimeService.getTimeRestrictions(childId).getOrThrow()
                val distribution = screenTimeService.getSubjectDistribution(childId).getOrThrow()
                val schedule = screenTimeService.getWeeklySchedule(childId).getOrThrow()

                _state.value = ScreenTimeState(
                    dailyUsage = usage,
                    timeRestrictions = restrictions,
                    subjectDistribution = distribution,
                    weeklySchedule = schedule
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateDailyLimit(childId: String, limit: Int) {
        viewModelScope.launch {
            screenTimeService.setDailyLimit(childId, limit)
            loadScreenTimeData(childId) // Refresh state
        }
    }

    fun addBlockedPeriod(childId: String, slot: TimeSlot) {
        viewModelScope.launch {
            screenTimeService.addBlockedPeriod(childId, slot)
            loadScreenTimeData(childId) // Refresh state
        }
    }

    fun removeBlockedPeriod(childId: String, slot: TimeSlot) {
        viewModelScope.launch {
            screenTimeService.removeBlockedPeriod(childId, slot)
            loadScreenTimeData(childId) // Refresh state
        }
    }

    fun updateWeeklySchedule(childId: String, day: DayOfWeek, slot: TimeSlot) {
        viewModelScope.launch {
            screenTimeService.updateScheduleForDay(childId, day, slot)
            loadScreenTimeData(childId) // Refresh state
        }
    }
} 