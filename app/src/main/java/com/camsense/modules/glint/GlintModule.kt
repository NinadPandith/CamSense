package com.camsense.modules.glint

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.NormRect
import com.camsense.core.Reasons
import com.camsense.core.camera.CameraOverlay
import com.camsense.core.camera.FrameOps
import com.camsense.core.camera.FrameStats
import com.camsense.core.camera.FrameStatsMeter
import com.camsense.core.camera.MotionMeter
import com.camsense.data.EvidenceStore
import com.camsense.modules.objects.ObjectsModule
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.core.Mat

/**
 * M1: lens glint with the torch on (PRD FR-1.1–1.5).
 *
 * 1. Finds tiny spots much brighter than their surroundings (FrameOps.brightSpots).
 * 2. Follows them across frames; only spots that stay small and steady confirm.
 * 3. Flash-blink test: for each confirmed spot the torch goes off for ~0.3 s. A lens
 *    reflection vanishes with it (confidence raised); a lamp or LED stays lit
 *    (finding dropped).
 * 4. Links the glint to the AI object it sits in, for the agreement bonus, and saves
 *    a snapshot.
 */
class GlintModule(
    private val evidence: EvidenceStore,
    private val objects: ObjectsModule?,
    private val ioScope: CoroutineScope,
) : BaseModule(ModuleId.GLINT) {

    private val _overlay = MutableStateFlow(CameraOverlay())
    val overlay: StateFlow<CameraOverlay> = _overlay.asStateFlow()

    private val _stats = MutableStateFlow(FrameStats())
    val stats: StateFlow<FrameStats> = _stats.asStateFlow()

    /** Set by the scan so snapshots land in files/evidence/<scanId>/. */
    var scanId: String? = null
    /** "Continue anyway" in a bright room lowers this module's confidence. */
    @Volatile var brightRoomAccepted = false
    @Volatile private var snapshotRequested = false
    /** Torch switch for the flash-blink test; null when no camera is bound. */
    @Volatile private var torch: ((Boolean) -> Unit)? = null

    private val tracker = BlobTracker(
        confirmFrames = ScanConfig.GLINT_CONFIRM_FRAMES,
        maxMissed = ScanConfig.GLINT_MAX_MISSED_FRAMES,
        matchDistance = ScanConfig.GLINT_MATCH_DISTANCE,
        maxSizeRatio = ScanConfig.GLINT_MAX_SIZE_RATIO,
        maxDiameterPx = ScanConfig.GLINT_MAX_DIAMETER_PX,
    )
    private val flash = FlashCheck(
        ScanConfig.FLASH_LATENCY_FRAMES, ScanConfig.FLASH_SAMPLE_FRAMES, ScanConfig.FLASH_SETTLE_FRAMES,
        ScanConfig.FLASH_LENS_RATIO, ScanConfig.FLASH_LIGHT_RATIO,
    )
    private val motion = MotionMeter(ScanConfig.MOTION_FRAME_SIZE)
    private val meter = FrameStatsMeter()
    private val findingsByKey = linkedMapOf<String, Finding>()
    private val keyByTrack = mutableMapOf<Int, String>()
    private val flashQueue = ArrayDeque<Int>()
    private var flashTrack: BlobTracker.Track? = null
    private val lightSourceTracks = mutableSetOf<Int>()
    private val lensKeys = mutableSetOf<String>()
    private var glintCount = 0
    private var frameCount = 0L

    override fun isSupported(): Boolean = true

    override suspend fun start() {
        if (results.value.status == ModuleResult.Status.RUNNING) return
        tracker.reset()
        motion.reset()
        markRunning()
    }

    /** Called by the camera preview when the camera is bound (or released: null). */
    fun attachCamera(camera: Camera?) {
        if (camera == null) {
            torch = null
            if (flash.active) flash.cancel()
            flashTrack = null
            return
        }
        torch = { on -> camera.cameraControl.enableTorch(on) }
    }

    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        try {
            analyze(image)
        } catch (e: Throwable) {
            Log.w(tag, "Frame failed", e)
        } finally {
            image.close() // or CameraX stops sending frames
        }
    }

    private fun analyze(image: ImageProxy) {
        if (results.value.status != ModuleResult.Status.RUNNING) return
        val started = System.currentTimeMillis()
        frameCount++
        val gray = FrameOps.grayUpright(image)
        try {
            analyzeGray(image, gray, started)
        } finally {
            gray.release()
        }
    }

    private fun analyzeGray(image: ImageProxy, gray: Mat, started: Long) {
        val width = gray.cols()
        val height = gray.rows()
        val luma = FrameOps.meanLuma(gray)
        val fast = motion.update(gray) > ScanConfig.MOTION_TOO_FAST

        var frameBitmap: Bitmap? = null
        fun bitmap(): Bitmap = frameBitmap ?: FrameOps.bitmapUpright(image).also { frameBitmap = it }

        // A flash-blink test is running: measure the spot, don't track (the torch is off).
        val testing = flashTrack
        if (flash.active && testing != null) {
            val step = flash.onFrame(FrameOps.peakAround(gray, testing.x, testing.y, ScanConfig.FLASH_SAMPLE_RADIUS_PX))
            step.torch?.let { torch?.invoke(it == FlashCheck.Torch.ON) }
            step.verdict?.let { applyVerdict(testing, it, step.ratio ?: 1.0) }
            publishStats(started, luma.toFloat(), 0, 0, note = "flash test")
            return
        }

        if (snapshotRequested) {
            snapshotRequested = false
            saveSnapshot("photo-${System.currentTimeMillis()}", bitmap())
        }
        // The AI model gets every Nth frame, on its own thread.
        if (objects != null && frameCount % ScanConfig.LENS_OBJECT_EVERY_N_FRAMES == 0L) {
            objects.submit(bitmap())
        }

        val found = FrameOps.brightSpots(gray, ScanConfig.GLINT_SPOTS)
        val update = tracker.update(found.spots)
        for (t in update.newlyConfirmed) {
            val objectKey = objects?.objectKeyAt(t.x, t.y)
            val key = objectKey ?: "glint-${++glintCount}"
            keyByTrack[t.id] = key
            val reason = objectKey?.let { k -> objects.nameFor(k)?.let(Reasons::glintOn) } ?: Reasons.GLINT
            val existing = findingsByKey[key]
            val finding = Finding(
                reason = existing?.reason ?: reason,
                confidence = maxOf(existing?.confidence ?: 0f, confidenceFor(t.hits)),
                targetKey = key,
                screenBox = NormRect.around(t.x, t.y, 0.03f),
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            )
            findingsByKey[key] = finding
            if (existing == null) saveSnapshot(finding.id, bitmap())
            flashQueue.addLast(t.id)
            Log.i(tag, "Glint confirmed at %.2f,%.2f (%.0f px, contrast %.0f) -> %s".format(
                t.x, t.y, t.diameterPx, found.spots.firstOrNull { it.x == t.x && it.y == t.y }?.strength ?: 0f, key))
        }
        // Confidence grows the longer a confirmed glint is tracked.
        val visible = tracker.visibleConfirmed().filter { it.id !in lightSourceTracks }
        for (t in visible) {
            val key = keyByTrack[t.id] ?: continue
            val f = findingsByKey[key] ?: continue
            val c = confidenceFor(t.hits)
            if (c > f.confidence) findingsByKey[key] = f.copy(confidence = c)
        }
        trimUnlinked()
        startNextFlashTest(gray, visible)

        _overlay.value = CameraOverlay(
            frameWidth = width,
            frameHeight = height,
            marks = visible.map {
                CameraOverlay.Mark(CameraOverlay.Kind.GLINT, it.x, it.y,
                    radius = (it.diameterPx / width * 2.5f).coerceAtLeast(0.025f))
            },
            tooBright = luma > ScanConfig.LENS_TOO_BRIGHT_LUMA,
            tooFast = fast,
        )
        publishFindings()
        publishStats(started, luma.toFloat(), found.candidates, found.spots.size)
    }

    /** Starts the flash-blink test for the next confirmed glint that is still in view. */
    private fun startNextFlashTest(gray: Mat, visible: List<BlobTracker.Track>) {
        val switch = torch ?: return
        while (flashQueue.isNotEmpty()) {
            val id = flashQueue.removeFirst()
            val track = visible.firstOrNull { it.id == id } ?: continue
            val brightness = FrameOps.peakAround(gray, track.x, track.y, ScanConfig.FLASH_SAMPLE_RADIUS_PX)
            flashTrack = track
            switch(flash.begin(brightness) == FlashCheck.Torch.ON)
            return
        }
    }

    private fun applyVerdict(track: BlobTracker.Track, verdict: FlashCheck.Verdict, ratio: Double) {
        flashTrack = null
        val key = keyByTrack[track.id] ?: return
        Log.i(tag, "Flash test $key: $verdict (torch-off/on brightness %.2f)".format(ratio))
        val f = findingsByKey[key] ?: return
        when (verdict) {
            FlashCheck.Verdict.LENS -> {
                lensKeys += key
                val reason = if (key.startsWith("glint-")) Reasons.GLINT_FLASH
                else objects?.nameFor(key)?.let(Reasons::glintFlashOn) ?: Reasons.GLINT_FLASH
                findingsByKey[key] = f.copy(confidence = maxOf(f.confidence, ScanConfig.FLASH_LENS_CONFIDENCE), reason = reason)
            }
            FlashCheck.Verdict.LIGHT_SOURCE -> {
                lightSourceTracks += track.id
                // Keep the finding if another glint on the same object already passed as a lens.
                if (key !in lensKeys) findingsByKey.remove(key)
            }
            FlashCheck.Verdict.UNCLEAR -> Unit
        }
        publishFindings()
    }

    private fun publishFindings() {
        val factor = if (brightRoomAccepted) ScanConfig.BRIGHT_ROOM_CONFIDENCE_FACTOR else 1f
        val findings = findingsByKey.values.map { it.copy(confidence = it.confidence * factor) }
        publish(findings.maxOfOrNull { it.confidence } ?: 0f, findings)
    }

    private fun publishStats(started: Long, luma: Float, candidates: Int, spots: Int, note: String = "") {
        meter.tick(System.currentTimeMillis() - started, luma, candidates, spots, findingsByKey.size, note)?.let {
            _stats.value = it
            Log.d(tag, "glint: ${it.line()}")
        }
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

    override fun stop() {
        if (flash.active) {
            flash.cancel()
            torch?.invoke(true)
        }
        finish()
    }

    private companion object { const val MAX_UNLINKED = 6 }
}
