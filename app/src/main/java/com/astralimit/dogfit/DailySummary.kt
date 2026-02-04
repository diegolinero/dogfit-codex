package com.astralimit.dogfit

data class DailySummary(
    val date: String = "",
    val totalActiveMinutes: Int = 0,
    val activeMinutes: Int = 0,
    val restMinutes: Int = 0,
    val caloriesBurned: Float = 0f,
    val peakActivityHour: String = "--",
    val longestWalk: Int = 0,
    val activityDistribution: Map<String, Int> = emptyMap(),
    val goalAchieved: Boolean = false,
    val wellnessScore: Int = 75,
    val activityTimes: Map<Int, Long> = emptyMap()
)
