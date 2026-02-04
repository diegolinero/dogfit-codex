package com.astralimit.dogfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.astralimit.dogfit.ui.theme.DogFitTheme
import androidx.compose.runtime.livedata.observeAsState
import java.util.concurrent.TimeUnit

class DogHealthActivity : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogFitTheme {
                DogHealthScreen(viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogHealthScreen(viewModel: DogFitViewModel, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val profile by viewModel.dogProfile.observeAsState()
    val dailyStats by viewModel.dailyStats.observeAsState(DailySummary())
    val activityTimes by viewModel.activityTimes.collectAsState()

    val targetActiveMinutes = profile?.targetActiveMinutes ?: 60
    val actualActiveMinutes = dailyStats?.totalActiveMinutes ?: 0

    val vaccinations = profile?.medicalRecord?.vaccinations ?: emptyList()
    val dewormings = profile?.medicalRecord?.dewormings ?: emptyList()

    val vaccinasAlDia = vaccinations.isNotEmpty() && vaccinations.any { it.nextDueDate.time > System.currentTimeMillis() }
    val desparasitacionAlDia = dewormings.isNotEmpty() && dewormings.any { it.nextDueDate.time > System.currentTimeMillis() }

    val activityScore = if (targetActiveMinutes > 0) {
        ((actualActiveMinutes.toFloat() / targetActiveMinutes) * 100).coerceIn(0f, 100f)
    } else 0f

    val medicalScore = when {
        vaccinasAlDia && desparasitacionAlDia -> 100f
        vaccinasAlDia || desparasitacionAlDia -> 50f
        else -> 0f
    }

    val weightScore = profile?.let {
        if (it.weightHistory.size >= 2) {
            val lastWeight = it.weightHistory.last().weight
            val avgWeight = it.weightHistory.map { w -> w.weight }.average().toFloat()
            val deviation = kotlin.math.abs(lastWeight - avgWeight) / avgWeight
            when {
                deviation < 0.05f -> 100f
                deviation < 0.10f -> 80f
                deviation < 0.15f -> 60f
                else -> 40f
            }
        } else 80f
    } ?: 80f

    val wellnessScore = ((activityScore * 0.4f) + (medicalScore * 0.4f) + (weightScore * 0.2f)).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Salud y Bienestar") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        wellnessScore >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        wellnessScore >= 50 -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        else -> Color(0xFFF44336).copy(alpha = 0.2f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Puntuación de Bienestar", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$wellnessScore/100",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            wellnessScore >= 80 -> Color(0xFF4CAF50)
                            wellnessScore >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            wellnessScore >= 80 -> "Excelente estado de salud"
                            wellnessScore >= 50 -> "Salud moderada - revisar pendientes"
                            else -> "Atención requerida"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Cómo se calcula:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreItem("Actividad Diaria (40%)", activityScore.toInt(), "$actualActiveMinutes/$targetActiveMinutes min")
                    ScoreItem("Vacunas al día (20%)", if (vaccinasAlDia) 100 else 0, if (vaccinasAlDia) "Al día" else "Pendientes")
                    ScoreItem("Desparasitación (20%)", if (desparasitacionAlDia) 100 else 0, if (desparasitacionAlDia) "Al día" else "Pendiente")
                    ScoreItem("Peso estable (20%)", weightScore.toInt(), "${profile?.weight ?: 0} kg")
                }
            }

            Text("Estado de Actividad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActivityTimeItem(Icons.Default.NightsStay, "Descanso", activityTimes[0] ?: 0L)
                        ActivityTimeItem(Icons.Default.DirectionsWalk, "Caminata", activityTimes[1] ?: 0L)
                        ActivityTimeItem(Icons.Default.DirectionsRun, "Correr", activityTimes[2] ?: 0L)
                        ActivityTimeItem(Icons.Default.SportsBaseball, "Juego", activityTimes[3] ?: 0L)
                    }
                }
            }

            Text("Registro Médico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MedicalStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Vaccines,
                    title = "Vacunas",
                    count = vaccinations.size,
                    isUpToDate = vaccinasAlDia
                )
                MedicalStatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Medication,
                    title = "Desparasitación",
                    count = dewormings.size,
                    isUpToDate = desparasitacionAlDia
                )
            }

            Text("Perfil de ${profile?.name ?: "Mascota"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tipo:", style = MaterialTheme.typography.bodyMedium)
                        Text(if (profile?.petType == PetType.CAT) "Gato" else "Perro", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Raza:", style = MaterialTheme.typography.bodyMedium)
                        Text(profile?.breed ?: "Desconocida", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Peso:", style = MaterialTheme.typography.bodyMedium)
                        Text("${profile?.weight ?: 0} kg", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Edad:", style = MaterialTheme.typography.bodyMedium)
                        Text("${profile?.age ?: 0} años", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreItem(label: String, score: Int, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(
            "$score%",
            fontWeight = FontWeight.Bold,
            color = when {
                score >= 80 -> Color(0xFF4CAF50)
                score >= 50 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
        )
    }
}

@Composable
fun ActivityTimeItem(icon: ImageVector, label: String, timeMs: Long) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("$minutes min", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun MedicalStatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    count: Int,
    isUpToDate: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isUpToDate) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isUpToDate) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text("$count registros", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(
                if (isUpToDate) "Al día" else "Revisar",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUpToDate) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
