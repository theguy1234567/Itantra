package com.itantara.app.presentation.theme

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
    primary = Color(0xFF1F6FEB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF113D74),
    secondary = Color(0xFF3BAA83),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2F5EF),
    onSecondaryContainer = Color(0xFF174E3E),
    tertiary = Color(0xFF7A5AF8),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF152033),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF152033),
    surfaceVariant = Color(0xFFF0F4FA),
    onSurfaceVariant = Color(0xFF53627A),
    outline = Color(0xFFD9E1EC),
    outlineVariant = Color(0xFFEAF0F7),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF7A1F1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF0A1F3D),
    primaryContainer = Color(0xFF173B6F),
    onPrimaryContainer = Color(0xFFEAF2FF),
    secondary = Color(0xFF63D4AE),
    onSecondary = Color(0xFF07251B),
    secondaryContainer = Color(0xFF163F36),
    onSecondaryContainer = Color(0xFFE2F5EF),
    background = Color(0xFF0E1522),
    onBackground = Color(0xFFEAF1FF),
    surface = Color(0xFF111B2B),
    onSurface = Color(0xFFEAF1FF),
    surfaceVariant = Color(0xFF1A2333),
    onSurfaceVariant = Color(0xFFB7C4D8),
    outline = Color(0xFF2B3A52),
    outlineVariant = Color(0xFF1A2333),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun ITantraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
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
        content = content
    )
}
