package com.macroai.tracker.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.data.remote.gemini.dto.GeminiIngredientEstimateDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiMealAnalysisDto
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.data.repository.NutritionRepository
import com.macroai.tracker.domain.model.AiModel
import com.macroai.tracker.domain.model.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

enum class CameraCaptureMode(val label: String) {
    BARCODE_SCAN("Barcode Scanner"),
    AI_PHOTO("AI Meal Photo")
}

sealed interface ReviewDraftState {
    data object Idle : ReviewDraftState

    data class PhotoContextPrompt(
        val bitmap: Bitmap,
        val userContextPrompt: String = ""
    ) : ReviewDraftState

    data class AnalyzingLoading(
        val statusMessage: String
    ) : ReviewDraftState

    data class BarcodeReview(
        val product: NormalizedOffProduct,
        val consumedGramsInput: String = product.defaultServingGrams.toInt().toString(),
        val isServingMode: Boolean = product.servingQuantityGrams != null && product.servingQuantityGrams > 0.0,
        val servingCountInput: String = "1"
    ) : ReviewDraftState

    data class AiMealReview(
        val analysis: GeminiMealAnalysisDto,
        val userContextPrompt: String
    ) : ReviewDraftState
}

data class CameraUiState(
    val captureMode: CameraCaptureMode = CameraCaptureMode.BARCODE_SCAN,
    val selectedMealType: MealType = defaultMealTypeForCurrentTime(),
    val selectedAiModel: AiModel = AiModel.DEFAULT,
    val reviewDraft: ReviewDraftState = ReviewDraftState.Idle,
    val lastScannedBarcode: String? = null,
    val errorMessage: String? = null,
    val savedSuccessEvent: Boolean = false,

    // Ingredient Swap State
    val swapIngredientIndex: Int? = null,
    val swapIngredientOriginal: GeminiIngredientEstimateDto? = null,
    val swapSearchQuery: String = "",
    val swapSearchResults: List<NormalizedOffProduct> = emptyList(),
    val isSearchingSwap: Boolean = false,
    val swapSelectedProduct: NormalizedOffProduct? = null,
    val showSwapBarcodeScanner: Boolean = false,
    val isLookingUpSwapBarcode: Boolean = false
)

private fun defaultMealTypeForCurrentTime(): MealType {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..10 -> MealType.BREAKFAST
        in 11..15 -> MealType.LUNCH
        in 17..21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}

