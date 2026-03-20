package com.gymapp.data.persistence

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gymapp.ui.viewmodel.WorkoutItem
import com.gymapp.ui.viewmodel.ExerciseWithSets
import com.gymapp.ui.viewmodel.ExerciseSet
import java.util.*

class WorkoutPersistence(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("gym_workouts", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveWorkouts(workouts: List<WorkoutItem>) {
        val json = gson.toJson(workouts)
        prefs.edit().putString("workouts", json).apply()
    }
    
    fun loadWorkouts(): List<WorkoutItem> {
        val json = prefs.getString("workouts", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<WorkoutItem>>() {}.type
                gson.fromJson(json, type) ?: getDefaultWorkouts()
            } catch (e: Exception) {
                getDefaultWorkouts()
            }
        } else {
            getDefaultWorkouts()
        }
    }
    
    private fun getDefaultWorkouts(): List<WorkoutItem> {
        return listOf(
            WorkoutItem(
                id = 1,
                name = "Treino de Peito",
                date = Date(),
                exerciseCount = 5,
                duration = "45 min",
                isCompleted = true,
                exercises = listOf(
                    ExerciseWithSets(name = "Supino Reto", sets = listOf(
                        ExerciseSet(weight = 80.0, reps = 10),
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 75.0, reps = 12)
                    )),
                    ExerciseWithSets(name = "Supino Inclinado"),
                    ExerciseWithSets(name = "Crucifixo"),
                    ExerciseWithSets(name = "Flexão"),
                    ExerciseWithSets(name = "Tríceps Pulley")
                )
            ),
            WorkoutItem(
                id = 2,
                name = "Treino de Costas",
                date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time,
                exerciseCount = 6,
                duration = "50 min",
                isCompleted = true,
                exercises = listOf(
                    ExerciseWithSets(name = "Puxada Frontal"),
                    ExerciseWithSets(name = "Remada Curvada"),
                    ExerciseWithSets(name = "Puxada Alta"),
                    ExerciseWithSets(name = "Remada Unilateral"),
                    ExerciseWithSets(name = "Encolhimento"),
                    ExerciseWithSets(name = "Rosca Direta")
                )
            ),
            WorkoutItem(
                id = 3,
                name = "Treino de Pernas",
                date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -2) }.time,
                exerciseCount = 7,
                duration = "60 min",
                isCompleted = false,
                exercises = listOf(
                    ExerciseWithSets(name = "Agachamento"),
                    ExerciseWithSets(name = "Leg Press"),
                    ExerciseWithSets(name = "Afundo"),
                    ExerciseWithSets(name = "Cadeira Extensora"),
                    ExerciseWithSets(name = "Cadeira Flexora"),
                    ExerciseWithSets(name = "Panturrilha"),
                    ExerciseWithSets(name = "Stiff")
                )
            )
        )
    }
}
