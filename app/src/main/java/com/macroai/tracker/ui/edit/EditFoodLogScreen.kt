package com.macroai.tracker.ui.edit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditFoodLogScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    viewModel: EditFoodLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostStateStateWrapper(SnackbarHostState()) }
    val scope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState.state) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Edit Logged Meal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = uiState.name.ifBlank { "Meal Details" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Meal",
                            tint = CrimsonDanger
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
            return@Scaffold
        }

        if (uiState.errorMessage != null && uiState.originalLog == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Log not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meal Name & Meal Type Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("Meal / Food Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Meal Slot",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MealType.entries.forEach { type ->
                            FilterChip(
                                selected = uiState.mealType == type,
                                onClick = { viewModel.onMealTypeSelected(type) },
                                label = { Text(type.displayName, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Live Macro Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(EmeraldPrimary.copy(alpha = 0.45f), CyanAccent.copy(alpha = 0.25f))),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Meal Totals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${uiState.servingWeightGrams.roundToInt()}g total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NutrientStat(label = "Calories", value = "${uiState.calories.roundToInt()}", unit = "kcal", color = EmeraldPrimary)
                        NutrientStat(label = "Protein", value = "${"%.1f".format(uiState.proteinG)}", unit = "g", color = CyanAccent)
                        NutrientStat(label = "Carbs", value = "${"%.1f".format(uiState.carbsG)}", unit = "g", color = AmberWarning)
                        NutrientStat(label = "Fat", value = "${"%.1f".format(uiState.fatG)}", unit = "g", color = IndigoAccent)
                    }

                    if (uiState.fibreG > 0 || uiState.polyunsaturatedFatG > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fibre: ${"%.1f".format(uiState.fibreG)}g • PUFA: ${"%.1f".format(uiState.polyunsaturatedFatG)}g • Sat: ${"%.1f".format(uiState.saturatedFatG)}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // If Multi-component (AI scan or Recipe)
            if (uiState.isMultiComponent) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Component Ingredients",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Adjust weights to recalculate nutrition",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setShowAddIngredientDialog(true) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Ingredient",
                                    tint = EmeraldPrimary
                                )
                            }
                        }

                        uiState.ingredients.forEachIndexed { index, ingredient ->
                            var weightText by remember(ingredient.name, ingredient.weightGrams) {
                                mutableStateOf(ingredient.weightGrams.roundToInt().toString())
                            }

                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ingredient.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${ingredient.calories.roundToInt()} kcal • P ${ingredient.proteinG.roundToInt()}g • C ${ingredient.carbsG.roundToInt()}g • F ${ingredient.fatG.roundToInt()}g",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.onRemoveIngredient(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                val curr = ingredient.weightGrams
                                                val next = (curr - 10.0).coerceAtLeast(0.0)
                                                viewModel.onUpdateIngredientWeight(index, next)
                                            },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease 10g",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = weightText,
                                            onValueChange = { newVal ->
                                                weightText = newVal
                                                newVal.toDoubleOrNull()?.let { validGrams ->
                                                    viewModel.onUpdateIngredientWeight(index, validGrams)
                                                }
                                            },
                                            label = { Text("Weight", maxLines = 1, softWrap = false) },
                                            suffix = { Text("g") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )

                                        FilledTonalIconButton(
                                            onClick = {
                                                val curr = ingredient.weightGrams
                                                val next = curr + 10.0
                                                viewModel.onUpdateIngredientWeight(index, next)
                                            },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase 10g",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Single Item / Proportion Scaling Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Serving Size & Weight Scaling",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Adjusting weight scales all calories and macros proportionally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var weightInput by remember(uiState.servingWeightGrams) {
                            mutableStateOf(uiState.servingWeightGrams.roundToInt().toString())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val curr = uiState.servingWeightGrams
                                    val next = (curr - 25.0).coerceAtLeast(1.0)
                                    viewModel.onScaleTotalWeight(next)
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Minus 25g")
                            }

                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { newVal ->
                                    weightInput = newVal
                                    newVal.toDoubleOrNull()?.let { validGrams ->
                                        if (validGrams > 0) {
                                            viewModel.onScaleTotalWeight(validGrams)
                                        }
                                    }
                                },
                                label = { Text("Weight", maxLines = 1, softWrap = false) },
                                suffix = { Text("g") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    val curr = uiState.servingWeightGrams
                                    val next = curr + 25.0
                                    viewModel.onScaleTotalWeight(next)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Plus 25g")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        Text(
                            text = "Direct Macro Adjustments",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.calories.roundToInt().toString(),
                                onValueChange = { it.toDoubleOrNull()?.let(viewModel::onCaloriesChanged) },
                                label = { Text("Calories", maxLines = 1, softWrap = false) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "%.1f".format(uiState.proteinG),
                                onValueChange = { it.toDoubleOrNull()?.let(viewModel::onProteinChanged) },
                                label = { Text("Protein (g)", maxLines = 1, softWrap = false) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = "%.1f".format(uiState.carbsG),
                                onValueChange = { it.toDoubleOrNull()?.let(viewModel::onCarbsChanged) },
                                label = { Text("Carbs (g)", maxLines = 1, softWrap = false) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "%.1f".format(uiState.fatG),
                                onValueChange = { it.toDoubleOrNull()?.let(viewModel::onFatChanged) },
                                label = { Text("Fat (g)", maxLines = 1, softWrap = false) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Food Quality: NOVA & Seed Oils
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Food Quality Classification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "NOVA Processing Group",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            1 to "Group 1 (Unprocessed)",
                            2 to "Group 2 (Culinary)",
                            3 to "Group 3 (Processed)",
                            4 to "Group 4 (UPF)"
                        ).forEach { (group, label) ->
                            FilterChip(
                                selected = uiState.novaGroup == group,
                                onClick = { viewModel.onNovaGroupChanged(group) },
                                label = { Text(label, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (group == 4) CrimsonDanger.copy(alpha = 0.25f) else EmeraldPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = if (group == 4) CrimsonDanger else EmeraldPrimary
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    val assessment = if (uiState.ingredients.isNotEmpty()) {
                        com.macroai.tracker.domain.util.SeedOilDetector.assessIngredients(
                            ingredients = uiState.ingredients,
                            totalMealWeight = uiState.servingWeightGrams,
                            totalMealFat = uiState.fatG,
                            mealName = uiState.name
                        )
                    } else {
                        com.macroai.tracker.domain.util.SeedOilDetector.assessProduct(
                            name = uiState.name,
                            containsSeedOils = uiState.containsSeedOils,
                            detectedOilsCsv = uiState.detectedSeedOils,
                            servingWeightGrams = uiState.servingWeightGrams,
                            fatG = uiState.fatG,
                            pufaG = uiState.polyunsaturatedFatG
                        )
                    }
                    val oilColor = when (assessment.riskLevel) {
                        com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> EmeraldPrimary
                        com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> AmberWarning
                        com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> CrimsonDanger
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Seed Oil Radar (${assessment.riskLevel.displayName})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = assessment.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = oilColor
                            )
                        }
                        Switch(
                            checked = uiState.containsSeedOils,
                            onCheckedChange = viewModel::onToggleSeedOils,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = oilColor,
                                checkedTrackColor = oilColor.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::openSaveAsRecipeDialog,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Recipe", maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = {
                        viewModel.saveChanges {
                            scope.launch {
                                snackbarHostState.state.showSnackbar("Meal updated successfully")
                                onNavigateBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF032215)
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Save as Recipe Dialog
    if (uiState.showSaveAsRecipeDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveAsRecipeDialog,
            title = { Text("Save as Repeatable Recipe") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Save this meal so you can log it again anytime or portion it out (e.g. 1/5 for family meals).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = uiState.recipeNameInput,
                        onValueChange = viewModel::onRecipeNameInputChanged,
                        label = { Text("Recipe / Meal Name", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.recipeServingsInput,
                        onValueChange = viewModel::onRecipeServingsInputChanged,
                        label = { Text("Total Servings (e.g. 1 or 5)", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAsRecipe {
                            scope.launch {
                                snackbarHostState.state.showSnackbar("Recipe saved to your Recipes collection!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF032215))
                ) {
                    Text("Save Recipe", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSaveAsRecipeDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Ingredient Dialog
    if (uiState.showAddIngredientDialog) {
        var newName by remember { mutableStateOf("") }
        var newWeight by remember { mutableStateOf("100") }
        var newKcal by remember { mutableStateOf("150") }
        var newProtein by remember { mutableStateOf("10") }
        var newCarbs by remember { mutableStateOf("15") }
        var newFat by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { viewModel.setShowAddIngredientDialog(false) },
            title = { Text("Add Component Item") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Item Name", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newWeight,
                            onValueChange = { newWeight = it },
                            label = { Text("Weight (g)", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newKcal,
                            onValueChange = { newKcal = it },
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
                            value = newProtein,
                            onValueChange = { newProtein = it },
                            label = { Text("Protein (g)", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCarbs,
                            onValueChange = { newCarbs = it },
                            label = { Text("Carbs (g)", maxLines = 1, softWrap = false) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newFat,
                        onValueChange = { newFat = it },
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
                        viewModel.onAddIngredient(
                            name = newName,
                            weightGrams = newWeight.toDoubleOrNull() ?: 100.0,
                            calories = newKcal.toDoubleOrNull() ?: 0.0,
                            protein = newProtein.toDoubleOrNull() ?: 0.0,
                            carbs = newCarbs.toDoubleOrNull() ?: 0.0,
                            fat = newFat.toDoubleOrNull() ?: 0.0
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF032215))
                ) {
                    Text("Add Item", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowAddIngredientDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Logged Meal") },
            text = { Text("Are you sure you want to delete this logged meal? This will remove it from today's macro intake and Health Connect.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteMeal {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private class SnackbarHostStateStateWrapper(val state: SnackbarHostState)

@Composable
private fun NutrientStat(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
