package com.tokoreader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkBlue,
    secondary = CyberpunkPink,
    tertiary = CyberpunkYellow,
    background = CyberpunkDark,
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    error = CyberpunkPink
)

private val DarkNavyColorScheme = darkColorScheme(
    primary = NavyAccent,
    secondary = CyberpunkBlue,
    tertiary = SuccessGreen,
    background = NavyDark,
    surface = NavyLight,
    surfaceVariant = Color(0xFF24365D),
    onPrimary = Color.White,
    onSecondary = NavyText,
    onBackground = NavyText,
    onSurface = NavyText,
    error = ErrorRed
)

private val OledColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF818CF8),
    tertiary = SuccessGreen,
    background = Color(0xFF000000),
    surface = Color(0xFF0B0E14),
    surfaceVariant = Color(0xFF181D26),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF0D9488),
    tertiary = Color(0xFF16A34A),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = Color(0xFFDC2626)
)

fun resolveAccentColor(name: String): Color {
    return when (name) {
        "Neon Cyan" -> Color(0xFF06B6D4)
        "Cyber Gold" -> Color(0xFFF59E0B)
        "Emerald Green" -> Color(0xFF10B981)
        "Vibrant Purple" -> Color(0xFFA855F7)
        "Sunset Orange" -> Color(0xFFF97316)
        else -> Color(0xFF38BDF8) // Electric Blue
    }
}

@Composable
fun TokoReaderTheme(
    themeMode: String = "Dark Navy", // Support "Cyberpunk", "Dark Navy", "OLED", "Light"
    accentColor: String = "Electric Blue",
    content: @Composable () -> Unit
) {
    val baseScheme = when (themeMode) {
        "Cyberpunk" -> CyberpunkColorScheme
        "Dark Navy" -> DarkNavyColorScheme
        "OLED" -> OledColorScheme
        "Light" -> LightColorScheme
        else -> DarkNavyColorScheme
    }

    val customAccent = resolveAccentColor(accentColor)
    val finalColorScheme = baseScheme.copy(
        primary = customAccent
    )

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}
