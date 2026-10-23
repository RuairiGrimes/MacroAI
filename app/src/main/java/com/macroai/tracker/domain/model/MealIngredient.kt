package com.macroai.tracker.domain.model

import com.macroai.tracker.data.remote.gemini.dto.GeminiIngredientEstimateDto
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.round

@Serializable
data class MealIngredient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val weightGrams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val saturatedFatG: Double = 0.0,
    val polyunsaturatedFatG: Double = 0.0,
    val monounsaturatedFatG: Double = 0.0,
    val sugarG: Double = 0.0,
    val fibreG: Double = 0.0,
    val novaGroup: Int? = 1,
    val containsSeedOils: Boolean = false,
    val detectedSeedOils: String = "",
    val barcode: String? = null
) {
    /**
     * Rescales this ingredient's macros proportionately when its gram weight changes.
     */
    fun withWeight(newWeightGrams: Double): MealIngredient {
        val oldWeight = weightGrams.coerceAtLeast(0.1)
        val ratio = (newWeightGrams.coerceAtLeast(0.0)) / oldWeight
        fun Double.scale(): Double = round((this * ratio) * 100.0) / 100.0

        return copy(
            weightGrams = round(newWeightGrams * 10.0) / 10.0,
            calories = calories.scale(),
            proteinG = proteinG.scale(),
            carbsG = carbsG.scale(),
            fatG = fatG.scale(),
            saturatedFatG = saturatedFatG.scale(),
            polyunsaturatedFatG = polyunsaturatedFatG.scale(),
            monounsaturatedFatG = monounsaturatedFatG.scale(),
            sugarG = sugarG.scale(),
            fibreG = fibreG.scale()
        )
    }

    /**
     * Scales this ingredient by an exact fraction (e.g. 1/5 for a family recipe).
     */
    fun scaleFraction(fraction: Double): MealIngredient {
        val r = fraction.coerceAtLeast(0.0)
        fun Double.scale(): Double = round((this * r) * 100.0) / 100.0

        return copy(
            weightGrams = round((weightGrams * r) * 10.0) / 10.0,
            calories = calories.scale(),
            proteinG = proteinG.scale(),
            carbsG = carbsG.scale(),
            fatG = fatG.scale(),
            saturatedFatG = saturatedFatG.scale(),
            polyunsaturatedFatG = polyunsaturatedFatG.scale(),
            monounsaturatedFatG = monounsaturatedFatG.scale(),
            sugarG = sugarG.scale(),
            fibreG = fibreG.scale()
        )
    }
}

fun GeminiIngredientEstimateDto.toMealIngredient(): MealIngredient {
    return MealIngredient(
        name = name,
        weightGrams = estimatedWeightGrams,
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        novaGroup = novaGroup,
        containsSeedOils = containsSeedOils,
        detectedSeedOils = if (containsSeedOils) "Seed Oil" else ""
    )
}

fun NormalizedOffProduct.toMealIngredient(grams: Double): MealIngredient {
    val factor = grams / 100.0
    fun Double.scale(): Double = round((this * factor) * 100.0) / 100.0

    return MealIngredient(
        name = name,
        weightGrams = grams,
        calories = caloriesPer100g.scale(),
        proteinG = proteinPer100g.scale(),
        carbsG = carbsPer100g.scale(),
        fatG = fatPer100g.scale(),
        saturatedFatG = saturatedFatPer100g.scale(),
        polyunsaturatedFatG = polyunsaturatedFatPer100g.scale(),
        monounsaturatedFatG = monounsaturatedFatPer100g.scale(),
        sugarG = sugarPer100g.scale(),
        fibreG = fibrePer100g.scale(),
        novaGroup = novaGroup,
        containsSeedOils = containsSeedOils,
        detectedSeedOils = detectedSeedOilsCsv,
        barcode = barcode
    )
}
