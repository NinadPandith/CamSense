package com.camsense.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.camera.CameraOverlay
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import com.camsense.util.CoordinateMapper
import com.camsense.util.PositionWords

/**
 * Draws module marks on the live preview: red pulsing circles for lens glints,
 * amber circles for IR glows, lime boxes with a label chip for AI objects.
 * TalkBack hears "Possible lens found, top left."
 */
@Composable
fun CameraOverlayCanvas(overlay: CameraOverlay, mirror: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()
    val measurer = rememberTextMeasurer()
    val pulse = if (reducedMotion) 0f else rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    ).value

    val announcement = overlay.marks.firstOrNull()?.let { m ->
        val where = PositionWords.of(m.x, m.y)
        when (m.kind) {
            CameraOverlay.Kind.GLINT -> context.getString(R.string.overlay_glint_found, where)
            CameraOverlay.Kind.IR -> context.getString(R.string.overlay_ir_found, where)
            CameraOverlay.Kind.OBJECT -> context.getString(R.string.overlay_object_found, m.label ?: "", where)
        }
    }.orEmpty()

    Box(modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = announcement
    }) {
        Canvas(Modifier.fillMaxSize()) {
            if (overlay.frameWidth == 0 || overlay.frameHeight == 0) return@Canvas
            val map = CoordinateMapper(overlay.frameWidth, overlay.frameHeight, size.width, size.height, mirror)
            val stroke = 2.dp.toPx()
            for (m in overlay.marks) {
                val center = Offset(map.x(m.x), map.y(m.y))
                when (m.kind) {
                    CameraOverlay.Kind.GLINT, CameraOverlay.Kind.IR -> {
                        val color = if (m.kind == CameraOverlay.Kind.GLINT) CamSenseColors.OverlayGlint else CamSenseColors.OverlayIr
                        val r = maxOf(map.length(m.radius), 14.dp.toPx())
                        drawCircle(color, r, center, style = Stroke(stroke))
                        if (m.kind == CameraOverlay.Kind.GLINT && !reducedMotion) {
                            drawCircle(color.copy(alpha = 1f - pulse), r * (1f + pulse), center, style = Stroke(stroke))
                        }
                    }
                    CameraOverlay.Kind.OBJECT -> {
                        val b = m.box ?: continue
                        val left = minOf(map.x(b.left), map.x(b.right))
                        val right = maxOf(map.x(b.left), map.x(b.right))
                        val top = map.y(b.top)
                        val bottom = map.y(b.bottom)
                        val radius = CornerRadius(8.dp.toPx())
                        drawRoundRect(CamSenseColors.OverlayObject, Offset(left, top), Size(right - left, bottom - top),
                            radius, style = Stroke(stroke))
                        val label = m.label ?: continue
                        val layout = measurer.measure(label, CamSenseType.Caption.copy(color = CamSenseColors.OnPrimary))
                        val pad = 6.dp.toPx()
                        val chipTop = (top - layout.size.height - pad * 2).coerceAtLeast(0f)
                        drawRoundRect(CamSenseColors.OverlayObject, Offset(left, chipTop),
                            Size(layout.size.width + pad * 2, layout.size.height + pad), CornerRadius(8.dp.toPx()))
                        drawText(layout, topLeft = Offset(left + pad, chipTop + pad / 2))
                    }
                }
            }
        }
    }
}
