package com.gymapp.data.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.Calendar

data class Exercise(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val muscleGroups: List<String> = emptyList(),
    val lastWeight: Double? = null,
    val lastReps: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Workout(
    val id: String = "",
    val name: String,
    val date: Long,
    val exerciseCount: Int = 0,
    val duration: Int = 0,
    val isCompleted: Boolean = false,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Set(
    val id: String = "",
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkoutWeek(
    val id: String = "",
    val weekStart: Date,
    val weekEnd: Date,
    val weekName: String,
    val workouts: List<Workout>,
    val totalVolume: Double = 0.0,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkoutSession(
    val id: String = "",
    val weekId: String,
    val name: String,
    val date: Long,
    val exercises: List<WorkoutExercise>,
    val isCompleted: Boolean = false,
    val duration: Int = 0, // em minutos
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkoutExercise(
    val id: String = "",
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val sets: List<ExerciseSet>,
    val order: Int = 0
)

data class ExerciseSet(
    val id: String = "",
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean = false,
    val restTime: Int = 0, // em segundos
    val notes: String = ""
)

class FirebaseRepositoryOptimized {
    private val db = try {
        val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance("gym-app"))
        // Configurar cache para melhor performance
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .build()
        firestore.firestoreSettings = settings
        firestore
    } catch (e: Exception) {
        val firestore = FirebaseFirestore.getInstance()
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .build()
        firestore.firestoreSettings = settings
        firestore
    }
    private val auth = try {
        FirebaseAuth.getInstance(FirebaseApp.getInstance("gym-app"))
    } catch (e: Exception) {
        FirebaseAuth.getInstance()
    }
    private val exercisesRef = db.collection("exercises")
    private val workoutsRef = db.collection("workouts")
    private val workoutExercisesRef = db.collection("workoutExercises")
    private val setsRef = db.collection("sets")
    private val workoutWeeksRef = db.collection("workoutWeeks")
    
    // Cache local para melhor performance
    private val exercisesCache = mutableMapOf<String, Exercise>()
    private val workoutsCache = mutableMapOf<String, Workout>()
    private val workoutWeeksCache = mutableMapOf<String, WorkoutWeek>()
    private var lastExercisesUpdate = 0L
    private var lastWorkoutsUpdate = 0L
    private var lastWeeksUpdate = 0L
    private val cacheTimeout = 30000L // 30 segundos
    
    /**
     * Invalida o cache de semanas para forçar recarregamento do Firebase
     */
    fun invalidateWeeksCache() {
        workoutWeeksCache.clear()
        lastWeeksUpdate = 0L
    }
    
    /**
     * Remove um treino de uma semana específica
     */
    suspend fun removeWorkoutFromWeek(weekId: String, workoutId: String): Boolean {
        return try {
            
            // Buscar a semana atual
            val weekDoc = workoutWeeksRef.document(weekId).get().await()
            val weekData = weekDoc.data
            
            if (weekData != null) {
                val currentWorkouts = (weekData["workouts"] as? List<*>)?.mapNotNull { 
                    it as? Map<String, Any> 
                }?.toMutableList() ?: mutableListOf()
                
                
                // Remover o treino da lista
                val beforeCount = currentWorkouts.size
                currentWorkouts.removeAll { it["id"] == workoutId }
                val afterCount = currentWorkouts.size
                
                
                // Atualizar a semana no Firebase
                workoutWeeksRef.document(weekId).update("workouts", currentWorkouts).await()
                
                // Atualizar cache local
                val week = workoutWeeksCache[weekId]
                if (week != null) {
                    val updatedWeek = week.copy(workouts = week.workouts.filter { it.id != workoutId })
                    workoutWeeksCache[weekId] = updatedWeek
                    lastWeeksUpdate = System.currentTimeMillis()
                    
                    // Atualizar o cache global
                    FirebaseCache.updateWorkoutWeek(updatedWeek)
                }
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun getCurrentUserId(): String {
        val userId = auth.currentUser?.uid
        return userId ?: "anonymous"
    }
    
    // ========== EXERCISES ==========
    
    suspend fun getAllExercises(): Flow<List<Exercise>> = flow {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Verificar se o cache ainda é válido
            if (exercisesCache.isNotEmpty() && (currentTime - lastExercisesUpdate) < cacheTimeout) {
                emit(exercisesCache.values.toList())
                return@flow
            }
            
            
            // Tentar cache primeiro, depois servidor
            val snapshot = try {
                exercisesRef
                    .orderBy("name")
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .await()
            } catch (e: Exception) {
                exercisesRef
                    .orderBy("name")
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
            }
            
            
            val exercises = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                val exercise = Exercise(
                    id = doc.id,
                    name = data?.get("name") as? String ?: "",
                    category = data?.get("category") as? String ?: "",
                    muscleGroups = (data?.get("muscleGroups") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    lastWeight = data?.get("lastWeight") as? Double,
                    lastReps = (data?.get("lastReps") as? Long)?.toInt(),
                    createdAt = (data?.get("createdAt") as? Long) ?: System.currentTimeMillis()
                )
                exercisesCache[exercise.id] = exercise
                exercise
            }
            
            lastExercisesUpdate = currentTime
            emit(exercises)
        } catch (e: Exception) {
            // Em caso de erro, tentar usar cache se disponível
            if (exercisesCache.isNotEmpty()) {
                emit(exercisesCache.values.toList())
            } else {
                emit(emptyList())
            }
        }
    }
    
    suspend fun addExercise(exercise: Exercise): String? {
        return try {
            val exerciseData = hashMapOf(
                "name" to exercise.name,
                "category" to exercise.category,
                "muscleGroups" to exercise.muscleGroups,
                "lastWeight" to exercise.lastWeight,
                "lastReps" to exercise.lastReps,
                "createdAt" to exercise.createdAt,
                "userId" to getCurrentUserId()
            )
            val docRef = exercisesRef.add(exerciseData).await()
            
            // Atualizar cache local
            val newExercise = exercise.copy(id = docRef.id)
            exercisesCache[docRef.id] = newExercise
            lastExercisesUpdate = System.currentTimeMillis()
            
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteExercise(exerciseId: String): Boolean {
        return try {
            exercisesRef.document(exerciseId).delete().await()
            
            // Remover do cache local
            exercisesCache.remove(exerciseId)
            lastExercisesUpdate = System.currentTimeMillis()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== WORKOUTS ==========
    
    suspend fun getAllWorkouts(): Flow<List<Workout>> = flow {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Verificar se o cache ainda é válido
            if (workoutsCache.isNotEmpty() && (currentTime - lastWorkoutsUpdate) < cacheTimeout) {
                emit(workoutsCache.values.toList())
                return@flow
            }
            
            val userId = getCurrentUserId()
            val snapshot = workoutsRef
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get(com.google.firebase.firestore.Source.CACHE)
                .await()
            
            val workouts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                val workout = Workout(
                    id = doc.id,
                    name = data?.get("name") as? String ?: "",
                    date = (data?.get("date") as? Long) ?: 0L,
                    exerciseCount = (data?.get("exerciseCount") as? Long)?.toInt() ?: 0,
                    duration = (data?.get("duration") as? Long)?.toInt() ?: 0,
                    isCompleted = data?.get("isCompleted") as? Boolean ?: false,
                    userId = data?.get("userId") as? String ?: "",
                    createdAt = (data?.get("createdAt") as? Long) ?: System.currentTimeMillis()
                )
                workoutsCache[workout.id] = workout
                workout
            }
            
            lastWorkoutsUpdate = currentTime
            emit(workouts)
        } catch (e: Exception) {
            // Em caso de erro, tentar usar cache se disponível
            if (workoutsCache.isNotEmpty()) {
                emit(workoutsCache.values.toList())
            } else {
                emit(emptyList())
            }
        }
    }
    
    suspend fun addWorkout(workout: Workout): String? {
        return try {
            val workoutData = hashMapOf(
                "name" to workout.name,
                "date" to workout.date,
                "exerciseCount" to workout.exerciseCount,
                "duration" to workout.duration,
                "isCompleted" to workout.isCompleted,
                "userId" to getCurrentUserId(),
                "createdAt" to workout.createdAt
            )
            val docRef = workoutsRef.add(workoutData).await()
            
            // Atualizar cache local
            val newWorkout = workout.copy(id = docRef.id)
            workoutsCache[docRef.id] = newWorkout
            lastWorkoutsUpdate = System.currentTimeMillis()
            
            docRef.id
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun deleteWorkout(workoutId: String): Boolean {
        return try {
            // Deletar também todas as séries relacionadas
            val setsSnapshot = setsRef
                .whereEqualTo("workoutId", workoutId)
                .get()
                .await()
            
            setsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            // Deletar o treino
            workoutsRef.document(workoutId).delete().await()
            
            // Remover do cache local
            workoutsCache.remove(workoutId)
            lastWorkoutsUpdate = System.currentTimeMillis()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String): Boolean {
        return try {
            
            // Buscar o treino
            val workoutDoc = workoutsRef.document(workoutId).get().await()
            if (!workoutDoc.exists()) {
                return false
            }
            
            val data = workoutDoc.data ?: return false
            
            // Buscar exercícios do array "exercises"
            val exercises = data["exercises"] as? List<Map<String, Any>> ?: return false
            
            
            // Filtrar o exercício removido - comparar strings corretamente
            val updatedExercises = exercises.filter { 
                val id = it["exerciseId"] as? String ?: ""
                val shouldKeep = id != exerciseId
                shouldKeep
            }
            
            
            // Atualizar o treino
            workoutsRef.document(workoutId).update("exercises", updatedExercises).await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ========== SETS ==========
    
    suspend fun getSetsByWorkout(workoutId: String): Flow<List<Set>> = flow {
        try {
            // Buscar todas as séries e filtrar localmente para evitar problemas de query
            val allSetsSnapshot = setsRef.get().await()
            
            val allSets = allSetsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                Set(
                    id = doc.id,
                    workoutId = data?.get("workoutId") as? String ?: "",
                    exerciseId = data?.get("exerciseId") as? String ?: "",
                    exerciseName = data?.get("exerciseName") as? String ?: "",
                    weight = (data?.get("weight") as? Double) ?: 0.0,
                    reps = (data?.get("reps") as? Long)?.toInt() ?: 0,
                    userId = data?.get("userId") as? String ?: "",
                    createdAt = (data?.get("createdAt") as? Long) ?: System.currentTimeMillis()
                )
            }
            
            // Filtrar localmente por workoutId e ordenar por createdAt (ordem de criação)
            val sets = allSets.filter { it.workoutId == workoutId }
                .sortedBy { it.createdAt }
            
            emit(sets)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun addSet(set: Set): String? {
        return try {
            
            val setData = hashMapOf(
                "workoutId" to set.workoutId,
                "exerciseId" to set.exerciseId,
                "exerciseName" to set.exerciseName,
                "weight" to set.weight,
                "reps" to set.reps,
                "userId" to getCurrentUserId(),
                "createdAt" to set.createdAt
            )
            
            
            val docRef = setsRef.add(setData).await()
            
            // Verificar se o documento foi realmente criado
            val createdDoc = setsRef.document(docRef.id).get().await()
            if (createdDoc.exists()) {
                
                // Verificar se a query consegue encontrar o documento
                val testQuery = setsRef
                    .whereEqualTo("workoutId", set.workoutId)
                    .get()
                    .await()
                testQuery.documents.forEach { doc ->
                    val data = doc.data
                }
            } else {
            }
            
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteSet(setId: String): Boolean {
        return try {
            setsRef.document(setId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== WORKOUT WEEKS ==========
    
    suspend fun getWorkoutsByWeek(): Flow<List<WorkoutWeek>> = flow {
        try {
            getAllWorkouts().collect { workoutList ->
                val weeks = workoutList.groupBy { workout ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = workout.date
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }.map { (weekStart: Long, weekWorkouts: List<Workout>) ->
                    val weekEnd = Calendar.getInstance().apply {
                        timeInMillis = weekStart
                        add(Calendar.DAY_OF_WEEK, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis
                    
                    val weekName = "Semana ${Calendar.getInstance().apply { timeInMillis = weekStart }.get(Calendar.WEEK_OF_YEAR)}/${Calendar.getInstance().apply { timeInMillis = weekStart }.get(Calendar.YEAR)}"
                    
                    WorkoutWeek(
                        weekStart = Date(weekStart),
                        weekEnd = Date(weekEnd),
                        weekName = weekName,
                        workouts = weekWorkouts,
                        totalVolume = 0.0 // Simplified for now
                    )
                }.sortedByDescending { it.weekStart.time }
                
                emit(weeks)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // ========== EXERCISE PROGRESS ==========
    
    fun getExerciseProgress(): Flow<List<ExerciseProgressData>> = flow {
        try {
            getAllExercises().collect { exercises ->
                val progress = exercises.map { exercise ->
                    val sets = db.collection("sets")
                        .whereEqualTo("exerciseName", exercise.name)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(3)
                        .get()
                        .await()
                    
                    val lastSets = sets.documents.mapNotNull { doc ->
                        val data = doc.data
                        val weight = data?.get("weight") as? Double ?: 0.0
                        val reps = (data?.get("reps") as? Long)?.toInt() ?: 0
                        if (weight > 0 && reps > 0) Pair(weight, reps) else null
                    }
                    
                    ExerciseProgressData(
                        exerciseName = exercise.name,
                        currentWeight = lastSets.firstOrNull()?.first ?: 0.0,
                        currentReps = lastSets.firstOrNull()?.second ?: 0,
                        previousWeight = lastSets.getOrNull(1)?.first ?: 0.0,
                        previousReps = lastSets.getOrNull(1)?.second ?: 0,
                        improvement = if (lastSets.size >= 2) {
                            val current = lastSets[0]
                            val previous = lastSets[1]
                            val weightDiff = current.first - previous.first
                            val repsDiff = current.second - previous.second
                            when {
                                weightDiff > 0 -> "Peso +${weightDiff.toInt()}kg"
                                repsDiff > 0 -> "Reps +$repsDiff"
                                weightDiff < 0 -> "Peso ${weightDiff.toInt()}kg"
                                repsDiff < 0 -> "Reps $repsDiff"
                                else -> "Estável"
                            }
                        } else "Novo"
                    )
                }
                emit(progress)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    fun getWeeklyProgress(): Flow<List<WeeklyProgressData>> = flow {
        try {
            getAllWorkouts().collect { workouts ->
                val totalVolume = workouts.sumOf { _ ->
                    // This needs to be fetched from sets associated with this workout
                    // For simplicity, we'll keep it 0.0 for now
                    0.0
                }
                emit(listOf(WeeklyProgressData(workouts.size, totalVolume)))
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    fun getWorkoutWeeks(): Flow<List<WorkoutWeek>> = flow {
        try {
            getAllWorkouts().collect { workouts ->
                val weeks = workouts.groupBy { workout ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = workout.date
                    val week = calendar.get(Calendar.WEEK_OF_YEAR)
                    val year = calendar.get(Calendar.YEAR)
                    "$year-W$week"
                }.map { (weekName, weekWorkouts) ->
                    val weekStart = weekWorkouts.minOfOrNull { it.date } ?: System.currentTimeMillis()
                    val weekEnd = weekWorkouts.maxOfOrNull { it.date } ?: System.currentTimeMillis()
                    
                    WorkoutWeek(
                        weekStart = Date(weekStart),
                        weekEnd = Date(weekEnd),
                        weekName = weekName,
                        workouts = weekWorkouts,
                        totalVolume = weekWorkouts.sumOf { _ -> 0.0 } // Simplified for now
                    )
                }
                emit(weeks)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Função para buscar semanas diretamente do Firestore (seguindo padrão dos exercícios)
    suspend fun getAllWorkoutWeeks(): Flow<List<WorkoutWeek>> = flow {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Verificar se o cache ainda é válido
            if (workoutWeeksCache.isNotEmpty() && (currentTime - lastWeeksUpdate) < cacheTimeout) {
                emit(workoutWeeksCache.values.toList())
                return@flow
            }
            
            val userId = getCurrentUserId()
            
            if (userId.isBlank() || userId == "anonymous") {
                emit(emptyList())
                return@flow
            }
            
            // Sempre buscar do servidor para garantir sincronização
            val snapshot = workoutWeeksRef
                .whereEqualTo("userId", userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            
            
            // Limpar cache local antes de atualizar
            workoutWeeksCache.clear()
            
            val weeks = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    val weekStart = (data["weekStart"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    val weekEnd = (data["weekEnd"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    val weekName = data["weekName"] as? String ?: "Semana"
                    val totalVolume = (data["totalVolume"] as? Double) ?: 0.0
                    val createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                    
                    // Converter workouts de Map para objetos Workout
                    val workoutsList = (data["workouts"] as? List<*>)?.mapNotNull { workoutMap ->
                        try {
                            val workoutData = workoutMap as? Map<String, Any> ?: return@mapNotNull null
                            Workout(
                                id = workoutData["id"] as? String ?: "",
                                name = workoutData["name"] as? String ?: "",
                                date = (workoutData["date"] as? Long) ?: 0L,
                                exerciseCount = (workoutData["exerciseCount"] as? Long)?.toInt() ?: 0,
                                duration = (workoutData["duration"] as? Long)?.toInt() ?: 0,
                                isCompleted = workoutData["isCompleted"] as? Boolean ?: false,
                                userId = workoutData["userId"] as? String ?: "",
                                createdAt = (workoutData["createdAt"] as? Long) ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    
                    val week = WorkoutWeek(
                        id = doc.id,
                        weekStart = weekStart,
                        weekEnd = weekEnd,
                        weekName = weekName,
                        workouts = workoutsList,
                        totalVolume = totalVolume,
                        userId = userId,
                        createdAt = createdAt
                    )
                    
                    workoutWeeksCache[week.id] = week
                    week
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            lastWeeksUpdate = currentTime
            emit(weeks)
        } catch (e: Exception) {
            e.printStackTrace()
            // Em caso de erro, tentar usar cache se disponível
            if (workoutWeeksCache.isNotEmpty()) {
                emit(workoutWeeksCache.values.toList())
            } else {
                emit(emptyList())
            }
        }
    }
    
    // ========== ADDITIONAL METHODS ==========
    
    suspend fun updateWorkout(workoutId: String, name: String, date: Long) {
        val updates = mapOf(
            "name" to name,
            "date" to date
        )
        workoutsRef.document(workoutId).update(updates).await()
    }
    
    fun getWorkoutById(workoutId: String): Flow<Workout?> = flow {
        try {
            val snapshot = workoutsRef.document(workoutId).get().await()
            val workout = snapshot.toObject(Workout::class.java)?.copy(id = snapshot.id)
            emit(workout)
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    fun getWorkoutWeekByStart(weekStartMillis: Long): Flow<WorkoutWeek?> = flow {
        try {
            val userId = getCurrentUserId()
            val snapshot = workoutsRef.whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", weekStartMillis)
                .whereLessThan("date", weekStartMillis + 7 * 24 * 60 * 60 * 1000) // Next week's start
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val workoutList = snapshot.documents.mapNotNull { it.toObject(Workout::class.java)?.copy(id = it.id) }

            if (workoutList.isNotEmpty()) {
                val weekStart = Calendar.getInstance().apply {
                    timeInMillis = weekStartMillis
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val weekEnd = Calendar.getInstance().apply {
                    timeInMillis = weekStart
                    add(Calendar.DAY_OF_WEEK, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val weekName = "Semana ${Calendar.getInstance().apply { timeInMillis = weekStart }.get(Calendar.WEEK_OF_YEAR)}/${Calendar.getInstance().apply { timeInMillis = weekStart }.get(Calendar.YEAR)}"

                val totalVolume = workoutList.sumOf { workout ->
                    // This needs to be fetched from sets associated with this workout
                    // For simplicity, we'll keep it 0.0 for now
                    0.0
                }

                emit(WorkoutWeek(
                    weekStart = Date(weekStart),
                    weekEnd = Date(weekEnd),
                    weekName = weekName,
                    workouts = workoutList,
                    totalVolume = totalVolume
                ))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    suspend fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        try {
            
            // Primeiro, vamos ver os dados brutos do documento
            val exerciseDoc = exercisesRef.document(exerciseId).get().await()
            
            if (exerciseDoc.exists()) {
                val data = exerciseDoc.data
                
                // Tentar deserializar manualmente
                val exercise = try {
                    exerciseDoc.toObject(Exercise::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    
                    // Criar exercício manualmente se a deserialização falhar
                    Exercise(
                        id = exerciseId,
                        name = data?.get("name") as? String ?: "Exercício Desconhecido",
                        category = data?.get("category") as? String ?: "Outros",
                        muscleGroups = (data?.get("muscleGroups") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        lastWeight = (data?.get("lastWeight") as? Double),
                        lastReps = (data?.get("lastReps") as? Long)?.toInt(),
                        createdAt = (data?.get("createdAt") as? Long) ?: System.currentTimeMillis()
                    )
                }
                
                
                if (exercise != null) {
                    // Verificar se o treino existe antes de tentar atualizar
                    val workoutDoc = workoutsRef.document(workoutId).get().await()
                    
                    if (workoutDoc.exists()) {
                        val workoutData = workoutDoc.data
                        
                        // NOVA ABORDAGEM: Adicionar exercício diretamente ao documento do treino
                        val currentExercises = (workoutData?.get("exercises") as? List<*>)?.toMutableList() ?: mutableListOf()
                        
                        // Verificar se o exercício já existe no treino
                        val exerciseExists = currentExercises.any { exerciseData ->
                            val exerciseMap = exerciseData as? Map<String, Any>
                            exerciseMap?.get("exerciseId") == exerciseId
                        }
                        
                        if (!exerciseExists) {
                            val newExerciseData = hashMapOf(
                                "exerciseId" to exerciseId,
                                "exerciseName" to exercise.name,
                                "exerciseCategory" to exercise.category,
                                "addedAt" to System.currentTimeMillis()
                            )
                            currentExercises.add(newExerciseData)
                            
                            // Atualizar o documento do treino com a nova lista de exercícios
                            workoutsRef.document(workoutId).update(
                                "exercises", currentExercises,
                                "exerciseCount", currentExercises.size
                            ).await()
                            
                        } else {
                        }
                        
                    } else {
                        throw NoSuchElementException("Workout not found: $workoutId")
                    }
                } else {
                    throw NoSuchElementException("Exercise could not be deserialized")
                }
            } else {
                throw NoSuchElementException("Exercise not found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    suspend fun deleteExerciseFromWorkout(workoutId: String, exerciseId: String) {
        workoutExercisesRef.whereEqualTo("workoutId", workoutId).whereEqualTo("exerciseId", exerciseId).get().await().documents.forEach {
            it.reference.delete().await()
        }
        // Optionally delete associated sets
        setsRef.whereEqualTo("workoutId", workoutId).whereEqualTo("exerciseId", exerciseId).get().await().documents.forEach { it.reference.delete().await() }

        // Update exerciseCount in workout
        workoutsRef.document(workoutId).update("exerciseCount", com.google.firebase.firestore.FieldValue.increment(-1)).await()
    }

    suspend fun getExercisesByWorkout(workoutId: String): List<Exercise> {
        
        try {
            
            // Buscar exercícios diretamente do documento do treino
            val workoutDoc = workoutsRef.document(workoutId).get().await()
            
            if (workoutDoc.exists()) {
                val workoutData = workoutDoc.data
                
                // Buscar exercícios do array "exercises"
                val exercisesData = workoutData?.get("exercises")
                
                val exercisesList = exercisesData as? List<*>
                
                val exercises = mutableListOf<Exercise>()
                
                if (exercisesList != null && exercisesList.isNotEmpty()) {
                    
                    for ((index, exerciseData) in exercisesList.withIndex()) {
                        
                        val exerciseMap = exerciseData as? Map<String, Any>
                        if (exerciseMap != null) {
                            val exerciseId = exerciseMap["exerciseId"] as? String
                            val exerciseName = exerciseMap["exerciseName"] as? String
                            val exerciseCategory = exerciseMap["exerciseCategory"] as? String
                            
                            
                            if (exerciseId != null) {
                                // Buscar dados completos do exercício na coleção exercises
                                try {
                                    val exerciseDoc = exercisesRef.document(exerciseId).get().await()
                                    val fullExercise = exerciseDoc.toObject(Exercise::class.java)
                                    
                                    if (fullExercise != null) {
                                        // Garantir que o ID está correto
                                        val exerciseWithId = fullExercise.copy(id = exerciseId)
                                        exercises.add(exerciseWithId)
                                    } else {
                                        // Se não encontrar na coleção exercises, criar um exercício básico
                                        val basicExercise = Exercise(
                                            id = exerciseId,
                                            name = exerciseName ?: "Exercício Desconhecido",
                                            category = exerciseCategory ?: "Outros",
                                            muscleGroups = emptyList(),
                                            lastWeight = null,
                                            lastReps = null,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        exercises.add(basicExercise)
                                    }
                                } catch (e: Exception) {
                                    // Criar exercício básico em caso de erro
                                    val basicExercise = Exercise(
                                        id = exerciseId,
                                        name = exerciseName ?: "Exercício Desconhecido",
                                        category = exerciseCategory ?: "Outros",
                                        muscleGroups = emptyList(),
                                        lastWeight = null,
                                        lastReps = null,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    exercises.add(basicExercise)
                                }
                            } else {
                            }
                        } else {
                        }
                    }
                } else {
                }
                
                
                return exercises
            } else {
                return emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    suspend fun addSetToExercise(workoutId: String, exerciseId: String, weight: Double, reps: Int) {
        try {
            
            var exerciseName = "Unknown Exercise"
            
            // Tentar buscar o nome do exercício de forma mais robusta
            try {
                val exerciseDoc = exercisesRef.document(exerciseId).get().await()
                if (exerciseDoc.exists()) {
                    val data = exerciseDoc.data
                    exerciseName = data?.get("name") as? String ?: "Unknown Exercise"
                } else {
                }
            } catch (e: Exception) {
                // Continuar com nome padrão
            }
            
            val setData = hashMapOf(
                "workoutId" to workoutId,
                "exerciseId" to exerciseId,
                "exerciseName" to exerciseName,
                "weight" to weight,
                "reps" to reps,
                "userId" to getCurrentUserId(),
                "createdAt" to System.currentTimeMillis()
            )
            
            
            val docRef = setsRef.add(setData).await()
            setsRef.document(docRef.id).update("id", docRef.id).await()
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    suspend fun deleteSet(workoutId: String, exerciseId: String, setIndex: Int) {
        val sets = setsRef.whereEqualTo("workoutId", workoutId)
            .whereEqualTo("exerciseId", exerciseId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await().documents

        if (setIndex < sets.size) {
            sets[setIndex].reference.delete().await()
        }
    }
    
    // ========== WORKOUT WEEKS ==========
    
    suspend fun addWorkoutWeek(week: WorkoutWeek): String? {
        return try {
            val weekData = hashMapOf(
                "weekStart" to com.google.firebase.Timestamp(week.weekStart),
                "weekEnd" to com.google.firebase.Timestamp(week.weekEnd),
                "weekName" to week.weekName,
                "totalVolume" to week.totalVolume,
                "userId" to getCurrentUserId(),
                "createdAt" to week.createdAt
            )
            val docRef = workoutWeeksRef.add(weekData).await()
            val weekId = docRef.id
            
            // Atualizar cache local imediatamente
            val weekWithId = week.copy(id = weekId)
            workoutWeeksCache[weekId] = weekWithId
            lastWeeksUpdate = System.currentTimeMillis()
            
            weekId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteWorkoutWeek(weekId: String): Boolean {
        return try {
            workoutWeeksRef.document(weekId).delete().await()
            
            // Remover do cache local imediatamente
            workoutWeeksCache.remove(weekId)
            lastWeeksUpdate = System.currentTimeMillis()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun addWorkoutToWeek(weekId: String, workoutName: String): String? {
        return try {
            
            // Criar o treino
            val workout = Workout(
                name = workoutName,
                date = System.currentTimeMillis(),
                exerciseCount = 0,
                duration = 0,
                isCompleted = false,
                userId = getCurrentUserId(),
                createdAt = System.currentTimeMillis()
            )
            
            // Adicionar treino ao Firebase
            val workoutId = addWorkout(workout)
            if (workoutId != null) {
                
                // Atualizar a semana para incluir o novo treino
                val weekDoc = workoutWeeksRef.document(weekId).get().await()
                val weekData = weekDoc.data
                
                if (weekData != null) {
                    val currentWorkouts = (weekData["workouts"] as? List<*>)?.mapNotNull { 
                        it as? Map<String, Any> 
                    }?.toMutableList() ?: mutableListOf()
                    
                    
                    // Adicionar novo treino à lista
                    val newWorkoutData = mapOf(
                        "id" to workoutId,
                        "name" to workoutName,
                        "date" to System.currentTimeMillis(),
                        "exerciseCount" to 0,
                        "duration" to 0,
                        "isCompleted" to false,
                        "userId" to getCurrentUserId(),
                        "createdAt" to System.currentTimeMillis()
                    )
                    currentWorkouts.add(newWorkoutData)
                    
                    
                    // Atualizar documento da semana
                    workoutWeeksRef.document(weekId).update("workouts", currentWorkouts).await()
                    
                    // Atualizar cache local
                    val week = workoutWeeksCache[weekId]
                    if (week != null) {
                        val updatedWorkout = workout.copy(id = workoutId)
                        val updatedWeek = week.copy(workouts = week.workouts + updatedWorkout)
                        workoutWeeksCache[weekId] = updatedWeek
                        lastWeeksUpdate = System.currentTimeMillis()
                        
                        // Atualizar o cache global do FirebaseCache
                        FirebaseCache.updateWorkoutWeek(updatedWeek)
                    } else {
                    }
                    
                    workoutId
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
}
