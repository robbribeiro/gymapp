package com.gymapp.data.repository

import android.content.Context
import com.gymapp.data.firebase.FirebaseRepositoryOptimized
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.WorkoutWeek
import com.gymapp.data.firebase.Set
import com.gymapp.data.persistence.LocalCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class HybridRepository(private val context: Context) {
    private val firebaseRepository = FirebaseRepositoryOptimized()
    private val localCache = LocalCache(context)
    
    // ========== EXERCISES ==========
    
    suspend fun getAllExercises(): Flow<List<Exercise>> = flow {
        // Se é o primeiro launch, buscar do Firebase
        if (localCache.isFirstLaunch()) {
            firebaseRepository.getAllExercises().collect { exercises ->
                localCache.saveExercises(exercises)
                emit(exercises)
            }
        } else {
            // Usar cache local primeiro
            val cachedExercises = localCache.getExercises()
            if (cachedExercises.isNotEmpty()) {
                emit(cachedExercises)
            } else {
                // Se cache vazio, buscar do Firebase
                firebaseRepository.getAllExercises().collect { exercises ->
                    localCache.saveExercises(exercises)
                    emit(exercises)
                }
            }
        }
    }
    
    suspend fun addExercise(exercise: Exercise): String? {
        // Adicionar ao Firebase primeiro
        val exerciseId = firebaseRepository.addExercise(exercise)
        if (exerciseId != null) {
            // Se sucesso no Firebase, atualizar cache local
            val exerciseWithId = exercise.copy(id = exerciseId)
            localCache.addExercise(exerciseWithId)
        }
        return exerciseId
    }
    
    suspend fun deleteExercise(exerciseId: String): Boolean {
        val success = firebaseRepository.deleteExercise(exerciseId)
        if (success) {
            localCache.deleteExercise(exerciseId)
        }
        return success
    }
    
    // ========== WORKOUTS ==========
    
    suspend fun getAllWorkouts(): Flow<List<Workout>> = flow {
        // Se é o primeiro launch, buscar do Firebase
        if (localCache.isFirstLaunch()) {
            firebaseRepository.getAllWorkouts().collect { workouts ->
                localCache.saveWorkouts(workouts)
                emit(workouts)
            }
        } else {
            // Usar cache local primeiro
            val cachedWorkouts = localCache.getWorkouts()
            if (cachedWorkouts.isNotEmpty()) {
                emit(cachedWorkouts)
            } else {
                // Se cache vazio, buscar do Firebase
                firebaseRepository.getAllWorkouts().collect { workouts ->
                    localCache.saveWorkouts(workouts)
                    emit(workouts)
                }
            }
        }
    }
    
    suspend fun addWorkout(workout: Workout): String? {
        val workoutId = firebaseRepository.addWorkout(workout)
        if (workoutId != null) {
            val workoutWithId = workout.copy(id = workoutId)
            localCache.addWorkout(workoutWithId)
        }
        return workoutId
    }
    
    suspend fun deleteWorkout(workoutId: String): Boolean {
        val success = firebaseRepository.deleteWorkout(workoutId)
        if (success) {
            localCache.deleteWorkout(workoutId)
        }
        return success
    }
    
    // ========== WORKOUT WEEKS ==========
    
    suspend fun getAllWorkoutWeeks(): Flow<List<WorkoutWeek>> = flow {
        if (localCache.isFirstLaunch()) {
            firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
                localCache.saveWorkoutWeeks(weeks)
                emit(weeks)
            }
        } else {
            val cachedWeeks = localCache.getWorkoutWeeks()
            if (cachedWeeks.isNotEmpty()) {
                emit(cachedWeeks)
            } else {
                firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
                    localCache.saveWorkoutWeeks(weeks)
                    emit(weeks)
                }
            }
        }
    }
    
    suspend fun addWorkoutWeek(week: WorkoutWeek): String? {
        val weekId = firebaseRepository.addWorkoutWeek(week)
        if (weekId != null) {
            val weekWithId = week.copy(id = weekId)
            // Adicionar ao cache local seria necessário implementar
        }
        return weekId
    }
    
    suspend fun deleteWorkoutWeek(weekId: String): Boolean {
        val success = firebaseRepository.deleteWorkoutWeek(weekId)
        if (success) {
        }
        return success
    }
    
    // ========== EXERCISES BY WORKOUT ==========
    
    suspend fun getExercisesByWorkout(workoutId: String): List<Exercise> {
        // Para exercícios de treino, sempre buscar do Firebase pois a estrutura é complexa
        return firebaseRepository.getExercisesByWorkout(workoutId)
    }
    
    suspend fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        firebaseRepository.addExerciseToWorkout(workoutId, exerciseId)
    }
    
    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String): Boolean {
        val success = firebaseRepository.removeExerciseFromWorkout(workoutId, exerciseId)
        if (success) {
        }
        return success
    }
    
    // ========== SETS ==========
    
    suspend fun getSetsByWorkout(workoutId: String): Flow<List<Set>> = flow {
        // Usar cache local primeiro para séries
        val cachedSets = localCache.getSetsByWorkout(workoutId)
        if (cachedSets.isNotEmpty()) {
            emit(cachedSets)
        } else {
            // Se cache vazio, buscar do Firebase
            firebaseRepository.getSetsByWorkout(workoutId).collect { sets ->
                localCache.saveSets(sets)
                emit(sets)
            }
        }
    }
    
    suspend fun addSet(set: Set): String? {
        val setId = firebaseRepository.addSet(set)
        if (setId != null) {
            val setWithId = set.copy(id = setId)
            localCache.addSet(setWithId)
        }
        return setId
    }
    
    suspend fun addSetToExercise(workoutId: String, exerciseId: String, weight: Double, reps: Int) {
        firebaseRepository.addSetToExercise(workoutId, exerciseId, weight, reps)
    }
    
    suspend fun deleteSet(setId: String): Boolean {
        val success = firebaseRepository.deleteSet(setId)
        if (success) {
            localCache.deleteSet(setId)
        }
        return success
    }
    
    // ========== CACHE MANAGEMENT ==========
    
    suspend fun forceSyncFromFirebase() {
        
        // Limpar cache local
        localCache.clearCache()
        
        // Recarregar todos os dados do Firebase
        firebaseRepository.getAllExercises().collect { exercises ->
            localCache.saveExercises(exercises)
        }
        
        firebaseRepository.getAllWorkouts().collect { workouts ->
            localCache.saveWorkouts(workouts)
        }
        
        firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
            localCache.saveWorkoutWeeks(weeks)
        }
        
    }
    
    fun isCacheValid(): Boolean {
        return localCache.isCacheValid()
    }
    
    fun clearCache() {
        localCache.clearCache()
    }
}
