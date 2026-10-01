package com.camsense.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.data.db.ScanEntity
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import java.text.DateFormat
import java.util.Date

fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** Place name (or "Unnamed place"), date and time, risk chip. */
@Composable
fun ScanRow(scan: ScanEntity, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .border(1.dp, CamSenseColors.Border, shape).clickable(onClick = onClick).padding(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(scan.placeName ?: stringResource(R.string.history_unnamed), style = CamSenseType.Heading, color = CamSenseColors.Text)
            Text(formatTime(scan.startedAt), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
        }
        scan.riskLevel?.let { RiskChip(it) }
    }
}
