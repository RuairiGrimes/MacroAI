package com.macroai.tracker.data.remote.gemini.dto

import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.toMealIngredient
import com.macroai.tracker.domain.util.SeedOilDetector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.round

@Serializable
data class GeminiGenerateContentRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiContentDto,
    @SerialName("contents") val contents: List<GeminiContentDto>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfigDto
)

@Serializable
data class GeminiContentDto(
    @SerialName("role") val role: String? = null,
    @SerialName("parts") val parts: List<GeminiPartDto>
)

@Serializable
data class GeminiPartDto(
    @SerialName("text") val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineDataDto? = null
)

@Serializable
data class GeminiInlineDataDto(
    @SerialName("mime_type") val mimeType: String,
    @SerialName("data") val data: String
)

@Serializable
data class GeminiThinkingConfigDto(
    @SerialName("thinkingLevel") val thinkingLevel: String = "low"
)

@Serializable
data class GeminiGenerationConfigDto(
    @SerialName("responseMimeType") val responseMimeType: String = "application/json",
    @SerialName("responseSchema") val responseSchema: JsonObject,
    @SerialName("temperature") val temperature: Double = 0.1,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 4096,
    @SerialName("thinkingConfig") val thinkingConfig: GeminiThinkingConfigDto? = GeminiThinkingConfigDto("low")
)

@Serializable
data class GeminiGenerateContentResponse(
    @SerialName("candidates") val candidates: List<GeminiCandidateDto> = emptyList()
)

@Serializable
data class GeminiCandidateDto(
    @SerialName("content") val content: GeminiContentDto? = null
)

/**
 * Structured output schema returned by `gemini-3.1-flash-lite`.
 * Matches `FoodLogEntity` fields + `ingredientBreakdown` array for interactive user tweaks.
 */
