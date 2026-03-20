package com.gymapp.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.*
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
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context
        this.notificationService = TimerNotificationService(context)
        android.util.Log.d(LogTags.TIMER_VIEWMODEL, "TimerViewModel inicializado com contexto")
    }
    
    fun startTimer() {
        android.util.Log.d(LogTags.TIMER_VIEWMODEL, "startTimer() chamado - isRunning: ${_isRunning.value}")
        if (!_isRunning.value) {
            _isRunning.value = true
            _isPaused.value = false
            
            android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Iniciando timer - notificationService: ${notificationService != null}")
            
            timerJob = scope.launch {
                while (_isRunning.value) {
                    delay(100)
                    _time.value += 100
                    updateNotification()
                }
            }
        }
    }
    
    fun pauseTimer() {
        android.util.Log.d(LogTags.TIMER_VIEWMODEL, "pauseTimer() chamado - isRunning: ${_isRunning.value}, isPaused: ${_isPaused.value}")
        
        // Se está rodando, pausar
        if (_isRunning.value) {
            _isRunning.value = false
            _isPaused.value = true
            timerJob?.cancel()
            android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Timer pausado - isRunning: ${_isRunning.value}, isPaused: ${_isPaused.value}")
            
            // Forçar atualização imediata da notificação
            scope.launch {
                updateNotification()
                // Pequeno delay e atualização adicional para garantir que a notificação seja atualizada
                kotlinx.coroutines.delay(100)
                updateNotification()
            }
        } else if (_isPaused.value) {
            // Se já está pausado, apenas atualizar notificação (pode ter sido chamado por engano)
            android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Timer já está pausado, apenas atualizando notificação")
            updateNotification()
        } else {
            // Se não está rodando nem pausado, não fazer nada
            android.util.Log.w(LogTags.TIMER_VIEWMODEL, "Tentativa de pausar timer que não está rodando")
        }
    }
    
    fun resumeTimer() {
        if (_isPaused.value) {
            _isRunning.value = true
            _isPaused.value = false
            
            timerJob = scope.launch {
                while (_isRunning.value) {
                    delay(100)
                    _time.value += 100
                    updateNotification()
                }
            }
        }
    }
    
    fun resetTimer() {
        _time.value = 0L
        _isRunning.value = false
        _isPaused.value = false
        timerJob?.cancel()
        hideNotification()
    }
    
    fun toggleTimer() {
        android.util.Log.d(LogTags.TIMER_VIEWMODEL, "toggleTimer() chamado - isRunning: ${_isRunning.value}, isPaused: ${_isPaused.value}")
        when {
            _isRunning.value -> {
                android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Timer rodando, pausando...")
                pauseTimer()
            }
            _isPaused.value -> {
                android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Timer pausado, retomando...")
                resumeTimer()
            }
            else -> {
                android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Timer parado, iniciando...")
                startTimer()
            }
        }
    }
    
    private fun updateNotification() {
        if (notificationService != null) {
            val minutes = (_time.value / 60000) % 60
            val seconds = (_time.value / 1000) % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)
            
            android.util.Log.d(LogTags.TIMER_VIEWMODEL, "Atualizando notificação: $timeText, isRunning: ${_isRunning.value}, isPaused: ${_isPaused.value}")
            
            // Sempre mostrar notificação se o timer foi iniciado (tempo > 0 ou está rodando/pausado)
            if (_time.value > 0 || _isRunning.value || _isPaused.value) {
                notificationService?.showTimerNotification(timeText, _isRunning.value, _isPaused.value)
            } else {
                // Se tempo é 0 e não está rodando nem pausado, esconder notificação
                hideNotification()
            }
        }
    }
    
    private fun hideNotification() {
        notificationService?.hideTimerNotification()
    }
    
    fun cleanup() {
        timerJob?.cancel()
        hideNotification()
        scope.cancel()
    }
}
