package com.macroai.tracker.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macroai.tracker.data.local.entity.SavedMealEntity
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.data.repository.NutritionRepository
import com.macroai.tracker.domain.model.MealIngredient
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.toMealIngredient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.round

data class SavedMealsUiState(
    val isLoading: Boolean = false,
    val recipes: List<SavedMealEntity> = emptyList(),
    val selectedMealForPortionLogging: SavedMealEntity? = null,
    val portionServingsToLog: Double = 1.0,
    val portionMealType: MealType = MealType.LUNCH,
    val showPortionLogSheet: Boolean = false,
    val isLogging: Boolean = false,
    val showCreateRecipeSheet: Boolean = false,
    val createMode: RecipeCreateMode = RecipeCreateMode.AI_PROMPT,
    val isGeneratingWithAi: Boolean = false,
    val aiRecipePrompt: String = "",
    val createRecipeName: String = "",
    val createRecipeDescription: String = "",
    val createRecipeServings: String = "4",
    val createRecipeIngredients: List<MealIngredient> = emptyList(),
    val isSearchingFood: Boolean = false,
    val searchFoodQuery: String = "",
    val searchFoodResults: List<NormalizedOffProduct> = emptyList(),
    val showBarcodeScanner: Boolean = false,
    val selectedProductForPortionSelection: NormalizedOffProduct? = null,
    val isLookingUpBarcode: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

enum class RecipeCreateMode {
    AI_PROMPT,
    MANUAL_SEARCH
}

@HiltViewModel
class SavedMealsViewModel @Inject constructor(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedMealsUiState())
    private var searchJob: Job? = null

    val uiState: StateFlow<SavedMealsUiState> = combine(
        _uiState,
        repository.observeAllSavedMeals()
    ) { state, recipes ->
        state.copy(recipes = recipes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedMealsUiState()
    )

    fun openPortionLog(recipe: SavedMealEntity) {
        _uiState.update {
            it.copy(
                selectedMealForPortionLogging = recipe,
                portionServingsToLog = 1.0,
                portionMealType = MealType.LUNCH,
                showPortionLogSheet = true
            )
        }
    }

    fun dismissPortionLog() {
        _uiState.update { it.copy(showPortionLogSheet = false, selectedMealForPortionLogging = null) }
    }

    fun onPortionServingsChanged(servings: Double) {
        _uiState.update { it.copy(portionServingsToLog = servings.coerceAtLeast(0.1)) }
    }

    fun onPortionMealTypeChanged(type: MealType) {
        _uiState.update { it.copy(portionMealType = type) }
    }

    fun confirmPortionLog(onSuccess: (String) -> Unit) {
        val recipe = _uiState.value.selectedMealForPortionLogging ?: return
        val servings = _uiState.value.portionServingsToLog
        val mealType = _uiState.value.portionMealType

        viewModelScope.launch {
            _uiState.update { it.copy(isLogging = true) }
            val logged = repository.logSavedMeal(
                meal = recipe,
                servingsToLog = servings,
                mealType = mealType
            )
            _uiState.update {
                it.copy(
                    isLogging = false,
                    showPortionLogSheet = false,
                    selectedMealForPortionLogging = null,
                    infoMessage = "Logged $servings portion(s) of ${recipe.name}"
                )
            }
            onSuccess(logged.id)
        }
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            repository.deleteSavedMeal(id)
        }
    }

    // --- Create Recipe Flow ---

    fun openCreateRecipeSheet() {
        _uiState.update {
            it.copy(
                showCreateRecipeSheet = true,
                createMode = RecipeCreateMode.AI_PROMPT,
                aiRecipePrompt = "",
                createRecipeName = "",
                createRecipeDescription = "",
                createRecipeServings = "4",
                createRecipeIngredients = emptyList(),
                searchFoodQuery = "",
                searchFoodResults = emptyList(),
                errorMessage = null
            )
        }
    }

    fun dismissCreateRecipeSheet() {
        _uiState.update { it.copy(showCreateRecipeSheet = false) }
    }

    fun setCreateMode(mode: RecipeCreateMode) {
        _uiState.update { it.copy(createMode = mode) }
    }

    fun onAiPromptChanged(prompt: String) {
        _uiState.update { it.copy(aiRecipePrompt = prompt) }
    }

    fun generateRecipeFromAiPrompt() {
        val prompt = _uiState.value.aiRecipePrompt.trim()
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingWithAi = true, errorMessage = null) }
            val result = repository.parseRecipePrompt(prompt)
            result.onSuccess { analysis ->
                val ingredients = analysis.ingredientBreakdown.map { it.toMealIngredient() }
                _uiState.update {
                    it.copy(
                        isGeneratingWithAi = false,
                        createRecipeName = analysis.name,
                        createRecipeDescription = analysis.aiConfidenceReasoning,
                        createRecipeIngredients = ingredients,
                        createMode = RecipeCreateMode.MANUAL_SEARCH // Switch to review ingredients!
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGeneratingWithAi = false,
                        errorMessage = err.message ?: "Failed to generate recipe from prompt."
                    )
                }
            }
        }
    }

    fun onCreateRecipeNameChanged(name: String) {
        _uiState.update { it.copy(createRecipeName = name) }
    }

    fun onCreateRecipeServingsChanged(servings: String) {
        _uiState.update { it.copy(createRecipeServings = servings) }
    }

    fun onUpdateCreateIngredientWeight(index: Int, newWeightGrams: Double) {
        val list = _uiState.value.createRecipeIngredients.toMutableList()
        if (index !in list.indices) return
        list[index] = list[index].withWeight(newWeightGrams.coerceAtLeast(0.0))
        _uiState.update { it.copy(createRecipeIngredients = list) }
    }

    fun onRemoveCreateIngredient(index: Int) {
        val list = _uiState.value.createRecipeIngredients.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _uiState.update { it.copy(createRecipeIngredients = list) }
    }

    fun onAddManualIngredient(
        name: String,
        weightGrams: Double,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        isUpf: Boolean,
        seedOils: String
    ) {
        val ingredient = MealIngredient(
            name = name.trim().ifBlank { "Ingredient" },
            weightGrams = weightGrams.coerceAtLeast(1.0),
            calories = calories.coerceAtLeast(0.0),
            proteinG = protein.coerceAtLeast(0.0),
            carbsG = carbs.coerceAtLeast(0.0),
            fatG = fat.coerceAtLeast(0.0),
            novaGroup = if (isUpf) 4 else 1,
            containsSeedOils = seedOils.isNotBlank(),
            detectedSeedOils = seedOils.trim()
        )
        _uiState.update { it.copy(createRecipeIngredients = it.createRecipeIngredients + ingredient) }
    }

    fun onSearchFoodQueryChanged(query: String) {
        _uiState.update { it.copy(searchFoodQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchFoodResults = emptyList(), isSearchingFood = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(isSearchingFood = true) }
            val res = repository.searchFoods(query)
            _uiState.update {
                it.copy(
                    isSearchingFood = false,
                    searchFoodResults = res.getOrDefault(emptyList())
                )
            }
        }
    }

    fun setShowBarcodeScanner(show: Boolean) {
        _uiState.update { it.copy(showBarcodeScanner = show) }
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showBarcodeScanner = false, isLookingUpBarcode = true, errorMessage = null) }
            val result = repository.lookupBarcode(barcode)
            result.onSuccess { product ->
                _uiState.update {
                    it.copy(
                        isLookingUpBarcode = false,
                        selectedProductForPortionSelection = product
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLookingUpBarcode = false,
                        errorMessage = "Product not found for barcode $barcode. You can search or add manually."
                    )
                }
            }
        }
    }

    fun selectProductForPortionSelection(product: NormalizedOffProduct) {
        _uiState.update { it.copy(selectedProductForPortionSelection = product) }
    }

    fun dismissPortionSelection() {
        _uiState.update { it.copy(selectedProductForPortionSelection = null) }
    }

    fun addPortionIngredient(ingredient: MealIngredient) {
        _uiState.update {
            it.copy(
                createRecipeIngredients = it.createRecipeIngredients + ingredient,
                selectedProductForPortionSelection = null,
                searchFoodQuery = "",
                searchFoodResults = emptyList()
            )
        }
    }

    fun onAddSearchedProductToRecipe(product: NormalizedOffProduct, grams: Double) {
        val ingredient = product.toMealIngredient(grams.coerceAtLeast(1.0))
        _uiState.update {
            it.copy(
                createRecipeIngredients = it.createRecipeIngredients + ingredient,
                searchFoodQuery = "",
                searchFoodResults = emptyList()
            )
        }
    }

    fun saveRecipe(onSuccess: () -> Unit) {
        val state = _uiState.value
        val name = state.createRecipeName.trim().ifBlank { "Custom Meal" }
        val servings = state.createRecipeServings.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0

        if (state.createRecipeIngredients.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please add at least 1 ingredient.") }
            return
        }

        viewModelScope.launch {
            val recipe = SavedMealEntity.fromIngredients(
                name = name,
                description = state.createRecipeDescription,
                servings = servings,
                ingredients = state.createRecipeIngredients,
                id = UUID.randomUUID().toString()
            )
            repository.saveSavedMeal(recipe)
            _uiState.update { it.copy(showCreateRecipeSheet = false, infoMessage = "Recipe saved!") }
            onSuccess()
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
