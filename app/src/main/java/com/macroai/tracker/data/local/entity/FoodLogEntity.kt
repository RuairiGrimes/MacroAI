package com.macroai.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealIngredient
import com.macroai.tracker.domain.model.MealType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.round

@Entity(
    tableName = "food_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isSyncedToSheets"])
    ]
)
data class FoodLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val mealType: MealType,
    val name: String,
    val source: LogSource,
    val servingWeightGrams: Double,

    // Macros
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val saturatedFatG: Double = 0.0,
    val polyunsaturatedFatG: Double = 0.0,
    val monounsaturatedFatG: Double = 0.0,
    val sugarG: Double = 0.0,
    val fibreG: Double = 0.0,

    // Quality & UPF
    val novaGroup: Int? = null,
    val isUpf: Boolean = (novaGroup == 4),
    val containsSeedOils: Boolean = false,
    val detectedSeedOils: String = "",
    val aiConfidenceReasoning: String? = null,

    // Key Micronutrients
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDUg: Double = 0.0,
    val magnesiumMg: Double = 0.0,

    // Sync State
    val isSyncedToSheets: Boolean = false,

    // Ingredients & Recipe Reference
    val ingredientBreakdownJson: String? = null,
    val recipeId: String? = null,
    val servingsConsumed: Double = 1.0
) {
    fun parseIngredients(): List<MealIngredient> {
        val jsonStr = ingredientBreakdownJson
        if (jsonStr.isNullOrBlank()) return emptyList()
        return runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<MealIngredient>>(jsonStr)
        }.getOrDefault(emptyList())
    }

    fun withUpdatedIngredients(updatedIngredients: List<MealIngredient>): FoodLogEntity {
        if (updatedIngredients.isEmpty()) return this
        val newTotalWeight = updatedIngredients.sumOf { it.weightGrams }
        val oldWeight = servingWeightGrams.takeIf { it > 0.0 } ?: 100.0
        val ratio = newTotalWeight / oldWeight

        fun Double.r1(): Double = round(this * 10.0) / 10.0

        val maxNova = updatedIngredients.mapNotNull { it.novaGroup }.maxOrNull() ?: (novaGroup ?: 1)
        val anyOils = containsSeedOils || updatedIngredients.any { it.containsSeedOils }
        val allOils = updatedIngredients.map { it.detectedSeedOils }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

        return copy(
            servingWeightGrams = newTotalWeight.r1(),
            calories = updatedIngredients.sumOf { it.calories }.r1(),
            proteinG = updatedIngredients.sumOf { it.proteinG }.r1(),
            carbsG = updatedIngredients.sumOf { it.carbsG }.r1(),
            fatG = updatedIngredients.sumOf { it.fatG }.r1(),
            saturatedFatG = (saturatedFatG * ratio).r1(),
            polyunsaturatedFatG = (polyunsaturatedFatG * ratio).r1(),
            monounsaturatedFatG = (monounsaturatedFatG * ratio).r1(),
            sugarG = updatedIngredients.sumOf { it.sugarG }.r1(),
            fibreG = updatedIngredients.sumOf { it.fibreG }.r1(),
            novaGroup = maxNova,
            isUpf = (maxNova == 4),
            containsSeedOils = anyOils,
            detectedSeedOils = allOils.ifBlank { detectedSeedOils },
            ingredientBreakdownJson = Json.encodeToString(updatedIngredients),
            isSyncedToSheets = false
        )
    }

    fun withScaledWeight(newWeightGrams: Double): FoodLogEntity {
        val oldWeight = servingWeightGrams.coerceAtLeast(0.1)
        val ratio = (newWeightGrams.coerceAtLeast(0.0)) / oldWeight

        fun Double.scale(): Double = round((this * ratio) * 100.0) / 100.0

        val currentIngredients = parseIngredients()
        val scaledIngredients = currentIngredients.map { it.scaleFraction(ratio) }
        val updatedJson = if (scaledIngredients.isNotEmpty()) Json.encodeToString(scaledIngredients) else ingredientBreakdownJson

        return copy(
            servingWeightGrams = round(newWeightGrams * 10.0) / 10.0,
            calories = calories.scale(),
            proteinG = proteinG.scale(),
            carbsG = carbsG.scale(),
            fatG = fatG.scale(),
            saturatedFatG = saturatedFatG.scale(),
            polyunsaturatedFatG = polyunsaturatedFatG.scale(),
            monounsaturatedFatG = monounsaturatedFatG.scale(),
            sugarG = sugarG.scale(),
            fibreG = fibreG.scale(),
            sodiumMg = sodiumMg.scale(),
            potassiumMg = potassiumMg.scale(),
            ironMg = ironMg.scale(),
            calciumMg = calciumMg.scale(),
            vitaminCMg = vitaminCMg.scale(),
            vitaminDUg = vitaminDUg.scale(),
            magnesiumMg = magnesiumMg.scale(),
            ingredientBreakdownJson = updatedJson,
            isSyncedToSheets = false
        )
    }

    fun toSavedMealEntity(
        recipeName: String = name,
        servings: Double = 1.0
    ): SavedMealEntity {
        val currentIngredients = parseIngredients()
        val ingredientsList = if (currentIngredients.isNotEmpty()) {
            currentIngredients
        } else {
            listOf(
                MealIngredient(
                    name = name,
                    weightGrams = servingWeightGrams,
                    calories = calories,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG,
                    saturatedFatG = saturatedFatG,
                    polyunsaturatedFatG = polyunsaturatedFatG,
                    monounsaturatedFatG = monounsaturatedFatG,
                    sugarG = sugarG,
                    fibreG = fibreG,
                    novaGroup = novaGroup,
                    containsSeedOils = containsSeedOils,
                    detectedSeedOils = detectedSeedOils
                )
            )
        }
        return SavedMealEntity.fromIngredients(
            name = recipeName,
            description = "Saved from meal: $name",
            servings = servings,
            ingredients = ingredientsList
        ).copy(
            sodiumMg = sodiumMg,
            potassiumMg = potassiumMg,
            ironMg = ironMg,
            calciumMg = calciumMg,
            vitaminCMg = vitaminCMg,
            vitaminDUg = vitaminDUg,
            magnesiumMg = magnesiumMg
        )
    }

    fun seedOilAssessment(): com.macroai.tracker.domain.util.SeedOilDetectionResult {
        val ingredients = parseIngredients()
        if (ingredients.isNotEmpty()) {
            return com.macroai.tracker.domain.util.SeedOilDetector.assessIngredients(
                ingredients = ingredients,
                totalMealWeight = servingWeightGrams,
                totalMealFat = fatG,
                mealName = name
            )
        }
        return com.macroai.tracker.domain.util.SeedOilDetector.assessProduct(
            name = name,
            containsSeedOils = containsSeedOils,
            detectedOilsCsv = detectedSeedOils,
            servingWeightGrams = servingWeightGrams,
            fatG = fatG,
            pufaG = polyunsaturatedFatG
        )
    }

    fun toNormalizedOffProduct(): com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct {
        val safeWeight = if (servingWeightGrams > 0.0) servingWeightGrams else 100.0
        val factor = 100.0 / safeWeight
        fun Double.per100(): Double = round((this * factor) * 100.0) / 100.0

        return com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct(
            barcode = "logged_$id",
            name = name,
            brand = "Recently Logged",
            servingSizeName = "${safeWeight.toInt()}g logged portion",
            servingQuantityGrams = safeWeight,
            ingredientsText = aiConfidenceReasoning ?: name,
            defaultServingGrams = safeWeight,
            novaGroup = novaGroup,
            isUpf = isUpf,
            containsSeedOils = containsSeedOils,
            detectedSeedOilsCsv = detectedSeedOils,
            caloriesPer100g = calories.per100(),
            proteinPer100g = proteinG.per100(),
            carbsPer100g = carbsG.per100(),
            fatPer100g = fatG.per100(),
            saturatedFatPer100g = saturatedFatG.per100(),
            polyunsaturatedFatPer100g = polyunsaturatedFatG.per100(),
            monounsaturatedFatPer100g = monounsaturatedFatG.per100(),
            sugarPer100g = sugarG.per100(),
            fibrePer100g = fibreG.per100(),
            sodiumMgPer100g = sodiumMg.per100(),
            potassiumMgPer100g = potassiumMg.per100(),
            ironMgPer100g = ironMg.per100(),
            calciumMgPer100g = calciumMg.per100(),
            vitaminCMgPer100g = vitaminCMg.per100(),
            vitaminDUgPer100g = vitaminDUg.per100(),
            magnesiumMgPer100g = magnesiumMg.per100()
        )
    }
}
