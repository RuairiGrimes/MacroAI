package com.macroai.tracker.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.ServingOption
import com.macroai.tracker.domain.model.ServingUnitType
import com.macroai.tracker.ui.theme.AmberWarning
import com.macroai.tracker.ui.theme.CrimsonDanger
import com.macroai.tracker.ui.theme.CyanAccent
import com.macroai.tracker.ui.theme.EmeraldGlow
import com.macroai.tracker.ui.theme.EmeraldPrimary
import com.macroai.tracker.ui.theme.ObsidianCard
import com.macroai.tracker.ui.theme.ObsidianElevated
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: FoodSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.loggedSuccessMessage) {
        uiState.loggedSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSuccessMessage()
            onNavigateBack()
        }
    }

    BackHandler(enabled = uiState.selectedProduct != null) {
        viewModel.dismissProductDetail()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.selectedProduct != null) {
                                viewModel.dismissProductDetail()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = if (uiState.selectedProduct != null) "Portion & Nutrition" else "Search Food Database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (uiState.selectedProduct != null) {
                // Detail & Portion Selector Screen (Cronometer / MyFitnessPal style)
                FoodPortionDetailView(
                    product = uiState.selectedProduct!!,
                    servingOptions = uiState.availableServingOptions,
                    selectedOption = uiState.selectedServingOption,
                    quantityInput = uiState.quantityInput,
                    selectedMealType = uiState.selectedMealType,
                    calculatedGrams = viewModel.computeCurrentTotalGrams(),
                    onSelectOption = viewModel::selectServingOption,
                    onQuantityChanged = viewModel::onQuantityInputChanged,
                    onStepQuantity = viewModel::stepQuantity,
                    onPresetQuantity = viewModel::setPresetQuantity,
                    onSelectMealType = viewModel::setMealType,
                    onConfirmLog = viewModel::logSelectedFood,
                    onCancel = viewModel::dismissProductDetail
                )
            } else {
                // Search list view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by food name, brand, or barcode…") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.onSearchActionTriggered()
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard
                        )
                    )

                    // Loading indicator
                    if (uiState.isSearching && !uiState.isAiSearching) {
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "Searching Open Food Facts database…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Gemini Flash 3.1 Lite AI Loading indicator
                    if (uiState.isAiSearching) {
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "Gemini Flash 3.1 Lite analyzing food & macros…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Error banner
                    if (uiState.errorMessage != null && !uiState.isSearching) {
                        Surface(
                            color = CrimsonDanger.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonDanger,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = uiState.errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonDanger
                                )
                            }
                        }
                    }

                    // Gemini Flash 3.1 Lite AI Search Card
                    if (uiState.query.isNotBlank() && !uiState.isSearching) {
                        Card(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.searchWithGeminiAi()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(EmeraldPrimary.copy(alpha = 0.55f), CyanAccent.copy(alpha = 0.35f))
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(EmeraldPrimary, CyanAccent))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF032215),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Food Search (Gemini Flash 3.1 Lite)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "Estimate macros & ingredients for \"${uiState.query}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Empty query state: Popular staples suggestions
                    if (uiState.query.isBlank() && uiState.searchResults.isEmpty() && !uiState.isSearching) {
                        PopularStaplesView(
                            staples = viewModel.popularStaples,
                            onSelectStaple = { staple ->
                                keyboardController?.hide()
                                viewModel.selectStaple(staple)
                            }
                        )
                    }

                    // Search results list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.searchResults, key = { it.barcode + it.name }) { product ->
                            FoodSearchResultCard(
                                product = product,
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.selectProduct(product)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularStaplesView(
    staples: List<String>,
    onSelectStaple: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Global Open Food Facts Database",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
                Text(
                    text = "Search individual food items, produce, and packaged goods. Choose custom serving sizes, grams, or portion multipliers with live macro calculation, UPF classification, and seed oil analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Popular Staples & Quick Ideas",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            staples.forEach { staple ->
                Surface(
                    onClick = { onSelectStaple(staple) },
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = staple,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResultCard(
    product: NormalizedOffProduct,
    onClick: () -> Unit
) {
    val isStaple = product.brand?.contains("Staple", ignoreCase = true) == true ||
            product.brand?.contains("CoFID", ignoreCase = true) == true

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (isStaple) EmeraldPrimary.copy(alpha = 0.05f) else ObsidianCard),
        border = if (isStaple) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isStaple) {
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (product.brand?.contains("CoFID", ignoreCase = true) == true) "UK/Irish Staple" else (product.brand ?: "Staple"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (!product.brand.isNullOrBlank() && !isStaple) {
                        Text(
                            text = product.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Serving indication tag
                val servingTag = product.servingSizeName?.takeIf { it.isNotBlank() }
                    ?: (product.servingQuantityGrams?.let { "${it.toInt()}g" } ?: "100g")
                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = servingTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            }

            // Macros per 100g snippet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${product.caloriesPer100g.roundToInt()} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${product.proteinPer100g.roundToInt()}g P",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )
                Text(
                    text = "${product.carbsPer100g.roundToInt()}g C",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent
                )
                Text(
                    text = "${product.fatPer100g.roundToInt()}g F",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmberWarning
                )
            }

            // Badges row: NOVA + Seed Oils
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.novaGroup != null) {
                    val novaColor = when (product.novaGroup) {
                        1, 2 -> EmeraldPrimary
                        3 -> AmberWarning
                        4 -> CrimsonDanger
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        color = novaColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (product.novaGroup == 4) "NOVA 4 (UPF)" else "NOVA ${product.novaGroup}",
                            style = MaterialTheme.typography.labelSmall,
                            color = novaColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val seedOilAssessment = product.seedOilAssessment(product.defaultServingGrams)
                val seedOilColor = when (seedOilAssessment.riskLevel) {
                    com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> EmeraldPrimary
                    com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> AmberWarning
                    com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> CrimsonDanger
                }

                if (product.containsSeedOils) {
                    Surface(
                        color = seedOilColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = seedOilAssessment.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = seedOilColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (product.ingredientsText.isNotBlank()) {
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Seed-Oil Free",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Serving Size Selector & Nutrition Review Panel
 * Mirrors the portioning functionality of Cronometer / MyFitnessPal
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodPortionDetailView(
    product: NormalizedOffProduct,
    servingOptions: List<ServingOption>,
    selectedOption: ServingOption?,
    quantityInput: String,
    selectedMealType: MealType,
    calculatedGrams: Double,
    onSelectOption: (ServingOption) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onStepQuantity: (Double) -> Unit,
    onPresetQuantity: (Double) -> Unit,
    onSelectMealType: (MealType) -> Unit,
    onConfirmLog: () -> Unit,
    onCancel: () -> Unit
) {
    val factor = calculatedGrams.coerceAtLeast(0.0) / 100.0
    val scaledKcal = product.caloriesPer100g * factor
    val scaledProtein = product.proteinPer100g * factor
    val scaledCarbs = product.carbsPer100g * factor
    val scaledFat = product.fatPer100g * factor
    val scaledSatFat = product.saturatedFatPer100g * factor
    val scaledPufa = product.polyunsaturatedFatPer100g * factor
    val scaledMufa = product.monounsaturatedFatPer100g * factor
    val scaledSugar = product.sugarPer100g * factor
    val scaledFibre = product.fibrePer100g * factor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Food Title & Brand Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            if (!product.brand.isNullOrBlank()) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quality Banner (NOVA + Seed Oils)
        val currentGrams = calculatedGrams
        QualityStatusRow(
            novaGroup = product.novaGroup,
            containsSeedOils = product.containsSeedOils,
            detectedSeedOils = product.detectedSeedOilsCsv,
            seedOilAssessment = product.seedOilAssessment(currentGrams)
        )

        // Meal Type Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Log to Meal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MealType.entries.forEach { mealType ->
                    FilterChip(
                        selected = selectedMealType == mealType,
                        onClick = { onSelectMealType(mealType) },
                        label = { Text(mealType.displayName, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
            }
        }

        // Serving Unit Selector (Cronometer style: Grams, Serving, Ounces, 100g)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Serving Unit",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                servingOptions.forEach { opt ->
                    val isSelected = selectedOption?.type == opt.type
                    Surface(
                        onClick = { onSelectOption(opt) },
                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.20f) else ObsidianElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = opt.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Portion Quantity Stepper & Input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Number of ${selectedOption?.label ?: "Units"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "= ${calculatedGrams.roundToInt()}g total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = {
                        val step = when (selectedOption?.type) {
                            ServingUnitType.GRAMS -> -10.0
                            ServingUnitType.OUNCES -> -0.5
                            ServingUnitType.SERVING -> -0.5
                            else -> -0.25
                        }
                        onStepQuantity(step)
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianElevated)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                }

                val keyboardController = LocalSoftwareKeyboardController.current

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = onQuantityChanged,
                    label = { Text("Amount", maxLines = 1, softWrap = false) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard
                    )
                )

                IconButton(
                    onClick = {
                        val step = when (selectedOption?.type) {
                            ServingUnitType.GRAMS -> 10.0
                            ServingUnitType.OUNCES -> 0.5
                            ServingUnitType.SERVING -> 0.5
                            else -> 0.25
                        }
                        onStepQuantity(step)
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianElevated)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                }
            }

            // Quick preset chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (selectedOption?.type) {
                    ServingUnitType.GRAMS -> {
                        listOf(50.0, 100.0, 150.0, 200.0, 250.0, 300.0).forEach { grams ->
                            QuickPresetChip("${grams.toInt()}g") { onPresetQuantity(grams) }
                        }
                    }
                    ServingUnitType.OUNCES -> {
                        listOf(1.0, 2.0, 4.0, 6.0, 8.0).forEach { oz ->
                            QuickPresetChip("${oz.toInt()} oz") { onPresetQuantity(oz) }
                        }
                    }
                    ServingUnitType.SERVING -> {
                        listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach { s ->
                            QuickPresetChip("${s}x") { onPresetQuantity(s) }
                        }
                    }
                    else -> {
                        listOf(0.5, 1.0, 1.5, 2.0).forEach { h ->
                            QuickPresetChip("${h}x") { onPresetQuantity(h) }
                        }
                    }
                }
            }
        }

        // Live Scaled Nutrition Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nutrition for Portion",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${calculatedGrams.roundToInt()} grams total weight",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${scaledKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                // Primary Macros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScaledMacroMetric("Protein", "${scaledProtein.roundToInt()}g", EmeraldPrimary)
                    ScaledMacroMetric("Carbs", "${scaledCarbs.roundToInt()}g", CyanAccent)
                    ScaledMacroMetric("Fat", "${scaledFat.roundToInt()}g", AmberWarning)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                // Secondary breakdown (FlowRow for portrait readability)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sat Fat: ${"%.1f".format(scaledSatFat)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "PUFA: ${"%.1f".format(scaledPufa)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Fibre: ${"%.1f".format(scaledFibre)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Sugar: ${"%.1f".format(scaledSugar)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Micronutrients preview
                val sodium = (product.sodiumMgPer100g * factor).roundToInt()
                val potassium = (product.potassiumMgPer100g * factor).roundToInt()
                val calcium = (product.calciumMgPer100g * factor).roundToInt()
                val iron = (product.ironMgPer100g * factor).roundToInt()

                if (sodium > 0 || potassium > 0 || calcium > 0 || iron > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (sodium > 0) Text("Sodium: ${sodium}mg", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        if (potassium > 0) Text("Potassium: ${potassium}mg", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        if (calcium > 0) Text("Calcium: ${calcium}mg", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        if (iron > 0) Text("Iron: ${iron}mg", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Ingredients snippet if available
        if (product.ingredientsText.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.ingredientsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", maxLines = 1, softWrap = false)
            }

            Button(
                onClick = onConfirmLog,
                modifier = Modifier.weight(1.6f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color(0xFF032215)
                )
            ) {
                Text(
                    text = "Log ${calculatedGrams.roundToInt()}g (${scaledKcal.roundToInt()} kcal)",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuickPresetChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = ObsidianElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ScaledMacroMetric(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 60.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun QualityStatusRow(
    novaGroup: Int?,
    containsSeedOils: Boolean,
    detectedSeedOils: String,
    seedOilAssessment: com.macroai.tracker.domain.util.SeedOilDetectionResult? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val novaColor = when (novaGroup) {
            1, 2 -> EmeraldPrimary
            3 -> AmberWarning
            4 -> CrimsonDanger
            else -> MaterialTheme.colorScheme.outline
        }

        Surface(
            color = novaColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (novaGroup == 4) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = novaColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (novaGroup != null) {
                        if (novaGroup == 4) "NOVA 4 (UPF)" else "NOVA $novaGroup"
                    } else "NOVA Unknown",
                    style = MaterialTheme.typography.labelMedium,
                    color = novaColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val riskLevel = seedOilAssessment?.riskLevel
            ?: if (containsSeedOils) com.macroai.tracker.domain.util.SeedOilRiskLevel.RED else com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN
        val oilColor = when (riskLevel) {
            com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> EmeraldPrimary
            com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> AmberWarning
            com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> CrimsonDanger
        }
        val oilIcon = when (riskLevel) {
            com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> Icons.Default.CheckCircle
            com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> Icons.Default.Warning
            com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> Icons.Default.Warning
        }
        val oilLabel = seedOilAssessment?.label ?: if (containsSeedOils) {
            "Seed Oils: ${detectedSeedOils.ifBlank { "Detected" }}"
        } else "Seed-Oil Free"

        Surface(
            color = oilColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.2f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = oilIcon,
                    contentDescription = null,
                    tint = oilColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = oilLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = oilColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

