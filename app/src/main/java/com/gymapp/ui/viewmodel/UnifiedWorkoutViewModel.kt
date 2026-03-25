package com.gymapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.data.firebase.FirebaseRepositoryOptimized
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.Set
import com.gymapp.data.firebase.WorkoutWeek
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class UnifiedWorkoutViewModel(private val context: Context) : ViewModel() {

    private val firebaseRepository = FirebaseRepositoryOptimized()

    // ========== STATE FLOWS ==========

    private val _allExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val allExercises: StateFlow<List<Exercise>> = _allExercises.asStateFlow()

    private val _allWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val allWorkouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()

    // CORRIGIDO: cache local no ViewModel, sem singleton FirebaseCache
    private val _allWorkoutWeeks = MutableStateFlow<List<WorkoutWeek>>(emptyList())
    val allWorkoutWeeks: StateFlow<List<WorkoutWeek>> = _allWorkoutWeeks.asStateFlow()

    private val _workoutExercises = MutableStateFlow<Map<String, List<Exercise>>>(emptyMap())
    val workoutExercises: StateFlow<Map<String, List<Exercise>>> = _workoutExercises.asStateFlow()

    private val _workoutSets = MutableStateFlow<Map<String, List<Set>>>(emptyMap())
    val workoutSets: StateFlow<Map<String, List<Set>>> = _workoutSets.asStateFlow()

    // Cache flat de TODAS as séries — usado pela aba Exercícios
    private val _allSets = MutableStateFlow<List<Set>>(emptyList())
    val allSets: StateFlow<List<Set>> = _allSets.asStateFlow()
    private var allSetsLoaded = false

    // ID real da última série salva — usado para animação de highlight na UI
    private val _lastAddedSetId = MutableStateFlow<String?>(null)
    val lastAddedSetId: StateFlow<String?> = _lastAddedSetId.asStateFlow()

    // Loading states
    private val _isLoadingExercises = MutableStateFlow(false)
    val isLoadingExercises: StateFlow<Boolean> = _isLoadingExercises.asStateFlow()

    private val _isLoadingWorkouts = MutableStateFlow(false)
    val isLoadingWorkouts: StateFlow<Boolean> = _isLoadingWorkouts.asStateFlow()

    private val _isLoadingWeeks = MutableStateFlow(false)
    val isLoadingWeeks: StateFlow<Boolean> = _isLoadingWeeks.asStateFlow()

    // NOVO: canal de erros para exibir Snackbar na UI
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Compatibilidade com telas existentes
    val exercises: StateFlow<List<Exercise>> = _allExercises.asStateFlow()
    val workouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()
    val workoutWeeks: StateFlow<List<WorkoutWeek>> = _allWorkoutWeeks.asStateFlow()

    private val _exerciseProgress = MutableStateFlow<List<com.gymapp.data.firebase.ExerciseProgressData>>(emptyList())
    val exerciseProgress: StateFlow<List<com.gymapp.data.firebase.ExerciseProgressData>> = _exerciseProgress.asStateFlow()

    private val _weeklyProgress = MutableStateFlow<List<com.gymapp.data.firebase.WeeklyProgressData>>(emptyList())
    val weeklyProgress: StateFlow<List<com.gymapp.data.firebase.WeeklyProgressData>> = _weeklyProgress.asStateFlow()

    // Flags de carregamento
    private var exercisesLoaded = false
    private var workoutsLoaded = false
    private var weeksLoaded = false

    // Cache local de séries para ExercisesScreen
    private val setsCache = mutableMapOf<String, List<Set>>()
    private var lastWorkoutId = ""

    // ========== ERRO ==========

    fun clearError() {
        _errorMessage.value = null
    }

    private fun emitError(message: String) {
        _errorMessage.value = message
    }

    // ========== CARREGAMENTO SOB DEMANDA ==========

    fun loadExercisesIfNeeded() {
        if (exercisesLoaded) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoadingExercises.value = true }
            try {
                firebaseRepository.getAllExercises().collect { exercises ->
                    withContext(Dispatchers.Main) {
                        _allExercises.value = exercises
                        exercisesLoaded = true
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao carregar exercícios. Verifique sua conexão.")
            } finally {
                withContext(Dispatchers.Main) { _isLoadingExercises.value = false }
            }
        }
    }

    fun loadWorkoutsIfNeeded() {
        if (workoutsLoaded) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoadingWorkouts.value = true }
            try {
                firebaseRepository.getAllWorkouts().collect { workouts ->
                    withContext(Dispatchers.Main) {
                        _allWorkouts.value = workouts
                        workoutsLoaded = true
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao carregar treinos. Verifique sua conexão.")
            } finally {
                withContext(Dispatchers.Main) { _isLoadingWorkouts.value = false }
            }
        }
    }

    fun loadWeeksIfNeeded() {
        if (weeksLoaded) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoadingWeeks.value = true }
            try {
                firebaseRepository.getAllWorkoutWeeks().collect { weeks ->
                    withContext(Dispatchers.Main) {
                        _allWorkoutWeeks.value = weeks
                        weeksLoaded = true
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao carregar semanas. Verifique sua conexão.")
            } finally {
                withContext(Dispatchers.Main) { _isLoadingWeeks.value = false }
            }
        }
    }

    fun forceReloadWeeks() {
        weeksLoaded = false
        loadWeeksIfNeeded()
    }

    // ========== EXERCISES ==========

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exerciseId = firebaseRepository.addExercise(exercise)
                if (exerciseId != null) {
                    withContext(Dispatchers.Main) {
                        _allExercises.value = _allExercises.value + exercise.copy(id = exerciseId)
                    }
                } else {
                    emitError("Não foi possível adicionar o exercício.")
                }
            } catch (e: Exception) {
                emitError("Erro ao adicionar exercício: ${e.message}")
            }
        }
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = firebaseRepository.deleteExercise(exerciseId)
                if (success) {
                    withContext(Dispatchers.Main) {
                        _allExercises.value = _allExercises.value.filter { it.id != exerciseId }
                    }
                } else {
                    emitError("Não foi possível apagar o exercício.")
                }
            } catch (e: Exception) {
                emitError("Erro ao apagar exercício: ${e.message}")
            }
        }
    }

    // ========== WORKOUTS ==========

    fun addWorkout(workout: Workout) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workoutId = firebaseRepository.addWorkout(workout)
                if (workoutId != null) {
                    withContext(Dispatchers.Main) {
                        _allWorkouts.value = _allWorkouts.value + workout.copy(id = workoutId)
                    }
                } else {
                    emitError("Não foi possível criar o treino.")
                }
            } catch (e: Exception) {
                emitError("Erro ao criar treino: ${e.message}")
            }
        }
    }

    suspend fun deleteWorkout(workoutId: String): Boolean {
        return try {
            // Deletar séries
            val sets = _workoutSets.value[workoutId] ?: emptyList()
            sets.forEach { firebaseRepository.deleteSet(it.id) }

            // Remover exercícios do treino
            val exercises = _workoutExercises.value[workoutId] ?: emptyList()
            exercises.forEach { firebaseRepository.removeExerciseFromWorkout(workoutId, it.id) }

            // Remover treino de semanas
            _allWorkoutWeeks.value.forEach { week ->
                if (week.workouts.any { it.id == workoutId }) {
                    firebaseRepository.removeWorkoutFromWeek(week.id, workoutId)
                }
            }

            val success = firebaseRepository.deleteWorkout(workoutId)
            if (success) {
                withContext(Dispatchers.Main) {
                    _allWorkouts.value = _allWorkouts.value.filter { it.id != workoutId }
                    _workoutExercises.value = _workoutExercises.value.toMutableMap()
                        .also { it.remove(workoutId) }
                    _workoutSets.value = _workoutSets.value.toMutableMap()
                        .also { it.remove(workoutId) }
                    setsCache.clear()
                }
                firebaseRepository.invalidateWeeksCache()
                forceReloadWeeks()
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            emitError("Erro ao apagar treino: ${e.message}")
            false
        }
    }

    fun updateWorkout(workoutId: String, name: String, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseRepository.updateWorkout(workoutId, name, date)
                withContext(Dispatchers.Main) {
                    _allWorkouts.value = _allWorkouts.value.map {
                        if (it.id == workoutId) it.copy(name = name, date = date) else it
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao editar treino: ${e.message}")
            }
        }
    }

    fun toggleWorkoutCompleted(workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Busca o treino nas semanas (fonte principal de dados)
                val workout = _allWorkoutWeeks.value
                    .flatMap { it.workouts }
                    .find { it.id == workoutId }
                    ?: _allWorkouts.value.find { it.id == workoutId }
                    ?: run {
                        emitError("Treino não encontrado.")
                        return@launch
                    }

                val newCompleted = !workout.isCompleted
                firebaseRepository.updateWorkoutCompleted(workoutId, newCompleted)

                withContext(Dispatchers.Main) {
                    // Atualiza _allWorkoutWeeks — fonte que a UI observa
                    _allWorkoutWeeks.value = _allWorkoutWeeks.value.map { week ->
                        week.copy(workouts = week.workouts.map { w ->
                            if (w.id == workoutId) w.copy(isCompleted = newCompleted) else w
                        })
                    }
                    // Atualiza _allWorkouts se estiver populado
                    if (_allWorkouts.value.isNotEmpty()) {
                        _allWorkouts.value = _allWorkouts.value.map {
                            if (it.id == workoutId) it.copy(isCompleted = newCompleted) else it
                        }
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao atualizar treino: ${e.message}")
            }
        }
    }

    fun copyWorkoutToWeek(workoutId: String, targetWeekId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workout = _allWorkouts.value.find { it.id == workoutId }
                    ?: _allWorkoutWeeks.value.flatMap { it.workouts }
                        .find { it.id == workoutId }
                    ?: return@launch

                // Criar novo treino com mesmo nome na semana destino
                val newWorkoutId = firebaseRepository.addWorkoutToWeek(
                    targetWeekId,
                    workout.name
                ) ?: return@launch

                // Copiar exercícios do treino original
                val exercises = firebaseRepository.getExercisesByWorkout(workoutId)
                exercises.forEach { exercise ->
                    try {
                        firebaseRepository.addExerciseToWorkout(newWorkoutId, exercise.id)
                    } catch (e: Exception) { /* continua se um falhar */ }
                }

                withContext(Dispatchers.Main) {
                    forceReloadWeeks()
                }
            } catch (e: Exception) {
                emitError("Erro ao copiar treino: ${e.message}")
            }
        }
    }

    // Busca todas as séries de um exercício pelo nome — usado no histórico
    fun getSetHistoryForExercise(exerciseName: String): Flow<List<Set>> = flow {
        try {
            firebaseRepository.getAllSets().collect { sets ->
                emit(
                    sets.filter { it.exerciseName == exerciseName }
                        .sortedBy { it.createdAt }
                )
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // ========== WORKOUT EXERCISES ==========

    fun loadWorkoutExercises(workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exercises = firebaseRepository.getExercisesByWorkout(workoutId)
                withContext(Dispatchers.Main) {
                    _workoutExercises.value = _workoutExercises.value.toMutableMap()
                        .also { it[workoutId] = exercises }
                }
            } catch (e: Exception) {
                emitError("Erro ao carregar exercícios do treino.")
            }
        }
    }

    fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                firebaseRepository.addExerciseToWorkout(workoutId, exerciseId)
                loadWorkoutExercises(workoutId)
                // Atualiza contador
                withContext(Dispatchers.Main) {
                    _allWorkouts.value = _allWorkouts.value.map {
                        if (it.id == workoutId) it.copy(exerciseCount = it.exerciseCount + 1) else it
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao adicionar exercício ao treino: ${e.message}")
            }
        }
    }

    fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.removeExerciseFromWorkout(workoutId, exerciseId)
                if (success) {
                    loadWorkoutExercises(workoutId)
                    withContext(Dispatchers.Main) {
                        _allWorkouts.value = _allWorkouts.value.map {
                            if (it.id == workoutId) it.copy(
                                exerciseCount = maxOf(0, it.exerciseCount - 1)
                            ) else it
                        }
                        setsCache.clear()
                    }
                } else {
                    emitError("Não foi possível remover o exercício.")
                }
            } catch (e: Exception) {
                emitError("Erro ao remover exercício: ${e.message}")
            }
        }
    }

    fun getWorkoutExercises(workoutId: String): List<Exercise> =
        _workoutExercises.value[workoutId] ?: emptyList()

    // ========== SETS ==========

    // CORRIGIDO: sempre recarrega (sem guard de cache que impedia atualização)
    fun loadWorkoutSets(workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseRepository.getSetsByWorkout(workoutId).collect { sets ->
                    withContext(Dispatchers.Main) {
                        _workoutSets.value = _workoutSets.value.toMutableMap()
                            .also { it[workoutId] = sets }
                        setsCache.clear()
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao carregar séries.")
            }
        }
    }

    fun addSet(set: Set) {
        viewModelScope.launch {
            try {
                val setId = firebaseRepository.addSet(set)
                if (setId != null) {
                    val newSet = set.copy(id = setId)
                    withContext(Dispatchers.Main) {
                        // Atualiza _workoutSets (usado na tela de detalhes do treino)
                        val current = (_workoutSets.value[set.workoutId] ?: emptyList()).toMutableList()
                        current.add(newSet)
                        _workoutSets.value = _workoutSets.value.toMutableMap()
                            .also { it[set.workoutId] = current }
                        // Atualiza _allSets imediatamente — reflete na aba Exercícios sem recarregar
                        addSetToAllSetsCache(newSet)
                        // Emite o ID real para a UI acionar a animação de highlight
                        _lastAddedSetId.value = setId
                    }
                } else {
                    emitError("Não foi possível adicionar a série.")
                }
            } catch (e: Exception) {
                emitError("Erro ao adicionar série: ${e.message}")
            }
        }
    }

    fun clearLastAddedSetId() {
        _lastAddedSetId.value = null
    }

    fun addSetToExercise(workoutId: String, exerciseId: String, weight: Double, reps: Int) {
        viewModelScope.launch {
            try {
                firebaseRepository.addSetToExercise(workoutId, exerciseId, weight, reps)
                loadWorkoutSets(workoutId)
            } catch (e: Exception) {
                emitError("Erro ao adicionar série: ${e.message}")
            }
        }
    }

    fun deleteSet(setId: String, workoutId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseRepository.deleteSet(setId)
                if (success) {
                    withContext(Dispatchers.Main) {
                        val current = (_workoutSets.value[workoutId] ?: emptyList())
                            .filter { it.id != setId }
                        _workoutSets.value = _workoutSets.value.toMutableMap()
                            .also { it[workoutId] = current }
                        setsCache.clear()
                    }
                } else {
                    // Recarrega para garantir consistência
                    loadWorkoutSets(workoutId)
                    emitError("Não foi possível apagar a série.")
                }
            } catch (e: Exception) {
                loadWorkoutSets(workoutId)
                emitError("Erro ao apagar série: ${e.message}")
            }
        }
    }

    fun getWorkoutSets(workoutId: String): List<Set> =
        _workoutSets.value[workoutId] ?: emptyList()

    fun getSetsByExercise(workoutId: String, exerciseName: String): List<Set> {
        if (lastWorkoutId != workoutId) {
            setsCache.clear()
            lastWorkoutId = workoutId
        }
        val cacheKey = "$workoutId-$exerciseName"
        return setsCache.getOrPut(cacheKey) {
            (_workoutSets.value[workoutId] ?: emptyList())
                .filter { it.exerciseName == exerciseName }
                .sortedBy { it.createdAt }
        }
    }

    fun getLastSetsForExercise(exerciseName: String): List<Set> {
        return _allSets.value
            .filter { it.exerciseName == exerciseName }
            .sortedBy { it.createdAt }
            .takeLast(3)
    }

    /**
     * Carrega TODAS as séries do usuário em uma única query no Firestore.
     * Chamado automaticamente pela aba Exercícios ao abrir.
     * Evita o problema de séries não aparecerem após reiniciar o app.
     */
    fun loadAllWorkoutSets() {
        // Sem guard: sempre carrega do Firebase ao abrir a aba Exercícios.
        // Atualizações em tempo real são feitas por addSetToAllSetsCache.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseRepository.getAllSets().collect { sets ->
                    withContext(Dispatchers.Main) {
                        _allSets.value = sets
                        // Também popula _workoutSets por workoutId para manter compatibilidade
                        val byWorkout = sets.groupBy { it.workoutId }
                        _workoutSets.value = _workoutSets.value.toMutableMap().also {
                            it.putAll(byWorkout)
                        }
                        allSetsLoaded = true
                        setsCache.clear()
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao carregar séries: ${e.message}")
            }
        }
    }

    /**
     * Chamado após adicionar uma série para atualizar _allSets imediatamente,
     * sem precisar recarregar do Firebase.
     */
    private fun addSetToAllSetsCache(set: Set) {
        _allSets.value = _allSets.value + set
        setsCache.clear()
    }

    // ========== WORKOUT WEEKS ==========

    fun addWorkoutWeek(weekName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val weekStart = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.time
                val weekEnd = Calendar.getInstance().apply {
                    time = weekStart
                    add(Calendar.DAY_OF_WEEK, 6)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.time

                val week = WorkoutWeek(
                    weekStart = weekStart, weekEnd = weekEnd,
                    weekName = weekName, workouts = emptyList(),
                    createdAt = System.currentTimeMillis()
                )
                val weekId = firebaseRepository.addWorkoutWeek(week)
                if (weekId != null) {
                    withContext(Dispatchers.Main) {
                        _allWorkoutWeeks.value = _allWorkoutWeeks.value + week.copy(id = weekId)
                    }
                } else {
                    emitError("Não foi possível criar a semana.")
                }
            } catch (e: Exception) {
                emitError("Erro ao criar semana: ${e.message}")
            }
        }
    }

    fun renameWeek(weekId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseRepository.renameWeek(weekId, newName)
                withContext(Dispatchers.Main) {
                    _allWorkoutWeeks.value = _allWorkoutWeeks.value.map {
                        if (it.id == weekId) it.copy(weekName = newName) else it
                    }
                }
            } catch (e: Exception) {
                emitError("Erro ao renomear semana: ${e.message}")
            }
        }
    }

    fun deleteWorkoutWeek(weekId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = firebaseRepository.deleteWorkoutWeek(weekId)
                if (success) {
                    withContext(Dispatchers.Main) {
                        _allWorkoutWeeks.value = _allWorkoutWeeks.value.filter { it.id != weekId }
                    }
                } else {
                    emitError("Não foi possível apagar a semana.")
                }
            } catch (e: Exception) {
                emitError("Erro ao apagar semana: ${e.message}")
            }
        }
    }

    fun addWorkoutToWeek(weekId: String, workoutName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workoutId = firebaseRepository.addWorkoutToWeek(weekId, workoutName)
                if (workoutId != null) {
                    forceReloadWeeks()
                } else {
                    emitError("Não foi possível adicionar o treino à semana.")
                }
            } catch (e: Exception) {
                emitError("Erro ao adicionar treino: ${e.message}")
            }
        }
    }

    // ========== DELETAR SEMANA COMPLETA (cascata) ==========

    fun deleteFullWeek(weekId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val week = _allWorkoutWeeks.value.find { it.id == weekId }
                val workoutsInWeek = week?.workouts ?: emptyList()

                // Deletar cada treino e seus dados
                for (workout in workoutsInWeek) {
                    if (workout.id.isBlank()) continue
                    val workoutId = workout.id

                    // Séries
                    (_workoutSets.value[workoutId] ?: emptyList()).forEach {
                        try { firebaseRepository.deleteSet(it.id) } catch (e: Exception) { }
                    }

                    // Exercícios do treino
                    try {
                        firebaseRepository.getExercisesByWorkout(workoutId).forEach {
                            firebaseRepository.removeExerciseFromWorkout(workoutId, it.id)
                        }
                    } catch (e: Exception) { }

                    // Treino
                    try { firebaseRepository.deleteWorkout(workoutId) } catch (e: Exception) { }
                }

                // Deletar a semana
                val success = firebaseRepository.deleteWorkoutWeek(weekId)
                if (success) {
                    val workoutIds = workoutsInWeek.map { it.id }.toSet()
                    withContext(Dispatchers.Main) {
                        _allWorkoutWeeks.value = _allWorkoutWeeks.value.filter { it.id != weekId }
                        _allWorkouts.value = _allWorkouts.value.filter { it.id !in workoutIds }
                        _workoutExercises.value = _workoutExercises.value.toMutableMap()
                            .also { map -> workoutIds.forEach { map.remove(it) } }
                        _workoutSets.value = _workoutSets.value.toMutableMap()
                            .also { map -> workoutIds.forEach { map.remove(it) } }
                        setsCache.clear()
                    }
                } else {
                    emitError("Não foi possível apagar a semana.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emitError("Erro ao apagar semana: ${e.message}")
            }
        }
    }

    fun deleteWorkoutFromWeek(weekId: String, workoutId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = deleteWorkout(workoutId)
                if (success) forceReloadWeeks()
                else emitError("Não foi possível apagar o treino.")
            } catch (e: Exception) {
                emitError("Erro ao apagar treino da semana: ${e.message}")
            }
        }
    }

    // ========== COMPATIBILIDADE ==========

    fun getExercisesByWorkout(workoutId: String): Flow<List<Exercise>> = flow {
        emit(firebaseRepository.getExercisesByWorkout(workoutId))
    }

    fun forceRefreshAll() {
        exercisesLoaded = false
        workoutsLoaded = false
        weeksLoaded = false
        loadExercisesIfNeeded()
        loadWorkoutsIfNeeded()
        loadWeeksIfNeeded()
    }

    fun clearAllCaches() {
        _allExercises.value = emptyList()
        _allWorkouts.value = emptyList()
        _allWorkoutWeeks.value = emptyList()
        _workoutExercises.value = emptyMap()
        _workoutSets.value = emptyMap()
        setsCache.clear()
        exercisesLoaded = false
        workoutsLoaded = false
        weeksLoaded = false
        allSetsLoaded = false
    }
}