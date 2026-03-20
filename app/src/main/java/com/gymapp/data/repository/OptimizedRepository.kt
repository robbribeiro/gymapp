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

/**
 * Repositório otimizado que implementa carregamento sob demanda
 * Consulta o Firebase apenas quando necessário, conforme solicitado pelo usuário
 */
class OptimizedRepository(private val context: Context) {
    companion object {
        private const val TAG = "GYMAPP_DATABASE_LOGS"
    }
    
    private val firebaseRepository = FirebaseRepositoryOptimized()
    private val localCache = LocalCache(context)
    
    // Flags para controlar o que já foi carregado
    private var exercisesLoaded = false
    private var workoutsLoaded = false
    private var weeksLoaded = false
    
    // Cache em memória para evitar consultas repetidas
    private val exercisesCache = mutableListOf<Exercise>()
    private val workoutsCache = mutableListOf<Workout>()
    private val weeksCache = mutableListOf<WorkoutWeek>()
    
    // ========== EXERCISES (CARREGAMENTO SOB DEMANDA) ==========
    
    suspend fun getAllExercises(): Flow<List<Exercise>> = flow {
        
        // Se já carregou, usar cache
        if (exercisesLoaded && exercisesCache.isNotEmpty()) {
            emit(exercisesCache)
            return@flow
        }
        
        // Verificar cache local primeiro
        val cachedExercises = localCache.getExercises()
        if (cachedExercises.isNotEmpty()) {
            exercisesCache.clear()
            exercisesCache.addAll(cachedExercises)
            exercisesLoaded = true
            emit(cachedExercises)
            return@flow
        }
        
        // Se não tem cache, buscar do Firebase
        firebaseRepository.getAllExercises().collect { exercises ->
            
            // Atualizar cache local
            localCache.saveExercises(exercises)
            
            // Atualizar cache em memória
            exercisesCache.clear()
            exercisesCache.addAll(exercises)
            exercisesLoaded = true
            
            emit(exercises)
        }
    }
    
    suspend fun addExercise(exercise: Exercise): String? {
        val startTime = System.currentTimeMillis()
        
        val exerciseId = firebaseRepository.addExercise(exercise)
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        if (exerciseId != null) {
            val exerciseWithId = exercise.copy(id = exerciseId)
            
            // Atualizar cache local
            localCache.addExercise(exerciseWithId)
            
            // Atualizar cache em memória
            exercisesCache.add(exerciseWithId)
            
        } else {
        }
        return exerciseId
    }
    
    suspend fun deleteExercise(exerciseId: String): Boolean {
        val success = firebaseRepository.deleteExercise(exerciseId)
        if (success) {
            // Atualizar cache local
            localCache.deleteExercise(exerciseId)
            
            // Atualizar cache em memória
            exercisesCache.removeAll { it.id == exerciseId }
            
        }
        return success
    }
    
    // ========== WORKOUTS (CARREGAMENTO SOB DEMANDA) ==========
    
    suspend fun getAllWorkouts(): Flow<List<Workout>> = flow {
        // Se já carregou, usar cache
        if (workoutsLoaded && workoutsCache.isNotEmpty()) {
            emit(workoutsCache)
            return@flow
        }
        
        // Verificar cache local primeiro
        val cachedWorkouts = localCache.getWorkouts()
        if (cachedWorkouts.isNotEmpty()) {
            workoutsCache.clear()
            workoutsCache.addAll(cachedWorkouts)
            workoutsLoaded = true
            emit(cachedWorkouts)
            return@flow
        }
        
        // Se não tem cache, buscar do Firebase
        firebaseRepository.getAllWorkouts().collect { workouts ->
            // Atualizar cache local
            localCache.saveWorkouts(workouts)
            
            // Atualizar cache em memória
            workoutsCache.clear()
            workoutsCache.addAll(workouts)
            workoutsLoaded = true
            
            emit(workouts)
        }
    }
    
