package com.astralimit.dogfit.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dog_profiles")
data class DogProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    val name: String = "Maxi",
    val petType: String = "DOG",
    val breed: String = "Mixed",
    val birthDate: Long? = null,
    val age: Int = 3,
    val weight: Float = 15f,
    val weightHistory: String = "[]",
    val height: Float = 50f,
    val targetActiveMinutes: Int = 60,
    val dailyCalories: Int = 800,
    val imageUrl: String = "",
    val color: String = "",
    val microchipNumber: String = "",
    @ColumnInfo(defaultValue = "[]")
    val vetVisitsJson: String = "[]",
    @ColumnInfo(defaultValue = "1.0")
    val activitySensitivity: Float = 1.0f,
    @ColumnInfo(defaultValue = "0")
    val isCalibrated: Int = 0,
    @ColumnInfo(defaultValue = "0.10")
    val calibrationRestValue: Float = 0.10f,
    @ColumnInfo(defaultValue = "0.30")
    val calibrationWalkValue: Float = 0.30f,
    @ColumnInfo(defaultValue = "0.80")
    val calibrationRunValue: Float = 0.80f,
    @ColumnInfo(defaultValue = "")
    val calibrationRestFeaturesJson: String = "",
    @ColumnInfo(defaultValue = "")
    val calibrationWalkFeaturesJson: String = "",
    @ColumnInfo(defaultValue = "")
    val calibrationRunFeaturesJson: String = ""
)
