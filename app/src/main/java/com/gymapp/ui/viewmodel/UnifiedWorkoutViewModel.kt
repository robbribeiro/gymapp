package com.gymapp.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.data.firebase.FirebaseRepositoryOptimized
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.Set
import com.gymapp.data.firebase.WorkoutWeek
import com.gymapp.data.firebase.FirebaseCache
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ViewModel unificado e otimizado que implementa carregamento sob demanda
 * conforme solicitado pelo usuário - consulta o banco apenas quando necessário
 */
class UnifiedWorkoutViewModel(private val context: Context) : ViewModel() {
    
    
    private val firebaseRepository = FirebaseRepositoryOptimized()
    
    // ========== STATE FLOWS ==========
    
    private val _allExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val allExercises: StateFlow<List<Exercise>> = _allExercises.asStateFlow()
    
    private val _allWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val allWorkouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()
    
    // Usar cache global para semanas para melhor sincronização
    val allWorkoutWeeks: StateFlow<List<WorkoutWeek>> = FirebaseCache.workoutWeeks
    
    // Cache para exercícios por treino (carregado sob demanda)
    private val _workoutExercises = MutableStateFlow<Map<String, List<Exercise>>>(emptyMap())
    val workoutExercises: StateFlow<Map<String, List<Exercise>>> = _workoutExercises.asStateFlow()
    
    // Cache para séries por treino (carregado sob demanda)
    private val _workoutSets = MutableStateFlow<Map<String, List<Set>>>(emptyMap())
    val workoutSets: StateFlow<Map<String, List<Set>>> = _workoutSets.asStateFlow()
    
    // Estados de carregamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingExercises = MutableStateFlow(false)
    val isLoadingExercises: StateFlow<Boolean> = _isLoadingExercises.asStateFlow()
    
    private val _isLoadingWorkouts = MutableStateFlow(false)
    val isLoadingWorkouts: StateFlow<Boolean> = _isLoadingWorkouts.asStateFlow()
    
    private val _isLoadingWeeks = MutableStateFlow(false)
    val isLoadingWeeks: StateFlow<Boolean> = _isLoadingWeeks.asStateFlow()
    
    // Compatibilidade com telas antigas
    val exercises: StateFlow<List<Exercise>> = _allExercises.asStateFlow()
    val workouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()
    val workoutWeeks: StateFlow<List<WorkoutWeek>> = FirebaseCache.workoutWeeks
    
    // Placeholder para dados de progresso (não implementados no novo ViewModel)
    private val _exerciseProgress = MutableStateFlow<List<com.gymapp.data.firebase.ExerciseProgressData>>(emptyList())
    val exerciseProgress: StateFlow<List<com.gymapp.data.firebase.ExerciseProgressData>> = _exerciseProgress.asStateFlow()
    
    private val _weeklyProgress = MutableStateFlow<List<com.gymapp.data.firebase.WeeklyProgressData>>(emptyList())
    val weeklyProgress: StateFlow<List<com.gymapp.data.firebase.WeeklyProgressData>> = _weeklyProgress.asStateFlow()
    
    // Flags para controlar o que já foi carregado
    private var exercisesLoaded = false
    private var workoutsLoaded = false
    private var weeksLoaded = false
    
    // Cache local para evitar consultas repetidas
    private val setsCache = mutableMapOf<String, List<Set>>()
    private var lastWorkoutId = ""
    
    // ========== CARREGAMENTO SOB DEMANDA ==========
    
