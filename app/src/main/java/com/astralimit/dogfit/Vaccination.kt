package com.astralimit.dogfit

import java.util.Date

data class Vaccination(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val applicationDate: Date,
    val nextDueDate: Date,
    val veterinarianName: String = "",
    val clinic: String = "",
    val batchNumber: String = "",
    val notes: String = ""
)

data class Deworming(
    val id: Long = System.currentTimeMillis(),
    val productName: String,
    val applicationDate: Date,
    val nextDueDate: Date,
    val dosage: String = "",
    val weight: Float = 0f,
    val veterinarianName: String = "",
    val notes: String = ""
)

data class GpsLocation(
    val id: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val date: String,
    val time: String,
    val accuracy: Float = 0f
)

data class RouteDay(
    val date: String,
    val locations: List<GpsLocation>,
    val totalDistanceKm: Float,
    val durationMinutes: Int
)

data class PetMedicalRecord(
    val vaccinations: List<Vaccination> = emptyList(),
    val dewormings: List<Deworming> = emptyList(),
    val upcomingReminders: List<MedicalReminder> = emptyList()
)

data class MedicalReminder(
    val id: Long = System.currentTimeMillis(),
    val type: ReminderType,
    val title: String,
    val description: String,
    val dueDate: Date,
    val isWeekWarning: Boolean = false,
    val isDayWarning: Boolean = false
)

enum class ReminderType {
    VACCINATION,
    DEWORMING
}

enum class PetType {
    DOG,
    CAT
}
