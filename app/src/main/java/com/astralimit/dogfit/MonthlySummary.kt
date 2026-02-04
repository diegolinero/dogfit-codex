package com.astralimit.dogfit

data class MonthlySummary(
    val month: String = "",
    val year: Int = 2026,
    val totalActiveMinutes: Int = 0,
    val avgDailyMinutes: Int = 0,
    val activeDays: Int = 0,
    val longestActiveStreak: Int = 0,
    val consistencyScore: Float = 0f,
    val comparisonWithLastMonth: Float = 0f,
    val monthlyGoalAchieved: Boolean = false
)
