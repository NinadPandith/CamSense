package com.camsense.core

import java.util.UUID

data class ModuleResult(
    val moduleId: ModuleId,
    val status: Status,
    val score: Float,                    // 0.0 to 1.0
    val findings: List<Finding>,
    val skipReason: SkipReason? = null,
    val durationMs: Long? = null,
) {
    enum class Status { IDLE, RUNNING, DONE, SKIPPED, FAILED }

    val isActive: Boolean get() = status == Status.RUNNING || status == Status.DONE

    companion object {
        fun idle(id: ModuleId) = ModuleResult(id, Status.IDLE, 0f, emptyList())
        fun skipped(id: ModuleId, reason: SkipReason) =
            ModuleResult(id, Status.SKIPPED, 0f, emptyList(), skipReason = reason)
        fun failed(id: ModuleId, reason: SkipReason = SkipReason.MODULE_FAILED) =
            ModuleResult(id, Status.FAILED, 0f, emptyList(), skipReason = reason)
    }
}

data class Finding(
    val reason: String,                  // plain words shown to the user
    val confidence: Float,
    val targetKey: String?,              // same object seen by several modules, e.g. "object-3"
    val screenBox: NormRect? = null,     // for camera modules, 0–1 fractions of the frame
    val deviceAddress: String? = null,   // for network and BLE modules
    /** Stable id; a glint snapshot is saved as evidence/<scanId>/<id>.jpg. */
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    /** Compass heading and up/down angle (degrees) of the spot; set when the scan is saved. */
    val yaw: Float? = null,
    val pitch: Float? = null,
)

/** Why a module didn't run. Stored as text in module_runs.skip_reason. */
enum class SkipReason {
    NO_WIFI,
    NO_NEARBY_PERMISSION,
    NO_CAMERA_PERMISSION,
    IR_NOT_SUPPORTED,
    NO_BLUETOOTH,
    NO_MAGNETOMETER,
    MODEL_NOT_LOADED,
    NOT_REACHED,
    /** The check isn't part of the chosen scan (quick check-in, small room). */
    NOT_IN_THIS_SCAN,
    MODULE_FAILED,
}
