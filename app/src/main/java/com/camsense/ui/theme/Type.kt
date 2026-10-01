@file:OptIn(ExperimentalTextApi::class)

package com.camsense.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.camsense.R

// Bundled variable fonts (res/font) so the app stays offline. SIL Open Font License.
private fun variable(res: Int, weight: FontWeight) =
    Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val SpaceGrotesk = FontFamily(
    variable(R.font.space_grotesk, FontWeight.SemiBold),
    variable(R.font.space_grotesk, FontWeight.Bold),
)
val Inter = FontFamily(
    variable(R.font.inter, FontWeight.Normal),
    variable(R.font.inter, FontWeight.Medium),
    variable(R.font.inter, FontWeight.SemiBold),
)
val JetBrainsMono = FontFamily(variable(R.font.jetbrains_mono, FontWeight.Normal))

/** Type scale from the Content Guidelines. Text uses sp so the phone's font size applies. */
object CamSenseType {
    val Display = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp)
    val Title = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val Heading = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val Data = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
}

val CamSenseTypography = Typography(
    displayLarge = CamSenseType.Display,
    headlineMedium = CamSenseType.Title,
    titleLarge = CamSenseType.Title,
    titleMedium = CamSenseType.Heading,
    bodyLarge = CamSenseType.Body,
    bodyMedium = CamSenseType.Body,
    labelLarge = CamSenseType.Label,
    labelMedium = CamSenseType.Label,
    bodySmall = CamSenseType.Caption,
    labelSmall = CamSenseType.Caption,
)
