package com.astralimit.dogfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect

class HistoryScreen : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogFitTheme {
                HistoryContent(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    viewModel: DogFitViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.activityHistory.observeAsState()
    val dailyStats by viewModel.dailyStats.observeAsState()
    val weeklyStats by viewModel.weeklyStats.observeAsState()
    val activityTimes by viewModel.activityTimes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.reloadActivityTimesFromDatabase()
    }

    var selectedPeriod by remember { mutableStateOf("Hoy") }
    val periods = listOf("Hoy", "Semana", "Mes")

    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Actividad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                when (selectedPeriod) {
                    "Hoy" -> TodaySummaryCard(dailyStats, activityTimes)
                    "Semana" -> WeeklySummaryCard(weeklyStats)
                    "Mes" -> MonthlySummaryCard(viewModel)
                }
            }

            item {
                Text(
                    text = "Actividad Reciente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedPeriod == "Hoy") {
                        // Selector simple de fecha para el modo "Hoy" (Historial diario específico)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(selectedYear, selectedMonth, selectedDay)
                                    cal.add(Calendar.DAY_OF_YEAR, -1)
                                    selectedYear = cal.get(Calendar.YEAR)
                                    selectedMonth = cal.get(Calendar.MONTH)
                                    selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                }) { Icon(Icons.Default.ChevronLeft, "Anterior") }

                                Text(
                                    text = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(selectedYear, selectedMonth, selectedDay)
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                    selectedYear = cal.get(Calendar.YEAR)
                                    selectedMonth = cal.get(Calendar.MONTH)
                                    selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                }) { Icon(Icons.Default.ChevronRight, "Siguiente") }
                            }
                        }
                    }
                }
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)

            val filteredHistory = when (selectedPeriod) {
                "Hoy" -> {
                    history?.filter { it.date == targetDate } ?: emptyList()
                }
                "Semana" -> {
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    val weekAgoDate = dateFormat.format(calendar.time)
                    history?.filter { it.date >= weekAgoDate } ?: emptyList()
                }
                else -> {
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -30)
                    val monthAgoDate = dateFormat.format(calendar.time)
                    history?.filter { it.date >= monthAgoDate } ?: emptyList()
                }
            }

            val groupedByDate = filteredHistory
                .sortedByDescending { it.date }
                .groupBy { it.date }

            if (groupedByDate.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay actividad registrada para este periodo", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }

            groupedByDate.forEach { (date, activities) ->
                item {
                    DateHeader(date)
                }

                items(activities) { activity ->
                    DailySummaryItem(activity)
                }
            }
        }
    }
}

@Composable
fun TodaySummaryCard(stats: DailySummary?, activityTimes: Map<Int, Long>) {
    val labels = listOf("Reposo", "Caminando", "Corriendo", "Jugando")
    val colors = listOf(Color.Gray, Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF5722))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Resumen del Día",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(
                    icon = Icons.Default.Timer,
                    value = "${stats?.activeMinutes ?: 0} min",
                    label = "Activo"
                )
                SummaryMetric(
                    icon = Icons.Default.NightsStay,
                    value = "${stats?.restMinutes ?: 0} min",
                    label = "Descanso"
                )
                SummaryMetric(
                    icon = Icons.Default.LocalFireDepartment,
                    value = String.format("%.0f", stats?.caloriesBurned ?: 0f),
                    label = "Calorías"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val targetMinutes = 60
            val progress = ((stats?.activeMinutes ?: 0).toFloat() / targetMinutes).coerceIn(0f, 1f)

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}% del objetivo diario",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Desglose de Actividad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { i, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors[i])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                        val time = activityTimes[i] ?: 0L
                        val minutes = time / 60
                        val seconds = time % 60
                        Text(
                            text = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklySummaryCard(stats: WeeklySummary?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen Semanal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stats?.trend ?: "",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${stats?.totalActiveMinutes ?: 0} min",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Minutos activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats?.avgDailyMinutes ?: 0} min",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Promedio diario",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${stats?.activeDays ?: 0} días activos",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun MonthlySummaryCard(viewModel: DogFitViewModel) {
    val monthlyStats by viewModel.monthlyStats.observeAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Resumen Mensual - ${monthlyStats?.month ?: ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${monthlyStats?.totalActiveMinutes ?: 0} min",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Minutos activos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${monthlyStats?.activeDays ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Días activos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Racha más larga: ${monthlyStats?.longestActiveStreak ?: 0} días",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SummaryMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DateHeader(dateStr: String) {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("EEEE dd 'de' MMMM", Locale("es"))

    val formattedDate = try {
        val date = inputFormat.parse(dateStr)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateStr
    }

    Text(
        text = formattedDate.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun DailySummaryItem(summary: DailySummary) {
    var expanded by remember { mutableStateOf(false) }
    val labels = listOf("Reposo", "Caminando", "Corriendo", "Jugando")
    val colors = listOf(Color.Gray, Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF5722))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resumen de Actividad",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = summary.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.activeMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded && summary.activityTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                summary.activityTimes.entries.sortedBy { it.key }.forEach { (act, time) ->
                    if (time > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.getOrElse(act) { Color.Gray }))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(labels.getOrElse(act) { "Desconocido" }, style = MaterialTheme.typography.bodySmall)
                            }
                            val minutes = time / 60
                            val seconds = time % 60
                            Text(
                                text = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
