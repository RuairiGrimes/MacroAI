package com.macroai.tracker.domain.util

import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.data.remote.gemini.dto.GeminiDayOverviewDto
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

import com.macroai.tracker.domain.model.WeightUnit

data class EndOfDayMetrics(
    val consumedCalories: Int,
    val dailyBudgetKcal: Int,
    val maintenanceExpenditureKcal: Int,
    val dailyCalorieDeficitKcal: Int, // Positive = calorie deficit, Negative = calorie surplus
    val budgetVarianceKcal: Int,      // Positive = under budget, Negative = over budget
    val currentWeightKg: Double,
    val projectedWeight4WeeksKg: Double,
    val weightChange4WeeksKg: Double,
    val fourWeekProjectionStatement: String,
    val weightUnit: WeightUnit = WeightUnit.KG
)

data class EndOfDayReport(
    val metrics: EndOfDayMetrics,
    val aiOverview: GeminiDayOverviewDto,
    val isAiGenerated: Boolean = true,
    val modelUsed: String? = null
)

object EndOfDayCalculator {

    const val DAYS_IN_4_WEEKS = 28
    const val KCAL_PER_KG_FAT = 7700.0

    /**
     * Calculates the daily calorie deficit and 4-week weight projection using Wishnofsky's rule
     * (7,700 kcal per 1 kg of body mass change over 28 days).
     */
    fun calculateMetrics(
        summary: DailyNutritionSummary,
        tdee: TdeeResult,
        weightUnit: WeightUnit = WeightUnit.KG
    ): EndOfDayMetrics {
        val consumedKcal = summary.totalCalories.roundToInt()
        val budgetKcal = tdee.energyTargetKcal.coerceAtLeast(800)
        val maintenanceKcal = tdee.tdeeKcal.coerceAtLeast(800)

        // Daily calorie deficit relative to true TDEE maintenance expenditure
        val dailyDeficit = maintenanceKcal - consumedKcal
        val budgetVariance = budgetKcal - consumedKcal

        val currentWeight = tdee.effectiveWeightKg.coerceAtLeast(30.0)
        val rawWeightChange4Weeks = -(dailyDeficit.toDouble() * DAYS_IN_4_WEEKS) / KCAL_PER_KG_FAT
        val roundedWeightChange = round(rawWeightChange4Weeks * 10.0) / 10.0
        val projectedWeight = (round((currentWeight + rawWeightChange4Weeks) * 10.0) / 10.0).coerceAtLeast(25.0)

        val formattedProjectedWeight = weightUnit.format(projectedWeight)
        val statement = "If you keep up with this, you'll weigh $formattedProjectedWeight in 4 weeks"

        return EndOfDayMetrics(
            consumedCalories = consumedKcal,
            dailyBudgetKcal = budgetKcal,
            maintenanceExpenditureKcal = maintenanceKcal,
            dailyCalorieDeficitKcal = dailyDeficit,
            budgetVarianceKcal = budgetVariance,
            currentWeightKg = round(currentWeight * 10.0) / 10.0,
            projectedWeight4WeeksKg = projectedWeight,
            weightChange4WeeksKg = roundedWeightChange,
            fourWeekProjectionStatement = statement,
            weightUnit = weightUnit
        )
    }

    /**
     * Generates an instantaneous, offline, deterministic End-of-Day report calculated locally on-device.
     */
    fun generateLocalReport(
        logs: List<FoodLogEntity>,
        summary: DailyNutritionSummary,
        tdee: TdeeResult,
        weightUnit: WeightUnit = WeightUnit.KG
    ): EndOfDayReport {
        val metrics = calculateMetrics(summary, tdee, weightUnit)
        val overview = generateFallbackOverview(logs, summary, tdee, metrics)
        return EndOfDayReport(
            metrics = metrics,
            aiOverview = overview,
            isAiGenerated = false,
            modelUsed = "On-Device Clinical Engine"
        )
    }

