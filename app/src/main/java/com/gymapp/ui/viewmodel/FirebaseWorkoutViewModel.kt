package com.gymapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.data.firebase.FirebaseRepositoryOptimized
import com.gymapp.data.firebase.FirebaseCache
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.Set
import com.gymapp.data.firebase.WorkoutWeek
import com.gymapp.data.firebase.ExerciseProgressData
import com.gymapp.data.firebase.WeeklyProgressData
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseWorkoutViewModel : ViewModel() {
    
    private val firebaseRepository = FirebaseRepositoryOptimized()
    
    // Usar cache global para melhor performance
    val exercises: StateFlow<List<Exercise>> = FirebaseCache.exercises
    val workouts: StateFlow<List<Workout>> = FirebaseCache.workouts
    val workoutWeeks: StateFlow<List<WorkoutWeek>> = FirebaseCache.workoutWeeks
    val exerciseProgress: StateFlow<List<ExerciseProgressData>> = FirebaseCache.exerciseProgress
    val weeklyProgress: StateFlow<List<WeeklyProgressData>> = FirebaseCache.weeklyProgress
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // StateFlow para exercícios de um treino específico
    private val _workoutExercises = MutableStateFlow<Map<String, List<Exercise>>>(emptyMap())
    val workoutExercises: StateFlow<Map<String, List<Exercise>>> = _workoutExercises.asStateFlow()
    
    // StateFlow para séries de um treino específico
    private val _workoutSets = MutableStateFlow<Map<String, List<Set>>>(emptyMap())
    val workoutSets: StateFlow<Map<String, List<Set>>> = _workoutSets.asStateFlow()
    
    // ========== INITIALIZATION ==========
    
    init {
        loadDataIfNeeded()
    }
    
    private fun loadDataIfNeeded() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                
                // Verificar se cache já está válido
                val needsExercises = !FirebaseCache.isExercisesCacheValid()
                val needsWorkouts = !FirebaseCache.isWorkoutsCacheValid()
                val needsWeeks = !FirebaseCache.isWeeksCacheValid()
                val needsProgress = !FirebaseCache.isProgressCacheValid()
                val needsWeeklyProgress = !FirebaseCache.isWeeklyProgressCacheValid()
                
                if (needsExercises || needsWorkouts || needsWeeks || needsProgress || needsWeeklyProgress) {
                    
                    // Aguardar um pouco para a autenticação ser processada
                    delay(500) // Reduzido ainda mais
                    
                    // Testar autenticação primeiro
                    val authTest = com.gymapp.data.firebase.FirebaseTest.testAuth()
                    
                    if (authTest) {
                        // Carregar apenas dados necessários em paralelo
                        coroutineScope {
                            val tasks = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
                            
                            if (needsExercises) {
                                tasks.add(async { loadExercises() })
                            }
                            if (needsWorkouts) {
                                tasks.add(async { loadWorkouts() })
                            }
                            if (needsWeeks) {
                                tasks.add(async { loadWorkoutWeeks() })
                            }
                            if (needsProgress) {
                                tasks.add(async { loadExerciseProgress() })
                            }
                            if (needsWeeklyProgress) {
                                tasks.add(async { loadWeeklyProgress() })
                            }
                            
                            // Aguardar apenas as tarefas necessárias
                            tasks.forEach { it.await() }
                        }
                    } else {
                    }
                } else {
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ========== EXERCISES ==========
    
    private fun loadExercises() {
        viewModelScope.launch {
            try {
                firebaseRepository.getAllExercises().collect { exerciseList ->
                    FirebaseCache.updateExercises(exerciseList)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                val exerciseId = firebaseRepository.addExercise(exercise)
                if (exerciseId != null) {
                    val newExercise = exercise.copy(id = exerciseId)
                    FirebaseCache.addExercise(newExercise)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.deleteExercise(exerciseId)
                if (success) {
                    FirebaseCache.removeExercise(exerciseId)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WORKOUTS ==========
    
    private fun loadWorkouts() {
        viewModelScope.launch {
            try {
                firebaseRepository.getAllWorkouts().collect { workoutList ->
                    FirebaseCache.updateWorkouts(workoutList)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun addWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                val workoutId = firebaseRepository.addWorkout(workout)
                if (workoutId != null) {
                    val newWorkout = workout.copy(id = workoutId)
                    FirebaseCache.addWorkout(newWorkout)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.deleteWorkout(workoutId)
                if (success) {
                    FirebaseCache.removeWorkout(workoutId)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun updateWorkout(workoutId: String, name: String, date: Long) {
        viewModelScope.launch {
            try {
                firebaseRepository.updateWorkout(workoutId, name, date)
                // Atualizar cache local se necessário
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WORKOUT WEEKS ==========
    
    private fun loadWorkoutWeeks() {
        viewModelScope.launch {
            try {
                
                // Usar a mesma lógica dos exercícios
                firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
                    FirebaseCache.updateWorkoutWeeks(weeks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ========== EXERCISE PROGRESS ==========
    
    private fun loadExerciseProgress() {
        viewModelScope.launch {
            try {
                firebaseRepository.getExerciseProgress().collect { progress ->
                    FirebaseCache.updateExerciseProgress(progress)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== WEEKLY PROGRESS ==========
    
    private fun loadWeeklyProgress() {
        viewModelScope.launch {
            try {
                firebaseRepository.getWeeklyProgress().collect { progress ->
                    FirebaseCache.updateWeeklyProgress(progress)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== SETS ==========
    
    fun addSet(set: Set) {
        viewModelScope.launch {
            try {
                
                if (set.exerciseId.isNotEmpty()) {
                    try {
                        // Usar o método específico para adicionar série ao exercício
                        firebaseRepository.addSetToExercise(set.workoutId, set.exerciseId, set.weight, set.reps)
                        
                        // Atualizar UI imediatamente
                        val currentSets = getWorkoutSets(set.workoutId).toMutableList()
                        val newSet = set.copy(id = "temp-${System.currentTimeMillis()}")
                        currentSets.add(newSet)
                        
                        val currentMap = _workoutSets.value.toMutableMap()
                        currentMap[set.workoutId] = currentSets
                        _workoutSets.value = currentMap
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback para o método genérico
                        val setId = firebaseRepository.addSet(set)
                        if (setId != null) {
                        } else {
                            throw Exception("Falha ao adicionar série - ambos os métodos falharam")
                        }
                    }
                } else {
                    // Fallback para o método genérico
                    val setId = firebaseRepository.addSet(set)
                    if (setId != null) {
                    } else {
                        throw Exception("Falha ao adicionar série - método genérico falhou")
                    }
                }
                
                // Atualizar UI imediatamente sem delay
                val currentSets = getWorkoutSets(set.workoutId).toMutableList()
                val newSet = set.copy(id = "temp-${System.currentTimeMillis()}")
                currentSets.add(newSet)
                
                val currentMap = _workoutSets.value.toMutableMap()
                currentMap[set.workoutId] = currentSets
                _workoutSets.value = currentMap
                
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun deleteSet(setId: String, workoutId: String) {
        viewModelScope.launch {
            try {
                // Atualizar UI imediatamente
                val currentSets = getWorkoutSets(workoutId).toMutableList()
                currentSets.removeAll { it.id == setId }
                
                val currentMap = _workoutSets.value.toMutableMap()
                currentMap[workoutId] = currentSets
                _workoutSets.value = currentMap
                
                
                // Deletar do Firebase em background
                val success = firebaseRepository.deleteSet(setId)
                if (success) {
                } else {
                    // Se falhou no Firebase, reverter a UI
                    loadWorkoutSets(workoutId)
                }
            } catch (e: Exception) {
                // Em caso de erro, recarregar do Firebase
                loadWorkoutSets(workoutId)
            }
        }
    }
    
    fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                
                // Atualizar UI imediatamente
                val currentExercises = getWorkoutExercises(workoutId).toMutableList()
                
                val beforeSize = currentExercises.size
                currentExercises.removeAll { it.id == exerciseId }
                val afterSize = currentExercises.size
                
                
                val currentMap = _workoutExercises.value.toMutableMap()
                currentMap[workoutId] = currentExercises
                _workoutExercises.value = currentMap
                
                // Limpar cache de séries
                setsCache.clear()
                
                
                // Remover do Firebase e aguardar resultado
                val success = firebaseRepository.removeExerciseFromWorkout(workoutId, exerciseId)
                if (success) {
                } else {
                    // Reverter UI se Firebase falhar
                    loadWorkoutExercises(workoutId)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Em caso de erro, recarregar do Firebase
                loadWorkoutExercises(workoutId)
            }
        }
    }
    
    suspend fun getSetsByWorkout(workoutId: String): Flow<List<Set>> {
        return firebaseRepository.getSetsByWorkout(workoutId)
    }
    
    // ========== WORKOUT WEEKS ==========
    
    fun addWorkoutWeek(weekName: String) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val weekStart = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val weekEnd = Calendar.getInstance().apply {
                    time = weekStart
                    add(Calendar.DAY_OF_WEEK, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
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
                    // Add to cache immediately
                    val weekWithId = week.copy(id = weekId)
                    FirebaseCache.addWorkoutWeek(weekWithId)
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Função para forçar recarregamento das semanas (para debug)
    fun forceReloadWeeks() {
        viewModelScope.launch {
            try {
                loadWorkoutWeeks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Função para deletar semana
    fun deleteWorkoutWeek(weekId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.deleteWorkoutWeek(weekId)
                if (success) {
                    FirebaseCache.removeWorkoutWeek(weekId)
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Função para adicionar exercício ao treino
    fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                firebaseRepository.addExerciseToWorkout(workoutId, exerciseId)
                
                // Aguardar um pouco para garantir que o Firebase processou a atualização
                kotlinx.coroutines.delay(500)
                
                // Recarregar exercícios do treino após adicionar
                loadWorkoutExercises(workoutId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getExercisesByWorkout(workoutId: String): Flow<List<Exercise>> = flow {
        try {
            val exercises = firebaseRepository.getExercisesByWorkout(workoutId)
            emit(exercises)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun loadWorkoutExercises(workoutId: String) {
        viewModelScope.launch {
            try {
                
                val exercises = firebaseRepository.getExercisesByWorkout(workoutId)
                
                // Atualizar o StateFlow de forma mais robusta
                val currentMap = _workoutExercises.value.toMutableMap()
                currentMap[workoutId] = exercises
                _workoutExercises.value = currentMap
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getWorkoutExercises(workoutId: String): List<Exercise> {
        return _workoutExercises.value[workoutId] ?: emptyList()
    }
    
    // ========== WORKOUT SETS ==========
    
    fun loadWorkoutSets(workoutId: String) {
        viewModelScope.launch {
            try {
                
                firebaseRepository.getSetsByWorkout(workoutId).collect { setsList ->
                    
                    // Atualizar o StateFlow
                    val currentMap = _workoutSets.value.toMutableMap()
                    
                    currentMap[workoutId] = setsList
                    _workoutSets.value = currentMap

                    // Limpar cache quando as séries são atualizadas
                    setsCache.clear()

                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun getWorkoutSets(workoutId: String): List<Set> {
        return _workoutSets.value[workoutId] ?: emptyList()
    }
    
    // Cache para evitar chamadas repetidas
    private val setsCache = mutableMapOf<String, List<Set>>()
    private var lastWorkoutId = ""

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
    
    // Função para adicionar treino à semana
    fun addWorkoutToWeek(weekId: String, workoutName: String) {
        viewModelScope.launch {
            try {
                val workoutId = firebaseRepository.addWorkoutToWeek(weekId, workoutName)
                if (workoutId != null) {
                    // Forçar recarregamento das semanas para atualizar a UI
                    loadWorkoutWeeks()
                    
                    // Log adicional para debug
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    fun testFirebaseConnection() {
        viewModelScope.launch {
            try {

                // Verificar autenticação primeiro
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

                if (auth.currentUser == null) {

                    // Tentar autenticação anônima
                    try {
                        auth.signInAnonymously().await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@launch
                    }
                }

                // Testar conexão direta com Firestore
                val firebaseTest = com.gymapp.data.firebase.FirebaseTest.testFirebaseConnection()

                if (firebaseTest) {
                    
                    // Testar adição de exercício
                    val testExercise = Exercise(
                        id = "",
                        name = "Teste de Conexão",
                        category = "teste",
                        muscleGroups = listOf("teste"),
                        lastWeight = 0.0,
                        lastReps = 0,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val exerciseId = firebaseRepository.addExercise(testExercise)
                    if (exerciseId != null) {
                        
                        // Remover exercício de teste
                        firebaseRepository.deleteExercise(exerciseId)
                    } else {
                    }
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun refreshData() {
        FirebaseCache.clearAllCache()
        loadDataIfNeeded()
    }
}