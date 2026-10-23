package com.macroai.tracker.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.data.local.entity.SavedMealEntity
import com.macroai.tracker.data.repository.NutritionRepository
import com.macroai.tracker.domain.model.MealIngredient
import com.macroai.tracker.domain.model.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import kotlin.math.round

data class EditFoodLogUiState(
    val isLoading: Boolean = true,
    val originalLog: FoodLogEntity? = null,
    val name: String = "",
    val mealType: MealType = MealType.LUNCH,
    val servingWeightGrams: Double = 0.0,
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val saturatedFatG: Double = 0.0,
    val polyunsaturatedFatG: Double = 0.0,
    val monounsaturatedFatG: Double = 0.0,
    val sugarG: Double = 0.0,
    val fibreG: Double = 0.0,
    val novaGroup: Int? = null,
    val containsSeedOils: Boolean = false,
    val detectedSeedOils: String = "",
    val ingredients: List<MealIngredient> = emptyList(),
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val showSaveAsRecipeDialog: Boolean = false,
    val recipeNameInput: String = "",
    val recipeServingsInput: String = "1",
    val showAddIngredientDialog: Boolean = false
) {
    val isMultiComponent: Boolean
        get() = ingredients.isNotEmpty()
}

@HiltViewModel
class EditFoodLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository
) : ViewModel() {

    private val logId: String = checkNotNull(savedStateHandle["logId"])

    private val _uiState = MutableStateFlow(EditFoodLogUiState())
    val uiState: StateFlow<EditFoodLogUiState> = _uiState.asStateFlow()

    init {
        loadLog()
    }

    private fun loadLog() {
        viewModelScope.launch {
            val log = repository.getFoodLogById(logId)
            if (log != null) {
                val parsedIngredients = log.parseIngredients()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalLog = log,
                        name = log.name,
                        mealType = log.mealType,
                        servingWeightGrams = log.servingWeightGrams,
                        calories = log.calories,
                        proteinG = log.proteinG,
                        carbsG = log.carbsG,
                        fatG = log.fatG,
                        saturatedFatG = log.saturatedFatG,
                        polyunsaturatedFatG = log.polyunsaturatedFatG,
                        monounsaturatedFatG = log.monounsaturatedFatG,
                        sugarG = log.sugarG,
                        fibreG = log.fibreG,
                        novaGroup = log.novaGroup,
                        containsSeedOils = log.containsSeedOils,
                        detectedSeedOils = log.detectedSeedOils,
                        ingredients = parsedIngredients,
                        recipeNameInput = log.name,
                        recipeServingsInput = "1"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Meal log not found.") }
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onMealTypeSelected(mealType: MealType) {
        _uiState.update { it.copy(mealType = mealType) }
    }

    fun onUpdateIngredientWeight(index: Int, newWeightGrams: Double) {
        val currentIngredients = _uiState.value.ingredients.toMutableList()
        if (index !in currentIngredients.indices) return
        val current = currentIngredients[index]
        val updated = current.withWeight(newWeightGrams.coerceAtLeast(0.0))
        currentIngredients[index] = updated
        recalculateFromIngredients(currentIngredients)
    }

    fun onRemoveIngredient(index: Int) {
        val currentIngredients = _uiState.value.ingredients.toMutableList()
        if (index !in currentIngredients.indices) return
        currentIngredients.removeAt(index)
        recalculateFromIngredients(currentIngredients)
    }

    fun onAddIngredient(
        name: String,
        weightGrams: Double,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        isUpf: Boolean = false,
        seedOils: String = ""
    ) {
        val newIngredient = MealIngredient(
            name = name.trim().ifBlank { "Custom Item" },
            weightGrams = weightGrams.coerceAtLeast(1.0),
            calories = calories.coerceAtLeast(0.0),
            proteinG = protein.coerceAtLeast(0.0),
            carbsG = carbs.coerceAtLeast(0.0),
            fatG = fat.coerceAtLeast(0.0),
            novaGroup = if (isUpf) 4 else 1,
            containsSeedOils = seedOils.isNotBlank(),
            detectedSeedOils = seedOils.trim()
        )
        val currentIngredients = _uiState.value.ingredients + newIngredient
        recalculateFromIngredients(currentIngredients)
        _uiState.update { it.copy(showAddIngredientDialog = false) }
    }

    private fun recalculateFromIngredients(ingredients: List<MealIngredient>) {
        fun Double.r1(): Double = round(this * 10.0) / 10.0

        val totalWeight = ingredients.sumOf { it.weightGrams }.r1()
        val totalCalories = ingredients.sumOf { it.calories }.r1()
        val totalProtein = ingredients.sumOf { it.proteinG }.r1()
        val totalCarbs = ingredients.sumOf { it.carbsG }.r1()
        val totalFat = ingredients.sumOf { it.fatG }.r1()
        val totalSugar = ingredients.sumOf { it.sugarG }.r1()
        val totalFibre = ingredients.sumOf { it.fibreG }.r1()

        val oldWeight = _uiState.value.servingWeightGrams.takeIf { it > 0.0 } ?: 100.0
        val ratio = totalWeight / oldWeight
        val sat = (_uiState.value.saturatedFatG * ratio).r1()
        val pufa = (_uiState.value.polyunsaturatedFatG * ratio).r1()
        val mufa = (_uiState.value.monounsaturatedFatG * ratio).r1()

        val maxNova = ingredients.mapNotNull { it.novaGroup }.maxOrNull() ?: (_uiState.value.novaGroup ?: 1)
        val anyOils = ingredients.any { it.containsSeedOils } || _uiState.value.containsSeedOils
        val allOils = ingredients.map { it.detectedSeedOils }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

        _uiState.update {
            it.copy(
                ingredients = ingredients,
                servingWeightGrams = totalWeight,
                calories = totalCalories,
                proteinG = totalProtein,
                carbsG = totalCarbs,
                fatG = totalFat,
                saturatedFatG = sat,
                polyunsaturatedFatG = pufa,
                monounsaturatedFatG = mufa,
                sugarG = totalSugar,
                fibreG = totalFibre,
                novaGroup = maxNova,
                containsSeedOils = anyOils,
                detectedSeedOils = allOils.ifBlank { it.detectedSeedOils }
            )
        }
    }

    fun onScaleTotalWeight(newWeightGrams: Double) {
        val oldWeight = _uiState.value.servingWeightGrams.coerceAtLeast(0.1)
        val ratio = (newWeightGrams.coerceAtLeast(0.0)) / oldWeight

        fun Double.scale(): Double = round((this * ratio) * 10.0) / 10.0

        val scaledIngredients = _uiState.value.ingredients.map { it.scaleFraction(ratio) }

        _uiState.update {
            it.copy(
                servingWeightGrams = round(newWeightGrams * 10.0) / 10.0,
                calories = it.calories.scale(),
                proteinG = it.proteinG.scale(),
                carbsG = it.carbsG.scale(),
                fatG = it.fatG.scale(),
                saturatedFatG = it.saturatedFatG.scale(),
                polyunsaturatedFatG = it.polyunsaturatedFatG.scale(),
                monounsaturatedFatG = it.monounsaturatedFatG.scale(),
                sugarG = it.sugarG.scale(),
                fibreG = it.fibreG.scale(),
                ingredients = scaledIngredients
            )
        }
    }

    fun onCaloriesChanged(newKcal: Double) {
        _uiState.update { it.copy(calories = newKcal.coerceAtLeast(0.0)) }
    }

    fun onProteinChanged(newPro: Double) {
        _uiState.update { it.copy(proteinG = newPro.coerceAtLeast(0.0)) }
    }

    fun onCarbsChanged(newCarb: Double) {
        _uiState.update { it.copy(carbsG = newCarb.coerceAtLeast(0.0)) }
    }

    fun onFatChanged(newFat: Double) {
        _uiState.update { it.copy(fatG = newFat.coerceAtLeast(0.0)) }
    }

    fun onNovaGroupChanged(nova: Int?) {
        _uiState.update { it.copy(novaGroup = nova) }
    }

    fun onToggleSeedOils(containsOils: Boolean) {
        _uiState.update {
            it.copy(
                containsSeedOils = containsOils,
                detectedSeedOils = if (containsOils && it.detectedSeedOils.isBlank()) "Vegetable Oil" else if (!containsOils) "" else it.detectedSeedOils
            )
        }
    }

    fun setShowAddIngredientDialog(show: Boolean) {
        _uiState.update { it.copy(showAddIngredientDialog = show) }
    }

    fun openSaveAsRecipeDialog() {
        _uiState.update {
            it.copy(
                showSaveAsRecipeDialog = true,
                recipeNameInput = it.name,
                recipeServingsInput = "1"
            )
        }
    }

    fun dismissSaveAsRecipeDialog() {
        _uiState.update { it.copy(showSaveAsRecipeDialog = false) }
    }

    fun onRecipeNameInputChanged(name: String) {
        _uiState.update { it.copy(recipeNameInput = name) }
    }

    fun onRecipeServingsInputChanged(servings: String) {
        _uiState.update { it.copy(recipeServingsInput = servings) }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        val original = _uiState.value.originalLog ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updatedIngredientsJson = if (_uiState.value.ingredients.isNotEmpty()) {
                Json.encodeToString(_uiState.value.ingredients)
            } else {
                original.ingredientBreakdownJson
            }

            val updatedEntity = original.copy(
                name = _uiState.value.name.trim().ifBlank { original.name },
                mealType = _uiState.value.mealType,
                servingWeightGrams = _uiState.value.servingWeightGrams,
                calories = _uiState.value.calories,
                proteinG = _uiState.value.proteinG,
                carbsG = _uiState.value.carbsG,
                fatG = _uiState.value.fatG,
                saturatedFatG = _uiState.value.saturatedFatG,
                polyunsaturatedFatG = _uiState.value.polyunsaturatedFatG,
                monounsaturatedFatG = _uiState.value.monounsaturatedFatG,
                sugarG = _uiState.value.sugarG,
                fibreG = _uiState.value.fibreG,
                novaGroup = _uiState.value.novaGroup,
                isUpf = (_uiState.value.novaGroup == 4),
                containsSeedOils = _uiState.value.containsSeedOils,
                detectedSeedOils = _uiState.value.detectedSeedOils,
                ingredientBreakdownJson = updatedIngredientsJson,
                isSyncedToSheets = false
            )

            repository.updateFoodLog(updatedEntity)
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    fun deleteMeal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.deleteFoodLog(logId)
            _uiState.update { it.copy(isSaving = false, isDeleted = true) }
            onSuccess()
        }
    }

    fun saveAsRecipe(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val recipeName = state.recipeNameInput.trim().ifBlank { state.name }
            val servings = state.recipeServingsInput.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0

            val ingredientsList = if (state.ingredients.isNotEmpty()) {
                state.ingredients
            } else {
                listOf(
                    MealIngredient(
                        name = state.name,
                        weightGrams = state.servingWeightGrams,
                        calories = state.calories,
                        proteinG = state.proteinG,
                        carbsG = state.carbsG,
                        fatG = state.fatG,
                        saturatedFatG = state.saturatedFatG,
                        polyunsaturatedFatG = state.polyunsaturatedFatG,
                        monounsaturatedFatG = state.monounsaturatedFatG,
                        sugarG = state.sugarG,
                        fibreG = state.fibreG,
                        novaGroup = state.novaGroup,
                        containsSeedOils = state.containsSeedOils,
                        detectedSeedOils = state.detectedSeedOils
                    )
                )
            }

            val savedMeal = SavedMealEntity.fromIngredients(
                name = recipeName,
                description = "Created from logged meal: ${state.name}",
                servings = servings,
                ingredients = ingredientsList,
                id = UUID.randomUUID().toString()
            )

            repository.saveSavedMeal(savedMeal)
            _uiState.update { it.copy(showSaveAsRecipeDialog = false) }
            onSuccess(savedMeal.id)
        }
    }
}

