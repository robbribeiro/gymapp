package com.gymapp.data.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseTest {
    
    suspend fun testFirebaseConnection(): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            
            
            // Aguardar um pouco para a autenticação ser processada
            kotlinx.coroutines.delay(2000)
            
            
            // Teste simples de conexão com Firestore
            val testDoc = db.collection("test").document("connection")
            
            testDoc.set(mapOf(
                "timestamp" to System.currentTimeMillis(),
                "test" to "conexao",
                "userId" to (auth.currentUser?.uid ?: "anonymous")
            )).await()
            
            
            // Teste de leitura
            val snapshot = testDoc.get().await()
            val success = snapshot.exists()
            
            
            // Limpar documento de teste
            testDoc.delete().await()
            
            
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun testAuth(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val isAuthenticated = auth.currentUser != null
        if (isAuthenticated) {
        }
        return isAuthenticated
    }
}
