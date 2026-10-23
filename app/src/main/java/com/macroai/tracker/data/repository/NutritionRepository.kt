package com.macroai.tracker.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.macroai.tracker.data.health.HealthConnectManager
import com.macroai.tracker.data.local.dao.FoodLogDao
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.data.remote.gemini.GeminiMealAnalyzer
import com.macroai.tracker.data.remote.gemini.dto.GeminiMealAnalysisDto
import com.macroai.tracker.data.remote.off.OpenFoodFactsApi
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.data.remote.sheets.SheetsWebhookClient
import com.macroai.tracker.data.worker.SheetsSyncWorker
import com.macroai.tracker.data.local.StapleIngredientsDatabase
import com.macroai.tracker.data.local.dao.SavedMealDao
import com.macroai.tracker.data.local.entity.SavedMealEntity
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.util.DailyNutritionSummary
import com.macroai.tracker.domain.util.EndOfDayCalculator
import com.macroai.tracker.domain.util.EndOfDayReport
import com.macroai.tracker.domain.util.TdeeResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodLogDao: FoodLogDao,
    private val savedMealDao: SavedMealDao,
    private val openFoodFactsApi: OpenFoodFactsApi,
    private val geminiMealAnalyzer: GeminiMealAnalyzer,
    private val sheetsWebhookClient: SheetsWebhookClient,
    private val healthConnectManager: HealthConnectManager
) {
    fun observeLogsForDate(date: LocalDate): Flow<List<FoodLogEntity>> {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return foodLogDao.observeLogsForDateRange(startMillis, endMillis)
    }

    fun observeUnsyncedCount(): Flow<Int> = foodLogDao.observeUnsyncedCount()

    suspend fun getFoodLogById(id: String): FoodLogEntity? = foodLogDao.getFoodLogById(id)

    fun observeFoodLogById(id: String): Flow<FoodLogEntity?> = foodLogDao.observeFoodLogById(id)

    suspend fun saveFoodLog(entity: FoodLogEntity) {
        foodLogDao.insertFoodLog(entity)
        healthConnectManager.writeNutritionRecord(entity)
        SheetsSyncWorker.enqueue(context)
        com.macroai.tracker.ui.widget.MacroAiWidgetManager.updateWidget(context)
    }

    suspend fun updateFoodLog(entity: FoodLogEntity) {
        foodLogDao.updateFoodLog(entity)
        healthConnectManager.deleteNutritionRecord(entity.id)
        healthConnectManager.writeNutritionRecord(entity)
        SheetsSyncWorker.enqueue(context)
        com.macroai.tracker.ui.widget.MacroAiWidgetManager.updateWidget(context)
    }

    suspend fun deleteFoodLog(id: String) {
        foodLogDao.deleteFoodLogById(id)
        healthConnectManager.deleteNutritionRecord(id)
        com.macroai.tracker.ui.widget.MacroAiWidgetManager.updateWidget(context)
    }

    // --- Saved Meals / Recipes ---

    fun observeAllSavedMeals(): Flow<List<SavedMealEntity>> = savedMealDao.observeAllSavedMeals()

    suspend fun getSavedMealById(id: String): SavedMealEntity? = savedMealDao.getSavedMealById(id)

    suspend fun saveSavedMeal(meal: SavedMealEntity) {
        savedMealDao.insertSavedMeal(meal)
    }

    suspend fun updateSavedMeal(meal: SavedMealEntity) {
        savedMealDao.updateSavedMeal(meal)
    }

    suspend fun deleteSavedMeal(id: String) {
        savedMealDao.deleteSavedMealById(id)
    }

    suspend fun logSavedMeal(
        meal: SavedMealEntity,
        servingsToLog: Double,
        mealType: MealType
    ): FoodLogEntity {
        val foodLog = meal.toFoodLogEntity(servingsToLog = servingsToLog, mealType = mealType)
        saveFoodLog(foodLog)
        savedMealDao.updateSavedMeal(meal.copy(lastUsedAt = System.currentTimeMillis()))
        return foodLog
    }

    suspend fun parseRecipePrompt(
        prompt: String,
        modelId: String = GeminiMealAnalyzer.MODEL_NAME
    ): Result<GeminiMealAnalysisDto> {
        return geminiMealAnalyzer.analyzeTextPrompt(prompt, modelId = modelId)
    }

    suspend fun lookupBarcode(barcode: String): Result<NormalizedOffProduct> {
        return openFoodFactsApi.getProductByBarcode(barcode)
    }

    suspend fun searchFoods(query: String): Result<List<NormalizedOffProduct>> = runCatching {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@runCatching emptyList()

        val queryLower = cleanQuery.lowercase()
        val rawTokens = queryLower.split(Regex("""[\s,.-]+""")).filter { it.isNotEmpty() }
        val stemmedTokens = rawTokens.map { StapleIngredientsDatabase.stemWord(it) }

        // 1. Search local Room food_logs & saved_meals (previously scanned / logged items & recipes)
        val localLoggedMatches = runCatching {
            foodLogDao.getRecentDistinctFoodLogs()
                .map { it.toNormalizedOffProduct() }
                .mapNotNull { product ->
                    val score = StapleIngredientsDatabase.scoreProductRelevance(product, rawTokens, stemmedTokens, queryLower)
                    if (score > 0) product to score else null
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }.getOrDefault(emptyList())

        val localRecipeMatches = runCatching {
            savedMealDao.getAllSavedMealsList()
                .map { it.toNormalizedOffProduct() }
                .mapNotNull { product ->
                    val score = StapleIngredientsDatabase.scoreProductRelevance(product, rawTokens, stemmedTokens, queryLower)
                    if (score > 0) product to score else null
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }.getOrDefault(emptyList())

        // 2. Search verified offline UK/Irish staples database
        val pureStaples = StapleIngredientsDatabase.search(cleanQuery)

        // 3. Search remote Open Food Facts API
        val offResult = openFoodFactsApi.searchFoods(cleanQuery)
        val offProducts = offResult.getOrDefault(emptyList())

        // Combine & deduplicate: Verified staples & local logged/recipe matches first, followed by remote products
        val seen = mutableSetOf<String>()
        val combined = mutableListOf<NormalizedOffProduct>()

        fun addIfUnique(list: List<NormalizedOffProduct>) {
            for (item in list) {
                val key = item.name.lowercase().trim()
                if (key !in seen) {
                    seen.add(key)
                    combined.add(item)
                }
            }
        }

        addIfUnique(localRecipeMatches)
        addIfUnique(localLoggedMatches)
        addIfUnique(pureStaples)
        addIfUnique(offProducts)

        combined
    }

    suspend fun preEncodeBitmap(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        geminiMealAnalyzer.compressAndEncodeBitmap(bitmap)
    }

    suspend fun analyzePreEncodedMealPhoto(
        base64Jpeg: String,
        userContextPrompt: String?,
        modelId: String = GeminiMealAnalyzer.MODEL_NAME
    ): Result<GeminiMealAnalysisDto> {
        return geminiMealAnalyzer.analyzeEncodedPhoto(base64Jpeg, userContextPrompt, modelId = modelId)
    }

    suspend fun analyzeMealPhoto(
        bitmap: Bitmap,
        userContextPrompt: String?,
        modelId: String = GeminiMealAnalyzer.MODEL_NAME
    ): Result<GeminiMealAnalysisDto> {
        return geminiMealAnalyzer.analyzeMealPhoto(bitmap, userContextPrompt, modelId = modelId)
    }

    /**
     * Generates the End of Day nutrition overview and 4-week weight projection.
     * Supports both instantaneous on-device calculation (`useAi = false`) and Gemini AI analysis (`useAi = true`).
     */
    suspend fun generateEndOfDayReport(
        logs: List<FoodLogEntity>,
        summary: DailyNutritionSummary,
        tdee: TdeeResult,
        weightUnit: com.macroai.tracker.domain.model.WeightUnit = com.macroai.tracker.domain.model.WeightUnit.KG,
        useAi: Boolean = true,
        modelId: String = GeminiMealAnalyzer.MODEL_NAME
    ): EndOfDayReport {
        if (!useAi || logs.isEmpty()) {
            return EndOfDayCalculator.generateLocalReport(
                logs = logs,
                summary = summary,
                tdee = tdee,
                weightUnit = weightUnit
            )
        }

        val metrics = EndOfDayCalculator.calculateMetrics(
            summary = summary,
            tdee = tdee,
            weightUnit = weightUnit
        )

        val prompt = EndOfDayCalculator.buildGeminiDayPrompt(
            logs = logs,
            summary = summary,
            tdee = tdee,
            metrics = metrics
        )

        val aiModelDisplayName = com.macroai.tracker.domain.model.AiModel.fromModelId(modelId).displayName
        val aiResult = geminiMealAnalyzer.analyzeDailyNutrition(prompt, modelId = modelId)
        return aiResult.fold(
            onSuccess = { dto ->
                EndOfDayReport(
                    metrics = metrics,
                    aiOverview = dto,
                    isAiGenerated = true,
                    modelUsed = aiModelDisplayName
                )
            },
            onFailure = {
                EndOfDayReport(
                    metrics = metrics,
                    aiOverview = EndOfDayCalculator.generateFallbackOverview(logs, summary, tdee, metrics),
                    isAiGenerated = false,
                    modelUsed = "On-Device Clinical Engine (AI Offline/Quota Fallback)"
                )
            }
        )
    }

    /**
     * Triggers an immediate sync check or schedules WorkManager when connectivity returns.
     */
    suspend fun syncPendingLogsNow(): Result<Int> = runCatching {
        val unsynced = foodLogDao.getUnsyncedLogs()
        if (unsynced.isEmpty()) return@runCatching 0
        val ok = sheetsWebhookClient.postUnsyncedLogs(unsynced).getOrThrow()
        if (ok) {
            foodLogDao.markLogsAsSynced(unsynced.map { it.id })
            unsynced.size
        } else {
            SheetsSyncWorker.enqueue(context)
            throw IllegalStateException("Webhook returned non-200 status; queued background retry.")
        }
    }
}
