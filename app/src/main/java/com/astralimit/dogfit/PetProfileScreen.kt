package com.astralimit.dogfit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.livedata.observeAsState
import com.astralimit.dogfit.ui.theme.DogFitTheme
import java.text.SimpleDateFormat
import java.util.*

class PetProfileScreen : ComponentActivity() {

    private val viewModel: DogFitViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogFitTheme {
                PetProfileContent(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetProfileContent(
    viewModel: DogFitViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.dogProfile.observeAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Perfil", "Peso", "Vacunas", "Desparasitación", "Veterinario")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha de ${profile?.name ?: "Mascota"}") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ProfileTab(profile, viewModel)
                1 -> WeightHistoryTab(profile, viewModel)
                2 -> VaccinationsTab(profile?.medicalRecord?.vaccinations ?: emptyList(), viewModel)
                3 -> DewormingTab(profile?.medicalRecord?.dewormings ?: emptyList(), viewModel)
                4 -> VetVisitsTab(profile?.vetVisits ?: emptyList(), viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetVisitsTab(visits: List<VetVisit>, viewModel: DogFitViewModel) {
    var showAddVisitDialog by remember { mutableStateOf(false) }
    var visitToEdit by remember { mutableStateOf<VetVisit?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize()) {
        if (visits.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MedicalServices,
                title = "Sin visitas médicas",
                subtitle = "Registra las consultas al veterinario de tu mascota"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(visits.reversed()) { visit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { visitToEdit = visit },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = visit.reason,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(visit.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (visit.clinicName.isNotEmpty()) {
                                Text(
                                    text = "Clínica: ${visit.clinicName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (visit.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = visit.notes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (visit.prescriptionImageUrl.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(visit.prescriptionImageUrl),
                                        contentDescription = "Receta médica",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showAddVisitDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar Visita")
        }
    }

    if (showAddVisitDialog) {
        AddVetVisitDialog(
            onDismiss = { showAddVisitDialog = false },
            onSave = { visit ->
                viewModel.addVetVisit(visit)
                showAddVisitDialog = false
            }
        )
    }

    if (visitToEdit != null) {
        AddVetVisitDialog(
            visit = visitToEdit,
            onDismiss = { visitToEdit = null },
            onSave = { visit ->
                viewModel.updateVetVisit(visit)
                visitToEdit = null
            },
            onDelete = { visit ->
                viewModel.deleteVetVisit(visit)
                visitToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVetVisitDialog(
    visit: VetVisit? = null,
    onDismiss: () -> Unit,
    onSave: (VetVisit) -> Unit,
    onDelete: ((VetVisit) -> Unit)? = null
) {
    var reason by remember { mutableStateOf(visit?.reason ?: "") }
    var clinicName by remember { mutableStateOf(visit?.clinicName ?: "") }
    var notes by remember { mutableStateOf(visit?.notes ?: "") }
    var prescriptionUri by remember { mutableStateOf<Uri?>(if (visit?.prescriptionImageUrl.isNullOrEmpty()) null else Uri.parse(visit!!.prescriptionImageUrl)) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        prescriptionUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (visit == null) "Registrar Visita" else "Editar Visita") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo de consulta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = clinicName,
                        onValueChange = { clinicName = it },
                        label = { Text("Clínica / Veterinario") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas / Diagnóstico") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                item {
                    Column {
                        Text("Receta / Documentos", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (prescriptionUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(prescriptionUri),
                                    contentDescription = "Receta seleccionada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Text("Subir foto de receta", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(visit?.copy(
                        reason = reason,
                        clinicName = clinicName,
                        notes = notes,
                        prescriptionImageUrl = prescriptionUri?.toString() ?: ""
                    ) ?: VetVisit(
                        reason = reason,
                        clinicName = clinicName,
                        notes = notes,
                        prescriptionImageUrl = prescriptionUri?.toString() ?: ""
                    ))
                },
                enabled = reason.isNotEmpty()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                if (visit != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(visit) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(profile: DogProfile?, viewModel: DogFitViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var manualBreedNameState = remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            photoUri = it
            profile?.let { p ->
                viewModel.updateDogProfile(p.copy(imageUrl = it.toString()))
            }
        }
    }

    val calculatedAge = viewModel.calculateAge(profile?.birthDate)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUrl = profile?.imageUrl ?: ""
                        val displayUri = photoUri?.toString() ?: imageUrl

                        if (displayUri.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(displayUri),
                                contentDescription = "Foto de mascota",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Agregar foto",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Toca para cambiar foto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile?.name ?: "Sin nombre",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${profile?.breed ?: "Raza desconocida"} - ${if (profile?.petType == PetType.CAT) "Gato" else "Perro"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Perfil")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Scale,
                    label = "Peso",
                    value = "${profile?.weight ?: 0} kg"
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Cake,
                    label = "Edad",
                    value = "$calculatedAge años"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    label = "Meta Actividad",
                    value = "${profile?.targetActiveMinutes ?: 60} min"
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Calorías",
                    value = "${profile?.dailyCalories ?: 0} cal"
                )
            }
        }

        if (!profile?.microchipNumber.isNullOrEmpty()) {
            item {
                InfoCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Memory,
                    label = "Microchip",
                    value = profile?.microchipNumber ?: ""
                )
            }
        }

        profile?.birthDate?.let { birthDate ->
            item {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                InfoCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CalendarMonth,
                    label = "Fecha de Nacimiento",
                    value = dateFormat.format(birthDate)
                )
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProfile ->
                val manualBreed = manualBreedNameState.value
                val finalProfile = if (updatedProfile.breed == "Otro (Manual)" && manualBreed.isNotEmpty()) {
                    updatedProfile.copy(breed = manualBreed)
                } else {
                    updatedProfile
                }
                viewModel.updateDogProfile(finalProfile)
                showEditDialog = false
            },
            breeds = viewModel.getAllBreeds(),
            viewModel = viewModel,
            manualBreedNameState = manualBreedNameState
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightHistoryTab(profile: DogProfile?, viewModel: DogFitViewModel) {
    var showAddWeightDialog by remember { mutableStateOf(false) }
    val weightHistory = profile?.weightHistory ?: emptyList()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        if (weightHistory.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Scale,
                title = "Sin historial de peso",
                subtitle = "Registra el peso de tu mascota para seguir su evolución"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Resumen de peso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Peso actual: ${profile?.weight ?: 0} kg",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                items(weightHistory.reversed()) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    dateFormat.format(Date(record.timestamp)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${record.weight} kg",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showAddWeightDialog = true },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar Peso")
        }
    }

    if (showAddWeightDialog) {
        var weightInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWeightDialog = false },
            title = { Text("Registrar Peso") },
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        weightInput.toFloatOrNull()?.let {
                            viewModel.addWeightRecord(it)
                        }
                        showAddWeightDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeightDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsTab(vaccinations: List<Vaccination>, viewModel: DogFitViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var vaccinationToEdit by remember { mutableStateOf<Vaccination?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize()) {
        if (vaccinations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Vaccines,
                title = "Sin vacunas",
                subtitle = "Registra las vacunas de tu mascota"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(vaccinations) { vaccination ->
                    VaccinationCard(
                        vaccination = vaccination,
                        dateFormat = dateFormat,
                        onEdit = { vaccinationToEdit = vaccination }
                    )
                }
            }
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar Vacuna")
        }
    }

    if (showAddDialog) {
        AddVaccinationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { v ->
                viewModel.addVaccination(v)
                showAddDialog = false
            }
        )
    }

    if (vaccinationToEdit != null) {
        AddVaccinationDialog(
            vaccination = vaccinationToEdit,
            onDismiss = { vaccinationToEdit = null },
            onSave = { v ->
                viewModel.updateVaccination(v)
                vaccinationToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaccinationDialog(
    vaccination: Vaccination? = null,
    onDismiss: () -> Unit,
    onSave: (Vaccination) -> Unit
) {
    var name by remember { mutableStateOf(vaccination?.name ?: "") }

    // Application Date
    val appCal = Calendar.getInstance().apply { time = vaccination?.applicationDate ?: Date() }
    var appDay by remember { mutableStateOf(appCal.get(Calendar.DAY_OF_MONTH).toString()) }
    var appMonth by remember { mutableStateOf((appCal.get(Calendar.MONTH) + 1).toString()) }
    var appYear by remember { mutableStateOf(appCal.get(Calendar.YEAR).toString()) }

    // Next Due Date
    val nextCal = Calendar.getInstance().apply { time = vaccination?.nextDueDate ?: Date() }
    var nextDay by remember { mutableStateOf(nextCal.get(Calendar.DAY_OF_MONTH).toString()) }
    var nextMonth by remember { mutableStateOf((nextCal.get(Calendar.MONTH) + 1).toString()) }
    var nextYear by remember { mutableStateOf(nextCal.get(Calendar.YEAR).toString()) }

    var veterinarian by remember { mutableStateOf(vaccination?.veterinarianName ?: "") }
    var notes by remember { mutableStateOf(vaccination?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vaccination == null) "Registrar Vacuna" else "Editar Vacuna") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la vacuna") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Fecha de aplicación", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = appDay,
                            onValueChange = { appDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = appMonth,
                            onValueChange = { appMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Mes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = appYear,
                            onValueChange = { appYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Próxima dosis", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nextDay,
                            onValueChange = { nextDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = nextMonth,
                            onValueChange = { nextMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Mes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = nextYear,
                            onValueChange = { nextYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = veterinarian,
                        onValueChange = { veterinarian = it },
                        label = { Text("Veterinario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val appCalendar = Calendar.getInstance()
                        appCalendar.set(
                            appYear.toIntOrNull() ?: 2024,
                            (appMonth.toIntOrNull() ?: 1) - 1,
                            appDay.toIntOrNull() ?: 1
                        )
                        val nextDueCalendar = Calendar.getInstance()
                        nextDueCalendar.set(
                            nextYear.toIntOrNull() ?: 2025,
                            (nextMonth.toIntOrNull() ?: 1) - 1,
                            nextDay.toIntOrNull() ?: 1
                        )

                        val newVaccination = Vaccination(
                            id = vaccination?.id ?: System.currentTimeMillis(),
                            name = name,
                            applicationDate = appCalendar.time,
                            nextDueDate = nextDueCalendar.time,
                            veterinarianName = veterinarian,
                            notes = notes
                        )
                        onSave(newVaccination)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun VaccinationCard(
    vaccination: Vaccination,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit
) {
    val daysUntilDue = ((vaccination.nextDueDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val isUrgent = daysUntilDue in 0..7
    val isOverdue = daysUntilDue < 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> MaterialTheme.colorScheme.errorContainer
                isUrgent -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = vaccination.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    if (isOverdue) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Vencida") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        )
                    } else if (isUrgent) {
                        AssistChip(
                            onClick = {},
                            label = { Text("$daysUntilDue días") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Aplicada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(vaccination.applicationDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Próxima dosis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(vaccination.nextDueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DewormingTab(dewormings: List<Deworming>, viewModel: DogFitViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var dewormingToEdit by remember { mutableStateOf<Deworming?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize()) {
        if (dewormings.isEmpty()) {
            EmptyState(
                icon = Icons.Default.BugReport,
                title = "Sin desparasitaciones",
                subtitle = "Registra los tratamientos de tu mascota"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(dewormings) { deworming ->
                    DewormingCard(
                        deworming = deworming,
                        dateFormat = dateFormat,
                        onEdit = { dewormingToEdit = deworming }
                    )
                }
            }
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar Tratamiento")
        }
    }

    if (showAddDialog) {
        AddDewormingDialog(
            onDismiss = { showAddDialog = false },
            onSave = { d ->
                viewModel.addDeworming(d)
                showAddDialog = false
            }
        )
    }

    if (dewormingToEdit != null) {
        AddDewormingDialog(
            deworming = dewormingToEdit,
            onDismiss = { dewormingToEdit = null },
            onSave = { d ->
                viewModel.updateDeworming(d)
                dewormingToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDewormingDialog(
    deworming: Deworming? = null,
    onDismiss: () -> Unit,
    onSave: (Deworming) -> Unit
) {
    var productName by remember { mutableStateOf(deworming?.productName ?: "") }

    // Application Date
    val appCal = Calendar.getInstance().apply { time = deworming?.applicationDate ?: Date() }
    var appDay by remember { mutableStateOf(appCal.get(Calendar.DAY_OF_MONTH).toString()) }
    var appMonth by remember { mutableStateOf((appCal.get(Calendar.MONTH) + 1).toString()) }
    var appYear by remember { mutableStateOf(appCal.get(Calendar.YEAR).toString()) }

    // Next Due Date
    val nextCal = Calendar.getInstance().apply { time = deworming?.nextDueDate ?: Date() }
    var nextDay by remember { mutableStateOf(nextCal.get(Calendar.DAY_OF_MONTH).toString()) }
    var nextMonth by remember { mutableStateOf((nextCal.get(Calendar.MONTH) + 1).toString()) }
    var nextYear by remember { mutableStateOf(nextCal.get(Calendar.YEAR).toString()) }

    var dosage by remember { mutableStateOf(deworming?.dosage ?: "") }
    var veterinarian by remember { mutableStateOf(deworming?.veterinarianName ?: "") }
    var notes by remember { mutableStateOf(deworming?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deworming == null) "Registrar Tratamiento" else "Editar Tratamiento") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Nombre del producto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Fecha de aplicación", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = appDay,
                            onValueChange = { appDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = appMonth,
                            onValueChange = { appMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Mes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = appYear,
                            onValueChange = { appYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Próxima aplicación", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nextDay,
                            onValueChange = { nextDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = nextMonth,
                            onValueChange = { nextMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Mes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = nextYear,
                            onValueChange = { nextYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosis") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = veterinarian,
                        onValueChange = { veterinarian = it },
                        label = { Text("Veterinario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isNotBlank()) {
                        val appCalendar = Calendar.getInstance()
                        appCalendar.set(
                            appYear.toIntOrNull() ?: 2024,
                            (appMonth.toIntOrNull() ?: 1) - 1,
                            appDay.toIntOrNull() ?: 1
                        )
                        val nextDueCalendar = Calendar.getInstance()
                        nextDueCalendar.set(
                            nextYear.toIntOrNull() ?: 2025,
                            (nextMonth.toIntOrNull() ?: 1) - 1,
                            nextDay.toIntOrNull() ?: 1
                        )

                        val newDeworming = Deworming(
                            id = deworming?.id ?: System.currentTimeMillis(),
                            productName = productName,
                            applicationDate = appCalendar.time,
                            nextDueDate = nextDueCalendar.time,
                            dosage = dosage,
                            veterinarianName = veterinarian,
                            notes = notes
                        )
                        onSave(newDeworming)
                    }
                },
                enabled = productName.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DewormingCard(
    deworming: Deworming,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit
) {
    val daysUntilDue = ((deworming.nextDueDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val isUrgent = daysUntilDue in 0..7
    val isOverdue = daysUntilDue < 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> MaterialTheme.colorScheme.errorContainer
                isUrgent -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = deworming.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    if (isOverdue) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Vencida") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        )
                    } else if (isUrgent) {
                        AssistChip(
                            onClick = {},
                            label = { Text("$daysUntilDue días") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Aplicada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(deworming.applicationDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Próxima dosis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(deworming.nextDueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: DogProfile?,
    onDismiss: () -> Unit,
    onSave: (DogProfile) -> Unit,
    breeds: List<String>,
    viewModel: DogFitViewModel,
    manualBreedNameState: MutableState<String>
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var selectedBreed by remember { mutableStateOf(profile?.breed ?: "Mixed") }
    var weight by remember { mutableStateOf(profile?.weight?.toString() ?: "0.0") }
    var targetMinutes by remember { mutableStateOf(profile?.targetActiveMinutes?.toString() ?: "60") }
    var microchip by remember { mutableStateOf(profile?.microchipNumber ?: "") }
    var petType by remember { mutableStateOf(profile?.petType ?: PetType.DOG) }
    var sensitivity by remember { mutableStateOf(profile?.activitySensitivity ?: 1.0f) }

    // Birth date components
    val cal = Calendar.getInstance().apply { time = profile?.birthDate ?: Date() }
    var day by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH).toString()) }
    var month by remember { mutableStateOf((cal.get(Calendar.MONTH) + 1).toString()) }
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR).toString()) }

    var expandedBreeds by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Tipo de mascota", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = petType == PetType.DOG,
                            onClick = { petType = PetType.DOG },
                            label = { Text("Perro") },
                            leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null) }
                        )
                        FilterChip(
                            selected = petType == PetType.CAT,
                            onClick = { petType = PetType.CAT },
                            label = { Text("Gato") },
                            leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null) }
                        )
                    }
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedBreeds,
                        onExpandedChange = { expandedBreeds = !expandedBreeds }
                    ) {
                        OutlinedTextField(
                            value = selectedBreed,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Raza") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBreeds) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBreeds,
                            onDismissRequest = { expandedBreeds = false }
                        ) {
                            val allBreeds = breeds.toMutableList()
                            if (!allBreeds.contains("Otro (Manual)")) allBreeds.add("Otro (Manual)")
                            allBreeds.forEach { breed ->
                                DropdownMenuItem(
                                    text = { Text(breed) },
                                    onClick = {
                                        selectedBreed = breed
                                        expandedBreeds = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedBreed == "Otro (Manual)") {
                    item {
                        OutlinedTextField(
                            value = manualBreedNameState.value,
                            onValueChange = { manualBreedNameState.value = it },
                            label = { Text("Nombre de la raza") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Text("Fecha de nacimiento", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = day,
                            onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = month,
                            onValueChange = { month = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Mes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    OutlinedTextField(
                        value = targetMinutes,
                        onValueChange = { targetMinutes = it },
                        label = { Text("Meta diaria (min)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    OutlinedTextField(
                        value = microchip,
                        onValueChange = { microchip = it },
                        label = { Text("Nº Microchip") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Sensibilidad de actividad", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.5f..1.5f,
                        steps = 10
                    )
                    Text(
                        text = when {
                            sensitivity < 0.9f -> "Baja (Perros grandes/tranquilos)"
                            sensitivity > 1.1f -> "Alta (Perros pequeños/inquietos)"
                            else -> "Normal"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val context = LocalContext.current
                    val isCalibrated = profile?.isCalibrated ?: false
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, CalibrationScreen::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Settings,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCalibrated) "Recalibrar Sensor" else "Calibrar Sensor (Recomendado)")
                    }
                    if (isCalibrated) {
                        Text(
                            text = "Sensor calibrado correctamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text(
                            text = "La calibración mejora la precisión de detección",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val birthCal = Calendar.getInstance()
                    birthCal.set(
                        year.toIntOrNull() ?: 2024,
                        (month.toIntOrNull() ?: 1) - 1,
                        day.toIntOrNull() ?: 1
                    )

                    val updated = (profile ?: DogProfile()).copy(
                        name = name,
                        breed = selectedBreed,
                        weight = weight.toFloatOrNull() ?: 0f,
                        targetActiveMinutes = targetMinutes.toIntOrNull() ?: 60,
                        birthDate = birthCal.time,
                        microchipNumber = microchip,
                        petType = petType,
                        activitySensitivity = sensitivity
                    )
                    onSave(updated)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
