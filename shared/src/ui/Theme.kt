@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package id.andreasmlbngaol.nas_project.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A light-blue / white palette generated with Material Color Utilities (the
 * `vibrant` variant) from a `#1A73E8` seed — cool, technical, and still vivid
 * enough to give the containers real presence.
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF005BC0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF004493),
    secondary = Color(0xFF585C7E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE0FF),
    onSecondaryContainer = Color(0xFF414465),
    tertiary = Color(0xFF615789),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE7DEFF),
    onTertiaryContainer = Color(0xFF494070),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF181C25),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF181C25),
    surfaceVariant = Color(0xFFDEE2F2),
    onSurfaceVariant = Color(0xFF424753),
    outline = Color(0xFF727785),
    outlineVariant = Color(0xFFC2C6D6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FF),
    surfaceContainer = Color(0xFFEBEDFA),
    surfaceContainerHigh = Color(0xFFE5E8F5),
    surfaceContainerHighest = Color(0xFFDFE2EF),
    surfaceBright = Color(0xFFF9F9FF),
    surfaceDim = Color(0xFFD7D9E6),
    inverseSurface = Color(0xFF2C303A),
    inverseOnSurface = Color(0xFFEEF0FD),
    inversePrimary = Color(0xFFADC7FF),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFADC7FF),
    onPrimary = Color(0xFF002E68),
    primaryContainer = Color(0xFF004493),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFC1C4EB),
    onSecondary = Color(0xFF2A2E4D),
    secondaryContainer = Color(0xFF414465),
    onSecondaryContainer = Color(0xFFDFE0FF),
    tertiary = Color(0xFFCBBFF8),
    onTertiary = Color(0xFF332958),
    tertiaryContainer = Color(0xFF494070),
    onTertiaryContainer = Color(0xFFE7DEFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F131C),
    onBackground = Color(0xFFDFE2EF),
    surface = Color(0xFF0F131C),
    onSurface = Color(0xFFDFE2EF),
    surfaceVariant = Color(0xFF424753),
    onSurfaceVariant = Color(0xFFC2C6D6),
    outline = Color(0xFF8C909F),
    outlineVariant = Color(0xFF424753),
    surfaceContainerLowest = Color(0xFF0A0E17),
    surfaceContainerLow = Color(0xFF181C25),
    surfaceContainer = Color(0xFF1C2029),
    surfaceContainerHigh = Color(0xFF262A33),
    surfaceContainerHighest = Color(0xFF31353F),
    surfaceBright = Color(0xFF353943),
    surfaceDim = Color(0xFF0F131C),
    inverseSurface = Color(0xFFDFE2EF),
    inverseOnSurface = Color(0xFF2C303A),
    inversePrimary = Color(0xFF005BC0),
)

/** Expressive shape scale — larger, rounder radii than the baseline. */
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun NasTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        content = content,
    )
}
