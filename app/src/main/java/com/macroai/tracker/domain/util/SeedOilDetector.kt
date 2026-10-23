package com.macroai.tracker.domain.util

import com.macroai.tracker.data.remote.gemini.dto.GeminiIngredientEstimateDto
import com.macroai.tracker.domain.model.MealIngredient
import kotlin.math.round

enum class SeedOilRiskLevel(val displayName: String) {
    GREEN("Low / Clean"),
    AMBER("Moderate"),
    RED("High")
}

data class SeedOilDetectionResult(
    val containsSeedOils: Boolean,
    val detectedOils: List<String>,
    val detectedOilsCsv: String,
    val riskLevel: SeedOilRiskLevel = if (containsSeedOils) SeedOilRiskLevel.RED else SeedOilRiskLevel.GREEN,
    val estimatedGrams: Double = 0.0,
    val ratioPercent: Double = 0.0,
    val label: String = if (containsSeedOils) "Seed Oils: ${detectedOilsCsv.ifBlank { "Detected" }}" else "Seed-Oil Free",
    val isDeepFried: Boolean = false
)

object SeedOilDetector {

    private val SEED_OIL_PATTERNS: List<Pair<String, Regex>> = listOf(
        "rapeseed" to Regex("""\brapeseed(?:\s+oil)?\b""", RegexOption.IGNORE_CASE),
        "canola" to Regex("""\bcanola(?:\s+oil)?\b""", RegexOption.IGNORE_CASE),
        "sunflower" to Regex("""\bsunflower(?:\s+oil)?\b""", RegexOption.IGNORE_CASE),
        "soybean" to Regex("""\b(?:soybean(?:\s+oil)?|soya\s+oil)\b""", RegexOption.IGNORE_CASE),
        "cottonseed" to Regex("""\bcottonseed(?:\s+oil)?\b""", RegexOption.IGNORE_CASE),
        "corn oil" to Regex("""\bcorn\s+oil\b""", RegexOption.IGNORE_CASE),
        "safflower" to Regex("""\bsafflower(?:\s+oil)?\b""", RegexOption.IGNORE_CASE),
        "grapeseed" to Regex("""\bgrapeseed(?:\s+oil)?\b""", RegexOption.IGNORE_CASE),
        "vegetable oil" to Regex("""\bvegetable\s+oils?\b""", RegexOption.IGNORE_CASE)
    )

