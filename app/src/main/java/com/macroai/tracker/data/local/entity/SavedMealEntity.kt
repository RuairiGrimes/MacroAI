package com.macroai.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealIngredient
import com.macroai.tracker.domain.model.MealType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.round

@Entity(tableName = "saved_meals")
data class SavedMealEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val totalServings: Double = 1.0,
    val totalWeightGrams: Double = 0.0,
    val ingredientsJson: String = "[]",

    // Total Recipe Macros
    val totalCalories: Double = 0.0,
    val totalProteinG: Double = 0.0,
    val totalCarbsG: Double = 0.0,
    val totalFatG: Double = 0.0,
    val totalSaturatedFatG: Double = 0.0,
    val totalPolyunsaturatedFatG: Double = 0.0,
    val totalMonounsaturatedFatG: Double = 0.0,
    val totalSugarG: Double = 0.0,
    val totalFibreG: Double = 0.0,

    // Quality
    val novaGroup: Int? = null,
    val isUpf: Boolean = false,
    val containsSeedOils: Boolean = false,
    val detectedSeedOils: String = "",

    // Micronutrients
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDUg: Double = 0.0,
    val magnesiumMg: Double = 0.0,

    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    fun parseIngredients(): List<MealIngredient> {
        return runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<MealIngredient>>(ingredientsJson)
        }.getOrDefault(emptyList())
    }

    val perServingCalories: Double
        get() = if (totalServings > 0.0) round((totalCalories / totalServings) * 10.0) / 10.0 else totalCalories

    val perServingProtein: Double
        get() = if (totalServings > 0.0) round((totalProteinG / totalServings) * 10.0) / 10.0 else totalProteinG

    val perServingCarbs: Double
        get() = if (totalServings > 0.0) round((totalCarbsG / totalServings) * 10.0) / 10.0 else totalCarbsG

    val perServingFat: Double
        get() = if (totalServings > 0.0) round((totalFatG / totalServings) * 10.0) / 10.0 else totalFatG

    val perServingWeightGrams: Double
        get() = if (totalServings > 0.0) round((totalWeightGrams / totalServings) * 10.0) / 10.0 else totalWeightGrams

    /**
     * Converts a fraction/portion of this saved recipe into a loggable FoodLogEntity.
     */
    fun toFoodLogEntity(
        servingsToLog: Double,
        mealType: MealType,
        timestamp: Long = System.currentTimeMillis()
    ): FoodLogEntity {
        val fraction = (servingsToLog / totalServings.coerceAtLeast(0.1)).coerceAtLeast(0.0)
        fun Double.scale(): Double = round((this * fraction) * 100.0) / 100.0

        val scaledIngredients = parseIngredients().map { it.scaleFraction(fraction) }
        val scaledIngredientsJson = Json.encodeToString(scaledIngredients)

        val logName = if (totalServings > 1.0) {
            val portionText = if (servingsToLog % 1.0 == 0.0) {
                "${servingsToLog.toInt()} of ${totalServings.toInt()} servings"
            } else {
                "${"%.1f".format(servingsToLog)} of ${"%.1f".format(totalServings)} servings"
            }
            "$name ($portionText)"
        } else {
            name
        }

        return FoodLogEntity(
            timestamp = timestamp,
            mealType = mealType,
            name = logName,
            source = LogSource.MANUAL,
            servingWeightGrams = totalWeightGrams.scale(),
            calories = totalCalories.scale(),
            proteinG = totalProteinG.scale(),
            carbsG = totalCarbsG.scale(),
            fatG = totalFatG.scale(),
            saturatedFatG = totalSaturatedFatG.scale(),
            polyunsaturatedFatG = totalPolyunsaturatedFatG.scale(),
            monounsaturatedFatG = totalMonounsaturatedFatG.scale(),
            sugarG = totalSugarG.scale(),
            fibreG = totalFibreG.scale(),
            novaGroup = novaGroup,
            isUpf = isUpf,
            containsSeedOils = containsSeedOils,
            detectedSeedOils = detectedSeedOils,
            aiConfidenceReasoning = "Logged from saved recipe: $name",
            sodiumMg = sodiumMg.scale(),
            potassiumMg = potassiumMg.scale(),
            ironMg = ironMg.scale(),
            calciumMg = calciumMg.scale(),
            vitaminCMg = vitaminCMg.scale(),
            vitaminDUg = vitaminDUg.scale(),
            magnesiumMg = magnesiumMg.scale(),
            isSyncedToSheets = false,
            ingredientBreakdownJson = scaledIngredientsJson,
            recipeId = id,
            servingsConsumed = servingsToLog
        )
    }

    fun seedOilAssessment(): com.macroai.tracker.domain.util.SeedOilDetectionResult {
        val ingredients = parseIngredients()
        if (ingredients.isNotEmpty()) {
            return com.macroai.tracker.domain.util.SeedOilDetector.assessIngredients(
                ingredients = ingredients,
                totalMealWeight = totalWeightGrams,
                totalMealFat = totalFatG,
                mealName = name
            )
        }
        return com.macroai.tracker.domain.util.SeedOilDetector.assessProduct(
            name = name,
            containsSeedOils = containsSeedOils,
            detectedOilsCsv = detectedSeedOils,
            servingWeightGrams = totalWeightGrams,
            fatG = totalFatG,
            pufaG = totalPolyunsaturatedFatG
        )
    }

    fun toNormalizedOffProduct(): com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct {
        val safeWeight = if (totalWeightGrams > 0.0) totalWeightGrams else 100.0
        val safeServings = totalServings.coerceAtLeast(1.0)
        val oneServingGrams = round((safeWeight / safeServings) * 10.0) / 10.0
        val factor = 100.0 / safeWeight
        fun Double.per100(): Double = round((this * factor) * 100.0) / 100.0

        val ingredientSummary = parseIngredients().joinToString(", ") { "${it.name} (${it.weightGrams.toInt()}g)" }

        return com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct(
            barcode = "recipe_$id",
            name = name,
            brand = "Saved Recipe",
            servingSizeName = "1 serving (${oneServingGrams.toInt()}g)",
            servingQuantityGrams = oneServingGrams,
            ingredientsText = ingredientSummary.ifBlank { description },
            defaultServingGrams = oneServingGrams,
            novaGroup = novaGroup,
            isUpf = isUpf,
            containsSeedOils = containsSeedOils,
            detectedSeedOilsCsv = detectedSeedOils,
            caloriesPer100g = totalCalories.per100(),
            proteinPer100g = totalProteinG.per100(),
            carbsPer100g = totalCarbsG.per100(),
            fatPer100g = totalFatG.per100(),
            saturatedFatPer100g = totalSaturatedFatG.per100(),
            polyunsaturatedFatPer100g = totalPolyunsaturatedFatG.per100(),
            monounsaturatedFatPer100g = totalMonounsaturatedFatG.per100(),
            sugarPer100g = totalSugarG.per100(),
            fibrePer100g = totalFibreG.per100(),
            sodiumMgPer100g = sodiumMg.per100(),
            potassiumMgPer100g = potassiumMg.per100(),
            ironMgPer100g = ironMg.per100(),
            calciumMgPer100g = calciumMg.per100(),
            vitaminCMgPer100g = vitaminCMg.per100(),
            vitaminDUgPer100g = vitaminDUg.per100(),
            magnesiumMgPer100g = magnesiumMg.per100()
        )
    }

    companion object {
        fun fromIngredients(
            name: String,
            description: String = "",
            servings: Double = 1.0,
            ingredients: List<MealIngredient>,
            id: String = UUID.randomUUID().toString()
        ): SavedMealEntity {
            val validServings = servings.coerceAtLeast(0.1)
            val totalWeight = ingredients.sumOf { it.weightGrams }
            val totalKcal = ingredients.sumOf { it.calories }
            val totalPro = ingredients.sumOf { it.proteinG }
            val totalCarb = ingredients.sumOf { it.carbsG }
            val totalF = ingredients.sumOf { it.fatG }
            val totalSat = ingredients.sumOf { it.saturatedFatG }
            val totalPufa = ingredients.sumOf { it.polyunsaturatedFatG }
            val totalMufa = ingredients.sumOf { it.monounsaturatedFatG }
            val totalSug = ingredients.sumOf { it.sugarG }
            val totalFib = ingredients.sumOf { it.fibreG }

            val maxNova = ingredients.mapNotNull { it.novaGroup }.maxOrNull() ?: 1
            val anySeedOils = ingredients.any { it.containsSeedOils }
            val distinctOils = ingredients.map { it.detectedSeedOils }
                .flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")

            fun Double.r1(): Double = round(this * 10.0) / 10.0

            return SavedMealEntity(
                id = id,
                name = name,
                description = description,
                totalServings = validServings,
                totalWeightGrams = totalWeight.r1(),
                ingredientsJson = Json.encodeToString(ingredients),
                totalCalories = totalKcal.r1(),
                totalProteinG = totalPro.r1(),
                totalCarbsG = totalCarb.r1(),
                totalFatG = totalF.r1(),
                totalSaturatedFatG = totalSat.r1(),
                totalPolyunsaturatedFatG = totalPufa.r1(),
                totalMonounsaturatedFatG = totalMufa.r1(),
                totalSugarG = totalSug.r1(),
                totalFibreG = totalFib.r1(),
                novaGroup = maxNova,
                isUpf = (maxNova == 4),
                containsSeedOils = anySeedOils,
                detectedSeedOils = distinctOils,
                lastUsedAt = System.currentTimeMillis()
            )
        }
    }
}

