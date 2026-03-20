package com.gymapp.ui.screens.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.data.firebase.WorkoutSession
import com.gymapp.data.firebase.WorkoutExercise
import com.gymapp.data.firebase.ExerciseSet
import com.gymapp.data.firebase.Exercise
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import com.gymapp.ui.components.AddExerciseToWorkoutDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    navController: NavController,
    workoutId: String,
    workoutViewModel: UnifiedWorkoutViewModel
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAddSetDialog by remember { mutableStateOf("") }
    
    // Get workout data
    val workouts by workoutViewModel.workouts.collectAsState(initial = emptyList())
    val exercises by workoutViewModel.exercises.collectAsState(initial = emptyList())
    
    // Find the workout
    val workout = remember(workouts, workoutId) {
        workouts.find { it.id == workoutId }
    }
    
    // Carregar exercícios gerais se ainda não foram carregados
    LaunchedEffect(workoutId) {
        workoutViewModel.loadExercisesIfNeeded()
    }
    
    // Get exercises for this workout
    val workoutExercises by workoutViewModel.getExercisesByWorkout(workoutId).collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
            
            Text(
                text = workout?.name ?: "Treino",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar exercício",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        // Workout Info
        if (workout != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informações do Treino",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exercícios: ${workout.exerciseCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Duração: ${workout.duration} min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Exercises List
        if (workout != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Exercícios do Treino (${workoutExercises.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (workoutExercises.isEmpty()) {
                        Text(
                            text = "Toque no botão + para adicionar exercícios",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(workoutExercises) { exercise ->
                                WorkoutExerciseCard(
                                    exercise = exercise,
                                    onAddSet = { exerciseId ->
                                        showAddSetDialog = exerciseId
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddExerciseToWorkoutDialog(
            exercises = exercises,
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { exerciseId ->
                // Debug logs
                
                // Adicionar exercício ao treino
                workoutViewModel.addExerciseToWorkout(workoutId, exerciseId)
                showAddExerciseDialog = false
            }
        )
    }
    
    // Add Set Dialog
    if (showAddSetDialog.isNotEmpty()) {
        AddSetExecutionDialog(
            onDismiss = { showAddSetDialog = "" },
            onConfirm = { weight, reps ->
                // TODO: Implement addSetToExercise in ViewModel
                showAddSetDialog = ""
            }
        )
    }
}

@Composable
fun ExerciseExecutionCard(
    exercise: WorkoutExercise,
    onAddSet: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = exercise.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sets
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercise.sets) { set ->
                    SetRow(
                        set = set,
                        setNumber = exercise.sets.indexOf(set) + 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Add Set Button
            Button(
                onClick = { onAddSet(exercise.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Série")
            }
        }
    }
}

@Composable
fun SetRow(
    set: ExerciseSet,
    setNumber: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Série $setNumber",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${set.weight}kg",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${set.reps} reps",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (set.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Concluído",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
fun WorkoutExerciseCard(
    exercise: Exercise,
    onAddSet: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = exercise.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Button(
                onClick = { onAddSet(exercise.id) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Adicionar Série")
            }
        }
    }
}


@Composable
fun AddSetExecutionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Série") },
        text = {
            Column {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Repetições") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weightValue = weight.toDoubleOrNull() ?: 0.0
                    val repsValue = reps.toIntOrNull() ?: 0
                    if (weightValue > 0 && repsValue > 0) {
                        onConfirm(weightValue, repsValue)
                    }
                },
                enabled = weight.isNotBlank() && reps.isNotBlank()
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
