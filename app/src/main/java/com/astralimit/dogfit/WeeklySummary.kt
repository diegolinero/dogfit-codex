package com.astralimit.dogfit

data class WeeklySummary(
    val weekNumber: Int = 1,
    val weekRange: String = "",
    val totalActiveMinutes: Int = 0,
    val avgDailyMinutes: Int = 0,
    val totalCalories: Float = 0f,
    val activeDays: Int = 0,
    val restDays: Int = 0,
    val trend: String = "",
    val bestDay: DailySummary? = null,
    val consistencyScore: Float = 0f
)
