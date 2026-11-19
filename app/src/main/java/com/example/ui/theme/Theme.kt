package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.model.AccentTheme

val IsaiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun IsaiStudioTheme(
    darkTheme: Boolean = true,
    accentTheme: AccentTheme = AccentTheme.CORAL,
    content: @Composable () -> Unit
) {
    val (primary, secondary, tertiary) = when (accentTheme) {
        AccentTheme.CORAL -> Triple(CoralPrimary, CoralSecondary, CoralTertiary)
        AccentTheme.CYAN -> Triple(CyanPrimary, CyanSecondary, CyanTertiary)
        AccentTheme.VIOLET -> Triple(VioletPrimary, VioletSecondary, VioletTertiary)
        AccentTheme.GOLD -> Triple(GoldPrimary, GoldSecondary, GoldTertiary)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF0A0B10),
            primaryContainer = primary.copy(alpha = 0.18f),
            onPrimaryContainer = IvoryText,
            secondary = secondary,
            onSecondary = Color(0xFF0A0B10),
            secondaryContainer = secondary.copy(alpha = 0.16f),
            onSecondaryContainer = IvoryText,
            tertiary = tertiary,
            background = ObsidianCanvas,
            onBackground = IvoryText,
            surface = ObsidianElevated,
            onSurface = IvoryText,
            surfaceVariant = ObsidianCard,
            onSurfaceVariant = SlateMuted,
            surfaceContainerHigh = ObsidianCardHigh,
            outline = SlateOutline,
            error = ErrorCrimson,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = DaylightInk,
            secondary = secondary,
            onSecondary = Color(0xFF0A0B10),
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = DaylightInk,
            tertiary = tertiary,
            background = DaylightCanvas,
            onBackground = DaylightInk,
            surface = DaylightSurface,
            onSurface = DaylightInk,
            surfaceVariant = DaylightCard,
            onSurfaceVariant = DaylightMuted,
            surfaceContainerHigh = Color(0xFFE2DFE8),
            outline = Color(0xFFD0CCD9),
            error = ErrorCrimson,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IsaiTypography,
        shapes = IsaiShapes,
        content = content
    )
}
