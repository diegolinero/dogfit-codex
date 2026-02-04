package com.astralimit.dogfit.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "vaccinations")
data class VaccinationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val applicationDate: Date,
    val nextDueDate: Date,
    val veterinarianName: String = "",
    val clinic: String = "",
    val batchNumber: String = "",
    val notes: String = ""
)

@Entity(tableName = "dewormings")
data class DewormingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val applicationDate: Date,
    val nextDueDate: Date,
    val dosage: String = "",
    val weight: Float = 0f,
    val veterinarianName: String = "",
    val notes: String = ""
)
