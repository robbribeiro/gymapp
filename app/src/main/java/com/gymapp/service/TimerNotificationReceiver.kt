package com.gymapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gymapp.ui.viewmodel.TimerViewModel

class TimerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerNotificationService.ACTION_PAUSE  -> TimerViewModel.pauseTimer()
            TimerNotificationService.ACTION_RESUME -> TimerViewModel.resumeTimer()
            TimerNotificationService.ACTION_STOP   -> TimerViewModel.resetTimer()
        }
    }
}