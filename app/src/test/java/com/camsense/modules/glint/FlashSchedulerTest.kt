package com.camsense.modules.glint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashSchedulerTest {

    private fun scheduler() = FlashScheduler(cooldownMs = 2_000, maxTests = 6, samePlaceDistance = 0.06f)

    @Test
    fun cooldownBetweenTests() {
        val s = scheduler()
        assertTrue(s.mayStart(0))
        s.started(0)
        assertFalse(s.mayStart(1_000))
        assertTrue(s.mayStart(2_000))
    }

    @Test
    fun stopsAfterMaxTests() {
        val s = scheduler()
        var now = 0L
        repeat(6) { assertTrue(s.mayStart(now)); s.started(now); now += 2_000 }
        assertFalse(s.mayStart(now + 60_000))
        assertEquals(6, s.testsRun)
    }

    @Test
    fun samePlaceReusesEarlierResult() {
        val s = scheduler()
        s.record(0.5f, 0.5f, "glint-1", FlashCheck.Verdict.LIGHT_SOURCE)
        assertEquals("glint-1", s.testedNear(0.52f, 0.49f)?.key)
        assertNull(s.testedNear(0.7f, 0.5f))
    }

    @Test
    fun testedPlacesFollowTheScene() {
        val s = scheduler()
        s.record(0.5f, 0.5f, "glint-1", FlashCheck.Verdict.LENS)
        repeat(10) { s.onSceneShift(0.02f, 0f) } // phone panned; the spot is now at x = 0.7
        assertEquals(FlashCheck.Verdict.LENS, s.testedNear(0.7f, 0.5f)?.verdict)
        assertNull(s.testedNear(0.5f, 0.5f))
    }

    @Test
    fun placesThatLeaveTheFrameAreForgotten() {
        val s = scheduler()
        s.record(0.9f, 0.5f, "glint-1", FlashCheck.Verdict.LENS)
        repeat(20) { s.onSceneShift(0.03f, 0f) }
        assertNull(s.testedNear(1.5f, 0.5f))
    }
}
