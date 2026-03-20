package com.gymapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymapp.ui.screens.exercises.ExercisesScreen
import com.gymapp.ui.screens.workouts.WorkoutsScreen
import com.gymapp.ui.screens.workouts.WorkoutDetailScreen
import com.gymapp.ui.screens.workouts.WeekWorkoutsScreen
import com.gymapp.ui.screens.timer.TimerScreen
import com.gymapp.ui.viewmodel.WorkoutWeek
import com.gymapp.ui.viewmodel.UnifiedWorkoutViewModel
import androidx.compose.ui.platform.LocalContext
import com.gymapp.ui.theme.GymAppTheme
import java.util.Date

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Workouts : Screen("workouts", "Treinos", Icons.Filled.FitnessCenter)
    object Exercises : Screen("exercises", "Exercícios", Icons.AutoMirrored.Filled.List)
    object Timer : Screen("timer", "Cronômetro", Icons.Filled.Timer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val workoutViewModel = remember { UnifiedWorkoutViewModel(context) }
    val items = listOf(
        Screen.Workouts,
        Screen.Exercises,
        Screen.Timer
    )

    GymAppTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true)
                                        androidx.compose.ui.graphics.Color.Black
                                    else
                                        androidx.compose.ui.graphics.Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    color = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true)
                                        androidx.compose.ui.graphics.Color.Black
                                    else
                                        androidx.compose.ui.graphics.Color.Gray
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color.Black,
                                selectedTextColor = androidx.compose.ui.graphics.Color.Black,
                                unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                                unselectedTextColor = androidx.compose.ui.graphics.Color.Gray,
                                indicatorColor = androidx.compose.ui.graphics.Color.LightGray
                            ),
                            onClick = {
                                if (currentDestination?.route != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(0) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Workouts.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Workouts.route) { 
                    WorkoutsScreen(navController, workoutViewModel) 
                }
                composable(Screen.Exercises.route) { 
                    ExercisesScreen(navController, workoutViewModel) 
                }
                composable(Screen.Timer.route) { 
                    TimerScreen() 
                }
                composable("workout_detail/{workoutId}") { backStackEntry ->
                    val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                    WorkoutDetailScreen(navController, workoutId, workoutViewModel)
                }
                composable("week_workouts/{weekId}") { backStackEntry ->
                    val weekId = backStackEntry.arguments?.getString("weekId")
                    if (weekId != null) {
                        val weeks by workoutViewModel.allWorkoutWeeks.collectAsState()
                        val week = weeks.find { it.id == weekId }
                        
                        
                        week?.let { weekData ->
                            val workoutWeek = com.gymapp.ui.viewmodel.WorkoutWeek(
                                id = weekData.id,
                                weekStart = weekData.weekStart,
                                weekEnd = weekData.weekEnd,
                                weekName = weekData.weekName,
                                workouts = weekData.workouts.map { workout ->
                                    com.gymapp.ui.viewmodel.WorkoutItem(
                                        id = workout.id.hashCode().toLong(), // Manter para compatibilidade com UI
                                        name = workout.name,
                                        date = Date(workout.date),
                                        exerciseCount = workout.exerciseCount,
                                        duration = workout.duration.toString(),
                                        isCompleted = workout.isCompleted,
                                        exercises = emptyList(),
                                        firebaseId = workout.id // Usar o ID do workout (que deve ser o ID do documento)
                                    )
                                },
                                totalVolume = weekData.totalVolume
                            )
                            WeekWorkoutsScreen(navController, workoutWeek, workoutViewModel)
                        } ?: run {
                            // Handle case where week is not found
                            Text("Semana não encontrada", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        // Handle invalid weekId argument
                        Text("ID da semana inválido", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}