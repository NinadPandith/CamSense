package com.camsense.modules.glint

import com.camsense.core.camera.FrameStatsMeter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashCheckTest {

    private fun check() = FlashCheck(latencyFrames = 3, sampleFrames = 4, settleFrames = 4, lensRatio = 0.5, lightRatio = 0.8)

    /** Runs a whole test with the given torch-off brightness; returns the steps. */
    private fun run(fc: FlashCheck, on: Double, off: Double): List<FlashCheck.Step> {
        assertEquals(FlashCheck.Torch.OFF, fc.begin(on))
        val steps = mutableListOf<FlashCheck.Step>()
        var torchOff = true
        repeat(20) {
            if (!fc.active) return steps
            val s = fc.onFrame(if (torchOff) off else on)
            if (s.torch == FlashCheck.Torch.ON) torchOff = false
            steps += s
        }
        return steps
    }

    @Test
    fun reflectionThatVanishesIsALens() {
        val fc = check()
        val steps = run(fc, on = 240.0, off = 60.0)
        assertEquals(1, steps.count { it.torch == FlashCheck.Torch.ON })
        assertEquals(FlashCheck.Verdict.LENS, steps.last().verdict)
        assertFalse(fc.active)
        // 3 latency + 4 samples + 4 settle frames.
        assertEquals(11, steps.size)
    }

    @Test
    fun spotThatStaysLitIsALightSource() {
        assertEquals(FlashCheck.Verdict.LIGHT_SOURCE, run(check(), on = 250.0, off = 245.0).last().verdict)
    }

    @Test
    fun inBetweenIsUnclear() {
        assertEquals(FlashCheck.Verdict.UNCLEAR, run(check(), on = 200.0, off = 130.0).last().verdict)
    }

    @Test
    fun latencyFramesAreIgnored() {
        val fc = check()
        fc.begin(240.0)
        // The camera still shows the torch on for the first frames: bright values there don't count.
        repeat(3) { assertNull(fc.onFrame(255.0).verdict) }
        repeat(3) { fc.onFrame(50.0) }
        fc.onFrame(50.0) // last sample: torch back on
        repeat(3) { fc.onFrame(255.0) }
        assertEquals(FlashCheck.Verdict.LENS, fc.onFrame(255.0).verdict)
    }

    @Test
    fun cancelStops() {
        val fc = check()
        fc.begin(200.0)
        fc.cancel()
        assertFalse(fc.active)
    }

    @Test
    fun statsMeterReportsOncePerSecond() {
        var now = 0L
        val meter = FrameStatsMeter(clock = { now })
        assertNull(meter.tick(10, 50f, 3, 1, 0))
        now = 500
        assertNull(meter.tick(20, 50f, 3, 1, 0))
        now = 1000
        val s = meter.tick(30, 60f, 4, 2, 1)!!
        assertEquals(3f, s.fps, 0.01f)
        assertEquals(20f, s.msPerFrame, 0.01f)
        assertTrue(s.line().contains("confirmed 1"))
    }
}
