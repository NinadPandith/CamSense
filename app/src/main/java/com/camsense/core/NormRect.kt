package com.camsense.core

/**
 * A box in 0–1 fractions of the upright camera frame (left/top = 0, right/bottom = 1).
 * Stored as box_left/top/right/bottom in the findings table. Kept free of Android
 * classes so the trackers and post-processors can be unit-tested on the laptop.
 */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = (width.coerceAtLeast(0f)) * (height.coerceAtLeast(0f))

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    /** Intersection over union, used by the object tracker. */
    fun iou(other: NormRect): Float {
        val l = maxOf(left, other.left)
        val t = maxOf(top, other.top)
        val r = minOf(right, other.right)
        val b = minOf(bottom, other.bottom)
        val inter = (r - l).coerceAtLeast(0f) * (b - t).coerceAtLeast(0f)
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }

    /** Grows the box by [fraction] of its size on every side, clamped to the frame. */
    fun expand(fraction: Float): NormRect {
        val dx = width * fraction
        val dy = height * fraction
        return NormRect(
            (left - dx).coerceIn(0f, 1f), (top - dy).coerceIn(0f, 1f),
            (right + dx).coerceIn(0f, 1f), (bottom + dy).coerceIn(0f, 1f)
        )
    }

    companion object {
        fun around(cx: Float, cy: Float, halfSize: Float) = NormRect(
            (cx - halfSize).coerceIn(0f, 1f), (cy - halfSize).coerceIn(0f, 1f),
            (cx + halfSize).coerceIn(0f, 1f), (cy + halfSize).coerceIn(0f, 1f)
        )
    }
}
