package com.camsense.core.sensors

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/** Where the camera points: compass heading (0–360°, 0 = north) and up/down angle (−90…90°, up positive). */
data class Pointing(val yaw: Float, val pitch: Float, val timeMs: Long = 0L)

object PointingMath {

    /**
     * Camera direction from a 3×3 rotation matrix (device → world, row-major, as from
     * SensorManager.getRotationMatrixFromVector). The rear camera looks along the
     * device's −Z axis, the front camera along +Z. World X = east, Y = north, Z = up.
     */
    fun fromRotationMatrix(r: FloatArray, frontCamera: Boolean = false): Pointing {
        val sign = if (frontCamera) 1f else -1f
        val east = sign * r[2]
        val north = sign * r[5]
        val up = sign * r[8]
        val yaw = (Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat() + 360f) % 360f
        val pitch = Math.toDegrees(asin(up.coerceIn(-1f, 1f).toDouble())).toFloat()
        return Pointing(yaw, pitch)
    }

    /** Signed difference a − b in degrees, in −180…180. */
    fun angleDiff(a: Float, b: Float): Float {
        var d = (a - b) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    /** "ahead", "right", "behind", "left" for a heading relative to [reference]. */
    fun side(yaw: Float, reference: Float): Side {
        val d = angleDiff(yaw, reference)
        return when {
            abs(d) <= 45f -> Side.AHEAD
            abs(d) >= 135f -> Side.BEHIND
            d > 0 -> Side.RIGHT
            else -> Side.LEFT
        }
    }

    enum class Side { AHEAD, RIGHT, BEHIND, LEFT }
}
