package com.gymapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymapp.data.firebase.Exercise

@Composable
fun AddExerciseToWorkoutDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedExercise by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    
    val categories = listOf("Todos", "Peito", "Costas", "Pernas", "Ombros", "Abdômen")
    
    val filteredExercises = remember(exercises, searchQuery, selectedCategory) {
        exercises.filter { exercise ->
            val matchesSearch = searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Todos" || exercise.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color.White,
        title = { 
            Text(
                "Adicionar Exercício",
                color = androidx.compose.ui.graphics.Color.Black
            ) 
        },
        text = {
            Column {
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Exercises List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    if (filteredExercises.isEmpty()) {
                        item {
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedCategory != "Todos") 
                                    "Nenhum exercício encontrado" 
                                else "Nenhum exercício cadastrado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(filteredExercises) { exercise ->
                            ExerciseSelectionCard(
                                exercise = exercise,
                                isSelected = selectedExercise == exercise.id,
                                onClick = { selectedExercise = exercise.id }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedExercise.isNotEmpty()) {
                        onConfirm(selectedExercise)
                    }
                },
                enabled = selectedExercise.isNotEmpty(),
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

@Composable
fun ExerciseSelectionCard(
    exercise: Exercise,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                androidx.compose.ui.graphics.Color.LightGray
            else androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
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
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = if (isSelected) 
                        androidx.compose.ui.graphics.Color.Black 
                    else androidx.compose.ui.graphics.Color.LightGray,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = exercise.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) 
                            androidx.compose.ui.graphics.Color.White 
                        else androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
