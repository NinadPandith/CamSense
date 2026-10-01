package com.camsense.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilTest {
    @Test
    fun fillCenterCropsLongSide() {
        // 720x1280 portrait frame shown in a 1080x1920 view: exact 1.5x scale, no crop.
        val m = CoordinateMapper(720, 1280, 1080f, 1920f)
        assertEquals(540f, m.x(0.5f), 1e-3f)
        assertEquals(1920f, m.y(1f), 1e-3f)
        // Same frame in a wider view: scale by width, crop top and bottom.
        val wide = CoordinateMapper(720, 1280, 1080f, 1080f)
        assertEquals(540f, wide.y(0.5f), 1e-3f)
        assertEquals(-420f, wide.y(0f), 1e-3f)
    }

    @Test
    fun mirrorFlipsX() {
        val m = CoordinateMapper(100, 100, 100f, 100f, mirror = true)
        assertEquals(90f, m.x(0.1f), 1e-4f)
    }

    @Test
    fun positionWords() {
        assertEquals("top left", PositionWords.of(0.1f, 0.1f))
        assertEquals("center", PositionWords.of(0.5f, 0.5f))
        assertEquals("bottom", PositionWords.of(0.5f, 0.9f))
        assertEquals("right", PositionWords.of(0.9f, 0.5f))
    }
}
