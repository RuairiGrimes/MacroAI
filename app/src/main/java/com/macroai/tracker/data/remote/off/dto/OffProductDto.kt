package com.macroai.tracker.data.remote.off.dto

import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.util.SeedOilDetector
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.round

@OptIn(ExperimentalSerializationApi::class)
object OffBrandsSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OffBrands", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try {
            decoder.decodeString()
        } catch (_: Exception) {
            null
        }
        return when (val elem = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> elem.mapNotNull {
                (it as? JsonPrimitive)?.content?.trim()?.takeIf { s -> s.isNotEmpty() }
            }.joinToString(", ").takeIf { it.isNotEmpty() }
            is JsonPrimitive -> elem.content.trim().takeIf { it.isNotEmpty() }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) encoder.encodeString(value) else encoder.encodeNull()
    }
}

@OptIn(ExperimentalSerializationApi::class)
object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try {
            decoder.decodeDouble()
        } catch (_: Exception) {
            null
        }
        return when (val elem = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> elem.content.toDoubleOrNull()
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value != null) encoder.encodeDouble(value) else encoder.encodeNull()
    }
}

@OptIn(ExperimentalSerializationApi::class)
object LenientIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try {
            decoder.decodeInt()
        } catch (_: Exception) {
            null
        }
        return when (val elem = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> elem.content.toIntOrNull()
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value != null) encoder.encodeInt(value) else encoder.encodeNull()
    }
}

@Serializable
data class OffProductResponseDto(
    @SerialName("code") val code: String? = null,
    @SerialName("status") val status: Int = 0,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    @SerialName("product") val product: OffProductDto? = null
)

@Serializable
data class OffSearchResponseDto(
    @SerialName("count") val count: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 0,
    @SerialName("hits") val hits: List<OffProductDto> = emptyList()
)

@Serializable
data class OffProductDto(
    @SerialName("code") val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    @Serializable(with = OffBrandsSerializer::class)
    @SerialName("brands") val brands: String? = null,
    @SerialName("quantity") val quantity: String? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    @SerialName("ingredients_text_en") val ingredientsTextEn: String? = null,
    @SerialName("nutriments") val nutriments: OffNutrimentsDto? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null
) {
    fun resolvedName(fallbackBarcode: String): String {
        val baseName = productName?.takeIf { it.isNotBlank() }
            ?: productNameEn?.takeIf { it.isNotBlank() }
            ?: "Product ($fallbackBarcode)"
        val brand = brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        return if (brand != null && !baseName.contains(brand, ignoreCase = true)) {
            "$brand — $baseName"
        } else {
            baseName
        }
    }

    fun resolvedIngredientsText(): String =
        ingredientsText?.takeIf { it.isNotBlank() }
            ?: ingredientsTextEn?.takeIf { it.isNotBlank() }
            ?: ""
}

@Serializable
data class OffNutrimentsDto(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("energy_100g") val energyKj100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("saturated-fat_100g") val saturatedFat100g: Double? = null,
    @SerialName("polyunsaturated-fat_100g") val polyunsaturatedFat100g: Double? = null,
    @SerialName("monounsaturated-fat_100g") val monounsaturatedFat100g: Double? = null,
    @SerialName("sugars_100g") val sugars100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("sodium_100g") val sodium100g: Double? = null,
    @SerialName("sodium_unit") val sodiumUnit: String? = null,
    @SerialName("salt_100g") val salt100g: Double? = null,
    @SerialName("potassium_100g") val potassium100g: Double? = null,
    @SerialName("potassium_unit") val potassiumUnit: String? = null,
    @SerialName("iron_100g") val iron100g: Double? = null,
    @SerialName("iron_unit") val ironUnit: String? = null,
    @SerialName("calcium_100g") val calcium100g: Double? = null,
    @SerialName("calcium_unit") val calciumUnit: String? = null,
    @SerialName("vitamin-c_100g") val vitaminC100g: Double? = null,
    @SerialName("vitamin-c_unit") val vitaminCUnit: String? = null,
    @SerialName("vitamin-d_100g") val vitaminD100g: Double? = null,
    @SerialName("vitamin-d_unit") val vitaminDUnit: String? = null,
    @SerialName("magnesium_100g") val magnesium100g: Double? = null,
    @SerialName("magnesium_unit") val magnesiumUnit: String? = null
)

/**
 * Normalized per-100g nutrition profile extracted from Open Food Facts,
 * ready for live proportional gram scaling in the UI.
 */
