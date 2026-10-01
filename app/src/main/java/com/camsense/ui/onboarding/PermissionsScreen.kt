package com.camsense.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.permissions.Permissions
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.TextAction
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/**
 * Explains, then requests camera and nearby devices in one go. Denying is fine:
 * the affected checks are skipped and Results explains why.
 */
@Composable
fun PermissionsScreen(firstLaunch: Boolean, onDone: (cameraAllowed: Boolean) -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val cameraOk = remember(refresh) { Permissions.hasCamera(context) }
    val nearbyOk = remember(refresh) { Permissions.hasNearby(context) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
        onDone(Permissions.hasCamera(context))
    }

    CamSenseScreen(
        title = stringResource(R.string.permissions_title),
        onBack = if (firstLaunch) null else ({ onDone(cameraOk) }),
        bottom = {
            PrimaryButton(stringResource(if (cameraOk && nearbyOk) R.string.action_continue else R.string.action_allow), onClick = {
                val missing = (Permissions.camera + Permissions.nearbyDevices)
                    .filterNot { Permissions.granted(context, listOf(it)) }
                if (missing.isEmpty()) onDone(true) else launcher.launch(missing.toTypedArray())
            })
            if (!(cameraOk && nearbyOk)) {
                TextAction(stringResource(R.string.action_not_now), { onDone(cameraOk) }, Modifier.align(Alignment.CenterHorizontally))
            }
        },
    ) {
        Text(stringResource(R.string.permissions_intro), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
        PermissionRow(Icons.Rounded.PhotoCamera, stringResource(R.string.permission_camera),
            stringResource(R.string.permission_camera_copy), cameraOk)
        PermissionRow(
            Icons.AutoMirrored.Rounded.BluetoothSearching,
            stringResource(if (Permissions.nearbyUsesLocation) R.string.permission_location else R.string.permission_nearby),
            stringResource(R.string.permission_nearby_copy),
            nearbyOk,
        )
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, copy: String, granted: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, CamSenseColors.Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = CamSenseColors.Text, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = CamSenseType.Heading, color = CamSenseColors.Text)
            Text(copy, style = CamSenseType.Body, color = CamSenseColors.TextMuted)
            Text(
                stringResource(if (granted) R.string.permission_granted else R.string.permission_denied),
                style = CamSenseType.Caption,
                color = if (granted) CamSenseColors.Primary else CamSenseColors.TextMuted,
            )
        }
    }
}
