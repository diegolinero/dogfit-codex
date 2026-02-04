package com.astralimit.dogfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astralimit.dogfit.ui.theme.DogFitTheme
import java.text.SimpleDateFormat
import java.util.*

class AlertsActivity : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogFitTheme {
                AlertsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(onBack: () -> Unit) {
    var alerts by remember { mutableStateOf(createTestAlerts()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val unreadCount = alerts.count { !it.isRead }
                    if (unreadCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("$unreadCount")
                        }
                    }
                    IconButton(onClick = { alerts = emptyList() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar todo")
                    }
                }
            )
        }
    ) { padding ->
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay alertas", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts) { alert ->
                    AlertItem(
                        alert = alert,
                        onClick = {
                            alerts = alerts.map { if (it.id == alert.id) it.copy(isRead = true) else it }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: DogAlert, onClick: () -> Unit) {
    val severityColor = when (alert.severity) {
        1 -> Color(0xFF2196F3)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!alert.isRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.message, fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal)
                Spacer(modifier = Modifier.height(4.dp))
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                Text(timeFormat.format(Date(alert.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                alert.recommendedAction?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Acción: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!alert.isRead) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

private fun createTestAlerts(): List<DogAlert> {
    return listOf(
        DogAlert(id = 1, message = "Tu perro necesita agua", timestamp = System.currentTimeMillis() - 3600000, severity = 2, isRead = false, recommendedAction = "Dar agua fresca"),
        DogAlert(id = 2, message = "Temperatura alta detectada", timestamp = System.currentTimeMillis() - 1800000, severity = 3, isRead = false, recommendedAction = "Mover a sombra"),
        DogAlert(id = 3, message = "Recordatorio: Paseo matutino", timestamp = System.currentTimeMillis() - 7200000, severity = 1, isRead = true, recommendedAction = "Pasear 20-30 min"),
        DogAlert(id = 4, message = "Hora de la comida", timestamp = System.currentTimeMillis() - 900000, severity = 1, isRead = false, recommendedAction = "Alimentar"),
        DogAlert(id = 5, message = "El perro ha estado muy activo", timestamp = System.currentTimeMillis() - 1200000, severity = 2, isRead = true, recommendedAction = "Permitir descanso")
    )
}