    /**
     * Carrega exercícios apenas quando necessário
     */
    fun loadExercisesIfNeeded() {
        if (exercisesLoaded) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoadingExercises.value = true }
            try {
                val startTime = System.currentTimeMillis()
                
                firebaseRepository.getAllExercises().collect { exercises ->
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    
                    withContext(Dispatchers.Main) {
                        _allExercises.value = exercises
                        exercisesLoaded = true
                    }
                    
                }
            } catch (e: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoadingExercises.value = false
                }
            }
        }
    }
    
    /**
     * Carrega treinos apenas quando necessário
     */
    fun loadWorkoutsIfNeeded() {
        if (workoutsLoaded) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoadingWorkouts.value = true }
            try {
                val startTime = System.currentTimeMillis()
                
                firebaseRepository.getAllWorkouts().collect { workouts ->
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    
                    withContext(Dispatchers.Main) {
                        _allWorkouts.value = workouts
                        workoutsLoaded = true
                    }
                    
                }
            } catch (e: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoadingWorkouts.value = false
                }
            }
        }
    }
    
    /**
     * Carrega semanas apenas quando necessário
     */
    fun loadWeeksIfNeeded() {
        if (weeksLoaded) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoadingWeeks.value = true }
            try {
                val startTime = System.currentTimeMillis()
                
                firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    
                    withContext(Dispatchers.Main) {
                        // Atualizar cache global
                        FirebaseCache.updateWorkoutWeeks(weeks)
                        weeksLoaded = true
                    }
                    
                }
            } catch (e: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoadingWeeks.value = false
                }
            }
        }
    }
    
    /**
     * Força o recarregamento das semanas do Firebase (ignora cache)
     */
    fun forceReloadWeeks() {
        weeksLoaded = false // Resetar flag para forçar recarregamento
        loadWeeksIfNeeded()
    }
    
    // ========== EXERCISES ==========
    
    fun addExercise(exercise: Exercise) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                val exerciseId = firebaseRepository.addExercise(exercise)
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                if (exerciseId != null) {
                    // Atualizar lista local imediatamente no thread principal
                    withContext(Dispatchers.Main) {
                        val updatedExercises = _allExercises.value.toMutableList()
                        updatedExercises.add(exercise.copy(id = exerciseId))
                        _allExercises.value = updatedExercises
                    }
                    
                } else {
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = firebaseRepository.deleteExercise(exerciseId)
                if (success) {
                    // Atualizar lista local imediatamente no thread principal
                    withContext(Dispatchers.Main) {
                        val updatedExercises = _allExercises.value.toMutableList()
                        updatedExercises.removeAll { it.id == exerciseId }
                        _allExercises.value = updatedExercises
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WORKOUTS ==========
    
    fun addWorkout(workout: Workout) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workoutId = firebaseRepository.addWorkout(workout)
                if (workoutId != null) {
                    // Atualizar lista local imediatamente no thread principal
                    withContext(Dispatchers.Main) {
                        val updatedWorkouts = _allWorkouts.value.toMutableList()
                        updatedWorkouts.add(workout.copy(id = workoutId))
                        _allWorkouts.value = updatedWorkouts
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    suspend fun deleteWorkout(workoutId: String): Boolean {
        return try {
            
            // 1. Deletar todas as séries do treino
            val sets = _workoutSets.value[workoutId] ?: emptyList()
            for (set in sets) {
                try {
                    val deleteResult = firebaseRepository.deleteSet(set.id)
                } catch (e: Exception) {
                }
            }
            
            // 2. Deletar todos os exercícios do treino
            val exercises = _workoutExercises.value[workoutId] ?: emptyList()
            for (exercise in exercises) {
                try {
                    val removeResult = firebaseRepository.removeExerciseFromWorkout(workoutId, exercise.id)
                } catch (e: Exception) {
                }
            }
            
            // 3. Remover o treino de todas as semanas
            val weeks = FirebaseCache.workoutWeeks.value
            for (week in weeks) {
                val weekHasWorkout = week.workouts.any { it.id == workoutId }
                if (weekHasWorkout) {
                    val removeSuccess = firebaseRepository.removeWorkoutFromWeek(week.id, workoutId)
                }
            }
            
            // 4. Deletar o treino em si
            val success = firebaseRepository.deleteWorkout(workoutId)
            
            if (success) {
                // Atualizar listas locais no thread principal
                withContext(Dispatchers.Main) {
                    // Remover treino da lista principal
                    val updatedWorkouts = _allWorkouts.value.toMutableList()
                    val beforeCount = updatedWorkouts.size
                    updatedWorkouts.removeAll { it.id == workoutId }
                    val afterCount = updatedWorkouts.size
                    _allWorkouts.value = updatedWorkouts
                    
                    // Remover exercícios do treino do cache
                    val updatedWorkoutExercises = _workoutExercises.value.toMutableMap()
                    updatedWorkoutExercises.remove(workoutId)
                    _workoutExercises.value = updatedWorkoutExercises
                    
                    // Remover séries do treino do cache
                    val updatedWorkoutSets = _workoutSets.value.toMutableMap()
                    updatedWorkoutSets.remove(workoutId)
                    _workoutSets.value = updatedWorkoutSets
                    
                    // Limpar cache de séries
                    setsCache.clear()
                }
                
                // Invalidar cache do repositório e forçar recarregamento das semanas
                firebaseRepository.invalidateWeeksCache()
                forceReloadWeeks()
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ========== WORKOUT EXERCISES (CARREGAMENTO SOB DEMANDA) ==========
    
    fun loadWorkoutExercises(workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                val exercises = firebaseRepository.getExercisesByWorkout(workoutId)
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                // Atualizar UI no thread principal
                withContext(Dispatchers.Main) {
                    val currentMap = _workoutExercises.value.toMutableMap()
                    currentMap[workoutId] = exercises
                    _workoutExercises.value = currentMap
                }
                
            } catch (e: Exception) {
            }
        }
    }
    
    fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                
                // Verificar se o treino existe no cache local
                val workoutExists = _allWorkouts.value.any { it.id == workoutId }
                
                if (!workoutExists) {
                    loadWorkoutsIfNeeded()
                    
                    // Aguardar um pouco para o carregamento
                    delay(1000)
                    
                    val workoutExistsAfterReload = _allWorkouts.value.any { it.id == workoutId }
                    
                    if (!workoutExistsAfterReload) {
                        return@launch
                    }
                }
                
                // Adicionar exercício ao treino no Firebase
                firebaseRepository.addExerciseToWorkout(workoutId, exerciseId)
                
                // Recarregar exercícios do treino imediatamente
                loadWorkoutExercises(workoutId)
                
                // Também atualizar o contador de exercícios no treino
                val currentWorkouts = _allWorkouts.value.toMutableList()
                val workoutIndex = currentWorkouts.indexOfFirst { it.id == workoutId }
                if (workoutIndex != -1) {
                    val updatedWorkout = currentWorkouts[workoutIndex].copy(
                        exerciseCount = currentWorkouts[workoutIndex].exerciseCount + 1
                    )
                    currentWorkouts[workoutIndex] = updatedWorkout
                    _allWorkouts.value = currentWorkouts
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.removeExerciseFromWorkout(workoutId, exerciseId)
                if (success) {
                    // Recarregar exercícios do treino imediatamente
                    loadWorkoutExercises(workoutId)
                    
                    // Atualizar contador de exercícios no treino
                    val currentWorkouts = _allWorkouts.value.toMutableList()
                    val workoutIndex = currentWorkouts.indexOfFirst { it.id == workoutId }
                    if (workoutIndex != -1) {
                        val updatedWorkout = currentWorkouts[workoutIndex].copy(
                            exerciseCount = maxOf(0, currentWorkouts[workoutIndex].exerciseCount - 1)
                        )
                        currentWorkouts[workoutIndex] = updatedWorkout
                        _allWorkouts.value = currentWorkouts
                    }
                    
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
    
    // ========== SETS (CARREGAMENTO SOB DEMANDA) ==========
    
    fun loadWorkoutSets(workoutId: String) {
        val cachedSets = _workoutSets.value[workoutId]
        if (cachedSets != null && cachedSets.isNotEmpty()) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                firebaseRepository.getSetsByWorkout(workoutId).collect { sets ->
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    
                    withContext(Dispatchers.Main) {
                        val currentMap = _workoutSets.value.toMutableMap()
                        currentMap[workoutId] = sets
                        _workoutSets.value = currentMap
                        
                        // Limpar cache quando as séries são atualizadas
                        setsCache.clear()
                    }
                    
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun addSet(set: Set) {
        viewModelScope.launch {
            try {
                val setId = firebaseRepository.addSet(set)
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
                firebaseRepository.addSetToExercise(workoutId, exerciseId, weight, reps)
                
                // Recarregar séries do treino
                loadWorkoutSets(workoutId)
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteSet(setId: String, workoutId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.deleteSet(setId)
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
    
    /**
     * Obtém as últimas séries de um exercício específico (últimas 3)
     */
    fun getLastSetsForExercise(exerciseName: String): List<Set> {
        // Se não temos séries carregadas, carregar todas as séries primeiro
        if (_workoutSets.value.isEmpty()) {
            loadAllWorkoutSets()
        }
        
        // Buscar todas as séries de todos os treinos para este exercício
        val allSets = _workoutSets.value.values.flatten()
            .filter { it.exerciseName == exerciseName }
            .sortedBy { it.createdAt } // Ordenar por data crescente (ordem de criação)
            .takeLast(3) // Pegar as 3 mais recentes mantendo a ordem de criação
        
        return allSets
    }
    
    /**
     * Carrega séries de todos os treinos para exibição nos cards de exercícios
     */
    fun loadAllWorkoutSets() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Carregar séries de todos os treinos
                val workouts = _allWorkouts.value
                workouts.forEach { workout ->
                    loadWorkoutSets(workout.id)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WORKOUT WEEKS ==========
    
    fun addWorkoutWeek(weekName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val calendar = java.util.Calendar.getInstance()
                val weekStart = calendar.apply {
                    set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time

                val weekEnd = java.util.Calendar.getInstance().apply {
                    time = weekStart
                    add(java.util.Calendar.DAY_OF_WEEK, 6)
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }.time

                val week = WorkoutWeek(
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    weekName = weekName,
                    workouts = emptyList(),
                    totalVolume = 0.0,
                    userId = "",
                    createdAt = System.currentTimeMillis()
                )

                val weekId = firebaseRepository.addWorkoutWeek(week)
                if (weekId != null) {
                    // Atualizar cache global imediatamente no thread principal
                    withContext(Dispatchers.Main) {
                        val updatedWeeks = FirebaseCache.workoutWeeks.value.toMutableList()
                        updatedWeeks.add(week.copy(id = weekId))
                        FirebaseCache.updateWorkoutWeeks(updatedWeeks)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteWorkoutWeek(weekId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = firebaseRepository.deleteWorkoutWeek(weekId)
                if (success) {
                    // Atualizar cache global imediatamente no thread principal
                    withContext(Dispatchers.Main) {
                        val updatedWeeks = FirebaseCache.workoutWeeks.value.toMutableList()
                        updatedWeeks.removeAll { it.id == weekId }
                        FirebaseCache.updateWorkoutWeeks(updatedWeeks)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun addWorkoutToWeek(weekId: String, workoutName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workoutId = firebaseRepository.addWorkoutToWeek(weekId, workoutName)
                if (workoutId != null) {
                    // Forçar recarregamento das semanas para atualizar a UI
                    forceReloadWeeks()
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteWorkoutFromWeek(weekId: String, workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                
                // Deletar o treino e todos os seus dados associados
                val success = deleteWorkout(workoutId)
                
                if (success) {
                    // Forçar recarregamento das semanas para atualizar a UI
                    forceReloadWeeks()
                    
                } else {
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== COMPATIBILIDADE COM TELAS ANTIGAS ==========
    
    fun getExercisesByWorkout(workoutId: String): kotlinx.coroutines.flow.Flow<List<Exercise>> {
        return kotlinx.coroutines.flow.flow {
            val exercises = firebaseRepository.getExercisesByWorkout(workoutId)
            emit(exercises)
        }
    }
    
    fun updateWorkout(workoutId: String, name: String, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseRepository.updateWorkout(workoutId, name, date)
                // Atualizar cache local se necessário no thread principal
                withContext(Dispatchers.Main) {
                    val updatedWorkouts = _allWorkouts.value.toMutableList()
                    val index = updatedWorkouts.indexOfFirst { it.id == workoutId }
                    if (index != -1) {
                        updatedWorkouts[index] = updatedWorkouts[index].copy(name = name, date = date)
                        _allWorkouts.value = updatedWorkouts
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Força o recarregamento de todos os dados (apenas quando necessário)
     */
    fun forceRefreshAll() {
        exercisesLoaded = false
        workoutsLoaded = false
        weeksLoaded = false
        
        loadExercisesIfNeeded()
        loadWorkoutsIfNeeded()
        loadWeeksIfNeeded()
    }
    
    /**
     * Mostra resumo do estado atual do cache
     */
    fun showCacheStatus() {
    }
    
    /**
     * Limpa todos os caches
     */
    fun clearAllCaches() {
        _allExercises.value = emptyList()
        _allWorkouts.value = emptyList()
        _workoutExercises.value = emptyMap()
        _workoutSets.value = emptyMap()
        setsCache.clear()
        
        exercisesLoaded = false
        workoutsLoaded = false
        weeksLoaded = false
    }
}
