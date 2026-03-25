package com.gymapp.ui.viewmodel

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.gymapp.service.TimerNotificationService
import com.gymapp.utils.LogTags

object TimerViewModel {

    private val _time = MutableStateFlow(0L)
    val time: StateFlow<Long> = _time.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var notificationService: TimerNotificationService? = null

    fun initialize(context: Context) {
        if (notificationService == null) {
            notificationService = TimerNotificationService(context.applicationContext)
        }
    }

    fun startTimer() {
        if (_isRunning.value) return
        _isRunning.value = true
        _isPaused.value = false

        timerJob = scope.launch {
            while (_isRunning.value) {
                delay(100)
                _time.value += 100
                // Atualiza notificação a cada segundo (evita spam)
                if (_time.value % 1000L == 0L) updateNotification()
            }
        }
    }

    fun pauseTimer() {
        if (!_isRunning.value) return
        _isRunning.value = false
        _isPaused.value = true
        timerJob?.cancel()
        updateNotification()
    }

    fun resumeTimer() {
        if (!_isPaused.value) return
        _isRunning.value = true
        _isPaused.value = false

        timerJob = scope.launch {
            while (_isRunning.value) {
                delay(100)
                _time.value += 100
                if (_time.value % 1000L == 0L) updateNotification()
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _time.value = 0L
        _isRunning.value = false
        _isPaused.value = false
        // Esconde a notificação ao parar
        notificationService?.hideTimerNotification()
    }

    fun toggleTimer() {
        when {
            _isRunning.value -> pauseTimer()
            _isPaused.value  -> resumeTimer()
            else             -> startTimer()
        }
    }

    private fun updateNotification() {
        val service = notificationService ?: return
        // Só exibe notificação se o timer foi iniciado
        if (_time.value == 0L && !_isRunning.value && !_isPaused.value) return

        val minutes = (_time.value / 60000) % 60
        val seconds = (_time.value / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        service.showTimerNotification(
            timeText  = timeText,
            isRunning = _isRunning.value,
            isPaused  = _isPaused.value
        )
    }

    fun cleanup() {
        timerJob?.cancel()
        notificationService?.hideTimerNotification()
        scope.cancel()
    }
}