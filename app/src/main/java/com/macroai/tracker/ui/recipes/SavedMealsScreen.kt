package com.macroai.tracker.ui.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import com.macroai.tracker.ui.camera.BarcodeScannerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.macroai.tracker.data.local.entity.SavedMealEntity
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.ui.theme.AmberWarning
import com.macroai.tracker.ui.theme.CrimsonDanger
import com.macroai.tracker.ui.theme.CyanAccent
import com.macroai.tracker.ui.theme.EmeraldPrimary
import com.macroai.tracker.ui.theme.IndigoAccent
import com.macroai.tracker.ui.theme.ObsidianBg
import com.macroai.tracker.ui.theme.ObsidianCard
import com.macroai.tracker.ui.theme.ObsidianElevated
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMealsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SavedMealsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Meals & Recipes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${uiState.recipes.size} repeatable recipe(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = viewModel::openCreateRecipeSheet,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF032215)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Recipe", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        }
    ) { innerPadding ->
        if (uiState.recipes.isEmpty()) {
            EmptyRecipesView(
                onCreateRecipe = viewModel::openCreateRecipeSheet,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.recipes, key = { it.id }) { recipe ->
                    SavedRecipeCard(
                        recipe = recipe,
                        onLogPortion = { viewModel.openPortionLog(recipe) },
                        onDelete = { viewModel.deleteRecipe(recipe.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Portion Logging BottomSheet
    if (uiState.showPortionLogSheet && uiState.selectedMealForPortionLogging != null) {
        val recipe = uiState.selectedMealForPortionLogging!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = viewModel::dismissPortionLog,
            sheetState = sheetState,
            containerColor = ObsidianBg
        ) {
            PortionLoggingSheetContent(
                recipe = recipe,
                portionServings = uiState.portionServingsToLog,
                selectedMealType = uiState.portionMealType,
                isLogging = uiState.isLogging,
                onPortionServingsChanged = viewModel::onPortionServingsChanged,
                onMealTypeChanged = viewModel::onPortionMealTypeChanged,
                onConfirmLog = {
                    viewModel.confirmPortionLog {
                        onNavigateToDashboard()
                    }
                },
                onCancel = viewModel::dismissPortionLog
            )
        }
    }

    // Create Recipe Sheet
    if (uiState.showCreateRecipeSheet) {
        val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = viewModel::dismissCreateRecipeSheet,
            sheetState = createSheetState,
            containerColor = ObsidianBg
        ) {
            CreateRecipeSheetContent(
                uiState = uiState,
                onSetCreateMode = viewModel::setCreateMode,
                onAiPromptChanged = viewModel::onAiPromptChanged,
                onGenerateAiRecipe = viewModel::generateRecipeFromAiPrompt,
                onRecipeNameChanged = viewModel::onCreateRecipeNameChanged,
                onRecipeServingsChanged = viewModel::onCreateRecipeServingsChanged,
                onUpdateIngredientWeight = viewModel::onUpdateCreateIngredientWeight,
                onRemoveIngredient = viewModel::onRemoveCreateIngredient,
                onSearchFood = viewModel::onSearchFoodQueryChanged,
                onOpenBarcodeScanner = { viewModel.setShowBarcodeScanner(true) },
                onSelectProductForPortion = viewModel::selectProductForPortionSelection,
                onAddManualIngredient = viewModel::onAddManualIngredient,
                onSaveRecipe = {
                    viewModel.saveRecipe {
                        scope.launch {
                            snackbarHostState.showSnackbar("Recipe created and saved!")
                        }
                    }
                },
                onCancel = viewModel::dismissCreateRecipeSheet
            )
        }
    }

    // Barcode Scanner Dialog for packet ingredients
    if (uiState.showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = viewModel::onBarcodeScanned,
            onDismiss = { viewModel.setShowBarcodeScanner(false) }
        )
    }

    // Serving Size & Portion Selector Dialog
    uiState.selectedProductForPortionSelection?.let { product ->
        IngredientPortionSelectorDialog(
            product = product,
            onConfirm = viewModel::addPortionIngredient,
            onDismiss = viewModel::dismissPortionSelection
        )
    }
}

@Composable
private fun EmptyRecipesView(
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "No Repeatable Meals Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Create recipes using Gemini AI or ingredient search. Ideal for family dinners (e.g. 1/5th portion) or meal prep you eat multiple days in a row.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onCreateRecipe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF032215)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Meal / Recipe", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun SavedRecipeCard(
    recipe: SavedMealEntity,
    onLogPortion: () -> Unit,
    onDelete: () -> Unit
) {
    val ingredients = remember(recipe.ingredientsJson) { recipe.parseIngredients() }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recipe.totalServings.let { if (it % 1.0 == 0.0) it.toInt() else it }} servings • ${recipe.totalWeightGrams.roundToInt()}g total (${recipe.perServingWeightGrams.roundToInt()}g / serving)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Recipe",
                        tint = CrimsonDanger.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Per-Serving Macro Summary Row
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Per Serving:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recipe.perServingCalories.roundToInt()} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "P ${recipe.perServingProtein.roundToInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "C ${recipe.perServingCarbs.roundToInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "F ${recipe.perServingFat.roundToInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quality Badges & Ingredient chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recipe.novaGroup?.let { nova ->
                    Surface(
                        color = if (nova == 4) CrimsonDanger.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (nova == 4) "NOVA 4 • UPF" else "NOVA $nova",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (nova == 4) CrimsonDanger else EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (recipe.containsSeedOils) {
                    val assessment = recipe.seedOilAssessment()
                    val oilColor = when (assessment.riskLevel) {
                        com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> EmeraldPrimary
                        com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> AmberWarning
                        com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> CrimsonDanger
                    }
                    Surface(
                        color = oilColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = assessment.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = oilColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (ingredients.isNotEmpty()) {
                    Text(
                        text = "${ingredients.size} ingredients",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Log Button
            Button(
                onClick = onLogPortion,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color(0xFF032215)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Portion", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortionLoggingSheetContent(
    recipe: SavedMealEntity,
    portionServings: Double,
    selectedMealType: MealType,
    isLogging: Boolean,
    onPortionServingsChanged: (Double) -> Unit,
    onMealTypeChanged: (MealType) -> Unit,
    onConfirmLog: () -> Unit,
    onCancel: () -> Unit
) {
    val fraction = portionServings / recipe.totalServings.coerceAtLeast(0.1)
    val portionCalories = (recipe.totalCalories * fraction).roundToInt()
    val portionProtein = (recipe.totalProteinG * fraction).roundToInt()
    val portionCarbs = (recipe.totalCarbsG * fraction).roundToInt()
    val portionFat = (recipe.totalFatG * fraction).roundToInt()
    val portionWeight = (recipe.totalWeightGrams * fraction).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Log Portion",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Portion Selector
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Portion (Total Recipe: ${recipe.totalServings.let { if (it % 1.0 == 0.0) it.toInt() else it }} servings)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickOptions = if (recipe.totalServings > 1.0) {
                        listOf(
                            1.0 to "1 serving (1/${recipe.totalServings.toInt()})",
                            0.5 to "0.5 serving",
                            2.0 to "2 servings",
                            recipe.totalServings to "Full recipe"
                        )
                    } else {
                        listOf(
                            1.0 to "1 serving",
                            0.5 to "0.5 serving",
                            1.5 to "1.5 servings",
                            2.0 to "2 servings"
                        )
                    }

                    quickOptions.forEach { (servings, label) ->
                        FilterChip(
                            selected = portionServings == servings,
                            onClick = { onPortionServingsChanged(servings) },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }

                // Custom Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Custom Servings:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { onPortionServingsChanged((portionServings - 0.25).coerceAtLeast(0.25)) }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus 0.25")
                        }
                        Text(
                            text = if (portionServings % 1.0 == 0.0) "${portionServings.toInt()} serv" else "${"%.2f".format(portionServings)} serv",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                        IconButton(
                            onClick = { onPortionServingsChanged(portionServings + 0.25) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus 0.25")
                        }
                    }
                }
            }
        }

        // Live Calculated Portion Nutrition
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Portion Nutrition (${portionWeight}g)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "$portionCalories kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Protein: ${portionProtein}g", color = CyanAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Carbs: ${portionCarbs}g", color = AmberWarning, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Fat: ${portionFat}g", color = IndigoAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Meal Slot Picker
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Meal Slot",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MealType.entries.forEach { slot ->
                    FilterChip(
                        selected = selectedMealType == slot,
                        onClick = { onMealTypeChanged(slot) },
                        label = { Text(slot.displayName, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
            }
        }

        // Log Button
        Button(
            onClick = onConfirmLog,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color(0xFF032215)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLogging
        ) {
            if (isLogging) {
                CircularProgressIndicator(color = Color(0xFF032215), modifier = Modifier.size(20.dp))
            } else {
                Text("Log to Today's Food Journal", fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CreateRecipeSheetContent(
    uiState: SavedMealsUiState,
    onSetCreateMode: (RecipeCreateMode) -> Unit,
    onAiPromptChanged: (String) -> Unit,
    onGenerateAiRecipe: () -> Unit,
    onRecipeNameChanged: (String) -> Unit,
    onRecipeServingsChanged: (String) -> Unit,
    onUpdateIngredientWeight: (Int, Double) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSearchFood: (String) -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onSelectProductForPortion: (NormalizedOffProduct) -> Unit,
    onAddManualIngredient: (String, Double, Double, Double, Double, Double, Boolean, String) -> Unit,
    onSaveRecipe: () -> Unit,
    onCancel: () -> Unit
) {
    var showManualAddDialog by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create Repeatable Recipe",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Mode Tabs: AI Prompt vs Manual / Search
        TabRow(
            selectedTabIndex = if (uiState.createMode == RecipeCreateMode.AI_PROMPT) 0 else 1,
            containerColor = ObsidianCard,
            contentColor = EmeraldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (uiState.createMode == RecipeCreateMode.AI_PROMPT) 0 else 1]),
                    color = EmeraldPrimary
                )
            }
        ) {
            Tab(
                selected = uiState.createMode == RecipeCreateMode.AI_PROMPT,
                onClick = { onSetCreateMode(RecipeCreateMode.AI_PROMPT) },
                text = { Text("AI Recipe (Gemini 3.1 Lite)", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = uiState.createMode == RecipeCreateMode.MANUAL_SEARCH,
                onClick = { onSetCreateMode(RecipeCreateMode.MANUAL_SEARCH) },
                text = { Text("Ingredients & Servings", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                icon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = CrimsonDanger
            )
        }

        when (uiState.createMode) {
            RecipeCreateMode.AI_PROMPT -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Describe your recipe or meal to Gemini Flash 3.1 Lite AI:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Include ingredients, weights/servings, and cooking oils. E.g.:\n'Family Bolognese: 500g 5% minced beef, 1 onion, 2 cloves garlic, 400g canned tomatoes, 300g whole wheat pasta, 1 tbsp olive oil. Makes 4 servings.'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.aiRecipePrompt,
                            onValueChange = onAiPromptChanged,
                            placeholder = { Text("Enter recipe ingredients, weights, and servings...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            maxLines = 6,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.aiRecipePrompt.isNotEmpty()) {
                                    IconButton(onClick = { onAiPromptChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = ObsidianCard,
                                unfocusedContainerColor = ObsidianCard
                            )
                        )

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onGenerateAiRecipe()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF032215)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isGeneratingWithAi && uiState.aiRecipePrompt.isNotBlank()
                        ) {
                            if (uiState.isGeneratingWithAi) {
                                CircularProgressIndicator(color = Color(0xFF032215), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Recipe with AI...", maxLines = 1, softWrap = false)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Recipe Breakdown", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            RecipeCreateMode.MANUAL_SEARCH -> {
                // Name and Servings Fields (Stacked/Balanced so floating labels never clip)
                OutlinedTextField(
                    value = uiState.createRecipeName,
                    onValueChange = onRecipeNameChanged,
                    label = { Text("Recipe Name", maxLines = 1, softWrap = false) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.createRecipeServings,
                    onValueChange = onRecipeServingsChanged,
                    label = { Text("Total Servings (e.g. 1 or 4)", maxLines = 1, softWrap = false) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Search Foods & Barcode Scanner Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Add Ingredients (Pure Staples, Search or Packet Barcode)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchFoodQuery,
                                onValueChange = onSearchFood,
                                placeholder = { Text("Search pure staples, meats, veg...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                onClick = onOpenBarcodeScanner,
                                color = EmeraldPrimary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Packet Barcode",
                                        tint = Color(0xFF032215),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Scan",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF032215),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (uiState.isLookingUpBarcode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Looking up packet in database...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (uiState.isSearchingFood) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        if (uiState.searchFoodResults.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.searchFoodResults.take(6).forEach { product ->
                                    val isPureStaple = product.brand?.contains("Staple", ignoreCase = true) == true ||
                                            product.brand?.contains("CoFID", ignoreCase = true) == true
                                    Surface(
                                        color = if (isPureStaple) EmeraldPrimary.copy(alpha = 0.08f) else ObsidianElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isPureStaple) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectProductForPortion(product) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = product.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (isPureStaple) {
                                                        Surface(
                                                            color = EmeraldPrimary.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = if (product.brand?.contains("CoFID", ignoreCase = true) == true) "Staple" else (product.brand ?: "Staple"),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = EmeraldPrimary,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${product.caloriesPer100g.roundToInt()} kcal/100g • P ${product.proteinPer100g.roundToInt()}g • C ${product.carbsPer100g.roundToInt()}g • F ${product.fatPer100g.roundToInt()}g",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Button(
                                                onClick = { onSelectProductForPortion(product) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EmeraldPrimary,
                                                    contentColor = Color(0xFF032215)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("Portion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showManualAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Custom Ingredient Manually", maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Current Ingredients List
                Text(
                    text = "Recipe Ingredients (${uiState.createRecipeIngredients.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.createRecipeIngredients.isEmpty()) {
                    Text(
                        text = "No ingredients added yet. Search or use AI prompt to populate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.createRecipeIngredients.forEachIndexed { index, item ->
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.weightGrams.roundToInt()}g • ${item.calories.roundToInt()} kcal • P ${item.proteinG.roundToInt()}g • C ${item.carbsG.roundToInt()}g • F ${item.fatG.roundToInt()}g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveIngredient(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Recipe Nutrition Preview
                    val totalWeight = uiState.createRecipeIngredients.sumOf { it.weightGrams }
                    val totalKcal = uiState.createRecipeIngredients.sumOf { it.calories }
                    val totalPro = uiState.createRecipeIngredients.sumOf { it.proteinG }
                    val totalCarb = uiState.createRecipeIngredients.sumOf { it.carbsG }
                    val totalFat = uiState.createRecipeIngredients.sumOf { it.fatG }
                    val servings = uiState.createRecipeServings.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Total: ${totalKcal.roundToInt()} kcal (${totalWeight.roundToInt()}g)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Per 1/${if (servings % 1.0 == 0.0) servings.toInt() else "%.1f".format(servings)} serving: ${(totalKcal / servings).roundToInt()} kcal • P ${(totalPro / servings).roundToInt()}g • C ${(totalCarb / servings).roundToInt()}g • F ${(totalFat / servings).roundToInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Save Recipe Button
                Button(
                    onClick = onSaveRecipe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF032215)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.createRecipeIngredients.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Recipe to Collection", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Manual Add Custom Ingredient Dialog
    if (showManualAddDialog) {
        var mName by remember { mutableStateOf("") }
        var mWeight by remember { mutableStateOf("100") }
        var mKcal by remember { mutableStateOf("150") }
        var mPro by remember { mutableStateOf("10") }
        var mCarb by remember { mutableStateOf("15") }
        var mFat by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Add Ingredient") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = mName,
                        onValueChange = { mName = it },
                        label = { Text("Name", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mWeight,
                            onValueChange = { mWeight = it },
                            label = { Text("Weight (g)", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mKcal,
                            onValueChange = { mKcal = it },
                            label = { Text("Calories", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mPro,
                            onValueChange = { mPro = it },
                            label = { Text("Protein (g)", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mCarb,
                            onValueChange = { mCarb = it },
                            label = { Text("Carbs (g)", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = mFat,
                        onValueChange = { mFat = it },
                        label = { Text("Fat (g)", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddManualIngredient(
                            mName,
                            mWeight.toDoubleOrNull() ?: 100.0,
                            mKcal.toDoubleOrNull() ?: 0.0,
                            mPro.toDoubleOrNull() ?: 0.0,
                            mCarb.toDoubleOrNull() ?: 0.0,
                            mFat.toDoubleOrNull() ?: 0.0,
                            false,
                            ""
                        )
                        showManualAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF032215))
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
