package com.gymapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gymapp.ui.navigation.GymAppNavigation
import com.gymapp.ui.theme.GymAppTheme
import com.google.firebase.FirebaseApp
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Firebase
        try {
            // Firebase será inicializado automaticamente via google-services.json
            // Não é necessário configuração manual
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Configurar autenticação anônima se necessário
        try {
            val auth = FirebaseAuth.getInstance()
            
            // Aguardar um pouco para o Firebase ser inicializado
            Thread.sleep(500)
            
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Usuário anônimo autenticado com sucesso
                        } else {
                            task.exception?.printStackTrace()
                        }
                    }
                    .addOnFailureListener { exception ->
                        exception.printStackTrace()
                    }
                   } else {
                       // Usuário já autenticado
                   }
                   
                   // Não fazer pré-carregamento - dados serão carregados sob demanda
                   
               } catch (e: Exception) {
                   e.printStackTrace()
               }
        
        setContent {
            GymAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GymAppNavigation()
                }
            }
        }
    }
}
