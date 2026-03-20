package com.gymapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gymapp.MainActivity
import com.gymapp.R
import android.graphics.Color
import android.media.RingtoneManager
import com.gymapp.utils.LogTags

class TimerNotificationService(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PAUSE = "com.gymapp.timer.PAUSE"
        const val ACTION_RESUME = "com.gymapp.timer.RESUME"
        const val ACTION_RESET = "com.gymapp.timer.RESET"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cronômetro",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificação do cronômetro"
                setShowBadge(false)
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showTimerNotification(timeText: String, isRunning: Boolean, isPaused: Boolean = false) {
        // Criar um intent vazio para evitar navegação indesejada
        val intent = Intent().apply {
            // Intent vazio para não abrir o app
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Cronômetro")
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setOnlyAlertOnce(false) // Força atualização da notificação
        
        // Determinar quais botões mostrar baseado no estado
        when {
            isRunning -> {
                // Quando rodando: mostrar botão de pausar
                android.util.Log.d(LogTags.TIMER_NOTIFICATION, "Adicionando botão PAUSAR")
                builder.addAction(
                    R.drawable.ic_launcher_foreground,
                    "Pausar",
                    createActionPendingIntent(ACTION_PAUSE)
                )
            }
            isPaused -> {
                // Quando pausado: mostrar botões de continuar e resetar
                android.util.Log.d(LogTags.TIMER_NOTIFICATION, "Adicionando botões CONTINUAR e RESETAR")
                builder.addAction(
                    R.drawable.ic_launcher_foreground,
                    "Continuar",
                    createActionPendingIntent(ACTION_RESUME)
                )
                builder.addAction(
                    R.drawable.ic_launcher_foreground,
                    "Resetar",
                    createActionPendingIntent(ACTION_RESET)
                )
            }
            else -> {
                // Quando parado: não mostrar botões
                android.util.Log.d(LogTags.TIMER_NOTIFICATION, "Timer parado - sem botões")
            }
        }
        
        val notification = builder.build()
        
        try {
            // Cancelar notificação existente primeiro para forçar atualização
            notificationManager.cancel(NOTIFICATION_ID)
            
            // Pequeno delay para garantir que a notificação anterior seja cancelada
            Thread.sleep(10)
            
            // Usar sempre o mesmo ID para atualizar a notificação
            notificationManager.notify(NOTIFICATION_ID, notification)
            android.util.Log.d(LogTags.TIMER_NOTIFICATION, "Notificação exibida: $timeText, isRunning: $isRunning, isPaused: $isPaused")
        } catch (e: Exception) {
            android.util.Log.e(LogTags.TIMER_NOTIFICATION, "Erro ao exibir notificação: ${e.message}")
        }
    }
    
    fun hideTimerNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        android.util.Log.d(LogTags.TIMER_NOTIFICATION, "Notificação do timer cancelada")
    }
    
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ).also {
            android.util.Log.d(LogTags.TIMER_NOTIFICATION, "Criado PendingIntent para ação: $action")
        }
    }
}