@Serializable
data class GeminiMealAnalysisDto(
    val name: String,
    val servingWeightGrams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val saturatedFatG: Double = 0.0,
    val polyunsaturatedFatG: Double = 0.0,
    val monounsaturatedFatG: Double = 0.0,
    val sugarG: Double = 0.0,
    val fibreG: Double = 0.0,
    val novaGroup: Int = 1,
    val isUpf: Boolean = false,
    val containsSeedOils: Boolean = false,
    val detectedSeedOils: String = "",
    val aiConfidenceReasoning: String = "",
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDUg: Double = 0.0,
    val magnesiumMg: Double = 0.0,
    val ingredientBreakdown: List<GeminiIngredientEstimateDto> = emptyList()
) {
    /**
     * Converts this AI analysis (potentially tweaked by the user in the review sheet)
     * into a `FoodLogEntity` ready for Room insertion.
     */
    fun toFoodLogEntity(
        mealType: MealType,
        userContextPrompt: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): FoodLogEntity {
        val combinedTextToScan = buildString {
            append(name).append(" ")
            append(detectedSeedOils).append(" ")
            append(aiConfidenceReasoning).append(" ")
            if (!userContextPrompt.isNullOrBlank()) append(userContextPrompt).append(" ")
            ingredientBreakdown.forEach { append(it.name).append(" ") }
        }
        val aiOilsList = detectedSeedOils.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val promptRegexCheck = SeedOilDetector.analyzeIngredients(
            ingredientsText = combinedTextToScan,
            servingWeightGrams = servingWeightGrams,
            totalFatG = fatG,
            pufaG = polyunsaturatedFatG,
            foodName = name
        )
        val ingredientCheck = if (ingredientBreakdown.isNotEmpty()) {
            SeedOilDetector.assessGeminiIngredients(
                ingredients = ingredientBreakdown,
                totalMealWeight = servingWeightGrams,
                totalMealFat = fatG,
                mealName = name
            )
        } else null

        val combinedOils = ((ingredientCheck?.detectedOils ?: emptyList()) + promptRegexCheck.detectedOils + aiOilsList)
            .map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()

        val hasOils = (ingredientCheck?.containsSeedOils == true) || promptRegexCheck.containsSeedOils || containsSeedOils || combinedOils.isNotEmpty()

        val seedOilCheck = if (hasOils) {
            val maxGrams = maxOf(ingredientCheck?.estimatedGrams ?: 0.0, promptRegexCheck.estimatedGrams)
            SeedOilDetector.assessProduct(
                name = name,
                containsSeedOils = true,
                detectedOilsCsv = combinedOils.joinToString(", "),
                servingWeightGrams = servingWeightGrams,
                fatG = fatG,
                pufaG = polyunsaturatedFatG
            ).copy(
                estimatedGrams = if (maxGrams > 0.0) maxGrams else 1.0,
                detectedOils = combinedOils,
                detectedOilsCsv = combinedOils.joinToString(", ")
            )
        } else {
            SeedOilDetector.analyzeIngredients(null)
        }

        val effectiveNova = (ingredientBreakdown.mapNotNull { it.novaGroup.takeIf { n -> n in 1..4 } }.maxOrNull()
            ?: novaGroup).coerceIn(1, 4)

        fun Double.round2(): Double = round(this * 100.0) / 100.0

        return FoodLogEntity(
            timestamp = timestamp,
            mealType = mealType,
            name = name,
            source = LogSource.AI_PHOTO,
            servingWeightGrams = servingWeightGrams.round2(),
            calories = calories.round2(),
            proteinG = proteinG.round2(),
            carbsG = carbsG.round2(),
            fatG = fatG.round2(),
            saturatedFatG = saturatedFatG.round2(),
            polyunsaturatedFatG = polyunsaturatedFatG.round2(),
            monounsaturatedFatG = monounsaturatedFatG.round2(),
            sugarG = sugarG.round2(),
            fibreG = fibreG.round2(),
            novaGroup = effectiveNova,
            isUpf = (effectiveNova == 4) || isUpf,
            containsSeedOils = seedOilCheck.containsSeedOils,
            detectedSeedOils = seedOilCheck.detectedOilsCsv,
            aiConfidenceReasoning = aiConfidenceReasoning,
            sodiumMg = sodiumMg.round2(),
            potassiumMg = potassiumMg.round2(),
            ironMg = ironMg.round2(),
            calciumMg = calciumMg.round2(),
            vitaminCMg = vitaminCMg.round2(),
            vitaminDUg = vitaminDUg.round2(),
            magnesiumMg = magnesiumMg.round2(),
            isSyncedToSheets = false,
            ingredientBreakdownJson = Json.encodeToString(ingredientBreakdown.map { it.toMealIngredient() })
        )
    }

    /**
     * Recomputes the meal totals when a user adjusts individual ingredient gram weights
     * in the review confirmation sheet.
     */
    fun withUpdatedIngredients(updatedIngredients: List<GeminiIngredientEstimateDto>): GeminiMealAnalysisDto {
        if (updatedIngredients.isEmpty()) return this
        val oldTotalWeight = servingWeightGrams.takeIf { it > 0.0 } ?: 100.0
        val newTotalWeight = updatedIngredients.sumOf { it.estimatedWeightGrams }.coerceAtLeast(1.0)
        val weightRatio = newTotalWeight / oldTotalWeight

        fun Double.round1(): Double = round(this * 10.0) / 10.0

        return copy(
            servingWeightGrams = newTotalWeight.round1(),
            calories = updatedIngredients.sumOf { it.calories }.round1(),
            proteinG = updatedIngredients.sumOf { it.proteinG }.round1(),
            carbsG = updatedIngredients.sumOf { it.carbsG }.round1(),
            fatG = updatedIngredients.sumOf { it.fatG }.round1(),
            saturatedFatG = (saturatedFatG * weightRatio).round1(),
            polyunsaturatedFatG = (polyunsaturatedFatG * weightRatio).round1(),
            monounsaturatedFatG = (monounsaturatedFatG * weightRatio).round1(),
            sugarG = (sugarG * weightRatio).round1(),
            fibreG = (fibreG * weightRatio).round1(),
            sodiumMg = (sodiumMg * weightRatio).round1(),
            potassiumMg = (potassiumMg * weightRatio).round1(),
            ironMg = (ironMg * weightRatio).round1(),
            calciumMg = (calciumMg * weightRatio).round1(),
            vitaminCMg = (vitaminCMg * weightRatio).round1(),
            vitaminDUg = (vitaminDUg * weightRatio).round1(),
            magnesiumMg = (magnesiumMg * weightRatio).round1(),
            ingredientBreakdown = updatedIngredients
        )
    }

    fun seedOilAssessment(): com.macroai.tracker.domain.util.SeedOilDetectionResult {
        if (ingredientBreakdown.isNotEmpty()) {
            return SeedOilDetector.assessGeminiIngredients(
                ingredients = ingredientBreakdown,
                totalMealWeight = servingWeightGrams,
                totalMealFat = fatG,
                mealName = name
            )
        }
        return SeedOilDetector.assessProduct(
            name = name,
            containsSeedOils = containsSeedOils,
            detectedOilsCsv = detectedSeedOils,
            servingWeightGrams = servingWeightGrams,
            fatG = fatG,
            pufaG = polyunsaturatedFatG
        )
    }
}

fun com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct.toGeminiIngredient(grams: Double): GeminiIngredientEstimateDto {
    val factor = grams / 100.0
    fun Double.scale(): Double = round((this * factor) * 10.0) / 10.0
    return GeminiIngredientEstimateDto(
        name = name,
        estimatedWeightGrams = round(grams * 10.0) / 10.0,
        calories = caloriesPer100g.scale(),
        proteinG = proteinPer100g.scale(),
        carbsG = carbsPer100g.scale(),
        fatG = fatPer100g.scale(),
        novaGroup = novaGroup ?: 1,
        containsSeedOils = containsSeedOils
    )
}

