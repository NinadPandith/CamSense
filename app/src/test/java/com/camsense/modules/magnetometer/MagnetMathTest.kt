package com.camsense.modules.magnetometer

import com.camsense.scoring.ScanConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagnetMathTest {
    @Test
    fun magnitudeOfVector() = assertEquals(5f, MagnetMath.magnitude(3f, 4f, 0f), 1e-6f)

    @Test
    fun silentBelowStartThreshold() = assertNull(MagnetMath.beepIntervalMs(ScanConfig.MAG_BEEP_START_UT - 1))

    @Test
    fun beepsFasterAsFieldRises() {
        val slow = MagnetMath.beepIntervalMs(10f)!!
        val fast = MagnetMath.beepIntervalMs(50f)!!
        assertTrue(fast < slow)
        assertEquals(ScanConfig.MAG_FASTEST_BEEP_MS, MagnetMath.beepIntervalMs(500f))
    }

    @Test
    fun deltaNeverNegativeAndScoreClamped() {
        assertEquals(0f, MagnetMath.delta(30f, 45f), 0f)
        assertEquals(1f, MagnetMath.score(500f), 0f)
        assertEquals(0.5f, MagnetMath.score(ScanConfig.MAG_FULL_SCORE_UT / 2), 1e-6f)
    }
}