    /**
     * Builds the structured prompt payload for Gemini to evaluate the user's complete day of food.
     */
    fun buildGeminiDayPrompt(
        logs: List<FoodLogEntity>,
        summary: DailyNutritionSummary,
        tdee: TdeeResult,
        metrics: EndOfDayMetrics
    ): String {
        val mealLines = if (logs.isEmpty()) {
            "- No meals logged today."
        } else {
            logs.joinToString("\n") { log ->
                val novaTag = log.novaGroup?.let { "NOVA $it" } ?: "NOVA unrated"
                val oilTag = if (log.containsSeedOils) "Seed Oils: ${log.detectedSeedOils.ifBlank { "Yes" }}" else "Seed-Oil Free"
                "- [${log.mealType.displayName}] ${log.name} (${log.servingWeightGrams.roundToInt()}g): " +
                    "${log.calories.roundToInt()} kcal | P: ${log.proteinG.roundToInt()}g, C: ${log.carbsG.roundToInt()}g, F: ${log.fatG.roundToInt()}g " +
                    "(PUFA ${"%.1f".format(Locale.US, log.polyunsaturatedFatG)}g, Sat ${"%.1f".format(Locale.US, log.saturatedFatG)}g, Fibre ${"%.1f".format(Locale.US, log.fibreG)}g) | $novaTag | $oilTag"
            }
        }

        val deficitLabel = if (metrics.dailyCalorieDeficitKcal >= 0) {
            "${metrics.dailyCalorieDeficitKcal} kcal deficit"
        } else {
            "${abs(metrics.dailyCalorieDeficitKcal)} kcal surplus"
        }

        return """
            Evaluate the user's completed daily nutrition log and return a structured JSON assessment.
            
            USER BIOMETRICS & TARGETS:
            - Current Weight: ${metrics.currentWeightKg} kg
            - Maintenance Expenditure (TDEE): ${metrics.maintenanceExpenditureKcal} kcal
            - Daily Calorie Target Budget: ${metrics.dailyBudgetKcal} kcal
            - Daily Calorie Balance: $deficitLabel (Consumed ${metrics.consumedCalories} kcal)
            - 4-Week Weight Projection: ${metrics.fourWeekProjectionStatement} (${if (metrics.weightChange4WeeksKg <= 0) "${metrics.weightChange4WeeksKg} kg" else "+${metrics.weightChange4WeeksKg} kg"})
            - Macro Targets: Protein ${tdee.proteinTargetGrams}g | Carbs ${tdee.carbsTargetGrams}g | Fat ${tdee.fatTargetGrams}g
            
            DAILY CONSUMED TOTALS:
            - Total Calories: ${summary.totalCalories.roundToInt()} kcal (${summary.totalWeightGrams.roundToInt()}g total food weight)
            - Protein: ${summary.proteinG.roundToInt()}g / ${tdee.proteinTargetGrams}g (${((summary.proteinG / tdee.proteinTargetGrams.coerceAtLeast(1)) * 100).roundToInt()}% of target)
            - Carbs: ${summary.carbsG.roundToInt()}g / ${tdee.carbsTargetGrams}g
            - Fat: ${summary.fatG.roundToInt()}g / ${tdee.fatTargetGrams}g (Saturated: ${summary.saturatedFatG}g, PUFA: ${summary.polyunsaturatedFatG}g, MUFA: ${summary.monounsaturatedFatG}g)
            - Dietary Fibre: ${summary.fibreG}g | Total Sugar: ${summary.sugarG}g
            - Ultra-Processed Food (NOVA 4): ${summary.upfCaloriePercentage}% of calories (${summary.upfWeightGrams.roundToInt()}g, ${summary.upfItemCount} item(s))
            - Industrial Seed Oils: ${summary.seedOilItemCount} flagged item(s) (${summary.distinctSeedOilsToday.joinToString(", ").ifBlank { "None" }})
            - Micronutrients: Sodium ${summary.sodiumMg.roundToInt()}mg, Potassium ${summary.potassiumMg.roundToInt()}mg, Magnesium ${summary.magnesiumMg.roundToInt()}mg, Calcium ${summary.calciumMg.roundToInt()}mg, Iron ${summary.ironMg}mg, Vitamin C ${summary.vitaminCMg.roundToInt()}mg, Vitamin D ${summary.vitaminDUg}µg
            
            MEALS LOGGED TODAY (${logs.size} items):
            $mealLines
        """.trimIndent()
    }

