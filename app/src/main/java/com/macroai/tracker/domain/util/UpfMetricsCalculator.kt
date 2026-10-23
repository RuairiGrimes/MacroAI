package com.macroai.tracker.domain.util

import com.macroai.tracker.data.local.entity.FoodLogEntity
import kotlin.math.round

data class DailyNutritionSummary(
    val totalCalories: Double = 0.0,
    val totalWeightGrams: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val saturatedFatG: Double = 0.0,
    val polyunsaturatedFatG: Double = 0.0,
    val monounsaturatedFatG: Double = 0.0,
    val sugarG: Double = 0.0,
    val fibreG: Double = 0.0,

    // UPF (NOVA 4) metrics
    val upfCalories: Double = 0.0,
    val upfWeightGrams: Double = 0.0,
    val upfCaloriePercentage: Double = 0.0,
    val upfWeightPercentage: Double = 0.0,
    val upfItemCount: Int = 0,

    // Seed Oil metrics
    val seedOilItemCount: Int = 0,
    val distinctSeedOilsToday: List<String> = emptyList(),
    val highSeedOilItemCount: Int = 0,
    val moderateSeedOilItemCount: Int = 0,
    val traceSeedOilItemCount: Int = 0,
    val totalSeedOilGrams: Double = 0.0,
    val seedOilRiskLevel: SeedOilRiskLevel = SeedOilRiskLevel.GREEN,

    // Key Micronutrients
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDUg: Double = 0.0,
    val magnesiumMg: Double = 0.0
)

object UpfMetricsCalculator {

    fun summarizeDay(logs: List<FoodLogEntity>): DailyNutritionSummary {
        if (logs.isEmpty()) return DailyNutritionSummary()

        fun Double.round1(): Double = round(this * 10.0) / 10.0

        val totalKcal = logs.sumOf { it.calories }
        val totalGrams = logs.sumOf { it.servingWeightGrams }

        val upfLogs = logs.filter { it.isUpf || it.novaGroup == 4 }
        val upfKcal = upfLogs.sumOf { it.calories }
        val upfGrams = upfLogs.sumOf { it.servingWeightGrams }

        val upfKcalPct = if (totalKcal > 0.0) ((upfKcal / totalKcal) * 100.0).round1() else 0.0
        val upfWeightPct = if (totalGrams > 0.0) ((upfGrams / totalGrams) * 100.0).round1() else 0.0

        val seedOilLogs = logs.filter { it.containsSeedOils }
        val distinctOils = seedOilLogs
            .flatMap { it.detectedSeedOils.split(",") }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

        val seedOilAssessments = logs.map { it.seedOilAssessment() }
        val highCount = seedOilAssessments.count { it.riskLevel == SeedOilRiskLevel.RED }
        val modCount = seedOilAssessments.count { it.riskLevel == SeedOilRiskLevel.AMBER }
        val traceCount = seedOilAssessments.count { it.riskLevel == SeedOilRiskLevel.GREEN && it.containsSeedOils }
        val totalOilGrams = (seedOilAssessments.sumOf { it.estimatedGrams } * 10.0).let { round(it) / 10.0 }
        val dailyRiskLevel = when {
            highCount > 0 -> SeedOilRiskLevel.RED
            modCount > 0 -> SeedOilRiskLevel.AMBER
            else -> SeedOilRiskLevel.GREEN
        }

        return DailyNutritionSummary(
            totalCalories = totalKcal.round1(),
            totalWeightGrams = totalGrams.round1(),
            proteinG = logs.sumOf { it.proteinG }.round1(),
            carbsG = logs.sumOf { it.carbsG }.round1(),
            fatG = logs.sumOf { it.fatG }.round1(),
            saturatedFatG = logs.sumOf { it.saturatedFatG }.round1(),
            polyunsaturatedFatG = logs.sumOf { it.polyunsaturatedFatG }.round1(),
            monounsaturatedFatG = logs.sumOf { it.monounsaturatedFatG }.round1(),
            sugarG = logs.sumOf { it.sugarG }.round1(),
            fibreG = logs.sumOf { it.fibreG }.round1(),
            upfCalories = upfKcal.round1(),
            upfWeightGrams = upfGrams.round1(),
            upfCaloriePercentage = upfKcalPct,
            upfWeightPercentage = upfWeightPct,
            upfItemCount = upfLogs.size,
            seedOilItemCount = seedOilLogs.size,
            distinctSeedOilsToday = distinctOils,
            highSeedOilItemCount = highCount,
            moderateSeedOilItemCount = modCount,
            traceSeedOilItemCount = traceCount,
            totalSeedOilGrams = totalOilGrams,
            seedOilRiskLevel = dailyRiskLevel,
            sodiumMg = logs.sumOf { it.sodiumMg }.round1(),
            potassiumMg = logs.sumOf { it.potassiumMg }.round1(),
            ironMg = logs.sumOf { it.ironMg }.round1(),
            calciumMg = logs.sumOf { it.calciumMg }.round1(),
            vitaminCMg = logs.sumOf { it.vitaminCMg }.round1(),
            vitaminDUg = logs.sumOf { it.vitaminDUg }.round1(),
            magnesiumMg = logs.sumOf { it.magnesiumMg }.round1()
        )
    }
}

