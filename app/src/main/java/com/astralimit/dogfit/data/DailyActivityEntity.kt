package com.astralimit.dogfit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val steps: Int,
    val activeMinutes: Int,
    val calories: Float,
    val distance: Float,
    val activityTimesJson: String? = null // Almacena el desglose de tiempos por actividad
)
