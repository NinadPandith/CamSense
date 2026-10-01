package com.camsense.core.camera

/** Per-second numbers for the debug line and the log, so thresholds can be tuned on real phones. */
data class FrameStats(
    val fps: Float = 0f,
    val msPerFrame: Float = 0f,
    val luma: Float = 0f,
    val candidates: Int = 0,
    val spots: Int = 0,
    val confirmed: Int = 0,
    val note: String = "",
) {
    fun line(): String = "%.0f fps · %.0f ms · light %.0f · candidates %d · spots %d · confirmed %d%s"
        .format(fps, msPerFrame, luma, candidates, spots, confirmed, if (note.isEmpty()) "" else " · $note")
}

/** Counts frames and processing time; [tick] returns new stats about once a second. */
class FrameStatsMeter(private val periodMs: Long = 1_000L, private val clock: () -> Long = System::currentTimeMillis) {
    private var windowStart = -1L
    private var frames = 0
    private var busyMs = 0L

    fun tick(processingMs: Long, luma: Float, candidates: Int, spots: Int, confirmed: Int, note: String = ""): FrameStats? {
        val now = clock()
        if (windowStart < 0) windowStart = now
        frames++
        busyMs += processingMs
        val elapsed = now - windowStart
        if (elapsed < periodMs) return null
        val stats = FrameStats(
            fps = frames * 1000f / elapsed,
            msPerFrame = busyMs.toFloat() / frames,
            luma = luma, candidates = candidates, spots = spots, confirmed = confirmed, note = note,
        )
        windowStart = now
        frames = 0
        busyMs = 0
        return stats
    }
}
