package com.astralimit.dogfit

data class DogHealthMetrics(
    val date: String,
    val dailyActivityGoal: Int,
    val activityLevel: String,
    val sleepHours: Float,
    val deepSleepPercentage: Float,
    val sleepQuality: String,
    val restlessnessScore: Int,
    val playTime: Int,
    val wellnessScore: Int
)