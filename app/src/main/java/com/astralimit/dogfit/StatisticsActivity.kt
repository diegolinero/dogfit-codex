package com.astralimit.dogfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astralimit.dogfit.ui.theme.DogFitTheme
import java.util.concurrent.TimeUnit

class StatisticsActivity : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogFitTheme {
                StatisticsScreen(viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: DogFitViewModel, onBack: () -> Unit) {
    val dailyStats by viewModel.dailyStats.observeAsState(DailySummary())
    val profile by viewModel.dogProfile.observeAsState()
    val activityTimes by viewModel.activityTimes.collectAsState()
    var selectedPeriod by remember { mutableStateOf("Día") }
    val periods = listOf("Día", "Semana", "Mes")

    val restMinutes = TimeUnit.MILLISECONDS.toMinutes(activityTimes[0] ?: 0L).toInt()
    val walkMinutes = TimeUnit.MILLISECONDS.toMinutes(activityTimes[1] ?: 0L).toInt()
    val runMinutes = TimeUnit.MILLISECONDS.toMinutes(activityTimes[2] ?: 0L).toInt()
    val playMinutes = TimeUnit.MILLISECONDS.toMinutes(activityTimes[3] ?: 0L).toInt()
    val totalActiveMinutes = dailyStats?.totalActiveMinutes ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Actividad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Resumen de $selectedPeriod", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(Icons.Default.Timer, "$totalActiveMinutes", "Min Activos")
                        StatCard(Icons.Default.LocalFireDepartment, "${dailyStats?.caloriesBurned?.toInt() ?: 0}", "Calorías")
                    }
                }
            }

            Text("Desglose de Actividad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivityRow(Icons.Default.NightsStay, "Descanso", restMinutes, Color(0xFF9E9E9E))
                    ActivityRow(Icons.Default.DirectionsWalk, "Caminata", walkMinutes, Color(0xFF4CAF50))
                    ActivityRow(Icons.Default.DirectionsRun, "Correr", runMinutes, Color(0xFFFF9800))
                    ActivityRow(Icons.Default.SportsBaseball, "Juego", playMinutes, Color(0xFF2196F3))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Progreso del Objetivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    val targetMinutes = profile?.targetActiveMinutes ?: 60
                    val progress = if (targetMinutes > 0) (totalActiveMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f) else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$totalActiveMinutes / $targetMinutes minutos activos (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when (selectedPeriod) {
                "Semana" -> {
                    val weeklyStats = viewModel.getWeeklyStats()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Estadísticas Semanales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Total minutos activos: ${weeklyStats.totalActiveMinutes}")
                            Text("Promedio diario: ${weeklyStats.avgDailyMinutes} min")
                            Text("Días activos: ${weeklyStats.activeDays}")
                        }
                    }
                }
                "Mes" -> {
                    val monthlyStats = viewModel.getMonthlyStats()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Estadísticas Mensuales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Mes: ${monthlyStats.month}")
                            Text("Total minutos activos: ${monthlyStats.totalActiveMinutes}")
                            Text("Promedio diario: ${monthlyStats.avgDailyMinutes} min")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, minutes: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text("$minutes min", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
