package com.astralimit.dogfit

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.astralimit.dogfit.ui.theme.DogFitTheme
import org.json.JSONObject
import androidx.compose.runtime.livedata.observeAsState
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private var isReceiverRegistered = false

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DogFitBleService.ACTION_NEW_DATA -> {
                    intent.getStringExtra("data")?.let { parsear(it) }
                }
                DogFitBleService.ACTION_BLE_STATUS -> {
                    val status = intent.getStringExtra(DogFitBleService.EXTRA_STATUS)
                    viewModel.updateBleConnection(status == DogFitBleService.STATUS_CONNECTED)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DogFitTheme {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = {
                        startActivity(Intent(this, PetProfileScreen::class.java))
                    },
                    onNavigateToHistory = {
                        startActivity(Intent(this, HistoryScreen::class.java))
                    },
                    onNavigateToRoutes = {
                        startActivity(Intent(this, RoutesScreen::class.java))
                    },
                    onNavigateToHealth = {
                        startActivity(Intent(this, DogHealthActivity::class.java))
                    },
                    onNavigateToAlerts = {
                        startActivity(Intent(this, AlertsActivity::class.java))
                    }
                )
            }
        }

        handleNotificationIntent(intent)

        if (checkPermissions()) {
            startService(Intent(this, DogFitBleService::class.java))
        } else {
            requestPermissions()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        when (intent?.getStringExtra("navigate_to")) {
            "pet_profile" -> startActivity(Intent(this, PetProfileScreen::class.java))
        }
    }

    private fun parsear(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val steps = json.optInt("stp", 0)
            val battery = json.optInt("bat", 0)
            val activity = json.optInt("act", 0)

            viewModel.updateBleConnection(true)
            viewModel.updateActivity(activity)
            viewModel.updateBattery(battery)
            viewModel.updateStepsFromBle(steps)

            if (json.has("lat") && json.has("lng")) {
                val lat = json.getDouble("lat")
                val lng = json.getDouble("lng")
                val date = json.optString("date", "")
                val time = json.optString("time", "")
                viewModel.addGpsLocation(lat, lng, date, time)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error parsing BLE data", e)
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
    }

    private fun registerBleReceivers() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(DogFitBleService.ACTION_NEW_DATA)
            addAction(DogFitBleService.ACTION_BLE_STATUS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dataReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(dataReceiver, filter)
        }
        isReceiverRegistered = true
    }

    override fun onStart() {
        super.onStart()
        registerBleReceivers()
    }

    override fun onStop() {
        super.onStop()
        if (isReceiverRegistered) {
            unregisterReceiver(dataReceiver)
            isReceiverRegistered = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DogFitViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToRoutes: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    val scrollState = rememberScrollState()
    val profile by viewModel.dogProfile.observeAsState()
    val dailyStats by viewModel.dailyStats.observeAsState()
    val batteryValue by viewModel.batteryValue.collectAsState()
    val activityValue by viewModel.activityValue.collectAsState()
    val alerts by viewModel.alerts.observeAsState()
    val restMinutes = dailyStats?.restMinutes ?: 0
    val restFallbackMs = TimeUnit.MINUTES.toMillis(restMinutes.toLong())

    val isBleConnected by viewModel.isBleConnected.collectAsState()
    val activityLabel = when {
        !isBleConnected -> "Desconectado"
        activityValue == null -> "Conectado"
        else -> when (activityValue) {
            0 -> "Reposo"
            1 -> "Caminando"
            2 -> "Corriendo"
            3 -> "Jugando"
            else -> "Desconectado"
        }
    }
    val activityColor = when {
        !isBleConnected -> MaterialTheme.colorScheme.outline
        activityValue == null -> MaterialTheme.colorScheme.primary
        else -> when (activityValue) {
            0 -> MaterialTheme.colorScheme.tertiary
            1 -> MaterialTheme.colorScheme.primary
            2 -> MaterialTheme.colorScheme.secondary
            3 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline
        }
    }
    val progress = when {
        !isBleConnected -> 0f
        activityValue == null -> 0.1f
        else -> when (activityValue) {
            0 -> 0.25f
            1 -> 0.5f
            2 -> 0.75f
            3 -> 1f
            else -> 0f
        }
    }
    val animatedColor by animateColorAsState(activityColor, label = "activityColor")

    val activityTextStyle = when (activityLabel.length) {
        in 0..9 -> MaterialTheme.typography.displayMedium
        in 10..11 -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.titleMedium
    }
    val activitySubLabel = if (activityValue == null && isBleConnected) "Conectado" else "ESTADO"
    val activityLabelText = if (activityValue == null && isBleConnected) "Esperando" else activityLabel
    val activityTimes = dailyStats?.activityTimes ?: emptyMap()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DogFit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile?.name ?: "Tu mascota",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAlerts) {
                        BadgedBox(
                            badge = {
                                val pendingAlerts = alerts?.size ?: 0
                                if (pendingAlerts > 0) {
                                    Badge { Text("$pendingAlerts") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alertas")
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Pets, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 12.dp,
                            color = animatedColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activityLabelText,
                                style = activityTextStyle,
                                fontWeight = FontWeight.Black,
                                color = animatedColor,
                                maxLines = 1
                            )
                            Text(
                                text = activitySubLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Estado actual del collar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Resumen de actividad del día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Descanso: $restMinutes min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumen 24h por actividad",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    listOf(
                        0 to "Reposo",
                        1 to "Caminando",
                        2 to "Corriendo",
                        3 to "Jugando"
                    ).forEach { (type, label) ->
                        val time = if (type == 0) {
                            activityTimes[type] ?: restFallbackMs
                        } else {
                            activityTimes[type] ?: 0L
                        }
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
                        val hours = minutes / 60
                        val remainingMinutes = minutes % 60
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${hours}h ${remainingMinutes}m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Estado",
                    value = activityLabel,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BatteryFull,
                    label = "Batería",
                    value = "${batteryValue ?: "--"}%",
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    value = String.format("%.0f", dailyStats?.caloriesBurned ?: 0f),
                    label = "Calorías"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Route,
                    value = String.format("%.1f", dailyStats?.distanceKm ?: 0f),
                    label = "Km"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    value = "${dailyStats?.activeMinutes ?: 0}",
                    label = "Min activos"
                )
            }

            Text(
                text = "Acceso Rápido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    label = "Historial",
                    onClick = onNavigateToHistory
                )
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Map,
                    label = "Rutas",
                    onClick = onNavigateToRoutes
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MonitorHeart,
                    label = "Salud",
                    onClick = onNavigateToHealth
                )
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Badge,
                    label = "Ficha",
                    onClick = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickAccessCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
