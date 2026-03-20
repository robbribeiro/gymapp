package com.gymapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymapp.data.firebase.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    exercise: Exercise,
    onDeleteExercise: () -> Unit,
    lastSets: List<com.gymapp.data.firebase.Set> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with name, sets info and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sets info - mostrar as últimas 3 séries
                    if (lastSets.isNotEmpty()) {
                        val setsText = lastSets.take(3).joinToString("/") { set ->
                            "${set.weight.toInt()}-${set.reps}"
                        }
                        Text(
                            text = setsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    } else {
                        Text(
                            text = "Nenhuma série",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteExercise
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Deletar exercício",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category and muscle groups
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Show category first
                item {
                    Surface(
                        color = androidx.compose.ui.graphics.Color.LightGray,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = exercise.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Show specific muscle groups (excluding duplicates of category)
                items(exercise.muscleGroups.filter { it != exercise.category }) { muscle ->
                    Surface(
                        color = androidx.compose.ui.graphics.Color.LightGray,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = muscle,
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
        }
    }
}