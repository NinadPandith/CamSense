package com.camsense.modules.objects

import com.camsense.core.NormRect

/**
 * Turns the raw YOLO output tensor into detections. Handles both export styles:
 *
 * - **End-to-end** (YOLO26, no NMS needed): shape [1, N, 6], rows of
 *   x1, y1, x2, y2, score, classId.
 * - **Raw** (YOLO11/YOLOv8 fallback): shape [1, 4 + classes, anchors] or
 *   [1, anchors, 4 + classes], rows of cx, cy, w, h, then one score per class.
 *   Needs NMS, which this class does.
 *
 * Coordinates may come normalised (0–1) or in input pixels; both are detected.
 * Pure Kotlin for unit tests.
 */
class YoloPostProcessor(
    private val labels: List<String>,
    private val minConfidence: Float,
    private val nmsIou: Float,
    private val maxDetections: Int,
) {
    enum class Layout { END_TO_END, RAW_CHANNELS_FIRST, RAW_CHANNELS_LAST }

    fun layoutFor(shape: IntArray): Layout {
        require(shape.size == 3) { "Expected a 3-D output tensor, got ${shape.contentToString()}" }
        val channels = 4 + labels.size
        return when {
            shape[2] == 6 && channels != 6 -> Layout.END_TO_END
            shape[1] == channels -> Layout.RAW_CHANNELS_FIRST
            shape[2] == channels -> Layout.RAW_CHANNELS_LAST
            shape[2] == 6 -> Layout.END_TO_END
            else -> error("Output shape ${shape.contentToString()} doesn't match ${labels.size} labels")
        }
    }

    fun process(output: FloatArray, shape: IntArray, letterbox: Letterbox): List<Detection> =
        when (layoutFor(shape)) {
            Layout.END_TO_END -> endToEnd(output, shape[1], letterbox)
            Layout.RAW_CHANNELS_FIRST -> raw(output, anchors = shape[2], channelsFirst = true, letterbox)
            Layout.RAW_CHANNELS_LAST -> raw(output, anchors = shape[1], channelsFirst = false, letterbox)
        }

    private fun endToEnd(out: FloatArray, rows: Int, lb: Letterbox): List<Detection> {
        var maxCoord = 0f
        for (i in 0 until rows) for (k in 0 until 4) maxCoord = maxOf(maxCoord, out[i * 6 + k])
        val mul = if (maxCoord <= 2f) lb.inputSize.toFloat() else 1f
        val result = ArrayList<Detection>()
        for (i in 0 until rows) {
            val o = i * 6
            val score = out[o + 4]
            if (score < minConfidence) continue
            val cls = out[o + 5].toInt()
            val label = labels.getOrNull(cls) ?: continue
            result += Detection(label, score, box(out[o] * mul, out[o + 1] * mul, out[o + 2] * mul, out[o + 3] * mul, lb))
        }
        return result.sortedByDescending { it.confidence }.take(maxDetections)
    }

    private fun raw(out: FloatArray, anchors: Int, channelsFirst: Boolean, lb: Letterbox): List<Detection> {
        val channels = 4 + labels.size
        fun at(anchor: Int, ch: Int) = if (channelsFirst) out[ch * anchors + anchor] else out[anchor * channels + ch]

        var maxCoord = 0f
        for (a in 0 until anchors) maxCoord = maxOf(maxCoord, at(a, 0), at(a, 1))
        val mul = if (maxCoord <= 2f) lb.inputSize.toFloat() else 1f

        val candidates = ArrayList<Pair<Int, Detection>>()
        for (a in 0 until anchors) {
            var best = -1
            var bestScore = minConfidence
            for (c in labels.indices) {
                val s = at(a, 4 + c)
                if (s >= bestScore) { bestScore = s; best = c }
            }
            if (best < 0) continue
            val cx = at(a, 0) * mul
            val cy = at(a, 1) * mul
            val w = at(a, 2) * mul
            val h = at(a, 3) * mul
            candidates += best to Detection(labels[best], bestScore, box(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, lb))
        }
        return nms(candidates).take(maxDetections)
    }

    /** Class-wise non-maximum suppression. */
    fun nms(candidates: List<Pair<Int, Detection>>): List<Detection> {
        val kept = ArrayList<Pair<Int, Detection>>()
        for (c in candidates.sortedByDescending { it.second.confidence }) {
            if (kept.none { it.first == c.first && it.second.box.iou(c.second.box) > nmsIou }) kept += c
        }
        return kept.map { it.second }
    }

    private fun box(x1: Float, y1: Float, x2: Float, y2: Float, lb: Letterbox) =
        NormRect(lb.toSourceX(x1), lb.toSourceY(y1), lb.toSourceX(x2), lb.toSourceY(y2))
}
