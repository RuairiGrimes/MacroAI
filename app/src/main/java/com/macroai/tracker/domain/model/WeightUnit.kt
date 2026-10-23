package com.macroai.tracker.domain.model

import java.util.Locale

enum class WeightUnit(val label: String, val symbol: String) {
    KG("Kilograms", "kg"),
    LBS("Pounds", "lbs");

    fun toKg(value: Double): Double = when (this) {
        KG -> value
        LBS -> value * 0.45359237
    }

    fun fromKg(kg: Double): Double = when (this) {
        KG -> kg
        LBS -> kg * 2.20462262185
    }

    fun format(kg: Double, decimals: Int = 1): String {
        val converted = fromKg(kg)
        return String.format(Locale.US, "%.${decimals}f %s", converted, symbol)
    }

    fun formatValueOnly(kg: Double, decimals: Int = 1): String {
        val converted = fromKg(kg)
        return String.format(Locale.US, "%.${decimals}f", converted)
    }
}
