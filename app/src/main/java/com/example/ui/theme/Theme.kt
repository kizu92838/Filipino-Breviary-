package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkAccent,
    tertiary = DarkPrimary,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = DarkAccentLight,
    onSecondary = DarkAccentLight,
    onTertiary = DarkAccentLight,
    onBackground = DarkOnBg,
    onSurface = DarkOnBg,
    surfaceVariant = DarkSecondaryCard,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = ThemePrimary,
    secondary = ThemeAccent,
    tertiary = ThemePrimary,
    background = ThemeBg,
    surface = ThemeSurface,
    onPrimary = ThemeAccentLight,
    onSecondary = ThemeAccentLight,
    onTertiary = ThemeAccentLight,
    onBackground = ThemeOnBg,
    onSurface = ThemeOnBg,
    surfaceVariant = ThemeSecondaryCard,
    onSurfaceVariant = ThemeTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Allow turning off dynamic color to maintain the beautiful, specific Liturgical theme branding:
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content
    )
}
