package com.camsense.ui.results

import com.camsense.core.ModuleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RoomMapTest {
    private fun p(n: Int, angle: Float) = MapPoint(n, ModuleId.GLINT, "spot", angle)

    @Test fun closeSpotsGoOnDifferentRings() {
        val rings = ringIndexes(listOf(p(1, 10f), p(2, 20f), p(3, 180f)))
        assertNotEquals(rings[1], rings[2])
        assertEquals(0, rings[3])
    }

    @Test fun closeAcrossNorthCounts() {
        val rings = ringIndexes(listOf(p(1, 355f), p(2, 5f)))
        assertNotEquals(rings[1], rings[2])
    }

    @Test fun spreadSpotsStayOnOuterRing() {
        val rings = ringIndexes(listOf(p(1, 0f), p(2, 90f), p(3, 180f), p(4, 270f)))
        assertEquals(setOf(0), rings.values.toSet())
    }
}
