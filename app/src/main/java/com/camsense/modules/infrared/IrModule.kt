package com.camsense.modules.infrared

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.NormRect
import com.camsense.core.Reasons
import com.camsense.core.SkipReason
import com.camsense.core.camera.CameraOverlay
import com.camsense.core.camera.FrameOps
import com.camsense.core.camera.FrameStats
import com.camsense.core.camera.FrameStatsMeter
import com.camsense.core.camera.MotionMeter
import com.camsense.data.SettingsStore
import com.camsense.modules.glint.BlobTracker
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M2: night-vision LEDs seen by the front camera in a dark room (PRD FR-2.1–2.4).
 * Runs only when the TV-remote calibration showed this phone's front camera sees
 * infrared; otherwise it is skipped and Results says so.
 */
class IrModule(private val settings: SettingsStore) : BaseModule(ModuleId.INFRARED) {

    private val _overlay = MutableStateFlow(CameraOverlay())
    val overlay: StateFlow<CameraOverlay> = _overlay.asStateFlow()

    private val _stats = MutableStateFlow(FrameStats())
    val stats: StateFlow<FrameStats> = _stats.asStateFlow()
    private val meter = FrameStatsMeter()

    @Volatile var brightRoomAccepted = false
    @Volatile private var supported = false

    private val tracker = BlobTracker(
        confirmFrames = ScanConfig.IR_CONFIRM_FRAMES,
        maxMissed = ScanConfig.GLINT_MAX_MISSED_FRAMES,
        matchDistance = ScanConfig.GLINT_MATCH_DISTANCE,
        maxSizeRatio = ScanConfig.GLINT_MAX_SIZE_RATIO,
        maxDiameterPx = ScanConfig.IR_MAX_DIAMETER_PX,
    )
    private val motion = MotionMeter(ScanConfig.MOTION_FRAME_SIZE)
    private val findingsByKey = linkedMapOf<String, Finding>()
    private val keyByTrack = mutableMapOf<Int, String>()
    private var count = 0

    override fun isSupported(): Boolean = supported

    /** Reads the calibration result; call before deciding whether to show the step. */
    suspend fun refreshSupport(): Boolean {
        supported = settings.isIrSupported()
        return supported
    }

    override suspend fun start() {
        if (!refreshSupport()) return skip(SkipReason.IR_NOT_SUPPORTED)
        if (results.value.status == ModuleResult.Status.RUNNING) return
        tracker.reset()
        markRunning()
    }

    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        try {
            analyze(image)
        } catch (e: Throwable) {
            Log.w(tag, "Frame failed", e)
        } finally {
            image.close()
        }
    }

    private fun analyze(image: ImageProxy) {
        if (results.value.status != ModuleResult.Status.RUNNING) return
        val gray = FrameOps.grayUpright(image)
        val width = gray.cols()
        val height = gray.rows()
        val luma: Double
        val fast: Boolean
        var blobs = emptyList<com.camsense.modules.glint.Blob>()
        var candidates = 0
        val started = System.currentTimeMillis()
        try {
            luma = FrameOps.meanLuma(gray)
            fast = motion.update(gray) > ScanConfig.MOTION_TOO_FAST
            // FR-2.3: only glows that show in darkness count.
            if (luma <= ScanConfig.IR_DARK_MAX_LUMA || brightRoomAccepted) {
                val found = FrameOps.brightSpots(gray, ScanConfig.IR_SPOTS)
                blobs = found.spots
                candidates = found.candidates
            }
        } finally {
            gray.release()
        }

        val update = tracker.update(blobs)
        for (t in update.newlyConfirmed) {
            val key = "ir-${++count}"
            keyByTrack[t.id] = key
            findingsByKey[key] = Finding(Reasons.IR, ScanConfig.IR_BASE_CONFIDENCE, key,
                screenBox = NormRect.around(t.x, t.y, 0.03f))
        }
        for (t in tracker.visibleConfirmed()) {
            val key = keyByTrack[t.id] ?: continue
            val f = findingsByKey[key] ?: continue
            val c = (ScanConfig.IR_BASE_CONFIDENCE + 0.02f * (t.hits - ScanConfig.IR_CONFIRM_FRAMES)).coerceAtMost(1f)
            if (c > f.confidence) findingsByKey[key] = f.copy(confidence = c)
        }

        _overlay.value = CameraOverlay(
            frameWidth = width, frameHeight = height,
            marks = tracker.visibleConfirmed().map {
                CameraOverlay.Mark(CameraOverlay.Kind.IR, it.x, it.y,
                    radius = (it.diameterPx / width * 2.5f).coerceAtLeast(0.025f))
            },
            tooBright = luma > ScanConfig.IR_DARK_MAX_LUMA,
            tooFast = fast,
        )
        val factor = if (brightRoomAccepted) ScanConfig.BRIGHT_ROOM_CONFIDENCE_FACTOR else 1f
        val findings = findingsByKey.values.map { it.copy(confidence = it.confidence * factor) }
        publish(findings.maxOfOrNull { it.confidence } ?: 0f, findings)
        meter.tick(System.currentTimeMillis() - started, luma.toFloat(), candidates, blobs.size, findingsByKey.size)?.let {
            _stats.value = it
            Log.d(tag, "ir: ${it.line()}")
        }
    }

    fun clearOverlay() { _overlay.value = CameraOverlay() }

    override fun stop() = finish()
}
