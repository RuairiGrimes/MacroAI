package com.macroai.tracker.ui.widget

import android.content.Context
import com.macroai.tracker.data.local.MacroAiDatabase
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.domain.util.TdeeCalculator
import com.macroai.tracker.domain.util.UpfMetricsCalculator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun database(): MacroAiDatabase
    fun userPreferencesDataStore(): UserPreferencesDataStore
}

object WidgetDataProvider {

    suspend fun getSnapshot(context: Context): MacroAiWidgetSnapshot {
        return runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            val db = entryPoint.database()
            val prefs = entryPoint.userPreferencesDataStore()

            val now = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val startMillis = now.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val logs = db.foodLogDao().getLogsForDateRange(startMillis, endMillis)
            val profile = prefs.userProfileFlow.firstOrNull() ?: com.macroai.tracker.domain.model.UserProfile()

            val summary = UpfMetricsCalculator.summarizeDay(logs)
            val tdee = TdeeCalculator.calculateTdee(profile = profile)

            val targetKcal = tdee.energyTargetKcal.coerceAtLeast(500)
            val consumedKcal = summary.totalCalories.roundToInt()
            val remainingKcal = targetKcal - consumedKcal

            MacroAiWidgetSnapshot(
                calorieTarget = targetKcal,
                caloriesConsumed = consumedKcal,
                remainingCalories = remainingKcal,
                proteinConsumedG = summary.proteinG.toFloat(),
                proteinTargetG = tdee.proteinTargetGrams.toFloat().coerceAtLeast(1f),
                carbsConsumedG = summary.carbsG.toFloat(),
                carbsTargetG = tdee.carbsTargetGrams.toFloat().coerceAtLeast(1f),
                fatConsumedG = summary.fatG.toFloat(),
                fatTargetG = tdee.fatTargetGrams.toFloat().coerceAtLeast(1f),
                upfPercentage = summary.upfCaloriePercentage.toFloat(),
                seedOilCount = summary.seedOilItemCount
            )
        }.getOrElse {
            MacroAiWidgetSnapshot()
        }
    }
}

