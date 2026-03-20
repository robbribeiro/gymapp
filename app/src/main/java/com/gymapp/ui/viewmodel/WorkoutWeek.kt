package com.gymapp.ui.viewmodel

import java.util.*

data class WorkoutWeek(
    val id: String = "",
    val weekStart: Date,
    val weekEnd: Date,
    val weekName: String,
    val workouts: List<WorkoutItem>,
    val totalVolume: Double = 0.0,
    val totalWorkouts: Int = workouts.size
) {
    val weekLabel: String
        get() = weekName
    
    val dateRange: String
        get() {
            val startFormat = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
            val endFormat = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
            return "${startFormat.format(weekStart)} - ${endFormat.format(weekEnd)}"
        }
}