package com.camsense.core.camera

import com.camsense.core.NormRect

/** What a camera module wants drawn on its live preview, in upright-frame fractions. */
data class CameraOverlay(
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val marks: List<Mark> = emptyList(),
    val tooBright: Boolean = false,
    val tooFast: Boolean = false,
) {
    enum class Kind { GLINT, IR, OBJECT }

    data class Mark(
        val kind: Kind,
        val x: Float,
        val y: Float,
        /** Circle radius as a fraction of frame width (glint and IR). */
        val radius: Float = 0.03f,
        /** Box for AI objects. */
        val box: NormRect? = null,
        val label: String? = null,
    )
}
