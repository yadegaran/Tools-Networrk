package com.tools.net.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF02316B),
    primaryContainer = Color(0xFF1E3A66),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = BrandCyan,
    tertiary = BrandPurple,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    outline = Color(0xFF3A4252)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0A3A8C),
    secondary = BrandCyan,
    tertiary = BrandPurple,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    outline = Color(0xFFD3D9E3)
)

/** آیا تم فعلی تاریک است؛ بر اساس انتخاب کاربر یا تنظیمات سیستم. */
@Composable
fun AppThemeMode.resolveIsDark(): Boolean = when (this) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}

@Composable
fun CleanIpCloudTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.resolveIsDark()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