    /**
     * Generates a deterministic clinical nutrition overview if offline or if Gemini API is unreachable.
     */
    fun generateFallbackOverview(
        logs: List<FoodLogEntity>,
        summary: DailyNutritionSummary,
        tdee: TdeeResult,
        metrics: EndOfDayMetrics
    ): GeminiDayOverviewDto {
        if (logs.isEmpty() && summary.totalCalories == 0.0) {
            return GeminiDayOverviewDto(
                overallScore = 0,
                scoreGrade = "N/A",
                headline = "No food items were logged for this day.",
                healthScore = 0,
                proteinAndMacroScore = 0,
                calorieControlScore = 0,
                executiveSummary = "Log your meals via AI Photo, Barcode Scan, or Database Search to receive a complete clinical breakdown of your protein, macros, UPF exposure, and seed oils.",
                keyWins = listOf("Ready to start tracking your nutrition."),
                actionableImprovements = listOf("Log your breakfast, lunch, dinner, and snacks before ending the day.")
            )
        }

        // 1. Health Score (0-100): UPF %, Seed Oils, Fibre
        val upfPenalty = (summary.upfCaloriePercentage * 0.8).roundToInt().coerceIn(0, 45)
        val seedOilPenalty = (summary.seedOilItemCount * 15).coerceIn(0, 35)
        val fibreBonus = ((summary.fibreG / 30.0) * 15.0).roundToInt().coerceIn(0, 15)
        val healthScore = (85 - upfPenalty - seedOilPenalty + fibreBonus).coerceIn(15, 100)

        // 2. Protein & Macro Score (0-100)
        val proteinRatio = summary.proteinG / tdee.proteinTargetGrams.coerceAtLeast(1).toDouble()
        val proteinScore = when {
            proteinRatio in 0.90..1.20 -> 96
            proteinRatio in 0.75..1.35 -> 84
            proteinRatio >= 0.60 -> 70
            else -> (proteinRatio * 100).roundToInt().coerceIn(20, 65)
        }

        // 3. Calorie Control Score (0-100)
        val budgetDiffAbs = abs(metrics.budgetVarianceKcal)
        val calorieControlScore = when {
            budgetDiffAbs <= 150 -> 96
            budgetDiffAbs <= 300 -> 86
            budgetDiffAbs <= 500 -> 74
            else -> (100 - (budgetDiffAbs / 15)).coerceIn(30, 68)
        }

        val overallScore = ((healthScore * 0.40) + (proteinScore * 0.35) + (calorieControlScore * 0.25))
            .roundToInt()
            .coerceIn(1, 100)

        val grade = scoreToGrade(overallScore)

        val wins = mutableListOf<String>()
        if (summary.seedOilItemCount == 0) {
            wins.add("100% free of industrial seed oils across all ${logs.size} logged item(s).")
        }
        if (summary.upfCaloriePercentage <= 15.0) {
            wins.add("Low Ultra-Processed Food intake (${"%.1f".format(Locale.US, summary.upfCaloriePercentage)}% NOVA 4 calories).")
        }
        if (proteinRatio >= 0.85) {
            wins.add("Strong protein intake at ${summary.proteinG.roundToInt()}g (${(proteinRatio * 100).roundToInt()}% of daily target).")
        }
        if (metrics.dailyCalorieDeficitKcal > 0) {
            wins.add("Achieved a ${metrics.dailyCalorieDeficitKcal} kcal daily deficit toward weight loss.")
        }
        if (wins.isEmpty()) {
            wins.add("Completed full day food logging with ${logs.size} tracked item(s).")
        }

        val improvements = mutableListOf<String>()
        if (proteinRatio < 0.85) {
            val gap = (tdee.proteinTargetGrams - summary.proteinG.roundToInt()).coerceAtLeast(10)
            improvements.add("Add ~${gap}g more lean protein (e.g., chicken breast, Greek yogurt, eggs, or fish) to hit your ${tdee.proteinTargetGrams}g target.")
        }
        if (summary.seedOilItemCount > 0) {
            improvements.add("Swap out the ${summary.seedOilItemCount} item(s) containing ${summary.distinctSeedOilsToday.joinToString(", ")} for olive oil, butter, or whole-food alternatives.")
        }
        if (summary.upfCaloriePercentage > 20.0) {
            improvements.add("Reduce NOVA 4 ultra-processed foods (currently ${"%.1f".format(Locale.US, summary.upfCaloriePercentage)}% of calories) in favor of single-ingredient staples.")
        }
        if (summary.fibreG < 25.0) {
            improvements.add("Increase dietary fibre (currently ${"%.1f".format(Locale.US, summary.fibreG)}g) with berries, oats, or leafy greens.")
        }
        if (improvements.isEmpty()) {
            improvements.add("Maintain this whole-food macro distribution and hydration tomorrow.")
        }

        val deficitText = if (metrics.dailyCalorieDeficitKcal >= 0) {
            "a ${metrics.dailyCalorieDeficitKcal} kcal calorie deficit"
        } else {
            "a ${abs(metrics.dailyCalorieDeficitKcal)} kcal calorie surplus"
        }

        return GeminiDayOverviewDto(
            overallScore = overallScore,
            scoreGrade = grade,
            headline = "Scored $overallScore/100 ($grade) with ${summary.proteinG.roundToInt()}g protein and $deficitText.",
            healthScore = healthScore,
            proteinAndMacroScore = proteinScore,
            calorieControlScore = calorieControlScore,
            executiveSummary = "You logged ${metrics.consumedCalories} kcal across ${logs.size} item(s) with ${summary.proteinG.roundToInt()}g protein, ${summary.carbsG.roundToInt()}g carbs, and ${summary.fatG.roundToInt()}g fat. " +
                "NOVA 4 ultra-processed foods accounted for ${"%.1f".format(Locale.US, summary.upfCaloriePercentage)}% of calories with ${summary.seedOilItemCount} seed-oil item(s). " +
                "${metrics.fourWeekProjectionStatement}.",
            keyWins = wins.take(4),
            actionableImprovements = improvements.take(3)
        )
    }

    fun scoreToGrade(score: Int): String = when {
        score >= 95 -> "A+"
        score >= 88 -> "A"
        score >= 82 -> "A-"
        score >= 76 -> "B+"
        score >= 70 -> "B"
        score >= 64 -> "B-"
        score >= 55 -> "C+"
        score >= 45 -> "C"
        else -> "D"
    }
}

