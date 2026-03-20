package com.gymapp.ui.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gymapp.ui.components.RestTimer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    navController: NavController,
    workoutName: String = "Treino de Peito"
) {
    var showRestTimer by remember { mutableStateOf(false) }
    var currentExerciseIndex by remember { mutableStateOf(0) }
    val exercises = remember { getSampleWorkoutExercises() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workoutName) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showRestTimer = !showRestTimer }) {
                        Icon(Icons.Default.Timer, contentDescription = "Cronômetro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentExerciseIndex + 1).toFloat() / exercises.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            Text(
                text = "Exercício ${currentExerciseIndex + 1} de ${exercises.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Rest Timer (if shown)
            if (showRestTimer) {
                RestTimer(
                    initialSeconds = 90,
                    onTimerFinished = { showRestTimer = false },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            
            // Current Exercise
            if (currentExerciseIndex < exercises.size) {
                ExerciseExecutionCard(
                    exercise = exercises[currentExerciseIndex],
                    onExerciseCompleted = {
                        if (currentExerciseIndex < exercises.size - 1) {
                            currentExerciseIndex++
                            showRestTimer = true
                        } else {
                            // Workout completed
                            navController.navigateUp()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseExecutionCard(
    exercise: WorkoutExerciseExecution,
    onExerciseCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sets by remember { mutableStateOf(exercise.sets.toMutableList()) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Meta: ${exercise.targetSets} séries × ${exercise.targetReps} reps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Sets
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, false)
            ) {
                items(sets.indices.toList()) { index ->
                    SetExecutionRow(
                        setNumber = index + 1,
                        set = sets[index],
                        onSetChanged = { newSet ->
                            sets[index] = newSet
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onExerciseCompleted,
                modifier = Modifier.fillMaxWidth(),
                enabled = sets.all { it.isCompleted }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exercício Concluído")
            }
        }
    }
}

@Composable
fun SetExecutionRow(
    setNumber: Int,
    set: SetExecution,
    onSetChanged: (SetExecution) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (set.isCompleted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$setNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )
            
            OutlinedTextField(
                value = if (set.weight > 0) set.weight.toString() else "",
                onValueChange = { 
                    val weight = it.toDoubleOrNull() ?: 0.0
                    onSetChanged(set.copy(weight = weight))
                },
                label = { Text("Peso") },
                suffix = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = if (set.reps > 0) set.reps.toString() else "",
                onValueChange = { 
                    val reps = it.toIntOrNull() ?: 0
                    onSetChanged(set.copy(reps = reps))
                },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = { 
                    onSetChanged(set.copy(isCompleted = it))
                }
            )
        }
    }
}

data class WorkoutExerciseExecution(
    val id: Long,
    val name: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double,
    val sets: List<SetExecution>
)

data class SetExecution(
    val weight: Double = 0.0,
    val reps: Int = 0,
    val isCompleted: Boolean = false
)

private fun getSampleWorkoutExercises(): List<WorkoutExerciseExecution> {
    return listOf(
        WorkoutExerciseExecution(
            id = 1,
            name = "Supino Reto",
            targetSets = 4,
            targetReps = 10,
            targetWeight = 80.0,
            sets = List(4) { SetExecution() }
        ),
        WorkoutExerciseExecution(
            id = 2,
            name = "Supino Inclinado",
            targetSets = 3,
            targetReps = 12,
            targetWeight = 70.0,
            sets = List(3) { SetExecution() }
        ),
        WorkoutExerciseExecution(
            id = 3,
            name = "Crucifixo",
            targetSets = 3,
            targetReps = 15,
            targetWeight = 25.0,
            sets = List(3) { SetExecution() }
        )
    )
}
