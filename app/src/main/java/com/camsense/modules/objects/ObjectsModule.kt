package com.camsense.modules.objects

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.Reasons
import com.camsense.core.SkipReason
import com.camsense.core.camera.CameraOverlay
import com.camsense.core.camera.FrameOps
import com.camsense.core.camera.MotionMeter
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M6: marks objects where cameras are often hidden (PRD FR-6.1–6.3). Runs on
 * every frame in the room sweep, and on every few frames in the lens check so a
 * glint can be tied to the object it sits in.
 */
class ObjectsModule(private val detector: ObjectDetector) : BaseModule(ModuleId.OBJECTS) {

    private val _overlay = MutableStateFlow(CameraOverlay())
    val overlay: StateFlow<CameraOverlay> = _overlay.asStateFlow()

    private val tracker = ObjectTracker(ScanConfig.OBJECT_TRACK_IOU, ScanConfig.OBJECT_TRACK_MAX_MISSED,
        ScanConfig.OBJECT_CONFIRM_FRAMES)
    private val motion = MotionMeter(ScanConfig.MOTION_FRAME_SIZE)
    private val findingsByKey = linkedMapOf<String, Finding>()
    private val nameByKey = mutableMapOf<String, String>()

    override fun isSupported(): Boolean = detector.ensureLoaded()

    override suspend fun start() {
        if (results.value.status == com.camsense.core.ModuleResult.Status.RUNNING) return
        if (!detector.ensureLoaded()) return fail(SkipReason.MODEL_NOT_LOADED)
        markRunning()
    }

    /** Analyzer for the room sweep: every frame goes to the model. */
    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        try {
            val gray = FrameOps.grayUpright(image)
            val fast = try { motion.update(gray) > ScanConfig.MOTION_TOO_FAST } finally { gray.release() }
            processFrame(FrameOps.bitmapUpright(image), tooFast = fast)
        } catch (e: Exception) {
            android.util.Log.w(tag, "Frame failed", e)
        } finally {
            image.close()
        }
    }

    /** Runs the model on one upright frame and updates tracks, findings and the overlay. */
    @Synchronized
    fun processFrame(bitmap: Bitmap, tooFast: Boolean = false) {
        if (results.value.status != com.camsense.core.ModuleResult.Status.RUNNING) return
        val detections = detector.detect(bitmap).filter { it.label in ScanConfig.HIDING_OBJECTS }
        tracker.update(detections)
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
        _overlay.value = CameraOverlay(
            frameWidth = bitmap.width,
            frameHeight = bitmap.height,
            marks = tracker.visibleConfirmed().map {
                CameraOverlay.Mark(CameraOverlay.Kind.OBJECT, it.box.centerX, it.box.centerY, box = it.box,
                    label = ScanConfig.HIDING_OBJECTS[it.label] ?: it.label)
            },
            tooFast = tooFast,
        )
        val findings = findingsByKey.values.toList()
        publish(findings.maxOfOrNull { it.confidence } ?: 0f, findings)
    }

    /** target_key of the object at this point of the latest frame, if any. */
    @Synchronized
    fun objectKeyAt(x: Float, y: Float): String? = tracker.objectAt(x, y, ScanConfig.GLINT_OBJECT_MARGIN)?.key

    @Synchronized
    fun nameFor(key: String): String? = nameByKey[key]

    fun clearOverlay() { _overlay.value = CameraOverlay() }

    override fun stop() = finish()
}
