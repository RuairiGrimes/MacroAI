package com.macroai.tracker.domain.model

enum class TdeeMode(val title: String, val subtitle: String) {
    FORMULA_MULTIPLIER(
        title = "Mifflin-St Jeor Formula",
        subtitle = "Calculated BMR × Activity Multiplier based on daily physical activity"
    ),
    HEALTH_CONNECT_DYNAMIC(
        title = "Health Connect Dynamic",
        subtitle = "Mifflin-St Jeor BMR + live Active Calories burned today from Health Connect"
    ),
    MANUAL_OVERRIDE(
        title = "Manual TDEE Expenditure",
        subtitle = "Fixed daily energy burn override"
    )
}

enum class EnergyTargetMode(val title: String, val subtitle: String) {
    SET_CALORIE_GOAL(
        title = "Set Calorie Goal",
        subtitle = "Fixed total calorie consumed target (e.g. 2100 kcal consumed)"
    ),
    TDEE_DEFICIT(
        title = "Deficit from TDEE",
        subtitle = "Dynamic target = daily TDEE minus chosen weight-loss deficit"
    )
}

enum class MacroInputMode(val label: String) {
    CUSTOM_GRAMS("Exact Grams (g)"),
    PERCENTAGE_SPLIT("Percentage Split (%)")
}

enum class ActivityLevel(val label: String, val multiplier: Double, val description: String) {
    SEDENTARY("Sedentary", 1.2, "Desk job, minimal exercise"),
    LIGHT("Lightly Active", 1.375, "Light exercise 1–3 days/week"),
    MODERATE("Moderately Active", 1.55, "Moderate exercise 3–5 days/week"),
    ACTIVE("Very Active", 1.725, "Hard exercise 6–7 days/week"),
    ATHLETE("Extra Active", 1.9, "Intense daily training or physical job")
}

enum class BiologicalSex(val label: String, val mifflinOffset: Int) {
    MALE("Male (+5)", 5),
    FEMALE("Female (-161)", -161)
}
