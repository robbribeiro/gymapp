package com.gymapp.ui.screens.exercises

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.ui.components.ExerciseCard
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import com.gymapp.data.firebase.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(navController: NavController, workoutViewModel: UnifiedWorkoutViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    // Controla qual exercício está aguardando confirmação de deleção
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        workoutViewModel.loadExercisesIfNeeded()
        workoutViewModel.loadAllWorkoutSets()
    }

    val exercises by workoutViewModel.allExercises.collectAsState()
    val isLoading by workoutViewModel.isLoadingExercises.collectAsState()
    val errorMessage by workoutViewModel.errorMessage.collectAsState()
    val allSets by workoutViewModel.allSets.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, actionLabel = "OK", duration = SnackbarDuration.Long)
            workoutViewModel.clearError()
        }
    }

    val filteredExercises = remember(exercises, searchQuery, selectedCategory) {
        exercises.filter { exercise ->
            (selectedCategory == "Todos" || exercise.category == selectedCategory) &&
                (searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = androidx.compose.ui.graphics.Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Add, "Adicionar exercício", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Exercícios",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pesquisar exercício...", color = androidx.compose.ui.graphics.Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                    focusedLabelColor = androidx.compose.ui.graphics.Color.Black,
                    unfocusedLabelColor = androidx.compose.ui.graphics.Color.Gray
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = androidx.compose.ui.graphics.Color.Gray)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todos", "Peito", "Costas", "Pernas", "Ombros", "Abdômen")) { category ->
                    FilterChip(
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                category,
                                color = if (selectedCategory == category)
                                    androidx.compose.ui.graphics.Color.White
                                else androidx.compose.ui.graphics.Color.Black
                            )
                        },
                        selected = selectedCategory == category,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = androidx.compose.ui.graphics.Color.Black,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                            containerColor = androidx.compose.ui.graphics.Color.White,
                            labelColor = androidx.compose.ui.graphics.Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Carregando exercícios...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }
                }
                filteredExercises.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (exercises.isEmpty()) "Nenhum exercício cadastrado"
                                   else "Nenhum exercício encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = if (exercises.isEmpty())
                                "Toque no botão + para criar seu primeiro exercício"
                            else
                                "Tente ajustar os filtros ou termo de pesquisa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredExercises, key = { it.id }) { exercise ->
                            val lastSets = remember(allSets, exercise.name) {
                                allSets
                                    .filter { it.exerciseName == exercise.name }
                                    .sortedBy { it.createdAt }
                                    .takeLast(3)
                            }
                            ExerciseCard(
                                exercise = exercise,
                                onDeleteExercise = { exerciseToDelete = exercise },
                                onViewHistory = {
                                    navController.navigate(
                                        "exercise_history/${exercise.name}"
                                    )
                                },
                                lastSets = lastSets
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Confirmação de deleção ────────────────────────────────────────────────
    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            containerColor = androidx.compose.ui.graphics.Color.White,
            title = {
                Text(
                    text = "Apagar exercício?",
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tem certeza que deseja apagar \"${exercise.name}\" da biblioteca?",
                        color = androidx.compose.ui.graphics.Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "As séries já registradas nos treinos não serão afetadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        workoutViewModel.deleteExercise(exercise.id)
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Apagar", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { exerciseToDelete = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Adicionar exercício ───────────────────────────────────────────────────
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, category ->
                workoutViewModel.addExercise(
                    Exercise(name = name, category = category, muscleGroups = listOf(category))
                )
                showAddExerciseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Peito") }
    val categories = listOf("Peito", "Costas", "Pernas", "Ombros", "Abdômen")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color.White,
        title = { Text("Adicionar Novo Exercício", color = androidx.compose.ui.graphics.Color.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Exercício", color = androidx.compose.ui.graphics.Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                        focusedTextColor = androidx.compose.ui.graphics.Color.Black,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Categoria:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    category,
                                    color = if (selectedCategory == category)
                                        androidx.compose.ui.graphics.Color.White
                                    else androidx.compose.ui.graphics.Color.Black
                                )
                            },
                            selected = selectedCategory == category,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = androidx.compose.ui.graphics.Color.Black,
                                containerColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedCategory) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.Black)
            ) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.Black)
            ) { Text("Cancelar") }
        }
    )
}