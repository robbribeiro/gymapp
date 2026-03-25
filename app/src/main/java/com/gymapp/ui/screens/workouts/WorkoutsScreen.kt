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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.ui.components.WeekCard
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(navController: NavController, workoutViewModel: UnifiedWorkoutViewModel) {
    var showAddWeekDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { workoutViewModel.loadWeeksIfNeeded() }

    val weeks by workoutViewModel.allWorkoutWeeks.collectAsState()
    val isLoading by workoutViewModel.isLoadingWeeks.collectAsState()
    val errorMessage by workoutViewModel.errorMessage.collectAsState()

    // Exibe Snackbar quando há erro
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, actionLabel = "OK", duration = SnackbarDuration.Long)
            workoutViewModel.clearError()
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(Icons.Default.Add, "Adicionar semana", tint = androidx.compose.ui.graphics.Color.Black)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Carregando seus treinos...", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.Gray)
                    }
                }
            } else if (weeks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nenhuma semana cadastrada", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Toque no botão + para criar sua primeira semana de treinos", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(weeks, key = { it.id }) { week ->
                        val workoutWeek = com.gymapp.ui.viewmodel.WorkoutWeek(
                            id = week.id,
                            weekStart = week.weekStart, weekEnd = week.weekEnd,
                            weekName = week.weekName,
                            workouts = week.workouts.map { workout ->
                                com.gymapp.ui.viewmodel.WorkoutItem(
                                    id = workout.id.toLongOrNull() ?: 0L,
                                    name = workout.name, date = Date(workout.date),
                                    exerciseCount = workout.exerciseCount,
                                    duration = workout.duration.toString(),
                                    isCompleted = workout.isCompleted, exercises = emptyList()
                                )
                            },
                            totalVolume = week.totalVolume
                        )
                        WeekCard(
                            week = workoutWeek,
                            onClick = { navController.navigate("week_workouts/${week.id}") },
                            onRenameWeek = { newName -> workoutViewModel.renameWeek(week.id, newName) },
                            onDeleteWeek = { workoutViewModel.deleteFullWeek(week.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddWeekDialog) {
        AddWeekDialog(
            onDismiss = { showAddWeekDialog = false },
            onConfirm = { weekName -> workoutViewModel.addWorkoutWeek(weekName); showAddWeekDialog = false }
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
        containerColor = androidx.compose.ui.graphics.Color.White,
        title = { Text("Adicionar Semana", color = androidx.compose.ui.graphics.Color.Black) },
        text = {
            OutlinedTextField(
                value = weekName,
                onValueChange = { weekName = it },
                label = { Text("Nome da semana", color = androidx.compose.ui.graphics.Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                    focusedLabelColor = androidx.compose.ui.graphics.Color.Black,
                    unfocusedLabelColor = androidx.compose.ui.graphics.Color.Gray,
                    focusedTextColor = androidx.compose.ui.graphics.Color.Black,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.Black
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (weekName.isNotBlank()) onConfirm(weekName) },
                colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.Black)
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.Black)
            ) {
                Text("Cancelar")
            }
        }
    )
}