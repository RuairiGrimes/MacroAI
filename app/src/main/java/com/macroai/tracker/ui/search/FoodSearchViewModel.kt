package com.macroai.tracker.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.data.repository.NutritionRepository
import com.macroai.tracker.domain.model.AiModel
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.ServingOption
import com.macroai.tracker.domain.model.ServingUnitType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.round

data class FoodSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isAiSearching: Boolean = false,
    val searchResults: List<NormalizedOffProduct> = emptyList(),
    val errorMessage: String? = null,
    val selectedProduct: NormalizedOffProduct? = null,
    val availableServingOptions: List<ServingOption> = emptyList(),
    val selectedServingOption: ServingOption? = null,
    val quantityInput: String = "100",
    val selectedMealType: MealType = defaultMealTypeForCurrentTime(),
    val selectedDate: LocalDate = LocalDate.now(),
    val loggedSuccessMessage: String? = null
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

@OptIn(FlowPreview::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialMealTypeName: String? = savedStateHandle["initialMealType"]

    private val _uiState = MutableStateFlow(
        FoodSearchUiState(
            selectedMealType = initialMealTypeName?.let { name ->
                runCatching { MealType.valueOf(name) }.getOrNull()
            } ?: defaultMealTypeForCurrentTime()
        )
    )
    val uiState: StateFlow<FoodSearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    val popularStaples = listOf(
        "Avonmore Chocolate Gold Protein Milk", "Chicken thighs", "Chicken breast", "5% minced beef",
        "Eggs", "Kerrygold butter", "Porridge oats", "Salmon fillet", "Sirloin steak",
        "Greek yogurt", "Bacon medallions", "Irish sausages", "Brennans bread", "Rooster potatoes"
    )

    init {
        _queryFlow
            .debounce(450)
            .distinctUntilChanged()
            .onEach { q ->
                if (q.trim().length >= 2) {
                    performSearch(q.trim())
                } else if (q.trim().isEmpty()) {
                    _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, errorMessage = null) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onSearchActionTriggered() {
        val q = _uiState.value.query.trim()
        if (q.isNotEmpty()) {
            performSearch(q)
        }
    }

    fun selectStaple(staple: String) {
        _uiState.update { it.copy(query = staple) }
        _queryFlow.value = staple
        performSearch(staple)
    }

    fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            val result = nutritionRepository.searchFoods(query)
            result.fold(
                onSuccess = { products ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = products,
                            errorMessage = if (products.isEmpty()) "No foods found for \"$query\". Try a different search." else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = "Search failed: ${err.localizedMessage ?: "Network error"}. Please check your connection."
                        )
                    }
                }
            )
        }
    }

    fun searchWithGeminiAi() {
        val q = _uiState.value.query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, isAiSearching = true, errorMessage = null) }
            val modelId = runCatching { userPreferencesDataStore.userProfileFlow.first().selectedAiModel.modelId }
                .getOrDefault(AiModel.DEFAULT.modelId)
            val result = nutritionRepository.parseRecipePrompt(q, modelId = modelId)
            result.fold(
                onSuccess = { analysis ->
                    val totalWeight = if (analysis.servingWeightGrams > 0.0) analysis.servingWeightGrams else 100.0
                    val factor = 100.0 / totalWeight
                    val ingredients = analysis.ingredientBreakdown.joinToString(", ") {
                        "${it.name} (~${round(it.estimatedWeightGrams).toInt()}g)"
                    }

                    val product = NormalizedOffProduct(
                        barcode = "gemini_${System.currentTimeMillis()}",
                        name = analysis.name,
                        ingredientsText = ingredients,
                        defaultServingGrams = totalWeight,
                        novaGroup = analysis.novaGroup,
                        isUpf = analysis.isUpf,
                        containsSeedOils = analysis.containsSeedOils,
                        detectedSeedOilsCsv = analysis.detectedSeedOils,
                        caloriesPer100g = round((analysis.calories * factor) * 100.0) / 100.0,
                        proteinPer100g = round((analysis.proteinG * factor) * 100.0) / 100.0,
                        carbsPer100g = round((analysis.carbsG * factor) * 100.0) / 100.0,
                        fatPer100g = round((analysis.fatG * factor) * 100.0) / 100.0,
                        saturatedFatPer100g = round((analysis.saturatedFatG * factor) * 100.0) / 100.0,
                        polyunsaturatedFatPer100g = round((analysis.polyunsaturatedFatG * factor) * 100.0) / 100.0,
                        monounsaturatedFatPer100g = round((analysis.monounsaturatedFatG * factor) * 100.0) / 100.0,
                        sugarPer100g = round((analysis.sugarG * factor) * 100.0) / 100.0,
                        fibrePer100g = round((analysis.fibreG * factor) * 100.0) / 100.0,
                        sodiumMgPer100g = round((analysis.sodiumMg * factor) * 100.0) / 100.0,
                        potassiumMgPer100g = round((analysis.potassiumMg * factor) * 100.0) / 100.0,
                        ironMgPer100g = round((analysis.ironMg * factor) * 100.0) / 100.0,
                        calciumMgPer100g = round((analysis.calciumMg * factor) * 100.0) / 100.0,
                        vitaminCMgPer100g = round((analysis.vitaminCMg * factor) * 100.0) / 100.0,
                        vitaminDUgPer100g = round((analysis.vitaminDUg * factor) * 100.0) / 100.0,
                        magnesiumMgPer100g = round((analysis.magnesiumMg * factor) * 100.0) / 100.0,
                        brand = "Gemini Flash 3.1 Lite AI Breakdown",
                        servingSizeName = "1 serving (${totalWeight.toInt()}g)",
                        servingQuantityGrams = totalWeight
                    )
                    selectProduct(product)
                    _uiState.update { it.copy(isSearching = false, isAiSearching = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            isAiSearching = false,
                            errorMessage = "Gemini Flash 3.1 Lite AI search failed: ${err.localizedMessage ?: "Unknown error"}. Check connection."
                        )
                    }
                }
            )
        }
    }

    fun selectProduct(product: NormalizedOffProduct) {
        val options = ServingOption.optionsForProduct(product)
        // Default to package serving if present, otherwise grams
        val defaultOption = options.firstOrNull { it.type == ServingUnitType.SERVING }
            ?: options.firstOrNull { it.type == ServingUnitType.GRAMS }
            ?: options.first()

        val defaultQuantity = when (defaultOption.type) {
            ServingUnitType.SERVING -> "1"
            ServingUnitType.GRAMS -> product.defaultServingGrams.toInt().toString()
            ServingUnitType.OUNCES -> "1"
            ServingUnitType.HUNDRED_GRAMS -> "1"
        }

        _uiState.update {
            it.copy(
                selectedProduct = product,
                availableServingOptions = options,
                selectedServingOption = defaultOption,
                quantityInput = defaultQuantity
            )
        }
    }

    fun dismissProductDetail() {
        _uiState.update {
            it.copy(
                selectedProduct = null,
                availableServingOptions = emptyList(),
                selectedServingOption = null
            )
        }
    }

    fun selectServingOption(option: ServingOption) {
        val currentGrams = computeCurrentTotalGrams()
        // Adjust quantity input to match the newly selected unit while maintaining similar weight
        val newQuantity = when (option.type) {
            ServingUnitType.GRAMS -> currentGrams.toInt().toString()
            ServingUnitType.OUNCES -> {
                val oz = currentGrams / option.gramsPerUnit
                (round(oz * 10.0) / 10.0).toString()
            }
            ServingUnitType.SERVING -> {
                val s = currentGrams / option.gramsPerUnit
                (round(s * 10.0) / 10.0).toString()
            }
            ServingUnitType.HUNDRED_GRAMS -> {
                val h = currentGrams / 100.0
                (round(h * 10.0) / 10.0).toString()
            }
        }

        _uiState.update {
            it.copy(
                selectedServingOption = option,
                quantityInput = newQuantity
            )
        }
    }

    fun onQuantityInputChanged(input: String) {
        _uiState.update { it.copy(quantityInput = input) }
    }

    fun stepQuantity(delta: Double) {
        val currentVal = _uiState.value.quantityInput.toDoubleOrNull() ?: 1.0
        val newVal = (currentVal + delta).coerceAtLeast(0.1)
        val formatted = if (newVal % 1.0 == 0.0) {
            newVal.toInt().toString()
        } else {
            (round(newVal * 10.0) / 10.0).toString()
        }
        _uiState.update { it.copy(quantityInput = formatted) }
    }

    fun setPresetQuantity(value: Double) {
        val formatted = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            (round(value * 10.0) / 10.0).toString()
        }
        _uiState.update { it.copy(quantityInput = formatted) }
    }

    fun setMealType(mealType: MealType) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun computeCurrentTotalGrams(): Double {
        val state = _uiState.value
        val option = state.selectedServingOption ?: return 100.0
        val qty = state.quantityInput.toDoubleOrNull() ?: 1.0
        return option.calculateTotalGrams(qty)
    }

    fun computeScaledPreviewEntity(): FoodLogEntity? {
        val state = _uiState.value
        val product = state.selectedProduct ?: return null
        val totalGrams = computeCurrentTotalGrams()
        return product.scaleToEntity(
            servingWeightGrams = totalGrams,
            mealType = state.selectedMealType,
            source = LogSource.SEARCH
        )
    }

    fun logSelectedFood() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        val totalGrams = computeCurrentTotalGrams()
        if (totalGrams <= 0.0) return

        viewModelScope.launch {
            val entity = product.scaleToEntity(
                servingWeightGrams = totalGrams,
                mealType = state.selectedMealType,
                source = LogSource.SEARCH
            )
            nutritionRepository.saveFoodLog(entity)
            _uiState.update {
                it.copy(
                    loggedSuccessMessage = "Logged ${totalGrams.toInt()}g of ${product.name} to ${state.selectedMealType.displayName}!",
                    selectedProduct = null
                )
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(loggedSuccessMessage = null) }
    }
}

