package com.gymapp.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gymapp.data.persistence.WorkoutPersistence
import java.util.*

class PersistentWorkoutViewModel(private val context: Context) : ViewModel() {
    private val persistence = WorkoutPersistence(context)
    private var _workouts by mutableStateOf(persistence.loadWorkouts())
    val workouts: List<WorkoutItem> get() = _workouts
    
    private fun saveWorkouts() {
        persistence.saveWorkouts(_workouts)
    }
    
    fun addWorkout(name: String) {
        val newWorkout = WorkoutItem(
            id = System.currentTimeMillis(),
            name = name,
            date = Date(),
            exerciseCount = 0,
            duration = "0 min",
            isCompleted = false,
            exercises = emptyList()
        )
        _workouts = _workouts + newWorkout
        saveWorkouts()
    }
    
    fun addExerciseToWorkout(workoutId: Long, exerciseName: String) {
        _workouts = _workouts.map { workout ->
            if (workout.id == workoutId) {
                val newExercise = ExerciseWithSets(name = exerciseName)
                val updatedExercises = workout.exercises + newExercise
                workout.copy(
                    exercises = updatedExercises,
                    exerciseCount = updatedExercises.size
                )
            } else {
                workout
            }
        }
        saveWorkouts()
    }
    
    fun removeExerciseFromWorkout(workoutId: Long, exerciseIndex: Int) {
        _workouts = _workouts.map { workout ->
            if (workout.id == workoutId) {
                val updatedExercises = workout.exercises.toMutableList().apply {
                    removeAt(exerciseIndex)
                }
                workout.copy(
                    exercises = updatedExercises,
                    exerciseCount = updatedExercises.size
                )
            } else {
                workout
            }
        }
        saveWorkouts()
    }
    
    fun addSetToExercise(workoutId: Long, exerciseIndex: Int, weight: Double, reps: Int) {
        _workouts = _workouts.map { workout ->
            if (workout.id == workoutId) {
                val updatedExercises = workout.exercises.toMutableList()
                val exercise = updatedExercises[exerciseIndex]
                val newSet = ExerciseSet(weight = weight, reps = reps)
                val updatedExercise = exercise.copy(sets = exercise.sets + newSet)
                updatedExercises[exerciseIndex] = updatedExercise
                
                workout.copy(exercises = updatedExercises)
            } else {
                workout
            }
        }
        saveWorkouts()
    }
    
    fun removeSetFromExercise(workoutId: Long, exerciseIndex: Int, setIndex: Int) {
        _workouts = _workouts.map { workout ->
            if (workout.id == workoutId) {
                val updatedExercises = workout.exercises.toMutableList()
                val exercise = updatedExercises[exerciseIndex]
                val updatedSets = exercise.sets.toMutableList().apply {
                    removeAt(setIndex)
                }
                val updatedExercise = exercise.copy(sets = updatedSets)
                updatedExercises[exerciseIndex] = updatedExercise
                
                workout.copy(exercises = updatedExercises)
            } else {
                workout
            }
        }
        saveWorkouts()
    }
    
    fun updateWorkout(workout: WorkoutItem) {
        _workouts = _workouts.map { 
            if (it.id == workout.id) workout else it 
        }
        saveWorkouts()
    }
    
    fun deleteWorkout(workoutId: Long) {
        _workouts = _workouts.filter { it.id != workoutId }
        saveWorkouts()
    }
    
}
