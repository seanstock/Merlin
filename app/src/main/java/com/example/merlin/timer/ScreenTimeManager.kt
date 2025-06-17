package com.example.merlin.timer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenTimeManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _timeExpiredEvent = MutableStateFlow<Unit?>(null)
    val timeExpiredEvent: StateFlow<Unit?> = _timeExpiredEvent.asStateFlow()

    fun addTime(seconds: Int) {
        val currentSeconds = _remainingSeconds.value
        val newTotal = currentSeconds + seconds
        _remainingSeconds.value = newTotal

        if (timerJob == null || timerJob?.isActive == false) {
            startTimer(newTotal)
        }
    }

    private fun startTimer(seconds: Int) {
        _remainingSeconds.value = seconds
        timerJob = scope.launch {
            while (_remainingSeconds.value > 0 && isActive) {
                delay(1000)
                _remainingSeconds.value--
            }

            if (_remainingSeconds.value == 0) {
                _timeExpiredEvent.value = Unit
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun consumeTimeExpiredEvent() {
        _timeExpiredEvent.value = null
    }
} 