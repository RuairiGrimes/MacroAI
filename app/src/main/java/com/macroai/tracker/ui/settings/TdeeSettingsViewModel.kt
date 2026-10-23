package com.macroai.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macroai.tracker.data.health.HealthConnectManager
import com.macroai.tracker.data.health.HealthConnectSnapshot
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.data.remote.gemini.GeminiMealAnalyzer
import com.macroai.tracker.data.remote.sheets.SheetsWebhookClient
import com.macroai.tracker.domain.model.UserProfile
import com.macroai.tracker.domain.model.WeightUnit
import com.macroai.tracker.domain.util.TdeeCalculator
import com.macroai.tracker.domain.util.TdeeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class TdeeSettingsUiState(
    val userProfile: UserProfile = UserProfile(),
    val healthSnapshot: HealthConnectSnapshot = HealthConnectSnapshot(),
    val tdeePreview: TdeeResult = TdeeCalculator.calculateTdee(UserProfile()),
    val savedFeedback: Boolean = false,
    val syncMessage: String? = null,
    val isTestingGeminiKey: Boolean = false,
    val geminiKeyTestResult: String? = null,
    val isTestingSheetsWebhook: Boolean = false,
    val sheetsWebhookTestResult: String? = null,
    val showSetupGuideDialog: Boolean = false
)

