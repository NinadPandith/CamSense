package com.camsense.modules.glint

/**
 * Flash-blink test for one confirmed glint. A camera lens shines only because the
 * torch lights it (retroreflection); a lamp, LED or screen pixel stays lit without
 * it. So: switch the torch off, wait for the camera to catch up, measure the spot,
 * switch the torch back on, let exposure settle, then compare.
 *
 * Feed it the spot's brightness once per frame; it says when to switch the torch
 * and finally gives a verdict. Pure Kotlin for unit tests.
 */
class FlashCheck(
    private val latencyFrames: Int,
    private val sampleFrames: Int,
    private val settleFrames: Int,
    private val lensRatio: Double,
    private val lightRatio: Double,
) {
    enum class Torch { OFF, ON }
    enum class Verdict { LENS, LIGHT_SOURCE, UNCLEAR }
    data class Step(val torch: Torch? = null, val verdict: Verdict? = null, val ratio: Double? = null)

    private enum class Phase { IDLE, OFF_WAIT, OFF_SAMPLE, SETTLE }

    private var phase = Phase.IDLE
    private var count = 0
    private var onBrightness = 0.0
    private var offBrightness = 0.0

    val active: Boolean get() = phase != Phase.IDLE

    /** Starts a test; [brightnessWithTorch] is the spot's brightness right now. Switch the torch off. */
    fun begin(brightnessWithTorch: Double): Torch {
        phase = Phase.OFF_WAIT
        count = 0
        onBrightness = brightnessWithTorch
        offBrightness = 0.0
        return Torch.OFF
    }

    fun onFrame(spotBrightness: Double): Step {
        count++
        return when (phase) {
            Phase.IDLE -> Step()
            Phase.OFF_WAIT -> {
                if (count >= latencyFrames) { phase = Phase.OFF_SAMPLE; count = 0 }
                Step()
            }
            Phase.OFF_SAMPLE -> {
                offBrightness = maxOf(offBrightness, spotBrightness)
                if (count >= sampleFrames) { phase = Phase.SETTLE; count = 0; Step(torch = Torch.ON) } else Step()
            }
            Phase.SETTLE -> {
                if (count < settleFrames) Step()
                else {
                    phase = Phase.IDLE
                    val ratio = if (onBrightness <= 0.0) 1.0 else offBrightness / onBrightness
                    val verdict = when {
                        ratio <= lensRatio -> Verdict.LENS
                        ratio >= lightRatio -> Verdict.LIGHT_SOURCE
                        else -> Verdict.UNCLEAR
                    }
                    Step(verdict = verdict, ratio = ratio)
                }
            }
        }
    }

    /** Abandons a running test (step left, camera lost). The caller turns the torch back on. */
    fun cancel() {
        phase = Phase.IDLE
        count = 0
    }
}
