package com.koshpal_android.koshpalapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark Theme Colors - Pure Black Background with Blue Accent
object AppColors {
    // Background Colors
    val PureBlack = Color(0xFF000000)
    val DarkCard = Color(0xFF1C1C1E) // Floating card background
    
    // Accent Colors - Vibrant Blue
    val AccentBlue = Color(0xFF007BFF)
    val AccentBlueLight = Color(0xFF3399FF)
    val AccentBlueDark = Color(0xFF0056B3)
    
    // Text Colors - Improved Hierarchy
    val TextPrimary = Color(0xFFFFFFFF) // Main amounts and category titles
    val TextSecondary = Color(0xFFAEAEB2) // Descriptive text (TOTAL SPENDS, transaction count, budget info)
    val TextTertiary = Color(0xFF808080)
    
    // Icon Colors
    val IconPrimary = Color(0xFFFFFFFF)
    val IconSecondary = Color(0xFF007BFF)
    
    // Progress Bar Colors
    val ProgressBar = Color(0xFF007BFF)
    val ProgressBarBackground = Color(0xFF2A2A2A)
    
    // Category Icon Background
    val CategoryIconBg = Color(0xFF2A2A2A)
    
    // Dark Button Background (desaturated blue)
    val DarkButtonBg = Color(0xFF1A232E)
}

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.AccentBlue,
    onPrimary = AppColors.TextPrimary,
    primaryContainer = AppColors.AccentBlueDark,
    onPrimaryContainer = AppColors.TextPrimary,
    secondary = AppColors.AccentBlueLight,
    onSecondary = AppColors.TextPrimary,
    tertiary = AppColors.AccentBlue,
    onTertiary = AppColors.TextPrimary,
    background = AppColors.PureBlack,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.DarkCard,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.DarkCard,
    onSurfaceVariant = AppColors.TextSecondary,
    error = Color(0xFFF44336),
    onError = AppColors.TextPrimary,
    outline = AppColors.TextTertiary
)

@Composable
fun KoshpalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Always use dark theme for Categories screen
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

