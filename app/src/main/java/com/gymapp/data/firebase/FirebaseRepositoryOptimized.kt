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
    val duration: Int = 0,
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
    val restTime: Int = 0,
    val notes: String = ""
)

class FirebaseRepositoryOptimized {

    private val db: FirebaseFirestore = try {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance("gym-app"))
    } catch (e: Exception) {
        FirebaseFirestore.getInstance()
    }

    private val auth: FirebaseAuth = try {
        FirebaseAuth.getInstance(FirebaseApp.getInstance("gym-app"))
    } catch (e: Exception) {
        FirebaseAuth.getInstance()
    }

    private val exercisesRef = db.collection("exercises")
    private val workoutsRef = db.collection("workouts")
    private val workoutExercisesRef = db.collection("workoutExercises")
    private val setsRef = db.collection("sets")
    private val workoutWeeksRef = db.collection("workoutWeeks")

    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: "anonymous"

    // ========== EXERCISES ==========

    // CORRIGIDO: removido suspend desnecessário — Flow já é assíncrono
    fun getAllExercises(): Flow<List<Exercise>> = flow {
        try {
            val snapshot = try {
                exercisesRef.orderBy("name")
                    .get(com.google.firebase.firestore.Source.CACHE).await()
            } catch (e: Exception) {
                exercisesRef.orderBy("name")
                    .get(com.google.firebase.firestore.Source.SERVER).await()
            }

            val exercises = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Exercise(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    category = data["category"] as? String ?: "",
                    muscleGroups = (data["muscleGroups"] as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList(),
                    lastWeight = data["lastWeight"] as? Double,
                    lastReps = (data["lastReps"] as? Long)?.toInt(),
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            }
            emit(exercises)
        } catch (e: Exception) {
            emit(emptyList())
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
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteExercise(exerciseId: String): Boolean {
        return try {
            exercisesRef.document(exerciseId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ========== WORKOUTS ==========

    // CORRIGIDO: removido suspend desnecessário
    fun getAllWorkouts(): Flow<List<Workout>> = flow {
        try {
            val userId = getCurrentUserId()
            val snapshot = workoutsRef
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get(com.google.firebase.firestore.Source.CACHE)
                .await()

            val workouts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Workout(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    date = (data["date"] as? Long) ?: 0L,
                    exerciseCount = (data["exerciseCount"] as? Long)?.toInt() ?: 0,
                    duration = (data["duration"] as? Long)?.toInt() ?: 0,
                    isCompleted = data["isCompleted"] as? Boolean ?: false,
                    userId = data["userId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            }
            emit(workouts)
        } catch (e: Exception) {
            emit(emptyList())
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
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteWorkout(workoutId: String): Boolean {
        return try {
            // Deletar séries relacionadas
            val setsSnapshot = setsRef.whereEqualTo("workoutId", workoutId).get().await()
            setsSnapshot.documents.forEach { it.reference.delete().await() }
            workoutsRef.document(workoutId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateWorkout(workoutId: String, name: String, date: Long) {
        workoutsRef.document(workoutId).update(mapOf("name" to name, "date" to date)).await()
    }

    suspend fun updateWorkoutCompleted(workoutId: String, isCompleted: Boolean) {
        // 1. Atualiza o documento do treino
        workoutsRef.document(workoutId).update("isCompleted", isCompleted).await()

        // 2. Atualiza isCompleted dentro do array "workouts" de cada semana que contém este treino
        val userId = getCurrentUserId()
        val weeksSnapshot = workoutWeeksRef.whereEqualTo("userId", userId).get().await()
        weeksSnapshot.documents.forEach { weekDoc ->
            val weekData = weekDoc.data ?: return@forEach
            val workoutsList = (weekData["workouts"] as? List<*>)
                ?.mapNotNull { it as? Map<String, Any> }
                ?: return@forEach

            val hasWorkout = workoutsList.any { it["id"] == workoutId }
            if (!hasWorkout) return@forEach

            // Reconstrói o array preservando todos os campos, só alterando isCompleted
            val updatedList = workoutsList.map { w ->
                if (w["id"] == workoutId) {
                    w.toMutableMap().also { it["isCompleted"] = isCompleted }
                } else {
                    w
                }
            }
            weekDoc.reference.update("workouts", updatedList).await()
        }
    }

    suspend fun renameWeek(weekId: String, newName: String) {
        workoutWeeksRef.document(weekId).update("weekName", newName).await()
    }

    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String): Boolean {
        return try {
            val workoutDoc = workoutsRef.document(workoutId).get().await()
            val data = workoutDoc.data ?: return false
            val exercises = (data["exercises"] as? List<Map<String, Any>>)
                ?.filter { it["exerciseId"] != exerciseId } ?: return false
            workoutsRef.document(workoutId).update("exercises", exercises).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun removeWorkoutFromWeek(weekId: String, workoutId: String): Boolean {
        return try {
            val weekDoc = workoutWeeksRef.document(weekId).get().await()
            val weekData = weekDoc.data ?: return false
            val currentWorkouts = (weekData["workouts"] as? List<*>)
                ?.mapNotNull { it as? Map<String, Any> }
                ?.toMutableList() ?: mutableListOf()
            currentWorkouts.removeAll { it["id"] == workoutId }
            workoutWeeksRef.document(weekId).update("workouts", currentWorkouts).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ========== SETS ==========

    /**
     * Busca TODAS as séries do usuário em uma única query.
     * Usado pela aba Exercícios para exibir séries sem precisar abrir cada treino.
     */
    fun getAllSets(): Flow<List<Set>> = flow {
        try {
            val userId = getCurrentUserId()
            // Sem orderBy para evitar necessidade de índice composto no Firestore.
            // A ordenação é feita localmente após a busca.
            val snapshot = setsRef
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val sets = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Set(
                    id = doc.id,
                    workoutId = data["workoutId"] as? String ?: "",
                    exerciseId = data["exerciseId"] as? String ?: "",
                    exerciseName = data["exerciseName"] as? String ?: "",
                    weight = (data["weight"] as? Double) ?: 0.0,
                    reps = (data["reps"] as? Long)?.toInt() ?: 0,
                    userId = data["userId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            }.sortedBy { it.createdAt } // ordenação local
            emit(sets)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

        fun getSetsByWorkout(workoutId: String): Flow<List<Set>> = flow {
        try {
            val allSetsSnapshot = setsRef.get().await()
            val sets = allSetsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Set(
                    id = doc.id,
                    workoutId = data["workoutId"] as? String ?: "",
                    exerciseId = data["exerciseId"] as? String ?: "",
                    exerciseName = data["exerciseName"] as? String ?: "",
                    weight = (data["weight"] as? Double) ?: 0.0,
                    reps = (data["reps"] as? Long)?.toInt() ?: 0,
                    userId = data["userId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            }.filter { it.workoutId == workoutId }.sortedBy { it.createdAt }
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

    suspend fun addSetToExercise(workoutId: String, exerciseId: String, weight: Double, reps: Int) {
        val exerciseName = try {
            val doc = exercisesRef.document(exerciseId).get().await()
            doc.data?.get("name") as? String ?: "Exercício"
        } catch (e: Exception) {
            "Exercício"
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
    }

    // ========== WORKOUT WEEKS ==========

    // CORRIGIDO: removido suspend desnecessário
    fun getAllWorkoutWeeks(): Flow<List<WorkoutWeek>> = flow {
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank() || userId == "anonymous") {
                emit(emptyList())
                return@flow
            }

            val snapshot = workoutWeeksRef
                .whereEqualTo("userId", userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            val weeks = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val weekStart = (data["weekStart"] as? com.google.firebase.Timestamp)
                        ?.toDate() ?: Date()
                    val weekEnd = (data["weekEnd"] as? com.google.firebase.Timestamp)
                        ?.toDate() ?: Date()

                    val workoutsList = (data["workouts"] as? List<*>)?.mapNotNull { workoutMap ->
                        val wData = workoutMap as? Map<String, Any> ?: return@mapNotNull null
                        Workout(
                            id = wData["id"] as? String ?: "",
                            name = wData["name"] as? String ?: "",
                            date = (wData["date"] as? Long) ?: 0L,
                            exerciseCount = (wData["exerciseCount"] as? Long)?.toInt() ?: 0,
                            duration = (wData["duration"] as? Long)?.toInt() ?: 0,
                            isCompleted = wData["isCompleted"] as? Boolean ?: false,
                            userId = wData["userId"] as? String ?: "",
                            createdAt = (wData["createdAt"] as? Long) ?: System.currentTimeMillis()
                        )
                    } ?: emptyList()

                    WorkoutWeek(
                        id = doc.id,
                        weekStart = weekStart,
                        weekEnd = weekEnd,
                        weekName = data["weekName"] as? String ?: "Semana",
                        workouts = workoutsList,
                        totalVolume = (data["totalVolume"] as? Double) ?: 0.0,
                        userId = userId,
                        createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            emit(weeks)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

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
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteWorkoutWeek(weekId: String): Boolean {
        return try {
            workoutWeeksRef.document(weekId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addWorkoutToWeek(weekId: String, workoutName: String): String? {
        return try {
            val workout = Workout(
                name = workoutName,
                date = System.currentTimeMillis(),
                userId = getCurrentUserId(),
                createdAt = System.currentTimeMillis()
            )
            val workoutId = addWorkout(workout) ?: return null

            val weekDoc = workoutWeeksRef.document(weekId).get().await()
            val weekData = weekDoc.data ?: return null

            // CORRIGIDO: ao reconstruir o array, busca isCompleted atualizado de cada
            // treino diretamente na coleção "workouts" para não perder o estado atual
            val rawList = (weekData["workouts"] as? List<*>)
                ?.mapNotNull { it as? Map<String, Any> }
                ?: emptyList()

            val currentWorkouts = rawList.map { w ->
                val wId = w["id"] as? String ?: return@map w
                val freshDoc = try {
                    workoutsRef.document(wId).get().await()
                } catch (e: Exception) { null }
                val freshCompleted = freshDoc?.getBoolean("isCompleted") ?: (w["isCompleted"] as? Boolean ?: false)
                w.toMutableMap().also { it["isCompleted"] = freshCompleted }
            }.toMutableList()

            // Adiciona o novo treino ao final
            currentWorkouts.add(mapOf(
                "id"          to workoutId,
                "name"        to workoutName,
                "date"        to System.currentTimeMillis(),
                "exerciseCount" to 0,
                "duration"    to 0,
                "isCompleted" to false,
                "userId"      to getCurrentUserId(),
                "createdAt"   to System.currentTimeMillis()
            ))
            workoutWeeksRef.document(weekId).update("workouts", currentWorkouts).await()
            workoutId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ========== EXERCISES BY WORKOUT ==========

    suspend fun getExercisesByWorkout(workoutId: String): List<Exercise> {
        return try {
            val workoutDoc = workoutsRef.document(workoutId).get().await()
            if (!workoutDoc.exists()) return emptyList()

            val exercisesList = workoutDoc.data?.get("exercises") as? List<*> ?: return emptyList()

            exercisesList.mapNotNull { exerciseData ->
                val exerciseMap = exerciseData as? Map<String, Any> ?: return@mapNotNull null
                val exerciseId = exerciseMap["exerciseId"] as? String ?: return@mapNotNull null

                try {
                    val exerciseDoc = exercisesRef.document(exerciseId).get().await()
                    if (exerciseDoc.exists()) {
                        val data = exerciseDoc.data ?: return@mapNotNull null
                        Exercise(
                            id = exerciseId,
                            name = data["name"] as? String ?: exerciseMap["exerciseName"] as? String ?: "",
                            category = data["category"] as? String ?: exerciseMap["exerciseCategory"] as? String ?: "",
                            muscleGroups = (data["muscleGroups"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            lastWeight = data["lastWeight"] as? Double,
                            lastReps = (data["lastReps"] as? Long)?.toInt(),
                            createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                        )
                    } else {
                        Exercise(
                            id = exerciseId,
                            name = exerciseMap["exerciseName"] as? String ?: "",
                            category = exerciseMap["exerciseCategory"] as? String ?: "Outros"
                        )
                    }
                } catch (e: Exception) {
                    Exercise(
                        id = exerciseId,
                        name = exerciseMap["exerciseName"] as? String ?: "",
                        category = exerciseMap["exerciseCategory"] as? String ?: "Outros"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        val exerciseDoc = exercisesRef.document(exerciseId).get().await()
        if (!exerciseDoc.exists()) throw NoSuchElementException("Exercise not found: $exerciseId")

        val data = exerciseDoc.data ?: throw NoSuchElementException("Exercise data null")
        val name = data["name"] as? String ?: ""
        val category = data["category"] as? String ?: ""

        val workoutDoc = workoutsRef.document(workoutId).get().await()
        if (!workoutDoc.exists()) throw NoSuchElementException("Workout not found: $workoutId")

        val workoutData = workoutDoc.data ?: throw NoSuchElementException("Workout data null")
        val currentExercises = (workoutData["exercises"] as? List<*>)?.toMutableList() ?: mutableListOf()

        val alreadyExists = currentExercises.any { ex ->
            (ex as? Map<String, Any>)?.get("exerciseId") == exerciseId
        }
        if (alreadyExists) return

        currentExercises.add(hashMapOf(
            "exerciseId" to exerciseId,
            "exerciseName" to name,
            "exerciseCategory" to category,
            "addedAt" to System.currentTimeMillis()
        ))

        workoutsRef.document(workoutId).update(
            "exercises", currentExercises,
            "exerciseCount", currentExercises.size
        ).await()
    }

    // NOVO: invalida cache de semanas (mantido para compatibilidade com ViewModel)
    fun invalidateWeeksCache() {
        // Cache foi centralizado no ViewModel — método mantido por compatibilidade
    }

    fun getExerciseProgress(): Flow<List<ExerciseProgressData>> = flow {
        try {
            getAllExercises().collect { exercises ->
                val progress = exercises.map { exercise ->
                    val sets = db.collection("sets")
                        .whereEqualTo("exerciseName", exercise.name)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(3)
                        .get().await()

                    val lastSets = sets.documents.mapNotNull { doc ->
                        val d = doc.data
                        val weight = d?.get("weight") as? Double ?: 0.0
                        val reps = (d?.get("reps") as? Long)?.toInt() ?: 0
                        if (weight > 0 && reps > 0) Pair(weight, reps) else null
                    }

                    ExerciseProgressData(
                        exerciseName = exercise.name,
                        currentWeight = lastSets.firstOrNull()?.first ?: 0.0,
                        currentReps = lastSets.firstOrNull()?.second ?: 0,
                        previousWeight = lastSets.getOrNull(1)?.first ?: 0.0,
                        previousReps = lastSets.getOrNull(1)?.second ?: 0,
                        improvement = when {
                            lastSets.size < 2 -> "Novo"
                            else -> {
                                val wDiff = lastSets[0].first - lastSets[1].first
                                val rDiff = lastSets[0].second - lastSets[1].second
                                when {
                                    wDiff > 0 -> "Peso +${wDiff.toInt()}kg"
                                    rDiff > 0 -> "Reps +$rDiff"
                                    wDiff < 0 -> "Peso ${wDiff.toInt()}kg"
                                    rDiff < 0 -> "Reps $rDiff"
                                    else -> "Estável"
                                }
                            }
                        }
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
                emit(listOf(WeeklyProgressData(workouts.size, 0.0)))
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}