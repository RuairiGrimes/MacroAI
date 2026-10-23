package com.macroai.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Eye-friendly Obsidian & Mint-Emerald Wellness Palette
val EmeraldPrimary = Color(0xFF2CE59B)
val EmeraldGlow = Color(0xFF6EE7B7)
val EmeraldDark = Color(0xFF059669)
val AmberWarning = Color(0xFFFFB547)
val CrimsonDanger = Color(0xFFFB7185)
val IndigoAccent = Color(0xFF818CF8)
val CyanAccent = Color(0xFF38BDF8)

val ObsidianBg = Color(0xFF070B11)
val ObsidianSurface = Color(0xFF101722)
val ObsidianCard = Color(0xFF151E2D)
val ObsidianElevated = Color(0xFF1D293D)

private val DarkColors = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF032215),
    primaryContainer = Color(0xFF0B3B29),
    onPrimaryContainer = Color(0xFF86EFAC),
    secondary = CyanAccent,
    onSecondary = Color(0xFF041E2B),
    secondaryContainer = Color(0xFF0E2F44),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = AmberWarning,
    error = CrimsonDanger,
    background = ObsidianBg,
    surface = ObsidianSurface,
    surfaceVariant = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF26354D)
)

private val LightColors = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    tertiary = Color(0xFFD97706),
    error = Color(0xFFE11D48),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MacroAiTheme(
    darkTheme: Boolean = true, // Default to eye-friendly Obsidian Dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
