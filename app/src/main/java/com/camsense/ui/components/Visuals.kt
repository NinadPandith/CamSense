package com.camsense.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbIridescent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.ModuleId
import com.camsense.core.SkipReason
import com.camsense.scoring.RiskLevel
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Risk is always colour + icon + word, never colour alone. */
data class RiskStyle(
    val icon: ImageVector,
    @StringRes val word: Int,
    val fill: Color,
    val text: Color,
    val tint: Color,
)

fun RiskLevel.style(): RiskStyle = when (this) {
    RiskLevel.LOW -> RiskStyle(Icons.Rounded.VerifiedUser, R.string.risk_low, CamSenseColors.LowFill,
        CamSenseColors.LowText, CamSenseColors.LowFill.copy(alpha = 0.15f))
    RiskLevel.MEDIUM -> RiskStyle(Icons.Rounded.Visibility, R.string.risk_medium, CamSenseColors.MediumFill,
        CamSenseColors.MediumText, CamSenseColors.MediumFill.copy(alpha = 0.15f))
    RiskLevel.HIGH -> RiskStyle(Icons.Rounded.Warning, R.string.risk_high, CamSenseColors.HighFill,
        CamSenseColors.HighText, CamSenseColors.HighFill.copy(alpha = 0.15f))
}

@Composable
fun RiskChip(level: RiskLevel, modifier: Modifier = Modifier) {
    val s = level.style()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.background(s.tint, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(s.icon, contentDescription = null, tint = s.text, modifier = Modifier.size(16.dp))
        Text(stringResource(s.word), style = CamSenseType.Label, color = s.text)
    }
}

fun ModuleId.icon(): ImageVector = when (this) {
    ModuleId.GLINT -> Icons.Rounded.Flare
    ModuleId.INFRARED -> Icons.Rounded.WbIridescent
    ModuleId.NETWORK -> Icons.Rounded.Router
    ModuleId.BLE -> Icons.AutoMirrored.Rounded.BluetoothSearching
    ModuleId.MAGNET -> Icons.Rounded.Explore
    ModuleId.OBJECTS -> Icons.Rounded.CenterFocusStrong
}

@StringRes
fun ModuleId.label(): Int = when (this) {
    ModuleId.GLINT -> R.string.module_glint
    ModuleId.INFRARED -> R.string.module_infrared
    ModuleId.NETWORK -> R.string.module_network
    ModuleId.BLE -> R.string.module_ble
    ModuleId.MAGNET -> R.string.module_magnet
    ModuleId.OBJECTS -> R.string.module_objects
}

@StringRes
fun skipReasonText(reason: String?): Int = when (reason?.let { r -> SkipReason.entries.firstOrNull { it.name == r } }) {
    SkipReason.NO_WIFI -> R.string.skip_no_wifi
    SkipReason.IR_NOT_SUPPORTED -> R.string.skip_ir_not_supported
    SkipReason.NO_CAMERA_PERMISSION -> R.string.skip_no_camera
    SkipReason.NO_NEARBY_PERMISSION -> R.string.skip_no_nearby
    SkipReason.NO_BLUETOOTH -> R.string.skip_no_bluetooth
    SkipReason.NO_MAGNETOMETER -> R.string.skip_no_magnetometer
    SkipReason.MODEL_NOT_LOADED -> R.string.skip_model
    SkipReason.NOT_REACHED -> R.string.skip_not_reached
    SkipReason.MODULE_FAILED, null -> R.string.skip_failed
}
