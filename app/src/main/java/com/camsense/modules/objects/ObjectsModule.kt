package com.camsense.modules.objects

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.Reasons
import com.camsense.core.SkipReason
import com.camsense.core.camera.CameraOverlay
import com.camsense.core.camera.FrameOps
import com.camsense.core.camera.FrameStats
import com.camsense.core.camera.FrameStatsMeter
import com.camsense.core.camera.MotionMeter
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * M6: marks objects where cameras are often hidden (PRD FR-6.1–6.3). Runs on
 * every frame in the room sweep, and on every few frames in the lens check (on its
 * own thread, so the glint check never waits for the model) so a glint can be tied
 * to the object it sits in.
 */
class ObjectsModule(private val detector: ObjectDetector) : BaseModule(ModuleId.OBJECTS) {

    private val _overlay = MutableStateFlow(CameraOverlay())
    val overlay: StateFlow<CameraOverlay> = _overlay.asStateFlow()

    private val _stats = MutableStateFlow(FrameStats())
    val stats: StateFlow<FrameStats> = _stats.asStateFlow()

    private val tracker = ObjectTracker(ScanConfig.OBJECT_TRACK_IOU, ScanConfig.OBJECT_TRACK_MAX_MISSED,
        ScanConfig.OBJECT_CONFIRM_FRAMES)
    private val motion = MotionMeter(ScanConfig.MOTION_FRAME_SIZE)
    private val meter = FrameStatsMeter()
    private val findingsByKey = linkedMapOf<String, Finding>()
    private val nameByKey = ConcurrentHashMap<String, String>()
    /** Latest tracks, read by the glint thread without taking the lock. */
    @Volatile private var snapshot: List<ObjectTracker.TrackedObject> = emptyList()

    private var worker: ExecutorService? = null
    private val workerBusy = AtomicBoolean(false)

    override fun isSupported(): Boolean = detector.ensureLoaded()

    override suspend fun start() {
        if (results.value.status == ModuleResult.Status.RUNNING) return
        if (!detector.ensureLoaded()) return fail(SkipReason.MODEL_NOT_LOADED)
        markRunning()
    }

    /** Analyzer for the room sweep: every frame goes to the model. */
    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        try {
            val gray = FrameOps.grayUpright(image)
            val fast = try { motion.update(gray) > ScanConfig.MOTION_TOO_FAST } finally { gray.release() }
            processFrame(FrameOps.bitmapUpright(image), tooFast = fast)
        } catch (e: Throwable) {
            Log.w(tag, "Frame failed", e)
        } finally {
            image.close()
        }
    }

    /**
     * Lens check: hands a frame to the model on a background thread. Dropped if the
     * previous one is still being processed, so the caller never waits.
     */
    fun submit(bitmap: Bitmap) {
        if (results.value.status != ModuleResult.Status.RUNNING) return
        if (!workerBusy.compareAndSet(false, true)) return
        val w = worker ?: Executors.newSingleThreadExecutor().also { worker = it }
        try {
            w.execute {
                try { processFrame(bitmap) } catch (e: Throwable) { Log.w(tag, "Frame failed", e) }
                finally { workerBusy.set(false) }
            }
        } catch (e: Exception) {
            workerBusy.set(false) // executor already shut down
        }
    }

    /** Runs the model on one upright frame and updates tracks, findings and the overlay. */
    @Synchronized
    fun processFrame(bitmap: Bitmap, tooFast: Boolean = false) {
        if (results.value.status != ModuleResult.Status.RUNNING) return
        val started = System.currentTimeMillis()
        val raw = detector.detect(bitmap)
        val detections = raw.filter { it.label in ScanConfig.HIDING_OBJECTS }
        snapshot = tracker.update(detections)
        for (t in tracker.allConfirmed()) {
            val name = ScanConfig.HIDING_OBJECTS[t.label] ?: t.label
            nameByKey[t.key] = name
            val existing = findingsByKey[t.key]
            if (existing == null || t.bestConfidence > existing.confidence) {
                findingsByKey[t.key] = Finding(
                    reason = Reasons.objectHidingSpot(name),
                    confidence = t.bestConfidence,
                    targetKey = t.key,
                    screenBox = t.box,
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                )
            }
        }
        val visible = tracker.visibleConfirmed()
        _overlay.value = CameraOverlay(
            frameWidth = bitmap.width,
            frameHeight = bitmap.height,
            marks = visible.map {
                CameraOverlay.Mark(CameraOverlay.Kind.OBJECT, it.box.centerX, it.box.centerY, box = it.box,
                    label = ScanConfig.HIDING_OBJECTS[it.label] ?: it.label)
            },
            tooFast = tooFast,
        )
        val findings = findingsByKey.values.toList()
        publish(findings.maxOfOrNull { it.confidence } ?: 0f, findings)

        meter.tick(System.currentTimeMillis() - started, 0f, raw.size, detections.size, visible.size,
            note = raw.take(3).joinToString { "${it.label} %.2f".format(it.confidence) })?.let {
            _stats.value = it
            Log.d(tag, "objects: ${it.line()}")
        }
    }

    /**
     * A finding the user reported by hand (the mirror fingertip test). Works even when
     * the AI model isn't loaded or the room sweep wasn't part of the scan.
     */
    @Synchronized
    fun addManualFinding(finding: Finding?) {
        finding?.targetKey?.let { key ->
            findingsByKey[key] = finding
            nameByKey[key] = finding.reason.substringBefore(':').trim()
        }
        val findings = findingsByKey.values.toList()
        state.value = ModuleResult(
            moduleId = id,
            status = ModuleResult.Status.DONE,
            score = findings.maxOfOrNull { it.confidence } ?: 0f,
            findings = findings,
        )
    }

    /** target_key of the object at this point of a recent frame, if any. Never blocks. */
    fun objectKeyAt(x: Float, y: Float): String? =
        ObjectTracker.pickAt(snapshot, x, y, ScanConfig.GLINT_OBJECT_MARGIN)?.key

    fun nameFor(key: String): String? = nameByKey[key]

    fun clearOverlay() { _overlay.value = CameraOverlay() }

    override fun stop() {
        if (results.value.status == ModuleResult.Status.RUNNING) finish()
        worker?.shutdown()
        worker = null
    }
}
