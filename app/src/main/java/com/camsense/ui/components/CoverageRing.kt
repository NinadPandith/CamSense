package com.camsense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.coverage.CoverageGrid
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlin.math.roundToInt

/** Ring of compass sectors (lime = checked) and "85% checked" for the camera steps. */
@Composable
fun CoverageRing(fraction: Float, ring: BooleanArray, modifier: Modifier = Modifier) {
    val percent = (fraction * 100).roundToInt()
    val label = stringResource(R.string.coverage_percent, percent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(CamSenseColors.Surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
    ) {
        Canvas(Modifier.size(28.dp)) {
            val stroke = 4.dp.toPx()
            val n = ring.size.coerceAtLeast(1)
            val sweep = 360f / n
            for (i in 0 until n) {
                drawArc(
                    color = if (ring.getOrElse(i) { false }) CamSenseColors.Primary else CamSenseColors.Border,
                    startAngle = -90f + i * sweep + 2f,
                    sweepAngle = sweep - 4f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke),
                )
            }
        }
        Text(label, style = CamSenseType.Label, color = CamSenseColors.Text)
    }
}

/** "Not checked yet: behind you" for the biggest gap. */
@Composable
fun coverageGapText(gap: CoverageGrid.Gap): String? = when (gap) {
    CoverageGrid.Gap.NONE -> null
    CoverageGrid.Gap.AHEAD -> stringResource(R.string.coverage_gap_ahead)
    CoverageGrid.Gap.RIGHT -> stringResource(R.string.coverage_gap_right)
    CoverageGrid.Gap.BEHIND -> stringResource(R.string.coverage_gap_behind)
    CoverageGrid.Gap.LEFT -> stringResource(R.string.coverage_gap_left)
    CoverageGrid.Gap.CEILING -> stringResource(R.string.coverage_gap_ceiling)
    CoverageGrid.Gap.FLOOR -> stringResource(R.string.coverage_gap_floor)
}
