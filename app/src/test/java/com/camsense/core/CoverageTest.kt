package com.camsense.core

import com.camsense.core.coverage.CoverageGrid
import com.camsense.core.sensors.PointingMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverageTest {

    @Test
    fun uprightPhoneFacingNorthPointsNorthAndLevel() {
        // Device x = east, y = up, z = south (screen towards the user); columns are device axes in world.
        val r = floatArrayOf(
            1f, 0f, 0f,
            0f, 0f, -1f,
            0f, 1f, 0f,
        )
        val rear = PointingMath.fromRotationMatrix(r)
        assertEquals(0f, rear.yaw, 0.01f)
        assertEquals(0f, rear.pitch, 0.01f)
        val front = PointingMath.fromRotationMatrix(r, frontCamera = true)
        assertEquals(180f, front.yaw, 0.01f) // the screen side faces south
    }

    @Test
    fun phoneFlatOnTableLooksDown() {
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        assertEquals(-90f, PointingMath.fromRotationMatrix(identity).pitch, 0.01f)
    }

    @Test
    fun anglesAndSides() {
        assertEquals(-20f, PointingMath.angleDiff(350f, 10f), 0.001f)
        assertEquals(PointingMath.Side.RIGHT, PointingMath.side(100f, 0f))
        assertEquals(PointingMath.Side.LEFT, PointingMath.side(280f, 0f))
        assertEquals(PointingMath.Side.BEHIND, PointingMath.side(190f, 0f))
        assertEquals(PointingMath.Side.AHEAD, PointingMath.side(20f, 350f))
    }

    private fun grid() = CoverageGrid(sectors = 12, samplesNeeded = 3, lowPitch = -25f, highPitch = 25f)

    @Test
    fun quickFlickDoesNotCount() {
        val g = grid()
        g.add(10f, 0f)
        assertEquals(0f, g.fraction(), 0f)
        repeat(2) { g.add(10f, 0f) }
        assertTrue(g.fraction() > 0f)
    }

    @Test
    fun fullMiddleSweepIsHalfOfTheRoom() {
        val g = grid()
        for (yaw in 0 until 360 step 5) repeat(3) { g.add(yaw.toFloat(), 0f) }
        // Middle band counts double: 24 of 48.
        assertEquals(0.5f, g.fraction(), 1e-6f)
        assertTrue(g.middleRing().all { it })
        assertTrue(g.biggestGap() == CoverageGrid.Gap.CEILING || g.biggestGap() == CoverageGrid.Gap.FLOOR)
    }

    @Test
    fun gapIsReportedRelativeToStart() {
        val g = grid()
        // Start facing 90° (east) and sweep only the front half: 0°..180°.
        for (yaw in listOf(90) + (0 until 180 step 5)) repeat(3) { g.add(yaw.toFloat(), 0f) }
        for (yaw in 0 until 360 step 5) repeat(3) { g.add(yaw.toFloat(), 40f); g.add(yaw.toFloat(), -40f) }
        assertEquals(90f, g.startYaw!!, 0f)
        assertEquals(CoverageGrid.Gap.BEHIND, g.biggestGap())
    }
}
