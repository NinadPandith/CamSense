package com.camsense.modules.glint

import org.junit.Assert.assertEquals
import org.junit.Test

class ParallaxCheckTest {

    private fun check() = ParallaxCheck(minSceneMotion = 0.03f, maxRatio = 0.5f)

    @Test
    fun spotMovingWithTheSceneIsAttached() {
        val p = check()
        var x = 0.5f
        p.update(1, x, 0.5f, 0f, 0f)
        var v = ParallaxCheck.Verdict.UNKNOWN
        repeat(10) { x += 0.01f; v = p.update(1, x, 0.5f, 0.01f, 0f) }
        assertEquals(ParallaxCheck.Verdict.ATTACHED, v)
    }

    @Test
    fun spotStayingPutWhileTheSceneMovesIsAReflection() {
        val p = check()
        p.update(1, 0.5f, 0.5f, 0f, 0f)
        var v = ParallaxCheck.Verdict.UNKNOWN
        repeat(10) { v = p.update(1, 0.5f, 0.5f, 0.01f, 0.002f) } // glare follows the phone
        assertEquals(ParallaxCheck.Verdict.REFLECTION, v)
    }

    @Test
    fun noVerdictUntilThePhoneHasMoved() {
        val p = check()
        p.update(1, 0.5f, 0.5f, 0f, 0f)
        repeat(5) { p.update(1, 0.5f, 0.5f, 0.001f, 0f) }
        assertEquals(ParallaxCheck.Verdict.UNKNOWN, p.verdict(1))
    }

    @Test
    fun smallJitterStillCountsAsAttached() {
        val p = check()
        var x = 0.3f
        p.update(7, x, 0.4f, 0f, 0f)
        repeat(12) { i -> x += 0.008f; p.update(7, x + if (i % 2 == 0) 0.001f else -0.001f, 0.4f, 0.008f, 0f) }
        assertEquals(ParallaxCheck.Verdict.ATTACHED, p.verdict(7))
    }
}
