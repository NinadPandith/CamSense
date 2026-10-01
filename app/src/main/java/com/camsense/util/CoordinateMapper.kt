package com.camsense.util

/**
 * Maps a point in 0–1 fractions of the upright camera frame to view pixels, for a
 * preview drawn with FILL_CENTER (scaled to cover the view, centred, edges cropped).
 * [mirror] flips x for the front camera, whose preview is mirrored.
 */
class CoordinateMapper(
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val viewWidth: Float,
    private val viewHeight: Float,
    private val mirror: Boolean = false,
) {
    private val scale = maxOf(viewWidth / frameWidth, viewHeight / frameHeight)
    private val offsetX = (viewWidth - frameWidth * scale) / 2f
    private val offsetY = (viewHeight - frameHeight * scale) / 2f

    fun x(fx: Float): Float {
        val f = if (mirror) 1f - fx else fx
        return offsetX + f * frameWidth * scale
    }

    fun y(fy: Float): Float = offsetY + fy * frameHeight * scale

    /** Converts a frame-fraction length (in frame-width units) to view pixels. */
    fun length(fraction: Float): Float = fraction * frameWidth * scale
}
