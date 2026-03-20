package com.gymapp.utils

object LogTags {
    // Timer related logs
    const val TIMER_VIEWMODEL = "TimerViewModel"
    const val TIMER_NOTIFICATION = "TimerNotification"
    const val TIMER_RECEIVER = "TimerReceiver"
    const val TIMER_SCREEN = "TimerScreen"
    
    // General app logs
    const val APP_MAIN = "GymApp"
    const val PERMISSIONS = "Permissions"
    
    // Workout related logs
    const val WORKOUT_VIEWMODEL = "WorkoutViewModel"
    const val FIREBASE_REPO = "FirebaseRepo"
    const val CACHE = "Cache"
    
    // Função para exibir todas as tags de uma vez
    fun logAllTags() {
        android.util.Log.d(APP_MAIN, "=== TODAS AS TAGS DISPONÍVEIS ===")
        android.util.Log.d(APP_MAIN, "TimerViewModel - Logs do ViewModel do cronômetro")
        android.util.Log.d(APP_MAIN, "TimerNotification - Logs do serviço de notificação")
        android.util.Log.d(APP_MAIN, "TimerReceiver - Logs do BroadcastReceiver")
        android.util.Log.d(APP_MAIN, "TimerScreen - Logs da tela do cronômetro")
        android.util.Log.d(APP_MAIN, "GymApp - Logs gerais do aplicativo")
        android.util.Log.d(APP_MAIN, "Permissions - Logs de permissões")
        android.util.Log.d(APP_MAIN, "WorkoutViewModel - Logs do ViewModel de treinos")
        android.util.Log.d(APP_MAIN, "FirebaseRepo - Logs do repositório Firebase")
        android.util.Log.d(APP_MAIN, "Cache - Logs de cache")
        android.util.Log.d(APP_MAIN, "=== FIM DAS TAGS ===")
    }
    
    // Função para filtrar todas as tags no Logcat
    fun getLogcatFilter(): String {
        return "tag:TimerViewModel OR tag:TimerNotification OR tag:TimerReceiver OR tag:TimerScreen OR tag:GymApp OR tag:Permissions OR tag:WorkoutViewModel OR tag:FirebaseRepo OR tag:Cache"
    }
}
