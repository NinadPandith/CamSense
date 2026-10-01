package com.camsense.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

private val ButtonShape = RoundedCornerShape(10.dp)

/** The one lime button per screen: full width, 56 dp, within thumb reach. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CamSenseColors.Primary,
            contentColor = CamSenseColors.OnPrimary,
            disabledContainerColor = CamSenseColors.SurfaceRaised,
            disabledContentColor = CamSenseColors.TextMuted,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) { Text(text, style = CamSenseType.Label) }
}

/** Secondary action: 1.5 dp lime outline, lime text. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, if (enabled) CamSenseColors.Primary else CamSenseColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CamSenseColors.Primary),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) { Text(text, style = CamSenseType.Label) }
}

/** Lime text action, at least 48 dp tall. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.height(48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = CamSenseColors.Primary)) {
        Text(text, style = CamSenseType.Label)
    }
}
