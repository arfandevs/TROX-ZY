package com.troxzy.xploit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TroxzyDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = AmoledBlack,
    primaryContainer = NeonPurpleDim,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = NeonCyanDim,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonGreen,
    onTertiary = AmoledBlack,
    tertiaryContainer = NeonGreenDim,
    onTertiaryContainer = TextPrimary,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = NeonCyanDim,
    outlineVariant = DarkElevated,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRed.copy(alpha = 0.3f),
    onErrorContainer = TextPrimary,
)

@Composable
fun TroxzyXploitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TroxzyDarkColorScheme,
        typography = TroxzyTypography,
        content = content
    )
}
