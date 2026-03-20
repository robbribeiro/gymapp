package com.gymapp.ui.screens.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.ui.components.WeekCard
import com.gymapp.ui.viewmodel.WorkoutWeek
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(navController: NavController, workoutViewModel: UnifiedWorkoutViewModel) {
    var showAddWeekDialog by remember { mutableStateOf(false) }
    
    // Carregar semanas sob demanda quando a tela é exibida
    LaunchedEffect(Unit) {
        workoutViewModel.loadWeeksIfNeeded()
    }
    
    // Get weeks from ViewModel
    val weeks by workoutViewModel.allWorkoutWeeks.collectAsState()
    val isLoading by workoutViewModel.isLoadingWeeks.collectAsState()
    
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
            // Espaço vazio para manter o layout centralizado
            Spacer(modifier = Modifier.width(48.dp))
            
            Text(
                text = "Meus Treinos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = { showAddWeekDialog = true },
                modifier = Modifier.size(48.dp),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar semana",
                    tint = androidx.compose.ui.graphics.Color.Black
                )
            }
        }
        
        // Weeks List
        if (weeks.isEmpty()) {
            // Mensagem quando não há semanas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nenhuma semana cadastrada",
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Toque no botão + para criar sua primeira semana de treinos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(weeks) { week ->
                    val workoutWeek = com.gymapp.ui.viewmodel.WorkoutWeek(
                        weekStart = week.weekStart,
                        weekEnd = week.weekEnd,
                        weekName = week.weekName,
                        workouts = week.workouts.map { workout ->
                            com.gymapp.ui.viewmodel.WorkoutItem(
                                id = workout.id.toLongOrNull() ?: 0L,
                                name = workout.name,
                                date = Date(workout.date),
                                exerciseCount = workout.exerciseCount,
                                duration = workout.duration.toString(),
                                isCompleted = workout.isCompleted,
                                exercises = emptyList()
                            )
                        },
                        totalVolume = week.totalVolume
                    )
                    WeekCard(
                        week = workoutWeek,
                        onClick = { 
                            navController.navigate("week_workouts/${week.id}")
                        }
                    )
                }
            }
        }
    }
    
    // Add Week Dialog
    if (showAddWeekDialog) {
        AddWeekDialog(
            onDismiss = { showAddWeekDialog = false },
            onConfirm = { weekName ->
                workoutViewModel.addWorkoutWeek(weekName)
                showAddWeekDialog = false
            }
        )
    }
}

@Composable
fun AddWeekDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var weekName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Semana") },
        text = {
            OutlinedTextField(
                value = weekName,
                onValueChange = { weekName = it },
                label = { Text("Nome da semana") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (weekName.isNotBlank()) {
                        onConfirm(weekName)
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