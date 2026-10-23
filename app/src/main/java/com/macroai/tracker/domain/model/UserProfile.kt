package com.macroai.tracker.domain.model

data class UserProfile(
    val weightKg: Double = 75.0,
    val heightCm: Double = 178.0,
    val age: Int = 30,
    val biologicalSex: BiologicalSex = BiologicalSex.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val tdeeMode: TdeeMode = TdeeMode.FORMULA_MULTIPLIER,
    val energyTargetMode: EnergyTargetMode = EnergyTargetMode.SET_CALORIE_GOAL,
    val customCalorieTargetKcal: Int = 2100,
    val manualOverrideKcal: Int = customCalorieTargetKcal,
    val targetDeficitKcal: Int = 500,
    val macroInputMode: MacroInputMode = MacroInputMode.PERCENTAGE_SPLIT,
    val proteinTargetRatio: Double = 0.35,
    val carbsTargetRatio: Double = 0.35,
    val fatTargetRatio: Double = 0.30,
    val customProteinGrams: Int = 175,
    val customCarbsGrams: Int = 175,
    val customFatGrams: Int = 67,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val autoSyncHealthConnectBiometrics: Boolean = true,
    val selectedAiModel: AiModel = AiModel.DEFAULT,
    val customGeminiApiKey: String? = null,
    val customSheetsWebhookUrl: String? = null
)