@HiltViewModel
class CameraCaptureViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        userPreferencesDataStore.userProfileFlow
            .onEach { profile ->
                _uiState.update { it.copy(selectedAiModel = profile.selectedAiModel) }
            }
            .launchIn(viewModelScope)
    }

    fun setSelectedAiModel(model: AiModel) {
        _uiState.update { it.copy(selectedAiModel = model) }
        viewModelScope.launch {
            userPreferencesDataStore.updateSelectedAiModel(model)
        }
    }

    fun setCaptureMode(mode: CameraCaptureMode) {
        _uiState.update {
            it.copy(
                captureMode = mode,
                lastScannedBarcode = null,
                errorMessage = null
            )
        }
    }

    fun setMealType(mealType: MealType) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissReviewDraft() {
        _uiState.update {
            it.copy(
                reviewDraft = ReviewDraftState.Idle,
                lastScannedBarcode = null,
                errorMessage = null
            )
        }
    }

    fun onBarcodeDetected(rawBarcode: String) {
        val current = _uiState.value
        if (current.reviewDraft !is ReviewDraftState.Idle) return
        if (current.lastScannedBarcode == rawBarcode) return

        _uiState.update {
            it.copy(
                lastScannedBarcode = rawBarcode,
                reviewDraft = ReviewDraftState.AnalyzingLoading("Looking up barcode $rawBarcode on Open Food Facts…"),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = nutritionRepository.lookupBarcode(rawBarcode)
            result.fold(
                onSuccess = { product ->
                    _uiState.update {
                        it.copy(
                            reviewDraft = ReviewDraftState.BarcodeReview(product = product)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            reviewDraft = ReviewDraftState.Idle,
                            lastScannedBarcode = null,
                            errorMessage = error.message ?: "Failed to fetch product for barcode $rawBarcode"
                        )
                    }
                }
            )
        }
    }

    private var preEncodedPhotoDeferred: Deferred<String>? = null

    fun onPhotoCaptured(bitmap: Bitmap) {
        preEncodedPhotoDeferred?.cancel()
        preEncodedPhotoDeferred = viewModelScope.async {
            nutritionRepository.preEncodeBitmap(bitmap)
        }
        _uiState.update {
            it.copy(
                reviewDraft = ReviewDraftState.PhotoContextPrompt(
                    bitmap = bitmap,
                    userContextPrompt = ""
                ),
                errorMessage = null
            )
        }
    }

    fun updatePhotoContextPrompt(prompt: String) {
        val draft = _uiState.value.reviewDraft
        if (draft is ReviewDraftState.PhotoContextPrompt) {
            _uiState.update {
                it.copy(reviewDraft = draft.copy(userContextPrompt = prompt))
            }
        }
    }

    fun submitPhotoForGeminiAnalysis() {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.PhotoContextPrompt ?: return
        val activeModel = _uiState.value.selectedAiModel
        _uiState.update {
            it.copy(
                reviewDraft = ReviewDraftState.AnalyzingLoading(
                    "Scanning meal macros, NOVA group & seed oils via ${activeModel.displayName}…"
                ),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val encodedJpeg = preEncodedPhotoDeferred?.await()
                ?: nutritionRepository.preEncodeBitmap(draft.bitmap)

            val result = nutritionRepository.analyzePreEncodedMealPhoto(
                base64Jpeg = encodedJpeg,
                userContextPrompt = draft.userContextPrompt,
                modelId = activeModel.modelId
            )
            result.fold(
                onSuccess = { analysis ->
                    _uiState.update {
                        it.copy(
                            reviewDraft = ReviewDraftState.AiMealReview(
                                analysis = analysis,
                                userContextPrompt = draft.userContextPrompt
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            reviewDraft = draft,
                            errorMessage = err.message ?: "Gemini analysis failed"
                        )
                    }
                }
            )
        }
    }

    fun updateBarcodeGramsInput(gramsInput: String) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.BarcodeReview ?: return
        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(
                    consumedGramsInput = gramsInput,
                    isServingMode = false
                )
            )
        }
    }

    fun toggleBarcodeServingMode(useServingMode: Boolean) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.BarcodeReview ?: return
        val servingGrams = draft.product.servingQuantityGrams ?: draft.product.defaultServingGrams
        val count = draft.servingCountInput.toDoubleOrNull() ?: 1.0
        val newGrams = if (useServingMode) {
            val total = kotlin.math.round(count * servingGrams * 10.0) / 10.0
            if (total % 1.0 == 0.0) total.toInt().toString() else total.toString()
        } else {
            draft.consumedGramsInput
        }
        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(
                    isServingMode = useServingMode,
                    consumedGramsInput = newGrams
                )
            )
        }
    }

    fun updateBarcodeServingCount(countInput: String) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.BarcodeReview ?: return
        val servingGrams = draft.product.servingQuantityGrams ?: draft.product.defaultServingGrams
        val count = countInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
        val totalGrams = kotlin.math.round(count * servingGrams * 10.0) / 10.0
        val gramsStr = if (totalGrams % 1.0 == 0.0) totalGrams.toInt().toString() else totalGrams.toString()
        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(
                    servingCountInput = countInput,
                    consumedGramsInput = gramsStr,
                    isServingMode = true
                )
            )
        }
    }

    fun selectBarcodeServingPreset(grams: Double, servingCount: Double? = null, isServing: Boolean = false) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.BarcodeReview ?: return
        val gramsStr = if (grams % 1.0 == 0.0) grams.toInt().toString() else (kotlin.math.round(grams * 10.0) / 10.0).toString()
        val countStr = servingCount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: draft.servingCountInput
        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(
                    consumedGramsInput = gramsStr,
                    servingCountInput = countStr,
                    isServingMode = isServing
                )
            )
        }
    }

    // ==========================================
    // AI Ingredient Swap Actions
    // ==========================================

    fun startSwapIngredient(index: Int) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.AiMealReview ?: return
        val ingredient = draft.analysis.ingredientBreakdown.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                swapIngredientIndex = index,
                swapIngredientOriginal = ingredient,
                swapSearchQuery = "",
                swapSearchResults = emptyList(),
                isSearchingSwap = true,
                swapSelectedProduct = null,
                showSwapBarcodeScanner = false,
                isLookingUpSwapBarcode = false
            )
        }
        viewModelScope.launch {
            val q = ingredient.name.split(" ", "(", ",").firstOrNull()?.trim() ?: ""
            val results = if (q.isNotBlank()) {
                nutritionRepository.searchFoods(q).getOrDefault(emptyList())
            } else {
                com.macroai.tracker.data.local.StapleIngredientsDatabase.staples.take(8)
            }
            _uiState.update {
                it.copy(
                    swapSearchResults = results,
                    isSearchingSwap = false
                )
            }
        }
    }

    fun updateSwapSearchQuery(query: String) {
        _uiState.update { it.copy(swapSearchQuery = query) }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingSwap = true) }
            val q = query.trim()
            val results = if (q.isNotEmpty()) {
                nutritionRepository.searchFoods(q).getOrDefault(emptyList())
            } else {
                com.macroai.tracker.data.local.StapleIngredientsDatabase.staples.take(8)
            }
            _uiState.update {
                it.copy(
                    swapSearchResults = results,
                    isSearchingSwap = false
                )
            }
        }
    }

    fun selectProductForSwap(product: NormalizedOffProduct) {
        _uiState.update { it.copy(swapSelectedProduct = product) }
    }

    fun dismissSwapProductSelection() {
        _uiState.update { it.copy(swapSelectedProduct = null) }
    }

    fun confirmSwapIngredient(product: NormalizedOffProduct, grams: Double) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.AiMealReview ?: return
        val index = _uiState.value.swapIngredientIndex ?: return
        val currentList = draft.analysis.ingredientBreakdown.toMutableList()
        if (index !in currentList.indices) return

        val factor = grams / 100.0
        fun Double.scale(): Double = kotlin.math.round((this * factor) * 10.0) / 10.0

        val newIngredient = GeminiIngredientEstimateDto(
            name = product.name,
            estimatedWeightGrams = kotlin.math.round(grams * 10.0) / 10.0,
            calories = product.caloriesPer100g.scale(),
            proteinG = product.proteinPer100g.scale(),
            carbsG = product.carbsPer100g.scale(),
            fatG = product.fatPer100g.scale(),
            novaGroup = product.novaGroup ?: 1,
            containsSeedOils = product.containsSeedOils
        )

        currentList[index] = newIngredient
        val updatedAnalysis = draft.analysis.withUpdatedIngredients(currentList)

        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(analysis = updatedAnalysis),
                swapIngredientIndex = null,
                swapIngredientOriginal = null,
                swapSearchQuery = "",
                swapSearchResults = emptyList(),
                swapSelectedProduct = null,
                showSwapBarcodeScanner = false,
                isLookingUpSwapBarcode = false
            )
        }
    }

    fun removeAiIngredient(index: Int) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.AiMealReview ?: return
        val currentList = draft.analysis.ingredientBreakdown.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            val updatedAnalysis = draft.analysis.withUpdatedIngredients(currentList)
            _uiState.update {
                it.copy(reviewDraft = draft.copy(analysis = updatedAnalysis))
            }
        }
    }

    fun dismissSwap() {
        _uiState.update {
            it.copy(
                swapIngredientIndex = null,
                swapIngredientOriginal = null,
                swapSearchQuery = "",
                swapSearchResults = emptyList(),
                swapSelectedProduct = null,
                showSwapBarcodeScanner = false,
                isLookingUpSwapBarcode = false
            )
        }
    }

    fun setShowSwapBarcodeScanner(show: Boolean) {
        _uiState.update { it.copy(showSwapBarcodeScanner = show) }
    }

    fun onSwapBarcodeDetected(barcode: String) {
        _uiState.update {
            it.copy(
                showSwapBarcodeScanner = false,
                isLookingUpSwapBarcode = true
            )
        }
        viewModelScope.launch {
            val result = nutritionRepository.lookupBarcode(barcode)
            result.fold(
                onSuccess = { product ->
                    _uiState.update {
                        it.copy(
                            isLookingUpSwapBarcode = false,
                            swapSelectedProduct = product
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLookingUpSwapBarcode = false,
                            errorMessage = "Barcode lookup failed: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun updateAiMealName(newName: String) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.AiMealReview ?: return
        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(
                    analysis = draft.analysis.copy(name = newName)
                )
            )
        }
    }

    fun updateAiIngredientWeight(index: Int, newWeightGrams: Double) {
        val draft = _uiState.value.reviewDraft as? ReviewDraftState.AiMealReview ?: return
        val currentList = draft.analysis.ingredientBreakdown
        if (index !in currentList.indices) return

        val updatedIngredients: List<GeminiIngredientEstimateDto> = currentList.mapIndexed { idx, item ->
            if (idx == index) item.scaleToWeight(newWeightGrams) else item
        }

        _uiState.update {
            it.copy(
                reviewDraft = draft.copy(
                    analysis = draft.analysis.withUpdatedIngredients(updatedIngredients)
                )
            )
        }
    }

    fun confirmAndSaveCurrentDraft() {
        val state = _uiState.value
        val mealType = state.selectedMealType

        val entity = when (val draft = state.reviewDraft) {
            is ReviewDraftState.BarcodeReview -> {
                val grams = draft.consumedGramsInput.toDoubleOrNull()?.coerceAtLeast(1.0)
                    ?: draft.product.defaultServingGrams
                draft.product.scaleToEntity(
                    servingWeightGrams = grams,
                    mealType = mealType
                )
            }
            is ReviewDraftState.AiMealReview -> {
                draft.analysis.toFoodLogEntity(
                    mealType = mealType,
                    userContextPrompt = draft.userContextPrompt
                )
            }
            else -> return
        }

        viewModelScope.launch {
            nutritionRepository.saveFoodLog(entity)
            _uiState.update {
                it.copy(
                    reviewDraft = ReviewDraftState.Idle,
                    savedSuccessEvent = true
                )
            }
        }
    }

    fun consumeSavedSuccessEvent() {
        _uiState.update { it.copy(savedSuccessEvent = false) }
    }
}

