package com.camsense.ui.home

import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.ConfirmDialog
import com.camsense.core.ScanMode
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.SecondaryButton
import com.camsense.ui.components.ScanRow
import com.camsense.ui.components.TextAction
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

private const val LOW_BATTERY_PERCENT = 10

/** The hub: start a scan, see recent scans, open History or Settings. */
@Composable
fun HomeScreen(onStartScan: (ScanMode) -> Unit, onHistory: () -> Unit, onSettings: () -> Unit, onOpenScan: (String) -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val recent by remember { container.repository.observeRecent(3) }.collectAsState(initial = emptyList())
    /** Scan type waiting for the low-battery answer. */
    var lowBatteryFor by remember { mutableStateOf<ScanMode?>(null) }
    fun start(mode: ScanMode) {
        val battery = context.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        if (battery in 0 until LOW_BATTERY_PERCENT) lowBatteryFor = mode else onStartScan(mode)
    }

    CamSenseScreen(
        title = stringResource(R.string.app_name),
        actions = {
            IconButton(onClick = onHistory) {
                Icon(Icons.Rounded.History, stringResource(R.string.home_history), tint = CamSenseColors.Text)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, stringResource(R.string.home_settings), tint = CamSenseColors.Text)
            }
        },
        bottom = {
            PrimaryButton(stringResource(R.string.home_start), onClick = { start(ScanMode.FULL) })
            SecondaryButton(stringResource(R.string.home_quick), onClick = { start(ScanMode.QUICK) })
            TextAction(stringResource(R.string.home_small_room), { start(ScanMode.SMALL_ROOM) },
                Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, tint = CamSenseColors.Primary, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.home_title), style = CamSenseType.Title, color = CamSenseColors.Text,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.home_body), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_recent), style = CamSenseType.Heading, color = CamSenseColors.Text)
                TextAction(stringResource(R.string.home_see_all), onHistory)
            }
            recent.forEach { scan -> ScanRow(scan) { onOpenScan(scan.id) } }
        }
    }

    lowBatteryFor?.let { mode ->
        ConfirmDialog(
            title = stringResource(R.string.battery_low_title),
            body = stringResource(R.string.battery_low_body),
            confirm = stringResource(R.string.battery_low_start),
            dismiss = stringResource(R.string.action_not_now),
            onConfirm = { lowBatteryFor = null; onStartScan(mode) },
            onDismiss = { lowBatteryFor = null },
        )
    }
}
