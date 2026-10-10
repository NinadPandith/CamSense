package com.camsense.ui.scan

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
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.ModuleResult
import com.camsense.core.SkipReason
import com.camsense.modules.network.Hotspot
import com.camsense.modules.network.NetworkDevice
import com.camsense.scoring.ScanConfig
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.ScanStepScreen
import com.camsense.ui.components.SpeakOnEnter
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Step 2: Wi-Fi and Bluetooth scans start here and keep running through step 3. */
@Composable
fun NetworkCheckScreen(vm: ScanViewModel, onContinue: () -> Unit, onAllowNearby: () -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    LaunchedEffect(Unit) { vm.startNetworkChecks() }

    val network by vm.modules.network.results.collectAsState()
    val ble by vm.modules.ble.results.collectAsState()
    val devices by vm.modules.network.devices.collectAsState()
    val bleDevices by vm.modules.ble.devices.collectAsState()
    val progress by vm.modules.network.progress.collectAsState()
    val lanSkip by vm.modules.network.lanSkip.collectAsState()
    val hotspots by vm.modules.network.hotspots.collectAsState()

    val title = stringResource(R.string.network_title)
    val instruction = stringResource(R.string.network_instruction)
    SpeakOnEnter(title, instruction)
    ScanStepScreen(
        step = ScanSteps.dotNumber(ScanStep.NETWORK),
        title = title,
        instruction = instruction,
        onStop = askStop,
        bottom = { TimedStepButton(stringResource(R.string.action_continue), ScanConfig.NETWORK_STEP_MIN_MS, onContinue) },
    ) {
        val needsNearby = network.skipReason == SkipReason.NO_NEARBY_PERMISSION ||
            ble.skipReason == SkipReason.NO_NEARBY_PERMISSION || lanSkip == SkipReason.NO_NEARBY_PERMISSION
        if (needsNearby) {
            NoticeBanner(stringResource(R.string.network_no_permission),
                actionText = stringResource(R.string.action_allow), onAction = onAllowNearby)
        }

        Text(stringResource(R.string.network_wifi_header), style = CamSenseType.Heading, color = CamSenseColors.Text)
        when {
            network.skipReason == SkipReason.NO_WIFI || lanSkip == SkipReason.NO_WIFI ->
                NoticeBanner(stringResource(R.string.network_not_on_wifi))
            network.status == ModuleResult.Status.SKIPPED || network.status == ModuleResult.Status.FAILED -> Unit
            else -> {
                if (network.status == ModuleResult.Status.RUNNING) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = CamSenseColors.Primary,
                        trackColor = CamSenseColors.Border,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (devices.isEmpty()) {
                    Text(stringResource(R.string.network_looking), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
                }
                devices.forEach { DeviceRow(it) }
            }
        }

        if (network.status != ModuleResult.Status.SKIPPED && network.status != ModuleResult.Status.FAILED) {
            Text(stringResource(R.string.network_hotspots_header), style = CamSenseType.Heading, color = CamSenseColors.Text)
            val flaggedNetworks = hotspots.filter { it.suspicionScore >= ScanConfig.NET_FINDING_MIN }
            when {
                hotspots.isEmpty() && network.status == ModuleResult.Status.RUNNING ->
                    Text(stringResource(R.string.network_hotspots_looking), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
                else -> {
                    Text(stringResource(R.string.network_hotspots_count, hotspots.size), style = CamSenseType.Body, color = CamSenseColors.Text)
                    if (flaggedNetworks.isEmpty() && hotspots.isNotEmpty()) {
                        Text(stringResource(R.string.network_hotspots_none_flagged), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
                    }
                    flaggedNetworks.forEach { HotspotRow(it) }
                }
            }
        }

        Text(stringResource(R.string.network_bluetooth_header), style = CamSenseType.Heading, color = CamSenseColors.Text)
        when (ble.skipReason) {
            SkipReason.NO_BLUETOOTH -> NoticeBanner(stringResource(R.string.network_bluetooth_off))
            SkipReason.NO_NEARBY_PERMISSION -> Unit
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.BluetoothSearching, null, tint = CamSenseColors.Text, modifier = Modifier.size(24.dp))
                    Column {
                        Text(stringResource(R.string.network_bluetooth_count, bleDevices.size), style = CamSenseType.Body, color = CamSenseColors.Text)
                        val flagged = bleDevices.count { it.suspicionScore >= ScanConfig.BLE_FINDING_MIN }
                        if (flagged > 0) {
                            Text(stringResource(R.string.network_bluetooth_flagged, flagged), style = CamSenseType.Caption, color = CamSenseColors.MediumText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(d: NetworkDevice) {
    val suspicious = d.suspicionScore >= ScanConfig.NET_FINDING_MIN
    val label = when {
        d.isSelf -> stringResource(R.string.device_you)
        d.isGateway -> stringResource(R.string.device_router)
        d.onvif != null -> stringResource(R.string.device_security_camera)
        suspicious -> stringResource(R.string.device_possible_camera)
        else -> d.mdnsName ?: stringResource(R.string.device_unknown)
    }
    val icon = when {
        d.isSelf -> Icons.Rounded.Smartphone
        suspicious -> Icons.Rounded.Warning
        else -> Icons.Rounded.Router
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, if (suspicious) CamSenseColors.MediumFill else CamSenseColors.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Icon(icon, null, tint = if (suspicious) CamSenseColors.MediumText else CamSenseColors.Text, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = CamSenseType.Body, color = CamSenseColors.Text)
            d.reason?.takeIf { suspicious }?.let { Text(it, style = CamSenseType.Caption, color = CamSenseColors.TextMuted) }
            val ports = if (d.openPorts.isEmpty()) "" else "  ·  " + d.openPorts.joinToString(", ")
            Text(d.ip + ports, style = CamSenseType.Data, color = CamSenseColors.TextMuted)
        }
    }
}

@Composable
private fun HotspotRow(h: Hotspot) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, CamSenseColors.MediumFill, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Icon(Icons.Rounded.Wifi, null, tint = CamSenseColors.MediumText, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(h.ssid, style = CamSenseType.Body, color = CamSenseColors.Text)
            h.reason?.let { Text(it, style = CamSenseType.Caption, color = CamSenseColors.TextMuted) }
            Text("${h.level} dBm" + (h.vendor?.let { "  ·  $it" } ?: ""), style = CamSenseType.Data, color = CamSenseColors.TextMuted)
        }
    }
}
