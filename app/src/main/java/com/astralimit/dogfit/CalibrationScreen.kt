package com.astralimit.dogfit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralimit.dogfit.ui.theme.DogFitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.sqrt

// Variables globales para la lógica (estables y accesibles desde el receptor)
private var magnitudeSamplesLogic = mutableListOf<Float>()
private var isRecordingForLogic = false

class CalibrationScreen : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    private var currentMagnitude by mutableFloatStateOf(0f)

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, i: Intent?) {
            if (i?.action == "com.astralimit.dogfit.NEW_DATA") {
                i.getStringExtra("data")?.let { parseCalibrationData(it) }
            }
        }
    }

    private fun parseCalibrationData(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            if (json.has("ax") && json.has("ay") && json.has("az")) {
                val ax = json.getDouble("ax").toFloat()
                val ay = json.getDouble("ay").toFloat()
                val az = json.getDouble("az").toFloat()

                // Procesar la muestra a través del clasificador para obtener la misma magnitud filtrada
                viewModel.activityClassifier.processSample(ax, ay, az)
                val magnitude = viewModel.activityClassifier.getLastMagnitude()

                // Actualizar magnitud actual para la UI
                currentMagnitude = magnitude

                // Si estamos grabando, añadir a la lista de muestras
                if (isRecordingForLogic) {
                    magnitudeSamplesLogic.add(magnitude)
                }
            }
        } catch (e: Exception) {
            Log.e("CalibrationScreen", "Error parsing calibration data", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogFitTheme {
                CalibrationContent(
                    viewModel = viewModel,
                    currentMagnitude = currentMagnitude,
                    onBack = { finish() },
                    onComplete = { rest, walk, run ->
                        viewModel.saveFullCalibration(rest, walk, run)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            dataReceiver,
            IntentFilter("com.astralimit.dogfit.NEW_DATA"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_EXPORTED else 0
        )
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(dataReceiver)
        } catch (e: Exception) {
            Log.w("CalibrationScreen", "Receiver not registered", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationContent(
    viewModel: DogFitViewModel,
    currentMagnitude: Float,
    onBack: () -> Unit,
    onComplete: (CalibrationFeatures, CalibrationFeatures, CalibrationFeatures) -> Unit
) {
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableFloatStateOf(0f) }
    var samplesCountUI by remember { mutableIntStateOf(0) }

    var restFeatures by remember { mutableStateOf(CalibrationFeatures.EMPTY) }
    var walkFeatures by remember { mutableStateOf(CalibrationFeatures.EMPTY) }
    var runFeatures by remember { mutableStateOf(CalibrationFeatures.EMPTY) }

    var lastExtractedFeatures by remember { mutableStateOf<CalibrationFeatures?>(null) }

    // Sincronizar el estado de grabación con la lógica global
    LaunchedEffect(isRecording) {
        isRecordingForLogic = isRecording
        if (isRecording) {
            magnitudeSamplesLogic.clear()
            samplesCountUI = 0
            while(isRecording) {
                samplesCountUI = magnitudeSamplesLogic.size
                delay(200)
            }
        }
    }

    val steps = listOf(
        CalibrationStep(
            title = "Reposo",
            description = "Coloca el collar en tu mascota mientras está quieta. Mantén esta posición 10 segundos.",
            icon = Icons.Default.Hotel,
            color = Color.Gray
        ),
        CalibrationStep(
            title = "Caminando",
            description = "Haz caminar a tu mascota a un ritmo normal durante 10 segundos.",
            icon = Icons.Default.DirectionsWalk,
            color = Color(0xFF4CAF50)
        ),
        CalibrationStep(
            title = "Corriendo",
            description = "Haz correr a tu mascota durante 10 segundos.",
            icon = Icons.Default.DirectionsRun,
            color = Color(0xFFFFC107)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibración") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )

            Text("Paso ${currentStep + 1} de 3", style = MaterialTheme.typography.labelMedium)

            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(steps[currentStep].color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(steps[currentStep].icon, contentDescription = null, modifier = Modifier.size(60.dp), tint = steps[currentStep].color)
            }

            Text(steps[currentStep].title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(steps[currentStep].description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.weight(1f))

            if (isRecording) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Grabando...", color = Color.Red)
                    CircularProgressIndicator(progress = { recordingProgress }, modifier = Modifier.size(80.dp))
                    Text("Muestras: $samplesCountUI", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                lastExtractedFeatures?.let { features ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Media: ${String.format("%.4f", features.mean)}")
                            Text("Desv. Std: ${String.format("%.4f", features.std)}")
                            Text("RMS: ${String.format("%.4f", features.rms)}")
                        }
                    }
                }

                Button(
                    onClick = {
                        isRecording = true
                        recordingProgress = 0f
                        lastExtractedFeatures = null
                        scope.launch {
                            for (i in 1..100) {
                                delay(100)
                                recordingProgress = i / 100f
                            }
                            isRecording = false
                            val features = extractFeatures(magnitudeSamplesLogic)
                            lastExtractedFeatures = features
                            when (currentStep) {
                                0 -> restFeatures = features
                                1 -> walkFeatures = features
                                2 -> runFeatures = features
                            }
                            if (currentStep < 2) currentStep++ else onComplete(restFeatures, walkFeatures, runFeatures)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (currentStep == 2) "Finalizar" else "Iniciar")
                }
            }
        }
    }
}

private fun extractFeatures(samples: List<Float>): CalibrationFeatures {
    if (samples.isEmpty()) return CalibrationFeatures.EMPTY
    val n = samples.size.toFloat()
    val mean = samples.sum() / n
    val variance = samples.map { (it - mean) * (it - mean) }.sum() / n
    val std = sqrt(variance)
    val rms = sqrt(samples.map { it * it }.sum() / n)
    val peak = samples.maxOrNull() ?: 0f
    val energy = samples.map { it * it }.sum() / n
    return CalibrationFeatures(mean, std, variance, rms, peak, energy)
}

data class CalibrationStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
