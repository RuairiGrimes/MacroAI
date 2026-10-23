package com.macroai.tracker.ui.camera

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.ui.theme.AmberWarning
import com.macroai.tracker.ui.theme.CrimsonDanger
import com.macroai.tracker.ui.theme.CyanAccent
import com.macroai.tracker.ui.theme.EmeraldPrimary
import com.macroai.tracker.ui.theme.ObsidianCard
import com.macroai.tracker.ui.theme.ObsidianElevated
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MealAnalysisAndReviewScreen(
    draft: ReviewDraftState,
    capturedBitmap: Bitmap?,
    selectedMealType: MealType,
    onMealTypeSelected: (MealType) -> Unit,
    onContextPromptChanged: (String) -> Unit,
    onAnalyzePhotoClicked: () -> Unit,
    onBarcodeGramsChanged: (String) -> Unit,
    onAiMealNameChanged: (String) -> Unit,
    onAiIngredientWeightChanged: (Int, Double) -> Unit,
    onConfirmSave: () -> Unit,
    onCancel: () -> Unit,
    onStartSwapIngredient: (Int) -> Unit = {},
    onRemoveAiIngredient: (Int) -> Unit = {},
    onBarcodeServingCountChanged: (String) -> Unit = {},
    onBarcodeToggleServingMode: (Boolean) -> Unit = {},
    onBarcodeSelectServingPreset: (Double, Double?, Boolean) -> Unit = { _, _, _ -> },
    selectedAiModel: com.macroai.tracker.domain.model.AiModel = com.macroai.tracker.domain.model.AiModel.DEFAULT,
    onAiModelSelected: (com.macroai.tracker.domain.model.AiModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (draft) {
        is ReviewDraftState.Idle -> Unit

        is ReviewDraftState.AnalyzingLoading -> {
            FullScreenAnalyzingView(
                statusMessage = draft.statusMessage,
                capturedBitmap = capturedBitmap,
                onCancel = onCancel,
                modifier = modifier
            )
        }

        else -> {
            val scrollState = rememberScrollState()
            val isImeVisible = WindowInsets.isImeVisible
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()
            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(isImeVisible) {
                if (isImeVisible) {
                    bringIntoViewRequester.bringIntoView()
                }
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (draft) {
                    is ReviewDraftState.PhotoContextPrompt -> {
                        // Header
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "AI Meal Context",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "Camera is paused. Add optional cooking details (oils, weights, or hidden items) so ${selectedAiModel.displayName} can calculate precise macros.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Responsive Hero vs Compact Photo Banner
                        AnimatedContent(
                            targetState = isImeVisible,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                            },
                            label = "photo_preview_compact_toggle"
                        ) { imeOpen ->
                            if (imeOpen) {
                                Surface(
                                    color = ObsidianCard,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { keyboardController?.hide() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Image(
                                            bitmap = draft.bitmap.asImageBitmap(),
                                            contentDescription = "Captured meal thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    1.dp,
                                                    EmeraldPrimary.copy(alpha = 0.6f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Meal Photo Attached",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary
                                                )
                                            }
                                            Text(
                                                text = "Keyboard open • Tap to dismiss keyboard",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Image(
                                    bitmap = draft.bitmap.asImageBitmap(),
                                    contentDescription = "Captured meal photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(
                                            width = 1.dp,
                                            color = EmeraldPrimary.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                )
                            }
                        }

                        // Meal Type Selector Row
                        MealTypeSelectorRow(
                            selected = selectedMealType,
                            onSelect = onMealTypeSelected
                        )

                        // AI Food Prompt Box with Auto-scroll and Quick Suggestions
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                        ) {
                            OutlinedTextField(
                                value = draft.userContextPrompt,
                                onValueChange = onContextPromptChanged,
                                label = { Text("Cooking details, weights, or brands (Optional)") },
                                placeholder = { Text("e.g., cooked in 1 tbsp olive oil, ~200g ribeye, rice") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusEvent { focusState ->
                                        if (focusState.isFocused) {
                                            coroutineScope.launch {
                                                bringIntoViewRequester.bringIntoView()
                                            }
                                        }
                                    },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (draft.userContextPrompt.isNotEmpty()) {
                                        IconButton(onClick = { onContextPromptChanged("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear text",
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

                            // Quick context suggestion chips for rapid 1-tap addition
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val suggestions = listOf(
                                    "+ 1 tbsp Olive Oil",
                                    "+ Cooked in Butter",
                                    "+ ~150g Portion",
                                    "+ ~250g Portion",
                                    "+ No Seed Oils"
                                )
                                suggestions.forEach { suggestion ->
                                    val chipText = suggestion.removePrefix("+ ")
                                    SuggestionChip(
                                        onClick = {
                                            val current = draft.userContextPrompt.trim()
                                            val updated = if (current.isEmpty()) {
                                                chipText
                                            } else if (current.endsWith(",") || current.endsWith(".")) {
                                                "$current $chipText"
                                            } else {
                                                "$current, $chipText"
                                            }
                                            onContextPromptChanged(updated)
                                        },
                                        label = {
                                            Text(
                                                text = suggestion,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = ObsidianElevated,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        )
                                    )
                                }
                            }
                        }

                        // AI Model Selector (3.1 Flash Lite vs 3.5 Flash Lite) to avoid rate limits
                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Model (Switch if rate-limited)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    com.macroai.tracker.domain.model.AiModel.entries.forEach { model ->
                                        val isSelected = selectedAiModel == model
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onAiModelSelected(model) },
                                            label = {
                                                Text(
                                                    text = model.shortName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons: Retake vs Analyze
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(bringIntoViewRequester),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onCancel()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Retake Photo", maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    onAnalyzePhotoClicked()
                                },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color(0xFF032215)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyze Meal",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    is ReviewDraftState.BarcodeReview -> {
                        val grams = draft.consumedGramsInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 100.0
                        val previewEntity = draft.product.scaleToEntity(
                            servingWeightGrams = grams,
                            mealType = selectedMealType
                        )
                        val seedOilAssessment = draft.product.seedOilAssessment(grams)

                        Text(
                            text = draft.product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        QualityAlertBanner(
                            novaGroup = draft.product.novaGroup,
                            containsSeedOils = draft.product.containsSeedOils,
                            detectedSeedOilsCsv = draft.product.detectedSeedOilsCsv,
                            seedOilAssessment = seedOilAssessment
                        )

                        MealTypeSelectorRow(
                            selected = selectedMealType,
                            onSelect = onMealTypeSelected
                        )

                        val explicitServingGrams = draft.product.servingQuantityGrams?.takeIf { it > 0.0 }
                            ?: draft.product.defaultServingGrams.takeIf { it > 0.0 && it != 100.0 }
                        val servingLabel = draft.product.servingSizeName?.takeIf { it.isNotBlank() }
                            ?: if (explicitServingGrams != null) "${explicitServingGrams.roundToInt()}g serving" else null

                        // Serving Size Choice Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (explicitServingGrams != null) {
                                FilterChip(
                                    selected = draft.isServingMode,
                                    onClick = { onBarcodeToggleServingMode(true) },
                                    label = {
                                        Text(
                                            text = "1 Serving (${servingLabel ?: "${explicitServingGrams.roundToInt()}g"})",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    leadingIcon = if (draft.isServingMode) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                            FilterChip(
                                selected = !draft.isServingMode && grams != 100.0,
                                onClick = { onBarcodeToggleServingMode(false) },
                                label = { Text("Custom Grams (g)", maxLines = 1, softWrap = false) }
                            )
                            FilterChip(
                                selected = !draft.isServingMode && grams == 100.0,
                                onClick = { onBarcodeSelectServingPreset(100.0, 1.0, false) },
                                label = { Text("100g Portion", maxLines = 1, softWrap = false) }
                            )
                        }

                        // Servings Stepper if Serving Mode is active
                        if (draft.isServingMode && explicitServingGrams != null) {
                            Surface(
                                color = ObsidianCard,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Number of Servings",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "1 srv = ${explicitServingGrams.roundToInt()}g • Total = ${grams.roundToInt()}g",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val current = draft.servingCountInput.toDoubleOrNull() ?: 1.0
                                                val next = (current - 0.5).coerceAtLeast(0.5)
                                                val str = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                                                onBarcodeServingCountChanged(str)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                        }
                                        OutlinedTextField(
                                            value = draft.servingCountInput,
                                            onValueChange = onBarcodeServingCountChanged,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.width(68.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                                        )
                                        IconButton(
                                            onClick = {
                                                val current = draft.servingCountInput.toDoubleOrNull() ?: 1.0
                                                val next = current + 0.5
                                                val str = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                                                onBarcodeServingCountChanged(str)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase")
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = draft.consumedGramsInput,
                            onValueChange = onBarcodeGramsChanged,
                            label = { Text("Consumed Weight (grams)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text("g") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        MacroSummaryPreviewCard(
                            calories = previewEntity.calories,
                            proteinG = previewEntity.proteinG,
                            carbsG = previewEntity.carbsG,
                            fatG = previewEntity.fatG,
                            pufaG = previewEntity.polyunsaturatedFatG,
                            satFatG = previewEntity.saturatedFatG,
                            fibreG = previewEntity.fibreG
                        )

                        if (draft.product.ingredientsText.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ingredients: ${draft.product.ingredientsText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Scan Another", maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = onConfirmSave,
                                modifier = Modifier.weight(1.4f)
                            ) {
                                val saveText = if (draft.isServingMode && explicitServingGrams != null) {
                                    "Save (${draft.servingCountInput} srv • ${grams.roundToInt()}g)"
                                } else {
                                    "Save (${grams.roundToInt()}g)"
                                }
                                Text(
                                    text = saveText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    is ReviewDraftState.AiMealReview -> {
                        val analysis = draft.analysis
                        val previewEntity = analysis.toFoodLogEntity(
                            mealType = selectedMealType,
                            userContextPrompt = draft.userContextPrompt
                        )
                        val seedOilAssessment = previewEntity.seedOilAssessment()

                        Text(
                            text = "Review Meal Estimate",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }

                        OutlinedTextField(
                            value = analysis.name,
                            onValueChange = onAiMealNameChanged,
                            label = { Text("Meal Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        QualityAlertBanner(
                            novaGroup = previewEntity.novaGroup,
                            containsSeedOils = previewEntity.containsSeedOils,
                            detectedSeedOilsCsv = previewEntity.detectedSeedOils,
                            seedOilAssessment = seedOilAssessment
                        )

                        MealTypeSelectorRow(
                            selected = selectedMealType,
                            onSelect = onMealTypeSelected
                        )

                        MacroSummaryPreviewCard(
                            calories = previewEntity.calories,
                            proteinG = previewEntity.proteinG,
                            carbsG = previewEntity.carbsG,
                            fatG = previewEntity.fatG,
                            pufaG = previewEntity.polyunsaturatedFatG,
                            satFatG = previewEntity.saturatedFatG,
                            fibreG = previewEntity.fibreG
                        )

                        if (analysis.aiConfidenceReasoning.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Clinical Reasoning",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = analysis.aiConfidenceReasoning,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        if (analysis.ingredientBreakdown.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ingredient Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap 'Swap' to replace guessed items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            analysis.ingredientBreakdown.forEachIndexed { index, item ->
                                var weightText by remember(item.name, item.estimatedWeightGrams) {
                                    mutableStateOf(item.estimatedWeightGrams.roundToInt().toString())
                                }
                                Surface(
                                    color = ObsidianCard,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "P ${item.proteinG.roundToInt()}g • C ${item.carbsG.roundToInt()}g • F ${item.fatG.roundToInt()}g",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${item.calories.roundToInt()} kcal",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onStartSwapIngredient(index) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = "Swap ingredient",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Swap",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldPrimary
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onRemoveAiIngredient(index) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove ingredient",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val current = weightText.toDoubleOrNull() ?: item.estimatedWeightGrams
                                                        val next = (current - 10.0).coerceAtLeast(5.0)
                                                        weightText = next.roundToInt().toString()
                                                        onAiIngredientWeightChanged(index, next)
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrease 10g", modifier = Modifier.size(18.dp))
                                                }

                                                OutlinedTextField(
                                                    value = weightText,
                                                    onValueChange = { newVal ->
                                                        weightText = newVal
                                                        newVal.toDoubleOrNull()?.let { validGrams ->
                                                            if (validGrams > 0.0) {
                                                                onAiIngredientWeightChanged(index, validGrams)
                                                            }
                                                        }
                                                    },
                                                    label = { Text("Grams", maxLines = 1, softWrap = false) },
                                                    suffix = { Text("g") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.width(96.dp)
                                                )

                                                IconButton(
                                                    onClick = {
                                                        val current = weightText.toDoubleOrNull() ?: item.estimatedWeightGrams
                                                        val next = current + 10.0
                                                        weightText = next.roundToInt().toString()
                                                        onAiIngredientWeightChanged(index, next)
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increase 10g", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discard", maxLines = 1, softWrap = false)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Brush.horizontalGradient(listOf(EmeraldPrimary, CyanAccent)))
                                    .clickable { onConfirmSave() }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Save Meal (${previewEntity.servingWeightGrams.roundToInt()}g)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF032215),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    else -> Unit
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FullScreenAnalyzingView(
    statusMessage: String,
    capturedBitmap: Bitmap?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital_loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (capturedBitmap != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(210.dp)
            ) {
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(176.dp)
                        .clip(CircleShape)
                )

                Canvas(modifier = Modifier.size(204.dp)) {
                    val stroke = 5.dp.toPx()
                    drawArc(
                        color = ObsidianElevated,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, EmeraldPrimary, CyanAccent)
                        ),
                        startAngle = rotation,
                        sweepAngle = 220f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    val stroke = 6.dp.toPx()
                    drawArc(
                        color = ObsidianElevated,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, EmeraldPrimary, CyanAccent)
                        ),
                        startAngle = rotation,
                        sweepAngle = 220f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Analyzing Nutrition Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealTypeSelectorRow(
    selected: MealType,
    onSelect: (MealType) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MealType.entries.forEach { mealType ->
            FilterChip(
                selected = selected == mealType,
                onClick = { onSelect(mealType) },
                label = { Text(mealType.displayName, maxLines = 1, softWrap = false) }
            )
        }
    }
}

@Composable
private fun QualityAlertBanner(
    novaGroup: Int?,
    containsSeedOils: Boolean,
    detectedSeedOilsCsv: String,
    seedOilAssessment: com.macroai.tracker.domain.util.SeedOilDetectionResult? = null
) {
    val novaColor = when (novaGroup) {
        1, 2 -> EmeraldPrimary
        3 -> AmberWarning
        4 -> CrimsonDanger
        else -> MaterialTheme.colorScheme.outline
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
        "Seed Oils: ${detectedSeedOilsCsv.ifBlank { "Detected" }}"
    } else {
        "Seed-Oil Free"
    }

    val shouldStack = riskLevel != com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN || novaGroup == 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (shouldStack) {
            // Stack vertically when seed oils are moderate/high or food is UPF
            Surface(
                color = novaColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (novaGroup == 4) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = novaColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (novaGroup != null) {
                            if (novaGroup == 4) "NOVA 4 (Ultra-Processed Food)" else "NOVA Group $novaGroup"
                        } else {
                            "NOVA Unclassified"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = novaColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Surface(
                color = oilColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = oilIcon,
                        contentDescription = null,
                        tint = oilColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (detectedSeedOilsCsv.isNotBlank() && riskLevel != com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN) {
                            "$oilLabel • $detectedSeedOilsCsv"
                        } else {
                            oilLabel
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = oilColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = novaColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (novaGroup == 4) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = novaColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (novaGroup != null) {
                                if (novaGroup == 4) "NOVA 4 (UPF)" else "NOVA $novaGroup"
                            } else {
                                "NOVA Unclassified"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = novaColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = oilColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = oilIcon,
                            contentDescription = null,
                            tint = oilColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = oilLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = oilColor,
                            fontWeight = FontWeight.Bold,
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
private fun MacroSummaryPreviewCard(
    calories: Double,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    pufaG: Double,
    satFatG: Double,
    fibreG: Double
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroMetricItem("Calories", "${calories.roundToInt()} kcal", EmeraldPrimary, Modifier.weight(1f))
                MacroMetricItem("Protein", "${proteinG.roundToInt()}g", EmeraldPrimary, Modifier.weight(1f))
                MacroMetricItem("Carbs", "${carbsG.roundToInt()}g", CyanAccent, Modifier.weight(1f))
                MacroMetricItem("Fat", "${fatG.roundToInt()}g", AmberWarning, Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PUFA: ${"%.1f".format(pufaG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Sat Fat: ${"%.1f".format(satFatG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Fibre: ${"%.1f".format(fibreG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun MacroMetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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