@HiltViewModel
class TdeeSettingsViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    val healthConnectManager: HealthConnectManager,
    private val geminiMealAnalyzer: GeminiMealAnalyzer,
    private val sheetsWebhookClient: SheetsWebhookClient
) : ViewModel() {

    private val healthSnapshotFlow = MutableStateFlow(HealthConnectSnapshot())
    private val savedFeedbackFlow = MutableStateFlow(false)
    private val syncMessageFlow = MutableStateFlow<String?>(null)
    private val isTestingGeminiKeyFlow = MutableStateFlow(false)
    private val geminiKeyTestResultFlow = MutableStateFlow<String?>(null)
    private val isTestingSheetsWebhookFlow = MutableStateFlow(false)
    private val sheetsWebhookTestResultFlow = MutableStateFlow<String?>(null)
    private val showSetupGuideDialogFlow = MutableStateFlow(false)

    val uiState: StateFlow<TdeeSettingsUiState> = combine(
        combine(
            userPreferencesDataStore.userProfileFlow,
            healthSnapshotFlow,
            savedFeedbackFlow,
            syncMessageFlow
        ) { profile, hc, saved, syncMsg ->
            val preview = TdeeCalculator.calculateTdee(
                profile = profile,
                healthConnectWeightKg = hc.latestWeightKg,
                healthConnectHeightCm = hc.latestHeightCm,
                healthConnectActiveCaloriesKcal = hc.activeCaloriesBurnedKcal
            )
            Triple(profile, hc, preview) to Pair(saved, syncMsg)
        },
        combine(
            isTestingGeminiKeyFlow,
            geminiKeyTestResultFlow,
            isTestingSheetsWebhookFlow,
            sheetsWebhookTestResultFlow,
            showSetupGuideDialogFlow
        ) { testingKey, keyResult, testingSheet, sheetResult, showGuide ->
            StateExtras(testingKey, keyResult, testingSheet, sheetResult, showGuide)
        }
    ) { (mainData, feedbackData), extras ->
        val (profile, hc, preview) = mainData
        val (saved, syncMsg) = feedbackData

        TdeeSettingsUiState(
            userProfile = profile,
            healthSnapshot = hc,
            tdeePreview = preview,
            savedFeedback = saved,
            syncMessage = syncMsg,
            isTestingGeminiKey = extras.testingKey,
            geminiKeyTestResult = extras.keyResult,
            isTestingSheetsWebhook = extras.testingSheet,
            sheetsWebhookTestResult = extras.sheetResult,
            showSetupGuideDialog = extras.showGuide
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TdeeSettingsUiState()
    )

    private data class StateExtras(
        val testingKey: Boolean,
        val keyResult: String?,
        val testingSheet: Boolean,
        val sheetResult: String?,
        val showGuide: Boolean
    )

    init {
        refreshHealthConnect()
    }

    fun refreshHealthConnect() {
        viewModelScope.launch {
            val snapshot = healthConnectManager.readSnapshotForDate()
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

    fun syncBiometricsNow() {
        viewModelScope.launch {
            val snapshot = healthConnectManager.readSnapshotForDate()
            healthSnapshotFlow.value = snapshot
            if (snapshot.latestWeightKg != null || snapshot.latestHeightCm != null) {
                val changed = userPreferencesDataStore.syncBiometricsFromHealthConnect(
                    weightKg = snapshot.latestWeightKg,
                    heightCm = snapshot.latestHeightCm
                )
                val unit = uiState.value.userProfile.weightUnit
                val weightPart = snapshot.latestWeightKg?.let { unit.format(it) } ?: "unchanged"
                val heightPart = snapshot.latestHeightCm?.let { String.format(Locale.US, "%.0f cm", it) } ?: "unchanged"
                syncMessageFlow.value = if (changed) {
                    "Synced biometrics: $weightPart, $heightPart"
                } else {
                    "Profile already up-to-date with Health Connect ($weightPart, $heightPart)"
                }
            } else {
                syncMessageFlow.value = if (!snapshot.hasBiometricsPermissions) {
                    "Please grant Health Connect permissions to sync weight & height."
                } else {
                    "No weight or height records found in Health Connect."
                }
            }
        }
    }

    fun updateWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            userPreferencesDataStore.updateWeightUnit(unit)
        }
    }

    fun updateAutoSyncBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.updateAutoSyncBiometrics(enabled)
            if (enabled) {
                refreshHealthConnect()
            }
        }
    }

    fun saveUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            userPreferencesDataStore.updateUserProfile(updated)
            savedFeedbackFlow.value = true
        }
    }

    fun consumeSavedFeedback() {
        savedFeedbackFlow.value = false
    }

    fun consumeSyncMessage() {
        syncMessageFlow.value = null
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            userPreferencesDataStore.updateCustomGeminiApiKey(key.trim())
            geminiKeyTestResultFlow.value = null
        }
    }

    fun testGeminiApiKey(key: String) {
        if (isTestingGeminiKeyFlow.value) return
        viewModelScope.launch {
            isTestingGeminiKeyFlow.value = true
            geminiKeyTestResultFlow.value = null
            val effectiveKey = key.trim().ifEmpty {
                userPreferencesDataStore.userProfileFlow.first().customGeminiApiKey?.trim() ?: ""
            }
            if (effectiveKey.isEmpty()) {
                isTestingGeminiKeyFlow.value = false
                geminiKeyTestResultFlow.value = "Please enter an API key to test."
                return@launch
            }

            val result = geminiMealAnalyzer.testApiKey(effectiveKey)
            isTestingGeminiKeyFlow.value = false
            geminiKeyTestResultFlow.value = result.fold(
                onSuccess = { "✓ Gemini API Key Verified & Working!" },
                onFailure = { err -> "Test Failed: ${err.message ?: "Authentication error"}" }
            )
        }
    }

    fun updateSheetsWebhookUrl(url: String) {
        viewModelScope.launch {
            userPreferencesDataStore.updateCustomSheetsWebhookUrl(url.trim())
            sheetsWebhookTestResultFlow.value = null
        }
    }

    fun testSheetsWebhookUrl(url: String) {
        if (isTestingSheetsWebhookFlow.value) return
        viewModelScope.launch {
            isTestingSheetsWebhookFlow.value = true
            sheetsWebhookTestResultFlow.value = null
            val effectiveUrl = url.trim().ifEmpty {
                userPreferencesDataStore.userProfileFlow.first().customSheetsWebhookUrl?.trim() ?: ""
            }
            if (effectiveUrl.isEmpty()) {
                isTestingSheetsWebhookFlow.value = false
                sheetsWebhookTestResultFlow.value = "Please enter a Webhook URL to test."
                return@launch
            }

            val result = sheetsWebhookClient.testWebhookUrl(effectiveUrl)
            isTestingSheetsWebhookFlow.value = false
            sheetsWebhookTestResultFlow.value = result.fold(
                onSuccess = { "✓ Google Sheets Webhook Connected Successfully!" },
                onFailure = { err -> "Connection Failed: ${err.message ?: "Network error"}" }
            )
        }
    }

    fun clearCloudCredentials() {
        viewModelScope.launch {
            userPreferencesDataStore.clearCustomCredentials()
            geminiKeyTestResultFlow.value = "Custom credentials cleared."
            sheetsWebhookTestResultFlow.value = null
        }
    }

    fun setShowSetupGuideDialog(show: Boolean) {
        showSetupGuideDialogFlow.value = show
    }

    fun dismissTestResults() {
        geminiKeyTestResultFlow.value = null
        sheetsWebhookTestResultFlow.value = null
    }
}

