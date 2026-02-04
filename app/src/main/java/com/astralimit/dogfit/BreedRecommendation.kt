package com.astralimit.dogfit

data class BreedRecommendation(
    val breed: String,
    val recommendedDailySteps: IntRange,
    val recommendedPlayTime: Int,
    val energyLevel: String,
    val healthTips: List<String>
)