package com.gymapp.data.firebase

import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FirebaseCache {
    // Cache global para exercícios
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()
    
    // Cache global para treinos
    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()
    
    // Cache global para semanas de treino
    private val _workoutWeeks = MutableStateFlow<List<WorkoutWeek>>(emptyList())
    val workoutWeeks: StateFlow<List<WorkoutWeek>> = _workoutWeeks.asStateFlow()
    
    // Cache global para progresso de exercícios
    private val _exerciseProgress = MutableStateFlow<List<ExerciseProgressData>>(emptyList())
    val exerciseProgress: StateFlow<List<ExerciseProgressData>> = _exerciseProgress.asStateFlow()
    
    // Cache global para progresso semanal
    private val _weeklyProgress = MutableStateFlow<List<WeeklyProgressData>>(emptyList())
    val weeklyProgress: StateFlow<List<WeeklyProgressData>> = _weeklyProgress.asStateFlow()
    
    // Timestamps para controle de cache
    private var lastExercisesUpdate = 0L
    private var lastWorkoutsUpdate = 0L
    private var lastWeeksUpdate = 0L
    private var lastProgressUpdate = 0L
    private var lastWeeklyProgressUpdate = 0L
    
    private val cacheTimeout = 60000L // 1 minuto
    
    fun updateExercises(exercises: List<Exercise>) {
        _exercises.value = exercises
        lastExercisesUpdate = System.currentTimeMillis()
    }
    
    fun updateWorkouts(workouts: List<Workout>) {
        _workouts.value = workouts
        lastWorkoutsUpdate = System.currentTimeMillis()
    }
    
    fun updateWorkoutWeeks(weeks: List<WorkoutWeek>) {
        _workoutWeeks.value = weeks
        lastWeeksUpdate = System.currentTimeMillis()
    }
    
    fun updateExerciseProgress(progress: List<ExerciseProgressData>) {
        _exerciseProgress.value = progress
        lastProgressUpdate = System.currentTimeMillis()
    }
    
    fun updateWeeklyProgress(progress: List<WeeklyProgressData>) {
        _weeklyProgress.value = progress
        lastWeeklyProgressUpdate = System.currentTimeMillis()
    }
    
    fun addExercise(exercise: Exercise) {
        val currentExercises = _exercises.value.toMutableList()
        currentExercises.add(exercise)
        _exercises.value = currentExercises
        lastExercisesUpdate = System.currentTimeMillis()
    }
    
    fun removeExercise(exerciseId: String) {
        val currentExercises = _exercises.value.toMutableList()
        currentExercises.removeAll { it.id == exerciseId }
        _exercises.value = currentExercises
        lastExercisesUpdate = System.currentTimeMillis()
    }
    
    fun addWorkout(workout: Workout) {
        val currentWorkouts = _workouts.value.toMutableList()
        currentWorkouts.add(workout)
        _workouts.value = currentWorkouts
        lastWorkoutsUpdate = System.currentTimeMillis()
    }
    
    fun removeWorkout(workoutId: String) {
        val currentWorkouts = _workouts.value.toMutableList()
        currentWorkouts.removeAll { it.id == workoutId }
        _workouts.value = currentWorkouts
        lastWorkoutsUpdate = System.currentTimeMillis()
    }
    
    fun addWorkoutWeek(week: WorkoutWeek) {
        val currentWeeks = _workoutWeeks.value.toMutableList()
        currentWeeks.add(week)
        _workoutWeeks.value = currentWeeks
        lastWeeksUpdate = System.currentTimeMillis()
    }
    
    fun removeWorkoutWeek(weekId: String) {
        val currentWeeks = _workoutWeeks.value.toMutableList()
        currentWeeks.removeAll { it.id == weekId }
        _workoutWeeks.value = currentWeeks
        lastWeeksUpdate = System.currentTimeMillis()
    }
    
    fun updateWorkoutWeek(updatedWeek: WorkoutWeek) {
        val currentWeeks = _workoutWeeks.value.toMutableList()
        val index = currentWeeks.indexOfFirst { it.id == updatedWeek.id }
        if (index != -1) {
            currentWeeks[index] = updatedWeek
            _workoutWeeks.value = currentWeeks
            lastWeeksUpdate = System.currentTimeMillis()
        } else {
        }
    }
    
    fun isExercisesCacheValid(): Boolean {
        return _exercises.value.isNotEmpty() && (System.currentTimeMillis() - lastExercisesUpdate) < cacheTimeout
    }
    
    fun isWorkoutsCacheValid(): Boolean {
        return _workouts.value.isNotEmpty() && (System.currentTimeMillis() - lastWorkoutsUpdate) < cacheTimeout
    }
    
    fun isWeeksCacheValid(): Boolean {
        return _workoutWeeks.value.isNotEmpty() && (System.currentTimeMillis() - lastWeeksUpdate) < cacheTimeout
    }
    
    fun isProgressCacheValid(): Boolean {
        return _exerciseProgress.value.isNotEmpty() && (System.currentTimeMillis() - lastProgressUpdate) < cacheTimeout
    }
    
    fun isWeeklyProgressCacheValid(): Boolean {
        return _weeklyProgress.value.isNotEmpty() && (System.currentTimeMillis() - lastWeeklyProgressUpdate) < cacheTimeout
    }
    
    fun clearAllCache() {
        _exercises.value = emptyList()
        _workouts.value = emptyList()
        _workoutWeeks.value = emptyList()
        _exerciseProgress.value = emptyList()
        _weeklyProgress.value = emptyList()
        lastExercisesUpdate = 0L
        lastWorkoutsUpdate = 0L
        lastWeeksUpdate = 0L
        lastProgressUpdate = 0L
        lastWeeklyProgressUpdate = 0L
    }
}
