package com.gymapp.ui.screens.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    
    // Carregar exercícios e séries sob demanda quando a tela é exibida
    LaunchedEffect(Unit) {
        workoutViewModel.loadExercisesIfNeeded()
        // Carregar séries de todos os treinos para poder exibir nos cards
        workoutViewModel.loadAllWorkoutSets()
    }
    
    // Get exercises from ViewModel
    val exercises by workoutViewModel.allExercises.collectAsState()
    val isLoading by workoutViewModel.isLoadingExercises.collectAsState()
    
    val filteredExercises = remember(exercises, searchQuery, selectedCategory) {
        exercises.filter { exercise ->
            (selectedCategory == "Todos" || exercise.category == selectedCategory) &&
                    (searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true))
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Exercícios",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { 
                Text(
                    "Pesquisar exercício...",
                    color = androidx.compose.ui.graphics.Color.Gray
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                focusedLabelColor = androidx.compose.ui.graphics.Color.Black,
                unfocusedLabelColor = androidx.compose.ui.graphics.Color.Gray
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = "Pesquisar",
                    tint = androidx.compose.ui.graphics.Color.Gray
                )
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Category Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { selectedCategory = "Todos" },
                    label = { 
                        Text(
                            "Todos",
                            color = if (selectedCategory == "Todos") 
                                androidx.compose.ui.graphics.Color.White 
                            else androidx.compose.ui.graphics.Color.Black
                        ) 
                    },
                    selected = selectedCategory == "Todos",
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.ui.graphics.Color.Black,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        labelColor = androidx.compose.ui.graphics.Color.Black
                    )
                )
            }
            items(listOf("Peito", "Costas", "Pernas", "Ombros", "Abdômen")) { category ->
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
        
        // Exercises List
        if (filteredExercises.isEmpty()) {
            // Mensagem quando não há exercícios
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exercises.isEmpty()) "Nenhum exercício cadastrado" else "Nenhum exercício encontrado",
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    val lastSets = workoutViewModel.getLastSetsForExercise(exercise.name)
                    ExerciseCard(
                        exercise = exercise,
                        onDeleteExercise = { workoutViewModel.deleteExercise(exercise.id) },
                        lastSets = lastSets
                    )
                }
            }
        }
    }
    
    // Add Exercise Button
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showAddExerciseDialog = true },
            modifier = Modifier.padding(16.dp),
            containerColor = androidx.compose.ui.graphics.Color.Black,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            )
        ) {
            Icon(
                Icons.Default.Add, 
                "Adicionar exercício", 
                tint = androidx.compose.ui.graphics.Color.White
            )
        }
    }
    
    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, category ->
                val exercise = com.gymapp.data.firebase.Exercise(
                    name = name,
                    category = category,
                    muscleGroups = listOf(category) // Usar a categoria como grupo muscular
                )
                workoutViewModel.addExercise(exercise)
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
        title = { 
            Text(
                "Adicionar Novo Exercício",
                color = androidx.compose.ui.graphics.Color.Black
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { 
                        Text(
                            "Nome do Exercício",
                            color = androidx.compose.ui.graphics.Color.Gray
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                        focusedLabelColor = androidx.compose.ui.graphics.Color.Black,
                        unfocusedLabelColor = androidx.compose.ui.graphics.Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Categoria:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category selection buttons
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                containerColor = androidx.compose.ui.graphics.Color.White,
                                labelColor = androidx.compose.ui.graphics.Color.Black
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedCategory)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                Text("Cancelar")
            }
        }
    )
}