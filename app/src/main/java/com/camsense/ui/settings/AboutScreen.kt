package com.camsense.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.ModuleId
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.icon
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** What each check does and what no phone can catch. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val rows = listOf(
        ModuleId.GLINT to R.string.about_glint,
        ModuleId.INFRARED to R.string.about_infrared,
        ModuleId.NETWORK to R.string.about_network,
        ModuleId.BLE to R.string.about_ble,
        ModuleId.MAGNET to R.string.about_magnet,
        ModuleId.OBJECTS to R.string.about_objects,
    )
    CamSenseScreen(title = stringResource(R.string.about_title), onBack = onBack) {
        Text(stringResource(R.string.about_intro), style = CamSenseType.Body, color = CamSenseColors.Text)
        rows.forEach { (module, text) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(module.icon(), null, tint = CamSenseColors.Text, modifier = Modifier.size(24.dp))
                Text(stringResource(text), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
            }
        }
        Text(stringResource(R.string.about_limits_title), style = CamSenseType.Heading, color = CamSenseColors.Text)
        Text(stringResource(R.string.about_limits), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
        Text(stringResource(R.string.limits_caption), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
    }
}
