package com.gymapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.*

class WorkoutViewModel : ViewModel() {
    private var _workouts by mutableStateOf(getInitialWorkouts())
    val workouts: List<WorkoutItem> get() = _workouts
    
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
    }
    
    fun updateWorkout(workout: WorkoutItem) {
        _workouts = _workouts.map { 
            if (it.id == workout.id) workout else it 
        }
    }
    
    fun deleteWorkout(workoutId: Long) {
        _workouts = _workouts.filter { it.id != workoutId }
    }
    
    private fun getInitialWorkouts(): List<WorkoutItem> {
        return listOf(
            WorkoutItem(
                id = 1,
                name = "Treino de Peito",
                date = Date(),
                exerciseCount = 5,
                duration = "45 min",
                isCompleted = true,
                exercises = listOf(
                    ExerciseWithSets(name = "Supino Reto", sets = listOf(
                        ExerciseSet(weight = 80.0, reps = 10),
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 75.0, reps = 12)
                    )),
                    ExerciseWithSets(name = "Supino Inclinado"),
                    ExerciseWithSets(name = "Crucifixo"),
                    ExerciseWithSets(name = "Flexão"),
                    ExerciseWithSets(name = "Tríceps Pulley")
                )
            ),
            WorkoutItem(
                id = 2,
                name = "Treino de Costas",
                date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time,
                exerciseCount = 6,
                duration = "50 min",
                isCompleted = true,
                exercises = listOf(
                    ExerciseWithSets(name = "Puxada Frontal"),
                    ExerciseWithSets(name = "Remada Curvada"),
                    ExerciseWithSets(name = "Puxada Alta"),
                    ExerciseWithSets(name = "Remada Unilateral"),
                    ExerciseWithSets(name = "Encolhimento"),
                    ExerciseWithSets(name = "Rosca Direta")
                )
            ),
            WorkoutItem(
                id = 3,
                name = "Treino de Pernas",
                date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -2) }.time,
                exerciseCount = 7,
                duration = "60 min",
                isCompleted = false,
                exercises = listOf(
                    ExerciseWithSets(name = "Agachamento"),
                    ExerciseWithSets(name = "Leg Press"),
                    ExerciseWithSets(name = "Afundo"),
                    ExerciseWithSets(name = "Cadeira Extensora"),
                    ExerciseWithSets(name = "Cadeira Flexora"),
                    ExerciseWithSets(name = "Panturrilha"),
                    ExerciseWithSets(name = "Stiff")
                )
            )
        )
    }
}

