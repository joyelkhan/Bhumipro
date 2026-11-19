package com.example.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = BhumiEmerald,
    onPrimary = Color.White,
    primaryContainer = BhumiEmeraldContainer,
    onPrimaryContainer = BhumiEmerald,
    secondary = BhumiBlue,
    onSecondary = Color.White,
    secondaryContainer = BhumiBlueContainer,
    onSecondaryContainer = BhumiBlue,
    tertiary = BhumiAmber,
    tertiaryContainer = BhumiAmberContainer,
    background = BhumiSurfaceLight,
    onBackground = BhumiInk,
    surface = BhumiCardLight,
    onSurface = BhumiInk,
    surfaceVariant = Color(0xFFEEF2F0),
    onSurfaceVariant = BhumiInkSecondary,
    outline = BhumiOutline,
    error = BhumiRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF06281C),
    primaryContainer = Color(0xFF0D4A38),
    onPrimaryContainer = Color(0xFFD9F3E4),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF0B1F44),
    background = BhumiDarkSurface,
    onBackground = BhumiDarkInk,
    surface = BhumiDarkCard,
    onSurface = BhumiDarkInk,
    surfaceVariant = Color(0xFF23322D),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171)
)

@Composable
fun BhumiProTheme(
    themeMode: String = "লাইট",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "ডার্ক" -> true
        "লাইট" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BhumiTypography,
        content = content
    )
}
