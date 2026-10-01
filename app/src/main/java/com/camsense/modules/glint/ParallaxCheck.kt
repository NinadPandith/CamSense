package com.camsense.modules.glint

import kotlin.math.hypot

/**
 * Angle test. A camera lens is part of the object, so as the phone moves it shifts
 * across the picture exactly like the scene around it. A reflection of the flash on
 * flat glass (a tablet or TV screen, a picture frame) depends on the viewing angle,
 * so it slides across the surface instead.
 *
 * For each tracked spot, compare how far it moved with how far the whole scene
 * moved. Once the phone has moved enough to tell, a spot whose own motion differs
 * from the scene's by more than [maxRatio] of it is a reflection. Pure Kotlin for
 * unit tests; positions and shifts are 0–1 fractions of the frame.
 */
class ParallaxCheck(private val minSceneMotion: Float, private val maxRatio: Float) {

    enum class Verdict { UNKNOWN, ATTACHED, REFLECTION }

    private class State(var lastX: Float, var lastY: Float, var scenePath: Float = 0f, var slide: Float = 0f)

    private val states = mutableMapOf<Int, State>()

    /**
     * Records one frame for a spot that was matched in it. [sceneDx]/[sceneDy] is the
     * whole picture's shift since the previous frame.
     */
    fun update(trackId: Int, x: Float, y: Float, sceneDx: Float, sceneDy: Float): Verdict {
        val s = states[trackId]
        if (s == null) {
            states[trackId] = State(x, y)
            return Verdict.UNKNOWN
        }
        val spotDx = x - s.lastX
        val spotDy = y - s.lastY
        s.scenePath += hypot(sceneDx, sceneDy)
        s.slide += hypot(spotDx - sceneDx, spotDy - sceneDy)
        s.lastX = x
        s.lastY = y
        return verdict(s)
    }

    fun verdict(trackId: Int): Verdict = states[trackId]?.let(::verdict) ?: Verdict.UNKNOWN

    private fun verdict(s: State): Verdict = when {
        s.scenePath < minSceneMotion -> Verdict.UNKNOWN
        s.slide > maxRatio * s.scenePath -> Verdict.REFLECTION
        else -> Verdict.ATTACHED
    }

    fun forget(trackId: Int) { states.remove(trackId) }

    fun reset() = states.clear()
}
