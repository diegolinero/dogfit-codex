package com.astralimit.dogfit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import android.util.Log
import java.util.*

class DogFitViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // 🔥 STATE FLOW para Compose (Recomendado)
    private val _activityValue = MutableStateFlow<Int?>(null)
    val activityValue: StateFlow<Int?> = _activityValue.asStateFlow()

    private val _batteryValue = MutableStateFlow<Int?>(null)
    val batteryValue: StateFlow<Int?> = _batteryValue.asStateFlow()

    // LiveData para compatibilidad
    private val _activityLiveData = MutableLiveData<Int?>()
    val activityLiveData: LiveData<Int?> = _activityLiveData

    private val _batteryLiveData = MutableLiveData<Int?>()
    val batteryLiveData: LiveData<Int?> = _batteryLiveData

    // Strings para UI
    private val _activityState = MutableLiveData<String>().apply { value = "Actividad: --" }
    val activityState: LiveData<String> = _activityState

    private val _batteryLevel = MutableLiveData<String>().apply { value = "Batería: --%" }
    val batteryLevel: LiveData<String> = _batteryLevel

    private val _stepsCount = MutableLiveData<String>().apply { value = "Pasos hoy: 0" }
    val stepsCount: LiveData<String> = _stepsCount

    private val _caloriesBurned = MutableLiveData<String>().apply { value = "Calorías: 0" }
    val caloriesBurned: LiveData<String> = _caloriesBurned

    private val _distanceToday = MutableLiveData<String>().apply { value = "Distancia: 0 km" }
    val distanceToday: LiveData<String> = _distanceToday

    private val _activityHistory = MutableLiveData<List<DogActivityData>>(emptyList())
    val activityHistory: LiveData<List<DogActivityData>> = _activityHistory

    private val _dogProfile = MutableLiveData<DogProfile>(DogProfile())
    val dogProfile: LiveData<DogProfile> = _dogProfile

    private val _alerts = MutableLiveData<List<DogAlert>>(emptyList())
    val alerts: LiveData<List<DogAlert>> = _alerts

    private val _dailyStats = MutableLiveData<DailySummary>()
    val dailyStats: LiveData<DailySummary> = _dailyStats

    private val _weeklyStats = MutableLiveData<WeeklySummary>()
    val weeklyStats: LiveData<WeeklySummary> = _weeklyStats

    private val _monthlyStats = MutableLiveData<MonthlySummary>()
    val monthlyStats: LiveData<MonthlySummary> = _monthlyStats

    private val breedRecommendations = mapOf(
        "Labrador" to BreedRecommendation(
            breed = "Labrador Retriever",
            recommendedDailySteps = 8000..12000,
            recommendedPlayTime = 60,
            energyLevel = "Alta",
            healthTips = listOf(
                "Propenso a obesidad - controlar peso",
                "Necesita ejercicio diario intenso",
                "Excelente para actividades acuáticas"
            )
        ),
        "Bulldog" to BreedRecommendation(
            breed = "Bulldog Francés",
            recommendedDailySteps = 3000..5000,
            recommendedPlayTime = 30,
            energyLevel = "Baja",
            healthTips = listOf(
                "Evitar ejercicio en horas de calor",
                "Problemas respiratorios - monitorear",
                "Necesita descansos frecuentes"
            )
        ),
        "Pastor Alemán" to BreedRecommendation(
            breed = "Pastor Alemán",
            recommendedDailySteps = 10000..15000,
            recommendedPlayTime = 90,
            energyLevel = "Muy Alta",
            healthTips = listOf(
                "Necesita estimulación mental",
                "Excelente para deportes caninos",
                "Propenso a problemas de cadera"
            )
        ),
        "Chihuahua" to BreedRecommendation(
            breed = "Chihuahua",
            recommendedDailySteps = 2000..4000,
            recommendedPlayTime = 20,
            energyLevel = "Moderada",
            healthTips = listOf(
                "Proteger del frío",
                "Socialización temprana importante",
                "Ejercicio en espacios controlados"
            )
        ),
        "Golden Retriever" to BreedRecommendation(
            breed = "Golden Retriever",
            recommendedDailySteps = 9000..13000,
            recommendedPlayTime = 75,
            energyLevel = "Alta",
            healthTips = listOf(
                "Amigable y sociable",
                "Necesita ejercicio regular",
                "Propenso a cáncer - chequeos frecuentes"
            )
        ),
        "Mixed" to BreedRecommendation(
            breed = "Mestizo",
            recommendedDailySteps = 5000..8000,
            recommendedPlayTime = 45,
            energyLevel = "Moderada",
            healthTips = listOf(
                "Adaptable a diferentes rutinas",
                "Observar comportamiento individual",
                "Chequeos veterinarios regulares"
            )
        )
    )

    init {
        loadMockData()
        generateInitialAlerts()
    }

    // 🔥 FUNCIONES PRINCIPALES para actualizar desde Bluetooth
    fun updateActivity(activityValue: Int) {
        viewModelScope.launch {
            val normalizedActivity = activityValue.coerceIn(0, 3)
            // Actualizar StateFlow (para Compose)
            _activityValue.value = normalizedActivity
            _activityLiveData.value = normalizedActivity

            // Actualizar texto formateado
            val activityStateText = when (normalizedActivity) {
                0 -> "🛌 Reposo"
                1 -> "🚶 Caminada"
                2 -> "🏃 Carrera"
                3 -> "🎾 Juego"
                else -> "❓ Desconocido"
            }
            _activityState.value = "Actividad: $activityStateText"

            Log.d("DogFitViewModel", "✅ Actividad actualizada: $normalizedActivity")
        }
    }

    fun updateBattery(batteryValue: Int) {
        viewModelScope.launch {
            // Actualizar StateFlow (para Compose)
            _batteryValue.value = batteryValue
            _batteryLiveData.value = batteryValue
            _batteryLevel.value = "Batería: $batteryValue%"

            if (batteryValue < 20) {
                addAlert(DogAlert(
                    id = System.currentTimeMillis(),
                    type = AlertType.BATTERY_LOW,
                    message = "Batería del dispositivo baja ($batteryValue%)",
                    severity = 2,
                    recommendedAction = "Recargar el dispositivo DogFit"
                ))
            }

            Log.d("DogFitViewModel", "✅ Batería actualizada: $batteryValue%")
        }
    }

    private fun updateSteps(newSteps: Int) {
        val currentStepsText = _stepsCount.value ?: "Pasos hoy: 0"
        val currentSteps = currentStepsText.substringAfter(": ").toIntOrNull() ?: 0
        _stepsCount.value = "Pasos hoy: ${currentSteps + newSteps}"
    }

    private fun updateCalories(steps: Int) {
        val calories = steps * 0.05f
        val currentCaloriesText = _caloriesBurned.value ?: "Calorías: 0"
        val currentCalories = currentCaloriesText.substringAfter(": ").toFloatOrNull() ?: 0f
        _caloriesBurned.value = "Calorías: ${String.format("%.1f", currentCalories + calories)}"
    }

    private fun updateDistance(steps: Int) {
        val distance = calculateDistance(steps, _dogProfile.value?.breed ?: "Mixed")
        val currentDistanceText = _distanceToday.value ?: "Distancia: 0 km"
        val currentDistance = currentDistanceText.substringAfter(": ").substringBefore(" ").toFloatOrNull() ?: 0f
        _distanceToday.value = "Distancia: ${String.format("%.2f", currentDistance + distance)} km"
    }

    // 🔥 GETTERS para MainActivity
    fun getActivityValue(): Int? = _activityValue.value
    fun getBatteryValue(): Int? = _batteryValue.value

    fun getActivityState(): String = _activityState.value ?: "Actividad: --"
    fun getBatteryLevel(): String = _batteryLevel.value ?: "Batería: --%"
    fun getStepsCount(): String = _stepsCount.value ?: "Pasos hoy: 0"
    fun getCaloriesBurned(): String = _caloriesBurned.value ?: "Calorías: 0"
    fun getDistanceToday(): String = _distanceToday.value ?: "Distancia: 0 km"

    // 🔥 FUNCIONES para Compose (StateFlow)
    fun getActivityFlow(): StateFlow<Int?> = _activityValue.asStateFlow()
    fun getBatteryFlow(): StateFlow<Int?> = _batteryValue.asStateFlow()

    // Funciones existentes (se mantienen igual)
    fun updateDogProfile(profile: DogProfile) {
        _dogProfile.value = profile

        val recommendation = breedRecommendations[profile.breed]
        if (recommendation != null) {
            addAlert(DogAlert(
                id = System.currentTimeMillis(),
                type = AlertType.HEALTH_TIP,
                message = "Recomendación para ${profile.breed}: ${recommendation.healthTips.firstOrNull()}",
                severity = 1,
                recommendedAction = "Ver más consejos en Perfil"
            ))
        }
    }

    private fun loadMockData() {
        val calendar = Calendar.getInstance()
        val mockData = mutableListOf<DogActivityData>()

        for (i in 29 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            for (hour in 6..22) {
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, (0..59).random())

                val activityType = when {
                    hour in 6..8 || hour in 17..19 -> 2
                    hour in 9..12 || hour in 14..16 -> 1
                    hour in 13..14 -> 3
                    else -> 0
                }

                val steps = when (activityType) {
                    2 -> (200..500).random()
                    1 -> (50..200).random()
                    3 -> (100..300).random()
                    else -> (0..20).random()
                }

                val calories = steps * 0.05f
                val distance = calculateDistance(steps, "Mixed")

                mockData.add(
                    DogActivityData(
                        id = calendar.timeInMillis + hour * 1000L,
                        timestamp = calendar.timeInMillis,
                        activityType = activityType,
                        intensity = when(activityType) {
                            2 -> 0.9f
                            1 -> 0.5f
                            3 -> 0.7f
                            else -> 0.1f
                        },
                        steps = steps,
                        estimatedDistance = distance * 1000,
                        calories = calories,
                        temperature = (15..30).random().toFloat()
                    )
                )
            }
        }

        _activityHistory.value = mockData
        updateAllStats()
    }

    private fun updateAllStats() {
        updateDailyStats()
        updateWeeklyStats()
        updateMonthlyStats()
    }

    private fun updateDailyStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayData = _activityHistory.value?.filter {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) == today
        } ?: emptyList()

        val totalSteps = todayData.sumOf { it.steps }
        val activeMinutes = todayData.count { it.activityType in 1..3 } * 5
        val restMinutes = todayData.count { it.activityType == 0 } * 5
        val distance = todayData.sumOf { it.estimatedDistance.toDouble() }.toFloat() / 1000
        val calories = todayData.sumOf { it.calories.toDouble() }.toFloat()

        val activityDistribution = mutableMapOf<String, Int>()
        todayData.forEach { data ->
            val activityName = when (data.activityType) {
                0 -> "Reposo"
                1 -> "Caminata"
                2 -> "Carrera"
                3 -> "Juego"
                else -> "Desconocido"
            }
            activityDistribution[activityName] = (activityDistribution[activityName] ?: 0) + 5
        }

        val hourGroups = todayData.groupBy {
            SimpleDateFormat("HH", Locale.getDefault()).format(Date(it.timestamp)).toInt()
        }
        val peakHour = hourGroups.maxByOrNull { it.value.size }?.key ?: 0

        val targetDailySteps = _dogProfile.value?.targetDailySteps ?: 5000
        val goalPercentage = if (targetDailySteps > 0) {
            (totalSteps.toFloat() / targetDailySteps) * 100
        } else {
            0f
        }

        val wellnessScore = calculateWellnessScore(
            activity = totalSteps.toFloat() / targetDailySteps,
            sleep = (restMinutes.toFloat() / (24 * 60)) * 2,
            consistency = 0.8f
        )

        _dailyStats.value = DailySummary(
            date = today,
            totalSteps = totalSteps,
            activeMinutes = activeMinutes,
            restMinutes = restMinutes,
            distanceKm = distance,
            caloriesBurned = calories,
            peakActivityHour = String.format("%02d:00", peakHour),
            longestWalk = if (todayData.isNotEmpty()) 30 else 0,
            activityDistribution = activityDistribution,
            goalAchieved = goalPercentage >= 100,
            wellnessScore = wellnessScore
        )
    }

    private fun updateWeeklyStats() {
        val calendar = Calendar.getInstance()
        val weekData = mutableListOf<DailySummary>()

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayData = _activityHistory.value?.filter {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) == dateStr
            } ?: emptyList()

            val totalSteps = dayData.sumOf { it.steps }
            val activeMinutes = dayData.count { it.activityType in 1..3 } * 5
            val distance = dayData.sumOf { it.estimatedDistance.toDouble() }.toFloat() / 1000
            val calories = dayData.sumOf { it.calories.toDouble() }.toFloat()

            val activityDistribution = mutableMapOf<String, Int>()
            dayData.forEach { data ->
                val activityName = when (data.activityType) {
                    0 -> "Reposo"
                    1 -> "Caminata"
                    2 -> "Carrera"
                    3 -> "Juego"
                    else -> "Desconocido"
                }
                activityDistribution[activityName] = (activityDistribution[activityName] ?: 0) + 5
            }

            weekData.add(
                DailySummary(
                    date = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time),
                    totalSteps = totalSteps,
                    activeMinutes = activeMinutes,
                    restMinutes = (24*60) - activeMinutes,
                    distanceKm = distance,
                    caloriesBurned = calories,
                    peakActivityHour = "12:00",
                    longestWalk = 30,
                    activityDistribution = activityDistribution,
                    goalAchieved = totalSteps >= (_dogProfile.value?.targetDailySteps ?: 5000),
                    wellnessScore = 75
                )
            )
        }

        val totalSteps = weekData.sumOf { it.totalSteps }
        val avgDailySteps = if (weekData.isNotEmpty()) totalSteps / weekData.size else 0
        val totalDistance = weekData.sumOf { it.distanceKm.toDouble() }.toFloat()
        val totalCalories = weekData.sumOf { it.caloriesBurned.toDouble() }.toFloat()
        val activeDays = weekData.count {
            val target = _dogProfile.value?.targetDailySteps ?: 5000
            it.totalSteps > target * 0.5
        }
        val bestDay = weekData.maxByOrNull { it.totalSteps }

        val consistencyScore = if (weekData.isNotEmpty()) {
            val avg = weekData.map { it.totalSteps }.average()
            val variance = weekData.map { Math.pow(it.totalSteps - avg, 2.0) }.average()
            (100 - (variance / avg * 100).toFloat()).coerceIn(0f, 100f)
        } else 0f

        _weeklyStats.value = WeeklySummary(
            weekNumber = calendar.get(Calendar.WEEK_OF_YEAR),
            weekRange = "${weekData.first().date} - ${weekData.last().date}",
            totalSteps = totalSteps,
            avgDailySteps = avgDailySteps,
            totalDistance = totalDistance,
            totalCalories = totalCalories,
            activeDays = activeDays,
            restDays = weekData.size - activeDays,
            trend = if (weekData.last().totalSteps > weekData.first().totalSteps) "↑" else "↓",
            bestDay = bestDay,
            consistencyScore = consistencyScore
        )
    }

    private fun updateMonthlyStats() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val monthData = _activityHistory.value?.filter {
            val dataCalendar = Calendar.getInstance().apply { time = Date(it.timestamp) }
            dataCalendar.get(Calendar.MONTH) == month &&
                    dataCalendar.get(Calendar.YEAR) == year
        } ?: emptyList()

        val dailyGroups = monthData.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
        }

        val dailyTotals = dailyGroups.map { (_, data) ->
            data.sumOf { it.steps }
        }

        val totalSteps = dailyTotals.sum()
        val avgDailySteps = if (dailyTotals.isNotEmpty()) totalSteps / dailyTotals.size else 0
        val totalDistance = monthData.sumOf { it.estimatedDistance.toDouble() }.toFloat() / 1000
        val activeDays = dailyTotals.count {
            val target = _dogProfile.value?.targetDailySteps ?: 5000
            it > target * 0.5
        }

        var currentStreak = 0
        var longestStreak = 0
        val sortedDates = dailyGroups.keys.sorted()

        for (date in sortedDates) {
            val steps = dailyGroups[date]?.sumOf { it.steps } ?: 0
            val target = _dogProfile.value?.targetDailySteps ?: 5000
            if (steps > target * 0.5) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        val consistencyScore = if (dailyTotals.isNotEmpty()) {
            val avg = dailyTotals.average()
            val variance = dailyTotals.map { Math.pow(it - avg, 2.0) }.average()
            (100 - (variance / avg * 100).toFloat()).coerceIn(0f, 100f)
        } else 0f

        _monthlyStats.value = MonthlySummary(
            month = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()),
            year = year,
            totalSteps = totalSteps,
            avgDailySteps = avgDailySteps,
            totalDistance = totalDistance,
            activeDays = activeDays,
            longestActiveStreak = longestStreak,
            consistencyScore = consistencyScore,
            comparisonWithLastMonth = 15.5f,
            monthlyGoalAchieved = totalSteps >= ((_dogProfile.value?.targetDailySteps ?: 5000) * 30)
        )
    }

    fun getBreedRecommendation(breed: String): BreedRecommendation? {
        return breedRecommendations[breed]
    }

    fun getAllBreeds(): List<String> {
        return breedRecommendations.keys.toList()
    }

    fun getWeeklyStats(): List<DailySummary> {
        val calendar = Calendar.getInstance()
        val weeklyStats = mutableListOf<DailySummary>()

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayData = _activityHistory.value?.filter {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) == dateStr
            } ?: emptyList()

            val totalSteps = dayData.sumOf { it.steps }
            val activeMinutes = dayData.count { it.activityType in 1..3 } * 5

            val activityDistribution = mutableMapOf<String, Int>()
            dayData.forEach { data ->
                val activityName = when (data.activityType) {
                    0 -> "Reposo"
                    1 -> "Caminata"
                    2 -> "Carrera"
                    3 -> "Juego"
                    else -> "Desconocido"
                }
                activityDistribution[activityName] = (activityDistribution[activityName] ?: 0) + 5
            }

            weeklyStats.add(
                DailySummary(
                    date = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time),
                    totalSteps = totalSteps,
                    activeMinutes = activeMinutes,
                    restMinutes = (24*60) - activeMinutes,
                    distanceKm = dayData.sumOf { it.estimatedDistance.toDouble() }.toFloat() / 1000,
                    caloriesBurned = dayData.sumOf { it.calories.toDouble() }.toFloat(),
                    peakActivityHour = "12:00",
                    longestWalk = 30,
                    activityDistribution = activityDistribution,
                    goalAchieved = totalSteps >= (_dogProfile.value?.targetDailySteps ?: 5000),
                    wellnessScore = 75
                )
            )
        }

        return weeklyStats
    }

    private fun calculateDistance(steps: Int, breed: String): Float {
        val stepLength = when(breed) {
            "Labrador", "Golden Retriever", "Pastor Alemán" -> 0.65f
            "Bulldog" -> 0.40f
            "Chihuahua" -> 0.25f
            else -> 0.50f
        }
        return steps * stepLength / 1000
    }

    private fun calculateWellnessScore(activity: Float, sleep: Float, consistency: Float): Int {
        val score = ((activity.coerceIn(0f, 1.5f) * 0.5f) +
                (sleep.coerceIn(0f, 1f) * 0.3f) +
                (consistency * 0.2f)) * 100
        return score.toInt().coerceIn(0, 100)
    }

    private fun checkAlerts(newActivity: DogActivityData) {
        val dailyStats = _dailyStats.value ?: return
        val profile = _dogProfile.value ?: return
        val target = profile.targetDailySteps

        if (dailyStats.totalSteps < target * 0.3) {
            addAlert(DogAlert(
                id = System.currentTimeMillis(),
                type = AlertType.LOW_ACTIVITY,
                message = "Actividad baja hoy: solo ${dailyStats.totalSteps} pasos",
                severity = 2,
                recommendedAction = "Programar paseo extra de 15 minutos"
            ))
        }

        if (dailyStats.totalSteps >= target && dailyStats.goalAchieved) {
            addAlert(DogAlert(
                id = System.currentTimeMillis(),
                type = AlertType.GOAL_ACHIEVED,
                message = "🎉 ¡Objetivo diario alcanzado! ${dailyStats.totalSteps} pasos",
                severity = 1,
                recommendedAction = "¡Buen trabajo! Mañana seguimos así"
            ))
        }

        if (newActivity.activityType == 2 && newActivity.intensity > 0.8f) {
            addAlert(DogAlert(
                id = System.currentTimeMillis(),
                type = AlertType.OVEREXERTION,
                message = "Actividad intensa detectada",
                severity = 2,
                recommendedAction = "Proporcionar agua y descanso"
            ))
        }
    }

    private fun addAlert(alert: DogAlert) {
        val currentAlerts = _alerts.value?.toMutableList() ?: mutableListOf()
        if (!currentAlerts.any { it.type == alert.type && it.message == alert.message }) {
            currentAlerts.add(0, alert)
            _alerts.value = currentAlerts.take(20)
        }
    }

    private fun generateInitialAlerts() {
        val initialAlerts = listOf(
            DogAlert(
                id = System.currentTimeMillis(),
                type = AlertType.HEALTH_TIP,
                message = "💡 Recuerda: Los perros necesitan ejercicio diario para su bienestar",
                severity = 1
            ),
            DogAlert(
                id = System.currentTimeMillis() + 1,
                type = AlertType.VET_REMINDER,
                message = "📅 Próxima visita al veterinario recomendada en 2 semanas",
                severity = 2,
                recommendedAction = "Agendar cita"
            ),
            DogAlert(
                id = System.currentTimeMillis() + 2,
                type = AlertType.HEALTH_TIP,
                message = "🐕 ¡Bienvenido a DogFit! Configura el perfil de tu mascota",
                severity = 1
            )
        )

        _alerts.value = initialAlerts
    }

    fun markAlertAsRead(alertId: Long) {
        val currentAlerts = _alerts.value?.toMutableList() ?: return
        val index = currentAlerts.indexOfFirst { it.id == alertId }
        if (index != -1) {
            currentAlerts[index] = currentAlerts[index].copy(isRead = true)
            _alerts.value = currentAlerts
        }
    }

    fun clearAllAlerts() {
        _alerts.value = emptyList()
    }

    fun getHealthMetrics(): DogHealthMetrics {
        val dailyStats = _dailyStats.value ?: return DogHealthMetrics(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            dailyActivityGoal = 5000,
            activityLevel = "Moderado",
            sleepHours = 12f,
            deepSleepPercentage = 60f,
            sleepQuality = "Buena",
            restlessnessScore = 3,
            playTime = 45,
            wellnessScore = 75
        )

        val activityLevel = when (dailyStats.totalSteps) {
            in 0..3000 -> "Bajo"
            in 3001..7000 -> "Moderado"
            in 7001..10000 -> "Alto"
            else -> "Muy Alto"
        }

        return DogHealthMetrics(
            date = dailyStats.date,
            dailyActivityGoal = _dogProfile.value?.targetDailySteps ?: 5000,
            activityLevel = activityLevel,
            sleepHours = dailyStats.restMinutes / 60f,
            deepSleepPercentage = 60f,
            sleepQuality = if (dailyStats.restMinutes > 600) "Excelente" else "Buena",
            restlessnessScore = 3,
            playTime = dailyStats.activityDistribution["Juego"] ?: 0,
            wellnessScore = dailyStats.wellnessScore
        )
    }

    private val _gpsRoutes = MutableLiveData<List<GpsLocation>>(emptyList())
    val gpsRoutes: LiveData<List<GpsLocation>> = _gpsRoutes

    fun addGpsLocation(lat: Double, lng: Double, date: String, time: String) {
        viewModelScope.launch {
            val location = GpsLocation(
                id = System.currentTimeMillis(),
                latitude = lat,
                longitude = lng,
                timestamp = System.currentTimeMillis(),
                date = date,
                time = time
            )
            val currentList = _gpsRoutes.value?.toMutableList() ?: mutableListOf()
            currentList.add(location)

            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            _gpsRoutes.value = currentList.filter { it.timestamp >= sevenDaysAgo }

            Log.d("DogFitViewModel", "GPS location added: $lat, $lng")
        }
    }

    fun updateStepsFromBle(steps: Int) {
        viewModelScope.launch {
            val normalizedSteps = steps.coerceAtLeast(0)
            val previousSteps = _stepsCount.value?.substringAfter(": ")?.toIntOrNull() ?: 0
            _stepsCount.value = "Pasos hoy: $normalizedSteps"

            val deltaSteps = (normalizedSteps - previousSteps).coerceAtLeast(0)
            if (deltaSteps > 0) {
                val activityType = _activityValue.value ?: 0
                val newData = DogActivityData(
                    id = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis(),
                    activityType = activityType,
                    intensity = when (activityType) {
                        0 -> 0.1f
                        1 -> 0.5f
                        2 -> 0.9f
                        3 -> 0.7f
                        else -> 0.3f
                    },
                    steps = deltaSteps,
                    estimatedDistance = calculateDistance(deltaSteps, _dogProfile.value?.breed ?: "Mixed") * 1000,
                    calories = deltaSteps * 0.05f
                )

                val currentList = _activityHistory.value?.toMutableList() ?: mutableListOf()
                currentList.add(newData)
                _activityHistory.value = currentList

                updateCalories(deltaSteps)
                updateDistance(deltaSteps)
                checkAlerts(newData)
            }

            updateAllStats()
        }
    }

    fun addVaccination(vaccination: Vaccination) {
        val currentProfile = _dogProfile.value ?: return
        val currentRecord = currentProfile.medicalRecord
        val updatedVaccinations = currentRecord.vaccinations + vaccination
        val updatedRecord = currentRecord.copy(vaccinations = updatedVaccinations)
        _dogProfile.value = currentProfile.copy(medicalRecord = updatedRecord)

        NotificationHelper.scheduleMedicalReminder(
            context,
            "Vacuna",
            vaccination.name,
            vaccination.nextDueDate.time
        )
        checkMedicalReminders()
    }

    fun addDeworming(deworming: Deworming) {
        val currentProfile = _dogProfile.value ?: return
        val currentRecord = currentProfile.medicalRecord
        val updatedDewormings = currentRecord.dewormings + deworming
        val updatedRecord = currentRecord.copy(dewormings = updatedDewormings)
        _dogProfile.value = currentProfile.copy(medicalRecord = updatedRecord)

        NotificationHelper.scheduleMedicalReminder(
            context,
            "Desparasitación",
            deworming.productName,
            deworming.nextDueDate.time
        )
        checkMedicalReminders()
    }

    private fun checkMedicalReminders() {
        val profile = _dogProfile.value ?: return
        val now = System.currentTimeMillis()
        val oneWeek = 7L * 24 * 60 * 60 * 1000
        val oneDay = 24 * 60 * 60 * 1000

        profile.medicalRecord.vaccinations.forEach { vaccination ->
            val timeUntilDue = vaccination.nextDueDate.time - now
            when {
                timeUntilDue in 0..oneDay -> {
                    addAlert(DogAlert(
                        id = System.currentTimeMillis(),
                        type = AlertType.VET_REMINDER,
                        message = "¡HOY vence vacuna ${vaccination.name}!",
                        severity = 3,
                        recommendedAction = "Aplicar vacuna inmediatamente"
                    ))
                    NotificationHelper.showMedicalReminder(
                        context,
                        "¡URGENTE! ${vaccination.name}",
                        "La vacuna vence HOY",
                        vaccination.id.toInt()
                    )
                }
                timeUntilDue in oneDay..oneWeek -> {
                    addAlert(DogAlert(
                        id = System.currentTimeMillis(),
                        type = AlertType.VET_REMINDER,
                        message = "Vacuna ${vaccination.name} vence en ${(timeUntilDue / oneDay).toInt()} días",
                        severity = 2,
                        recommendedAction = "Agendar cita con veterinario"
                    ))
                }
            }
        }

        profile.medicalRecord.dewormings.forEach { deworming ->
            val timeUntilDue = deworming.nextDueDate.time - now
            when {
                timeUntilDue in 0..oneDay -> {
                    addAlert(DogAlert(
                        id = System.currentTimeMillis(),
                        type = AlertType.VET_REMINDER,
                        message = "¡HOY vence desparasitación ${deworming.productName}!",
                        severity = 3,
                        recommendedAction = "Aplicar desparasitante"
                    ))
                    NotificationHelper.showMedicalReminder(
                        context,
                        "¡URGENTE! ${deworming.productName}",
                        "La desparasitación vence HOY",
                        deworming.id.toInt()
                    )
                }
                timeUntilDue in oneDay..oneWeek -> {
                    addAlert(DogAlert(
                        id = System.currentTimeMillis(),
                        type = AlertType.VET_REMINDER,
                        message = "Desparasitación ${deworming.productName} vence en ${(timeUntilDue / oneDay).toInt()} días",
                        severity = 2,
                        recommendedAction = "Preparar desparasitante"
                    ))
                }
            }
        }
    }

    fun getRoutesForDay(date: String): List<GpsLocation> {
        return _gpsRoutes.value?.filter { it.date == date } ?: emptyList()
    }

    fun getLast7DaysRoutes(): Map<String, List<GpsLocation>> {
        val routes = _gpsRoutes.value ?: return emptyMap()
        return routes.groupBy { it.date }
    }

    private fun Log.d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }
}