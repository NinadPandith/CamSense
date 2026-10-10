package com.camsense.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.camsense.scoring.ScanConfig
import com.camsense.ui.components.CoverageRing
import com.camsense.ui.components.coverageGapText
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Coverage ring plus "Not checked yet: …" while coverage is low. Null when the phone has no orientation sensor. */
fun coverageSlot(vm: ScanViewModel): (@Composable () -> Unit)? {
    if (!vm.coverageAvailable) return null
    return {
        val c by vm.coverage.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CoverageRing(c.fraction, c.ring)
            if (c.fraction < ScanConfig.COVERAGE_GOOD) {
                coverageGapText(c.gap)?.let { Text(it, style = CamSenseType.Caption, color = CamSenseColors.Text) }
            }
        }
    }
}
