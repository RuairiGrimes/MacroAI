package com.macroai.tracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macroai.tracker.BuildConfig
import com.macroai.tracker.domain.model.ActivityLevel
import com.macroai.tracker.domain.model.BiologicalSex
import com.macroai.tracker.domain.model.EnergyTargetMode
import com.macroai.tracker.domain.model.MacroInputMode
import com.macroai.tracker.domain.model.TdeeMode
import com.macroai.tracker.domain.model.WeightUnit
import com.macroai.tracker.domain.util.TdeeCalculator
import com.macroai.tracker.ui.theme.AmberWarning
import com.macroai.tracker.ui.theme.CyanAccent
import com.macroai.tracker.ui.theme.EmeraldPrimary
import com.macroai.tracker.ui.theme.ObsidianCard
import com.macroai.tracker.ui.theme.ObsidianElevated
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TdeeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TdeeSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.healthConnectManager.createRequestPermissionContract()
    ) {
        viewModel.refreshHealthConnect()
    }

    var energyTargetMode by remember(uiState.userProfile.energyTargetMode) {
        mutableStateOf(uiState.userProfile.energyTargetMode)
    }
    var calorieGoalInput by remember(uiState.userProfile.customCalorieTargetKcal) {
        mutableStateOf(uiState.userProfile.customCalorieTargetKcal.toString())
    }
    var selectedMode by remember(uiState.userProfile.tdeeMode) {
        mutableStateOf(uiState.userProfile.tdeeMode)
    }
    var targetDeficitKcal by remember(uiState.userProfile.targetDeficitKcal) {
        mutableIntStateOf(uiState.userProfile.targetDeficitKcal)
    }
    var macroInputMode by remember(uiState.userProfile.macroInputMode) {
        mutableStateOf(uiState.userProfile.macroInputMode)
    }

    // Percentage Split states (0..100)
    var proteinPct by remember(uiState.userProfile.proteinTargetRatio) {
        mutableFloatStateOf((uiState.userProfile.proteinTargetRatio * 100f).toFloat())
    }
    var carbsPct by remember(uiState.userProfile.carbsTargetRatio) {
        mutableFloatStateOf((uiState.userProfile.carbsTargetRatio * 100f).toFloat())
    }
    var fatPct by remember(uiState.userProfile.fatTargetRatio) {
        mutableFloatStateOf((uiState.userProfile.fatTargetRatio * 100f).toFloat())
    }

    // Exact Grams states
    var customProteinG by remember(uiState.userProfile.customProteinGrams) {
        mutableIntStateOf(uiState.userProfile.customProteinGrams)
    }
    var customCarbsG by remember(uiState.userProfile.customCarbsGrams) {
        mutableIntStateOf(uiState.userProfile.customCarbsGrams)
    }
    var customFatG by remember(uiState.userProfile.customFatGrams) {
        mutableIntStateOf(uiState.userProfile.customFatGrams)
    }

    // Biometrics & Unit preferences
    var selectedWeightUnit by remember(uiState.userProfile.weightUnit) {
        mutableStateOf(uiState.userProfile.weightUnit)
    }
    var autoSyncHcBiometrics by remember(uiState.userProfile.autoSyncHealthConnectBiometrics) {
        mutableStateOf(uiState.userProfile.autoSyncHealthConnectBiometrics)
    }

    val initialWeightDisplay = remember(uiState.userProfile.weightKg, selectedWeightUnit) {
        selectedWeightUnit.formatValueOnly(uiState.userProfile.weightKg)
    }
    var weightInput by remember(initialWeightDisplay) {
        mutableStateOf(initialWeightDisplay)
    }

    var heightCmInput by remember(uiState.userProfile.heightCm) {
        mutableStateOf(String.format(Locale.US, "%.0f", uiState.userProfile.heightCm))
    }
    var ageInput by remember(uiState.userProfile.age) {
        mutableStateOf(uiState.userProfile.age.toString())
    }
    var biologicalSex by remember(uiState.userProfile.biologicalSex) {
        mutableStateOf(uiState.userProfile.biologicalSex)
    }
    var activityLevel by remember(uiState.userProfile.activityLevel) {
        mutableStateOf(uiState.userProfile.activityLevel)
    }

    var geminiApiKeyInput by remember(uiState.userProfile.customGeminiApiKey) {
        mutableStateOf(uiState.userProfile.customGeminiApiKey ?: "")
    }
    var sheetsWebhookUrlInput by remember(uiState.userProfile.customSheetsWebhookUrl) {
        mutableStateOf(uiState.userProfile.customSheetsWebhookUrl ?: "")
    }
    var isApiKeyMasked by remember { mutableStateOf(true) }

    val totalPctSum = (proteinPct.roundToInt() + carbsPct.roundToInt() + fatPct.roundToInt()).coerceAtLeast(1)
    val normProteinRatio = proteinPct.roundToInt().toDouble() / totalPctSum
    val normCarbsRatio = carbsPct.roundToInt().toDouble() / totalPctSum
    val normFatRatio = fatPct.roundToInt().toDouble() / totalPctSum

    val parsedWeight = weightInput.toDoubleOrNull()
    val draftWeightKg = if (parsedWeight != null) {
        selectedWeightUnit.toKg(parsedWeight).coerceIn(30.0, 300.0)
    } else {
        uiState.userProfile.weightKg
    }

    val parsedCalorieGoal = calorieGoalInput.toIntOrNull()?.coerceAtLeast(800) ?: 2100
    val draftProfile = uiState.userProfile.copy(
        weightKg = draftWeightKg,
        heightCm = heightCmInput.toDoubleOrNull()?.coerceAtLeast(100.0) ?: 178.0,
        age = ageInput.toIntOrNull()?.coerceIn(10, 110) ?: 30,
        biologicalSex = biologicalSex,
        activityLevel = activityLevel,
        tdeeMode = selectedMode,
        energyTargetMode = energyTargetMode,
        customCalorieTargetKcal = parsedCalorieGoal,
        manualOverrideKcal = parsedCalorieGoal,
        targetDeficitKcal = targetDeficitKcal,
        macroInputMode = macroInputMode,
        proteinTargetRatio = normProteinRatio,
        carbsTargetRatio = normCarbsRatio,
        fatTargetRatio = normFatRatio,
        customProteinGrams = customProteinG,
        customCarbsGrams = customCarbsG,
        customFatGrams = customFatG,
        weightUnit = selectedWeightUnit,
        autoSyncHealthConnectBiometrics = autoSyncHcBiometrics,
        customGeminiApiKey = geminiApiKeyInput.trim().ifEmpty { null },
        customSheetsWebhookUrl = sheetsWebhookUrlInput.trim().ifEmpty { null }
    )

    val livePreview = TdeeCalculator.calculateTdee(
        profile = draftProfile,
        healthConnectWeightKg = uiState.healthSnapshot.latestWeightKg,
        healthConnectHeightCm = uiState.healthSnapshot.latestHeightCm,
        healthConnectActiveCaloriesKcal = uiState.healthSnapshot.activeCaloriesBurnedKcal
    )

    LaunchedEffect(uiState.savedFeedback) {
        if (uiState.savedFeedback) {
            snackbarHostState.showSnackbar("Goals & Custom Macro Targets saved.")
            viewModel.consumeSavedFeedback()
        }
    }

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        text = "Calorie & Macro Studio",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshHealthConnect) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Health Connect")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Interactive Summary Header
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(EmeraldPrimary.copy(alpha = 0.5f), CyanAccent.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    EmeraldPrimary.copy(alpha = 0.12f),
                                    CyanAccent.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Calorie Target (Goal)",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${livePreview.energyTargetKcal} kcal",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TDEE (Burned)",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${livePreview.tdeeKcal} kcal",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Planned Deficit & 4-Week Weight Projection Badge
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
                            val deficitSign = if (livePreview.plannedDeficitKcal >= 0) "-" else "+"
                            val absDeficit = kotlin.math.abs(livePreview.plannedDeficitKcal)
                            val estWeightKg4W = (absDeficit * 28.0) / 7700.0
                            val unit = selectedWeightUnit
                            val estWeightConverted = unit.fromKg(estWeightKg4W)

                            Text(
                                text = "Planned Deficit: $deficitSign$absDeficit kcal/day",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (livePreview.plannedDeficitKcal >= 0) CyanAccent else AmberWarning
                            )

                            Text(
                                text = "4-Wk: $deficitSign${String.format(Locale.US, "%.1f", estWeightConverted)} ${unit.symbol}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroPillBadge(
                            label = "Protein",
                            grams = livePreview.proteinTargetGrams,
                            kcal = livePreview.proteinTargetGrams * 4,
                            color = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MacroPillBadge(
                            label = "Carbs",
                            grams = livePreview.carbsTargetGrams,
                            kcal = livePreview.carbsTargetGrams * 4,
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MacroPillBadge(
                            label = "Fat",
                            grams = livePreview.fatTargetGrams,
                            kcal = livePreview.fatTargetGrams * 9,
                            color = AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Daily Calorie Target Card (Energy Consumed Goal)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Daily Calorie Target (Energy In)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Your total calorie consumed goal. This is separate from your TDEE expenditure so that deficit and weight loss are calculated accurately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    EnergyTargetMode.entries.forEach { mode ->
                        val isSelected = energyTargetMode == mode
                        Surface(
                            onClick = { energyTargetMode = mode },
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else ObsidianElevated.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { energyTargetMode = mode }
                                )
                                Column(modifier = Modifier.padding(start = 6.dp)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Set Calorie Goal Controls
                    AnimatedVisibility(visible = energyTargetMode == EnergyTargetMode.SET_CALORIE_GOAL) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "Quick Calorie Presets",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1500, 1750, 1900, 2000, 2100, 2200, 2500).forEach { presetKcal ->
                                    FilterChip(
                                        selected = calorieGoalInput.toIntOrNull() == presetKcal,
                                        onClick = { calorieGoalInput = presetKcal.toString() },
                                        label = { Text("$presetKcal kcal") }
                                    )
                                }
                            }

                            val sliderVal = (calorieGoalInput.toFloatOrNull() ?: 2100f).coerceIn(1200f, 3800f)
                            Slider(
                                value = sliderVal,
                                onValueChange = {
                                    val stepped = ((it / 25f).roundToInt() * 25)
                                    calorieGoalInput = stepped.toString()
                                },
                                valueRange = 1200f..3800f,
                                colors = SliderDefaults.colors(
                                    thumbColor = EmeraldPrimary,
                                    activeTrackColor = EmeraldPrimary
                                )
                            )

                            OutlinedTextField(
                                value = calorieGoalInput,
                                onValueChange = { calorieGoalInput = it },
                                label = { Text("Exact Daily Calorie Consumed Goal (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Deficit from TDEE Controls
                    AnimatedVisibility(visible = energyTargetMode == EnergyTargetMode.TDEE_DEFICIT) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "Target Calorie Deficit from TDEE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    0 to "Maintenance (0)",
                                    300 to "Lean Cut (-300)",
                                    500 to "Standard Cut (-500)",
                                    750 to "Aggressive (-750)"
                                ).forEach { (deficit, label) ->
                                    FilterChip(
                                        selected = targetDeficitKcal == deficit,
                                        onClick = { targetDeficitKcal = deficit },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. TDEE Expenditure Card (Energy Burned)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Total Daily Energy Expenditure (TDEE Burned)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "How many calories your body burns per day (Energy Out / Maintenance).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TdeeMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            onClick = { selectedMode = mode },
                            color = if (isSelected) CyanAccent.copy(alpha = 0.12f) else ObsidianElevated.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyanAccent else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedMode = mode }
                                )
                                Column(modifier = Modifier.padding(start = 6.dp)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (selectedMode == TdeeMode.FORMULA_MULTIPLIER) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "Activity Multiplier (BMR: ${livePreview.bmrKcal} kcal)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActivityLevel.entries.forEach { level ->
                                    FilterChip(
                                        selected = activityLevel == level,
                                        onClick = { activityLevel = level },
                                        label = { Text("${level.label} (${level.multiplier}x)") }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedMode == TdeeMode.HEALTH_CONNECT_DYNAMIC) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Text(
                                text = "Health Connect Active Burn: +${livePreview.activeCaloriesKcal} kcal (BMR: ${livePreview.bmrKcal} kcal)",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Custom Macro Selector Card (Exact Grams OR Percentage Split)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Custom Macro Selector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        MacroInputMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = macroInputMode == mode,
                                onClick = { macroInputMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = MacroInputMode.entries.size
                                )
                            ) {
                                Text(mode.label)
                            }
                        }
                    }

                    if (macroInputMode == MacroInputMode.CUSTOM_GRAMS) {
                        val macroKcalSum = (customProteinG * 4) + (customCarbsG * 4) + (customFatG * 9)

                        val gPerLb = customProteinG / (draftWeightKg * 2.20462262).coerceAtLeast(1.0)
                        val gPerKg = customProteinG / draftWeightKg.coerceAtLeast(1.0)
                        val proteinSubtitle = if (selectedWeightUnit == WeightUnit.LBS) {
                            "${customProteinG * 4} kcal • ${String.format(Locale.US, "%.2f", gPerLb)} g/lb (${String.format(Locale.US, "%.1f", gPerKg)} g/kg)"
                        } else {
                            "${customProteinG * 4} kcal • ${String.format(Locale.US, "%.1f", gPerKg)} g/kg (${String.format(Locale.US, "%.2f", gPerLb)} g/lb)"
                        }

                        CustomGramSliderRow(
                            label = "Protein",
                            grams = customProteinG,
                            valueRange = 50f..320f,
                            color = EmeraldPrimary,
                            subtitle = proteinSubtitle,
                            onGramsChange = { customProteinG = it }
                        )
                        CustomGramSliderRow(
                            label = "Carbohydrates",
                            grams = customCarbsG,
                            valueRange = 0f..450f,
                            color = CyanAccent,
                            subtitle = "${customCarbsG * 4} kcal",
                            onGramsChange = { customCarbsG = it }
                        )
                        CustomGramSliderRow(
                            label = "Dietary Fat",
                            grams = customFatG,
                            valueRange = 20f..180f,
                            color = AmberWarning,
                            subtitle = "${customFatG * 9} kcal",
                            onGramsChange = { customFatG = it }
                        )

                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Macro Sum: $macroKcalSum kcal",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "4×P + 4×C + 9×F",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        energyTargetMode = EnergyTargetMode.SET_CALORIE_GOAL
                                        calorieGoalInput = macroKcalSum.toString()
                                    }
                                ) {
                                    Text(
                                        text = "Match Target",
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Weight-Loss & Performance Presets",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("High-Protein Cut (40/30/30)", 40f, 30f to 30f),
                                Triple("Balanced Cut (35/35/30)", 35f, 35f to 30f),
                                Triple("Low-Carb (40/15/45)", 40f, 15f to 45f),
                                Triple("Athletic (30/45/25)", 30f, 45f to 25f)
                            ).forEach { (title, p, cf) ->
                                FilterChip(
                                    selected = proteinPct.roundToInt() == p.roundToInt() &&
                                        carbsPct.roundToInt() == cf.first.roundToInt() &&
                                        fatPct.roundToInt() == cf.second.roundToInt(),
                                    onClick = {
                                        proteinPct = p
                                        carbsPct = cf.first
                                        fatPct = cf.second
                                    },
                                    label = { Text(title, maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        PercentageMacroSliderRow(
                            label = "Protein",
                            pct = (normProteinRatio * 100).roundToInt(),
                            grams = livePreview.proteinTargetGrams,
                            rawSliderValue = proteinPct,
                            color = EmeraldPrimary,
                            onValueChange = { proteinPct = it }
                        )
                        PercentageMacroSliderRow(
                            label = "Carbohydrates",
                            pct = (normCarbsRatio * 100).roundToInt(),
                            grams = livePreview.carbsTargetGrams,
                            rawSliderValue = carbsPct,
                            color = CyanAccent,
                            onValueChange = { carbsPct = it }
                        )
                        PercentageMacroSliderRow(
                            label = "Dietary Fat",
                            pct = (normFatRatio * 100).roundToInt(),
                            grams = livePreview.fatTargetGrams,
                            rawSliderValue = fatPct,
                            color = AmberWarning,
                            onValueChange = { fatPct = it }
                        )
                    }
                }
            }

            // 3. Health Connect Food Write & Biometric Sync Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.healthSnapshot.hasAllPermissions) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.HealthAndSafety
                            },
                            contentDescription = null,
                            tint = if (uiState.healthSnapshot.hasAllPermissions) {
                                EmeraldPrimary
                            } else {
                                CyanAccent
                            }
                        )
                        Text(
                            text = "3. Health Connect Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    val hc = uiState.healthSnapshot
                    Text(
                        text = when {
                            !hc.isAvailable -> "Health Connect is not available on this device."
                            hc.hasAllPermissions -> "Connected — Meals logged in MacroAI automatically sync into Health Connect (NutritionRecords), and weight/height automatically sync into your profile."
                            else -> "Grant Health Connect permissions so MacroAI can log your meals and automatically sync weight & height from smart scales and connected health apps."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (hc.isAvailable) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        // Nutrition write status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meal Nutrition Logging",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Surface(
                                color = if (hc.hasWriteNutritionPermission) EmeraldPrimary.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (hc.hasWriteNutritionPermission) "Active" else "Permission Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hc.hasWriteNutritionPermission) EmeraldPrimary else AmberWarning,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Auto-sync biometrics switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Auto-Sync Weight & Height",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automatically pulls updated weight & height records from Health Connect into your profile and TDEE calculations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoSyncHcBiometrics,
                                onCheckedChange = {
                                    autoSyncHcBiometrics = it
                                    viewModel.updateAutoSyncBiometrics(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = EmeraldPrimary.copy(alpha = 0.35f)
                                )
                            )
                        }

                        // Health Connect latest readings details
                        Surface(
                            color = ObsidianElevated.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Latest Health Connect Records",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    if (livePreview.isUsingHealthConnectWeight) {
                                        Surface(
                                            color = EmeraldPrimary.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Synced",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (hc.latestWeightKg != null) {
                                    val hcKg = hc.latestWeightKg
                                    val hcLbs = hcKg * 2.20462262
                                    val displayMain = if (selectedWeightUnit == WeightUnit.LBS) {
                                        "${String.format(Locale.US, "%.1f", hcLbs)} lbs (${String.format(Locale.US, "%.1f", hcKg)} kg)"
                                    } else {
                                        "${String.format(Locale.US, "%.1f", hcKg)} kg (${String.format(Locale.US, "%.1f", hcLbs)} lbs)"
                                    }
                                    Text(
                                        text = "Weight: $displayMain • ${formatInstant(hc.latestWeightTime)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        text = "Weight: No record found in Health Connect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (hc.latestHeightCm != null) {
                                    val cm = hc.latestHeightCm
                                    val totalInches = cm / 2.54
                                    val feet = (totalInches / 12).toInt()
                                    val inches = (totalInches % 12).roundToInt()
                                    Text(
                                        text = "Height: ${String.format(Locale.US, "%.0f", cm)} cm ($feet ft $inches in) • ${formatInstant(hc.latestHeightTime)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        text = "Height: No record found in Health Connect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = viewModel::syncBiometricsNow,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", maxLines = 1, softWrap = false)
                            }

                            if (!hc.hasAllPermissions) {
                                Button(
                                    onClick = {
                                        hcPermissionLauncher.launch(viewModel.healthConnectManager.requiredPermissions)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Grant Access", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Biometrics Profile & Unit Preferences Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. Biometrics Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "BMR: ${livePreview.bmrKcal} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Weight Unit Selector (kg vs lbs)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred Weight Unit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            WeightUnit.entries.forEachIndexed { index, unit ->
                                SegmentedButton(
                                    selected = selectedWeightUnit == unit,
                                    onClick = {
                                        if (unit != selectedWeightUnit) {
                                            val currentVal = weightInput.toDoubleOrNull()
                                            if (currentVal != null) {
                                                val currentKg = selectedWeightUnit.toKg(currentVal)
                                                val converted = unit.fromKg(currentKg)
                                                weightInput = String.format(Locale.US, "%.1f", converted)
                                            }
                                            selectedWeightUnit = unit
                                            viewModel.updateWeightUnit(unit)
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = WeightUnit.entries.size
                                    )
                                ) {
                                    Text(unit.label, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    // Row 1: Weight & Height in spacious 2-column layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val weightSupportingText = parsedWeight?.let {
                            if (selectedWeightUnit == WeightUnit.KG) {
                                "≈ ${String.format(Locale.US, "%.1f", WeightUnit.KG.fromKg(it))} lbs"
                            } else {
                                "≈ ${String.format(Locale.US, "%.1f", WeightUnit.LBS.toKg(it))} kg"
                            }
                        } ?: ""

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (${selectedWeightUnit.symbol})", maxLines = 1, softWrap = false) },
                            supportingText = {
                                if (livePreview.isUsingHealthConnectWeight) {
                                    Text(
                                        text = "✓ HC ($weightSupportingText)",
                                        color = EmeraldPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else if (weightSupportingText.isNotBlank()) {
                                    Text(text = weightSupportingText, maxLines = 1, softWrap = false)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        val parsedHeight = heightCmInput.toDoubleOrNull()
                        val heightSupportingText = parsedHeight?.let {
                            val totalInches = it / 2.54
                            val feet = (totalInches / 12).toInt()
                            val inches = (totalInches % 12).roundToInt()
                            "≈ $feet ft $inches in"
                        } ?: ""

                        OutlinedTextField(
                            value = heightCmInput,
                            onValueChange = { heightCmInput = it },
                            label = { Text("Height (cm)", maxLines = 1, softWrap = false) },
                            supportingText = {
                                if (livePreview.isUsingHealthConnectHeight) {
                                    Text(
                                        text = "✓ HC ($heightSupportingText)",
                                        color = EmeraldPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else if (heightSupportingText.isNotBlank()) {
                                    Text(text = heightSupportingText, maxLines = 1, softWrap = false)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Age & Biological Sex Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Age (years)", maxLines = 1, softWrap = false) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Biological Sex",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BiologicalSex.entries.forEach { sex ->
                                    FilterChip(
                                        selected = biologicalSex == sex,
                                        onClick = { biologicalSex = sex },
                                        label = { Text(sex.label, maxLines = 1, softWrap = false) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Cloud & AI Integration (Gemini API & Google Sheets Backup)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "4. Cloud & AI Integration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Your personal Gemini AI key & Google Sheets webhook URL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.setShowSetupGuideDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Setup Guide Tutorial",
                                tint = CyanAccent
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // --- GEMINI API KEY SECTION ---
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gemini AI API Key",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val geminiStatusText = when {
                                geminiApiKeyInput.isNotBlank() -> "Custom Key Set"
                                BuildConfig.GEMINI_API_KEY.isNotBlank() -> "Build Default Active"
                                else -> "Key Missing"
                            }
                            val geminiStatusColor = when {
                                geminiApiKeyInput.isNotBlank() -> EmeraldPrimary
                                BuildConfig.GEMINI_API_KEY.isNotBlank() -> CyanAccent
                                else -> Color(0xFFEF4444)
                            }
                            Surface(
                                color = geminiStatusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = geminiStatusText,
                                    color = geminiStatusColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = geminiApiKeyInput,
                            onValueChange = {
                                geminiApiKeyInput = it
                                viewModel.dismissTestResults()
                            },
                            label = { Text("Gemini API Key (Google AI Studio)") },
                            placeholder = {
                                if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                                    Text("Using build-time default key")
                                } else {
                                    Text("AIzaSy...")
                                }
                            },
                            visualTransformation = if (isApiKeyMasked) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyMasked = !isApiKeyMasked }) {
                                    Icon(
                                        imageVector = if (isApiKeyMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isApiKeyMasked) "Show API Key" else "Hide API Key"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.setShowSetupGuideDialog(true) }) {
                                Text(
                                    text = "Get Free Gemini Key →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }

                            Button(
                                onClick = { viewModel.testGeminiApiKey(geminiApiKeyInput) },
                                enabled = !uiState.isTestingGeminiKey,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isTestingGeminiKey) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Key", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        uiState.geminiKeyTestResult?.let { result ->
                            val isSuccess = result.startsWith("✓")
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) EmeraldPrimary else Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // --- GOOGLE SHEETS WEBHOOK SECTION ---
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Sheets Webhook URL",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val sheetsStatusText = when {
                                sheetsWebhookUrlInput.isNotBlank() -> "Custom Webhook Set"
                                BuildConfig.SHEETS_WEBHOOK_URL.isNotBlank() -> "Build Default Active"
                                else -> "Local Only (No Sync)"
                            }
                            val sheetsStatusColor = when {
                                sheetsWebhookUrlInput.isNotBlank() -> EmeraldPrimary
                                BuildConfig.SHEETS_WEBHOOK_URL.isNotBlank() -> CyanAccent
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = sheetsStatusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = sheetsStatusText,
                                    color = sheetsStatusColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = sheetsWebhookUrlInput,
                            onValueChange = {
                                sheetsWebhookUrlInput = it
                                viewModel.dismissTestResults()
                            },
                            label = { Text("Apps Script Webhook URL (/exec)") },
                            placeholder = {
                                if (BuildConfig.SHEETS_WEBHOOK_URL.isNotBlank()) {
                                    Text("Using build-time webhook URL")
                                } else {
                                    Text("https://script.google.com/macros/s/.../exec")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.setShowSetupGuideDialog(true) }) {
                                Text(
                                    text = "Create Sheet & Webhook →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }

                            Button(
                                onClick = { viewModel.testSheetsWebhookUrl(sheetsWebhookUrlInput) },
                                enabled = !uiState.isTestingSheetsWebhook,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isTestingSheetsWebhook) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pinging...", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Webhook", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        uiState.sheetsWebhookTestResult?.let { result ->
                            val isSuccess = result.startsWith("✓")
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) EmeraldPrimary else Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Action row: Save Credentials / Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (geminiApiKeyInput.isNotBlank() || sheetsWebhookUrlInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    geminiApiKeyInput = ""
                                    sheetsWebhookUrlInput = ""
                                    viewModel.clearCloudCredentials()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clear Keys",
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.updateGeminiApiKey(geminiApiKeyInput)
                                viewModel.updateSheetsWebhookUrl(sheetsWebhookUrlInput)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Credentials", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(EmeraldPrimary, CyanAccent)))
                    .clickable { viewModel.saveUserProfile(draftProfile) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save Calorie & Macro Targets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF032215)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (uiState.showSetupGuideDialog) {
            SetupGuideDialog(onDismiss = { viewModel.setShowSetupGuideDialog(false) })
        }
    }
}

@Composable
private fun MacroPillBadge(
    label: String,
    grams: Int,
    kcal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${grams}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "$kcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun CustomGramSliderRow(
    label: String,
    grams: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
    subtitle: String,
    onGramsChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { onGramsChange((grams - 5).coerceAtLeast(valueRange.start.toInt())) },
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "-5g",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "${grams}g",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Surface(
                    onClick = { onGramsChange((grams + 5).coerceAtMost(valueRange.endInclusive.toInt())) },
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+5g",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Slider(
            value = grams.toFloat().coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { onGramsChange(it.roundToInt()) },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}

@Composable
private fun PercentageMacroSliderRow(
    label: String,
    pct: Int,
    grams: Int,
    rawSliderValue: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "$pct% (${grams}g)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
        }
        Slider(
            value = rawSliderValue.coerceIn(5f, 70f),
            onValueChange = onValueChange,
            valueRange = 5f..70f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}

private fun formatInstant(instant: Instant?): String {
    if (instant == null) return "Recent"
    val zone = ZoneId.systemDefault()
    val localDateTime = instant.atZone(zone).toLocalDateTime()
    val today = LocalDate.now()
    return if (localDateTime.toLocalDate() == today) {
        String.format(Locale.US, "Today at %02d:%02d", localDateTime.hour, localDateTime.minute)
    } else {
        String.format(
            Locale.US,
            "%d %s %d, %02d:%02d",
            localDateTime.dayOfMonth,
            localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
            localDateTime.year,
            localDateTime.hour,
            localDateTime.minute
        )
    }
}

@Composable
private fun SetupGuideDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cloud & AI Setup Guide",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ObsidianElevated,
                    contentColor = EmeraldPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("1. Gemini API", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("2. Google Sheets", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    )
                }

                if (selectedTab == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "How to get a Free Gemini API Key:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        GuideStep(
                            number = "1",
                            title = "Open Google AI Studio",
                            body = "Visit aistudio.google.com in your browser and sign in with any standard Google account."
                        )
                        GuideStep(
                            number = "2",
                            title = "Create API Key",
                            body = "Tap 'Get API key' -> 'Create API key'. Select or create a free default project."
                        )
                        GuideStep(
                            number = "3",
                            title = "Copy & Paste",
                            body = "Copy your key (starts with 'AIzaSy...') and paste it into the Gemini API Key field in MacroAI."
                        )
                        GuideStep(
                            number = "4",
                            title = "Test Connection",
                            body = "Tap 'Test Key' to verify. The free tier provides generous limits (15 requests/min) with zero charges."
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "How to connect your personal Google Sheet:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        GuideStep(
                            number = "1",
                            title = "Create a Google Sheet",
                            body = "Open sheets.new in your browser and give it a name (e.g. 'My MacroAI Journal')."
                        )
                        GuideStep(
                            number = "2",
                            title = "Open Apps Script",
                            body = "In the top menu toolbar, click Extensions -> Apps Script."
                        )
                        GuideStep(
                            number = "3",
                            title = "Paste the Webhook Script",
                            body = "Delete any code in Code.gs and paste the code from scripts/google_apps_script_webhook.js in the MacroAI repo."
                        )
                        GuideStep(
                            number = "4",
                            title = "Deploy as Web App",
                            body = "Click Deploy -> New deployment. Select 'Web app'. Set Execute as: 'Me', and Who has access: 'Anyone'."
                        )
                        GuideStep(
                            number = "5",
                            title = "Copy Web App URL",
                            body = "Click Deploy, authorize permissions, and copy the URL ending in '/exec'."
                        )
                        GuideStep(
                            number = "6",
                            title = "Connect to MacroAI",
                            body = "Paste the URL into Google Sheets Webhook URL in MacroAI and tap 'Test Webhook'."
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun GuideStep(
    number: String,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = ObsidianElevated,
            shape = CircleShape,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
