package com.gymapp.ui.screens.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gymapp.ui.components.WorkoutCard
import com.gymapp.ui.viewmodel.WorkoutWeek
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import com.gymapp.data.firebase.Workout
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekWorkoutsScreen(
    navController: NavController,
    week: WorkoutWeek,
    workoutViewModel: UnifiedWorkoutViewModel
) {
    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var showEditWorkoutDialog by remember { mutableStateOf<com.gymapp.data.firebase.Workout?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<com.gymapp.data.firebase.Workout?>(null) }
    
    // VOLTA: Usar lista embutida da semana (abordagem manual)
    val workouts = week.workouts
    
    
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
                text = week.weekName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { showAddWorkoutDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar treino",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Week Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Resumo da Semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${workouts.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Treinos", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Workouts List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(workouts) { workout ->
                // workout já é do tipo WorkoutItem, não precisa converter
                WorkoutCard(
                    workout = workout,
                    onStartWorkout = { 
                        navController.navigate("workout_detail/${workout.firebaseId}")
                    },
                    onEditWorkout = { 
                        // Converter WorkoutItem para Workout para edição
                        val workoutData = com.gymapp.data.firebase.Workout(
                            id = workout.firebaseId,
                            name = workout.name,
                            date = workout.date.time,
                            exerciseCount = workout.exerciseCount,
                            duration = workout.duration.toIntOrNull() ?: 0,
                            isCompleted = workout.isCompleted,
                            userId = "",
                            createdAt = System.currentTimeMillis()
                        )
                        showEditWorkoutDialog = workoutData
                    },
                    onDeleteWorkout = {
                        // Converter WorkoutItem para Workout para deleção
                        val workoutData = com.gymapp.data.firebase.Workout(
                            id = workout.firebaseId,
                            name = workout.name,
                            date = workout.date.time,
                            exerciseCount = workout.exerciseCount,
                            duration = workout.duration.toIntOrNull() ?: 0,
                            isCompleted = workout.isCompleted,
                            userId = "",
                            createdAt = System.currentTimeMillis()
                        )
                        showDeleteConfirmDialog = workoutData
                    }
                )
            }
        }
    }
    
    // Add Workout Dialog
    if (showAddWorkoutDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddWorkoutDialog = false },
            onConfirm = { workoutName, date ->
                // CORRIGIDO: Usar ID da semana diretamente
                workoutViewModel.addWorkoutToWeek(week.id, workoutName)
                showAddWorkoutDialog = false
            }
        )
    }
    
    // Edit Workout Dialog
    if (showEditWorkoutDialog != null) {
        EditWorkoutDialog(
            workout = showEditWorkoutDialog!!,
            onDismiss = { showEditWorkoutDialog = null },
            onConfirm = { name, date ->
                workoutViewModel.updateWorkout(showEditWorkoutDialog!!.id, name, date.time)
                showEditWorkoutDialog = null
            }
        )
    }
    
    // Dialog de confirmação de deleção
    showDeleteConfirmDialog?.let { workout ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirmar Deleção") },
            text = { 
                Text("Tem certeza que deseja deletar o treino \"${workout.name}\"?\n\nEsta ação irá remover:\n• O treino\n• Todos os exercícios associados\n• Todas as séries registradas\n\nEsta ação não pode ser desfeita.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        workoutViewModel.deleteWorkoutFromWeek(week.id, workout.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Deletar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Date) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Treino") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do treino") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Data: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedDate)
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

@Composable
fun EditWorkoutDialog(
    workout: Workout,
    onDismiss: () -> Unit,
    onConfirm: (String, Date) -> Unit
) {
    var name by remember { mutableStateOf(workout.name) }
    var selectedDate by remember { mutableStateOf(Date(workout.date)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Treino") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do treino") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Data: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedDate)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
