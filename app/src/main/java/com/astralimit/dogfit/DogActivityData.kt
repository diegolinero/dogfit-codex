package com.astralimit.dogfit

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "dog_activity")
data class DogActivityData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp)),
    val activityType: Int = 0, // 0=reposo, 1=caminata, 2=carrera, 3=juego
    val intensity: Float = 0f, // 0.0 a 1.0
    val durationMinutes: Int = 0,
    val steps: Int = 0,
    val estimatedDistance: Float = 0f,
    val calories: Float = 0f,
    val heartRate: Int? = null,
    val temperature: Float? = null // temperatura ambiente
) {
    fun getActivityTypeString(): String {
        return when(activityType) {
            0 -> "Reposo"
            1 -> "Caminata"
            2 -> "Carrera"
            3 -> "Juego"
            else -> "Desconocido"
        }
    }

    fun getActivityEmoji(): String {
        return when(activityType) {
            0 -> "🛌"
            1 -> "🚶"
            2 -> "🏃"
            3 -> "🎾"
            else -> "❓"
        }
    }
}
