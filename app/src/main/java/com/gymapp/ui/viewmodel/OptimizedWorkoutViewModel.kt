package com.gymapp.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.data.repository.HybridRepository
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.Set
import com.gymapp.data.firebase.WorkoutWeek
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OptimizedWorkoutViewModel(private val context: Context) : ViewModel() {
    
    private val hybridRepository = HybridRepository(context)
    
    // ========== STATE FLOWS ==========
    
    private val _allExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val allExercises: StateFlow<List<Exercise>> = _allExercises.asStateFlow()
    
    private val _allWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val allWorkouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()
    
    private val _allWorkoutWeeks = MutableStateFlow<List<WorkoutWeek>>(emptyList())
    val allWorkoutWeeks: StateFlow<List<WorkoutWeek>> = _allWorkoutWeeks.asStateFlow()
    
    // Cache para exercícios por treino
    private val _workoutExercises = MutableStateFlow<Map<String, List<Exercise>>>(emptyMap())
    val workoutExercises: StateFlow<Map<String, List<Exercise>>> = _workoutExercises.asStateFlow()
    
    // Cache para séries por treino
    private val _workoutSets = MutableStateFlow<Map<String, List<Set>>>(emptyMap())
    val workoutSets: StateFlow<Map<String, List<Set>>> = _workoutSets.asStateFlow()
    
    // Cache para séries por exercício (otimização)
    private val setsCache = mutableMapOf<String, List<Set>>()
    private var lastWorkoutId = ""
    
    // ========== INITIALIZATION ==========
    
    init {
        // Carregar dados iniciais do cache local
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                
                // Carregar exercícios
                hybridRepository.getAllExercises().collect { exercises ->
                    _allExercises.value = exercises
                }
                
                // Carregar treinos
                hybridRepository.getAllWorkouts().collect { workouts ->
                    _allWorkouts.value = workouts
                }
                
                // Carregar semanas
                hybridRepository.getAllWorkoutWeeks().collect { weeks ->
                    _allWorkoutWeeks.value = weeks
                }
                
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== EXERCISES ==========
    
    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                val exerciseId = hybridRepository.addExercise(exercise)
                if (exerciseId != null) {
                    // Atualizar lista local imediatamente
                    val updatedExercises = _allExercises.value.toMutableList()
                    updatedExercises.add(exercise.copy(id = exerciseId))
                    _allExercises.value = updatedExercises
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            try {
                val success = hybridRepository.deleteExercise(exerciseId)
                if (success) {
                    // Atualizar lista local imediatamente
                    val updatedExercises = _allExercises.value.toMutableList()
                    updatedExercises.removeAll { it.id == exerciseId }
                    _allExercises.value = updatedExercises
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WORKOUTS ==========
    
    fun addWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                val workoutId = hybridRepository.addWorkout(workout)
                if (workoutId != null) {
                    // Atualizar lista local imediatamente
                    val updatedWorkouts = _allWorkouts.value.toMutableList()
                    updatedWorkouts.add(workout.copy(id = workoutId))
                    _allWorkouts.value = updatedWorkouts
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                val success = hybridRepository.deleteWorkout(workoutId)
                if (success) {
                    // Atualizar lista local imediatamente
                    val updatedWorkouts = _allWorkouts.value.toMutableList()
                    updatedWorkouts.removeAll { it.id == workoutId }
                    _allWorkouts.value = updatedWorkouts
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WORKOUT EXERCISES ==========
    
    fun loadWorkoutExercises(workoutId: String) {
        viewModelScope.launch {
            try {
                val exercises = hybridRepository.getExercisesByWorkout(workoutId)
                
                val currentMap = _workoutExercises.value.toMutableMap()
                currentMap[workoutId] = exercises
                _workoutExercises.value = currentMap
                
            } catch (e: Exception) {
            }
        }
    }
    
    fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                hybridRepository.addExerciseToWorkout(workoutId, exerciseId)
                
                // Recarregar exercícios do treino
                loadWorkoutExercises(workoutId)
            } catch (e: Exception) {
            }
        }
    }
    
    fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                val success = hybridRepository.removeExerciseFromWorkout(workoutId, exerciseId)
                if (success) {
                    // Atualizar UI imediatamente
                    val currentExercises = getWorkoutExercises(workoutId).toMutableList()
                    currentExercises.removeAll { it.id == exerciseId }
                    
                    val currentMap = _workoutExercises.value.toMutableMap()
                    currentMap[workoutId] = currentExercises
                    _workoutExercises.value = currentMap
                    
                    // Limpar cache de séries
                    setsCache.clear()
                    
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun getWorkoutExercises(workoutId: String): List<Exercise> {
        return _workoutExercises.value[workoutId] ?: emptyList()
    }
    
    // ========== SETS ==========
    
    fun loadWorkoutSets(workoutId: String) {
        viewModelScope.launch {
            try {
                hybridRepository.getSetsByWorkout(workoutId).collect { sets ->
                    val currentMap = _workoutSets.value.toMutableMap()
                    currentMap[workoutId] = sets
                    _workoutSets.value = currentMap
                    
                    // Limpar cache quando as séries são atualizadas
                    setsCache.clear()
                    
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun addSet(set: Set) {
        viewModelScope.launch {
            try {
                val setId = hybridRepository.addSet(set)
                if (setId != null) {
                    // Atualizar UI imediatamente
                    val currentSets = getWorkoutSets(set.workoutId).toMutableList()
                    val newSet = set.copy(id = setId)
                    currentSets.add(newSet)
                    
                    val currentMap = _workoutSets.value.toMutableMap()
                    currentMap[set.workoutId] = currentSets
                    _workoutSets.value = currentMap
                    
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun addSetToExercise(workoutId: String, exerciseId: String, weight: Double, reps: Int) {
        viewModelScope.launch {
            try {
                hybridRepository.addSetToExercise(workoutId, exerciseId, weight, reps)
                
                // Recarregar séries do treino
                loadWorkoutSets(workoutId)
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteSet(setId: String, workoutId: String) {
        viewModelScope.launch {
            try {
                val success = hybridRepository.deleteSet(setId)
                if (success) {
                    // Atualizar UI imediatamente
                    val currentSets = getWorkoutSets(workoutId).toMutableList()
                    currentSets.removeAll { it.id == setId }
                    
                    val currentMap = _workoutSets.value.toMutableMap()
                    currentMap[workoutId] = currentSets
                    _workoutSets.value = currentMap
                    
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun getWorkoutSets(workoutId: String): List<Set> {
        return _workoutSets.value[workoutId] ?: emptyList()
    }
    
    fun getSetsByExercise(workoutId: String, exerciseName: String): List<Set> {
        // Se mudou o workoutId, limpar cache
        if (lastWorkoutId != workoutId) {
            setsCache.clear()
            lastWorkoutId = workoutId
        }
        
        // Verificar se já temos as séries em cache
        val cacheKey = "$workoutId-$exerciseName"
        if (setsCache.containsKey(cacheKey)) {
            return setsCache[cacheKey] ?: emptyList()
        }
        
        val allSets = getWorkoutSets(workoutId)
        val filteredSets = allSets.filter { it.exerciseName == exerciseName }
            .sortedBy { it.createdAt }
        
        // Armazenar no cache
        setsCache[cacheKey] = filteredSets
        
        return filteredSets
    }
    
    // ========== CACHE MANAGEMENT ==========
    
    fun forceSyncFromFirebase() {
        viewModelScope.launch {
            try {
                hybridRepository.forceSyncFromFirebase()
                
                // Recarregar todos os dados
                loadInitialData()
                
            } catch (e: Exception) {
            }
        }
    }
    
    fun isCacheValid(): Boolean {
        return hybridRepository.isCacheValid()
    }
    
    fun clearCache() {
        hybridRepository.clearCache()
        _allExercises.value = emptyList()
        _allWorkouts.value = emptyList()
        _allWorkoutWeeks.value = emptyList()
        _workoutExercises.value = emptyMap()
        _workoutSets.value = emptyMap()
        setsCache.clear()
    }
}
