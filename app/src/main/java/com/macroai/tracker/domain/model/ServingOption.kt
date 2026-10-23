package com.macroai.tracker.domain.model

import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import kotlin.math.round

enum class ServingUnitType(val shortName: String, val label: String) {
    GRAMS("g", "Grams (g)"),
    OUNCES("oz", "Ounces (oz)"),
    SERVING("serving", "Serving / Portion"),
    HUNDRED_GRAMS("100g", "100g Portion")
}

data class ServingOption(
    val type: ServingUnitType,
    val label: String,
    val gramsPerUnit: Double
) {
    fun calculateTotalGrams(quantity: Double): Double {
        val total = (quantity.coerceAtLeast(0.0) * gramsPerUnit)
        return round(total * 10.0) / 10.0
    }

    companion object {
        fun optionsForProduct(product: NormalizedOffProduct): List<ServingOption> {
            val list = mutableListOf<ServingOption>()

            // 1. Grams (Standard base unit)
            list.add(ServingOption(ServingUnitType.GRAMS, "Grams (g)", 1.0))

            // 2. Package Serving (if defined)
            val explicitServingGrams = product.servingQuantityGrams?.takeIf { it > 0.0 }
                ?: product.defaultServingGrams.takeIf { it > 0.0 && it != 100.0 }

            if (explicitServingGrams != null) {
                val servingName = product.servingSizeName?.takeIf { it.isNotBlank() }
                    ?: "${explicitServingGrams.toInt()}g serving"
                list.add(
                    ServingOption(
                        type = ServingUnitType.SERVING,
                        label = "Serving ($servingName)",
                        gramsPerUnit = explicitServingGrams
                    )
                )
            }

            // 3. Ounces (1 oz = 28.3495g)
            list.add(ServingOption(ServingUnitType.OUNCES, "Ounces (oz)", 28.3495))

            // 4. 100g Reference
            list.add(ServingOption(ServingUnitType.HUNDRED_GRAMS, "100g Portion", 100.0))

            return list
        }
    }
}

