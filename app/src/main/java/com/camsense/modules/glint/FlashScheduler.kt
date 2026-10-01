package com.camsense.modules.glint

import kotlin.math.hypot

/**
 * Decides when the flash-blink test may run, so the torch never blinks in a loop:
 * at most one test per [cooldownMs], at most [maxTests] per lens check, and never
 * twice on the same place. A spot that shows up where one was already tested reuses
 * that result. Tested places are moved along with the scene as the phone pans.
 * Pure Kotlin for unit tests; positions are 0–1 frame fractions.
 */
class FlashScheduler(
    private val cooldownMs: Long,
    private val maxTests: Int,
    private val samePlaceDistance: Float,
) {
    data class Tested(val x: Float, val y: Float, val key: String, val verdict: FlashCheck.Verdict)

    private val tested = mutableListOf<Tested>()
    private var lastStartMs = Long.MIN_VALUE / 2
    var testsRun = 0
        private set

    /** Moves remembered places with the scene (shift since the previous frame). */
    fun onSceneShift(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) return
        for (i in tested.indices) tested[i] = tested[i].copy(x = tested[i].x + dx, y = tested[i].y + dy)
        tested.removeAll { it.x < -0.2f || it.x > 1.2f || it.y < -0.2f || it.y > 1.2f }
    }

    /** An earlier test at this place, if any. */
    fun testedNear(x: Float, y: Float): Tested? =
        tested.filter { hypot(it.x - x, it.y - y) <= samePlaceDistance }.minByOrNull { hypot(it.x - x, it.y - y) }

    fun mayStart(nowMs: Long): Boolean = testsRun < maxTests && nowMs - lastStartMs >= cooldownMs

    fun started(nowMs: Long) {
        lastStartMs = nowMs
        testsRun++
    }

    fun record(x: Float, y: Float, key: String, verdict: FlashCheck.Verdict) {
        tested += Tested(x, y, key, verdict)
    }

    fun reset() {
        tested.clear()
        testsRun = 0
        lastStartMs = Long.MIN_VALUE / 2
    }
}
