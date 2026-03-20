package com.gymapp.ui.viewmodel

import java.util.*

data class WorkoutItem(
    val id: Long,
    val name: String,
    val date: Date,
    val exerciseCount: Int,
    val duration: String,
    val isCompleted: Boolean,
    val exercises: List<ExerciseWithSets>,
    val firebaseId: String = "" // ID real do Firebase
)

data class ExerciseWithSets(
    val name: String,
    val sets: List<ExerciseSet> = emptyList()
)

