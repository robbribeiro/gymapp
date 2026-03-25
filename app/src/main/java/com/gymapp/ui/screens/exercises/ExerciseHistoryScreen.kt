package com.gymapp.ui.screens.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.gymapp.data.firebase.Set
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    navController: NavController,
    exerciseName: String,
    workoutViewModel: UnifiedWorkoutViewModel
) {
    val allSets by workoutViewModel.allSets.collectAsState()

    val historySets = remember(allSets, exerciseName) {
        allSets
            .filter { it.exerciseName == exerciseName }
            .sortedByDescending { it.createdAt }
    }

    // Agrupa por treino (workoutId) mantendo a ordem cronológica
    val groupedByWorkout = remember(historySets) {
        historySets.groupBy { it.workoutId }
            .entries
            .sortedByDescending { (_, sets) -> sets.maxOf { it.createdAt } }
    }

    // Métricas de evolução
    val maxWeight = historySets.maxOfOrNull { it.weight } ?: 0.0
    val lastWeight = historySets.firstOrNull()?.weight ?: 0.0
    val totalSets = historySets.size
    val improvement = if (historySets.size >= 2) {
        val first = historySets.last().weight
        val last = historySets.first().weight
        val diff = last - first
        if (diff > 0) "+${diff.toInt()} kg desde o início"
        else if (diff < 0) "${diff.toInt()} kg desde o início"
        else "Estável"
    } else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Histórico de evolução",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (historySets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Nenhuma série registrada ainda",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Adicione séries nos seus treinos para\nver a evolução aqui",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Cards de métricas
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Maior carga",
                                value = "${maxWeight.toInt()} kg",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Última carga",
                                value = "${lastWeight.toInt()} kg",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Total séries",
                                value = "$totalSets",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Badge de evolução
                    if (improvement != null) {
                        item {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Evolução: $improvement",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }

                    // Histórico agrupado por treino
                    item {
                        Text(
                            "Histórico por sessão",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(groupedByWorkout) { index, (workoutId, sets) ->
                        val sessionDate = sets.maxOf { it.createdAt }
                        val isLatest = index == 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLatest)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = SimpleDateFormat(
                                            "dd/MM/yyyy", Locale.getDefault()
                                        ).format(Date(sessionDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isLatest) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Última sessão",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Cabeçalho tabela
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("#",    modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("Peso", modifier = Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("Reps", modifier = Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.Gray.copy(alpha = 0.2f)
                                )

                                sets.sortedBy { it.createdAt }.forEachIndexed { i, set ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            "${i + 1}",
                                            modifier = Modifier.width(28.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "${set.weight.toInt()} kg",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${set.reps} reps",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
