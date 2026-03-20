package com.gymapp.ui.viewmodel

data class ExerciseSet(
    val id: Long = System.currentTimeMillis(),
    val weight: Double,
    val reps: Int,
    val restTime: Int = 0 // em segundos
)
