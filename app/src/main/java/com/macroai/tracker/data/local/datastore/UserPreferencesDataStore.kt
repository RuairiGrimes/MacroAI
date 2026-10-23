package com.macroai.tracker.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.macroai.tracker.domain.model.ActivityLevel
import com.macroai.tracker.domain.model.AiModel
import com.macroai.tracker.domain.model.BiologicalSex
import com.macroai.tracker.domain.model.MacroInputMode
import com.macroai.tracker.domain.model.TdeeMode
import com.macroai.tracker.domain.model.UserProfile
import com.macroai.tracker.domain.model.WeightUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "macro_ai_user_prefs"
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val HEIGHT_CM = doublePreferencesKey("height_cm")
        val AGE = intPreferencesKey("age")
        val BIOLOGICAL_SEX = stringPreferencesKey("biological_sex")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val TDEE_MODE = stringPreferencesKey("tdee_mode")
        val ENERGY_TARGET_MODE = stringPreferencesKey("energy_target_mode")
        val CUSTOM_CALORIE_TARGET_KCAL = intPreferencesKey("custom_calorie_target_kcal")
        val MANUAL_OVERRIDE_KCAL = intPreferencesKey("manual_override_kcal")
        val TARGET_DEFICIT_KCAL = intPreferencesKey("target_deficit_kcal")
        val MACRO_INPUT_MODE = stringPreferencesKey("macro_input_mode")
        val PROTEIN_RATIO = doublePreferencesKey("protein_ratio")
        val CARBS_RATIO = doublePreferencesKey("carbs_ratio")
        val FAT_RATIO = doublePreferencesKey("fat_ratio")
        val CUSTOM_PROTEIN_G = intPreferencesKey("custom_protein_g")
        val CUSTOM_CARBS_G = intPreferencesKey("custom_carbs_g")
        val CUSTOM_FAT_G = intPreferencesKey("custom_fat_g")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val AUTO_SYNC_HC_BIOMETRICS = booleanPreferencesKey("auto_sync_hc_biometrics")
        val SELECTED_AI_MODEL = stringPreferencesKey("selected_ai_model")
        val CUSTOM_GEMINI_API_KEY = stringPreferencesKey("custom_gemini_api_key")
        val CUSTOM_SHEETS_WEBHOOK_URL = stringPreferencesKey("custom_sheets_webhook_url")
    }

    val userProfileFlow: Flow<UserProfile> = context.userPreferencesDataStore.data.map { prefs ->
        val sex = prefs[Keys.BIOLOGICAL_SEX]?.let {
            runCatching { BiologicalSex.valueOf(it) }.getOrNull()
        } ?: BiologicalSex.MALE

        val activityLevel = prefs[Keys.ACTIVITY_LEVEL]?.let {
            runCatching { ActivityLevel.valueOf(it) }.getOrNull()
        } ?: ActivityLevel.MODERATE

        val rawTdeeMode = prefs[Keys.TDEE_MODE]?.let {
            runCatching { TdeeMode.valueOf(it) }.getOrNull()
        }

        val energyTargetMode = prefs[Keys.ENERGY_TARGET_MODE]?.let {
            runCatching { com.macroai.tracker.domain.model.EnergyTargetMode.valueOf(it) }.getOrNull()
        } ?: if (rawTdeeMode == TdeeMode.MANUAL_OVERRIDE) {
            com.macroai.tracker.domain.model.EnergyTargetMode.SET_CALORIE_GOAL
        } else {
            com.macroai.tracker.domain.model.EnergyTargetMode.SET_CALORIE_GOAL
        }

        val tdeeMode = if (rawTdeeMode == TdeeMode.MANUAL_OVERRIDE && energyTargetMode == com.macroai.tracker.domain.model.EnergyTargetMode.SET_CALORIE_GOAL) {
            TdeeMode.FORMULA_MULTIPLIER
        } else {
            rawTdeeMode ?: TdeeMode.FORMULA_MULTIPLIER
        }

        val macroMode = prefs[Keys.MACRO_INPUT_MODE]?.let {
            runCatching { MacroInputMode.valueOf(it) }.getOrNull()
        } ?: MacroInputMode.PERCENTAGE_SPLIT

        val weightUnit = prefs[Keys.WEIGHT_UNIT]?.let {
            runCatching { WeightUnit.valueOf(it) }.getOrNull()
        } ?: WeightUnit.KG

        val autoSyncHc = prefs[Keys.AUTO_SYNC_HC_BIOMETRICS] ?: true
        val selectedAiModel = AiModel.fromModelId(prefs[Keys.SELECTED_AI_MODEL])
        val calorieTarget = prefs[Keys.CUSTOM_CALORIE_TARGET_KCAL]
            ?: prefs[Keys.MANUAL_OVERRIDE_KCAL]
            ?: 2100

        UserProfile(
            weightKg = prefs[Keys.WEIGHT_KG] ?: 75.0,
            heightCm = prefs[Keys.HEIGHT_CM] ?: 178.0,
            age = prefs[Keys.AGE] ?: 30,
            biologicalSex = sex,
            activityLevel = activityLevel,
            tdeeMode = tdeeMode,
            energyTargetMode = energyTargetMode,
            customCalorieTargetKcal = calorieTarget,
            manualOverrideKcal = calorieTarget,
            targetDeficitKcal = prefs[Keys.TARGET_DEFICIT_KCAL] ?: 500,
            macroInputMode = macroMode,
            proteinTargetRatio = prefs[Keys.PROTEIN_RATIO] ?: 0.35,
            carbsTargetRatio = prefs[Keys.CARBS_RATIO] ?: 0.35,
            fatTargetRatio = prefs[Keys.FAT_RATIO] ?: 0.30,
            customProteinGrams = prefs[Keys.CUSTOM_PROTEIN_G] ?: 175,
            customCarbsGrams = prefs[Keys.CUSTOM_CARBS_G] ?: 175,
            customFatGrams = prefs[Keys.CUSTOM_FAT_G] ?: 67,
            weightUnit = weightUnit,
            autoSyncHealthConnectBiometrics = autoSyncHc,
            selectedAiModel = selectedAiModel,
            customGeminiApiKey = prefs[Keys.CUSTOM_GEMINI_API_KEY]?.takeIf { it.isNotBlank() },
            customSheetsWebhookUrl = prefs[Keys.CUSTOM_SHEETS_WEBHOOK_URL]?.takeIf { it.isNotBlank() }
        )
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.WEIGHT_KG] = profile.weightKg
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.AGE] = profile.age
            prefs[Keys.BIOLOGICAL_SEX] = profile.biologicalSex.name
            prefs[Keys.ACTIVITY_LEVEL] = profile.activityLevel.name
            prefs[Keys.TDEE_MODE] = profile.tdeeMode.name
            prefs[Keys.ENERGY_TARGET_MODE] = profile.energyTargetMode.name
            prefs[Keys.CUSTOM_CALORIE_TARGET_KCAL] = profile.customCalorieTargetKcal
            prefs[Keys.MANUAL_OVERRIDE_KCAL] = profile.customCalorieTargetKcal
            prefs[Keys.TARGET_DEFICIT_KCAL] = profile.targetDeficitKcal
            prefs[Keys.MACRO_INPUT_MODE] = profile.macroInputMode.name
            prefs[Keys.PROTEIN_RATIO] = profile.proteinTargetRatio
            prefs[Keys.CARBS_RATIO] = profile.carbsTargetRatio
            prefs[Keys.FAT_RATIO] = profile.fatTargetRatio
            prefs[Keys.CUSTOM_PROTEIN_G] = profile.customProteinGrams
            prefs[Keys.CUSTOM_CARBS_G] = profile.customCarbsGrams
            prefs[Keys.CUSTOM_FAT_G] = profile.customFatGrams
            prefs[Keys.WEIGHT_UNIT] = profile.weightUnit.name
            prefs[Keys.AUTO_SYNC_HC_BIOMETRICS] = profile.autoSyncHealthConnectBiometrics
            prefs[Keys.SELECTED_AI_MODEL] = profile.selectedAiModel.modelId
            if (profile.customGeminiApiKey != null) {
                prefs[Keys.CUSTOM_GEMINI_API_KEY] = profile.customGeminiApiKey.trim()
            } else {
                prefs.remove(Keys.CUSTOM_GEMINI_API_KEY)
            }
            if (profile.customSheetsWebhookUrl != null) {
                prefs[Keys.CUSTOM_SHEETS_WEBHOOK_URL] = profile.customSheetsWebhookUrl.trim()
            } else {
                prefs.remove(Keys.CUSTOM_SHEETS_WEBHOOK_URL)
            }
        }
    }

    suspend fun updateCustomGeminiApiKey(key: String?) {
        context.userPreferencesDataStore.edit { prefs ->
            val trimmed = key?.trim()
            if (!trimmed.isNullOrEmpty()) {
                prefs[Keys.CUSTOM_GEMINI_API_KEY] = trimmed
            } else {
                prefs.remove(Keys.CUSTOM_GEMINI_API_KEY)
            }
        }
    }

    suspend fun updateCustomSheetsWebhookUrl(url: String?) {
        context.userPreferencesDataStore.edit { prefs ->
            val trimmed = url?.trim()
            if (!trimmed.isNullOrEmpty()) {
                prefs[Keys.CUSTOM_SHEETS_WEBHOOK_URL] = trimmed
            } else {
                prefs.remove(Keys.CUSTOM_SHEETS_WEBHOOK_URL)
            }
        }
    }

    suspend fun clearCustomCredentials() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs.remove(Keys.CUSTOM_GEMINI_API_KEY)
            prefs.remove(Keys.CUSTOM_SHEETS_WEBHOOK_URL)
        }
    }

    suspend fun syncBiometricsFromHealthConnect(weightKg: Double?, heightCm: Double?): Boolean {
        var changed = false
        context.userPreferencesDataStore.edit { prefs ->
            if (weightKg != null && weightKg > 20.0 && prefs[Keys.WEIGHT_KG] != weightKg) {
                prefs[Keys.WEIGHT_KG] = weightKg
                changed = true
            }
            if (heightCm != null && heightCm > 80.0 && prefs[Keys.HEIGHT_CM] != heightCm) {
                prefs[Keys.HEIGHT_CM] = heightCm
                changed = true
            }
        }
        return changed
    }

    suspend fun updateWeightUnit(unit: WeightUnit) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.WEIGHT_UNIT] = unit.name
        }
    }

    suspend fun updateAutoSyncBiometrics(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_HC_BIOMETRICS] = enabled
        }
    }

    suspend fun updateTdeeMode(mode: TdeeMode) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.TDEE_MODE] = mode.name
        }
    }

    suspend fun updateSelectedAiModel(model: AiModel) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.SELECTED_AI_MODEL] = model.modelId
        }
    }
}
