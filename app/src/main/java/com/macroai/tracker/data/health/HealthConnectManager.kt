package com.macroai.tracker.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.domain.model.MealType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.health.connect.client.records.MealType as HcMealType

data class HealthConnectSnapshot(
    val isAvailable: Boolean = false,
    val hasAllPermissions: Boolean = false,
    val hasWriteNutritionPermission: Boolean = false,
    val hasBiometricsPermissions: Boolean = false,
    val latestWeightKg: Double? = null,
    val latestWeightTime: Instant? = null,
    val latestHeightCm: Double? = null,
    val latestHeightTime: Instant? = null,
    val activeCaloriesBurnedKcal: Double? = null
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val writeNutritionPermission =
        HealthPermission.getWritePermission(NutritionRecord::class)
    private val readNutritionPermission =
        HealthPermission.getReadPermission(NutritionRecord::class)

    val biometricsPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        readNutritionPermission,
        writeNutritionPermission
    )

    fun isAvailable(): Boolean {
        return runCatching {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }.getOrDefault(false)
    }

    private fun getClientOrNull(): HealthConnectClient? {
        if (!isAvailable()) return null
        return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    fun createRequestPermissionContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = getClientOrNull() ?: return false
        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        }.getOrDefault(false)
    }

    /**
     * Writes a consumed meal (`FoodLogEntity`) into Android Health Connect as a `NutritionRecord`
     * with full macro, lipid (PUFA/MUFA/SatFat), fibre, and micronutrient breakdown.
     */
    suspend fun writeNutritionRecord(entity: FoodLogEntity): Boolean {
        val client = getClientOrNull() ?: return false
        val granted = runCatching { client.permissionController.getGrantedPermissions() }
            .getOrDefault(emptySet())
        if (writeNutritionPermission !in granted) return false

        return runCatching {
            val startInstant = Instant.ofEpochMilli(entity.timestamp)
            val endInstant = startInstant.plusSeconds(60)
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

            val hcMealType = when (entity.mealType) {
                MealType.BREAKFAST -> HcMealType.MEAL_TYPE_BREAKFAST
                MealType.LUNCH -> HcMealType.MEAL_TYPE_LUNCH
                MealType.DINNER -> HcMealType.MEAL_TYPE_DINNER
                MealType.SNACK -> HcMealType.MEAL_TYPE_SNACK
            }

            val record = NutritionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                name = entity.name,
                mealType = hcMealType,
                energy = Energy.kilocalories(entity.calories.coerceAtLeast(0.0)),
                protein = Mass.grams(entity.proteinG.coerceAtLeast(0.0)),
                totalCarbohydrate = Mass.grams(entity.carbsG.coerceAtLeast(0.0)),
                totalFat = Mass.grams(entity.fatG.coerceAtLeast(0.0)),
                saturatedFat = Mass.grams(entity.saturatedFatG.coerceAtLeast(0.0)),
                polyunsaturatedFat = Mass.grams(entity.polyunsaturatedFatG.coerceAtLeast(0.0)),
                monounsaturatedFat = Mass.grams(entity.monounsaturatedFatG.coerceAtLeast(0.0)),
                dietaryFiber = Mass.grams(entity.fibreG.coerceAtLeast(0.0)),
                sugar = Mass.grams(entity.sugarG.coerceAtLeast(0.0)),
                sodium = Mass.milligrams(entity.sodiumMg.coerceAtLeast(0.0)),
                potassium = Mass.milligrams(entity.potassiumMg.coerceAtLeast(0.0)),
                iron = Mass.milligrams(entity.ironMg.coerceAtLeast(0.0)),
                calcium = Mass.milligrams(entity.calciumMg.coerceAtLeast(0.0)),
                vitaminC = Mass.milligrams(entity.vitaminCMg.coerceAtLeast(0.0)),
                vitaminD = Mass.micrograms(entity.vitaminDUg.coerceAtLeast(0.0)),
                magnesium = Mass.milligrams(entity.magnesiumMg.coerceAtLeast(0.0)),
                metadata = Metadata(clientRecordId = entity.id)
            )
            client.insertRecords(listOf(record))
            true
        }.getOrDefault(false)
    }

    suspend fun deleteNutritionRecord(clientRecordId: String): Boolean {
        val client = getClientOrNull() ?: return false
        val granted = runCatching { client.permissionController.getGrantedPermissions() }
            .getOrDefault(emptySet())
        if (writeNutritionPermission !in granted) return false

        return runCatching {
            client.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(clientRecordId)
            )
            true
        }.getOrDefault(false)
    }

    suspend fun readSnapshotForDate(date: LocalDate = LocalDate.now()): HealthConnectSnapshot {
        val client = getClientOrNull()
            ?: return HealthConnectSnapshot(isAvailable = false, hasAllPermissions = false)

        val granted = runCatching { client.permissionController.getGrantedPermissions() }
            .getOrDefault(emptySet())
        val hasPermissions = granted.containsAll(requiredPermissions)
        val hasWriteNutrition = writeNutritionPermission in granted
        val hasBiometrics = biometricsPermissions.all { it in granted }

        if (granted.isEmpty()) {
            return HealthConnectSnapshot(
                isAvailable = true,
                hasAllPermissions = false,
                hasWriteNutritionPermission = false,
                hasBiometricsPermissions = false
            )
        }

        val now = Instant.now()
        val weightRecord = runCatching {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        now.minus(730, ChronoUnit.DAYS),
                        now.plus(1, ChronoUnit.DAYS)
                    )
                )
            )
            response.records.maxByOrNull { it.time }
        }.getOrNull()
        val weightKg = weightRecord?.weight?.inKilograms
        val weightTime = weightRecord?.time

        val heightRecord = runCatching {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        now.minus(3650, ChronoUnit.DAYS),
                        now.plus(1, ChronoUnit.DAYS)
                    )
                )
            )
            response.records.maxByOrNull { it.time }
        }.getOrNull()
        val heightCm = heightRecord?.height?.inMeters?.let { it * 100.0 }
        val heightTime = heightRecord?.time

        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().let {
            if (it.isAfter(now)) now else it
        }

        val activeKcal = if (endOfDay.isAfter(startOfDay)) {
            runCatching {
                val aggregateResult = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                aggregateResult[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            }.getOrNull()
        } else {
            0.0
        }

        return HealthConnectSnapshot(
            isAvailable = true,
            hasAllPermissions = hasPermissions,
            hasWriteNutritionPermission = hasWriteNutrition,
            hasBiometricsPermissions = hasBiometrics,
            latestWeightKg = weightKg,
            latestWeightTime = weightTime,
            latestHeightCm = heightCm,
            latestHeightTime = heightTime,
            activeCaloriesBurnedKcal = activeKcal
        )
    }
}
