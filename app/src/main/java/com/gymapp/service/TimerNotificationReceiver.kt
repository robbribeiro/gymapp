package com.gymapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gymapp.ui.viewmodel.TimerViewModel
import com.gymapp.utils.LogTags

class TimerNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(LogTags.TIMER_RECEIVER, "=== BROADCAST RECEIVED ===")
        android.util.Log.d(LogTags.TIMER_RECEIVER, "Ação recebida: ${intent.action}")
        android.util.Log.d(LogTags.TIMER_RECEIVER, "Timestamp: ${System.currentTimeMillis()}")
        
        when (intent.action) {
            TimerNotificationService.ACTION_PAUSE -> {
                android.util.Log.d(LogTags.TIMER_RECEIVER, "Executando pausar timer")
                TimerViewModel.pauseTimer()
                android.util.Log.d(LogTags.TIMER_RECEIVER, "Pausar timer executado")
            }
            TimerNotificationService.ACTION_RESUME -> {
                android.util.Log.d(LogTags.TIMER_RECEIVER, "Executando continuar timer")
                TimerViewModel.resumeTimer()
                android.util.Log.d(LogTags.TIMER_RECEIVER, "Continuar timer executado")
            }
            TimerNotificationService.ACTION_RESET -> {
                android.util.Log.d(LogTags.TIMER_RECEIVER, "Executando resetar timer")
                TimerViewModel.resetTimer()
                android.util.Log.d(LogTags.TIMER_RECEIVER, "Resetar timer executado")
            }
            else -> {
                android.util.Log.w(LogTags.TIMER_RECEIVER, "Ação desconhecida: ${intent.action}")
            }
        }
        android.util.Log.d(LogTags.TIMER_RECEIVER, "=== FIM BROADCAST ===")
    }
}
