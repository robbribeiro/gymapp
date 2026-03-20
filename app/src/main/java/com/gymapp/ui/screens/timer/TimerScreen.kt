package com.gymapp.ui.screens.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.gymapp.ui.viewmodel.TimerViewModel
import com.gymapp.utils.PermissionUtils
import com.gymapp.utils.LogTags
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen() {
    val context = LocalContext.current
    
    // Inicializar o ViewModel com o contexto
    LaunchedEffect(Unit) {
        // Exibir todas as tags disponíveis para debug
        LogTags.logAllTags()
        
        TimerViewModel.initialize(context)
        
        // Verificar permissões de notificação
        if (!PermissionUtils.hasNotificationPermission(context)) {
            android.util.Log.w(LogTags.TIMER_SCREEN, "Permissão de notificação não concedida")
        } else {
            android.util.Log.d(LogTags.TIMER_SCREEN, "Permissão de notificação concedida")
        }
    }
    
    val time by TimerViewModel.time.collectAsState()
    val isRunning by TimerViewModel.isRunning.collectAsState()
    
    // Format time
    val minutes = (time / 60000) % 60
    val seconds = (time / 1000) % 60
    
    val timeText = String.format("%02d", minutes)
    val subTimeText = String.format("%02d", seconds)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circular Timer
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 20
                
                // Draw background circle
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw progress circle
                if (time > 0) {
                    val progress = (time % 60000) / 60000f // Progress for each minute
                    val sweepAngle = 360f * progress
                    
                    drawArc(
                        color = androidx.compose.ui.graphics.Color.Black,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            
            // Time display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subTimeText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start/Pause button
            FloatingActionButton(
                onClick = {
                    TimerViewModel.toggleTimer()
                },
                modifier = Modifier.size(64.dp),
                containerColor = androidx.compose.ui.graphics.Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pausar" else "Iniciar",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Reset button
            FloatingActionButton(
                onClick = {
                    TimerViewModel.resetTimer()
                },
                modifier = Modifier.size(64.dp),
                containerColor = androidx.compose.ui.graphics.Color.Gray,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Resetar",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
