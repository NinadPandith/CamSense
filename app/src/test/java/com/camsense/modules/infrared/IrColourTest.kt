package com.camsense.modules.infrared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrColourTest {
    private val tol = 12f

    @Test
    fun whiteAndPurpleGlowsAreIrLike() {
        assertTrue(IrColour.isIrLike(128f, 128f, tol))   // white
        assertTrue(IrColour.isIrLike(160f, 165f, tol))   // purple / magenta
        assertTrue(IrColour.isIrLike(120f, 122f, tol))   // nearly white
    }

    @Test
    fun colouredStandbyLightsAreNot() {
        assertFalse(IrColour.isIrLike(90f, 200f, tol))   // red
        assertFalse(IrColour.isIrLike(80f, 90f, tol))    // green
        assertFalse(IrColour.isIrLike(200f, 100f, tol))  // blue
        assertFalse(IrColour.isIrLike(70f, 160f, tol))   // amber
    }

    @Test
    fun rotationMapsBackToTheSensorFrame() {
        assertEquals(0.2f to 0.3f, IrColour.uprightToSensor(0.2f, 0.3f, 0))
        // Rotated 90° clockwise: the sensor's top-left corner ends up top-right.
        assertEquals(0f to 0f, IrColour.uprightToSensor(1f, 0f, 90))
        assertEquals(0f to 0f, IrColour.uprightToSensor(1f, 1f, 180))
        assertEquals(0f to 0f, IrColour.uprightToSensor(0f, 1f, 270))
    }
}
