package com.gymapp.data.firebase

data class ExerciseProgressData(
    val exerciseName: String,
    val currentWeight: Double,
    val currentReps: Int,
    val previousWeight: Double,
    val previousReps: Int,
    val improvement: String
)
