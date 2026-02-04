package com.astralimit.dogfit

import kotlin.math.sqrt

/**
 * Clasificador de actividad para mascotas basado en extracción de características estadísticas.
 *
 * Mejoras sobre la versión anterior:
 * - Usa múltiples métricas (mean, std, variance, rms, peak, energy) en lugar de solo promedio
 * - Ventana temporal real (~1 segundo a 10Hz = 10 muestras)
 * - Clasificación basada en rangos calibrados, no valores fijos
 * - Majority voting entre ventanas para estabilidad
 * - Histéresis mejorada con contador de estabilidad
 */
class ActivityClassifier {

    companion object {
        const val ACTIVITY_REST = 0
        const val ACTIVITY_WALK = 1
        const val ACTIVITY_RUN = 2
        const val ACTIVITY_PLAY = 3

        private const val SAMPLE_RATE_HZ = 10
        private const val WINDOW_DURATION_SEC = 1
        private const val WINDOW_SIZE = SAMPLE_RATE_HZ * WINDOW_DURATION_SEC // 10 muestras = 1 segundo

        private const val STABILITY_THRESHOLD = 3 // Ventanas consecutivas para cambiar estado
        private const val VOTE_WINDOW_SIZE = 5    // Ventanas para majority voting
    }

    var sensitivityFactor: Float = 1.0f

    // Filtro Low-pass para suavizar ruido de alta frecuencia
    private var lastFilteredMagnitude = 0f
    private val ALPHA = 0.15f

    // Ventana de muestras filtradas
    private val samples = mutableListOf<Float>()

    // Historial de clasificaciones para majority voting
    private val classificationHistory = mutableListOf<Int>()

    // Estado actual con histéresis
    private var currentActivity = ACTIVITY_REST
    private var stableActivityCount = 0
    private var lastRawActivity = ACTIVITY_REST

    // Características de calibración por actividad
    private var restCalibration = CalibrationFeatures()
    private var walkCalibration = CalibrationFeatures()
    private var runCalibration = CalibrationFeatures()

    private var isCalibrated = false

    // Modo calibración
    private var isCalibrating = false
    private var calibrationActivity = ACTIVITY_REST
    private val calibrationSamples = mutableListOf<Float>()

    fun startCalibration(activityType: Int) {
        isCalibrating = true
        calibrationActivity = activityType
        calibrationSamples.clear()
    }

    fun stopCalibration(): CalibrationFeatures {
        isCalibrating = false
        val features = FeatureExtractor.extract(calibrationSamples)
        val result = CalibrationFeatures(
            mean = features.mean,
            std = features.std,
            variance = features.variance,
            rms = features.rms,
            peak = features.peak,
            energy = features.energy
        )
        calibrationSamples.clear()
        return result
    }

    /**
     * Aplica calibración completa con todas las métricas estadísticas.
     */
    fun applyFullCalibration(
        rest: CalibrationFeatures,
        walk: CalibrationFeatures,
        run: CalibrationFeatures
    ) {
        restCalibration = rest
        walkCalibration = walk
        runCalibration = run
        isCalibrated = true
    }

    /**
     * Compatibilidad con calibración simple (3 valores promedio).
     * Estima las demás métricas a partir de los promedios.
     */
    fun applyCalibration(restValue: Float, walkValue: Float, runValue: Float) {
        restCalibration = CalibrationFeatures(
            mean = restValue,
            std = restValue * 0.3f,
            variance = (restValue * 0.3f) * (restValue * 0.3f),
            rms = restValue * 1.1f,
            peak = restValue * 1.5f,
            energy = restValue * restValue
        )
        walkCalibration = CalibrationFeatures(
            mean = walkValue,
            std = walkValue * 0.4f,
            variance = (walkValue * 0.4f) * (walkValue * 0.4f),
            rms = walkValue * 1.1f,
            peak = walkValue * 2.0f,
            energy = walkValue * walkValue
        )
        runCalibration = CalibrationFeatures(
            mean = runValue,
            std = runValue * 0.5f,
            variance = (runValue * 0.5f) * (runValue * 0.5f),
            rms = runValue * 1.1f,
            peak = runValue * 2.5f,
            energy = runValue * runValue
        )
        isCalibrated = true
    }

    fun getCalibrationValues(): FloatArray = floatArrayOf(
        restCalibration.mean, walkCalibration.mean, runCalibration.mean
    )

    /**
     * Devuelve la última magnitud procesada (después de filtros).
     */
    fun getLastMagnitude(): Float = lastFilteredMagnitude

