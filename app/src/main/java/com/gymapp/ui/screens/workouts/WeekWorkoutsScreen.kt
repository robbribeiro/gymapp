package com.gymapp.ui.screens.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.gymapp.ui.components.WorkoutCard
import com.gymapp.ui.viewmodel.WorkoutItem
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
    var showEditWorkoutDialog by remember { mutableStateOf<Workout?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Workout?>(null) }
    var showCopyWorkoutDialog by remember { mutableStateOf<WorkoutItem?>(null) }

    val workouts = week.workouts
    // Para o diálogo de copiar: busca todas as semanas disponíveis
    val allWeeks by workoutViewModel.allWorkoutWeeks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = week.weekName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddWorkoutDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar treino")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resumo da semana
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${workouts.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Treinos", style = MaterialTheme.typography.bodySmall)
                }
                VerticalDivider(modifier = Modifier.height(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${workouts.count { it.isCompleted }}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("Concluídos", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(workouts, key = { it.firebaseId }) { workout ->
                WorkoutCard(
                    workout = workout,
                    onStartWorkout = {
                        navController.navigate("workout_detail/${workout.firebaseId}")
                    },
                    onEditWorkout = {
                        showEditWorkoutDialog = Workout(
                            id = workout.firebaseId,
                            name = workout.name,
                            date = workout.date.time,
                            exerciseCount = workout.exerciseCount,
                            duration = workout.duration.toIntOrNull() ?: 0,
                            isCompleted = workout.isCompleted
                        )
                    },
                    onDeleteWorkout = {
                        showDeleteConfirmDialog = Workout(
                            id = workout.firebaseId,
                            name = workout.name,
                            date = workout.date.time,
                            exerciseCount = workout.exerciseCount,
                            duration = workout.duration.toIntOrNull() ?: 0,
                            isCompleted = workout.isCompleted
                        )
                    },
                    onToggleCompleted = {
                        workoutViewModel.toggleWorkoutCompleted(workout.firebaseId)
                    },
                    onCopyWorkout = {
                        showCopyWorkoutDialog = workout
                    }
                )
            }
        }
    }

    // ── Adicionar Treino ──────────────────────────────────────────────────────
    if (showAddWorkoutDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddWorkoutDialog = false },
            onConfirm = { workoutName, _ ->
                workoutViewModel.addWorkoutToWeek(week.id, workoutName)
                showAddWorkoutDialog = false
            }
        )
    }

    // ── Editar Treino ─────────────────────────────────────────────────────────
    showEditWorkoutDialog?.let { workout ->
        EditWorkoutDialog(
            workout = workout,
            onDismiss = { showEditWorkoutDialog = null },
            onConfirm = { name, date ->
                workoutViewModel.updateWorkout(workout.id, name, date.time)
                showEditWorkoutDialog = null
            }
        )
    }

    // ── Copiar treino para outra semana ───────────────────────────────────────
    showCopyWorkoutDialog?.let { workout ->
        val otherWeeks = allWeeks.filter { it.id != week.id }
        CopyWorkoutDialog(
            workoutName = workout.name,
            availableWeeks = otherWeeks.map { it.id to it.weekName },
            onDismiss = { showCopyWorkoutDialog = null },
            onConfirm = { targetWeekId ->
                workoutViewModel.copyWorkoutToWeek(workout.firebaseId, targetWeekId)
                showCopyWorkoutDialog = null
            }
        )
    }

    // ── Confirmar Deleção ─────────────────────────────────────────────────────
    showDeleteConfirmDialog?.let { workout ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirmar Deleção", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tem certeza que deseja deletar o treino \"${workout.name}\"?\n\n" +
                    "Esta ação irá remover o treino, todos os exercícios e séries registradas.\n\n" +
                    "Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        workoutViewModel.deleteWorkoutFromWeek(week.id, workout.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun CopyWorkoutDialog(
    workoutName: String,
    availableWeeks: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedWeekId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Copiar treino", fontWeight = FontWeight.Bold)
                Text(
                    workoutName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (availableWeeks.isEmpty()) {
                Text(
                    "Não há outras semanas disponíveis. Crie outra semana primeiro.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column {
                    Text(
                        "Selecione a semana destino:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    availableWeeks.forEach { (id, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedWeekId == id,
                                onClick = { selectedWeekId = id }
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedWeekId?.let { onConfirm(it) } },
                enabled = selectedWeekId != null && availableWeeks.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface
                )
            ) { Text("Copiar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Date) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selectedDate = remember { Date() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Treino", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do treino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedDate) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
    val selectedDate = remember { Date(workout.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Treino", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do treino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedDate) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}