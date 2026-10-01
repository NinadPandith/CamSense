package com.camsense.modules.glint

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.Reasons
import com.camsense.core.camera.CameraOverlay
import com.camsense.core.camera.FrameOps
import com.camsense.core.camera.MotionMeter
import com.camsense.data.EvidenceStore
import com.camsense.modules.objects.ObjectsModule
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * M1: lens glint with the torch on (PRD FR-1.1–1.5). Finds tiny near-saturated
 * spots, follows them across frames and keeps only the ones that stay small and
 * steady. Each confirmed glint is linked to the AI object it sits in (when there is
 * one) so the RiskEngine can give the agreement bonus, and a snapshot is saved.
 */
class GlintModule(
    private val evidence: EvidenceStore,
    private val objects: ObjectsModule?,
    private val ioScope: CoroutineScope,
) : BaseModule(ModuleId.GLINT) {

    private val _overlay = MutableStateFlow(CameraOverlay())
    val overlay: StateFlow<CameraOverlay> = _overlay.asStateFlow()

    /** Set by the scan so snapshots land in files/evidence/<scanId>/. */
    var scanId: String? = null
    /** "Continue anyway" in a bright room lowers this module's confidence. */
    @Volatile var brightRoomAccepted = false
    @Volatile private var snapshotRequested = false

    private val tracker = BlobTracker(
        confirmFrames = ScanConfig.GLINT_CONFIRM_FRAMES,
        maxMissed = ScanConfig.GLINT_MAX_MISSED_FRAMES,
        matchDistance = ScanConfig.GLINT_MATCH_DISTANCE,
        maxSizeRatio = ScanConfig.GLINT_MAX_SIZE_RATIO,
        maxDiameterPx = ScanConfig.GLINT_MAX_DIAMETER_PX,
    )
    private val motion = MotionMeter(ScanConfig.MOTION_FRAME_SIZE)
    private val findingsByKey = linkedMapOf<String, Finding>()
    private val keyByTrack = mutableMapOf<Int, String>()
    private var glintCount = 0
    private var frameCount = 0L

    override fun isSupported(): Boolean = true

    override suspend fun start() {
        if (results.value.status == ModuleResult.Status.RUNNING) return
        tracker.reset()
        motion.reset()
        markRunning()
    }

    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        try {
            analyze(image)
        } catch (e: Exception) {
            Log.w(tag, "Frame failed", e)
        } finally {
            image.close() // or CameraX stops sending frames
        }
    }

    private fun analyze(image: ImageProxy) {
        if (results.value.status != ModuleResult.Status.RUNNING) return
        frameCount++
        val gray = FrameOps.grayUpright(image)
        val width = gray.cols()
        val height = gray.rows()
        val luma: Double
        val fast: Boolean
        val blobs: List<Blob>
        try {
            luma = FrameOps.meanLuma(gray)
            fast = motion.update(gray) > ScanConfig.MOTION_TOO_FAST
            blobs = FrameOps.brightBlobs(
                gray, ScanConfig.GLINT_LUMA_THRESHOLD, ScanConfig.GLINT_MIN_DIAMETER_PX,
                ScanConfig.GLINT_MAX_DIAMETER_PX, ScanConfig.GLINT_MIN_CIRCULARITY,
                ScanConfig.GLINT_CIRCULARITY_MIN_AREA, ScanConfig.GLINT_MAX_BLOBS_PER_FRAME,
            )
        } finally {
            gray.release()
        }

        // The AI model runs on every Nth frame so glints can be tied to objects.
        var frameBitmap: Bitmap? = null
        fun bitmap(): Bitmap = frameBitmap ?: FrameOps.bitmapUpright(image).also { frameBitmap = it }
        if (objects != null && frameCount % ScanConfig.LENS_OBJECT_EVERY_N_FRAMES == 0L) {
            objects.processFrame(bitmap())
        }

        if (snapshotRequested) {
            snapshotRequested = false
            saveSnapshot("photo-${System.currentTimeMillis()}", bitmap())
        }

        val update = tracker.update(blobs)
        for (t in update.newlyConfirmed) {
            val objectKey = objects?.objectKeyAt(t.x, t.y)
            val key = objectKey ?: "glint-${++glintCount}"
            keyByTrack[t.id] = key
            val reason = objectKey?.let { k -> objects.nameFor(k)?.let(Reasons::glintOn) } ?: Reasons.GLINT
            val existing = findingsByKey[key]
            val finding = Finding(
                reason = reason,
                confidence = maxOf(existing?.confidence ?: 0f, confidenceFor(t.hits)),
                targetKey = key,
                screenBox = com.camsense.core.NormRect.around(t.x, t.y, 0.03f),
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            )
            findingsByKey[key] = finding
            if (existing == null) saveSnapshot(finding.id, bitmap())
        }
        // Confidence grows the longer a confirmed glint is tracked.
        for (t in tracker.visibleConfirmed()) {
            val key = keyByTrack[t.id] ?: continue
            val f = findingsByKey[key] ?: continue
            val c = confidenceFor(t.hits)
            if (c > f.confidence) findingsByKey[key] = f.copy(confidence = c)
        }
        trimUnlinked()

        _overlay.value = CameraOverlay(
            frameWidth = width,
            frameHeight = height,
            marks = tracker.visibleConfirmed().map {
                CameraOverlay.Mark(CameraOverlay.Kind.GLINT, it.x, it.y,
                    radius = (it.diameterPx / width * 2.5f).coerceAtLeast(0.025f))
            },
            tooBright = luma > ScanConfig.LENS_TOO_BRIGHT_LUMA,
            tooFast = fast,
        )
        val factor = if (brightRoomAccepted) ScanConfig.BRIGHT_ROOM_CONFIDENCE_FACTOR else 1f
        val findings = findingsByKey.values.map { it.copy(confidence = it.confidence * factor) }
        publish(findings.maxOfOrNull { it.confidence } ?: 0f, findings)
    }

    private fun confidenceFor(hits: Int): Float {
        val extra = (hits - ScanConfig.GLINT_CONFIRM_FRAMES).toFloat() /
            (ScanConfig.GLINT_FULL_CONFIDENCE_FRAMES - ScanConfig.GLINT_CONFIRM_FRAMES)
        return (ScanConfig.GLINT_BASE_CONFIDENCE + (1f - ScanConfig.GLINT_BASE_CONFIDENCE) * extra.coerceIn(0f, 1f))
    }

    /** Keeps at most 6 glints that aren't tied to an object; the weakest go first. */
    private fun trimUnlinked() {
        val unlinked = findingsByKey.filterKeys { it.startsWith("glint-") }
        if (unlinked.size <= MAX_UNLINKED) return
        unlinked.entries.sortedBy { it.value.confidence }.take(unlinked.size - MAX_UNLINKED)
            .forEach { findingsByKey.remove(it.key) }
    }

    private fun saveSnapshot(findingId: String, bitmap: Bitmap) {
        val id = scanId ?: return
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        ioScope.launch { evidence.saveSnapshot(id, findingId, copy) }
    }

    /** "Take photo": the next frame is kept as an evidence photo. */
    fun requestSnapshot() { snapshotRequested = true }

    fun clearOverlay() { _overlay.value = CameraOverlay() }

    override fun stop() = finish()

    private companion object { const val MAX_UNLINKED = 6 }
}