    /**
     * Procesa una nueva muestra de aceleración.
     * @return El tipo de actividad estable detectada
     */
    fun processSample(ax: Float, ay: Float, az: Float): Int {
        // 1. Calcular magnitud del vector de aceleración
        val rawMagnitude = sqrt(ax * ax + ay * ay + az * az)

        // 2. Eliminar componente de gravedad (1g en reposo)
        val linearAcceleration = kotlin.math.abs(rawMagnitude - 1.0f)

        // 3. Aplicar filtro Low-pass para eliminar ruido de alta frecuencia
        // El filtro suaviza picos de ruido eléctrico y vibraciones mecánicas
        lastFilteredMagnitude = lastFilteredMagnitude + ALPHA * (linearAcceleration - lastFilteredMagnitude)

        // 4. Añadir a ventana temporal
        samples.add(lastFilteredMagnitude)
        if (samples.size > WINDOW_SIZE) {
            samples.removeAt(0)
        }

        // Si estamos calibrando, acumular muestras sin clasificar
        if (isCalibrating) {
            calibrationSamples.add(lastFilteredMagnitude)
            return calibrationActivity
        }

        // 5. Extraer características de la ventana actual
        if (samples.size < WINDOW_SIZE) {
            return currentActivity // No hay suficientes muestras aún
        }

        val features = FeatureExtractor.extract(samples)

        // 6. Clasificar usando características y calibración
        val rawActivity = classifyByFeatures(features)

        // 7. Añadir a historial para majority voting
        classificationHistory.add(rawActivity)
        if (classificationHistory.size > VOTE_WINDOW_SIZE) {
            classificationHistory.removeAt(0)
        }

        // 8. Aplicar majority voting
        val votedActivity = majorityVote()

        // 9. Aplicar histéresis final
        return applyHysteresis(votedActivity)
    }

    /**
     * Clasifica la actividad basándose en múltiples características estadísticas.
     * Usa los rangos calibrados para cada actividad.
     */
    private fun classifyByFeatures(features: ActivityFeatures): Int {
        if (!isCalibrated) {
            // Fallback a clasificación simple si no hay calibración
            return classifyByMagnitudeOnly(features.mean)
        }

        // Aplicar factor de sensibilidad a los umbrales
        val sf = sensitivityFactor

        // REPOSO: varianza muy baja, energía baja, std baja
        // El perro está quieto, solo pequeñas vibraciones
        val isRest = features.variance < restCalibration.variance * 2.0f * sf &&
                features.energy < (restCalibration.energy + walkCalibration.energy) / 2f * sf &&
                features.std < restCalibration.std * 2.0f * sf

        if (isRest) return ACTIVITY_REST

        // JUGAR: picos muy altos, alta varianza, energía irregular
        // Movimientos erráticos con cambios bruscos de dirección
        val isPlaying = features.peak > runCalibration.peak * 1.3f / sf &&
                features.variance > runCalibration.variance * 1.2f / sf

        if (isPlaying) return ACTIVITY_PLAY

        // CORRER: energía alta, std alta, ritmo más constante que jugar
        val isRunning = features.energy > (walkCalibration.energy + runCalibration.energy) / 2f / sf &&
                features.rms > (walkCalibration.rms + runCalibration.rms) / 2f / sf

        if (isRunning) return ACTIVITY_RUN

        // CAMINAR: energía media, std media, patrón estable
        val isWalking = features.energy > restCalibration.energy * 1.5f / sf ||
                features.std > restCalibration.std * 1.5f / sf

        if (isWalking) return ACTIVITY_WALK

        return ACTIVITY_REST
    }

    /**
     * Clasificación simple por magnitud promedio (fallback sin calibración).
     */
    private fun classifyByMagnitudeOnly(avgMagnitude: Float): Int {
        val walkThreshold = 0.08f * sensitivityFactor
        val runThreshold = 0.25f * sensitivityFactor
        val playThreshold = 0.50f * sensitivityFactor

        return when {
            avgMagnitude > playThreshold -> ACTIVITY_PLAY
            avgMagnitude > runThreshold -> ACTIVITY_RUN
            avgMagnitude > walkThreshold -> ACTIVITY_WALK
            else -> ACTIVITY_REST
        }
    }

    /**
     * Majority voting: devuelve la actividad más frecuente en las últimas N ventanas.
     * Reduce cambios espurios por 1-2 ventanas ruidosas.
     */
    private fun majorityVote(): Int {
        if (classificationHistory.isEmpty()) return currentActivity

        val counts = IntArray(4)
        classificationHistory.forEach { counts[it]++ }

        var maxCount = 0
        var majority = currentActivity
        for (i in counts.indices) {
            if (counts[i] > maxCount) {
                maxCount = counts[i]
                majority = i
            }
        }
        return majority
    }

    /**
     * Histéresis: requiere N clasificaciones consecutivas iguales para cambiar de estado.
     * Evita oscilaciones entre estados cercanos.
     */
    private fun applyHysteresis(votedActivity: Int): Int {
        if (votedActivity == currentActivity) {
            stableActivityCount = 0
            lastRawActivity = votedActivity
            return currentActivity
        }

        if (votedActivity == lastRawActivity) {
            stableActivityCount++
        } else {
            stableActivityCount = 1
            lastRawActivity = votedActivity
        }

        if (stableActivityCount >= STABILITY_THRESHOLD) {
            currentActivity = votedActivity
            stableActivityCount = 0
        }

        return currentActivity
    }

    /**
     * Reinicia el estado del clasificador.
     */
    fun reset() {
        samples.clear()
        classificationHistory.clear()
        currentActivity = ACTIVITY_REST
        stableActivityCount = 0
        lastRawActivity = ACTIVITY_REST
        lastFilteredMagnitude = 0f
    }
}
