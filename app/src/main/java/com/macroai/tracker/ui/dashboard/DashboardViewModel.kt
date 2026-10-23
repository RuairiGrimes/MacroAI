package com.macroai.tracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macroai.tracker.data.health.HealthConnectManager
import com.macroai.tracker.data.health.HealthConnectSnapshot
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.data.repository.NutritionRepository
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.UserProfile
import com.macroai.tracker.domain.util.DailyNutritionSummary
import com.macroai.tracker.domain.util.EndOfDayReport
import com.macroai.tracker.domain.util.SeedOilDetector
import com.macroai.tracker.domain.util.TdeeCalculator
import com.macroai.tracker.domain.util.TdeeResult
import com.macroai.tracker.domain.util.UpfMetricsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val logs: List<FoodLogEntity> = emptyList(),
    val summary: DailyNutritionSummary = DailyNutritionSummary(),
    val userProfile: UserProfile = UserProfile(),
    val healthSnapshot: HealthConnectSnapshot = HealthConnectSnapshot(),
    val tdeeResult: TdeeResult = TdeeCalculator.calculateTdee(UserProfile()),
    val unsyncedCount: Int = 0,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val isAnalyzingEndDay: Boolean = false,
    val endOfDayReport: EndOfDayReport? = null,
    val showEndDayOverviewModal: Boolean = false
)

