package com.koshpal_android.koshpalapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Koshpal Brand Colors - Exact from HTML design
object KoshpalColors {
    // Primary Brand Color - Account screen blue
    val Primary = Color(0xFF0052FF) // #0052FF - from HTML design
    val LegacyPrimary = Color(0xFF3B59BA) // #3B59BA - original

    // Light Theme Colors
    val LightBackground = Color(0xFFF8FAFC) // #F8FAFC
    val LightSurface = Color(0xFFFFFFFF) // White
    val LightSurfaceCard = Color.White
    val LightOnSurface = Color(0xFF0F1115) // Dark text
    val LightOnSurfaceVariant = Color(0xFF64748B) // Muted text
    val LightOutline = Color(0xFFE2E8F0) // Light borders

    // Dark Theme Colors
    val DarkBackground = Color(0xFF0F1115) // #0F1115
    val DarkSurface = Color(0xFF1A1D24) // #1A1D24
    val DarkSurfaceCard = Color(0xFF1A1D24)
    val DarkOnSurface = Color.White
    val DarkOnSurfaceVariant = Color(0xFF94A3B8) // Muted text
    val DarkOutline = Color(0xFF334155) // Dark borders

    // Common Colors
    val PrimaryContainer = Color(0xFF3B59BA).copy(alpha = 0.1f)
    val OnPrimary = Color.White
    val Error = Color(0xFFEF4444)
    val OnError = Color.White

    // Legacy AppColors for backward compatibility
    val AccentBlue = Primary
    val AccentBlueLight = Primary
    val AccentBlueDark = Primary.copy(alpha = 0.8f)
    val PureBlack = DarkBackground
    val DarkCard = DarkSurface
    val TextPrimary = DarkOnSurface
    val TextSecondary = DarkOnSurfaceVariant
    val TextTertiary = DarkOutline
    val IconPrimary = DarkOnSurface
    val IconSecondary = Primary
    val ProgressBar = Primary
    val ProgressBarBackground = Color(0xFF2A2A2A)
    val CategoryIconBg = Color(0xFF2A2A2A)
    val DarkButtonBg = Color(0xFF1A232E)
}

// Backward compatibility object
object AppColors {
    val AccentBlue get() = KoshpalColors.AccentBlue
    val AccentBlueLight get() = KoshpalColors.AccentBlueLight
    val AccentBlueDark get() = KoshpalColors.AccentBlueDark
    val PureBlack get() = KoshpalColors.PureBlack
    val DarkCard get() = KoshpalColors.DarkCard
    val TextPrimary get() = KoshpalColors.TextPrimary
    val TextSecondary get() = KoshpalColors.TextSecondary
    val TextTertiary get() = KoshpalColors.TextTertiary
    val IconPrimary get() = KoshpalColors.IconPrimary
    val IconSecondary get() = KoshpalColors.IconSecondary
    val ProgressBar get() = KoshpalColors.ProgressBar
    val ProgressBarBackground get() = KoshpalColors.ProgressBarBackground
    val CategoryIconBg get() = KoshpalColors.CategoryIconBg
    val DarkButtonBg get() = KoshpalColors.DarkButtonBg
}

private val LightColorScheme = lightColorScheme(
    primary = KoshpalColors.Primary,
    onPrimary = KoshpalColors.OnPrimary,
    primaryContainer = KoshpalColors.PrimaryContainer,
    onPrimaryContainer = KoshpalColors.Primary,
    secondary = KoshpalColors.Primary,
    onSecondary = KoshpalColors.OnPrimary,
    tertiary = KoshpalColors.Primary,
    onTertiary = KoshpalColors.OnPrimary,
    background = KoshpalColors.LightBackground,
    onBackground = KoshpalColors.LightOnSurface,
    surface = KoshpalColors.LightSurface,
    onSurface = KoshpalColors.LightOnSurface,
    surfaceVariant = KoshpalColors.LightSurfaceCard,
    onSurfaceVariant = KoshpalColors.LightOnSurfaceVariant,
    error = KoshpalColors.Error,
    onError = KoshpalColors.OnError,
    outline = KoshpalColors.LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = KoshpalColors.Primary,
    onPrimary = KoshpalColors.OnPrimary,
    primaryContainer = KoshpalColors.PrimaryContainer,
    onPrimaryContainer = KoshpalColors.Primary,
    secondary = KoshpalColors.Primary,
    onSecondary = KoshpalColors.OnPrimary,
    tertiary = KoshpalColors.Primary,
    onTertiary = KoshpalColors.OnPrimary,
    background = KoshpalColors.DarkBackground,
    onBackground = KoshpalColors.DarkOnSurface,
    surface = KoshpalColors.DarkSurface,
    onSurface = KoshpalColors.DarkOnSurface,
    surfaceVariant = KoshpalColors.DarkSurfaceCard,
    onSurfaceVariant = KoshpalColors.DarkOnSurfaceVariant,
    error = KoshpalColors.Error,
    onError = KoshpalColors.OnError,
    outline = KoshpalColors.DarkOutline
)

@Composable
fun KoshpalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

