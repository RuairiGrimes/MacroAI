package com.macroai.tracker.ui.widget

data class MacroAiWidgetSnapshot(
    val calorieTarget: Int = 2000,
    val caloriesConsumed: Int = 0,
    val remainingCalories: Int = 2000,
    val proteinConsumedG: Float = 0f,
    val proteinTargetG: Float = 150f,
    val carbsConsumedG: Float = 0f,
    val carbsTargetG: Float = 200f,
    val fatConsumedG: Float = 0f,
    val fatTargetG: Float = 65f,
    val upfPercentage: Float = 0f,
    val seedOilCount: Int = 0
) {
    val calorieProgress: Float
        get() = if (calorieTarget > 0) (caloriesConsumed.toFloat() / calorieTarget.toFloat()).coerceIn(0f, 1f) else 0f

    val isOverBudget: Boolean
        get() = remainingCalories < 0

    val proteinProgress: Float
        get() = if (proteinTargetG > 0f) (proteinConsumedG / proteinTargetG).coerceIn(0f, 1f) else 0f

    val carbsProgress: Float
        get() = if (carbsTargetG > 0f) (carbsConsumedG / carbsTargetG).coerceIn(0f, 1f) else 0f

    val fatProgress: Float
        get() = if (fatTargetG > 0f) (fatConsumedG / fatTargetG).coerceIn(0f, 1f) else 0f
}