data class NormalizedOffProduct(
    val barcode: String,
    val name: String,
    val ingredientsText: String,
    val defaultServingGrams: Double,
    val novaGroup: Int?,
    val isUpf: Boolean,
    val containsSeedOils: Boolean,
    val detectedSeedOilsCsv: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val saturatedFatPer100g: Double = 0.0,
    val polyunsaturatedFatPer100g: Double = 0.0,
    val monounsaturatedFatPer100g: Double = 0.0,
    val sugarPer100g: Double = 0.0,
    val fibrePer100g: Double = 0.0,
    val sodiumMgPer100g: Double = 0.0,
    val potassiumMgPer100g: Double = 0.0,
    val ironMgPer100g: Double = 0.0,
    val calciumMgPer100g: Double = 0.0,
    val vitaminCMgPer100g: Double = 0.0,
    val vitaminDUgPer100g: Double = 0.0,
    val magnesiumMgPer100g: Double = 0.0,
    val brand: String? = null,
    val servingSizeName: String? = null,
    val servingQuantityGrams: Double? = null,
    val imageUrl: String? = null
) {
    fun scaleToEntity(
        servingWeightGrams: Double,
        mealType: MealType,
        source: LogSource = LogSource.BARCODE,
        timestamp: Long = System.currentTimeMillis()
    ): FoodLogEntity {
        val factor = (servingWeightGrams.coerceAtLeast(0.0)) / 100.0
        fun Double.scale(): Double = round((this * factor) * 100.0) / 100.0

        return FoodLogEntity(
            timestamp = timestamp,
            mealType = mealType,
            name = name,
            source = source,
            servingWeightGrams = servingWeightGrams,
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
            isUpf = isUpf,
            containsSeedOils = containsSeedOils,
            detectedSeedOils = detectedSeedOilsCsv,
            aiConfidenceReasoning = null,
            sodiumMg = sodiumMgPer100g.scale(),
            potassiumMg = potassiumMgPer100g.scale(),
            ironMg = ironMgPer100g.scale(),
            calciumMg = calciumMgPer100g.scale(),
            vitaminCMg = vitaminCMgPer100g.scale(),
            vitaminDUg = vitaminDUgPer100g.scale(),
            magnesiumMg = magnesiumMgPer100g.scale(),
            isSyncedToSheets = false
        )
    }

    fun seedOilAssessment(grams: Double = defaultServingGrams): com.macroai.tracker.domain.util.SeedOilDetectionResult {
        val factor = (grams.coerceAtLeast(0.0)) / 100.0
        return com.macroai.tracker.domain.util.SeedOilDetector.assessProduct(
            name = name,
            containsSeedOils = containsSeedOils,
            detectedOilsCsv = detectedSeedOilsCsv,
            servingWeightGrams = grams,
            fatG = fatPer100g * factor,
            pufaG = polyunsaturatedFatPer100g * factor
        )
    }
}

fun OffProductDto.toNormalizedProduct(fallbackBarcode: String = ""): NormalizedOffProduct {
    val barcode = code?.takeIf { it.isNotBlank() } ?: fallbackBarcode
    val ingredients = resolvedIngredientsText()
    val n = nutriments ?: OffNutrimentsDto()
    val fat100g = n.fat100g ?: 0.0
    val pufa100g = n.polyunsaturatedFat100g ?: 0.0
    val resolvedTitle = resolvedName(barcode)
    val seedOilResult = com.macroai.tracker.domain.util.SeedOilDetector.analyzeIngredients(
        ingredientsText = ingredients,
        servingWeightGrams = 100.0,
        totalFatG = fat100g,
        pufaG = pufa100g,
        foodName = resolvedTitle
    )

    val kcal100g = n.energyKcal100g
        ?: n.energyKj100g?.let { it / 4.184 }
        ?: 0.0

    fun toMg(value: Double?, unit: String?): Double {
        if (value == null) return 0.0
        return when (unit?.trim()?.lowercase()) {
            "g" -> value * 1000.0
            "ug", "µg", "mcg" -> value / 1000.0
            else -> value
        }
    }

    fun toUg(value: Double?, unit: String?): Double {
        if (value == null) return 0.0
        return when (unit?.trim()?.lowercase()) {
            "g" -> value * 1_000_000.0
            "mg" -> value * 1000.0
            else -> value
        }
    }

    val sodiumMg100g = when {
        n.sodium100g != null -> toMg(n.sodium100g, n.sodiumUnit)
        n.salt100g != null -> (n.salt100g * 1000.0) * 0.4 // salt(g) -> sodium(mg)
        else -> 0.0
    }

    val validNova = novaGroup?.takeIf { it in 1..4 }

    val parsedServingGrams: Double? = servingQuantity?.takeIf { it > 0.0 }
        ?: servingSize?.let { text ->
            val regex = Regex("""(\d+(?:\.\d+)?)\s*(?:g|ml|gram)""", RegexOption.IGNORE_CASE)
            regex.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        }

    val cleanBrand = brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    return NormalizedOffProduct(
        barcode = barcode,
        name = resolvedName(barcode),
        brand = cleanBrand,
        servingSizeName = servingSize?.takeIf { it.isNotBlank() },
        servingQuantityGrams = parsedServingGrams,
        ingredientsText = ingredients,
        defaultServingGrams = parsedServingGrams ?: 100.0,
        novaGroup = validNova,
        isUpf = (validNova == 4),
        containsSeedOils = seedOilResult.containsSeedOils,
        detectedSeedOilsCsv = seedOilResult.detectedOilsCsv,
        caloriesPer100g = kcal100g,
        proteinPer100g = n.proteins100g ?: 0.0,
        carbsPer100g = n.carbohydrates100g ?: 0.0,
        fatPer100g = n.fat100g ?: 0.0,
        saturatedFatPer100g = n.saturatedFat100g ?: 0.0,
        polyunsaturatedFatPer100g = n.polyunsaturatedFat100g ?: 0.0,
        monounsaturatedFatPer100g = n.monounsaturatedFat100g ?: 0.0,
        sugarPer100g = n.sugars100g ?: 0.0,
        fibrePer100g = n.fiber100g ?: 0.0,
        sodiumMgPer100g = sodiumMg100g,
        potassiumMgPer100g = toMg(n.potassium100g, n.potassiumUnit),
        ironMgPer100g = toMg(n.iron100g, n.ironUnit),
        calciumMgPer100g = toMg(n.calcium100g, n.calciumUnit),
        vitaminCMgPer100g = toMg(n.vitaminC100g, n.vitaminCUnit),
        vitaminDUgPer100g = toUg(n.vitaminD100g, n.vitaminDUnit),
        magnesiumMgPer100g = toMg(n.magnesium100g, n.magnesiumUnit),
        imageUrl = imageFrontSmallUrl
    )
}