@Serializable
data class GeminiIngredientEstimateDto(
    val name: String,
    val estimatedWeightGrams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val novaGroup: Int = 1,
    val containsSeedOils: Boolean = false
) {
    fun scaleToWeight(newWeightGrams: Double): GeminiIngredientEstimateDto {
        val base = estimatedWeightGrams.takeIf { it > 0.0 } ?: return copy(estimatedWeightGrams = newWeightGrams)
        val factor = newWeightGrams.coerceAtLeast(0.0) / base
        fun Double.round1(): Double = round(this * factor * 10.0) / 10.0
        return copy(
            estimatedWeightGrams = round(newWeightGrams * 10.0) / 10.0,
            calories = calories.round1(),
            proteinG = proteinG.round1(),
            carbsG = carbsG.round1(),
            fatG = fatG.round1()
        )
    }
}

object GeminiMealResponseSchema {
    val jsonSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("name") { put("type", "STRING") }
            putJsonObject("servingWeightGrams") { put("type", "NUMBER") }
            putJsonObject("calories") { put("type", "NUMBER") }
            putJsonObject("proteinG") { put("type", "NUMBER") }
            putJsonObject("carbsG") { put("type", "NUMBER") }
            putJsonObject("fatG") { put("type", "NUMBER") }
            putJsonObject("saturatedFatG") { put("type", "NUMBER") }
            putJsonObject("polyunsaturatedFatG") { put("type", "NUMBER") }
            putJsonObject("monounsaturatedFatG") { put("type", "NUMBER") }
            putJsonObject("sugarG") { put("type", "NUMBER") }
            putJsonObject("fibreG") { put("type", "NUMBER") }
            putJsonObject("novaGroup") {
                put("type", "INTEGER")
                put("description", "NOVA food processing classification from 1 (unprocessed) to 4 (ultra-processed)")
            }
            putJsonObject("isUpf") { put("type", "BOOLEAN") }
            putJsonObject("containsSeedOils") { put("type", "BOOLEAN") }
            putJsonObject("detectedSeedOils") {
                put("type", "STRING")
                put("description", "Comma-separated list of detected or likely seed oils, empty string if none")
            }
            putJsonObject("aiConfidenceReasoning") {
                put("type", "STRING")
                put("description", "Concise clinical rationale for weight, cooking fats, NOVA group, and macro estimates")
            }
            putJsonObject("sodiumMg") { put("type", "NUMBER") }
            putJsonObject("potassiumMg") { put("type", "NUMBER") }
            putJsonObject("ironMg") { put("type", "NUMBER") }
            putJsonObject("calciumMg") { put("type", "NUMBER") }
            putJsonObject("vitaminCMg") { put("type", "NUMBER") }
            putJsonObject("vitaminDUg") { put("type", "NUMBER") }
            putJsonObject("magnesiumMg") { put("type", "NUMBER") }
            putJsonObject("ingredientBreakdown") {
                put("type", "ARRAY")
                put("description", "Consolidate into at most 5 primary meal components for fast analysis")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") { put("type", "STRING") }
                        putJsonObject("estimatedWeightGrams") { put("type", "NUMBER") }
                        putJsonObject("calories") { put("type", "NUMBER") }
                        putJsonObject("proteinG") { put("type", "NUMBER") }
                        putJsonObject("carbsG") { put("type", "NUMBER") }
                        putJsonObject("fatG") { put("type", "NUMBER") }
                        putJsonObject("novaGroup") { put("type", "INTEGER") }
                        putJsonObject("containsSeedOils") { put("type", "BOOLEAN") }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("name"))
                        add(JsonPrimitive("estimatedWeightGrams"))
                        add(JsonPrimitive("calories"))
                        add(JsonPrimitive("proteinG"))
                        add(JsonPrimitive("carbsG"))
                        add(JsonPrimitive("fatG"))
                        add(JsonPrimitive("novaGroup"))
                        add(JsonPrimitive("containsSeedOils"))
                    }
                }
            }
        }
        putJsonArray("required") {
            listOf(
                "name", "servingWeightGrams", "calories", "proteinG", "carbsG", "fatG",
                "saturatedFatG", "polyunsaturatedFatG", "monounsaturatedFatG", "sugarG", "fibreG",
                "novaGroup", "isUpf", "containsSeedOils", "detectedSeedOils", "aiConfidenceReasoning",
                "sodiumMg", "potassiumMg", "ironMg", "calciumMg", "vitaminCMg", "vitaminDUg", "magnesiumMg",
                "ingredientBreakdown"
            ).forEach { add(JsonPrimitive(it)) }
        }
    }
}

