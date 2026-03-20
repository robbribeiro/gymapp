package com.gymapp.data.persistence

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.WorkoutWeek
import com.gymapp.data.firebase.Set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalCache(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gym_app_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_EXERCISES = "exercises"
        private const val KEY_WORKOUTS = "workouts"
        private const val KEY_WORKOUT_WEEKS = "workout_weeks"
        private const val KEY_SETS = "sets"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_USER_ID = "user_id"
        
        // Cache válido por 1 hora para dados que não mudam frequentemente
        private const val CACHE_VALIDITY_MS = 60 * 60 * 1000L
    }
    
    // ========== EXERCISES ==========
    
    suspend fun saveExercises(exercises: List<Exercise>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(exercises)
        prefs.edit().putString(KEY_EXERCISES, json).apply()
        updateLastSync()
    }
    
    suspend fun getExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_EXERCISES, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Exercise>>() {}.type
                gson.fromJson<List<Exercise>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    suspend fun addExercise(exercise: Exercise) = withContext(Dispatchers.IO) {
        val currentExercises = getExercises().toMutableList()
        currentExercises.add(exercise)
        saveExercises(currentExercises)
    }
    
    suspend fun updateExercise(exercise: Exercise) = withContext(Dispatchers.IO) {
        val currentExercises = getExercises().toMutableList()
        val index = currentExercises.indexOfFirst { it.id == exercise.id }
        if (index != -1) {
            currentExercises[index] = exercise
            saveExercises(currentExercises)
        }
    }
    
    suspend fun deleteExercise(exerciseId: String) = withContext(Dispatchers.IO) {
        val currentExercises = getExercises().toMutableList()
        currentExercises.removeAll { it.id == exerciseId }
        saveExercises(currentExercises)
    }
    
    // ========== WORKOUTS ==========
    
    suspend fun saveWorkouts(workouts: List<Workout>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(workouts)
        prefs.edit().putString(KEY_WORKOUTS, json).apply()
        updateLastSync()
    }
    
    suspend fun getWorkouts(): List<Workout> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_WORKOUTS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Workout>>() {}.type
                gson.fromJson<List<Workout>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    suspend fun addWorkout(workout: Workout) = withContext(Dispatchers.IO) {
        val currentWorkouts = getWorkouts().toMutableList()
        currentWorkouts.add(workout)
        saveWorkouts(currentWorkouts)
    }
    
    suspend fun updateWorkout(workout: Workout) = withContext(Dispatchers.IO) {
        val currentWorkouts = getWorkouts().toMutableList()
        val index = currentWorkouts.indexOfFirst { it.id == workout.id }
        if (index != -1) {
            currentWorkouts[index] = workout
            saveWorkouts(currentWorkouts)
        }
    }
    
    suspend fun deleteWorkout(workoutId: String) = withContext(Dispatchers.IO) {
        val currentWorkouts = getWorkouts().toMutableList()
        currentWorkouts.removeAll { it.id == workoutId }
        saveWorkouts(currentWorkouts)
    }
    
    // ========== WORKOUT WEEKS ==========
    
    suspend fun saveWorkoutWeeks(weeks: List<WorkoutWeek>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(weeks)
        prefs.edit().putString(KEY_WORKOUT_WEEKS, json).apply()
        updateLastSync()
    }
    
    suspend fun getWorkoutWeeks(): List<WorkoutWeek> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_WORKOUT_WEEKS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<WorkoutWeek>>() {}.type
                gson.fromJson<List<WorkoutWeek>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    // ========== SETS ==========
    
    suspend fun saveSets(sets: List<Set>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(sets)
        prefs.edit().putString(KEY_SETS, json).apply()
        updateLastSync()
    }
    
    suspend fun getSets(): List<Set> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_SETS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Set>>() {}.type
                gson.fromJson<List<Set>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    suspend fun addSet(set: Set) = withContext(Dispatchers.IO) {
        val currentSets = getSets().toMutableList()
        currentSets.add(set)
        saveSets(currentSets)
    }
    
    suspend fun updateSet(set: Set) = withContext(Dispatchers.IO) {
        val currentSets = getSets().toMutableList()
        val index = currentSets.indexOfFirst { it.id == set.id }
        if (index != -1) {
            currentSets[index] = set
            saveSets(currentSets)
        }
    }
    
    suspend fun deleteSet(setId: String) = withContext(Dispatchers.IO) {
        val currentSets = getSets().toMutableList()
        currentSets.removeAll { it.id == setId }
        saveSets(currentSets)
    }
    
    // ========== CACHE MANAGEMENT ==========
    
    private fun updateLastSync() {
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
    }
    
    fun isCacheValid(): Boolean {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        return (System.currentTimeMillis() - lastSync) < CACHE_VALIDITY_MS
    }
    
    fun clearCache() {
        prefs.edit().clear().apply()
    }
    
    fun setUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    fun isFirstLaunch(): Boolean {
        return !prefs.contains(KEY_LAST_SYNC)
    }
    
    // ========== SPECIFIC QUERIES ==========
    
    suspend fun getExercisesByWorkout(workoutId: String): List<Exercise> = withContext(Dispatchers.IO) {
        // Esta lógica será implementada no repository híbrido
        emptyList()
    }
    
    suspend fun getSetsByWorkout(workoutId: String): List<Set> = withContext(Dispatchers.IO) {
        getSets().filter { it.workoutId == workoutId }
            .sortedBy { it.createdAt }
    }
    
    suspend fun getSetsByExercise(workoutId: String, exerciseName: String): List<Set> = withContext(Dispatchers.IO) {
        getSets().filter { it.workoutId == workoutId && it.exerciseName == exerciseName }
            .sortedBy { it.createdAt }
    }
}
