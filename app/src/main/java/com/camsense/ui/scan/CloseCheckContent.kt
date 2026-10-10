package com.camsense.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.Spot
import com.camsense.modules.magnetometer.HotCold
import com.camsense.modules.magnetometer.MagnetSweepModule
import com.camsense.ui.components.TextAction
import com.camsense.ui.components.icon
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Live meter plus the list of flagged spots; shared by step 5 and the Medium re-check. */
@Composable
fun ColumnScope.CloseCheckContent(
    live: MagnetSweepModule.Live,
    hotCold: HotCold.State,
    spots: List<Spot>,
    onSelect: (String?) -> Unit,
    onReset: () -> Unit,
) {
    Meter(live)
    if (!live.calibrating) HotColdLine(hotCold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextAction(stringResource(R.string.close_reset), onReset)
    }
    if (spots.isEmpty()) {
        Text(stringResource(R.string.close_free_sweep), style = CamSenseType.Body, color = CamSenseColors.Text)
    } else {
        Text(stringResource(R.string.close_pick_spot), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
        spots.forEach { spot ->
            val selected = live.target == spot.key
            val shape = RoundedCornerShape(12.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
                    .background(if (selected) CamSenseColors.PrimaryMuted else CamSenseColors.Background)
                    .border(if (selected) 2.dp else 1.dp, if (selected) CamSenseColors.Primary else CamSenseColors.Border, shape)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(spot.key) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(spot.name, style = CamSenseType.Body, color = CamSenseColors.Text, modifier = Modifier.weight(1f))
                spot.modules.forEach { m ->
                    Icon(m.icon(), contentDescription = null, tint = CamSenseColors.TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** "Getting warmer", "Getting colder", or "You passed the strongest point, go back a little". */
@Composable
private fun HotColdLine(s: HotCold.State) {
    val (icon, text, color) = when {
        s.passedPeak -> Triple(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.hotcold_passed), CamSenseColors.MediumText)
        s.trend == HotCold.Trend.WARMER -> Triple(Icons.Rounded.LocalFireDepartment, stringResource(R.string.hotcold_warmer), CamSenseColors.Primary)
        s.trend == HotCold.Trend.COLDER -> Triple(Icons.Rounded.AcUnit, stringResource(R.string.hotcold_colder), CamSenseColors.TextMuted)
        else -> Triple(Icons.Rounded.Explore, stringResource(R.string.hotcold_move), CamSenseColors.TextMuted)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(text, style = CamSenseType.Body, color = color)
    }
}

@Composable
private fun Meter(live: MagnetSweepModule.Live) {
    val shape = RoundedCornerShape(12.dp)
    val rise = stringResource(R.string.close_rise)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().background(CamSenseColors.Surface, shape)
            .border(1.dp, CamSenseColors.Border, shape).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Explore, null, tint = CamSenseColors.Text, modifier = Modifier.size(48.dp))
            if (live.calibrating) {
                Text(stringResource(R.string.close_calibrating), style = CamSenseType.Body, color = CamSenseColors.Text)
            } else {
                Column {
                    Text(rise, style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
                    Text("%.0f µT".format(live.delta), style = CamSenseType.Data, color = CamSenseColors.Text)
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(CamSenseColors.Border)
                .semantics { contentDescription = "$rise ${"%.0f".format(live.delta)} µT" }
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(live.level).background(CamSenseColors.Primary))
        }
    }
}
