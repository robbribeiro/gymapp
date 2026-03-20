package com.gymapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

object FirebaseConfig {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    fun getCurrentUser() = auth.currentUser
    fun isLoggedIn() = auth.currentUser != null
    fun getCurrentUserId() = auth.currentUser?.uid ?: ""
    
    // Firestore Collections
    fun getUsersCollection() = db.collection("users")
    fun getExercisesCollection() = db.collection("exercises")
    fun getWorkoutsCollection() = db.collection("workouts")
    fun getSetsCollection() = db.collection("sets")
    fun getProgressCollection() = db.collection("progress")
}
