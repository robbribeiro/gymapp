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

class TimerNotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE  = "com.gymapp.timer.PAUSE"
        const val ACTION_RESUME = "com.gymapp.timer.RESUME"
        const val ACTION_STOP   = "com.gymapp.timer.STOP"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cronômetro",
                NotificationManager.IMPORTANCE_LOW   // LOW = sem som, sem vibração
            ).apply {
                description = "Notificação do cronômetro"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Exibe a notificação no estilo media player.
     * Quando rodando  → botões: [X parar] [⏸ pausar]
     * Quando pausado  → botões: [X parar] [▶ continuar]
     */
    fun showTimerNotification(timeText: String, isRunning: Boolean, isPaused: Boolean) {
        // Abre o app ao tocar na notificação
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação X — parar o cronômetro
        val stopAction = NotificationCompat.Action(
            R.drawable.ic_timer_stop,
            "Parar",
            buildActionPendingIntent(ACTION_STOP, 1)
        )

        // Ação ⏸ pausar  ou  ▶ continuar
        val toggleAction = if (isRunning) {
            NotificationCompat.Action(
                R.drawable.ic_timer_pause,
                "Pausar",
                buildActionPendingIntent(ACTION_PAUSE, 2)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_timer_play,
                "Continuar",
                buildActionPendingIntent(ACTION_RESUME, 3)
            )
        }

        val statusText = when {
            isRunning -> timeText
            isPaused  -> "$timeText Pausado"
            else      -> timeText
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cronômetro")
            .setContentText(statusText)
            .setContentIntent(openAppPending)
            .setOngoing(true)           // não pode ser dispensada por swipe
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)     // não faz som a cada update
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Estilo media player: define a ordem dos botões
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1) // mostra ambos no modo compacto
            )
            .addAction(stopAction)      // índice 0 → X
            .addAction(toggleAction)    // índice 1 → ⏸ ou ▶
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun hideTimerNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun buildActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}