private data class SyncAndEndDayAuxState(
    val unsyncedCount: Int,
    val isSyncing: Boolean,
    val statusMessage: String?,
    val isAnalyzingEndDay: Boolean,
    val reportsByDate: Map<LocalDate, EndOfDayReport>,
    val showEndDayOverviewModal: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val healthSnapshotFlow = MutableStateFlow(HealthConnectSnapshot())
    private val isSyncingFlow = MutableStateFlow(false)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val isAnalyzingEndDayFlow = MutableStateFlow(false)
    private val endOfDayReportsByDateFlow = MutableStateFlow<Map<LocalDate, EndOfDayReport>>(emptyMap())
    private val showEndDayOverviewModalFlow = MutableStateFlow(false)

    private val logsForSelectedDateFlow = selectedDateFlow.flatMapLatest { date ->
        nutritionRepository.observeLogsForDate(date)
    }

    private val auxStateFlow = combine(
        combine(
            nutritionRepository.observeUnsyncedCount(),
            isSyncingFlow,
            statusMessageFlow
        ) { unsynced, syncing, msg -> Triple(unsynced, syncing, msg) },
        combine(
            isAnalyzingEndDayFlow,
            endOfDayReportsByDateFlow,
            showEndDayOverviewModalFlow
        ) { analyzing, reports, showModal -> Triple(analyzing, reports, showModal) }
    ) { syncTriple, endDayTriple ->
        SyncAndEndDayAuxState(
            unsyncedCount = syncTriple.first,
            isSyncing = syncTriple.second,
            statusMessage = syncTriple.third,
            isAnalyzingEndDay = endDayTriple.first,
            reportsByDate = endDayTriple.second,
            showEndDayOverviewModal = endDayTriple.third
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            selectedDateFlow,
            logsForSelectedDateFlow,
            userPreferencesDataStore.userProfileFlow
        ) { date, logs, profile -> Triple(date, logs, profile) },
        combine(
            healthSnapshotFlow,
            auxStateFlow
        ) { hcSnapshot, aux -> Pair(hcSnapshot, aux) }
    ) { mainTriple, secondPair ->
        val date = mainTriple.first
        val logs = mainTriple.second
        val profile = mainTriple.third

        val hcSnapshot = secondPair.first
        val aux = secondPair.second

        val summary = UpfMetricsCalculator.summarizeDay(logs)
        val tdee = TdeeCalculator.calculateTdee(
            profile = profile,
            healthConnectWeightKg = hcSnapshot.latestWeightKg,
            healthConnectHeightCm = hcSnapshot.latestHeightCm,
            healthConnectActiveCaloriesKcal = hcSnapshot.activeCaloriesBurnedKcal
        )
        DashboardUiState(
            selectedDate = date,
            logs = logs,
            summary = summary,
            userProfile = profile,
            healthSnapshot = hcSnapshot,
            tdeeResult = tdee,
            unsyncedCount = aux.unsyncedCount,
            isSyncing = aux.isSyncing,
            statusMessage = aux.statusMessage,
            isAnalyzingEndDay = aux.isAnalyzingEndDay,
            endOfDayReport = aux.reportsByDate[date],
            showEndDayOverviewModal = aux.showEndDayOverviewModal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        refreshHealthConnectData()
    }

    fun refreshHealthConnectData() {
        viewModelScope.launch {
            val snapshot = healthConnectManager.readSnapshotForDate(selectedDateFlow.value)
            healthSnapshotFlow.value = snapshot
            val profile = uiState.value.userProfile
            if (profile.autoSyncHealthConnectBiometrics) {
                userPreferencesDataStore.syncBiometricsFromHealthConnect(
                    weightKg = snapshot.latestWeightKg,
                    heightCm = snapshot.latestHeightCm
                )
            }
        }
    }

    fun selectPreviousDay() {
        selectedDateFlow.value = selectedDateFlow.value.minusDays(1)
        refreshHealthConnectData()
    }

    fun selectNextDay() {
        val next = selectedDateFlow.value.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            selectedDateFlow.value = next
            refreshHealthConnectData()
        }
    }

    fun selectToday() {
        selectedDateFlow.value = LocalDate.now()
        refreshHealthConnectData()
    }

    fun deleteFoodLog(id: String) {
        viewModelScope.launch {
            nutritionRepository.deleteFoodLog(id)
        }
    }

    fun saveManualFoodLog(
        name: String,
        mealType: MealType,
        servingWeightGrams: Double,
        calories: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        pufaG: Double,
        satFatG: Double,
        fibreG: Double,
        novaGroup: Int?,
        ingredientsOrOilsNote: String
    ) {
        val seedOilResult = SeedOilDetector.analyzeIngredients(ingredientsOrOilsNote)
        val validNova = novaGroup?.takeIf { it in 1..4 }
        val entity = FoodLogEntity(
            mealType = mealType,
            name = name.ifBlank { "Manual Entry" },
            source = LogSource.MANUAL,
            servingWeightGrams = servingWeightGrams,
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            polyunsaturatedFatG = pufaG,
            saturatedFatG = satFatG,
            fibreG = fibreG,
            novaGroup = validNova,
            isUpf = (validNova == 4),
            containsSeedOils = seedOilResult.containsSeedOils,
            detectedSeedOils = seedOilResult.detectedOilsCsv
        )
        viewModelScope.launch {
            nutritionRepository.saveFoodLog(entity)
        }
    }

    fun updateSelectedAiModel(model: com.macroai.tracker.domain.model.AiModel) {
        viewModelScope.launch {
            userPreferencesDataStore.updateSelectedAiModel(model)
        }
    }

    fun confirmStandardEndDay() {
        val currentState = uiState.value
        val targetDate = currentState.selectedDate
        viewModelScope.launch {
            val report = nutritionRepository.generateEndOfDayReport(
                logs = currentState.logs,
                summary = currentState.summary,
                tdee = currentState.tdeeResult,
                weightUnit = currentState.userProfile.weightUnit,
                useAi = false
            )
            endOfDayReportsByDateFlow.value = endOfDayReportsByDateFlow.value + (targetDate to report)
            showEndDayOverviewModalFlow.value = true
        }
    }

    fun confirmAiEndDay(modelOverride: com.macroai.tracker.domain.model.AiModel? = null) {
        if (isAnalyzingEndDayFlow.value) return
        val currentState = uiState.value
        val targetDate = currentState.selectedDate
        val activeModel = modelOverride ?: currentState.userProfile.selectedAiModel
        viewModelScope.launch {
            if (modelOverride != null && modelOverride != currentState.userProfile.selectedAiModel) {
                userPreferencesDataStore.updateSelectedAiModel(modelOverride)
            }
            isAnalyzingEndDayFlow.value = true
            showEndDayOverviewModalFlow.value = true
            val report = nutritionRepository.generateEndOfDayReport(
                logs = currentState.logs,
                summary = currentState.summary,
                tdee = currentState.tdeeResult,
                weightUnit = currentState.userProfile.weightUnit,
                useAi = true,
                modelId = activeModel.modelId
            )
            endOfDayReportsByDateFlow.value = endOfDayReportsByDateFlow.value + (targetDate to report)
            isAnalyzingEndDayFlow.value = false
        }
    }

    fun confirmEndDay() {
        confirmAiEndDay()
    }

    fun openEndDayOverview() {
        showEndDayOverviewModalFlow.value = true
    }

    fun dismissEndDayOverview() {
        showEndDayOverviewModalFlow.value = false
    }

    fun syncToSheetsNow() {
        if (isSyncingFlow.value) return
        viewModelScope.launch {
            isSyncingFlow.value = true
            val result = nutritionRepository.syncPendingLogsNow()
            isSyncingFlow.value = false
            statusMessageFlow.value = result.fold(
                onSuccess = { count ->
                    if (count == 0) "All logs are already synced to Google Sheets."
                    else "Synced $count food log(s) to Google Sheets."
                },
                onFailure = { err ->
                    err.message ?: "Sync failed; queued for background retry."
                }
            )
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }
}

