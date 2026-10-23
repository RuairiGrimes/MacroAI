package com.macroai.tracker.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.domain.model.MealIngredient
import com.macroai.tracker.domain.model.ServingOption
import com.macroai.tracker.domain.model.ServingUnitType
import com.macroai.tracker.domain.model.toMealIngredient
import com.macroai.tracker.ui.theme.AmberWarning
import com.macroai.tracker.ui.theme.CrimsonDanger
import com.macroai.tracker.ui.theme.CyanAccent
import com.macroai.tracker.ui.theme.EmeraldPrimary
import com.macroai.tracker.ui.theme.IndigoAccent
import com.macroai.tracker.ui.theme.ObsidianCard
import com.macroai.tracker.ui.theme.ObsidianElevated
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientPortionSelectorDialog(
    product: NormalizedOffProduct,
    onConfirm: (MealIngredient) -> Unit,
    onDismiss: () -> Unit
) {
    val servingOptions = remember(product) { ServingOption.optionsForProduct(product) }

    // Default to the explicit package serving if available, otherwise Grams
    val initialOption = remember(servingOptions) {
        servingOptions.firstOrNull { it.type == ServingUnitType.SERVING }
            ?: servingOptions.firstOrNull { it.type == ServingUnitType.GRAMS }
            ?: servingOptions.first()
    }

    var selectedOption by remember(product) { mutableStateOf(initialOption) }
    var quantityInput by remember(selectedOption) {
        val defaultQty = if (selectedOption.type == ServingUnitType.GRAMS) {
            product.defaultServingGrams.roundToInt().toString()
        } else {
            "1"
        }
        mutableStateOf(defaultQty)
    }

    val parsedQuantity = quantityInput.toDoubleOrNull() ?: 1.0
    val totalGrams = selectedOption.calculateTotalGrams(parsedQuantity).coerceAtLeast(1.0)
    val scaledIngredient = remember(product, totalGrams) { product.toMealIngredient(totalGrams) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!product.brand.isNullOrBlank()) {
                            Text(
                                text = product.brand,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Serving Unit Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Serving Unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        servingOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedOption == opt,
                                onClick = {
                                    selectedOption = opt
                                    quantityInput = if (opt.type == ServingUnitType.GRAMS) {
                                        product.defaultServingGrams.roundToInt().toString()
                                    } else {
                                        "1"
                                    }
                                },
                                label = { Text(opt.label, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                // Quantity Stepper & Text Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val curr = parsedQuantity
                            val step = if (selectedOption.type == ServingUnitType.GRAMS) 25.0 else 0.5
                            val next = (curr - step).coerceAtLeast(if (selectedOption.type == ServingUnitType.GRAMS) 5.0 else 0.25)
                            quantityInput = if (next % 1.0 == 0.0) next.toInt().toString() else "%.1f".format(next)
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Quantity (${selectedOption.type.shortName})", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val curr = parsedQuantity
                            val step = if (selectedOption.type == ServingUnitType.GRAMS) 25.0 else 0.5
                            val next = curr + step
                            quantityInput = if (next % 1.0 == 0.0) next.toInt().toString() else "%.1f".format(next)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // Live Nutrition Breakdown for this Serving
                Surface(
                    color = ObsidianElevated,
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Portion: ${totalGrams.roundToInt()}g",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${scaledIngredient.calories.roundToInt()} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("P: ${scaledIngredient.proteinG.roundToInt()}g", color = CyanAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                            Text("C: ${scaledIngredient.carbsG.roundToInt()}g", color = AmberWarning, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                            Text("F: ${scaledIngredient.fatG.roundToInt()}g", color = IndigoAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                            if (scaledIngredient.fibreG > 0) {
                                Text("Fib: ${"%.1f".format(scaledIngredient.fibreG)}g", color = EmeraldPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            scaledIngredient.novaGroup?.let { nova ->
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

                            if (scaledIngredient.containsSeedOils) {
                                Surface(
                                    color = CrimsonDanger.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Seed Oils",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonDanger,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Button
                Button(
                    onClick = {
                        onConfirm(scaledIngredient)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color(0xFF032215)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add to Recipe (${totalGrams.roundToInt()}g)", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