    suspend fun addWorkout(workout: Workout): String? {
        val workoutId = firebaseRepository.addWorkout(workout)
        if (workoutId != null) {
            val workoutWithId = workout.copy(id = workoutId)
            
            // Atualizar cache local
            localCache.addWorkout(workoutWithId)
            
            // Atualizar cache em memória
            workoutsCache.add(workoutWithId)
            
        }
        return workoutId
    }
    
    suspend fun deleteWorkout(workoutId: String): Boolean {
        val success = firebaseRepository.deleteWorkout(workoutId)
        if (success) {
            // Atualizar cache local
            localCache.deleteWorkout(workoutId)
            
            // Atualizar cache em memória
            workoutsCache.removeAll { it.id == workoutId }
            
        }
        return success
    }
    
    // ========== WORKOUT WEEKS (CARREGAMENTO SOB DEMANDA) ==========
    
    suspend fun getAllWorkoutWeeks(): Flow<List<WorkoutWeek>> = flow {
        // Se já carregou, usar cache
        if (weeksLoaded && weeksCache.isNotEmpty()) {
            emit(weeksCache)
            return@flow
        }
        
        // Verificar cache local primeiro
        val cachedWeeks = localCache.getWorkoutWeeks()
        if (cachedWeeks.isNotEmpty()) {
            weeksCache.clear()
            weeksCache.addAll(cachedWeeks)
            weeksLoaded = true
            emit(cachedWeeks)
            return@flow
        }
        
        // Se não tem cache, buscar do Firebase
        firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
            // Atualizar cache local
            localCache.saveWorkoutWeeks(weeks)
            
            // Atualizar cache em memória
            weeksCache.clear()
            weeksCache.addAll(weeks)
            weeksLoaded = true
            
            emit(weeks)
        }
    }
    
    suspend fun addWorkoutWeek(week: WorkoutWeek): String? {
        val weekId = firebaseRepository.addWorkoutWeek(week)
        if (weekId != null) {
            val weekWithId = week.copy(id = weekId)
            
            // Atualizar cache em memória
            weeksCache.add(weekWithId)
            
        }
        return weekId
    }
    
    suspend fun deleteWorkoutWeek(weekId: String): Boolean {
        val success = firebaseRepository.deleteWorkoutWeek(weekId)
        if (success) {
            // Atualizar cache em memória
            weeksCache.removeAll { it.id == weekId }
            
        }
        return success
    }
    
    // ========== EXERCISES BY WORKOUT (SEMPRE DO FIREBASE) ==========
    
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
    
    // ========== SETS (CARREGAMENTO SOB DEMANDA) ==========
    
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
        
        // Limpar caches
        clearAllCaches()
        
        // Recarregar todos os dados do Firebase
        firebaseRepository.getAllExercises().collect { exercises ->
            localCache.saveExercises(exercises)
            exercisesCache.clear()
            exercisesCache.addAll(exercises)
        }
        
        firebaseRepository.getAllWorkouts().collect { workouts ->
            localCache.saveWorkouts(workouts)
            workoutsCache.clear()
            workoutsCache.addAll(workouts)
        }
        
        firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
            localCache.saveWorkoutWeeks(weeks)
            weeksCache.clear()
            weeksCache.addAll(weeks)
        }
        
        exercisesLoaded = true
        workoutsLoaded = true
        weeksLoaded = true
        
    }
    
    fun isCacheValid(): Boolean {
        return localCache.isCacheValid()
    }
    
    fun clearCache() {
        localCache.clearCache()
    }
    
    fun clearAllCaches() {
        localCache.clearCache()
        exercisesCache.clear()
        workoutsCache.clear()
        weeksCache.clear()
        exercisesLoaded = false
        workoutsLoaded = false
        weeksLoaded = false
    }
    
    // ========== UTILITY METHODS ==========
    
    fun isExercisesLoaded(): Boolean = exercisesLoaded
    fun isWorkoutsLoaded(): Boolean = workoutsLoaded
    fun isWeeksLoaded(): Boolean = weeksLoaded
    
    fun getCachedExercises(): List<Exercise> = exercisesCache.toList()
    fun getCachedWorkouts(): List<Workout> = workoutsCache.toList()
    fun getCachedWeeks(): List<WorkoutWeek> = weeksCache.toList()
}
