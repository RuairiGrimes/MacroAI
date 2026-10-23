package com.macroai.tracker.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.model.TdeeMode
import com.macroai.tracker.domain.util.DailyNutritionSummary
import com.macroai.tracker.domain.util.SeedOilRiskLevel
import com.macroai.tracker.domain.util.TdeeResult
import com.macroai.tracker.domain.util.UpfMetricsCalculator
import com.macroai.tracker.ui.theme.AmberWarning
import com.macroai.tracker.ui.theme.CrimsonDanger
import com.macroai.tracker.ui.theme.CyanAccent
import com.macroai.tracker.ui.theme.EmeraldGlow
import com.macroai.tracker.ui.theme.EmeraldPrimary
import com.macroai.tracker.ui.theme.IndigoAccent
import com.macroai.tracker.ui.theme.ObsidianCard
import com.macroai.tracker.ui.theme.ObsidianElevated
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (MealType?) -> Unit = {},
    onOpenRecipes: () -> Unit = {},
    onEditFoodLog: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showManualEntryDialog by rememberSaveable { mutableStateOf(false) }
    var showEndDayConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var isMicrosExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedMealNames by rememberSaveable { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "MacroAI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Nutrition • UPF • Seed Oils",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = viewModel::syncToSheetsNow,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (uiState.unsyncedCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AmberWarning) {
                                            Text(uiState.unsyncedCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Sync Pending Logs to Google Sheets",
                                        tint = AmberWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "All Logs Synced",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onOpenRecipes,
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "Repeatable Meals & Recipes",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Recipes",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Surface(
                            onClick = onOpenSettings,
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Goals & Macro Settings",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Goals",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            FloatingActionDock(
                onScanOrPhoto = onOpenCamera,
                onManualEntry = { showManualEntryDialog = true },
                onOpenSearch = { onOpenSearch(null) },
                onEndDay = { showEndDayConfirmDialog = true },
                dayScoreGrade = uiState.endOfDayReport?.aiOverview?.let { "${it.overallScore} (${it.scoreGrade})" }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "date_nav") {
                DateNavigatorBar(
                    selectedDate = uiState.selectedDate,
                    onPreviousDay = viewModel::selectPreviousDay,
                    onNextDay = viewModel::selectNextDay,
                    onToday = viewModel::selectToday
                )
            }

            item(key = "hero_card") {
                HeroCalorieAndMacroRingCard(
                    summary = uiState.summary,
                    tdee = uiState.tdeeResult,
                    hasHcNutritionSync = uiState.healthSnapshot.hasWriteNutritionPermission,
                    onOpenTdeeSettings = onOpenSettings
                )
            }

            item(key = "upf_seed_oil_row") {
                UpfAndSeedOilBentoRow(summary = uiState.summary)
            }

            item(key = "micros_card") {
                ExpandableLipidAndMicronutrientCard(
                    summary = uiState.summary,
                    expanded = isMicrosExpanded,
                    onToggleExpand = { isMicrosExpanded = !isMicrosExpanded }
                )
            }

            if (uiState.logs.isEmpty()) {
                item(key = "empty_state") {
                    EmptyDayCard(
                        onOpenCamera = onOpenCamera,
                        onOpenSearch = { onOpenSearch(null) }
                    )
                }
            } else {
                MealType.entries.forEach { mealType ->
                    val mealLogs = uiState.logs.filter { it.mealType == mealType }
                    if (mealLogs.isNotEmpty()) {
                        val isMealExpanded = mealType.name in expandedMealNames
                        item(key = "header_${mealType.name}") {
                            MealSectionHeaderAndDrillDown(
                                mealType = mealType,
                                mealLogs = mealLogs,
                                dailySummary = uiState.summary,
                                tdee = uiState.tdeeResult,
                                isExpanded = isMealExpanded,
                                onToggleExpand = {
                                    expandedMealNames = if (isMealExpanded) {
                                        expandedMealNames - mealType.name
                                    } else {
                                        expandedMealNames + mealType.name
                                    }
                                },
                                onAddFood = { onOpenSearch(mealType) }
                            )
                        }

                        items(mealLogs, key = { it.id }) { logItem ->
                            Box(modifier = Modifier.animateItem()) {
                                FoodLogItemCard(
                                    log = logItem,
                                    onClick = { onEditFoodLog(logItem.id) },
                                    onDelete = { viewModel.deleteFoodLog(logItem.id) }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "end_day_section") {
                EndDayDashboardSection(
                    report = uiState.endOfDayReport,
                    selectedAiModel = uiState.userProfile.selectedAiModel,
                    onSelectAiModel = viewModel::updateSelectedAiModel,
                    onEndDayClick = { showEndDayConfirmDialog = true },
                    onStandardEndDayClick = viewModel::confirmStandardEndDay,
                    onAiEndDayClick = { showEndDayConfirmDialog = true },
                    onViewReportClick = viewModel::openEndDayOverview
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEndDayConfirmDialog) {
        EndDayConfirmationDialog(
            loggedMealsCount = uiState.logs.size,
            summary = uiState.summary,
            tdee = uiState.tdeeResult,
            selectedAiModel = uiState.userProfile.selectedAiModel,
            onAiModelSelected = viewModel::updateSelectedAiModel,
            onConfirmStandard = {
                showEndDayConfirmDialog = false
                viewModel.confirmStandardEndDay()
            },
            onConfirmAi = {
                showEndDayConfirmDialog = false
                viewModel.confirmAiEndDay()
            },
            onDismiss = { showEndDayConfirmDialog = false }
        )
    }

    if (uiState.showEndDayOverviewModal) {
        EndOfDayOverviewDialog(
            selectedDate = uiState.selectedDate,
            isLoading = uiState.isAnalyzingEndDay,
            report = uiState.endOfDayReport,
            selectedAiModel = uiState.userProfile.selectedAiModel,
            onSelectAiModel = viewModel::updateSelectedAiModel,
            onReAnalyzeStandard = viewModel::confirmStandardEndDay,
            onReAnalyzeAi = { model -> viewModel.confirmAiEndDay(model) },
            onDismiss = viewModel::dismissEndDayOverview
        )
    }

    if (showManualEntryDialog) {
        ManualFoodEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onSave = { name, mealType, weightG, kcal, pro, carbs, fat, pufa, sat, fibre, nova, oilsNote ->
                viewModel.saveManualFoodLog(
                    name = name,
                    mealType = mealType,
                    servingWeightGrams = weightG,
                    calories = kcal,
                    proteinG = pro,
                    carbsG = carbs,
                    fatG = fat,
                    pufaG = pufa,
                    satFatG = sat,
                    fibreG = fibre,
                    novaGroup = nova,
                    ingredientsOrOilsNote = oilsNote
                )
                showManualEntryDialog = false
            }
        )
    }
}

@Composable
private fun FloatingActionDock(
    onScanOrPhoto: () -> Unit,
    onManualEntry: () -> Unit,
    onOpenSearch: () -> Unit,
    onEndDay: () -> Unit,
    dayScoreGrade: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Search Open Food Facts Database
                    Surface(
                        onClick = onOpenSearch,
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Food Database",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 2. Scan Barcode / AI Photo
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(EmeraldPrimary, CyanAccent)
                                )
                            )
                            .clickable { onScanOrPhoto() }
                            .padding(vertical = 11.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF032215),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Scan / AI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF032215),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 3. Quick Manual Numbers
                    Surface(
                        onClick = onManualEntry,
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick Manual Log",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Quick",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 4. "End Day" Button at the bottom of the screen
                Surface(
                    onClick = onEndDay,
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    EmeraldPrimary.copy(alpha = 0.55f),
                                    CyanAccent.copy(alpha = 0.45f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = "End Day",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dayScoreGrade == null) {
                                "End Day"
                            } else {
                                "End Day • AI Score $dayScoreGrade"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigatorBar(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val formatted = if (isToday) {
        "Today • ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}"
    } else {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedContent(
                    targetState = formatted,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { it / 3 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically { -it / 3 })
                    },
                    label = "date_text"
                ) { dateLabel ->
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isToday) {
                    Surface(
                        onClick = onToday,
                        color = EmeraldPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Jump to Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onNextDay,
                enabled = !isToday
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day"
                )
            }
        }
    }
}

@Composable
private fun HeroCalorieAndMacroRingCard(
    summary: DailyNutritionSummary,
    tdee: TdeeResult,
    hasHcNutritionSync: Boolean,
    onOpenTdeeSettings: () -> Unit
) {
    val consumedRaw = summary.totalCalories.roundToInt()
    val targetRaw = tdee.energyTargetKcal.coerceAtLeast(1)
    val remainingRaw = targetRaw - consumedRaw
    val isOverBudget = remainingRaw < 0

    val animatedConsumed by animateIntAsState(
        targetValue = consumedRaw,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "consumed_kcal"
    )
    val animatedRemaining by animateIntAsState(
        targetValue = kotlin.math.abs(remainingRaw),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "remaining_kcal"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = (consumedRaw.toFloat() / targetRaw.toFloat()).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "calorie_ring_progress"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        EmeraldPrimary.copy(alpha = 0.38f),
                        CyanAccent.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 650f
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar: Mode Pill + Health Connect Write Indicator
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onOpenTdeeSettings,
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Text(
                            text = "Target ${tdee.energyTargetKcal} • TDEE ${tdee.tdeeKcal} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (hasHcNutritionSync) {
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "HC Sync Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Center Radial Gauge + Side Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(148.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringBrush = if (!isOverBudget) {
                        Brush.sweepGradient(
                            listOf(EmeraldPrimary, CyanAccent, EmeraldGlow, EmeraldPrimary)
                        )
                    } else {
                        Brush.sweepGradient(
                            listOf(AmberWarning, CrimsonDanger, AmberWarning)
                        )
                    }

                    Canvas(modifier = Modifier.size(138.dp)) {
                        val strokeWidth = 13.dp.toPx()
                        val startAngle = 140f
                        val maxSweep = 260f

                        // Background Track Arc
                        drawArc(
                            color = ObsidianElevated,
                            startAngle = startAngle,
                            sweepAngle = maxSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Soft Glow Under-Arc
                        if (animatedProgress > 0.01f) {
                            drawArc(
                                brush = ringBrush,
                                startAngle = startAngle,
                                sweepAngle = maxSweep * animatedProgress,
                                useCenter = false,
                                alpha = 0.25f,
                                style = Stroke(width = strokeWidth * 1.6f, cap = StrokeCap.Round)
                            )

                            // Crisp Foreground Arc
                            drawArc(
                                brush = ringBrush,
                                startAngle = startAngle,
                                sweepAngle = maxSweep * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = animatedRemaining.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (!isOverBudget) MaterialTheme.colorScheme.onSurface else CrimsonDanger,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (!isOverBudget) "kcal left" else "kcal over",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!isOverBudget) EmeraldPrimary else CrimsonDanger,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$animatedConsumed / $targetRaw",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Right Column: Quick Stats (Stacked label + value for 100% portrait legibility)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniPill(
                        title = "Consumed Today",
                        value = "$animatedConsumed kcal",
                        accent = EmeraldPrimary
                    )
                    StatMiniPill(
                        title = "Energy Target",
                        value = "$targetRaw kcal",
                        accent = CyanAccent
                    )
                    StatMiniPill(
                        title = "Food Weight",
                        value = "${summary.totalWeightGrams.roundToInt()} g",
                        accent = IndigoAccent
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            // 3 Animated Macro Ring Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedMacroRingCard(
                    label = "Protein",
                    consumedGrams = summary.proteinG,
                    targetGrams = tdee.proteinTargetGrams,
                    color = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnimatedMacroRingCard(
                    label = "Carbs",
                    consumedGrams = summary.carbsG,
                    targetGrams = tdee.carbsTargetGrams,
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                AnimatedMacroRingCard(
                    label = "Fat",
                    consumedGrams = summary.fatG,
                    targetGrams = tdee.fatTargetGrams,
                    color = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatMiniPill(
    title: String,
    value: String,
    accent: Color
) {
    Surface(
        color = ObsidianElevated.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun AnimatedMacroRingCard(
    label: String,
    consumedGrams: Double,
    targetGrams: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val safeTarget = targetGrams.coerceAtLeast(1)
    val ratio = (consumedGrams.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "${label}_macro_ring"
    )
    val remainingG = (safeTarget - consumedGrams.roundToInt()).coerceAtLeast(0)

    Surface(
        color = ObsidianElevated.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(42.dp)) {
                    val stroke = 5.dp.toPx()
                    drawArc(
                        color = color.copy(alpha = 0.16f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedRatio,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(animatedRatio * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${consumedGrams.roundToInt()}/${safeTarget}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${remainingG}g left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun UpfAndSeedOilBentoRow(summary: DailyNutritionSummary) {
    val targetUpfColor = when {
        summary.upfCaloriePercentage <= 10.0 -> EmeraldPrimary
        summary.upfCaloriePercentage <= 25.0 -> AmberWarning
        else -> CrimsonDanger
    }
    val upfColor by animateColorAsState(targetUpfColor, label = "upf_color")

    val targetOilColor = when (summary.seedOilRiskLevel) {
        com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> EmeraldPrimary
        com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> AmberWarning
        com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> CrimsonDanger
    }
    val seedOilColor by animateColorAsState(targetOilColor, label = "oil_color")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Daily UPF % Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = upfColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (summary.upfCaloriePercentage > 20.0) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = upfColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "UPF (NOVA 4)",
                        style = MaterialTheme.typography.labelMedium,
                        color = upfColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text(
                    text = "${"%.1f".format(summary.upfCaloriePercentage)}% kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = upfColor,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${summary.upfWeightGrams.roundToInt()}g (${"%.0f".format(summary.upfWeightPercentage)}% wt) • ${summary.upfItemCount} item(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Seed Oil Radar Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = seedOilColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (summary.seedOilRiskLevel == com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = seedOilColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Seed Oil Radar",
                        style = MaterialTheme.typography.labelMedium,
                        color = seedOilColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                val oilHeadline = when (summary.seedOilRiskLevel) {
                    com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> if (summary.seedOilItemCount == 0) "Clean (0)" else "Trace (~${summary.totalSeedOilGrams.roundToInt()}g)"
                    com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> "Moderate (~${summary.totalSeedOilGrams.roundToInt()}g)"
                    com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> "High (~${summary.totalSeedOilGrams.roundToInt()}g)"
                }
                Text(
                    text = oilHeadline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = seedOilColor,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (summary.distinctSeedOilsToday.isEmpty()) {
                        "No industrial seed oils today"
                    } else {
                        "${summary.highSeedOilItemCount} high • ${summary.moderateSeedOilItemCount} mod • ${summary.traceSeedOilItemCount} trace\n${summary.distinctSeedOilsToday.joinToString(", ")}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ExpandableLipidAndMicronutrientCard(
    summary: DailyNutritionSummary,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Lipid Profile (PUFA / Sat) & Micronutrients",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PUFA ${"%.1f".format(summary.polyunsaturatedFatG)}g • Sat ${"%.1f".format(summary.saturatedFatG)}g • Fibre ${"%.1f".format(summary.fibreG)}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = ObsidianElevated,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Micronutrient Details",
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Text(
                        text = "Fatty Acids & Carbohydrates",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    MicroProgressRow("Polyunsaturated (PUFA)", summary.polyunsaturatedFatG, 15.0, "g", AmberWarning)
                    MicroProgressRow("Saturated Fat", summary.saturatedFatG, 25.0, "g", IndigoAccent)
                    MicroProgressRow("Monounsaturated (MUFA)", summary.monounsaturatedFatG, 30.0, "g", EmeraldPrimary)
                    MicroProgressRow("Dietary Fibre", summary.fibreG, 30.0, "g", EmeraldPrimary)
                    MicroProgressRow("Total Sugars", summary.sugarG, 35.0, "g", AmberWarning)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Text(
                        text = "Key Micronutrients (Daily Reference Targets)",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    MicroProgressRow("Sodium", summary.sodiumMg, 2300.0, "mg", CyanAccent)
                    MicroProgressRow("Potassium", summary.potassiumMg, 3500.0, "mg", EmeraldPrimary)
                    MicroProgressRow("Magnesium", summary.magnesiumMg, 400.0, "mg", EmeraldPrimary)
                    MicroProgressRow("Calcium", summary.calciumMg, 1000.0, "mg", CyanAccent)
                    MicroProgressRow("Iron", summary.ironMg, 14.0, "mg", IndigoAccent)
                    MicroProgressRow("Vitamin C", summary.vitaminCMg, 90.0, "mg", EmeraldPrimary)
                    MicroProgressRow("Vitamin D", summary.vitaminDUg, 20.0, "µg", AmberWarning)
                }
            }
        }
    }
}

@Composable
private fun MicroProgressRow(
    label: String,
    value: Double,
    dailyTarget: Double,
    unit: String,
    color: Color
) {
    val rawProgress = (value / dailyTarget.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = label
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                text = "${"%.1f".format(value)} / ${dailyTarget.roundToInt()} $unit",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = color,
            trackColor = ObsidianElevated,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun EmptyDayCard(
    onOpenCamera: () -> Unit,
    onOpenSearch: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
            }
            Text(
                text = "Ready to log your first meal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Search individual foods in the Open Food Facts database, scan a barcode, or take an AI photo to analyze calories, UPF classification, and seed oils.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                OutlinedButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search Foods", maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onOpenCamera,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF032215)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan / Photo", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodLogItemCard(
    log: FoodLogEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Food Name on left, Sync dot + Calories level on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (log.isSyncedToSheets) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                    }

                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${log.calories.roundToInt()} kcal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Middle Row: Clear Macro Summary
            Text(
                text = "${log.servingWeightGrams.roundToInt()}g • P ${log.proteinG.roundToInt()}g • C ${log.carbsG.roundToInt()}g • F ${log.fatG.roundToInt()}g • PUFA ${"%.1f".format(log.polyunsaturatedFatG)}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bottom Row: FlowRow of Quality Badges + Compact Edit & Delete Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallBadgePill(
                        text = log.source.displayName,
                        color = IndigoAccent
                    )
                    log.novaGroup?.let { nova ->
                        SmallBadgePill(
                            text = if (nova == 4) "NOVA 4 • UPF" else "NOVA $nova",
                            color = if (nova == 4) CrimsonDanger else EmeraldPrimary
                        )
                    }
                    if (log.containsSeedOils) {
                        val assessment = log.seedOilAssessment()
                        val pillColor = when (assessment.riskLevel) {
                            com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN -> EmeraldPrimary
                            com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER -> AmberWarning
                            com.macroai.tracker.domain.util.SeedOilRiskLevel.RED -> CrimsonDanger
                        }
                        SmallBadgePill(
                            text = assessment.label,
                            color = pillColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Meal",
                            tint = EmeraldPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallBadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualFoodEntryDialog(
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var weightG by remember { mutableStateOf("200") }
    var calories by remember { mutableStateOf("") }
    var proteinG by remember { mutableStateOf("") }
    var carbsG by remember { mutableStateOf("") }
    var fatG by remember { mutableStateOf("") }
    var pufaG by remember { mutableStateOf("0") }
    var satFatG by remember { mutableStateOf("0") }
    var fibreG by remember { mutableStateOf("0") }
    var novaGroup by remember { mutableStateOf<Int?>(1) }
    var ingredientsNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Manual Food Log", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(type.displayName, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                // 2-Column Grid for Text Boxes so labels never wrap or clip on portrait phones
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightG,
                        onValueChange = { weightG = it },
                        label = { Text("Weight (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Calories", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinG,
                        onValueChange = { proteinG = it },
                        label = { Text("Protein (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbsG,
                        onValueChange = { carbsG = it },
                        label = { Text("Carbs (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fatG,
                        onValueChange = { fatG = it },
                        label = { Text("Fat (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fibreG,
                        onValueChange = { fibreG = it },
                        label = { Text("Fibre (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pufaG,
                        onValueChange = { pufaG = it },
                        label = { Text("PUFA (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = satFatG,
                        onValueChange = { satFatG = it },
                        label = { Text("Sat Fat (g)", maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("NOVA Classification (1 = Whole, 4 = UPF)", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..4).forEach { n ->
                        FilterChip(
                            selected = novaGroup == n,
                            onClick = { novaGroup = n },
                            label = { Text("NOVA $n", maxLines = 1, softWrap = false) }
                        )
                    }
                }

                OutlinedTextField(
                    value = ingredientsNote,
                    onValueChange = { ingredientsNote = it },
                    label = { Text("Cooking Oil / Ingredients (Optional)") },
                    placeholder = { Text("e.g., cooked in butter or sunflower oil") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        mealType,
                        weightG.toDoubleOrNull() ?: 100.0,
                        calories.toDoubleOrNull() ?: 0.0,
                        proteinG.toDoubleOrNull() ?: 0.0,
                        carbsG.toDoubleOrNull() ?: 0.0,
                        fatG.toDoubleOrNull() ?: 0.0,
                        pufaG.toDoubleOrNull() ?: 0.0,
                        satFatG.toDoubleOrNull() ?: 0.0,
                        fibreG.toDoubleOrNull() ?: 0.0,
                        novaGroup,
                        ingredientsNote
                    )
                }
            ) {
                Text("Save Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MealSectionHeaderAndDrillDown(
    mealType: MealType,
    mealLogs: List<FoodLogEntity>,
    dailySummary: DailyNutritionSummary,
    tdee: TdeeResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddFood: () -> Unit
) {
    val mealSummary = remember(mealLogs) { UpfMetricsCalculator.summarizeDay(mealLogs) }
    val mealKcal = mealSummary.totalCalories.roundToInt()
    val mealPro = mealSummary.proteinG.roundToInt()
    val mealCarb = mealSummary.carbsG.roundToInt()
    val mealFat = mealSummary.fatG.roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row: Meal Name + Summary Pills + Drill-Down Toggle + Add Button
        Surface(
            color = ObsidianCard.copy(alpha = 0.78f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggleExpand() }
                .border(
                    width = 1.dp,
                    color = if (isExpanded) {
                        EmeraldPrimary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = mealType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${mealLogs.size} ${if (mealLogs.size == 1) "item" else "items"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$mealKcal kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = if (isExpanded) EmeraldPrimary.copy(alpha = 0.18f) else ObsidianElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Insights",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpanded) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse ${mealType.displayName} insights" else "Expand ${mealType.displayName} insights",
                                    tint = if (isExpanded) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onAddFood,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Search foods for ${mealType.displayName}",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Compact Macro Summary Row right in the Meal Header (similar to calories, for everything else)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealHeaderMacroBadge(label = "P", grams = mealPro, color = EmeraldPrimary)
                    MealHeaderMacroBadge(label = "C", grams = mealCarb, color = CyanAccent)
                    MealHeaderMacroBadge(label = "F", grams = mealFat, color = AmberWarning)
                    MealHeaderMacroBadge(
                        label = "Fibre",
                        grams = mealSummary.fibreG.roundToInt(),
                        color = IndigoAccent
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val shareOfDayPct = if (dailySummary.totalCalories > 0.0) {
                        ((mealSummary.totalCalories / dailySummary.totalCalories) * 100.0).roundToInt()
                    } else {
                        0
                    }
                    Text(
                        text = "$shareOfDayPct% of day • ${mealSummary.totalWeightGrams.roundToInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Expandable Drill-Down Menu for Per-Meal Insights
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically { -it / 4 },
            exit = fadeOut() + slideOutVertically { -it / 4 }
        ) {
            MealInsightsDrillDownCard(
                mealType = mealType,
                mealSummary = mealSummary,
                dailySummary = dailySummary,
                tdee = tdee
            )
        }
    }
}

@Composable
private fun MealHeaderMacroBadge(
    label: String,
    grams: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$label ${grams}g",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MealInsightsDrillDownCard(
    mealType: MealType,
    mealSummary: DailyNutritionSummary,
    dailySummary: DailyNutritionSummary,
    tdee: TdeeResult
) {
    val proKcal = mealSummary.proteinG * 4.0
    val carbKcal = mealSummary.carbsG * 4.0
    val fatKcal = mealSummary.fatG * 9.0
    val totalMacroKcal = (proKcal + carbKcal + fatKcal).coerceAtLeast(1.0)

    val proPct = ((proKcal / totalMacroKcal) * 100.0).roundToInt()
    val carbPct = ((carbKcal / totalMacroKcal) * 100.0).roundToInt()
    val fatPct = (100 - proPct - carbPct).coerceAtLeast(0)

    val proTargetShare = if (tdee.proteinTargetGrams > 0) {
        ((mealSummary.proteinG / tdee.proteinTargetGrams) * 100.0).roundToInt()
    } else 0
    val carbTargetShare = if (tdee.carbsTargetGrams > 0) {
        ((mealSummary.carbsG / tdee.carbsTargetGrams) * 100.0).roundToInt()
    } else 0
    val fatTargetShare = if (tdee.fatTargetGrams > 0) {
        ((mealSummary.fatG / tdee.fatTargetGrams) * 100.0).roundToInt()
    } else 0

    val upfColor = when {
        mealSummary.upfCaloriePercentage <= 10.0 -> EmeraldPrimary
        mealSummary.upfCaloriePercentage <= 25.0 -> AmberWarning
        else -> CrimsonDanger
    }
    val oilColor = when (mealSummary.seedOilRiskLevel) {
        SeedOilRiskLevel.GREEN -> EmeraldPrimary
        SeedOilRiskLevel.AMBER -> AmberWarning
        SeedOilRiskLevel.RED -> CrimsonDanger
    }
    val oilLabel = when (mealSummary.seedOilRiskLevel) {
        SeedOilRiskLevel.GREEN -> if (mealSummary.seedOilItemCount == 0) "Clean (0g)" else "Trace (${mealSummary.totalSeedOilGrams}g)"
        SeedOilRiskLevel.AMBER -> "Moderate (${mealSummary.totalSeedOilGrams}g)"
        SeedOilRiskLevel.RED -> "High (${mealSummary.totalSeedOilGrams}g)"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(EmeraldPrimary.copy(alpha = 0.4f), CyanAccent.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Title + Calorie Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mealType.displayName} Nutrition Breakdown",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
                val targetSharePct = if (tdee.energyTargetKcal > 0) {
                    ((mealSummary.totalCalories / tdee.energyTargetKcal) * 100.0).roundToInt()
                } else 0
                Text(
                    text = "$targetSharePct% of daily kcal target",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Stacked Macro Calorie Split Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ObsidianElevated)
                ) {
                    if (proPct > 0) {
                        Box(
                            modifier = Modifier
                                .weight(proPct.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(EmeraldPrimary)
                        )
                    }
                    if (carbPct > 0) {
                        Box(
                            modifier = Modifier
                                .weight(carbPct.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(CyanAccent)
                        )
                    }
                    if (fatPct > 0) {
                        Box(
                            modifier = Modifier
                                .weight(fatPct.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(AmberWarning)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Protein $proPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Carbs $carbPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fat $fatPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3 Macro Detail Cards for this Meal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealDrillDownMacroCard(
                    title = "Protein",
                    gramsText = "${"%.1f".format(mealSummary.proteinG)}g",
                    subline1 = "$proTargetShare% of daily goal",
                    subline2 = "${proKcal.roundToInt()} kcal",
                    accent = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MealDrillDownMacroCard(
                    title = "Carbs",
                    gramsText = "${"%.1f".format(mealSummary.carbsG)}g",
                    subline1 = "$carbTargetShare% of daily goal",
                    subline2 = "Sugar ${"%.1f".format(mealSummary.sugarG)}g • Fibre ${"%.1f".format(mealSummary.fibreG)}g",
                    accent = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                MealDrillDownMacroCard(
                    title = "Fat",
                    gramsText = "${"%.1f".format(mealSummary.fatG)}g",
                    subline1 = "$fatTargetShare% of daily goal",
                    subline2 = "Sat ${"%.1f".format(mealSummary.saturatedFatG)}g • PUFA ${"%.1f".format(mealSummary.polyunsaturatedFatG)}g",
                    accent = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Per-Meal UPF, Seed Oils & Micronutrients Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Meal UPF (NOVA 4)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${"%.0f".format(mealSummary.upfCaloriePercentage)}% kcal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = upfColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${mealSummary.upfWeightGrams.roundToInt()}g • ${mealSummary.upfItemCount} item(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Meal Seed Oils",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = oilLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = oilColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (mealSummary.distinctSeedOilsToday.isEmpty()) {
                                "No industrial seed oils"
                            } else {
                                mealSummary.distinctSeedOilsToday.joinToString(", ")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Compact Micronutrient strip for this meal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Na ${mealSummary.sodiumMg.roundToInt()}mg • K ${mealSummary.potassiumMg.roundToInt()}mg • Ca ${mealSummary.calciumMg.roundToInt()}mg • Fe ${"%.1f".format(mealSummary.ironMg)}mg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${mealSummary.totalWeightGrams.roundToInt()}g total",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoAccent,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun MealDrillDownMacroCard(
    title: String,
    gramsText: String,
    subline1: String,
    subline2: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianElevated.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = gramsText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subline1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subline2,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

