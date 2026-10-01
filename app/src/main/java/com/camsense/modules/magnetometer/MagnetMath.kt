package com.camsense.modules.magnetometer

import com.camsense.scoring.ScanConfig
import kotlin.math.sqrt

/** Pure maths for the magnetometer sweep, kept apart from SensorManager for tests. */
object MagnetMath {

    fun magnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

    /** Rise above baseline in µT; never negative. */
    fun delta(magnitude: Float, baseline: Float): Float = (magnitude - baseline).coerceAtLeast(0f)

    /** 0–1 meter level shown on screen. */
    fun level(delta: Float): Float =
        ((delta - ScanConfig.MAG_BEEP_START_UT) / (ScanConfig.MAG_BEEP_MAX_UT - ScanConfig.MAG_BEEP_START_UT))
            .coerceIn(0f, 1f)

    /** Milliseconds between beeps, or null for silence (below the start threshold). */
    fun beepIntervalMs(delta: Float): Long? {
        if (delta < ScanConfig.MAG_BEEP_START_UT) return null
        val l = level(delta)
        return (ScanConfig.MAG_SLOWEST_BEEP_MS -
            l * (ScanConfig.MAG_SLOWEST_BEEP_MS - ScanConfig.MAG_FASTEST_BEEP_MS)).toLong()
    }

    /** Score for a spot from the biggest rise seen near it. */
    fun score(peakDelta: Float): Float = (peakDelta / ScanConfig.MAG_FULL_SCORE_UT).coerceIn(0f, 1f)
}
