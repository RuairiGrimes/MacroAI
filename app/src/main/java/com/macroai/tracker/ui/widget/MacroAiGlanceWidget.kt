package com.macroai.tracker.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.macroai.tracker.MainActivity
import kotlin.math.abs
import kotlin.math.roundToInt

private val DestinationActionKey = ActionParameters.Key<String>(MainActivity.EXTRA_DESTINATION)

class MacroAiGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataProvider.getSnapshot(context)

        provideContent {
            GlanceTheme {
                WidgetRoot(snapshot = snapshot)
            }
        }
    }
}

class MacroAiGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MacroAiGlanceWidget()
}

@androidx.compose.runtime.Composable
private fun WidgetRoot(
    snapshot: MacroAiWidgetSnapshot
) {
    val bgDark = Color(0xFF0F172A) // Slate 900
    val cardDark = Color(0xFF1E293B) // Slate 800
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)
    val emerald = Color(0xFF10B981)
    val amber = Color(0xFFF59E0B)
    val cyan = Color(0xFF38BDF8)
    val rose = Color(0xFFF43F5E)

    val openAppAction = actionStartActivity<MainActivity>()
    val scanAction = actionStartActivity<MainActivity>(
        actionParametersOf(DestinationActionKey to MainActivity.DEST_SCANNER)
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgDark))
            .cornerRadius(18.dp)
            .padding(12.dp)
            .clickable(openAppAction)
    ) {
        // --- Header Row: Brand & Status Pill ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MACROAI",
                style = TextStyle(
                    color = ColorProvider(emerald),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            val upfText = when {
                snapshot.seedOilCount > 0 -> "⚠️ Seed Oil"
                snapshot.upfPercentage > 20f -> "UPF ${snapshot.upfPercentage.roundToInt()}%"
                snapshot.caloriesConsumed > 0 -> "🌿 Clean"
                else -> "Ready"
            }
            val upfColor = when {
                snapshot.seedOilCount > 0 || snapshot.upfPercentage > 20f -> amber
                snapshot.caloriesConsumed > 0 -> emerald
                else -> textSecondary
            }

            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(cardDark))
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = upfText,
                    style = TextStyle(
                        color = ColorProvider(upfColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // --- Center Row: Calories Remaining + Quick Action Buttons ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                val calNumberText = if (snapshot.isOverBudget) {
                    "+${abs(snapshot.remainingCalories)}"
                } else {
                    "${snapshot.remainingCalories}"
                }
                val calColor = if (snapshot.isOverBudget) rose else textPrimary

                Text(
                    text = calNumberText,
                    style = TextStyle(
                        color = ColorProvider(calColor),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (snapshot.isOverBudget) "kcal over target" else "kcal remaining",
                    style = TextStyle(
                        color = ColorProvider(textSecondary),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "${snapshot.caloriesConsumed} / ${snapshot.calorieTarget} kcal",
                    style = TextStyle(
                        color = ColorProvider(textSecondary),
                        fontSize = 10.sp
                    )
                )
            }

            // Quick Scan Action Button
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(Color(0xFF047857))) // Emerald 700
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(scanAction)
            ) {
                Text(
                    text = "📷 Scan",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // --- Bottom Macro Progress Bars ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Protein
            MacroBar(
                modifier = GlanceModifier.defaultWeight(),
                label = "P",
                consumed = snapshot.proteinConsumedG.roundToInt(),
                target = snapshot.proteinTargetG.roundToInt(),
                progress = snapshot.proteinProgress,
                barColor = cyan,
                trackColor = cardDark
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Carbs
            MacroBar(
                modifier = GlanceModifier.defaultWeight(),
                label = "C",
                consumed = snapshot.carbsConsumedG.roundToInt(),
                target = snapshot.carbsTargetG.roundToInt(),
                progress = snapshot.carbsProgress,
                barColor = amber,
                trackColor = cardDark
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Fat
            MacroBar(
                modifier = GlanceModifier.defaultWeight(),
                label = "F",
                consumed = snapshot.fatConsumedG.roundToInt(),
                target = snapshot.fatTargetG.roundToInt(),
                progress = snapshot.fatProgress,
                barColor = rose,
                trackColor = cardDark
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun MacroBar(
    modifier: GlanceModifier,
    label: String,
    consumed: Int,
    target: Int,
    progress: Float,
    barColor: Color,
    trackColor: Color
) {
    Column(modifier = modifier) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "$label: $consumed",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFF1F5F9)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "/${target}g",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF94A3B8)),
                    fontSize = 9.sp
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(3.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = ColorProvider(barColor),
            backgroundColor = ColorProvider(trackColor)
        )
    }
}
