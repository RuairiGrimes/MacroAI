package com.macroai.tracker.domain.util

import com.macroai.tracker.domain.model.EnergyTargetMode
import com.macroai.tracker.domain.model.MacroInputMode
import com.macroai.tracker.domain.model.TdeeMode
import com.macroai.tracker.domain.model.UserProfile
import kotlin.math.roundToInt

data class TdeeResult(
    val bmrKcal: Int,
    val tdeeKcal: Int,             // Total Daily Energy Expenditure (Burned / Maintenance)
    val energyTargetKcal: Int,     // Daily Calorie Consumed Goal (Intake Budget)
    val plannedDeficitKcal: Int = tdeeKcal - energyTargetKcal,
    val maintenanceKcal: Int = tdeeKcal,
    val activeCaloriesKcal: Int,
    val effectiveWeightKg: Double,
    val effectiveHeightCm: Double,
    val isUsingHealthConnectWeight: Boolean,
    val isUsingHealthConnectHeight: Boolean,
    val isUsingHealthConnectActiveCalories: Boolean,
    val activeMode: TdeeMode,
    val energyTargetMode: EnergyTargetMode = EnergyTargetMode.SET_CALORIE_GOAL,
    val proteinTargetGrams: Int,
    val carbsTargetGrams: Int,
    val fatTargetGrams: Int
)

object TdeeCalculator {

    /**
     * Mifflin-St Jeor Equation:
     * BMR = (10 * weight_kg) + (6.25 * height_cm) - (5 * age) + sexOffset (+5 default)
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sexOffset: Int = 5
    ): Double {
        return (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + sexOffset
    }

    fun calculateTdee(
        profile: UserProfile,
        healthConnectWeightKg: Double? = null,
        healthConnectHeightCm: Double? = null,
        healthConnectActiveCaloriesKcal: Double? = null
    ): TdeeResult {
        val effectiveWeight = if (profile.autoSyncHealthConnectBiometrics) {
            healthConnectWeightKg?.takeIf { it > 20.0 } ?: profile.weightKg
        } else {
            profile.weightKg
        }
        val effectiveHeight = if (profile.autoSyncHealthConnectBiometrics) {
            healthConnectHeightCm?.takeIf { it > 80.0 } ?: profile.heightCm
        } else {
            profile.heightCm
        }
        val activeKcal = (healthConnectActiveCaloriesKcal ?: 0.0).coerceAtLeast(0.0)

        val bmr = calculateBmr(
            weightKg = effectiveWeight,
            heightCm = effectiveHeight,
            age = profile.age,
            sexOffset = profile.biologicalSex.mifflinOffset
        ).coerceAtLeast(800.0)

        // 1. TDEE: Total Daily Energy Expenditure (Calories Burned / Maintenance)
        val calculatedTdee = when (profile.tdeeMode) {
            TdeeMode.HEALTH_CONNECT_DYNAMIC -> (bmr + activeKcal).roundToInt()
            TdeeMode.FORMULA_MULTIPLIER -> (bmr * profile.activityLevel.multiplier).roundToInt()
            TdeeMode.MANUAL_OVERRIDE -> {
                if (profile.energyTargetMode == EnergyTargetMode.SET_CALORIE_GOAL) {
                    // Set calorie goal is the CONSUMED target, so TDEE is computed from formula
                    (bmr * profile.activityLevel.multiplier).roundToInt()
                } else {
                    profile.manualOverrideKcal.coerceAtLeast(800)
                }
            }
        }.coerceAtLeast(800)

        // 2. Energy Target: Total Calorie Consumed Goal (Intake Budget)
        val energyTarget = when (profile.energyTargetMode) {
            EnergyTargetMode.SET_CALORIE_GOAL -> profile.customCalorieTargetKcal.coerceAtLeast(800)
            EnergyTargetMode.TDEE_DEFICIT -> (calculatedTdee - profile.targetDeficitKcal).coerceAtLeast(800)
        }

        // 3. Macro targets based on the daily energy target (consumed intake goal)
        val (proteinGrams, carbsGrams, fatGrams) = when (profile.macroInputMode) {
            MacroInputMode.CUSTOM_GRAMS -> Triple(
                profile.customProteinGrams.coerceAtLeast(10),
                profile.customCarbsGrams.coerceAtLeast(0),
                profile.customFatGrams.coerceAtLeast(10)
            )
            MacroInputMode.PERCENTAGE_SPLIT -> Triple(
                ((energyTarget * profile.proteinTargetRatio) / 4.0).roundToInt().coerceAtLeast(10),
                ((energyTarget * profile.carbsTargetRatio) / 4.0).roundToInt().coerceAtLeast(0),
                ((energyTarget * profile.fatTargetRatio) / 9.0).roundToInt().coerceAtLeast(10)
            )
        }

        return TdeeResult(
            bmrKcal = bmr.roundToInt(),
            tdeeKcal = calculatedTdee,
            energyTargetKcal = energyTarget,
            plannedDeficitKcal = calculatedTdee - energyTarget,
            maintenanceKcal = calculatedTdee,
            activeCaloriesKcal = activeKcal.roundToInt(),
            effectiveWeightKg = effectiveWeight,
            effectiveHeightCm = effectiveHeight,
            isUsingHealthConnectWeight = profile.autoSyncHealthConnectBiometrics && healthConnectWeightKg != null && healthConnectWeightKg > 20.0,
            isUsingHealthConnectHeight = profile.autoSyncHealthConnectBiometrics && healthConnectHeightCm != null && healthConnectHeightCm > 80.0,
            isUsingHealthConnectActiveCalories = healthConnectActiveCaloriesKcal != null,
            activeMode = profile.tdeeMode,
            energyTargetMode = profile.energyTargetMode,
            proteinTargetGrams = proteinGrams,
            carbsTargetGrams = carbsGrams,
            fatTargetGrams = fatGrams
        )
    }
}
