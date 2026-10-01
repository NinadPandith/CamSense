package com.camsense.ui.history

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.data.db.ScanEntity
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.ScanRow
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Completed scans, newest first. Aborted scans never show here. */
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenScan: (String) -> Unit, onStartScan: () -> Unit) {
    val container = appContainer()
    val scans by remember { container.repository.observeHistory() }.collectAsState(initial = null as List<ScanEntity>?)

    CamSenseScreen(
        title = stringResource(R.string.history_title),
        onBack = onBack,
        bottom = { if (scans?.isEmpty() == true) PrimaryButton(stringResource(R.string.home_start), onStartScan) },
    ) {
        val list = scans ?: return@CamSenseScreen
        if (list.isEmpty()) {
            Text(stringResource(R.string.history_empty), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
        }
        list.forEach { scan -> ScanRow(scan) { onOpenScan(scan.id) } }
    }
}
