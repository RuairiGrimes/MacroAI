package com.macroai.tracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack")
}

