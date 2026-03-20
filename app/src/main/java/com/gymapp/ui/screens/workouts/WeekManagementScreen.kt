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
import androidx.compose.runtime.collectAsState
import com.gymapp.data.firebase.WorkoutWeek
import com.gymapp.data.firebase.WorkoutSession
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekManagementScreen(
    navController: NavController,
    workoutViewModel: UnifiedWorkoutViewModel
) {
    var showAddWeekDialog by remember { mutableStateOf(false) }
    
    // Get weeks from ViewModel
    val weeks by workoutViewModel.workoutWeeks.collectAsState(initial = emptyList())
    
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
            Text(
                text = "Minhas Semanas",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = { showAddWeekDialog = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar semana",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        // Weeks List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(weeks) { week ->
                WeekCard(
                    week = week,
                    onClick = { 
                        navController.navigate("week_detail/${week.id}")
                    }
                )
            }
        }
    }
    
    // Add Week Dialog
    if (showAddWeekDialog) {
        AddWeekManagementDialog(
            onDismiss = { showAddWeekDialog = false },
            onConfirm = { weekName ->
                // TODO: Implement addNewWeek in ViewModel
                showAddWeekDialog = false
            }
        )
    }
}

@Composable
fun WeekCard(
    week: WorkoutWeek,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    
    Card(
        onClick = onClick,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = week.weekName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${dateFormat.format(week.weekStart)} - ${dateFormat.format(week.weekEnd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${week.workouts.size} treinos",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "${week.totalVolume}kg total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AddWeekManagementDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var weekName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Semana de Treino") },
        text = {
            OutlinedTextField(
                value = weekName,
                onValueChange = { weekName = it },
                label = { Text("Nome da semana (ex: Semana 1)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (weekName.isNotBlank()) {
                        onConfirm(weekName)
                    }
                },
                enabled = weekName.isNotBlank()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
