package com.camsense.modules.magnetometer

/**
 * Hot–cold finder for the close check. The field strength a phone measures doesn't
 * change when you turn it, only when you move it, so this follows the trend as the
 * phone moves: getting warmer, getting colder, or "you passed the strongest point".
 * Pure Kotlin for unit tests; deltas are µT above baseline.
 */
class HotCold(
    private val fastAlpha: Float,
    private val slowAlpha: Float,
    private val trendThreshold: Float,
    private val passedDrop: Float,
    private val peakWindowMs: Long,
    private val minPeak: Float,
) {
    enum class Trend { STEADY, WARMER, COLDER }
    data class State(val trend: Trend = Trend.STEADY, val strongest: Float = 0f, val passedPeak: Boolean = false)

    private var fast = Float.NaN
    private var slow = Float.NaN
    private var strongest = 0f
    private var recentPeak = 0f
    private var recentPeakAt = 0L

    fun update(delta: Float, timeMs: Long): State {
        if (fast.isNaN()) { fast = delta; slow = delta }
        fast += fastAlpha * (delta - fast)
        slow += slowAlpha * (delta - slow)
        strongest = maxOf(strongest, delta)
        if (delta >= recentPeak || timeMs - recentPeakAt > peakWindowMs) {
            recentPeak = delta
            recentPeakAt = timeMs
        }
        val diff = fast - slow
        val trend = when {
            diff > trendThreshold -> Trend.WARMER
            diff < -trendThreshold -> Trend.COLDER
            else -> Trend.STEADY
        }
        val passed = recentPeak >= minPeak && delta < recentPeak * (1f - passedDrop) &&
            timeMs - recentPeakAt <= peakWindowMs
        return State(trend, strongest, passed)
    }

    fun reset() {
        fast = Float.NaN
        slow = Float.NaN
        strongest = 0f
        recentPeak = 0f
        recentPeakAt = 0L
    }
}
