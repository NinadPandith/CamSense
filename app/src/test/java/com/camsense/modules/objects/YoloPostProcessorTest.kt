package com.camsense.modules.objects

import com.camsense.core.NormRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloPostProcessorTest {

    private val labels = listOf("clock", "tissue_box", "vase")
    private val pp = YoloPostProcessor(labels, minConfidence = 0.4f, nmsIou = 0.5f, maxDetections = 20)

    @Test
    fun letterboxForLandscapeFrame() {
        val lb = Letterbox.compute(1280, 720, 640)
        assertEquals(0.5f, lb.scale, 1e-6f)
        assertEquals(0f, lb.padX, 1e-6f)
        assertEquals(140f, lb.padY, 1e-6f)
        assertEquals(0.5f, lb.toSourceX(320f), 1e-6f)
        assertEquals(0f, lb.toSourceY(140f), 1e-6f)
        assertEquals(1f, lb.toSourceY(500f), 1e-6f)
    }

    @Test
    fun endToEndPixelCoords() {
        val lb = Letterbox.compute(640, 640, 640)
        val out = floatArrayOf(
            64f, 64f, 192f, 192f, 0.9f, 1f,   // tissue box
            0f, 0f, 10f, 10f, 0.2f, 0f,       // below threshold
        )
        val d = pp.process(out, intArrayOf(1, 2, 6), lb)
        assertEquals(1, d.size)
        assertEquals("tissue_box", d[0].label)
        assertEquals(NormRect(0.1f, 0.1f, 0.3f, 0.3f), d[0].box)
    }

    @Test
    fun endToEndNormalisedCoords() {
        val lb = Letterbox.compute(640, 640, 640)
        val d = pp.process(floatArrayOf(0.1f, 0.1f, 0.3f, 0.3f, 0.8f, 0f), intArrayOf(1, 1, 6), lb)
        assertEquals("clock", d[0].label)
        assertEquals(0.1f, d[0].box.left, 1e-5f)
    }

    @Test
    fun rawChannelsFirstWithNms() {
        val lb = Letterbox.compute(640, 640, 640)
        val anchors = 3
        // rows: cx, cy, w, h, clock, tissue_box, vase (channels-first: [7][3])
        val rows = arrayOf(
            floatArrayOf(320f, 322f, 100f),
            floatArrayOf(320f, 320f, 100f),
            floatArrayOf(100f, 100f, 50f),
            floatArrayOf(100f, 100f, 50f),
            floatArrayOf(0.9f, 0.8f, 0.1f),
            floatArrayOf(0.1f, 0.1f, 0.1f),
            floatArrayOf(0.0f, 0.0f, 0.7f),
        )
        val out = FloatArray(7 * anchors)
        for (c in 0 until 7) for (a in 0 until anchors) out[c * anchors + a] = rows[c][a]
        val d = pp.process(out, intArrayOf(1, 7, anchors), lb)
        assertEquals(2, d.size) // the two overlapping clocks collapse to one
        assertEquals("clock", d[0].label)
        assertEquals(0.9f, d[0].confidence, 1e-6f)
        assertEquals("vase", d[1].label)
    }

    @Test
    fun rawChannelsLastLayoutDetected() {
        assertEquals(YoloPostProcessor.Layout.RAW_CHANNELS_LAST, pp.layoutFor(intArrayOf(1, 8400, 7)))
        assertEquals(YoloPostProcessor.Layout.RAW_CHANNELS_FIRST, pp.layoutFor(intArrayOf(1, 7, 8400)))
        assertEquals(YoloPostProcessor.Layout.END_TO_END, pp.layoutFor(intArrayOf(1, 300, 6)))
    }

    @Test
    fun cocoSizedLabelsUseEndToEnd() {
        val coco = YoloPostProcessor(List(80) { "c$it" }, 0.4f, 0.5f, 20)
        assertEquals(YoloPostProcessor.Layout.END_TO_END, coco.layoutFor(intArrayOf(1, 300, 6)))
        assertEquals(YoloPostProcessor.Layout.RAW_CHANNELS_FIRST, coco.layoutFor(intArrayOf(1, 84, 8400)))
        assertTrue(coco.process(FloatArray(300 * 6), intArrayOf(1, 300, 6), Letterbox.compute(640, 640, 640)).isEmpty())
    }
}
