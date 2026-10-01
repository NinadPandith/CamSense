package com.camsense.modules.objects

/**
 * How a frame was fitted into the square model input: scaled to fit, then padded
 * equally on both sides (Ultralytics letterbox). Needed to map boxes back.
 */
data class Letterbox(
    val srcWidth: Int,
    val srcHeight: Int,
    val inputSize: Int,
    val scale: Float,
    val padX: Float,
    val padY: Float,
) {
    /** Model-input pixel x → 0–1 fraction of the source frame. */
    fun toSourceX(px: Float): Float = ((px - padX) / scale / srcWidth).coerceIn(0f, 1f)
    fun toSourceY(py: Float): Float = ((py - padY) / scale / srcHeight).coerceIn(0f, 1f)

    companion object {
        fun compute(srcWidth: Int, srcHeight: Int, inputSize: Int): Letterbox {
            val scale = minOf(inputSize.toFloat() / srcWidth, inputSize.toFloat() / srcHeight)
            val newW = srcWidth * scale
            val newH = srcHeight * scale
            return Letterbox(srcWidth, srcHeight, inputSize, scale, (inputSize - newW) / 2f, (inputSize - newH) / 2f)
        }
    }
}
