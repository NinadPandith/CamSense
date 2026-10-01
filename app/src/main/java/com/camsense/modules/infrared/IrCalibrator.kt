package com.camsense.modules.infrared

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camsense.core.camera.CameraOverlay
import com.camsense.core.camera.FrameOps
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TV-remote test (PRD FR-2.1): the remote's IR LED flickers while a button is held.
 * If the front camera sees a small bright spot appear and disappear a few times,
 * this phone can see infrared.
 */
class IrCalibrator {

    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected.asStateFlow()

    private val _overlay = MutableStateFlow(CameraOverlay())
    val overlay: StateFlow<CameraOverlay> = _overlay.asStateFlow()

    private var spotVisible = false
    private var risingEdges = 0

    fun reset() {
        spotVisible = false
        risingEdges = 0
        _detected.value = false
    }

    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        try {
            val gray = FrameOps.grayUpright(image)
            try {
                val blobs = FrameOps.brightBlobs(
                    gray, ScanConfig.IR_LUMA_THRESHOLD, ScanConfig.IR_MIN_DIAMETER_PX,
                    ScanConfig.IR_MAX_DIAMETER_PX * 2, minCircularity = 0.3f,
                    circularityMinArea = ScanConfig.GLINT_CIRCULARITY_MIN_AREA, maxBlobs = 8,
                )
                val visible = blobs.isNotEmpty()
                if (visible && !spotVisible) risingEdges++
                spotVisible = visible
                _overlay.value = CameraOverlay(gray.cols(), gray.rows(), blobs.map {
                    CameraOverlay.Mark(CameraOverlay.Kind.IR, it.x, it.y, radius = 0.04f)
                })
                if (risingEdges >= ScanConfig.IR_CALIBRATION_HITS) _detected.value = true
            } finally {
                gray.release()
            }
        } catch (e: Exception) {
            Log.w("CamSense/Infrared", "Calibration frame failed", e)
        } finally {
            image.close()
        }
    }
}
