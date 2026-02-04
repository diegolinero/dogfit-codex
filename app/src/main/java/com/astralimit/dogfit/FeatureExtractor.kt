package com.astralimit.dogfit

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Características estadísticas extraídas de una ventana de muestras de aceleración.
 * Estas métricas permiten diferenciar patrones de movimiento más allá del promedio simple.
 */
data class ActivityFeatures(
    val mean: Float,       // Media: nivel base de actividad
    val std: Float,        // Desviación estándar: variabilidad del movimiento
    val variance: Float,   // Varianza: dispersión de la señal (std²)
    val rms: Float,        // RMS: energía efectiva del movimiento
    val peak: Float,       // Pico máximo: intensidad máxima en la ventana
    val energy: Float      // Energía total: suma de cuadrados normalizada
) {
    companion object {
        val EMPTY = ActivityFeatures(0f, 0f, 0f, 0f, 0f, 0f)
    }
}

/**
 * Datos de calibración para una actividad específica.
 * Almacena todas las métricas estadísticas necesarias para la clasificación.
 */
data class CalibrationFeatures(
    val mean: Float = 0f,
    val std: Float = 0f,
    val variance: Float = 0f,
    val rms: Float = 0f,
    val peak: Float = 0f,
    val energy: Float = 0f
) {
    fun toFloatArray(): FloatArray = floatArrayOf(mean, std, variance, rms, peak, energy)

    companion object {
        fun fromFloatArray(arr: FloatArray): CalibrationFeatures {
            return if (arr.size >= 6) {
                CalibrationFeatures(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5])
            } else {
                CalibrationFeatures()
            }
        }

        val EMPTY = CalibrationFeatures()
    }
}

/**
 * Extractor de características estadísticas de señales de aceleración.
 * Calcula métricas que caracterizan el patrón de movimiento en una ventana temporal.
 */
object FeatureExtractor {

    /**
     * Extrae todas las características estadísticas de una lista de muestras.
     * @param samples Lista de magnitudes de aceleración (ya filtradas)
     * @return ActivityFeatures con todas las métricas calculadas
     */
    fun extract(samples: List<Float>): ActivityFeatures {
        if (samples.isEmpty()) return ActivityFeatures.EMPTY

        val n = samples.size
        val mean = samples.average().toFloat()

        var sumSqDiff = 0f
        var sumSq = 0f
        var maxVal = 0f

        for (s in samples) {
            sumSqDiff += (s - mean).pow(2)
            sumSq += s.pow(2)
            if (kotlin.math.abs(s) > maxVal) maxVal = kotlin.math.abs(s)
        }

        val variance = sumSqDiff / n
        val std = sqrt(variance)
        val rms = sqrt(sumSq / n)
        val energy = sumSq / n

        return ActivityFeatures(mean, std, variance, rms, maxVal, energy)
    }

    /**
     * Calcula la distancia euclidiana entre dos conjuntos de características.
     * Útil para comparar ventanas actuales con patrones de calibración.
     */
    fun distance(current: ActivityFeatures, reference: CalibrationFeatures): Float {
        val dMean = current.mean - reference.mean
        val dStd = current.std - reference.std
        val dRms = current.rms - reference.rms
        val dEnergy = current.energy - reference.energy

        return sqrt(dMean * dMean + dStd * dStd + dRms * dRms + dEnergy * dEnergy)
    }
}
