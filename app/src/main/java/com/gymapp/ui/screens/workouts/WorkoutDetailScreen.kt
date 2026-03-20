package com.gymapp.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.Set
import com.gymapp.ui.components.AddExerciseToWorkoutDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    navController: NavController,
    workoutId: String,
    workoutViewModel: UnifiedWorkoutViewModel
) {
    var workout by remember { mutableStateOf<Workout?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAddSetDialog by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf("") }
    
    // Load workout data
    val workouts by workoutViewModel.workouts.collectAsState(initial = emptyList())
    
    // Load exercises for this workout using StateFlow (similar to weeks)
    val workoutExercises by workoutViewModel.workoutExercises.collectAsState(initial = emptyMap())
    val exercisesForWorkout = workoutExercises[workoutId] ?: emptyList()
    
    // Load sets for this workout using StateFlow (similar to exercises)
    val workoutSets by workoutViewModel.workoutSets.collectAsState(initial = emptyMap())
    val setsForWorkout = workoutSets[workoutId] ?: emptyList()
    
    // Load exercises and sets when screen is first displayed
    LaunchedEffect(workoutId) {
        
        // Carregar exercícios gerais se ainda não foram carregados
        workoutViewModel.loadExercisesIfNeeded()
        
        // Carregar exercícios e séries específicos do treino
        workoutViewModel.loadWorkoutExercises(workoutId)
        workoutViewModel.loadWorkoutSets(workoutId)
        
        workout = workouts.find { it.id == workoutId }
    }
    
    // Debug logs for exercise and sets loading
    LaunchedEffect(exercisesForWorkout) {
    }
    
    LaunchedEffect(setsForWorkout) {
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = workout?.name ?: "Treino",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Espaço vazio para manter o layout centralizado
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Workout Info Card
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
                    text = "Informações do Treino",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Exercícios: ${exercisesForWorkout.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add Exercise Button
        Button(
            onClick = { showAddExerciseDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Exercício")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exercises List with Sets (Hierarchical View)
        if (exercisesForWorkout.isNotEmpty()) {
            Text(
                text = "Exercícios do Treino (${exercisesForWorkout.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercisesForWorkout) { exercise ->
                    val allWorkoutSets by workoutViewModel.workoutSets.collectAsState()
                    val exerciseSets = remember(exercise.name, allWorkoutSets) {
                        allWorkoutSets[workoutId]?.filter { it.exerciseName == exercise.name } ?: emptyList()
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Exercise Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = exercise.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            selectedExercise = exercise.name
                                            showAddSetDialog = true
                                        }
                                    ) {
                                        Text("Adicionar Série")
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            workoutViewModel.removeExerciseFromWorkout(workoutId, exercise.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remover exercício",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            
                            // Sets for this exercise
                            if (exerciseSets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Séries: ${exerciseSets.joinToString("/") { "${it.weight.toInt()}-${it.reps}" }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            // Deletar todas as séries do exercício
                                            exerciseSets.forEach { set ->
                                                workoutViewModel.deleteSet(set.id, workoutId)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Deletar todas as séries",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma série adicionada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
    }
    
    // Add Exercise Dialog - NOVA VERSÃO COM BUSCA E FILTROS
    if (showAddExerciseDialog) {
        val exercises by workoutViewModel.exercises.collectAsState(initial = emptyList())
        AddExerciseToWorkoutDialog(
            exercises = exercises,
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { exerciseId ->
                // Debug logs
                
                // Add exercise to workout
                workoutViewModel.addExerciseToWorkout(workoutId, exerciseId)
                showAddExerciseDialog = false
            }
        )
    }
    
    // Add Set Dialog
    if (showAddSetDialog) {
        AddSetDialog(
            onDismiss = { 
                showAddSetDialog = false 
            },
            onConfirm = { weight, reps ->
                // Buscar o exerciseId correto baseado no nome do exercício
                val exercise = exercisesForWorkout.find { it.name == selectedExercise }
                var exerciseId = exercise?.id ?: ""
                
                // Se exerciseId estiver vazio, usar o primeiro exercício como fallback
                if (exerciseId.isEmpty() && exercisesForWorkout.isNotEmpty()) {
                    val firstExercise = exercisesForWorkout.first()
                    exerciseId = firstExercise.id
                }
                
                val set = com.gymapp.data.firebase.Set(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    exerciseName = selectedExercise,
                    weight = weight,
                    reps = reps,
                    createdAt = System.currentTimeMillis()
                )
                
                workoutViewModel.addSet(set)
                showAddSetDialog = false
            }
        )
    }
}



@Composable
fun AddSetDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Série") },
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
                }
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