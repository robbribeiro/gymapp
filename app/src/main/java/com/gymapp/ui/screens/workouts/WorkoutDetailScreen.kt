package com.gymapp.ui.screens.workouts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import com.gymapp.data.firebase.Exercise
import com.gymapp.data.firebase.Workout
import com.gymapp.data.firebase.Set
import com.gymapp.ui.components.AddExerciseToWorkoutDialog
import kotlinx.coroutines.delay

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
    var selectedExerciseName by remember { mutableStateOf("") }
    var selectedExerciseId by remember { mutableStateOf("") }
    var pendingSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Observa o ID real da série salva — vem do ViewModel após o Firebase confirmar
    val lastAddedSetId by workoutViewModel.lastAddedSetId.collectAsState()

    val workouts by workoutViewModel.workouts.collectAsState(initial = emptyList())
    val workoutExercises by workoutViewModel.workoutExercises.collectAsState(initial = emptyMap())
    val exercisesForWorkout = workoutExercises[workoutId] ?: emptyList()
    val workoutSets by workoutViewModel.workoutSets.collectAsState(initial = emptyMap())
    val errorMessage by workoutViewModel.errorMessage.collectAsState()

    LaunchedEffect(workoutId) {
        workoutViewModel.loadExercisesIfNeeded()
        workoutViewModel.loadWorkoutExercises(workoutId)
        workoutViewModel.loadWorkoutSets(workoutId)
        workout = workouts.find { it.id == workoutId }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, actionLabel = "OK", duration = SnackbarDuration.Long)
            workoutViewModel.clearError()
        }
    }

    // Snackbar de feedback ao adicionar série
    LaunchedEffect(pendingSnackbar) {
        pendingSnackbar?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            pendingSnackbar = null
        }
    }

    // Remove highlight após 1.5s
    LaunchedEffect(lastAddedSetId) {
        if (lastAddedSetId != null) {
            delay(1500)
            workoutViewModel.clearLastAddedSetId()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Text(
                    text = workout?.name ?: "Treino",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Exercícios: ${exercisesForWorkout.size}", style = MaterialTheme.typography.bodyMedium)
                    val totalSets = (workoutSets[workoutId] ?: emptyList()).size
                    Text("Séries totais: $totalSets", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showAddExerciseDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Exercício")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (exercisesForWorkout.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Toque em \"Adicionar Exercício\" para começar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(exercisesForWorkout, key = { it.id }) { exercise ->
                        val exerciseSets = remember(exercise.name, workoutSets) {
                            (workoutSets[workoutId] ?: emptyList())
                                .filter { it.exerciseName == exercise.name }
                                .sortedBy { it.createdAt }
                        }
                        ExerciseDetailCard(
                            exercise = exercise,
                            sets = exerciseSets,
                            lastAddedSetId = lastAddedSetId,
                            onAddSet = {
                                selectedExerciseName = exercise.name
                                selectedExerciseId = exercise.id
                                showAddSetDialog = true
                            },
                            onDeleteExercise = {
                                workoutViewModel.removeExerciseFromWorkout(workoutId, exercise.id)
                            },
                            onDeleteSet = { setId ->
                                workoutViewModel.deleteSet(setId, workoutId)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        val exercises by workoutViewModel.exercises.collectAsState(initial = emptyList())
        AddExerciseToWorkoutDialog(
            exercises = exercises,
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { exerciseId ->
                workoutViewModel.addExerciseToWorkout(workoutId, exerciseId)
                showAddExerciseDialog = false
            }
        )
    }

    if (showAddSetDialog) {
        WorkoutDetailAddSetDialog(
            exerciseName = selectedExerciseName,
            onDismiss = { showAddSetDialog = false },
            onConfirm = { weight, reps ->
                val set = Set(
                    workoutId = workoutId,
                    exerciseId = selectedExerciseId,
                    exerciseName = selectedExerciseName,
                    weight = weight,
                    reps = reps,
                    createdAt = System.currentTimeMillis()
                )
                workoutViewModel.addSet(set)
                pendingSnackbar = "Série adicionada: ${weight.toInt()}kg × $reps reps"
                showAddSetDialog = false
            }
        )
    }
}

// ── Exercise Detail Card ──────────────────────────────────────────────────────

@Composable
fun ExerciseDetailCard(
    exercise: Exercise,
    sets: List<Set>,
    lastAddedSetId: String?,
    onAddSet: () -> Unit,
    onDeleteExercise: () -> Unit,
    onDeleteSet: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = exercise.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Botão + série
                IconButton(
                    onClick = onAddSet,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Adicionar série",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Botão remover exercício
                IconButton(onClick = onDeleteExercise) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remover exercício",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Tabela de séries
            if (sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Cabeçalho da tabela
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("#",    modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Peso", modifier = Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Reps", modifier = Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(36.dp))
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                sets.forEachIndexed { index, set ->
                    val isNew = set.id == lastAddedSetId

                    // Animação de fundo verde na série recém adicionada
                    val highlightAlpha by animateFloatAsState(
                        targetValue = if (isNew) 0.18f else 0f,
                        animationSpec = tween(durationMillis = 500),
                        label = "highlight"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF4CAF50).copy(alpha = highlightAlpha),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.width(32.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "${set.weight.toInt()} kg",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${set.reps} reps",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { onDeleteSet(set.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Apagar série",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (index < sets.lastIndex) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Nenhuma série adicionada",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ── Add Set Dialog ────────────────────────────────────────────────────────────

@Composable
fun WorkoutDetailAddSetDialog(
    exerciseName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    val isValid = weight.toDoubleOrNull() != null && reps.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Text("Adicionar Série", fontWeight = FontWeight.Bold, color = Color.Black)
                Text(exerciseName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Repetições", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val r = reps.toIntOrNull() ?: 0
                    if (w > 0 && r > 0) onConfirm(w, r)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
            ) { Text("Cancelar") }
        }
    )
}