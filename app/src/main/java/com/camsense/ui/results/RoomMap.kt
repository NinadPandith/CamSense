package com.camsense.ui.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.ModuleId
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun ModuleId.mapColor(): Color = when (this) {
    ModuleId.GLINT -> CamSenseColors.OverlayGlint
    ModuleId.INFRARED -> CamSenseColors.OverlayIr
    else -> CamSenseColors.OverlayObject
}

/** "ahead", "ahead-right", … for an angle from "ahead" (clockwise, degrees). */
@Composable
fun directionWord(angle: Float): String {
    val i = (((angle + 360f) % 360f) / 45f).roundToInt() % 8
    return stringResource(listOf(
        R.string.dir_ahead, R.string.dir_ahead_right, R.string.dir_right, R.string.dir_behind_right,
        R.string.dir_behind, R.string.dir_behind_left, R.string.dir_left, R.string.dir_ahead_left,
    )[i])
}

/**
 * Where each flagged spot was, around where the user stood. "Ahead" is the way the
 * phone pointed when the camera steps began. Numbers match the list underneath.
 */
@Composable
fun RoomMap(points: List<MapPoint>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val you = stringResource(R.string.map_you)
    val ahead = stringResource(R.string.dir_ahead)
    val description = stringResource(R.string.map_description, points.size)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .background(CamSenseColors.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, CamSenseColors.Border, RoundedCornerShape(12.dp))
                .semantics { contentDescription = description }
        ) {
            Canvas(Modifier.matchParentSize().padding(24.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                for (f in listOf(0.45f, 0.9f)) {
                    drawCircle(CamSenseColors.Border, r * f, c, style = Stroke(1.dp.toPx(), pathEffect = dash))
                }
                drawCircle(CamSenseColors.Text, 6.dp.toPx(), c)
                val youLayout = measurer.measure(you, CamSenseType.Caption.copy(color = CamSenseColors.TextMuted))
                drawText(youLayout, topLeft = Offset(c.x - youLayout.size.width / 2, c.y + 10.dp.toPx()))
                val aheadLayout = measurer.measure(ahead, CamSenseType.Caption.copy(color = CamSenseColors.TextMuted))
                drawText(aheadLayout, topLeft = Offset(c.x - aheadLayout.size.width / 2, -6.dp.toPx()))
                for (p in points) {
                    val a = Math.toRadians(p.angle.toDouble())
                    // Spots seen in the same direction are spread out a little so numbers don't overlap.
                    val ring = r * (0.7f + 0.1f * ((p.number - 1) % 3 - 1))
                    val pos = Offset(c.x + (ring * sin(a)).toFloat(), c.y - (ring * cos(a)).toFloat())
                    val dot = 13.dp.toPx()
                    drawCircle(p.module.mapColor(), dot, pos)
                    val n = measurer.measure("${p.number}", CamSenseType.Label.copy(color = CamSenseColors.OnPrimary))
                    drawText(n, topLeft = Offset(pos.x - n.size.width / 2, pos.y - n.size.height / 2))
                }
            }
        }
        points.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(22.dp).background(p.module.mapColor(), CircleShape), contentAlignment = Alignment.Center) {
                    Text("${p.number}", style = CamSenseType.Caption, color = CamSenseColors.OnPrimary)
                }
                Text("${directionWord(p.angle)} · ${p.reason}", style = CamSenseType.Body, color = CamSenseColors.Text,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}
