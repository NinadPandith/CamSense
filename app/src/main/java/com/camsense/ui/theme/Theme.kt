package com.camsense.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val Scheme = darkColorScheme(
    primary = CamSenseColors.Primary,
    onPrimary = CamSenseColors.OnPrimary,
    primaryContainer = CamSenseColors.PrimaryMuted,
    onPrimaryContainer = CamSenseColors.Primary,
    secondary = CamSenseColors.Primary,
    onSecondary = CamSenseColors.OnPrimary,
    background = CamSenseColors.Background,
    onBackground = CamSenseColors.Text,
    surface = CamSenseColors.Surface,
    onSurface = CamSenseColors.Text,
    surfaceVariant = CamSenseColors.SurfaceRaised,
    onSurfaceVariant = CamSenseColors.TextMuted,
    surfaceContainer = CamSenseColors.Surface,
    surfaceContainerHigh = CamSenseColors.SurfaceRaised,
    surfaceContainerHighest = CamSenseColors.SurfaceRaised,
    surfaceContainerLow = CamSenseColors.Surface,
    outline = CamSenseColors.Border,
    outlineVariant = CamSenseColors.Border,
    error = CamSenseColors.HighText,
)

private val CamSenseShapes = Shapes(
    small = RoundedCornerShape(8.dp),    // chips
    medium = RoundedCornerShape(10.dp),  // buttons
    large = RoundedCornerShape(12.dp),   // cards
    extraLarge = RoundedCornerShape(16.dp),
)

/** Dark by default (Content Guidelines); a light theme can come later. */
@Composable
fun CamSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = CamSenseTypography, shapes = CamSenseShapes, content = content)
}

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val screen = 20.dp
}
