package com.camsense.modules.infrared

/**
 * Night-vision LEDs (850 nm infrared) look white or purple to a phone camera.
 * Ordinary standby lights are coloured: red, green, blue or amber. In YUV terms
 * (128 = no colour), red and amber have low U, green has low U and V, blue has low V.
 * So a glow is IR-like when neither U nor V is clearly below neutral.
 * Pure Kotlin for unit tests.
 */
object IrColour {
    fun isIrLike(u: Float, v: Float, tolerance: Float): Boolean =
        u >= 128f - tolerance && v >= 128f - tolerance

    /**
     * Maps a point given in upright-frame fractions back to sensor-frame fractions,
     * for a frame that was rotated by [rotationDegrees] clockwise to make it upright.
     */
    fun uprightToSensor(x: Float, y: Float, rotationDegrees: Int): Pair<Float, Float> = when (rotationDegrees) {
        90 -> y to 1f - x
        180 -> 1f - x to 1f - y
        270 -> 1f - y to x
        else -> x to y
    }
}
