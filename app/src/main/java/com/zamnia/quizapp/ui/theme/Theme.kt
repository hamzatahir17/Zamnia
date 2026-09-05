package com.zamnia.quizapp.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DefaultColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4F46E5),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF1E1B4B),
    tertiary = Color(0xFFA5B4FC),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF3730A3),
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF0F131D),
    surface = Color(0xFF1E1B4B),
    onBackground = Color(0xFFE0E7FF),
    onSurface = Color(0xFFE0E7FF),
    surfaceVariant = Color(0xFF312E81),
    onSurfaceVariant = Color(0xFFC7D2FE),
    outline = Color(0xFF6366F1)
)

private val OceanBlueColorScheme = darkColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFF7DD3FC),
    background = Color(0xFF0B132B),
    surface = Color(0xFF1C2541),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF3A506B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B)
)

private val EmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF047857),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF064E3B),
    tertiary = Color(0xFF6EE7B7),
    background = Color(0xFF022C22),
    surface = Color(0xFF064E3B),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF065F46),
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF059669)
)

private val SunsetColorScheme = darkColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB45309),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    tertiary = Color(0xFFFDE68A),
    background = Color(0xFF2E1005),
    surface = Color(0xFF451A03),
    onBackground = Color(0xFFFEF3C7),
    onSurface = Color(0xFFFEF3C7),
    surfaceVariant = Color(0xFF78350F),
    onSurfaceVariant = Color(0xFFFDE68A),
    outline = Color(0xFFD97706)
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFEC4899),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBE185D),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD946EF),
    onSecondary = Color(0xFF180828),
    tertiary = Color(0xFFA855F7),
    background = Color(0xFF180828),
    surface = Color(0xFF2E1065),
    onBackground = Color(0xFFF5D0FE),
    onSurface = Color(0xFFF5D0FE),
    surfaceVariant = Color(0xFF3B0764),
    onSurfaceVariant = Color(0xFFE9D5FF),
    outline = Color(0xFFEC4899)
)

private fun getDynamicColorScheme(themeId: String): ColorScheme {
    return when (themeId.trim().lowercase()) {
        "ocean_blue" -> OceanBlueColorScheme
        "emerald" -> EmeraldColorScheme
        "sunset" -> SunsetColorScheme
        "cyberpunk" -> CyberpunkColorScheme
        "default", "default_midnight", "midnight", "" -> DefaultColorScheme
        else -> DefaultColorScheme
    }
}

@Composable
fun ZamniaTheme(
    activeThemeId: String = "default",
    content: @Composable () -> Unit
) {
    val colorScheme = getDynamicColorScheme(activeThemeId)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
