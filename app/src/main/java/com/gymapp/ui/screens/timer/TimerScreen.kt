package com.gymapp.ui.screens.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.gymapp.ui.viewmodel.TimerViewModel

@Composable
fun TimerScreen() {
    val context = LocalContext.current

    // CORRIGIDO: solicita permissão de notificação em runtime (obrigatório Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* permissão concedida ou negada — o timer funciona de qualquer forma */ }

    LaunchedEffect(Unit) {
        TimerViewModel.initialize(context)

        // Solicita a permissão POST_NOTIFICATIONS se Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val time by TimerViewModel.time.collectAsState()
    val isRunning by TimerViewModel.isRunning.collectAsState()

    val minutes = (time / 60000) % 60
    val seconds = (time / 1000) % 60

    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.background
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circular Timer
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 20

                // Círculo de fundo
                drawCircle(
                    color = outlineColor.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Arco de progresso
                if (time > 0) {
                    val progress = (time % 60000) / 60000f
                    drawArc(
                        color = onSurface,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%02d", minutes),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = String.format("%02d", seconds),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause
            FloatingActionButton(
                onClick = { TimerViewModel.toggleTimer() },
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pausar" else "Iniciar",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Stop / Reset
            FloatingActionButton(
                onClick = { TimerViewModel.resetTimer() },
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.outline,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Parar",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}