    private val DEEP_FRIED_PATTERNS = Regex(
        """\b(?:deep[\s-]?fried|deep[\s-]?fry(?:ing)?|french\s*fries|chippy|crisps|potato\s*chips|battered|tempura|fritters?|fish\s*and\s*chips|fried\s*chicken|nuggets?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val PURE_SEED_OIL_PATTERNS = Regex(
        """\b(?:rapeseed|canola|sunflower|soybean|cottonseed|corn|safflower|grapeseed|vegetable)\s+oils?\b""",
        RegexOption.IGNORE_CASE
    )

    private val PERCENTAGE_REGEX = Regex(
        """(?:rapeseed|canola|sunflower|soybean|cottonseed|corn|safflower|grapeseed|vegetable)(?:\s+oil)?\s*\(?(\d+(?:\.\d+)?)\s*%""",
        RegexOption.IGNORE_CASE
    )

    fun analyzeIngredients(
        ingredientsText: String?,
        servingWeightGrams: Double = 100.0,
        totalFatG: Double = 0.0,
        pufaG: Double = 0.0,
        foodName: String = ""
    ): SeedOilDetectionResult {
        if (ingredientsText.isNullOrBlank() && foodName.isBlank()) {
            return SeedOilDetectionResult(
                containsSeedOils = false,
                detectedOils = emptyList(),
                detectedOilsCsv = "",
                riskLevel = SeedOilRiskLevel.GREEN,
                estimatedGrams = 0.0,
                ratioPercent = 0.0,
                label = "Seed-Oil Free",
                isDeepFried = false
            )
        }

        val textToScan = "$foodName ${ingredientsText.orEmpty()}"
        val detected = SEED_OIL_PATTERNS.mapNotNull { (canonicalName, regex) ->
            if (regex.containsMatchIn(textToScan)) canonicalName else null
        }.distinct()

        val containsOils = detected.isNotEmpty()
        if (!containsOils) {
            return SeedOilDetectionResult(
                containsSeedOils = false,
                detectedOils = emptyList(),
                detectedOilsCsv = "",
                riskLevel = SeedOilRiskLevel.GREEN,
                estimatedGrams = 0.0,
                ratioPercent = 0.0,
                label = "Seed-Oil Free",
                isDeepFried = false
            )
        }

        val isDeepFried = DEEP_FRIED_PATTERNS.containsMatchIn(textToScan)
        val isPureOil = PURE_SEED_OIL_PATTERNS.containsMatchIn(foodName) && !foodName.contains("chicken", ignoreCase = true)

        val pctMatch = PERCENTAGE_REGEX.find(ingredientsText.orEmpty())
        val explicitPct = pctMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()

        val estimatedGrams: Double = when {
            explicitPct != null -> {
                (explicitPct / 100.0) * servingWeightGrams.coerceAtLeast(1.0)
            }
            isPureOil -> {
                if (totalFatG > 0.0) totalFatG else servingWeightGrams.coerceAtLeast(1.0)
            }
            isDeepFried -> {
                if (totalFatG > 0.0) totalFatG * 0.85 else servingWeightGrams * 0.16
            }
            totalFatG > 0.0 -> {
                if (pufaG > 0.0) {
                    (pufaG * 1.5).coerceIn(0.5, totalFatG * 0.75)
                } else {
                    (totalFatG * 0.45).coerceAtLeast(0.5)
                }
            }
            else -> 1.0 // Trace fallback
        }

        return evaluateRisk(
            containsSeedOils = true,
            detectedOils = detected,
            estimatedGrams = estimatedGrams,
            servingWeightGrams = servingWeightGrams,
            isDeepFried = isDeepFried
        )
    }

    /**
     * Merges AI-reported seed oils and regex-detected seed oils from ingredient/prompt text.
     */
    fun mergeDetections(
        aiContainsSeedOils: Boolean,
        aiDetectedOils: List<String>,
        textToScan: String?,
        servingWeightGrams: Double = 100.0,
        totalFatG: Double = 0.0,
        pufaG: Double = 0.0
    ): SeedOilDetectionResult {
        val regexResult = analyzeIngredients(
            ingredientsText = textToScan,
            servingWeightGrams = servingWeightGrams,
            totalFatG = totalFatG,
            pufaG = pufaG
        )
        val combined = (aiDetectedOils.map { it.trim().lowercase() }.filter { it.isNotEmpty() } +
            regexResult.detectedOils).distinct()

        val hasSeedOils = aiContainsSeedOils || combined.isNotEmpty()
        if (!hasSeedOils) {
            return SeedOilDetectionResult(
                containsSeedOils = false,
                detectedOils = emptyList(),
                detectedOilsCsv = "",
                riskLevel = SeedOilRiskLevel.GREEN,
                estimatedGrams = 0.0,
                ratioPercent = 0.0,
                label = "Seed-Oil Free",
                isDeepFried = false
            )
        }

        val isDeepFried = regexResult.isDeepFried || DEEP_FRIED_PATTERNS.containsMatchIn(textToScan.orEmpty())
        val estimatedGrams = if (regexResult.estimatedGrams > 0.0) {
            regexResult.estimatedGrams
        } else if (isDeepFried) {
            if (totalFatG > 0.0) totalFatG * 0.85 else 18.0
        } else if (totalFatG > 0.0) {
            (totalFatG * 0.45).coerceAtLeast(1.0)
        } else {
            1.5
        }

        return evaluateRisk(
            containsSeedOils = true,
            detectedOils = combined,
            estimatedGrams = estimatedGrams,
            servingWeightGrams = servingWeightGrams,
            isDeepFried = isDeepFried
        )
    }

    /**
     * Assesses a multi-component meal (from Room or recipes) using individual ingredients.
     */
    fun assessIngredients(
        ingredients: List<MealIngredient>,
        totalMealWeight: Double,
        totalMealFat: Double,
        mealName: String
    ): SeedOilDetectionResult {
        val detectedOilNames = mutableListOf<String>()
        var totalSeedOilGrams = 0.0
        var anyDeepFried = DEEP_FRIED_PATTERNS.containsMatchIn(mealName)

        for (item in ingredients) {
            val itemText = "${item.name} ${item.detectedSeedOils}"
            val check = analyzeIngredients(
                ingredientsText = itemText,
                servingWeightGrams = item.weightGrams,
                totalFatG = item.fatG,
                pufaG = item.polyunsaturatedFatG,
                foodName = item.name
            )

            if (check.containsSeedOils || item.containsSeedOils) {
                detectedOilNames.addAll(check.detectedOils)
                if (item.detectedSeedOils.isNotBlank()) {
                    detectedOilNames.addAll(item.detectedSeedOils.split(",").map { it.trim().lowercase() })
                }

                val isPureOil = PURE_SEED_OIL_PATTERNS.containsMatchIn(item.name)
                val isItemFried = check.isDeepFried || DEEP_FRIED_PATTERNS.containsMatchIn(item.name)
                if (isItemFried) anyDeepFried = true

                val itemGrams = when {
                    isPureOil -> item.weightGrams
                    isItemFried -> if (item.fatG > 0.0) item.fatG * 0.85 else item.weightGrams * 0.16
                    check.estimatedGrams > 0.0 -> check.estimatedGrams
                    item.fatG > 0.0 -> (item.fatG * 0.5).coerceAtLeast(0.5)
                    else -> 1.0
                }
                totalSeedOilGrams += itemGrams
            }
        }

        val distinctOils = detectedOilNames.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        val hasSeedOils = distinctOils.isNotEmpty() || totalSeedOilGrams > 0.0

        if (!hasSeedOils) {
            return SeedOilDetectionResult(
                containsSeedOils = false,
                detectedOils = emptyList(),
                detectedOilsCsv = "",
                riskLevel = SeedOilRiskLevel.GREEN,
                estimatedGrams = 0.0,
                ratioPercent = 0.0,
                label = "Seed-Oil Free",
                isDeepFried = false
            )
        }

        return evaluateRisk(
            containsSeedOils = true,
            detectedOils = distinctOils.ifEmpty { listOf("Seed Oil") },
            estimatedGrams = totalSeedOilGrams,
            servingWeightGrams = totalMealWeight,
            isDeepFried = anyDeepFried
        )
    }

    /**
     * Assesses a Gemini AI meal analysis using individual ingredient estimates.
     */
    fun assessGeminiIngredients(
        ingredients: List<GeminiIngredientEstimateDto>,
        totalMealWeight: Double,
        totalMealFat: Double,
        mealName: String
    ): SeedOilDetectionResult {
        val detectedOilNames = mutableListOf<String>()
        var totalSeedOilGrams = 0.0
        var anyDeepFried = DEEP_FRIED_PATTERNS.containsMatchIn(mealName)

        for (item in ingredients) {
            val check = analyzeIngredients(
                ingredientsText = item.name,
                servingWeightGrams = item.estimatedWeightGrams,
                totalFatG = item.fatG,
                pufaG = 0.0,
                foodName = item.name
            )

            if (check.containsSeedOils || item.containsSeedOils) {
                detectedOilNames.addAll(check.detectedOils)
                val isPureOil = PURE_SEED_OIL_PATTERNS.containsMatchIn(item.name)
                val isItemFried = check.isDeepFried || DEEP_FRIED_PATTERNS.containsMatchIn(item.name)
                if (isItemFried) anyDeepFried = true

                val itemGrams = when {
                    isPureOil -> item.estimatedWeightGrams
                    isItemFried -> if (item.fatG > 0.0) item.fatG * 0.85 else item.estimatedWeightGrams * 0.16
                    check.estimatedGrams > 0.0 -> check.estimatedGrams
                    item.fatG > 0.0 -> (item.fatG * 0.5).coerceAtLeast(0.5)
                    else -> 1.0
                }
                totalSeedOilGrams += itemGrams
            }
        }

        val distinctOils = detectedOilNames.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        val hasSeedOils = distinctOils.isNotEmpty() || totalSeedOilGrams > 0.0

        if (!hasSeedOils) {
            return SeedOilDetectionResult(
                containsSeedOils = false,
                detectedOils = emptyList(),
                detectedOilsCsv = "",
                riskLevel = SeedOilRiskLevel.GREEN,
                estimatedGrams = 0.0,
                ratioPercent = 0.0,
                label = "Seed-Oil Free",
                isDeepFried = false
            )
        }

        return evaluateRisk(
            containsSeedOils = true,
            detectedOils = distinctOils.ifEmpty { listOf("Seed Oil") },
            estimatedGrams = totalSeedOilGrams,
            servingWeightGrams = totalMealWeight,
            isDeepFried = anyDeepFried
        )
    }

    /**
     * Assesses a single product/food log with given macros and seed oil flags.
     */
    fun assessProduct(
        name: String,
        containsSeedOils: Boolean,
        detectedOilsCsv: String,
        servingWeightGrams: Double,
        fatG: Double,
        pufaG: Double
    ): SeedOilDetectionResult {
        if (!containsSeedOils && detectedOilsCsv.isBlank()) {
            val check = analyzeIngredients(name, servingWeightGrams, fatG, pufaG, name)
            if (!check.containsSeedOils) {
                return SeedOilDetectionResult(
                    containsSeedOils = false,
                    detectedOils = emptyList(),
                    detectedOilsCsv = "",
                    riskLevel = SeedOilRiskLevel.GREEN,
                    estimatedGrams = 0.0,
                    ratioPercent = 0.0,
                    label = "Seed-Oil Free",
                    isDeepFried = false
                )
            }
            return check
        }

        val oils = detectedOilsCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val isDeepFried = DEEP_FRIED_PATTERNS.containsMatchIn(name)
        val isPureOil = PURE_SEED_OIL_PATTERNS.containsMatchIn(name)

        val estimatedGrams = when {
            isPureOil -> if (fatG > 0.0) fatG else servingWeightGrams
            isDeepFried -> if (fatG > 0.0) fatG * 0.85 else servingWeightGrams * 0.16
            fatG > 0.0 -> {
                if (pufaG > 0.0) (pufaG * 1.5).coerceIn(0.5, fatG * 0.75)
                else (fatG * 0.45).coerceAtLeast(0.5)
            }
            else -> 1.0
        }

        return evaluateRisk(
            containsSeedOils = true,
            detectedOils = oils.ifEmpty { listOf("Seed Oil") },
            estimatedGrams = estimatedGrams,
            servingWeightGrams = servingWeightGrams,
            isDeepFried = isDeepFried
        )
    }

    /**
     * Core Traffic Light Decision Engine:
     * - GREEN: Clean (0g) or Trace (<= 2.5g or <= 2.5% ratio)
     * - AMBER: Moderate (2.5g - 10g or 2.5% - 10% ratio)
     * - RED: High (> 10g or > 10% ratio, or deep-fried)
     */
    private fun evaluateRisk(
        containsSeedOils: Boolean,
        detectedOils: List<String>,
        estimatedGrams: Double,
        servingWeightGrams: Double,
        isDeepFried: Boolean
    ): SeedOilDetectionResult {
        val totalWeight = servingWeightGrams.coerceAtLeast(1.0)
        val roundedGrams = round(estimatedGrams.coerceAtLeast(0.0) * 10.0) / 10.0
        val ratio = (roundedGrams / totalWeight) * 100.0
        val roundedRatio = round(ratio * 10.0) / 10.0
        val csv = detectedOils.joinToString(", ")

        if (!containsSeedOils || roundedGrams <= 0.05) {
            return SeedOilDetectionResult(
                containsSeedOils = false,
                detectedOils = emptyList(),
                detectedOilsCsv = "",
                riskLevel = SeedOilRiskLevel.GREEN,
                estimatedGrams = 0.0,
                ratioPercent = 0.0,
                label = "Seed-Oil Free",
                isDeepFried = false
            )
        }

        val riskLevel: SeedOilRiskLevel
        val label: String

        if (roundedGrams <= 2.5 && !isDeepFried) {
            riskLevel = SeedOilRiskLevel.GREEN
            val gramsText = if (roundedGrams < 1.0) "<1g" else "${roundedGrams}g"
            label = "Trace Seed Oil (~$gramsText • ${roundedRatio.toInt()}%)"
        } else if (roundedGrams <= 10.0 && !isDeepFried) {
            riskLevel = SeedOilRiskLevel.AMBER
            label = "Moderate Seed Oil (~${roundedGrams.toInt()}g • ${roundedRatio.toInt()}%)"
        } else {
            riskLevel = SeedOilRiskLevel.RED
            label = if (isDeepFried) {
                "High Seed Oil (~${roundedGrams.toInt()}g • Deep-Fried)"
            } else {
                "High Seed Oil (~${roundedGrams.toInt()}g • ${roundedRatio.toInt()}%)"
            }
        }

        return SeedOilDetectionResult(
            containsSeedOils = true,
            detectedOils = detectedOils,
            detectedOilsCsv = csv,
            riskLevel = riskLevel,
            estimatedGrams = roundedGrams,
            ratioPercent = roundedRatio,
            label = label,
            isDeepFried = isDeepFried
        )
    }
}
