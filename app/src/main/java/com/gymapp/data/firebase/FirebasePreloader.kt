package com.gymapp.data.firebase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object FirebasePreloader {
    private var isPreloaded = false
    private val preloadScope = CoroutineScope(Dispatchers.IO)
    
    fun preloadData() {
        if (isPreloaded) return
        
        preloadScope.launch {
            try {
                
                // Aguardar autenticação
                delay(1000)
                
                val repository = FirebaseRepositoryOptimized()
                
                // Pré-carregar dados essenciais em paralelo
                coroutineScope {
                    val exercisesDeferred = async { 
                        try {
                            repository.getAllExercises().collect { exercises ->
                                FirebaseCache.updateExercises(exercises)
                            }
                        } catch (e: Exception) {
                        }
                    }
                    
                    val workoutsDeferred = async { 
                        try {
                            repository.getAllWorkouts().collect { workouts ->
                                FirebaseCache.updateWorkouts(workouts)
                            }
                        } catch (e: Exception) {
                        }
                    }
                    
                    // Aguardar dados essenciais
                    exercisesDeferred.await()
                    workoutsDeferred.await()
                }
                
                isPreloaded = true
                
            } catch (e: Exception) {
            }
        }
    }
    
    fun isDataPreloaded(): Boolean = isPreloaded
}
