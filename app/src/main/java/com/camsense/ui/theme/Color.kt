package com.camsense.ui.theme

import androidx.compose.ui.graphics.Color

/** Charcoal lime palette (Content Guidelines). Lime means "act here"; risk colours mean risk only. */
object CamSenseColors {
    val Background = Color(0xFF111311)
    val Surface = Color(0xFF1D211D)
    val SurfaceRaised = Color(0xFF272C27)
    val Border = Color(0xFF333933)
    val Primary = Color(0xFFA3E635)
    val OnPrimary = Color(0xFF1A2605)
    val PrimaryMuted = Primary.copy(alpha = 0.15f)
    val Text = Color(0xFFF1F5EE)
    val TextMuted = Color(0xFF94A08F)

    val LowFill = Color(0xFF22C55E)
    val LowText = Color(0xFF4ADE80)
    val MediumFill = Color(0xFFF59E0B)
    val MediumText = Color(0xFFFBBF24)
    val HighFill = Color(0xFFEF4444)
    val HighText = Color(0xFFF87171)

    // Camera overlays
    val OverlayGlint = Color(0xFFEF4444)
    val OverlayIr = Color(0xFFF59E0B)
    val OverlayObject = Primary
}
