package com.camsense.core.coverage

import com.camsense.core.sensors.PointingMath
import kotlin.math.floor

/**
 * Which parts of the room the camera has pointed at. The room around the user is
 * split into [sectors] compass sectors × 3 bands (low, middle, high). A cell counts
 * as covered after [samplesNeeded] readings in it, so a quick flick past doesn't count.
 * Pure Kotlin for unit tests.
 */
class CoverageGrid(
    private val sectors: Int,
    private val samplesNeeded: Int,
    private val lowPitch: Float,
    private val highPitch: Float,
) {
    enum class Band { LOW, MIDDLE, HIGH }
    enum class Gap { NONE, AHEAD, RIGHT, BEHIND, LEFT, CEILING, FLOOR }

    private val counts = Array(3) { IntArray(sectors) }
    /** Heading when coverage started: "ahead". */
    var startYaw: Float? = null
        private set

    fun add(yaw: Float, pitch: Float) {
        if (startYaw == null) startYaw = yaw
        val band = when {
            pitch < lowPitch -> Band.LOW
            pitch > highPitch -> Band.HIGH
            else -> Band.MIDDLE
        }
        counts[band.ordinal][sectorOf(yaw)]++
    }

    private fun sectorOf(yaw: Float): Int =
        (floor(((yaw % 360f) + 360f) % 360f / (360f / sectors)).toInt()).coerceIn(0, sectors - 1)

    fun covered(band: Band, sector: Int): Boolean = counts[band.ordinal][sector] >= samplesNeeded

    /** 0–1. The middle band (walls at eye level) counts double: that's where most cameras are. */
    fun fraction(): Float {
        var got = 0f
        var total = 0f
        for (band in Band.entries) {
            val w = if (band == Band.MIDDLE) 2f else 1f
            for (s in 0 until sectors) {
                total += w
                if (covered(band, s)) got += w
            }
        }
        return got / total
    }

    /** Middle-band sectors covered, for the ring on screen. */
    fun middleRing(): BooleanArray = BooleanArray(sectors) { covered(Band.MIDDLE, it) }

    /** The biggest part not yet checked, relative to where the user started. */
    fun biggestGap(): Gap {
        val ref = startYaw ?: return Gap.AHEAD
        val missing = mutableMapOf<Gap, Int>()
        for (s in 0 until sectors) {
            if (!covered(Band.MIDDLE, s)) {
                val center = (s + 0.5f) * 360f / sectors
                val gap = when (PointingMath.side(center, ref)) {
                    PointingMath.Side.AHEAD -> Gap.AHEAD
                    PointingMath.Side.RIGHT -> Gap.RIGHT
                    PointingMath.Side.BEHIND -> Gap.BEHIND
                    PointingMath.Side.LEFT -> Gap.LEFT
                }
                missing[gap] = (missing[gap] ?: 0) + 2
            }
            if (!covered(Band.HIGH, s)) missing[Gap.CEILING] = (missing[Gap.CEILING] ?: 0) + 1
            if (!covered(Band.LOW, s)) missing[Gap.FLOOR] = (missing[Gap.FLOOR] ?: 0) + 1
        }
        return missing.maxByOrNull { it.value }?.key ?: Gap.NONE
    }

    fun reset() {
        counts.forEach { it.fill(0) }
        startYaw = null
    }
}
